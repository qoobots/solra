/*
 * Solra Core SDK - DirectX 12 Backend Implementation
 *
 * Full GpuDevice/GpuBuffer/GpuTexture/GpuShader/GpuPipeline/GpuCommandBuffer
 * implementation over D3D12 Ultimate API. Supports Windows 10+.
 *
 * When SOLRA_GPU_D3D12 is not defined, this file compiles to empty stubs.
 *
 * Copyright 2026 Solra Project
 * SPDX-License-Identifier: Apache-2.0
 */

#include "d3d12_device.hpp"

#if defined(SOLRA_GPU_D3D12) && defined(_WIN32)

// We use a minimal D3D12 include approach — full headers only in the bridge
#include <spdlog/spdlog.h>
#include <cstring>
#include <algorithm>
#include <stdexcept>

// D3D12 bridge functions (implemented in d3d12_device_bridge.cpp)
// This avoids pulling in the full D3D12 headers into all translation units

namespace {

extern "C" {

// Device & swap chain
void* d3d12_create_factory();
void  d3d12_release_factory(void* factory);
void* d3d12_create_device(void* factory, int adapterIndex, char** errorOut);
void  d3d12_release_device(void* dev);
void* d3d12_create_command_queue(void* dev);
void  d3d12_release_command_queue(void* q);
void* d3d12_create_swap_chain(void* factory, void* queue, void* hwnd, uint32_t w, uint32_t h, uint32_t bufferCount);
void  d3d12_release_swap_chain(void* sc);

// Device info
const char* d3d12_device_name(void* dev);
uint64_t    d3d12_device_vram_budget(void* dev);
uint64_t    d3d12_device_vram_used(void* dev);

// Buffer
void* d3d12_create_buffer(void* dev, uint64_t size, int isUpload, const void* data);
void  d3d12_release_buffer(void* buf);
void* d3d12_buffer_map(void* buf);
void  d3d12_buffer_unmap(void* buf);
uint64_t d3d12_buffer_size(void* buf);

// Texture
void* d3d12_create_texture(void* dev, uint32_t w, uint32_t h, int fmt, uint32_t mips);
void  d3d12_release_texture(void* tex);
void  d3d12_texture_get_size(void* tex, uint32_t* w, uint32_t* h);

// Root signature
void* d3d12_create_root_signature(void* dev);
void  d3d12_release_root_signature(void* rs);

// Pipeline states
void* d3d12_create_graphics_pso(void* dev, void* rootSig,
    const void* vsBytecode, size_t vsSize,
    const void* fsBytecode, size_t fsSize,
    int depthTest, int depthWrite, int blending, char** errorOut);
void* d3d12_create_compute_pso(void* dev, void* rootSig,
    const void* csBytecode, size_t csSize, char** errorOut);
void  d3d12_release_pso(void* pso);

// Descriptor heaps
void* d3d12_create_rtv_heap(void* dev, uint32_t count);
void* d3d12_create_dsv_heap(void* dev, uint32_t count);
void* d3d12_create_srv_heap(void* dev, uint32_t count);
void  d3d12_release_descriptor_heap(void* heap);
uint32_t d3d12_rtv_descriptor_size(void* dev);

// Command list
void* d3d12_create_command_allocator(void* dev);
void  d3d12_release_command_allocator(void* alloc);
void* d3d12_create_command_list(void* dev, void* allocator);
void  d3d12_release_command_list(void* list);
void  d3d12_reset_command_list(void* list, void* allocator);
void  d3d12_close_command_list(void* list);

// Command recording
void  d3d12_cmd_set_pipeline(void* list, void* pso);
void  d3d12_cmd_set_root_signature(void* list, void* rs);
void  d3d12_cmd_set_vertex_buffer(void* list, void* buf, uint64_t offset, uint32_t slot);
void  d3d12_cmd_set_index_buffer(void* list, void* buf, uint64_t offset);
void  d3d12_cmd_draw(void* list, uint32_t vertexCount, uint32_t instanceCount, uint32_t firstVertex, uint32_t firstInstance);
void  d3d12_cmd_draw_indexed(void* list, uint32_t indexCount, uint32_t instanceCount, uint32_t firstIndex, int32_t vertexOffset, uint32_t firstInstance);
void  d3d12_cmd_dispatch(void* list, uint32_t groupsX, uint32_t groupsY, uint32_t groupsZ);

// Submission
void  d3d12_execute_command_lists(void* queue, void** lists, uint32_t count);
void  d3d12_signal_fence(void* queue, void* fence, uint64_t value);
void  d3d12_wait_fence(void* fence, uint64_t value, void* event);
void  d3d12_present(void* swapChain, uint32_t syncInterval);

// Fence
void* d3d12_create_fence(void* dev);
void  d3d12_release_fence(void* fence);

void d3d12_free_string(char* str);

} // extern "C"

// ============================================================
// Texture format mapping
// ============================================================
int mapTextureFormat(TextureFormat fmt) {
    switch (fmt) {
        case TextureFormat::RGBA8:           return 28; // DXGI_FORMAT_R8G8B8A8_UNORM
        case TextureFormat::BGRA8:           return 87; // DXGI_FORMAT_B8G8R8A8_UNORM
        case TextureFormat::Depth32F:        return 40; // DXGI_FORMAT_D32_FLOAT
        case TextureFormat::Depth24Stencil8: return 45; // DXGI_FORMAT_D24_UNORM_S8_UINT
        case TextureFormat::BC1:             return 71; // DXGI_FORMAT_BC1_UNORM
        case TextureFormat::BC3:             return 77; // DXGI_FORMAT_BC3_UNORM
        case TextureFormat::ASTC4x4:         return 134; // DXGI_FORMAT_ASTC_4X4_UNORM
        case TextureFormat::ASTC8x8:         return 140; // DXGI_FORMAT_ASTC_8X8_UNORM
        default: return 28;
    }
}

// ============================================================
// SPIR-V → DXIL conversion (delegated to DXC at pipeline creation)
// ============================================================

} // anonymous namespace

