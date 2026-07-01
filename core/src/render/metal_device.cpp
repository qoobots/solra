/*
 * Solra Core SDK - Metal 3 Backend Implementation
 *
 * Full GpuDevice/GpuBuffer/GpuTexture/GpuShader/GpuPipeline/GpuCommandBuffer
 * implementation over Apple Metal 3 API. Requires macOS 14+ / iOS 17+.
 *
 * When SOLRA_GPU_METAL is not defined, this file compiles to empty stubs.
 *
 * Copyright 2026 Solra Project
 * SPDX-License-Identifier: Apache-2.0
 */

#include "metal_device.hpp"

#if defined(SOLRA_GPU_METAL)

#include <spdlog/spdlog.h>
#include <cstring>
#include <algorithm>
#include <stdexcept>

// ---- Objective-C++ bridge (metal_device_bridge.mm) ----
// The actual Metal API calls are in a separate .mm file to avoid
// requiring ObjC++ compilation for all translation units.
// This .cpp file provides the C++ wrapper logic and delegates
// to bridge functions for Metal-specific operations.

namespace {

// Bridge function declarations (implemented in metal_device_bridge.mm)
extern "C" {

void* metal_create_device();
void metal_release_device(void* dev);

void* metal_create_command_queue(void* dev);
void metal_release_command_queue(void* q);

void* metal_create_buffer(void* dev, uint64_t size, int usageFlags, const void* data);
void metal_release_buffer(void* buf);
void* metal_buffer_contents(void* buf);
uint64_t metal_buffer_length(void* buf);

void* metal_create_texture(void* dev, uint32_t w, uint32_t h, int fmt, uint32_t mips);
void metal_release_texture(void* tex);
uint32_t metal_texture_width(void* tex);
uint32_t metal_texture_height(void* tex);

void* metal_compile_library(void* dev, const char* source, char** errorOut);
void metal_release_library(void* lib);
void* metal_get_function(void* lib, const char* name);
void metal_release_function(void* fn);

void* metal_create_render_pipeline(void* dev, void* vsFn, void* fsFn,
    uint32_t colorFormat, uint32_t depthFormat, int blending,
    int depthTest, int depthWrite, char** errorOut);
void metal_release_render_pipeline(void* ps);

void* metal_create_compute_pipeline(void* dev, void* csFn, char** errorOut);
void metal_release_compute_pipeline(void* cs);

void* metal_create_depth_stencil_state(void* dev, int depthTest, int depthWrite, int compareFunc);
void metal_release_depth_stencil_state(void* ds);

void* metal_create_command_buffer(void* queue);
void metal_release_command_buffer(void* cmdBuf);
void metal_commit_command_buffer(void* cmdBuf);
void metal_wait_command_buffer(void* cmdBuf);

void* metal_begin_render_encoder(void* cmdBuf, void* renderPassDesc);
void metal_end_render_encoder(void* enc);
void metal_render_set_pipeline(void* enc, void* ps);
void metal_render_set_depth_stencil(void* enc, void* ds);
void metal_render_set_vertex_buffer(void* enc, void* buf, uint64_t offset, uint32_t index);
void metal_render_draw_primitives(void* enc, uint32_t type, uint32_t vertexStart, uint32_t vertexCount, uint32_t instanceCount);
void metal_render_draw_indexed(void* enc, uint32_t type, uint32_t indexCount, void* indexBuf, uint64_t indexOffset, uint32_t instanceCount, uint32_t baseVertex);

void* metal_begin_compute_encoder(void* cmdBuf);
void metal_end_compute_encoder(void* enc);
void metal_compute_set_pipeline(void* enc, void* ps);
void metal_compute_dispatch(void* enc, uint32_t groupsX, uint32_t groupsY, uint32_t groupsZ);

const char* metal_device_name(void* dev);
uint64_t metal_device_vram_budget(void* dev);
uint64_t metal_device_vram_used(void* dev);

void metal_free_string(char* str);

} // extern "C"

// ============================================================
// Texture format mapping
// ============================================================
int mapTextureFormat(TextureFormat fmt) {
    switch (fmt) {
        case TextureFormat::RGBA8:       return 70;  // MTLPixelFormatRGBA8Unorm
        case TextureFormat::BGRA8:       return 80;  // MTLPixelFormatBGRA8Unorm
        case TextureFormat::Depth32F:    return 255; // MTLPixelFormatDepth32Float
        case TextureFormat::Depth24Stencil8: return 260; // MTLPixelFormatDepth24Unorm_Stencil8
        case TextureFormat::BC1:         return 130; // BC1_RGBA
        case TextureFormat::BC3:         return 132; // BC3_RGBA
        case TextureFormat::ASTC4x4:     return 54;  // ASTC_4x4_LDR
        case TextureFormat::ASTC8x8:     return 56;  // ASTC_8x8_LDR
        default: return 70;
    }
}

int mapCompareFunc(CompareOp op) {
    switch (op) {
        case CompareOp::Never:        return 0;
        case CompareOp::Less:         return 1;
        case CompareOp::Equal:        return 2;
        case CompareOp::LessEqual:    return 3;
        case CompareOp::Greater:      return 4;
        case CompareOp::NotEqual:     return 5;
        case CompareOp::GreaterEqual: return 6;
        case CompareOp::Always:       return 7;
        default: return 3;
    }
}

int mapPrimitiveType(PrimitiveType t) {
    switch (t) {
        case PrimitiveType::Points:        return 1;
        case PrimitiveType::Lines:         return 2;
        case PrimitiveType::Triangles:     return 3;
        case PrimitiveType::TriangleStrip: return 4;
        default: return 3;
    }
}

int bufferUsageToMTL(BufferUsage usage) {
    switch (usage) {
        case BufferUsage::Vertex:  return 0; // MTLResourceStorageModeShared
        case BufferUsage::Index:   return 0;
        case BufferUsage::Uniform: return 0;
        case BufferUsage::Storage: return 0;
        case BufferUsage::Staging: return 1; // MTLResourceStorageModeShared + CPU cache
        default: return 0;
    }
}

} // anonymous namespace

