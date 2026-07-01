/*
 * Solra Core SDK - ONNX Runtime Backend implementation
 *
 * Provides cross-platform ML inference using ONNX Runtime.
 * Supports CPU, CUDA, Metal, CoreML, DirectML, OpenVINO, TensorRT, QNN.
 *
 * Build requirement: onnxruntime (v1.17+) via vcpkg or system package.
 */

#include "onnx_backend.hpp"
#include <spdlog/spdlog.h>
#include <fstream>
#include <cstring>
#include <algorithm>
#include <chrono>
#include <thread>
#include <filesystem>
#include <mutex>

// ============================================================================
// Conditional ONNX Runtime includes
// ============================================================================
#if defined(SOLRA_HAS_ONNX)
#include <onnxruntime_cxx_api.h>
#else
// Stub namespace when ONNX Runtime is not available
namespace Ort {
    struct Env {};
    struct Session {};
    struct MemoryInfo {};
    struct Value {};
    struct RunOptions {};
    struct IoBinding {};
    struct SessionOptions {};
    struct AllocatorWithDefaultOptions {};
}
#endif

namespace solra::inference {

// ============================================================================
// Provider name mapping
// ============================================================================

static const char* providerNameStr(OnnxExecutionProvider ep) {
    switch (ep) {
        case OnnxExecutionProvider::CPU:       return "CPUExecutionProvider";
        case OnnxExecutionProvider::CUDA:      return "CUDAExecutionProvider";
        case OnnxExecutionProvider::TensorRT:  return "TensorrtExecutionProvider";
        case OnnxExecutionProvider::Metal:     return "MetalExecutionProvider";
        case OnnxExecutionProvider::CoreML:    return "CoreMLExecutionProvider";
        case OnnxExecutionProvider::DirectML:  return "DmlExecutionProvider";
        case OnnxExecutionProvider::OpenVINO:  return "OpenVINOExecutionProvider";
        case OnnxExecutionProvider::QNN:       return "QNNExecutionProvider";
        case OnnxExecutionProvider::XNNPACK:   return "XnnpackExecutionProvider";
        case OnnxExecutionProvider::ROCM:      return "ROCMExecutionProvider";
        case OnnxExecutionProvider::VitisAI:   return "VitisAIExecutionProvider";
        case OnnxExecutionProvider::Auto:      return "Auto";
    }
    return "Unknown";
}

// ============================================================================
// OnnxRuntimeManager
// ============================================================================

OnnxRuntimeManager& OnnxRuntimeManager::instance() {
    static OnnxRuntimeManager mgr;
    return mgr;
}

bool OnnxRuntimeManager::initialize(const std::string& logLevel) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (initialized_) return true;

    spdlog::info("OnnxRuntimeManager initializing...");

#if defined(SOLRA_HAS_ONNX)
    try {
        auto* env = new Ort::Env(OrtLoggingLevel::ORT_LOGGING_LEVEL_WARNING, "SolraCore");
        ortEnv_ = env;
        initialized_ = true;

        // Log available providers
        auto providers = availableProviders();
        spdlog::info("ONNX Runtime initialized (v{})", onnxRuntimeVersion());
        spdlog::info("  Available providers: {} detected", providers.size());
        for (auto ep : providers) {
            spdlog::info("    - {}", providerName(ep));
        }
        spdlog::info("  Best provider: {}", providerName(bestProvider()));
    } catch (const std::exception& e) {
        spdlog::error("OnnxRuntimeManager init failed: {}", e.what());
        return false;
    }
#else
    spdlog::warn("OnnxRuntimeManager: ONNX Runtime not available (stub mode)");
    initialized_ = true;
#endif

    return true;
}

void OnnxRuntimeManager::shutdown() {
    std::lock_guard<std::mutex> lock(mutex_);
    if (!initialized_) return;

#if defined(SOLRA_HAS_ONNX)
    delete static_cast<Ort::Env*>(ortEnv_);
    ortEnv_ = nullptr;
#endif

    initialized_ = false;
    spdlog::info("OnnxRuntimeManager shutdown");
}

