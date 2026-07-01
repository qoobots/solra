#include "gpu_abstraction.hpp"
#include <stdexcept>

namespace solra::render {

namespace {

// Platform-specific device creation stubs
// Full implementations in platform/ subdirectories

#if defined(SOLRA_GPU_METAL)
extern std::shared_ptr<GpuDevice> createMetalDevice();
#endif

#if defined(SOLRA_GPU_VULKAN)
extern std::shared_ptr<GpuDevice> createVulkanDevice();
#endif

#if defined(SOLRA_GPU_OPENGLES)
extern std::shared_ptr<GpuDevice> createOpenGLESDevice();
#endif

} // namespace

std::shared_ptr<GpuDevice> createGpuDevice(Backend preferred) {
    // Auto-detect best backend
    if (preferred == Backend::Auto) {
#if defined(SOLRA_GPU_METAL)
        preferred = Backend::Metal;
#elif defined(SOLRA_GPU_VULKAN)
        preferred = Backend::Vulkan;
#elif defined(SOLRA_GPU_OPENGLES)
        preferred = Backend::OpenGLES;
#else
        // No GPU backend compiled — return null, caller handles gracefully
        return nullptr;
#endif
    }

    switch (preferred) {
    case Backend::Metal:
#if defined(SOLRA_GPU_METAL)
        return createMetalDevice();
#else
        return nullptr;
#endif
    case Backend::Vulkan:
#if defined(SOLRA_GPU_VULKAN)
        return createVulkanDevice();
#else
        return nullptr;
#endif
    case Backend::OpenGLES:
#if defined(SOLRA_GPU_OPENGLES)
        return createOpenGLESDevice();
#else
        return nullptr;
#endif
    default:
        return nullptr;
    }
}

} // namespace solra::render
