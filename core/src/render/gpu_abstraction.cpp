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

} // namespace

std::shared_ptr<GpuDevice> createGpuDevice(Backend preferred) {
    // Auto-detect best backend
    if (preferred == Backend::Auto) {
#if defined(SOLRA_GPU_METAL)
        preferred = Backend::Metal;
#elif defined(SOLRA_GPU_VULKAN)
        preferred = Backend::Vulkan;
#else
        preferred = Backend::OpenGLES;
#endif
    }

    switch (preferred) {
    case Backend::Metal:
#if defined(SOLRA_GPU_METAL)
        return createMetalDevice();
#else
        throw std::runtime_error("Metal backend not compiled into this build");
#endif
    case Backend::Vulkan:
#if defined(SOLRA_GPU_VULKAN)
        return createVulkanDevice();
#else
        throw std::runtime_error("Vulkan backend not compiled into this build");
#endif
    case Backend::OpenGLES:
#if defined(SOLRA_GPU_OPENGLES)
        return createOpenGLESDevice();
#else
        throw std::runtime_error("OpenGL ES backend not compiled into this build");
#endif
    default:
        throw std::invalid_argument("Unknown backend");
    }
}

} // namespace solra::render