bool OnnxRuntimeManager::isInitialized() const {
    return initialized_;
}

std::vector<OnnxExecutionProvider> OnnxRuntimeManager::availableProviders() const {
    std::vector<OnnxExecutionProvider> result;
    result.push_back(OnnxExecutionProvider::CPU); // Always available

#if defined(SOLRA_HAS_ONNX)
    try {
        auto* env = static_cast<Ort::Env*>(ortEnv_);
        auto providers = Ort::GetAvailableProviders();

        for (const auto& p : providers) {
            std::string name(p);
            if (name == "CUDAExecutionProvider") result.push_back(OnnxExecutionProvider::CUDA);
            else if (name == "TensorrtExecutionProvider") result.push_back(OnnxExecutionProvider::TensorRT);
            else if (name == "MetalExecutionProvider") result.push_back(OnnxExecutionProvider::Metal);
            else if (name == "CoreMLExecutionProvider") result.push_back(OnnxExecutionProvider::CoreML);
            else if (name == "DmlExecutionProvider") result.push_back(OnnxExecutionProvider::DirectML);
            else if (name == "OpenVINOExecutionProvider") result.push_back(OnnxExecutionProvider::OpenVINO);
            else if (name == "QNNExecutionProvider") result.push_back(OnnxExecutionProvider::QNN);
            else if (name == "XnnpackExecutionProvider") result.push_back(OnnxExecutionProvider::XNNPACK);
            else if (name == "ROCMExecutionProvider") result.push_back(OnnxExecutionProvider::ROCM);
        }
    } catch (...) {}
#endif

    return result;
}

std::string OnnxRuntimeManager::providerName(OnnxExecutionProvider ep) const {
    return providerNameStr(ep);
}

OnnxExecutionProvider OnnxRuntimeManager::bestProvider() const {
    auto providers = availableProviders();

    // Priority: NPU > GPU > CPU
    // Apple: CoreML (ANE) > Metal > CPU
    // Qualcomm: QNN > CPU
    // NVIDIA: TensorRT > CUDA > CPU
    // Intel: OpenVINO > CPU
    // Windows: DirectML > CPU

#if defined(SOLRA_PLATFORM_APPLE)
    for (auto ep : {OnnxExecutionProvider::CoreML, OnnxExecutionProvider::Metal})
        if (std::find(providers.begin(), providers.end(), ep) != providers.end())
            return ep;
#elif defined(SOLRA_PLATFORM_ANDROID)
    for (auto ep : {OnnxExecutionProvider::QNN, OnnxExecutionProvider::XNNPACK})
        if (std::find(providers.begin(), providers.end(), ep) != providers.end())
            return ep;
#elif defined(SOLRA_PLATFORM_WINDOWS)
    for (auto ep : {OnnxExecutionProvider::TensorRT, OnnxExecutionProvider::CUDA,
                     OnnxExecutionProvider::DirectML})
        if (std::find(providers.begin(), providers.end(), ep) != providers.end())
            return ep;
#else
    for (auto ep : {OnnxExecutionProvider::CUDA, OnnxExecutionProvider::TensorRT,
                     OnnxExecutionProvider::OpenVINO})
        if (std::find(providers.begin(), providers.end(), ep) != providers.end())
            return ep;
#endif

    return OnnxExecutionProvider::CPU;
}

std::string OnnxRuntimeManager::onnxRuntimeVersion() {
#if defined(SOLRA_HAS_ONNX)
    return Ort::GetVersionString();
#else
    return "not-linked";
#endif
}

std::unique_ptr<OnnxInferenceSession> OnnxRuntimeManager::createSession(const OnnxConfig& config) {
    auto session = std::make_unique<OnnxInferenceSession>();
    if (!session->loadModel(config)) return nullptr;
    return session;
}

