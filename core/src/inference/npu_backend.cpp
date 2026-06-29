#include "npu_backend.hpp"
#include <stdexcept>
#include <algorithm>

namespace solra::inference {

// ---- NpuTensor helpers ----
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

// ---- Factory: auto-detect ----
std::unique_ptr<NpuBackend> createNpuBackend() {
#if defined(SOLRA_PLATFORM_IOS)
    return createCoreMLBackend();
#elif defined(SOLRA_PLATFORM_ANDROID)
    // Try Qualcomm → HiSilicon → Samsung → MediaTek → NNAPI fallback
    auto snpe = createSNPEBackend();
    if (snpe->initialize()) return snpe;

    auto hiai = createHiAIBackend();
    if (hiai->initialize()) return hiai;

    return createNNAPIBackend();
#else
    return nullptr; // Desktop: no NPU
#endif
}

// ---- Stub implementations (linked to platform SDKs in final build) ----

std::unique_ptr<NpuBackend> createCoreMLBackend() {
    // Stub: CoreML backend wrapper using MLModel + ANE
    // Production impl: iOS platform layer links CoreML.framework
    return nullptr;
}

std::unique_ptr<NpuBackend> createSNPEBackend() {
    // Stub: Qualcomm SNPE SDK backend
    // Production impl: links libSNPE.so, uses SNPEBuilder
    return nullptr;
}

std::unique_ptr<NpuBackend> createHiAIBackend() {
    // Stub: Huawei HiAI Foundation SDK
    // Production impl: links hiai_ir.so, uses ModelManager
    return nullptr;
}

std::unique_ptr<NpuBackend> createNNAPIBackend() {
    // Stub: Android NNAPI fallback
    // Production impl: ANeuralNetworks_* C API
    return nullptr;
}

} // namespace solra::inference