namespace solra::render {

// ============================================================
// D3D12Buffer
// ============================================================
D3D12Buffer::D3D12Buffer(ID3D12Device* device, const BufferDesc& desc, const void* data)
    : size_(desc.size), hostVisible_(desc.hostVisible) {
    if (desc.hostVisible) {
        uploadResource_ = (ID3D12Resource*)d3d12_create_buffer(device, desc.size, 1, data);
        if (uploadResource_) {
            mappedData_ = d3d12_buffer_map(uploadResource_);
        }
    } else {
        resource_ = (ID3D12Resource*)d3d12_create_buffer(device, desc.size, 0, data);
    }
}

D3D12Buffer::~D3D12Buffer() {
    if (uploadResource_) {
        if (mappedData_) d3d12_buffer_unmap(uploadResource_);
        d3d12_release_buffer(uploadResource_);
    }
    if (resource_) d3d12_release_buffer(resource_);
}

void* D3D12Buffer::map() {
    if (uploadResource_) return mappedData_;
    if (resource_) return d3d12_buffer_map(resource_);
    return nullptr;
}

void D3D12Buffer::unmap() {
    if (uploadResource_) return; // persistently mapped
    if (resource_) d3d12_buffer_unmap(resource_);
}

uint64_t D3D12Buffer::size() const { return size_; }

// ============================================================
// D3D12Texture
// ============================================================
D3D12Texture::D3D12Texture(ID3D12Device* device, const TextureDesc& desc)
    : width_(desc.width), height_(desc.height) {
    resource_ = (ID3D12Resource*)d3d12_create_texture(
        device, desc.width, desc.height,
        mapTextureFormat(desc.format), desc.mipLevels);
}

D3D12Texture::~D3D12Texture() {
    if (resource_) d3d12_release_texture(resource_);
}

uint32_t D3D12Texture::width() const { return width_; }
uint32_t D3D12Texture::height() const { return height_; }

// ============================================================
// D3D12Shader
// ============================================================
D3D12Shader::D3D12Shader(ShaderStage stage, const std::vector<uint8_t>& dxil)
    : stage_(stage), bytecode_(dxil) {}

D3D12Shader::~D3D12Shader() {}

// ============================================================
// D3D12Pipeline
// ============================================================
D3D12Pipeline::D3D12Pipeline(ID3D12Device* device, ID3D12RootSignature* rootSig,
                               const PipelineDesc& desc,
                               std::shared_ptr<D3D12Shader> vs,
                               std::shared_ptr<D3D12Shader> fs)
    : isCompute_(false), rootSig_(rootSig) {
    char* errorStr = nullptr;
    state_ = (ID3D12PipelineState*)d3d12_create_graphics_pso(
        device, rootSig,
        vs ? vs->bytecode() : nullptr, vs ? vs->bytecodeSize() : 0,
        fs ? fs->bytecode() : nullptr, fs ? fs->bytecodeSize() : 0,
        desc.depthTest ? 1 : 0, desc.depthWrite ? 1 : 0,
        desc.blending ? 1 : 0, &errorStr);

    if (!state_ && errorStr) {
        spdlog::error("D3D12 graphics PSO creation failed: {}", errorStr);
        d3d12_free_string(errorStr);
    }
}

D3D12Pipeline::D3D12Pipeline(ID3D12Device* device, ID3D12RootSignature* rootSig,
                               std::shared_ptr<D3D12Shader> cs)
    : isCompute_(true), rootSig_(rootSig) {
    char* errorStr = nullptr;
    state_ = (ID3D12PipelineState*)d3d12_create_compute_pso(
        device, rootSig,
        cs ? cs->bytecode() : nullptr, cs ? cs->bytecodeSize() : 0,
        &errorStr);

    if (!state_ && errorStr) {
        spdlog::error("D3D12 compute PSO creation failed: {}", errorStr);
        d3d12_free_string(errorStr);
    }
}

D3D12Pipeline::~D3D12Pipeline() {
    if (state_) d3d12_release_pso(state_);
}

// ============================================================
// D3D12CommandBuffer
// ============================================================
D3D12CommandBuffer::D3D12CommandBuffer(ID3D12Device* device,
                                         ID3D12CommandAllocator* allocator,
                                         ID3D12GraphicsCommandList* cmdList)
    : device_(device), allocator_(allocator), cmdList_(cmdList) {}

D3D12CommandBuffer::~D3D12CommandBuffer() {
    if (recording_) end();
}

void D3D12CommandBuffer::begin() {
    if (recording_) return;
    d3d12_reset_command_list(cmdList_, allocator_);
    recording_ = true;
}

void D3D12CommandBuffer::end() {
    if (!recording_) return;
    d3d12_close_command_list(cmdList_);
    recording_ = false;
}

void D3D12CommandBuffer::bindPipeline(std::shared_ptr<GpuPipeline> pipeline) {
    auto d3dPipeline = std::dynamic_pointer_cast<D3D12Pipeline>(pipeline);
    if (!d3dPipeline) return;
    currentPipeline_ = d3dPipeline;

    d3d12_cmd_set_pipeline(cmdList_, d3dPipeline->state());
    d3d12_cmd_set_root_signature(cmdList_, d3dPipeline->rootSignature());
}

void D3D12CommandBuffer::bindVertexBuffer(std::shared_ptr<GpuBuffer> buffer, uint64_t offset, bool skinned) {
    auto d3dBuf = std::dynamic_pointer_cast<D3D12Buffer>(buffer);
    if (d3dBuf && d3dBuf->resource()) {
        d3d12_cmd_set_vertex_buffer(cmdList_, d3dBuf->resource(), offset, skinned ? 1 : 0);
    }
}

void D3D12CommandBuffer::bindIndexBuffer(std::shared_ptr<GpuBuffer> buffer, uint64_t offset) {
    auto d3dBuf = std::dynamic_pointer_cast<D3D12Buffer>(buffer);
    if (d3dBuf && d3dBuf->resource()) {
        d3d12_cmd_set_index_buffer(cmdList_, d3dBuf->resource(), offset);
    }
}

void D3D12CommandBuffer::draw(uint32_t vertexCount, uint32_t instanceCount,
                               uint32_t firstVertex, uint32_t firstInstance) {
    d3d12_cmd_draw(cmdList_, vertexCount, instanceCount, firstVertex, firstInstance);
}

void D3D12CommandBuffer::drawIndexed(uint32_t indexCount, uint32_t instanceCount,
                                      uint32_t firstIndex, int32_t vertexOffset,
                                      uint32_t firstInstance) {
    d3d12_cmd_draw_indexed(cmdList_, indexCount, instanceCount,
                           firstIndex, vertexOffset, firstInstance);
}

void D3D12CommandBuffer::dispatch(uint32_t groupsX, uint32_t groupsY, uint32_t groupsZ) {
    d3d12_cmd_dispatch(cmdList_, groupsX, groupsY, groupsZ);
}

// ============================================================
// D3D12Device
// ============================================================
D3D12Device::D3D12Device() {}
D3D12Device::~D3D12Device() { shutdown(); }

std::string D3D12Device::name() const {
    return "DirectX 12 Ultimate — " + deviceName_;
}

bool D3D12Device::initialize(void* hwnd) {
    if (initialized_) return true;

    factory_ = (IDXGIFactory4*)d3d12_create_factory();
    if (!factory_) {
        spdlog::error("D3D12Device: failed to create DXGI factory");
        return false;
    }

    char* errorStr = nullptr;
    device_ = (ID3D12Device*)d3d12_create_device(factory_, 0, &errorStr);
    if (!device_) {
        if (errorStr) {
            spdlog::error("D3D12Device: {}", errorStr);
            d3d12_free_string(errorStr);
        }
        d3d12_release_factory(factory_);
        factory_ = nullptr;
        return false;
    }

    const char* name = d3d12_device_name(device_);
    deviceName_ = name ? name : "D3D12 GPU";
    vendorName_ = "Microsoft";
    vramBudget_ = d3d12_device_vram_budget(device_);

    commandQueue_ = (ID3D12CommandQueue*)d3d12_create_command_queue(device_);
    if (!commandQueue_) {
        spdlog::error("D3D12Device: failed to create command queue");
        shutdown();
        return false;
    }

    if (hwnd) {
        if (!createDeviceAndSwapChain(hwnd)) {
            shutdown();
            return false;
        }
    }

    if (!createDescriptorHeaps() || !createFence()) {
        shutdown();
        return false;
    }

    rtvDescriptorSize_ = d3d12_rtv_descriptor_size(device_);
    initialized_ = true;

    spdlog::info("D3D12Device initialized: {} (VRAM: {} MB)", deviceName_, vramBudget_ / 1024 / 1024);
    return true;
}

bool D3D12Device::createDeviceAndSwapChain(void* hwnd) {
    if (!hwnd) return true;

    swapChain_ = (IDXGISwapChain3*)d3d12_create_swap_chain(
        factory_, commandQueue_, hwnd, 1920, 1080, 3);
    if (!swapChain_) {
        spdlog::error("D3D12Device: failed to create swap chain");
        return false;
    }

    frameIndex_ = 0; // current back buffer index
    return true;
}

bool D3D12Device::createDescriptorHeaps() {
    rtvHeap_ = (ID3D12DescriptorHeap*)d3d12_create_rtv_heap(device_, 3);
    dsvHeap_ = (ID3D12DescriptorHeap*)d3d12_create_dsv_heap(device_, 1);
    srvHeap_ = (ID3D12DescriptorHeap*)d3d12_create_srv_heap(device_, 256);
    return rtvHeap_ && dsvHeap_ && srvHeap_;
}

bool D3D12Device::createFence() {
    fence_ = (ID3D12Fence*)d3d12_create_fence(device_);
    if (!fence_) return false;

    fenceEvent_ = CreateEventA(nullptr, FALSE, FALSE, nullptr);
    return fenceEvent_ != nullptr;
}

void D3D12Device::shutdown() {
    if (!initialized_) return;
    waitIdle();

    if (fenceEvent_) {
        CloseHandle(fenceEvent_);
        fenceEvent_ = nullptr;
    }
    if (fence_) {
        d3d12_release_fence(fence_);
        fence_ = nullptr;
    }
    if (srvHeap_) {
        d3d12_release_descriptor_heap(srvHeap_);
        srvHeap_ = nullptr;
    }
    if (dsvHeap_) {
        d3d12_release_descriptor_heap(dsvHeap_);
        dsvHeap_ = nullptr;
    }
    if (rtvHeap_) {
        d3d12_release_descriptor_heap(rtvHeap_);
        rtvHeap_ = nullptr;
    }
    if (swapChain_) {
        d3d12_release_swap_chain(swapChain_);
        swapChain_ = nullptr;
    }
    if (commandQueue_) {
        d3d12_release_command_queue(commandQueue_);
        commandQueue_ = nullptr;
    }
    if (device_) {
        d3d12_release_device(device_);
        device_ = nullptr;
    }
    if (factory_) {
        d3d12_release_factory(factory_);
        factory_ = nullptr;
    }

    initialized_ = false;
}

std::shared_ptr<GpuBuffer> D3D12Device::createBuffer(const BufferDesc& desc, const void* data) {
    return std::make_shared<D3D12Buffer>(device_, desc, data);
}

std::shared_ptr<GpuTexture> D3D12Device::createTexture(const TextureDesc& desc) {
    return std::make_shared<D3D12Texture>(device_, desc);
}

std::shared_ptr<GpuShader> D3D12Device::createShader(ShaderStage stage, const std::vector<uint32_t>& spirv) {
    // SPIR-V → DXIL is handled by spirv-cross or DXC at pipeline creation time
    // Store SPIR-V as bytecode for now (actual DXIL compilation deferred)
    std::vector<uint8_t> dxil(spirv.size() * sizeof(uint32_t));
    std::memcpy(dxil.data(), spirv.data(), dxil.size());
    return std::make_shared<D3D12Shader>(stage, dxil);
}

std::shared_ptr<D3D12Shader> D3D12Device::createShaderFromDXIL(ShaderStage stage, const std::vector<uint8_t>& dxil) {
    return std::make_shared<D3D12Shader>(stage, dxil);
}

std::shared_ptr<GpuPipeline> D3D12Device::createPipeline(const PipelineDesc& desc) {
    // Create root signature (shared for all pipelines of the same layout)
    // In production, this would be cached per layout
    ID3D12RootSignature* rootSig = (ID3D12RootSignature*)d3d12_create_root_signature(device_);
    if (!rootSig) return nullptr;

    // Compute pipeline
    if (desc.computeShader) {
        auto cs = std::dynamic_pointer_cast<D3D12Shader>(desc.computeShader);
        if (!cs) {
            d3d12_release_root_signature(rootSig);
            return nullptr;
        }
        auto pipeline = std::make_shared<D3D12Pipeline>(device_, rootSig, cs);
        return pipeline;
    }

    // Graphics pipeline
    auto vs = std::dynamic_pointer_cast<D3D12Shader>(desc.vertexShader);
    auto fs = std::dynamic_pointer_cast<D3D12Shader>(desc.fragmentShader);
    if (!vs && !fs) {
        d3d12_release_root_signature(rootSig);
        return nullptr;
    }
    auto pipeline = std::make_shared<D3D12Pipeline>(device_, rootSig, desc, vs, fs);
    return pipeline;
}

std::shared_ptr<GpuCommandBuffer> D3D12Device::createCommandBuffer() {
    auto allocator = (ID3D12CommandAllocator*)d3d12_create_command_allocator(device_);
    if (!allocator) return nullptr;

    auto cmdList = (ID3D12GraphicsCommandList*)d3d12_create_command_list(device_, allocator);
    if (!cmdList) {
        d3d12_release_command_allocator(allocator);
        return nullptr;
    }

    return std::make_shared<D3D12CommandBuffer>(device_, allocator, cmdList);
}

void D3D12Device::submit(std::shared_ptr<GpuCommandBuffer> cmd) {
    auto d3dCmd = std::dynamic_pointer_cast<D3D12CommandBuffer>(cmd);
    if (!d3dCmd) return;

    d3dCmd->end();

    void* list = d3dCmd->cmdList();
    d3d12_execute_command_lists(commandQueue_, &list, 1);

    // Signal fence
    fenceValue_++;
    d3d12_signal_fence(commandQueue_, fence_, fenceValue_);
}

void D3D12Device::present() {
    if (swapChain_) {
        d3d12_present(swapChain_, 1); // VSync enabled
        frameIndex_ = (frameIndex_ + 1) % 3;
    }
}

void D3D12Device::waitIdle() {
    if (!fence_ || !fenceEvent_) return;

    fenceValue_++;
    d3d12_signal_fence(commandQueue_, fence_, fenceValue_);
    d3d12_wait_fence(fence_, fenceValue_, fenceEvent_);
}

uint64_t D3D12Device::gpuMemoryUsed() const {
    return d3d12_device_vram_used(device_);
}

uint64_t D3D12Device::gpuMemoryBudget() const {
    return vramBudget_;
}

// ============================================================
// Factory
// ============================================================
std::shared_ptr<GpuDevice> createD3D12Device() {
    auto device = std::make_shared<D3D12Device>();
    if (!device->initialize(nullptr)) {
        return nullptr;
    }
    return device;
}

} // namespace solra::render

#else // !SOLRA_GPU_D3D12

// Empty stubs when D3D12 backend not compiled
namespace solra::render {
std::shared_ptr<GpuDevice> createD3D12Device() {
    return nullptr;
}
} // namespace solra::render

#endif // SOLRA_GPU_D3D12