OnnxRuntimeManager::~OnnxRuntimeManager() {
    shutdown();
}

// ============================================================================
// OnnxInferenceSession
// ============================================================================

struct OnnxInferenceSession::Impl {
#if defined(SOLRA_HAS_ONNX)
    std::unique_ptr<Ort::Session> session;
    std::unique_ptr<Ort::MemoryInfo> memoryInfo;
    Ort::AllocatorWithDefaultOptions allocator;
#endif
    OnnxConfig config;
    std::vector<OnnxTensorInfo> inputInfo;
    std::vector<OnnxTensorInfo> outputInfo;
    std::mutex mutex;
    uint64_t totalInferences = 0;
    double totalTimeMs = 0.0;
    double maxTimeMs = 0.0;
    std::vector<double> recentLatencies; // last 100 for percentile calc
    std::atomic<bool> loaded{false};
};

OnnxInferenceSession::OnnxInferenceSession()
    : impl_(std::make_unique<Impl>()) {}

OnnxInferenceSession::~OnnxInferenceSession() {
    unload();
}

bool OnnxInferenceSession::loadModel(const OnnxConfig& config) {
    std::lock_guard<std::mutex> lock(impl_->mutex);
    impl_->config = config;

    if (!std::filesystem::exists(config.modelPath)) {
        spdlog::error("OnnxInferenceSession: model not found: {}", config.modelPath);
        return false;
    }

    spdlog::info("OnnxInferenceSession loading: {}", config.modelPath);
    spdlog::info("  Provider: {}", providerNameStr(config.provider));
    spdlog::info("  Threads: {}/{}", config.intraOpNumThreads, config.interOpNumThreads);

#if defined(SOLRA_HAS_ONNX)
    try {
        auto& env = *static_cast<Ort::Env*>(OnnxRuntimeManager::instance().ortEnv_);

        Ort::SessionOptions sessionOpts;
        sessionOpts.SetGraphOptimizationLevel(
            static_cast<GraphOptimizationLevel>(config.graphOptimizationLevel));
        sessionOpts.SetIntraOpNumThreads(config.intraOpNumThreads);
        sessionOpts.SetInterOpNumThreads(config.interOpNumThreads);

        if (config.enableMemoryPattern) {
            sessionOpts.EnableMemPattern();
        }
        if (config.enableCpuMemArena) {
            sessionOpts.EnableCpuMemArena();
        }

        // Configure execution provider
        if (config.provider == OnnxExecutionProvider::Auto) {
            config.provider = OnnxRuntimeManager::instance().bestProvider();
        }

        switch (config.provider) {
            case OnnxExecutionProvider::CUDA:
                OrtCUDAProviderOptions cudaOpts;
                cudaOpts.device_id = config.cudaDeviceId;
                cudaOpts.arena_extend_strategy = 0;
                cudaOpts.do_copy_in_default_stream = 1;
                if (config.cudaUseTensorCores) {
                    cudaOpts.cudnn_conv_algo_search = OrtCudnnConvAlgoSearchHeuristic;
                }
                sessionOpts.AppendExecutionProvider_CUDA(cudaOpts);
                break;

            case OnnxExecutionProvider::TensorRT:
                OrtTensorRTProviderOptions trtOpts;
                trtOpts.device_id = config.cudaDeviceId;
                trtOpts.trt_fp16_enable = config.tensorrtFp16 ? 1 : 0;
                sessionOpts.AppendExecutionProvider_TensorRT(trtOpts);
                break;

            case OnnxExecutionProvider::CoreML:
                // CoreML EP flags: 0=CPUOnly, 1=CPUAndGPU, 2=ALL (includes ANE)
                uint32_t coremlFlags = config.coremlUseANE ? 2u : 1u;
                sessionOpts.AppendExecutionProvider("CoreML",
                    {{"MLComputeUnits", std::to_string(coremlFlags)}});
                break;

            case OnnxExecutionProvider::DirectML:
                sessionOpts.AppendExecutionProvider("DML",
                    {{"device_id", std::to_string(config.deviceId)}});
                break;

            case OnnxExecutionProvider::Metal:
                sessionOpts.AppendExecutionProvider("Metal", {});
                break;

            case OnnxExecutionProvider::QNN:
                sessionOpts.AppendExecutionProvider("QNN", {
                    {"backend_path", "QnnHtp.dll"}, // or libQnnHtp.so
                });
                break;

            case OnnxExecutionProvider::XNNPACK:
                sessionOpts.AppendExecutionProvider("XNNPACK", {});
                break;

            case OnnxExecutionProvider::OpenVINO:
                sessionOpts.AppendExecutionProvider("OpenVINO", {
                    {"device_type", "CPU"},
                    {"num_of_threads", std::to_string(config.intraOpNumThreads)},
                });
                break;

            default: // CPU
                break;
        }

        if (config.enableProfiling) {
            sessionOpts.EnableProfiling("solra_onnx_profile");
        }

        // Create session
        impl_->session = std::make_unique<Ort::Session>(
            env, config.modelPath.c_str(), sessionOpts);

        impl_->memoryInfo = std::make_unique<Ort::MemoryInfo>(
            "Cpu", OrtAllocatorType::OrtArenaAllocator,
            OrtMemType::OrtMemTypeDefault);

        // Extract I/O info
        size_t numInputs = impl_->session->GetInputCount();
        for (size_t i = 0; i < numInputs; ++i) {
            auto name = impl_->session->GetInputNameAllocated(i, impl_->allocator);
            auto typeInfo = impl_->session->GetInputTypeInfo(i);
            auto tensorInfo = typeInfo.GetTensorTypeAndShapeInfo();

            OnnxTensorInfo info;
            info.name = name.get();
            info.shape = tensorInfo.GetShape();
            info.isDynamic = tensorInfo.GetShape().empty() ||
                std::find(info.shape.begin(), info.shape.end(), -1) != info.shape.end();

            ONNXTensorElementDataType elemType = tensorInfo.GetElementType();
            switch (elemType) {
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT:  info.dtype = OnnxTensorInfo::FLOAT; break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT16: info.dtype = OnnxTensorInfo::FLOAT16; break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_INT32:  info.dtype = OnnxTensorInfo::INT32; break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_INT64:  info.dtype = OnnxTensorInfo::INT64; break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_INT8:   info.dtype = OnnxTensorInfo::INT8; break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_UINT8:  info.dtype = OnnxTensorInfo::UINT8; break;
                case ONNX_TENSOR_ELEMENT_DATA_TYPE_BOOL:   info.dtype = OnnxTensorInfo::BOOL; break;
                default: break;
            }

            impl_->inputInfo.push_back(info);
            spdlog::debug("  Input[{}]: {} shape={} dtype={}",
                          i, info.name, info.shape.size(), static_cast<int>(info.dtype));
        }

        size_t numOutputs = impl_->session->GetOutputCount();
        for (size_t i = 0; i < numOutputs; ++i) {
            auto name = impl_->session->GetOutputNameAllocated(i, impl_->allocator);
            auto typeInfo = impl_->session->GetOutputTypeInfo(i);
            auto tensorInfo = typeInfo.GetTensorTypeAndShapeInfo();

            OnnxTensorInfo info;
            info.name = name.get();
            info.shape = tensorInfo.GetShape();

            impl_->outputInfo.push_back(info);
            spdlog::debug("  Output[{}]: {} shape={}", i, info.name, info.shape.size());
        }

        impl_->loaded = true;
        spdlog::info("OnnxInferenceSession loaded: {} inputs, {} outputs",
                     numInputs, numOutputs);

        return true;
    } catch (const Ort::Exception& e) {
        spdlog::error("OnnxInferenceSession load failed: {} (code={})", e.what(), e.GetOrtErrorCode());
        return false;
    } catch (const std::exception& e) {
        spdlog::error("OnnxInferenceSession load failed: {}", e.what());
        return false;
    }
#else
    spdlog::warn("OnnxInferenceSession: ONNX Runtime not available (stub)");
    impl_->loaded = true;
    return true;
#endif
}

