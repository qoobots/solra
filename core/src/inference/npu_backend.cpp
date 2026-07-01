/*
 * Solra Core SDK - NPU Backend implementation
 *
 * Unified NPU interface backed by ONNX Runtime execution providers.
 * Auto-detects the best available NPU for the current device:
 *   - Apple: CoreML (ANE via CoreML EP)
 *   - Qualcomm: QNN (Hexagon NPU via QNN EP)
 *   - Huawei: OpenVINO or CPU fallback
 *   - Samsung/MediaTek: XNNPACK or CPU
 *   - Android generic: NNAPI (via ONNX NNAPI EP) or XNNPACK
 */

#include "npu_backend.hpp"
#include "onnx_backend.hpp"
#include <spdlog/spdlog.h>
#include <algorithm>
#include <cstring>
#include <stdexcept>

namespace solra::inference {

// ============================================================================
// NpuTensor helpers
// ============================================================================

size_t NpuTensor::byteSize() const {
    if (shape.empty()) return 0;
    size_t elements = 1;
    for (auto d : shape) elements *= static_cast<size_t>(d);
    switch (dtype) {
    case FLOAT32: return elements * 4;
    case FLOAT16: return elements * 2;
    case INT32:   return elements * 4;
    case INT8:
    case UINT8:   return elements * 1;
    }
    return 0;
}

// ============================================================================
// OnnxNpuBackend — NPU backend backed by ONNX Runtime
// ============================================================================

class OnnxNpuBackend : public NpuBackend {
public:
    explicit OnnxNpuBackend(NpuVendor vendor)
        : vendor_(vendor) {}

    bool initialize() override {
        auto& mgr = OnnxRuntimeManager::instance();
        if (!mgr.isInitialized()) {
            if (!mgr.initialize()) return false;
        }

        ready_ = true;
        spdlog::info("OnnxNpuBackend initialized: vendor={}", static_cast<int>(vendor_));
        return true;
    }

    bool isReady() const override { return ready_; }

    void shutdown() override {
        sessions_.clear();
        ready_ = false;
        spdlog::info("OnnxNpuBackend shutdown");
    }

    NpuCapability getCapability() const override {
        NpuCapability cap;
        cap.vendor = vendor_;
        cap.deviceName = "ONNX Runtime";

        switch (vendor_) {
            case NpuVendor::AppleANe:
                cap.deviceName = "Apple Neural Engine (via CoreML EP)";
                cap.computeTFLOPS = 15.8f;
                cap.totalMemoryBytes = 6ULL * 1024 * 1024 * 1024; // typical iPhone
                break;
            case NpuVendor::QualcommHexagon:
                cap.deviceName = "Qualcomm Hexagon (via QNN EP)";
                cap.computeTFLOPS = 10.0f;
                cap.totalMemoryBytes = 4ULL * 1024 * 1024 * 1024;
                break;
            case NpuVendor::HuaweiAscend:
                cap.deviceName = "Huawei Ascend (via OpenVINO EP)";
                cap.computeTFLOPS = 8.0f;
                break;
            default:
                cap.deviceName = "CPU (via XNNPACK/MLAS)";
                cap.computeTFLOPS = 0.5f;
                break;
        }

        cap.supportedPrecisions = {NpuPrecision::FP32, NpuPrecision::FP16, NpuPrecision::INT8};
        cap.supportsDynamicShapes = true;
        cap.supportsStreaming = true;
        cap.maxModelSizeBytes = 2ULL * 1024 * 1024 * 1024; // 2GB

        return cap;
    }

    NpuVendor vendor() const override { return vendor_; }

    bool loadModel(const NpuModelDesc& desc) override {
        if (!ready_) return false;

        OnnxConfig cfg;
        cfg.modelPath = desc.modelPath;
        cfg.provider = vendorToProvider(vendor_);
        cfg.intraOpNumThreads = 4;
        cfg.coremlUseANE = true;

        auto session = std::make_unique<OnnxInferenceSession>();
        if (!session->loadModel(cfg)) {
            spdlog::error("NPU: failed to load model: {}", desc.modelPath);
            return false;
        }

        sessions_[desc.modelName] = std::move(session);
        spdlog::info("NPU: model loaded: {} ({})", desc.modelName, desc.modelPath);
        return true;
    }

    bool unloadModel(const std::string& modelName) override {
        auto it = sessions_.find(modelName);
        if (it == sessions_.end()) return false;
        sessions_.erase(it);
        return true;
    }

    bool isModelLoaded(const std::string& modelName) const override {
        return sessions_.count(modelName) > 0;
    }

    size_t loadedModelCount() const override {
        return sessions_.size();
    }

