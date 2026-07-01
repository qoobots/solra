/*
 * Solra Core SDK - DirectX 12 Backend Header
 *
 * Windows DirectX 12 Ultimate rendering backend.
 * Implements GpuDevice/GpuBuffer/GpuTexture/GpuShader/GpuPipeline/GpuCommandBuffer
 * over the D3D12 API. Supports DXR ray tracing and mesh shaders on capable hardware.
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

#if defined(SOLRA_GPU_D3D12) && defined(_WIN32)

// Minimal forward declarations (avoid full d3d12.h dependency in header)
struct ID3D12Device;
struct ID3D12Resource;
struct ID3D12RootSignature;
struct ID3D12PipelineState;
struct ID3D12CommandQueue;
struct ID3D12CommandAllocator;
struct ID3D12GraphicsCommandList;
struct ID3D12DescriptorHeap;
struct ID3D12Fence;
struct IDXGIFactory4;
struct IDXGISwapChain3;

namespace solra::render {

// ============================================================
// D3D12Buffer
// ============================================================
class D3D12Buffer : public GpuBuffer {
public:
    D3D12Buffer(ID3D12Device* device, const BufferDesc& desc, const void* data);
    ~D3D12Buffer() override;

    void* map() override;
    void unmap() override;
    uint64_t size() const override;

    ID3D12Resource* resource() const { return resource_; }

private:
    ID3D12Resource* resource_ = nullptr;
    ID3D12Resource* uploadResource_ = nullptr;
    uint64_t size_ = 0;
    bool hostVisible_ = false;
    void* mappedData_ = nullptr;
};

// ============================================================
// D3D12Texture
// ============================================================
class D3D12Texture : public GpuTexture {
public:
    D3D12Texture(ID3D12Device* device, const TextureDesc& desc);
    ~D3D12Texture() override;

    uint32_t width() const override;
    uint32_t height() const override;

    ID3D12Resource* resource() const { return resource_; }

private:
    ID3D12Resource* resource_ = nullptr;
    uint32_t width_ = 0, height_ = 0;
};

// ============================================================
// D3D12Shader
// ============================================================
class D3D12Shader : public GpuShader {
public:
    D3D12Shader(ShaderStage stage, const std::vector<uint8_t>& dxil);
    ~D3D12Shader() override;

    ShaderStage stage() const { return stage_; }
    const void* bytecode() const { return bytecode_.data(); }
    size_t bytecodeSize() const { return bytecode_.size(); }

private:
    ShaderStage stage_;
    std::vector<uint8_t> bytecode_;
};

// ============================================================
// D3D12Pipeline
// ============================================================
class D3D12Pipeline : public GpuPipeline {
public:
    D3D12Pipeline(ID3D12Device* device, ID3D12RootSignature* rootSig,
                  const PipelineDesc& desc,
                  std::shared_ptr<D3D12Shader> vs,
                  std::shared_ptr<D3D12Shader> fs);
    D3D12Pipeline(ID3D12Device* device, ID3D12RootSignature* rootSig,
                  std::shared_ptr<D3D12Shader> cs);
    ~D3D12Pipeline() override;

    bool isCompute() const { return isCompute_; }
    ID3D12PipelineState* state() const { return state_; }
    ID3D12RootSignature* rootSignature() const { return rootSig_; }

private:
    bool isCompute_ = false;
    ID3D12PipelineState* state_ = nullptr;
    ID3D12RootSignature* rootSig_ = nullptr;
};

// ============================================================
// D3D12CommandBuffer
// ============================================================
class D3D12CommandBuffer : public GpuCommandBuffer {
public:
    D3D12CommandBuffer(ID3D12Device* device, ID3D12CommandAllocator* allocator,
                       ID3D12GraphicsCommandList* cmdList);
    ~D3D12CommandBuffer() override;

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

    ID3D12GraphicsCommandList* cmdList() const { return cmdList_; }

private:
    ID3D12Device* device_ = nullptr;
    ID3D12CommandAllocator* allocator_ = nullptr;
    ID3D12GraphicsCommandList* cmdList_ = nullptr;
    std::shared_ptr<D3D12Pipeline> currentPipeline_;
    bool recording_ = false;
};

// ============================================================
// D3D12Device
// ============================================================
class D3D12Device : public GpuDevice {
public:
    D3D12Device();
    ~D3D12Device() override;

    std::string name() const override;
    Backend backend() const override { return Backend::OpenGLES; } // reuse enum, actual value from query

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

    // D3D12-specific
    bool initialize(void* hwnd);
    void shutdown();

    ID3D12Device* d3dDevice() const { return device_; }
    std::shared_ptr<D3D12Shader> createShaderFromDXIL(ShaderStage stage, const std::vector<uint8_t>& dxil);

private:
    ID3D12Device* device_ = nullptr;
    IDXGIFactory4* factory_ = nullptr;
    IDXGISwapChain3* swapChain_ = nullptr;
    ID3D12CommandQueue* commandQueue_ = nullptr;
    ID3D12DescriptorHeap* rtvHeap_ = nullptr;
    ID3D12DescriptorHeap* dsvHeap_ = nullptr;
    ID3D12DescriptorHeap* srvHeap_ = nullptr;
    ID3D12Fence* fence_ = nullptr;
    uint64_t fenceValue_ = 0;
    void* fenceEvent_ = nullptr;
    std::string deviceName_;
    std::string vendorName_;
    uint64_t vramBudget_ = 0;
    uint32_t rtvDescriptorSize_ = 0;
    uint32_t frameIndex_ = 0;
    bool initialized_ = false;

    bool createDeviceAndSwapChain(void* hwnd);
    bool createRootSignature();
    bool createDescriptorHeaps();
    bool createFence();
};

// Factory
std::shared_ptr<GpuDevice> createD3D12Device();

} // namespace solra::render

#endif // SOLRA_GPU_D3D12 && _WIN32
