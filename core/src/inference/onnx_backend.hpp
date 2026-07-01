#pragma once
// ONNX Runtime Backend: cross-platform inference with GPU/NPU acceleration
//
// Provides a unified ONNX Runtime backend that works across:
//   - CPU (all platforms)
//   - CUDA (NVIDIA GPU)
//   - Metal (Apple GPU)
//   - CoreML (Apple Neural Engine)
//   - DirectML (Windows GPU/NPU via DirectX)
//   - OpenVINO (Intel CPU/GPU/NPU)
//   - TensorRT (NVIDIA GPU, best perf)
//   - QNN (Qualcomm Hexagon NPU)
//   - XNNPACK (ARM CPU optimized)
//
// This complements llama.cpp for LLM inference and provides general-purpose
// ML inference for other models (embeddings, classifiers, TTS, etc.)

#include "npu_backend.hpp"
#include <cstdint>
#include <string>
#include <vector>
#include <memory>
#include <functional>
#include <mutex>
#include <unordered_map>
#include <atomic>

namespace solra::inference {

// ============================================================================
// ONNX Backend Configuration
// ============================================================================
enum class OnnxExecutionProvider {
    CPU,            // Default CPU (MLAS/Eigen)
    CUDA,           // NVIDIA GPU
    TensorRT,       // NVIDIA TensorRT (requires model optimization)
    Metal,          // Apple Metal GPU
    CoreML,         // Apple Neural Engine (via CoreML EP)
    DirectML,       // Windows DirectML (GPU/NPU)
    OpenVINO,       // Intel CPU/GPU/NPU
    QNN,            // Qualcomm AI Engine (Hexagon NPU)
    XNNPACK,        // XNNPACK (ARM CPU optimized)
    ROCM,           // AMD GPU
    VitisAI,        // Xilinx FPGA
    Auto,           // Auto-select best available
};

struct OnnxConfig {
    std::string modelPath;                    // .onnx model file
    OnnxExecutionProvider provider = OnnxExecutionProvider::Auto;
    int32_t deviceId = 0;                     // GPU device ID
    int32_t intraOpNumThreads = 0;            // 0 = auto
    int32_t interOpNumThreads = 0;            // 0 = auto
    int32_t graphOptimizationLevel = 2;       // 0=OFF, 1=BASIC, 2=EXTENDED, 99=ALL
    bool enableProfiling = false;
    bool enableMemoryPattern = true;           // Reuse memory across runs
    uint64_t arenaSizeLimitMB = 512;          // Max memory arena size
    bool enableCpuMemArena = true;

    // Provider-specific options
    int32_t cudaDeviceId = 0;
    bool cudaUseTensorCores = true;
    bool tensorrtFp16 = true;
    bool coremlUseANE = true;                  // Use Apple Neural Engine
    bool directmlUseNpu = true;
};

// ============================================================================
// ONNX I/O Binding
// ============================================================================
struct OnnxTensorInfo {
    std::string name;
    std::vector<int64_t> shape;
    enum DataType { FLOAT, FLOAT16, INT32, INT64, INT8, UINT8, BOOL, STRING, BFLOAT16 };
    DataType dtype = FLOAT;
    bool isDynamic = false;
    size_t elementCount() const {
        size_t n = 1;
        for (auto d : shape) n *= static_cast<size_t>(d);
        return n;
    }
    size_t byteSize() const {
        size_t elemSize = 4; // default float
        switch (dtype) {
            case FLOAT16: case BFLOAT16: elemSize = 2; break;
            case INT32: elemSize = 4; break;
            case INT64: elemSize = 8; break;
            case INT8: case UINT8: case BOOL: elemSize = 1; break;
            default: break;
        }
        return elementCount() * elemSize;
    }
};

// ============================================================================
// OnnxInferenceSession — wraps Ort::Session
// ============================================================================
class OnnxInferenceSession {
public:
    OnnxInferenceSession();
    ~OnnxInferenceSession();

    // Load model
    bool loadModel(const OnnxConfig& config);
    void unload();
    bool isLoaded() const;

    // Get model metadata
    std::vector<OnnxTensorInfo> getInputInfo() const;
    std::vector<OnnxTensorInfo> getOutputInfo() const;
    size_t getInputCount() const;
    size_t getOutputCount() const;
    std::string getModelDescription() const;

    // Synchronous inference
    struct InferResult {
        bool success = false;
        std::vector<std::vector<float>> floatOutputs;
        std::vector<std::vector<int32_t>> intOutputs;
        std::vector<std::vector<int64_t>> int64Outputs;
        std::string errorMessage;
        double latencyMs = 0.0;
        size_t memoryUsedBytes = 0;
    };

    InferResult infer(const std::vector<float>& input, const std::vector<int64_t>& inputShape);
    InferResult inferMulti(const std::vector<std::string>& inputNames,
                           const std::vector<std::vector<float>>& inputs,
                           const std::vector<std::vector<int64_t>>& shapes);

    // Asynchronous inference (for LLM token-by-token streaming)
    using InferCallback = std::function<void(InferResult)>;
    void inferAsync(const std::vector<float>& input,
                    const std::vector<int64_t>& inputShape,
                    InferCallback callback);

    // Performance
    struct SessionStats {
        uint64_t totalInferences = 0;
        double totalTimeMs = 0.0;
        double avgTimeMs = 0.0;
        double p50Ms = 0.0, p90Ms = 0.0, p99Ms = 0.0;
        double maxTimeMs = 0.0;
        uint64_t totalMemoryBytes = 0;
    };
    SessionStats stats() const;

    // Dynamic shape support
    bool setInputShape(const std::string& name, const std::vector<int64_t>& shape);
    bool setOutputShape(const std::string& name, const std::vector<int64_t>& shape);

    // IO Binding (zero-copy)
    void* createIoBinding();

private:
    struct Impl;
    std::unique_ptr<Impl> impl_;
};

// ============================================================================
// OnnxRuntimeManager — global ONNX Runtime environment
// ============================================================================
class OnnxRuntimeManager {
public:
    static OnnxRuntimeManager& instance();

    bool initialize(const std::string& logLevel = "warning");
    void shutdown();
    bool isInitialized() const;

    // List available execution providers
    std::vector<OnnxExecutionProvider> availableProviders() const;
    std::string providerName(OnnxExecutionProvider ep) const;

    // Get recommended provider for current hardware
    OnnxExecutionProvider bestProvider() const;

    // Create session with specified config
    std::unique_ptr<OnnxInferenceSession> createSession(const OnnxConfig& config);

    // Version info
    static std::string onnxRuntimeVersion();

private:
    OnnxRuntimeManager() = default;
    ~OnnxRuntimeManager();
    OnnxRuntimeManager(const OnnxRuntimeManager&) = delete;
    OnnxRuntimeManager& operator=(const OnnxRuntimeManager&) = delete;

    bool initialized_ = false;
    std::mutex mutex_;

    // ONNX Runtime environment handle
    void* ortEnv_ = nullptr; // Ort::Env*
};

} // namespace solra::inference