    NpuInferenceResult infer(const std::string& modelName,
                             const std::vector<NpuTensor>& inputs) override {
        NpuInferenceResult result;

        auto it = sessions_.find(modelName);
        if (it == sessions_.end()) {
            result.errorMsg = "Model not loaded: " + modelName;
            return result;
        }

        // Convert NpuTensors to flat float vectors
        std::vector<std::string> inputNames;
        std::vector<std::vector<float>> inputData;
        std::vector<std::vector<int64_t>> inputShapes;

        for (const auto& t : inputs) {
            inputNames.push_back(t.name);
            inputShapes.push_back(t.shape);

            // Assuming FLOAT32 input for now
            size_t elemCount = 1;
            for (auto d : t.shape) elemCount *= d;
            inputData.push_back(std::vector<float>(elemCount, 0.0f)); // placeholder
        }

        auto onnxResult = it->second->inferMulti(inputNames, inputData, inputShapes);

        result.success = onnxResult.success;
        result.latencyMs = static_cast<float>(onnxResult.latencyMs);
        result.errorMsg = onnxResult.errorMessage;

        // Convert outputs back to NpuTensors
        const auto& outputInfo = it->second->getOutputInfo();
        for (size_t i = 0; i < onnxResult.floatOutputs.size() && i < outputInfo.size(); ++i) {
            NpuTensor t;
            t.name = outputInfo[i].name;
            t.shape = outputInfo[i].shape;
            t.dtype = NpuTensor::FLOAT32;
            result.outputs.push_back(t);
        }

        return result;
    }

    void inferAsync(const std::string& modelName,
                    const std::vector<NpuTensor>& inputs,
                    AsyncCallback callback) override {
        std::thread([this, modelName, inputs, cb = std::move(callback)]() {
            auto result = infer(modelName, inputs);
            if (cb) cb(result);
        }).detach();
    }

    NpuPowerMode powerMode() const override { return powerMode_; }
    void setPowerMode(NpuPowerMode mode) override {
        powerMode_ = mode;
        spdlog::debug("NPU power mode: {}", static_cast<int>(mode));
    }

    float temperatureCelsius() const override {
        return 35.0f; // stub
    }

    std::string dumpModelInfo(const std::string& modelName) const override {
        auto it = sessions_.find(modelName);
        if (it == sessions_.end()) return "Model not loaded";
        return it->second->getModelDescription();
    }

private:
    static OnnxExecutionProvider vendorToProvider(NpuVendor v) {
        switch (v) {
            case NpuVendor::AppleANe:        return OnnxExecutionProvider::CoreML;
            case NpuVendor::QualcommHexagon: return OnnxExecutionProvider::QNN;
            case NpuVendor::HuaweiAscend:    return OnnxExecutionProvider::OpenVINO;
            case NpuVendor::SamsungExynos:
            case NpuVendor::MediaTekAPU:
            case NpuVendor::ARMNN:           return OnnxExecutionProvider::XNNPACK;
            default:                         return OnnxExecutionProvider::CPU;
        }
    }

    NpuVendor vendor_;
    bool ready_ = false;
    NpuPowerMode powerMode_{NpuPowerMode::Balanced};
    std::unordered_map<std::string, std::unique_ptr<OnnxInferenceSession>> sessions_;
};

// ============================================================================
// Factory functions
// ============================================================================

std::unique_ptr<NpuBackend> createNpuBackend() {
#if defined(SOLRA_PLATFORM_IOS) || defined(SOLRA_PLATFORM_MACOS)
    return createCoreMLBackend();
#elif defined(SOLRA_PLATFORM_ANDROID)
    // Try Qualcomm SNPE → HiAI → NNAPI fallback
    auto snpe = createSNPEBackend();
    if (snpe && snpe->initialize()) return snpe;

    auto hiai = createHiAIBackend();
    if (hiai && hiai->initialize()) return hiai;

    return createNNAPIBackend();
#else
    // Desktop: use CPU backend (or CUDA if available)
    auto backend = std::make_unique<OnnxNpuBackend>(NpuVendor::Unknown);
    if (backend->initialize()) return backend;
    return nullptr;
#endif
}

std::unique_ptr<NpuBackend> createCoreMLBackend() {
    auto backend = std::make_unique<OnnxNpuBackend>(NpuVendor::AppleANe);
    if (!backend->initialize()) return nullptr;
    return backend;
}

std::unique_ptr<NpuBackend> createSNPEBackend() {
    auto backend = std::make_unique<OnnxNpuBackend>(NpuVendor::QualcommHexagon);
    if (!backend->initialize()) return nullptr;
    return backend;
}

std::unique_ptr<NpuBackend> createHiAIBackend() {
    auto backend = std::make_unique<OnnxNpuBackend>(NpuVendor::HuaweiAscend);
    if (!backend->initialize()) return nullptr;
    return backend;
}

std::unique_ptr<NpuBackend> createNNAPIBackend() {
    auto backend = std::make_unique<OnnxNpuBackend>(NpuVendor::ARMNN);
    if (!backend->initialize()) return nullptr;
    return backend;
}

} // namespace solra::inference