void OnnxInferenceSession::unload() {
    std::lock_guard<std::mutex> lock(impl_->mutex);
    impl_->loaded = false;
#if defined(SOLRA_HAS_ONNX)
    impl_->session.reset();
    impl_->memoryInfo.reset();
#endif
    spdlog::debug("OnnxInferenceSession unloaded");
}

bool OnnxInferenceSession::isLoaded() const {
    return impl_->loaded.load();
}

std::vector<OnnxTensorInfo> OnnxInferenceSession::getInputInfo() const {
    return impl_->inputInfo;
}

std::vector<OnnxTensorInfo> OnnxInferenceSession::getOutputInfo() const {
    return impl_->outputInfo;
}

size_t OnnxInferenceSession::getInputCount() const {
    return impl_->inputInfo.size();
}

size_t OnnxInferenceSession::getOutputCount() const {
    return impl_->outputInfo.size();
}

std::string OnnxInferenceSession::getModelDescription() const {
    return std::filesystem::path(impl_->config.modelPath).filename().string();
}

OnnxInferenceSession::InferResult OnnxInferenceSession::infer(
    const std::vector<float>& input, const std::vector<int64_t>& inputShape) {

    if (impl_->inputInfo.empty()) {
        InferResult r;
        r.success = false;
        r.errorMessage = "No input info available";
        return r;
    }
    return inferMulti({impl_->inputInfo[0].name}, {input}, {inputShape});
}

