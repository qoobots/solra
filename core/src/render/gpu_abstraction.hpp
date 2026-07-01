#pragma once
// GPU Abstraction Layer: unified interface over Metal / Vulkan / OpenGL ES
#include <cstdint>
#include <string>
#include <vector>
#include <memory>
#include <functional>

namespace solra::render {

enum class Backend { Metal, Vulkan, OpenGLES, Auto };
enum class BufferUsage { Vertex, Index, Uniform, Storage, Staging };
enum class TextureFormat { RGBA8, BGRA8, Depth32F, Depth24Stencil8, BC1, BC3, ASTC4x4, ASTC8x8 };
enum class ShaderStage { Vertex, Fragment, Compute, Geometry };
enum class BlendFactor { Zero, One, SrcAlpha, OneMinusSrcAlpha, DstAlpha, OneMinusDstAlpha };
enum class CompareOp { Never, Less, Equal, LessEqual, Greater, NotEqual, GreaterEqual, Always };
enum class PrimitiveType { Points, Lines, Triangles, TriangleStrip };

// ---- Buffer ----
struct BufferDesc {
    uint64_t size = 0;
    BufferUsage usage = BufferUsage::Vertex;
    bool hostVisible = false;  // CPU-mappable for staging
};

class GpuBuffer {
public:
    virtual ~GpuBuffer() = default;
    virtual void* map() = 0;
    virtual void unmap() = 0;
    virtual uint64_t size() const = 0;
};

// ---- Texture ----
struct TextureDesc {
    uint32_t width = 1, height = 1, depth = 1, mipLevels = 1, arrayLayers = 1;
    TextureFormat format = TextureFormat::RGBA8;
};

class GpuTexture {
public:
    virtual ~GpuTexture() = default;
    virtual uint32_t width() const = 0;
    virtual uint32_t height() const = 0;
};

// ---- Shader ----
class GpuShader {
public:
    virtual ~GpuShader() = default;
};

// ---- Pipeline State ----
struct PipelineDesc {
    std::shared_ptr<GpuShader> vertexShader, fragmentShader, computeShader;
    PrimitiveType primitive = PrimitiveType::Triangles;
    bool depthTest = true, depthWrite = true;
    CompareOp depthCompare = CompareOp::LessEqual;
    bool blending = false;
    BlendFactor srcBlend = BlendFactor::SrcAlpha;
    BlendFactor dstBlend = BlendFactor::OneMinusSrcAlpha;
};

class GpuPipeline {
public:
    virtual ~GpuPipeline() = default;
};

// ---- Render Pass ----
struct ClearValue {
    float color[4] = {0, 0, 0, 1};
    float depth = 1.0f;
    uint32_t stencil = 0;
};

class GpuRenderPass {
public:
    virtual ~GpuRenderPass() = default;
};

// ---- Command Buffer ----
class GpuCommandBuffer {
public:
    virtual ~GpuCommandBuffer() = default;
    virtual void begin() = 0;
    virtual void end() = 0;
    virtual void bindPipeline(std::shared_ptr<GpuPipeline> pipeline) = 0;
    virtual void bindVertexBuffer(std::shared_ptr<GpuBuffer> buffer, uint64_t offset = 0, bool skinned = false) = 0;
    virtual void bindIndexBuffer(std::shared_ptr<GpuBuffer> buffer, uint64_t offset = 0) = 0;
    virtual void draw(uint32_t vertexCount, uint32_t instanceCount = 1,
                      uint32_t firstVertex = 0, uint32_t firstInstance = 0) = 0;
    virtual void drawIndexed(uint32_t indexCount, uint32_t instanceCount = 1,
                             uint32_t firstIndex = 0, int32_t vertexOffset = 0,
                             uint32_t firstInstance = 0) = 0;
    virtual void dispatch(uint32_t groupsX, uint32_t groupsY = 1, uint32_t groupsZ = 1) = 0;
};

// ---- Device (entry point) ----
class GpuDevice {
public:
    virtual ~GpuDevice() = default;

    virtual std::string name() const = 0;
    virtual Backend backend() const = 0;

    // Resource creation
    virtual std::shared_ptr<GpuBuffer> createBuffer(const BufferDesc& desc, const void* data = nullptr) = 0;
    virtual std::shared_ptr<GpuTexture> createTexture(const TextureDesc& desc) = 0;
    virtual std::shared_ptr<GpuShader> createShader(ShaderStage stage, const std::vector<uint32_t>& spirv) = 0;
    virtual std::shared_ptr<GpuPipeline> createPipeline(const PipelineDesc& desc) = 0;

    // Frame management
    virtual std::shared_ptr<GpuCommandBuffer> createCommandBuffer() = 0;
    virtual void submit(std::shared_ptr<GpuCommandBuffer> cmd) = 0;
    virtual void present() = 0;
    virtual void waitIdle() = 0;

    // Queries
    virtual uint64_t gpuMemoryUsed() const = 0;
    virtual uint64_t gpuMemoryBudget() const = 0;
};

// ---- Factory ----
std::shared_ptr<GpuDevice> createGpuDevice(Backend preferred = Backend::Auto);

} // namespace solra::render