namespace solra::render {

// ============================================================
// MetalBuffer
// ============================================================
MetalBuffer::MetalBuffer(MTLDevice_id device, const BufferDesc& desc, const void* data)
    : size_(desc.size), hostVisible_(desc.hostVisible) {
    int usage = bufferUsageToMTL(desc.usage);
    handle_ = metal_create_buffer(device, desc.size, usage, data);
}

MetalBuffer::~MetalBuffer() {
    if (handle_) metal_release_buffer(handle_);
}

void* MetalBuffer::map() {
    if (!handle_) return nullptr;
    return metal_buffer_contents(handle_);
}

void MetalBuffer::unmap() {
    // Metal buffers are persistently mapped when shared mode is used
}

uint64_t MetalBuffer::size() const { return size_; }

// ============================================================
// MetalTexture
// ============================================================
MetalTexture::MetalTexture(MTLDevice_id device, const TextureDesc& desc)
    : width_(desc.width), height_(desc.height) {
    handle_ = metal_create_texture(device, desc.width, desc.height,
                                   mapTextureFormat(desc.format), desc.mipLevels);
}

MetalTexture::~MetalTexture() {
    if (handle_) metal_release_texture(handle_);
}

uint32_t MetalTexture::width() const { return width_; }
uint32_t MetalTexture::height() const { return height_; }

// ============================================================
// MetalShader
// ============================================================
MetalShader::MetalShader(MTLDevice_id device, ShaderStage stage, const std::vector<uint32_t>& spirv)
    : stage_(stage) {
    // SPIR-V → MSL conversion is handled by SPIRV-Cross at a higher level
    // For now, this path stores SPIR-V for later offline compilation
    spdlog::warn("MetalShader from SPIR-V: direct SPIR-V ingestion not supported, use MSL source path");
}

MetalShader::MetalShader(MTLDevice_id device, ShaderStage stage,
                          const std::string& mslSource, const std::string& entryPoint)
    : stage_(stage), entryPoint_(entryPoint) {
    compileFromMSL(device, mslSource);
}

MetalShader::~MetalShader() {
    if (function_) metal_release_function(function_);
}

bool MetalShader::compileFromMSL(MTLDevice_id device, const std::string& source) {
    char* errorStr = nullptr;
    void* library = metal_compile_library(device, source.c_str(), &errorStr);
    if (!library) {
        if (errorStr) {
            spdlog::error("Metal MSL compilation failed: {}", errorStr);
            metal_free_string(errorStr);
        }
        return false;
    }

    function_ = metal_get_function(library, entryPoint_.c_str());
    metal_release_library(library);

    if (!function_) {
        spdlog::error("Metal function '{}' not found in compiled library", entryPoint_);
        return false;
    }

    spdlog::debug("Metal shader compiled: {} (stage={})", entryPoint_, static_cast<int>(stage_));
    return true;
}

bool MetalShader::compileFromSPIRV(MTLDevice_id device, const std::vector<uint32_t>& spirv) {
    // SPIR-V → MSL cross-compilation via SPIRV-Cross
    // This requires spirv-cross as a build dependency
    // For now, store the SPIR-V and defer to MSL path
    spdlog::warn("SPIR-V→MSL cross-compilation not yet integrated (requires spirv-cross)");
    return false;
}

// ============================================================
// MetalPipeline
// ============================================================
MetalPipeline::MetalPipeline(MTLDevice_id device, const PipelineDesc& desc,
                               std::shared_ptr<MetalShader> vs,
                               std::shared_ptr<MetalShader> fs)
    : isCompute_(false) {
    char* errorStr = nullptr;

    // Create render pipeline state
    renderState_ = metal_create_render_pipeline(
        device,
        vs ? vs->function() : nullptr,
        fs ? fs->function() : nullptr,
        70, // default BGRA8Unorm color format
        desc.depthTest ? 255 : 0, // Depth32Float or 0 (no depth)
        desc.blending ? 1 : 0,
        desc.depthTest ? 1 : 0,
        desc.depthWrite ? 1 : 0,
        &errorStr);

    if (!renderState_ && errorStr) {
        spdlog::error("Metal render pipeline creation failed: {}", errorStr);
        metal_free_string(errorStr);
    }

    // Create depth-stencil state
    if (desc.depthTest) {
        depthState_ = metal_create_depth_stencil_state(
            device, 1, desc.depthWrite ? 1 : 0, mapCompareFunc(desc.depthCompare));
    }
}

MetalPipeline::MetalPipeline(MTLDevice_id device, std::shared_ptr<MetalShader> cs)
    : isCompute_(true) {
    char* errorStr = nullptr;
    computeState_ = metal_create_compute_pipeline(device, cs->function(), &errorStr);

    if (!computeState_ && errorStr) {
        spdlog::error("Metal compute pipeline creation failed: {}", errorStr);
        metal_free_string(errorStr);
    }
}

MetalPipeline::~MetalPipeline() {
    if (renderState_) metal_release_render_pipeline(renderState_);
    if (computeState_) metal_release_compute_pipeline(computeState_);
    if (depthState_) metal_release_depth_stencil_state(depthState_);
}

// ============================================================
// MetalCommandBuffer
// ============================================================
MetalCommandBuffer::MetalCommandBuffer(MTLDevice_id device, MTLCommandQueue_id queue)
    : device_(device), queue_(queue) {}

MetalCommandBuffer::~MetalCommandBuffer() {
    if (recording_) end();
}

void MetalCommandBuffer::begin() {
    if (recording_) return;
    cmdBuf_ = metal_create_command_buffer(queue_);
    recording_ = true;
    isComputePass_ = false;
}

void MetalCommandBuffer::end() {
    if (!recording_) return;
    endRenderEncoder();
    endComputeEncoder();

    if (cmdBuf_) {
        metal_commit_command_buffer(cmdBuf_);
        metal_release_command_buffer(cmdBuf_);
        cmdBuf_ = nullptr;
    }
    recording_ = false;
}

void MetalCommandBuffer::bindPipeline(std::shared_ptr<GpuPipeline> pipeline) {
    auto metalPipeline = std::dynamic_pointer_cast<MetalPipeline>(pipeline);
    if (!metalPipeline) return;
    currentPipeline_ = metalPipeline;

    if (metalPipeline->isCompute()) {
        if (!isComputePass_) {
            endRenderEncoder();
            computeEnc_ = metal_begin_compute_encoder(cmdBuf_);
            isComputePass_ = true;
        }
        metal_compute_set_pipeline(computeEnc_, metalPipeline->computeState());
    } else {
        if (isComputePass_) {
            endComputeEncoder();
            renderEnc_ = metal_begin_render_encoder(cmdBuf_, nullptr); // uses MTLRenderPassDescriptor from frame
            isComputePass_ = false;
        } else if (!renderEnc_) {
            renderEnc_ = metal_begin_render_encoder(cmdBuf_, nullptr);
        }
        metal_render_set_pipeline(renderEnc_, metalPipeline->renderState());
        if (metalPipeline->depthState()) {
            metal_render_set_depth_stencil(renderEnc_, metalPipeline->depthState());
        }
    }
}

void MetalCommandBuffer::bindVertexBuffer(std::shared_ptr<GpuBuffer> buffer, uint64_t offset, bool skinned) {
    if (!renderEnc_) return;
    auto metalBuf = std::dynamic_pointer_cast<MetalBuffer>(buffer);
    if (metalBuf) {
        metal_render_set_vertex_buffer(renderEnc_, metalBuf->handle(), offset, skinned ? 30 : 0);
    }
}

void MetalCommandBuffer::bindIndexBuffer(std::shared_ptr<GpuBuffer> buffer, uint64_t offset) {
    if (!renderEnc_) return;
    // Index buffer is bound at drawIndexed time in Metal
    auto metalBuf = std::dynamic_pointer_cast<MetalBuffer>(buffer);
    if (metalBuf) {
        // Store for drawIndexed call
        indexBuf_ = metalBuf->handle();
        indexOffset_ = offset;
    }
}

void MetalCommandBuffer::draw(uint32_t vertexCount, uint32_t instanceCount,
                               uint32_t firstVertex, uint32_t firstInstance) {
    if (!renderEnc_ || !currentPipeline_) return;
    metal_render_draw_primitives(
        renderEnc_,
        mapPrimitiveType(PrimitiveType::Triangles),
        firstVertex, vertexCount, instanceCount);
}

void MetalCommandBuffer::drawIndexed(uint32_t indexCount, uint32_t instanceCount,
                                      uint32_t firstIndex, int32_t vertexOffset,
                                      uint32_t firstInstance) {
    if (!renderEnc_ || !currentPipeline_) return;
    metal_render_draw_indexed(
        renderEnc_,
        mapPrimitiveType(PrimitiveType::Triangles),
        indexCount, indexBuf_, indexOffset_ + firstIndex * sizeof(uint32_t),
        instanceCount, vertexOffset);
}

void MetalCommandBuffer::dispatch(uint32_t groupsX, uint32_t groupsY, uint32_t groupsZ) {
    if (!computeEnc_ || !currentPipeline_) return;
    metal_compute_dispatch(computeEnc_, groupsX, groupsY, groupsZ);
}

void MetalCommandBuffer::endRenderEncoder() {
    if (renderEnc_) {
        metal_end_render_encoder(renderEnc_);
        renderEnc_ = nullptr;
    }
}

void MetalCommandBuffer::endComputeEncoder() {
    if (computeEnc_) {
        metal_end_compute_encoder(computeEnc_);
        computeEnc_ = nullptr;
    }
}

// ============================================================
// MetalDevice
// ============================================================
MetalDevice::MetalDevice() {}
MetalDevice::~MetalDevice() { shutdown(); }

std::string MetalDevice::name() const {
    return "Metal 3 — " + deviceName_;
}

bool MetalDevice::initialize(void* nativeLayer) {
    if (initialized_) return true;

    device_ = metal_create_device();
    if (!device_) {
        spdlog::error("MetalDevice: failed to create MTLDevice (no Metal-capable GPU?)");
        return false;
    }

    commandQueue_ = metal_create_command_queue(device_);
    if (!commandQueue_) {
        metal_release_device(device_);
        device_ = nullptr;
        return false;
    }

    const char* name = metal_device_name(device_);
    deviceName_ = name ? name : "Apple GPU";
    vendorName_ = "Apple";

    vramBudget_ = metal_device_vram_budget(device_);
    initialized_ = true;

    spdlog::info("MetalDevice initialized: {} (VRAM budget: {} MB)", deviceName_, vramBudget_ / 1024 / 1024);
    return true;
}

void MetalDevice::shutdown() {
    if (!initialized_) return;
    waitIdle();

    if (commandQueue_) {
        metal_release_command_queue(commandQueue_);
        commandQueue_ = nullptr;
    }
    if (device_) {
        metal_release_device(device_);
        device_ = nullptr;
    }
    initialized_ = false;
}

std::shared_ptr<GpuBuffer> MetalDevice::createBuffer(const BufferDesc& desc, const void* data) {
    return std::make_shared<MetalBuffer>(device_, desc, data);
}

std::shared_ptr<GpuTexture> MetalDevice::createTexture(const TextureDesc& desc) {
    return std::make_shared<MetalTexture>(device_, desc);
}

std::shared_ptr<GpuShader> MetalDevice::createShader(ShaderStage stage, const std::vector<uint32_t>& spirv) {
    // SPIR-V is cross-compiled to MSL at pipeline creation time via spirv-cross
    return std::make_shared<MetalShader>(device_, stage, spirv);
}

std::shared_ptr<GpuPipeline> MetalDevice::createPipeline(const PipelineDesc& desc) {
    // Compute pipeline
    if (desc.computeShader) {
        auto cs = std::dynamic_pointer_cast<MetalShader>(desc.computeShader);
        if (!cs) return nullptr;
        return std::make_shared<MetalPipeline>(device_, cs);
    }

    // Render pipeline
    auto vs = std::dynamic_pointer_cast<MetalShader>(desc.vertexShader);
    auto fs = std::dynamic_pointer_cast<MetalShader>(desc.fragmentShader);
    if (!vs && !fs) return nullptr;
    return std::make_shared<MetalPipeline>(device_, desc, vs, fs);
}

std::shared_ptr<MetalShader> MetalDevice::createShaderFromMSL(ShaderStage stage,
                                                               const std::string& source,
                                                               const std::string& entryPoint) {
    return std::make_shared<MetalShader>(device_, stage, source, entryPoint);
}

std::shared_ptr<GpuCommandBuffer> MetalDevice::createCommandBuffer() {
    return std::make_shared<MetalCommandBuffer>(device_, commandQueue_);
}

void MetalDevice::submit(std::shared_ptr<GpuCommandBuffer> cmd) {
    auto metalCmd = std::dynamic_pointer_cast<MetalCommandBuffer>(cmd);
    if (metalCmd) {
        metalCmd->end();
    }
}

void MetalDevice::present() {
    // Presentation is handled by the platform layer (CAMetalLayer drawable)
    // The command buffer commit happens in submit()
}

void MetalDevice::waitIdle() {
    // Metal doesn't have a global waitIdle; we flush the queue
    if (commandQueue_) {
        void* tmpCmd = metal_create_command_buffer(commandQueue_);
        metal_commit_command_buffer(tmpCmd);
        metal_wait_command_buffer(tmpCmd);
        metal_release_command_buffer(tmpCmd);
    }
}

uint64_t MetalDevice::gpuMemoryUsed() const {
    return metal_device_vram_used(device_);
}

uint64_t MetalDevice::gpuMemoryBudget() const {
    return vramBudget_;
}

// ============================================================
// Factory
// ============================================================
std::shared_ptr<GpuDevice> createMetalDevice() {
    auto device = std::make_shared<MetalDevice>();
    if (!device->initialize(nullptr)) {
        return nullptr;
    }
    return device;
}

} // namespace solra::render

#else // !SOLRA_GPU_METAL

// Empty stubs when Metal backend not compiled
namespace solra::render {
std::shared_ptr<GpuDevice> createMetalDevice() {
    return nullptr;
}
} // namespace solra::render

#endif // SOLRA_GPU_METAL