OnnxInferenceSession::InferResult OnnxInferenceSession::inferMulti(
    const std::vector<std::string>& inputNames,
    const std::vector<std::vector<float>>& inputs,
    const std::vector<std::vector<int64_t>>& shapes) {

    InferResult result;
    auto startTime = std::chrono::high_resolution_clock::now();

#if defined(SOLRA_HAS_ONNX)
    try {
        std::lock_guard<std::mutex> lock(impl_->mutex);
        if (!impl_->session) {
            result.errorMessage = "Session not loaded";
            return result;
        }

        // Create input tensors
        std::vector<Ort::Value> inputTensors;
        std::vector<const char*> inputNamePtrs;

        for (size_t i = 0; i < inputs.size(); ++i) {
            std::vector<int64_t> shape = shapes[i];

            // Replace -1 dynamic dims with actual size
            for (auto& d : shape) {
                if (d < 0) {
                    size_t totalElements = 1;
                    for (auto sd : shape) if (sd > 0) totalElements *= sd;
                    d = inputs[i].size() / std::max(totalElements, size_t(1));
                }
            }

            Ort::Value tensor = Ort::Value::CreateTensor<float>(
                *impl_->memoryInfo,
                const_cast<float*>(inputs[i].data()),
                inputs[i].size(),
                shape.data(),
                shape.size());

            inputTensors.push_back(std::move(tensor));
            inputNamePtrs.push_back(inputNames[i].c_str());
        }

        // Get output names
        std::vector<const char*> outputNamePtrs;
        for (const auto& info : impl_->outputInfo) {
            outputNamePtrs.push_back(info.name.c_str());
        }

        // Run inference
        Ort::RunOptions runOpts;
        auto outputs = impl_->session->Run(
            runOpts,
            inputNamePtrs.data(), inputTensors.data(), inputTensors.size(),
            outputNamePtrs.data(), outputNamePtrs.size());

        // Extract outputs
        for (size_t i = 0; i < outputs.size(); ++i) {
            auto& tensor = outputs[i];
            auto typeInfo = tensor.GetTensorTypeAndShapeInfo();
            auto elemType = typeInfo.GetElementType();
            size_t elemCount = typeInfo.GetElementCount();

            if (elemType == ONNX_TENSOR_ELEMENT_DATA_TYPE_FLOAT) {
                float* data = tensor.GetTensorMutableData<float>();
                result.floatOutputs.emplace_back(data, data + elemCount);
            } else if (elemType == ONNX_TENSOR_ELEMENT_DATA_TYPE_INT32) {
                int32_t* data = tensor.GetTensorMutableData<int32_t>();
                result.intOutputs.emplace_back(data, data + elemCount);
            } else if (elemType == ONNX_TENSOR_ELEMENT_DATA_TYPE_INT64) {
                int64_t* data = tensor.GetTensorMutableData<int64_t>();
                result.int64Outputs.emplace_back(data, data + elemCount);
            }
        }

        result.success = true;
    } catch (const Ort::Exception& e) {
        result.errorMessage = std::string("ONNX error: ") + e.what();
        spdlog::error("Inference failed: {}", result.errorMessage);
    } catch (const std::exception& e) {
        result.errorMessage = std::string("Error: ") + e.what();
        spdlog::error("Inference failed: {}", result.errorMessage);
    }
#else
    // Stub: return dummy output
    result.floatOutputs.push_back({0.0f, 0.0f, 0.0f});
    result.success = true;
    spdlog::trace("ONNX inference stub: returned dummy output");
#endif

    auto endTime = std::chrono::high_resolution_clock::now();
    result.latencyMs = std::chrono::duration<double, std::milli>(endTime - startTime).count();

    // Update stats
    impl_->totalInferences++;
    impl_->totalTimeMs += result.latencyMs;
    impl_->maxTimeMs = std::max(impl_->maxTimeMs, result.latencyMs);
    impl_->recentLatencies.push_back(result.latencyMs);
    if (impl_->recentLatencies.size() > 100) {
        impl_->recentLatencies.erase(impl_->recentLatencies.begin());
    }

    return result;
}

