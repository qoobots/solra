#pragma once
// NPU Backend Abstraction: unified interface over CoreML / SNPE / HiAI / NNAPI
#include <cstdint>
#include <string>
#include <vector>
#include <memory>
#include <functional>
#include <optional>

namespace solra::inference {

enum class NpuVendor { Unknown, AppleANe, QualcommHexagon, HuaweiAscend, SamsungExynos, MediaTekAPU, ARMNN };
enum class NpuPrecision { FP32, FP16, INT8, INT4, Mixed };
enum class NpuPowerMode { LowPower, Balanced, HighPerformance, Turbo };

// ---- Model descriptor ----
struct NpuModelDesc {
    std::string modelPath;         // .mlmodel / .dlc / .om / .tflite
    std::string modelName;
    NpuPrecision precision = NpuPrecision::INT8;
    uint32_t batchSize = 1;
    uint64_t preferredMemoryBytes = 256 * 1024 * 1024; // 256 MB
};

// ---- Tensor descriptor ----
struct NpuTensor {
    std::string name;
    std::vector<int64_t> shape;    // [batch, channels, height, width] or [batch, seq_len, hidden]
    enum DataType { FLOAT32, FLOAT16, INT32, INT8, UINT8 };
    DataType dtype = DataType::FLOAT32;
    size_t byteSize() const;
};

// ---- Inference result ----
struct NpuInferenceResult {
    bool success = false;
    std::vector<NpuTensor> outputs;
    float latencyMs = 0;
    float powerWatts = 0;
    std::string errorMsg;
};

// ---- Capability query ----
struct NpuCapability {
    NpuVendor vendor;
    std::string deviceName;
    uint64_t totalMemoryBytes = 0;
    uint64_t availableMemoryBytes = 0;
    float computeTFLOPS = 0;       // theoretical peak
    uint32_t maxModelSizeBytes = 0;
    std::vector<NpuPrecision> supportedPrecisions;
    bool supportsDynamicShapes = false;
    bool supportsStreaming = false; // LLM token-by-token
};

// ---- NpuBackend ----
class NpuBackend {
public:
    virtual ~NpuBackend() = default;

    // Lifecycle
    virtual bool initialize() = 0;
    virtual bool isReady() const = 0;
    virtual void shutdown() = 0;

    // Capability
    virtual NpuCapability getCapability() const = 0;
    virtual NpuVendor vendor() const = 0;

    // Model management
    virtual bool loadModel(const NpuModelDesc& desc) = 0;
    virtual bool unloadModel(const std::string& modelName) = 0;
    virtual bool isModelLoaded(const std::string& modelName) const = 0;
    virtual size_t loadedModelCount() const = 0;

    // Inference
    virtual NpuInferenceResult infer(const std::string& modelName,
                                     const std::vector<NpuTensor>& inputs) = 0;

    // Async inference (streaming for LLM)
    using AsyncCallback = std::function<void(NpuInferenceResult)>;
    virtual void inferAsync(const std::string& modelName,
                            const std::vector<NpuTensor>& inputs,
                            AsyncCallback callback) = 0;

    // Power / thermal
    virtual NpuPowerMode powerMode() const = 0;
    virtual void setPowerMode(NpuPowerMode mode) = 0;
    virtual float temperatureCelsius() const = 0;

    // Debug
    virtual std::string dumpModelInfo(const std::string& modelName) const = 0;
};

// ---- Factory: auto-detect best NPU for current device ----
std::unique_ptr<NpuBackend> createNpuBackend();

// ---- Per-vendor factory (for testing / forced selection) ----
std::unique_ptr<NpuBackend> createCoreMLBackend();   // Apple ANE
std::unique_ptr<NpuBackend> createSNPEBackend();     // Qualcomm Hexagon
std::unique_ptr<NpuBackend> createHiAIBackend();     // Huawei Ascend
std::unique_ptr<NpuBackend> createNNAPIBackend();    // Android NNAPI fallback

} // namespace solra::inference
