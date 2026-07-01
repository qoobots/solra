/*
 * Solra Core SDK - Metal 3 Backend Header
 *
 * Apple Metal 3 rendering backend for macOS 14+ / iOS 17+ / visionOS 1+.
 * Implements GpuDevice/GpuBuffer/GpuTexture/GpuShader/GpuPipeline/GpuCommandBuffer
 * over the Metal C++ API (metal-cpp).
 *
 * Copyright 2026 Solra Project
 * SPDX-License-Identifier: Apache-2.0
 */

#pragma once

#include "gpu_abstraction.hpp"
#include <memory>
#include <string>
#include <vector>
#include <unordered_map>

#if defined(SOLRA_GPU_METAL)

// Forward declarations (Metal C++ API types via opaque pointers when metal-cpp not available)
#ifdef __OBJC__
#import <Metal/Metal.h>
#import <MetalKit/MetalKit.h>
#else
// Opaque types for non-ObjC compilation units
using MTLDevice_id   = void*;
using MTLBuffer_id   = void*;
using MTLTexture_id  = void*;
using MTLLibrary_id  = void*;
using MTLFunction_id = void*;
using MTLRenderPipelineState_id = void*;
using MTLComputePipelineState_id = void*;
using MTLCommandQueue_id = void*;
using MTLCommandBuffer_id = void*;
using MTLRenderCommandEncoder_id = void*;
using MTLComputeCommandEncoder_id = void*;
using MTLDepthStencilState_id = void*;
using MTLSamplerState_id = void*;
#endif

namespace solra::render {

// ============================================================
// MetalBuffer
// ============================================================
class MetalBuffer : public GpuBuffer {
public:
    MetalBuffer(MTLDevice_id device, const BufferDesc& desc, const void* data);
    ~MetalBuffer() override;

    void* map() override;
    void unmap() override;
    uint64_t size() const override;

    MTLBuffer_id handle() const { return handle_; }

private:
    MTLBuffer_id handle_ = nullptr;
    uint64_t size_ = 0;
    bool hostVisible_ = false;
};

// ============================================================
// MetalTexture
// ============================================================
class MetalTexture : public GpuTexture {
public:
    MetalTexture(MTLDevice_id device, const TextureDesc& desc);
    ~MetalTexture() override;

    uint32_t width() const override;
    uint32_t height() const override;

    MTLTexture_id handle() const { return handle_; }

private:
    MTLTexture_id handle_ = nullptr;
    uint32_t width_ = 0, height_ = 0;
};

// ============================================================
// MetalShader
// ============================================================
class MetalShader : public GpuShader {
public:
    MetalShader(MTLDevice_id device, ShaderStage stage, const std::vector<uint32_t>& spirv);
    MetalShader(MTLDevice_id device, ShaderStage stage, const std::string& mslSource, const std::string& entryPoint);
    ~MetalShader() override;

    ShaderStage stage() const { return stage_; }
    MTLFunction_id function() const { return function_; }
    const std::string& entryPoint() const { return entryPoint_; }

private:
    ShaderStage stage_;
    MTLFunction_id function_ = nullptr;
    std::string entryPoint_ = "main0";
    bool compileFromMSL(MTLDevice_id device, const std::string& source);
    bool compileFromSPIRV(MTLDevice_id device, const std::vector<uint32_t>& spirv);
};

// ============================================================
// MetalPipeline
// ============================================================
class MetalPipeline : public GpuPipeline {
public:
    MetalPipeline(MTLDevice_id device, const PipelineDesc& desc,
                  std::shared_ptr<MetalShader> vs,
                  std::shared_ptr<MetalShader> fs);
    MetalPipeline(MTLDevice_id device, std::shared_ptr<MetalShader> cs);
    ~MetalPipeline() override;

    bool isCompute() const { return isCompute_; }
    MTLRenderPipelineState_id renderState() const { return renderState_; }
    MTLComputePipelineState_id computeState() const { return computeState_; }
    MTLDepthStencilState_id depthState() const { return depthState_; }

private:
    bool isCompute_ = false;
    MTLRenderPipelineState_id renderState_ = nullptr;
    MTLComputePipelineState_id computeState_ = nullptr;
    MTLDepthStencilState_id depthState_ = nullptr;
};

// ============================================================
// MetalCommandBuffer
// ============================================================
class MetalCommandBuffer : public GpuCommandBuffer {
public:
    explicit MetalCommandBuffer(MTLDevice_id device, MTLCommandQueue_id queue);
    ~MetalCommandBuffer() override;

    void begin() override;
    void end() override;
    void bindPipeline(std::shared_ptr<GpuPipeline> pipeline) override;
    void bindVertexBuffer(std::shared_ptr<GpuBuffer> buffer, uint64_t offset = 0, bool skinned = false) override;
    void bindIndexBuffer(std::shared_ptr<GpuBuffer> buffer, uint64_t offset = 0) override;
    void draw(uint32_t vertexCount, uint32_t instanceCount = 1,
              uint32_t firstVertex = 0, uint32_t firstInstance = 0) override;
    void drawIndexed(uint32_t indexCount, uint32_t instanceCount = 1,
                     uint32_t firstIndex = 0, int32_t vertexOffset = 0,
                     uint32_t firstInstance = 0) override;
    void dispatch(uint32_t groupsX, uint32_t groupsY = 1, uint32_t groupsZ = 1) override;

private:
    MTLDevice_id device_ = nullptr;
    MTLCommandQueue_id queue_ = nullptr;
    MTLCommandBuffer_id cmdBuf_ = nullptr;
    MTLRenderCommandEncoder_id renderEnc_ = nullptr;
    MTLComputeCommandEncoder_id computeEnc_ = nullptr;
    std::shared_ptr<MetalPipeline> currentPipeline_;
    bool recording_ = false;
    bool isComputePass_ = false;

    void endRenderEncoder();
    void endComputeEncoder();
};

// ============================================================
// MetalDevice
// ============================================================
class MetalDevice : public GpuDevice {
public:
    MetalDevice();
    ~MetalDevice() override;

    std::string name() const override;
    Backend backend() const override { return Backend::Metal; }

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

    // Metal-specific
    bool initialize(void* nativeLayer); // CAMetalLayer*
    void shutdown();

    MTLDevice_id mtlDevice() const { return device_; }

    // Compile MSL shader from source (used by ShaderCompiler)
    std::shared_ptr<MetalShader> createShaderFromMSL(ShaderStage stage,
                                                      const std::string& source,
                                                      const std::string& entryPoint);

private:
    MTLDevice_id device_ = nullptr;
    MTLCommandQueue_id commandQueue_ = nullptr;
    std::string deviceName_;
    std::string vendorName_;
    uint64_t vramBudget_ = 0;
    bool initialized_ = false;
};

// Factory
std::shared_ptr<GpuDevice> createMetalDevice();

} // namespace solra::render

#endif // SOLRA_GPU_METAL