void OnnxInferenceSession::inferAsync(
    const std::vector<float>& input,
    const std::vector<int64_t>& inputShape,
    InferCallback callback) {
    // Run inference on a background thread
    std::thread([this, input, inputShape, cb = std::move(callback)]() {
        auto result = infer(input, inputShape);
        if (cb) cb(result);
    }).detach();
}

OnnxInferenceSession::SessionStats OnnxInferenceSession::stats() const {
    SessionStats s;
    s.totalInferences = impl_->totalInferences;
    s.totalTimeMs = impl_->totalTimeMs;
    s.avgTimeMs = impl_->totalInferences > 0 ? impl_->totalTimeMs / impl_->totalInferences : 0.0;
    s.maxTimeMs = impl_->maxTimeMs;

    // Calculate percentiles from recent latencies
    if (!impl_->recentLatencies.empty()) {
        auto sorted = impl_->recentLatencies;
        std::sort(sorted.begin(), sorted.end());
        size_t n = sorted.size();
        s.p50Ms = sorted[n / 2];
        s.p90Ms = sorted[n * 90 / 100];
        s.p99Ms = sorted[n * 99 / 100];
    }

    return s;
}

bool OnnxInferenceSession::setInputShape(const std::string& name, const std::vector<int64_t>& shape) {
    for (auto& info : impl_->inputInfo) {
        if (info.name == name) {
            info.shape = shape;
            return true;
        }
    }
    return false;
}

bool OnnxInferenceSession::setOutputShape(const std::string& name, const std::vector<int64_t>& shape) {
    for (auto& info : impl_->outputInfo) {
        if (info.name == name) {
            info.shape = shape;
            return true;
        }
    }
    return false;
}

void* OnnxInferenceSession::createIoBinding() {
#if defined(SOLRA_HAS_ONNX)
    try {
        return new Ort::IoBinding(*impl_->session);
    } catch (...) {
        return nullptr;
    }
#else
    return nullptr;
#endif
}

} // namespace solra::inference
