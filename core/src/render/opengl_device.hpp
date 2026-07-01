#pragma once
// OpenGL 4.6 backend implementation of GpuDevice interface
// Supports Windows (WGL), macOS (CGL), Linux (GLX)
// Also usable as OpenGL ES 3.2 fallback via compile flag

#include "gpu_abstraction.hpp"
#include <unordered_map>
#include <vector>
#include <string>
#include <mutex>

// Forward declarations for GL types
#if defined(_WIN32)
#include <windows.h>
#endif

namespace solra::render {

class OpenGLDevice : public GpuDevice {
public:
    explicit OpenGLDevice();
    ~OpenGLDevice() override;

    // GpuDevice interface
    std::string name() const override;
    Backend backend() const override;

    std::shared_ptr<GpuBuffer> createBuffer(const BufferDesc& desc, const void* data = nullptr) override;
    std::shared_ptr<GpuTexture> createTexture(const TextureDesc& desc) override;
    std::shared_ptr<GpuShader> createShader(ShaderStage stage, const std::vector<uint32_t>& spirv) override;
    std::shared_ptr<GpuPipeline> createPipeline(const PipelineDesc& desc) override;

    std::shared_ptr<GpuCommandBuffer> createCommandBuffer() override;
    void submit(std::shared_ptr<GpuCommandBuffer> cmd) override;
    void present() override;
    void waitIdle() override;

    uint64_t gpuMemoryUsed() const override;
    uint64_t gpuMemoryBudget() const override;

    // GL-specific
    bool initialize();  // creates GL context (off-screen by default)
    void shutdown();
    bool isInitialized() const { return initialized_; }

private:
    bool initialized_ = false;
    std::string renderer_name_;
    std::string vendor_name_;
    std::string gl_version_;

    // Platform-specific context handle
#if defined(_WIN32)
    HGLRC gl_context_ = nullptr;
    HDC device_context_ = nullptr;
    HWND dummy_window_ = nullptr;
#elif defined(__APPLE__)
    void* gl_context_ = nullptr;  // CGLContextObj
#else
    void* gl_context_ = nullptr;  // GLXContext
    void* display_ = nullptr;
    void* dummy_window_ = nullptr;
#endif

    // Resource tracking
    uint64_t memory_used_ = 0;
    uint64_t memory_budget_ = 4096ULL * 1024 * 1024; // 4GB default
    std::mutex resource_mutex_;

    void trackAllocation(uint64_t bytes);
    void trackDeallocation(uint64_t bytes);
};

// ============================================================
// OpenGL buffer implementation
// ============================================================
class OpenGLBuffer : public GpuBuffer {
public:
    OpenGLBuffer(uint32_t target, const BufferDesc& desc, const void* data, OpenGLDevice* device);
    ~OpenGLBuffer() override;

    void* map() override;
    void unmap() override;
    uint64_t size() const override { return size_; }

    uint32_t handle() const { return handle_; }
    uint32_t target() const { return target_; }

private:
    uint32_t handle_ = 0;
    uint32_t target_;
    uint64_t size_;
    OpenGLDevice* device_;
    bool mapped_ = false;
};

// ============================================================
// OpenGL texture implementation
// ============================================================
class OpenGLTexture : public GpuTexture {
public:
    OpenGLTexture(const TextureDesc& desc, OpenGLDevice* device);
    ~OpenGLTexture() override;

    uint32_t width() const override { return width_; }
    uint32_t height() const override { return height_; }

    uint32_t handle() const { return handle_; }
    uint32_t target() const { return target_; }
    TextureFormat format() const { return format_; }

    /** Bind this texture to a texture unit slot */
    void bind(uint32_t slot);

private:
    uint32_t handle_ = 0;
    uint32_t target_ = 0;
    uint32_t width_ = 1, height_ = 1;
    TextureFormat format_;
    OpenGLDevice* device_;
};

// ============================================================
// OpenGL shader implementation
// ============================================================
class OpenGLShader : public GpuShader {
public:
    OpenGLShader(ShaderStage stage, const std::vector<uint32_t>& spirv, OpenGLDevice* device);
    ~OpenGLShader() override;

    uint32_t handle() const { return handle_; }
    ShaderStage stage() const { return stage_; }

    // Compile GLSL source directly (bypasses SPIR-V when using source passthrough)
    static std::shared_ptr<OpenGLShader> compileFromSource(
        ShaderStage stage, const std::string& glslSource, OpenGLDevice* device);

private:
    uint32_t handle_ = 0;
    ShaderStage stage_;
    OpenGLDevice* device_;
};

// ============================================================
// OpenGL pipeline implementation
// ============================================================
class OpenGLPipeline : public GpuPipeline {
public:
    OpenGLPipeline(const PipelineDesc& desc, OpenGLDevice* device);
    ~OpenGLPipeline() override;

    uint32_t handle() const { return program_; }
    void bind();
    const PipelineDesc& desc() const { return desc_; }

private:
    uint32_t program_ = 0;
    PipelineDesc desc_;
    OpenGLDevice* device_;

    bool linkProgram(std::shared_ptr<OpenGLShader> vs, std::shared_ptr<OpenGLShader> fs);
};

// ============================================================
// OpenGL command buffer implementation
// ============================================================
class OpenGLCommandBuffer : public GpuCommandBuffer {
public:
    explicit OpenGLCommandBuffer(OpenGLDevice* device);
    ~OpenGLCommandBuffer() override;

    void begin() override;
    void end() override;
    void bindPipeline(std::shared_ptr<GpuPipeline> pipeline) override;
    void bindVertexBuffer(std::shared_ptr<GpuBuffer> buffer, uint64_t offset = 0) override;
    void bindIndexBuffer(std::shared_ptr<GpuBuffer> buffer, uint64_t offset = 0) override;
    void draw(uint32_t vertexCount, uint32_t instanceCount = 1,
              uint32_t firstVertex = 0, uint32_t firstInstance = 0) override;
    void drawIndexed(uint32_t indexCount, uint32_t instanceCount = 1,
                     uint32_t firstIndex = 0, int32_t vertexOffset = 0,
                     uint32_t firstInstance = 0) override;
    void dispatch(uint32_t groupsX, uint32_t groupsY = 1, uint32_t groupsZ = 1) override;

    // Extra: set uniforms, viewport, clear color for immediate-mode GL usage
    void setViewport(int x, int y, int w, int h);
    void setClearColor(float r, float g, float b, float a);
    void clear(bool color = true, bool depth = true, bool stencil = false);
    void setUniformMat4(int location, const float* data);
    void setUniformVec4(int location, const float* data);
    void setUniformVec3(int location, const float* data);
    void setUniformFloat(int location, float value);
    int getUniformLocation(const std::string& name);

private:
    OpenGLDevice* device_;
    std::shared_ptr<OpenGLPipeline> current_pipeline_;
    uint32_t current_vao_ = 0;
    bool recording_ = false;

    uint32_t createVAO();
};

// Factory (matches the extern declaration in gpu_abstraction.cpp)
std::shared_ptr<GpuDevice> createOpenGLESDevice();

} // namespace solra::render
