/*
 * Solra Core SDK - DirectX 12 Bridge (Windows)
 *
 * Bridges between C++ d3d12_device.cpp and the DirectX 12 C API.
 * Compiled only when SOLRA_GPU_D3D12 is defined and target is Windows.
 *
 * Copyright 2026 Solra Project
 * SPDX-License-Identifier: Apache-2.0
 */

#if defined(SOLRA_GPU_D3D12) && defined(_WIN32)

#include <windows.h>
#include <d3d12.h>
#include <dxgi1_6.h>
#include <dxgidebug.h>
#include <cstring>
#include <cstdlib>
#include <string>

#pragma comment(lib, "d3d12.lib")
#pragma comment(lib, "dxgi.lib")
#pragma comment(lib, "dxguid.lib")

// ============================================================
// Helper
// ============================================================
static inline char* str_dup(const char* s) {
    if (!s) return nullptr;
    size_t len = strlen(s) + 1;
    char* buf = (char*)malloc(len);
    memcpy(buf, s, len);
    return buf;
}

static inline std::wstring to_wstr(const char* s) {
    if (!s) return L"";
    int len = MultiByteToWideChar(CP_UTF8, 0, s, -1, nullptr, 0);
    std::wstring w(len, L'\0');
    MultiByteToWideChar(CP_UTF8, 0, s, -1, &w[0], len);
    return w;
}

// ============================================================
// Device
// ============================================================
extern "C" {

void* d3d12_create_factory() {
    IDXGIFactory4* factory = nullptr;
    HRESULT hr = CreateDXGIFactory2(0, IID_PPV_ARGS(&factory));
    if (FAILED(hr)) return nullptr;
    return factory;
}

void d3d12_release_factory(void* factory) {
    if (factory) ((IDXGIFactory4*)factory)->Release();
}

void* d3d12_create_device(void* factory, int adapterIndex, char** errorOut) {
    IDXGIFactory4* f = (IDXGIFactory4*)factory;
    IDXGIAdapter1* adapter = nullptr;

    HRESULT hr = f->EnumAdapters1(adapterIndex, &adapter);
    if (FAILED(hr)) {
        // Try software adapter (WARP)
        hr = f->EnumWarpAdapter(IID_PPV_ARGS(&adapter));
        if (FAILED(hr)) {
            if (errorOut) *errorOut = str_dup("No D3D12-capable GPU or WARP adapter found");
            return nullptr;
        }
    }

    ID3D12Device* device = nullptr;
    hr = D3D12CreateDevice(adapter, D3D_FEATURE_LEVEL_12_1, IID_PPV_ARGS(&device));
    adapter->Release();

    if (FAILED(hr)) {
        // Try feature level 12.0
        hr = D3D12CreateDevice(adapter, D3D_FEATURE_LEVEL_12_0, IID_PPV_ARGS(&device));
    }
    if (FAILED(hr)) {
        if (errorOut) *errorOut = str_dup("Failed to create D3D12 device (requires FL 12.0+)");
        return nullptr;
    }

    return device;
}

void d3d12_release_device(void* dev) {
    if (dev) ((ID3D12Device*)dev)->Release();
}

const char* d3d12_device_name(void* dev) {
    ID3D12Device* device = (ID3D12Device*)dev;
    DXGI_ADAPTER_DESC desc{};
    IDXGIAdapter* adapter = nullptr;

    // Get adapter via LUID
    LUID luid = device->GetAdapterLuid();
    IDXGIFactory4* factory = nullptr;
    if (SUCCEEDED(CreateDXGIFactory2(0, IID_PPV_ARGS(&factory)))) {
        factory->EnumAdapterByLuid(luid, IID_PPV_ARGS(&adapter));
        if (adapter) {
            adapter->GetDesc(&desc);
            adapter->Release();
        }
        factory->Release();
    }

    char buf[256];
    WideCharToMultiByte(CP_UTF8, 0, desc.Description, -1, buf, sizeof(buf), nullptr, nullptr);
    return str_dup(buf);
}

uint64_t d3d12_device_vram_budget(void* dev) {
    ID3D12Device* device = (ID3D12Device*)dev;
    LUID luid = device->GetAdapterLuid();
    IDXGIFactory4* factory = nullptr;
    uint64_t budget = 0;

    if (SUCCEEDED(CreateDXGIFactory2(0, IID_PPV_ARGS(&factory)))) {
        IDXGIAdapter3* adapter = nullptr;
        if (SUCCEEDED(factory->EnumAdapterByLuid(luid, IID_PPV_ARGS(&adapter)))) {
            DXGI_QUERY_VIDEO_MEMORY_INFO memInfo{};
            if (SUCCEEDED(adapter->QueryVideoMemoryInfo(0, DXGI_MEMORY_SEGMENT_GROUP_LOCAL, &memInfo))) {
                budget = memInfo.Budget;
            }
            adapter->Release();
        }
        factory->Release();
    }

    return budget > 0 ? budget : 4ULL * 1024 * 1024 * 1024; // 4GB fallback
}

uint64_t d3d12_device_vram_used(void* dev) {
    ID3D12Device* device = (ID3D12Device*)dev;
    LUID luid = device->GetAdapterLuid();
    IDXGIFactory4* factory = nullptr;
    uint64_t used = 0;

    if (SUCCEEDED(CreateDXGIFactory2(0, IID_PPV_ARGS(&factory)))) {
        IDXGIAdapter3* adapter = nullptr;
        if (SUCCEEDED(factory->EnumAdapterByLuid(luid, IID_PPV_ARGS(&adapter)))) {
            DXGI_QUERY_VIDEO_MEMORY_INFO memInfo{};
            if (SUCCEEDED(adapter->QueryVideoMemoryInfo(0, DXGI_MEMORY_SEGMENT_GROUP_LOCAL, &memInfo))) {
                used = memInfo.CurrentUsage;
            }
            adapter->Release();
        }
        factory->Release();
    }

    return used;
}

// ============================================================
// Command Queue
// ============================================================
void* d3d12_create_command_queue(void* dev) {
    ID3D12Device* device = (ID3D12Device*)dev;

    D3D12_COMMAND_QUEUE_DESC desc{};
    desc.Type = D3D12_COMMAND_LIST_TYPE_DIRECT;
    desc.Priority = D3D12_COMMAND_QUEUE_PRIORITY_NORMAL;
    desc.Flags = D3D12_COMMAND_QUEUE_FLAG_NONE;

    ID3D12CommandQueue* queue = nullptr;
    HRESULT hr = device->CreateCommandQueue(&desc, IID_PPV_ARGS(&queue));
    if (FAILED(hr)) return nullptr;
    return queue;
}

void d3d12_release_command_queue(void* q) {
    if (q) ((ID3D12CommandQueue*)q)->Release();
}

// ============================================================
// Swap Chain
// ============================================================
void* d3d12_create_swap_chain(void* factory, void* queue, void* hwnd,
                               uint32_t w, uint32_t h, uint32_t bufferCount) {
    IDXGIFactory4* f = (IDXGIFactory4*)factory;
    ID3D12CommandQueue* q = (ID3D12CommandQueue*)queue;

    DXGI_SWAP_CHAIN_DESC1 desc{};
    desc.Width = w;
    desc.Height = h;
    desc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    desc.SampleDesc.Count = 1;
    desc.BufferUsage = DXGI_USAGE_RENDER_TARGET_OUTPUT;
    desc.BufferCount = bufferCount;
    desc.SwapEffect = DXGI_SWAP_EFFECT_FLIP_DISCARD;

    IDXGISwapChain1* swapChain1 = nullptr;
    HRESULT hr = f->CreateSwapChainForHwnd(q, (HWND)hwnd, &desc, nullptr, nullptr, &swapChain1);
    if (FAILED(hr)) return nullptr;

    IDXGISwapChain3* swapChain3 = nullptr;
    swapChain1->QueryInterface(IID_PPV_ARGS(&swapChain3));
    swapChain1->Release();

    return swapChain3;
}

void d3d12_release_swap_chain(void* sc) {
    if (sc) ((IDXGISwapChain3*)sc)->Release();
}

// ============================================================
// Buffer
// ============================================================
void* d3d12_create_buffer(void* dev, uint64_t size, int isUpload, const void* data) {
    ID3D12Device* device = (ID3D12Device*)dev;

    D3D12_HEAP_PROPERTIES heapProps{};
    D3D12_RESOURCE_DESC resDesc{};
    resDesc.Dimension = D3D12_RESOURCE_DIMENSION_BUFFER;
    resDesc.Width = size;
    resDesc.Height = 1;
    resDesc.DepthOrArraySize = 1;
    resDesc.MipLevels = 1;
    resDesc.SampleDesc.Count = 1;
    resDesc.Layout = D3D12_TEXTURE_LAYOUT_ROW_MAJOR;

    if (isUpload) {
        heapProps.Type = D3D12_HEAP_TYPE_UPLOAD;
        resDesc.Flags = D3D12_RESOURCE_FLAG_NONE;
    } else {
        heapProps.Type = D3D12_HEAP_TYPE_DEFAULT;
        resDesc.Flags = D3D12_RESOURCE_FLAG_ALLOW_UNORDERED_ACCESS;
    }

    D3D12_RESOURCE_STATES initialState = isUpload ?
        D3D12_RESOURCE_STATE_GENERIC_READ : D3D12_RESOURCE_STATE_COMMON;

    ID3D12Resource* resource = nullptr;
    HRESULT hr = device->CreateCommittedResource(
        &heapProps, D3D12_HEAP_FLAG_NONE, &resDesc,
        initialState, nullptr, IID_PPV_ARGS(&resource));

    if (FAILED(hr)) return nullptr;

    // Copy initial data for upload buffers
    if (data && isUpload && size > 0) {
        void* mapped = nullptr;
        D3D12_RANGE range{0, 0};
        if (SUCCEEDED(resource->Map(0, &range, &mapped))) {
            memcpy(mapped, data, (size_t)size);
            resource->Unmap(0, nullptr);
        }
    }

    return resource;
}

void d3d12_release_buffer(void* buf) {
    if (buf) ((ID3D12Resource*)buf)->Release();
}

void* d3d12_buffer_map(void* buf) {
    ID3D12Resource* resource = (ID3D12Resource*)buf;
    void* data = nullptr;
    D3D12_RANGE range{0, 0};
    if (SUCCEEDED(resource->Map(0, &range, &data))) return data;
    return nullptr;
}

void d3d12_buffer_unmap(void* buf) {
    ID3D12Resource* resource = (ID3D12Resource*)buf;
    resource->Unmap(0, nullptr);
}

uint64_t d3d12_buffer_size(void* buf) {
    ID3D12Resource* resource = (ID3D12Resource*)buf;
    return resource->GetDesc().Width;
}

// ============================================================
// Texture
// ============================================================
void* d3d12_create_texture(void* dev, uint32_t w, uint32_t h, int fmt, uint32_t mips) {
    ID3D12Device* device = (ID3D12Device*)dev;

    D3D12_HEAP_PROPERTIES heapProps{};
    heapProps.Type = D3D12_HEAP_TYPE_DEFAULT;

    D3D12_RESOURCE_DESC desc{};
    desc.Dimension = D3D12_RESOURCE_DIMENSION_TEXTURE2D;
    desc.Width = w;
    desc.Height = (UINT)h;
    desc.DepthOrArraySize = 1;
    desc.MipLevels = mips;
    desc.Format = (DXGI_FORMAT)fmt;
    desc.SampleDesc.Count = 1;
    desc.Flags = D3D12_RESOURCE_FLAG_ALLOW_RENDER_TARGET;

    ID3D12Resource* resource = nullptr;
    HRESULT hr = device->CreateCommittedResource(
        &heapProps, D3D12_HEAP_FLAG_NONE, &desc,
        D3D12_RESOURCE_STATE_COMMON, nullptr, IID_PPV_ARGS(&resource));

    if (FAILED(hr)) return nullptr;
    return resource;
}

void d3d12_release_texture(void* tex) {
    if (tex) ((ID3D12Resource*)tex)->Release();
}

void d3d12_texture_get_size(void* tex, uint32_t* w, uint32_t* h) {
    ID3D12Resource* resource = (ID3D12Resource*)tex;
    auto desc = resource->GetDesc();
    if (w) *w = (uint32_t)desc.Width;
    if (h) *h = desc.Height;
}

// ============================================================
// Root Signature (simple: 1 CBV + 1 SRV descriptor table)
// ============================================================
void* d3d12_create_root_signature(void* dev) {
    ID3D12Device* device = (ID3D12Device*)dev;

    // Parameter 0: CBV for per-frame constants
    D3D12_ROOT_PARAMETER params[2]{};
    params[0].ParameterType = D3D12_ROOT_PARAMETER_TYPE_CBV;
    params[0].Descriptor.ShaderRegister = 0;
    params[0].Descriptor.RegisterSpace = 0;
    params[0].ShaderVisibility = D3D12_SHADER_VISIBILITY_ALL;

    // Parameter 1: SRV descriptor table for textures
    D3D12_DESCRIPTOR_RANGE srvRange{};
    srvRange.RangeType = D3D12_DESCRIPTOR_RANGE_TYPE_SRV;
    srvRange.NumDescriptors = 16;
    srvRange.BaseShaderRegister = 0;
    srvRange.RegisterSpace = 0;
    srvRange.OffsetInDescriptorsFromTableStart = 0;

    params[1].ParameterType = D3D12_ROOT_PARAMETER_TYPE_DESCRIPTOR_TABLE;
    params[1].DescriptorTable.NumDescriptorRanges = 1;
    params[1].DescriptorTable.pDescriptorRanges = &srvRange;
    params[1].ShaderVisibility = D3D12_SHADER_VISIBILITY_PIXEL;

    // Static samplers
    D3D12_STATIC_SAMPLER_DESC sampler{};
    sampler.Filter = D3D12_FILTER_ANISOTROPIC;
    sampler.AddressU = D3D12_TEXTURE_ADDRESS_MODE_WRAP;
    sampler.AddressV = D3D12_TEXTURE_ADDRESS_MODE_WRAP;
    sampler.AddressW = D3D12_TEXTURE_ADDRESS_MODE_WRAP;
    sampler.MaxAnisotropy = 16;
    sampler.ComparisonFunc = D3D12_COMPARISON_FUNC_NEVER;
    sampler.ShaderRegister = 0;
    sampler.RegisterSpace = 0;
    sampler.ShaderVisibility = D3D12_SHADER_VISIBILITY_PIXEL;

    D3D12_ROOT_SIGNATURE_DESC sigDesc{};
    sigDesc.NumParameters = 2;
    sigDesc.pParameters = params;
    sigDesc.NumStaticSamplers = 1;
    sigDesc.pStaticSamplers = &sampler;
    sigDesc.Flags = D3D12_ROOT_SIGNATURE_FLAG_ALLOW_INPUT_ASSEMBLER_INPUT_LAYOUT;

    ID3DBlob* signature = nullptr;
    ID3DBlob* error = nullptr;
    HRESULT hr = D3D12SerializeRootSignature(&sigDesc, D3D_ROOT_SIGNATURE_VERSION_1,
                                              &signature, &error);
    if (FAILED(hr)) {
        if (error) error->Release();
        return nullptr;
    }

    ID3D12RootSignature* rootSig = nullptr;
    hr = device->CreateRootSignature(0, signature->GetBufferPointer(),
                                      signature->GetBufferSize(),
                                      IID_PPV_ARGS(&rootSig));
    signature->Release();

    if (FAILED(hr)) return nullptr;
    return rootSig;
}

void d3d12_release_root_signature(void* rs) {
    if (rs) ((ID3D12RootSignature*)rs)->Release();
}

// ============================================================
// Pipeline State Objects
// ============================================================
void* d3d12_create_graphics_pso(void* dev, void* rootSig,
    const void* vsBytecode, size_t vsSize,
    const void* fsBytecode, size_t fsSize,
    int depthTest, int depthWrite, int blending, char** errorOut) {
    ID3D12Device* device = (ID3D12Device*)dev;

    D3D12_GRAPHICS_PIPELINE_STATE_DESC desc{};
    desc.pRootSignature = (ID3D12RootSignature*)rootSig;

    if (vsBytecode && vsSize > 0) {
        desc.VS.pShaderBytecode = vsBytecode;
        desc.VS.BytecodeLength = vsSize;
    }
    if (fsBytecode && fsSize > 0) {
        desc.PS.pShaderBytecode = fsBytecode;
        desc.PS.BytecodeLength = fsSize;
    }

    // Input layout (standard PBR vertex: pos3+normal3+uv2)
    D3D12_INPUT_ELEMENT_DESC inputLayout[] = {
        {"POSITION", 0, DXGI_FORMAT_R32G32B32_FLOAT,    0, 0,  D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA, 0},
        {"NORMAL",   0, DXGI_FORMAT_R32G32B32_FLOAT,    0, 12, D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA, 0},
        {"TEXCOORD", 0, DXGI_FORMAT_R32G32_FLOAT,       0, 24, D3D12_INPUT_CLASSIFICATION_PER_VERTEX_DATA, 0},
    };
    desc.InputLayout.pInputElementDescs = inputLayout;
    desc.InputLayout.NumElements = 3;

    desc.PrimitiveTopologyType = D3D12_PRIMITIVE_TOPOLOGY_TYPE_TRIANGLE;

    // RTV
    desc.NumRenderTargets = 1;
    desc.RTVFormats[0] = DXGI_FORMAT_R8G8B8A8_UNORM;

    // DSV
    if (depthTest) {
        desc.DSVFormat = DXGI_FORMAT_D32_FLOAT;
    } else {
        desc.DSVFormat = DXGI_FORMAT_UNKNOWN;
    }

    desc.SampleDesc.Count = 1;

    // Rasterizer
    desc.RasterizerState.FillMode = D3D12_FILL_MODE_SOLID;
    desc.RasterizerState.CullMode = D3D12_CULL_MODE_BACK;
    desc.RasterizerState.DepthClipEnable = TRUE;

    // Depth-stencil
    desc.DepthStencilState.DepthEnable = depthTest ? TRUE : FALSE;
    desc.DepthStencilState.DepthWriteMask = depthWrite ? D3D12_DEPTH_WRITE_MASK_ALL : D3D12_DEPTH_WRITE_MASK_ZERO;
    desc.DepthStencilState.DepthFunc = D3D12_COMPARISON_FUNC_LESS_EQUAL;

    // Blend
    if (blending) {
        desc.BlendState.RenderTarget[0].BlendEnable = TRUE;
        desc.BlendState.RenderTarget[0].SrcBlend = D3D12_BLEND_SRC_ALPHA;
        desc.BlendState.RenderTarget[0].DestBlend = D3D12_BLEND_INV_SRC_ALPHA;
        desc.BlendState.RenderTarget[0].BlendOp = D3D12_BLEND_OP_ADD;
        desc.BlendState.RenderTarget[0].SrcBlendAlpha = D3D12_BLEND_ONE;
        desc.BlendState.RenderTarget[0].DestBlendAlpha = D3D12_BLEND_INV_SRC_ALPHA;
        desc.BlendState.RenderTarget[0].BlendOpAlpha = D3D12_BLEND_OP_ADD;
        desc.BlendState.RenderTarget[0].RenderTargetWriteMask = D3D12_COLOR_WRITE_ENABLE_ALL;
    }

    ID3D12PipelineState* pso = nullptr;
    HRESULT hr = device->CreateGraphicsPipelineState(&desc, IID_PPV_ARGS(&pso));
    if (FAILED(hr)) {
        if (errorOut) *errorOut = str_dup("Failed to create D3D12 graphics PSO");
        return nullptr;
    }

    return pso;
}

void* d3d12_create_compute_pso(void* dev, void* rootSig,
    const void* csBytecode, size_t csSize, char** errorOut) {
    ID3D12Device* device = (ID3D12Device*)dev;

    D3D12_COMPUTE_PIPELINE_STATE_DESC desc{};
    desc.pRootSignature = (ID3D12RootSignature*)rootSig;
    desc.CS.pShaderBytecode = csBytecode;
    desc.CS.BytecodeLength = csSize;

    ID3D12PipelineState* pso = nullptr;
    HRESULT hr = device->CreateComputePipelineState(&desc, IID_PPV_ARGS(&pso));
    if (FAILED(hr)) {
        if (errorOut) *errorOut = str_dup("Failed to create D3D12 compute PSO");
        return nullptr;
    }

    return pso;
}

void d3d12_release_pso(void* pso) {
    if (pso) ((ID3D12PipelineState*)pso)->Release();
}

// ============================================================
// Descriptor Heaps
// ============================================================
void* d3d12_create_rtv_heap(void* dev, uint32_t count) {
    ID3D12Device* device = (ID3D12Device*)dev;

    D3D12_DESCRIPTOR_HEAP_DESC desc{};
    desc.Type = D3D12_DESCRIPTOR_HEAP_TYPE_RTV;
    desc.NumDescriptors = count;
    desc.Flags = D3D12_DESCRIPTOR_HEAP_FLAG_NONE;

    ID3D12DescriptorHeap* heap = nullptr;
    if (FAILED(device->CreateDescriptorHeap(&desc, IID_PPV_ARGS(&heap)))) return nullptr;
    return heap;
}

void* d3d12_create_dsv_heap(void* dev, uint32_t count) {
    ID3D12Device* device = (ID3D12Device*)dev;

    D3D12_DESCRIPTOR_HEAP_DESC desc{};
    desc.Type = D3D12_DESCRIPTOR_HEAP_TYPE_DSV;
    desc.NumDescriptors = count;
    desc.Flags = D3D12_DESCRIPTOR_HEAP_FLAG_NONE;

    ID3D12DescriptorHeap* heap = nullptr;
    if (FAILED(device->CreateDescriptorHeap(&desc, IID_PPV_ARGS(&heap)))) return nullptr;
    return heap;
}

void* d3d12_create_srv_heap(void* dev, uint32_t count) {
    ID3D12Device* device = (ID3D12Device*)dev;

    D3D12_DESCRIPTOR_HEAP_DESC desc{};
    desc.Type = D3D12_DESCRIPTOR_HEAP_TYPE_CBV_SRV_UAV;
    desc.NumDescriptors = count;
    desc.Flags = D3D12_DESCRIPTOR_HEAP_FLAG_SHADER_VISIBLE;

    ID3D12DescriptorHeap* heap = nullptr;
    if (FAILED(device->CreateDescriptorHeap(&desc, IID_PPV_ARGS(&heap)))) return nullptr;
    return heap;
}

void d3d12_release_descriptor_heap(void* heap) {
    if (heap) ((ID3D12DescriptorHeap*)heap)->Release();
}

uint32_t d3d12_rtv_descriptor_size(void* dev) {
    ID3D12Device* device = (ID3D12Device*)dev;
    return device->GetDescriptorHandleIncrementSize(D3D12_DESCRIPTOR_HEAP_TYPE_RTV);
}

// ============================================================
// Command List
// ============================================================
void* d3d12_create_command_allocator(void* dev) {
    ID3D12Device* device = (ID3D12Device*)dev;
    ID3D12CommandAllocator* allocator = nullptr;
    if (FAILED(device->CreateCommandAllocator(D3D12_COMMAND_LIST_TYPE_DIRECT, IID_PPV_ARGS(&allocator))))
        return nullptr;
    return allocator;
}

void d3d12_release_command_allocator(void* alloc) {
    if (alloc) ((ID3D12CommandAllocator*)alloc)->Release();
}

void* d3d12_create_command_list(void* dev, void* allocator) {
    ID3D12Device* device = (ID3D12Device*)dev;
    ID3D12GraphicsCommandList* list = nullptr;
    if (FAILED(device->CreateCommandList(0, D3D12_COMMAND_LIST_TYPE_DIRECT,
        (ID3D12CommandAllocator*)allocator, nullptr, IID_PPV_ARGS(&list))))
        return nullptr;
    // Close initially; will be reset in begin()
    list->Close();
    return list;
}

void d3d12_release_command_list(void* list) {
    if (list) ((ID3D12GraphicsCommandList*)list)->Release();
}

void d3d12_reset_command_list(void* list, void* allocator) {
    ((ID3D12GraphicsCommandList*)list)->Reset((ID3D12CommandAllocator*)allocator, nullptr);
}

void d3d12_close_command_list(void* list) {
    ((ID3D12GraphicsCommandList*)list)->Close();
}

// ============================================================
// Command Recording
// ============================================================
void d3d12_cmd_set_pipeline(void* list, void* pso) {
    ((ID3D12GraphicsCommandList*)list)->SetPipelineState((ID3D12PipelineState*)pso);
}

void d3d12_cmd_set_root_signature(void* list, void* rs) {
    ((ID3D12GraphicsCommandList*)list)->SetGraphicsRootSignature((ID3D12RootSignature*)rs);
}

void d3d12_cmd_set_vertex_buffer(void* list, void* buf, uint64_t offset, uint32_t slot) {
    D3D12_VERTEX_BUFFER_VIEW vbv{};
    vbv.BufferLocation = ((ID3D12Resource*)buf)->GetGPUVirtualAddress() + offset;
    vbv.SizeInBytes = (UINT)(((ID3D12Resource*)buf)->GetDesc().Width - offset);
    vbv.StrideInBytes = 32; // pos3+normal3+uv2 = 8 floats = 32 bytes
    ((ID3D12GraphicsCommandList*)list)->IASetVertexBuffers(slot, 1, &vbv);
}

void d3d12_cmd_set_index_buffer(void* list, void* buf, uint64_t offset) {
    D3D12_INDEX_BUFFER_VIEW ibv{};
    ibv.BufferLocation = ((ID3D12Resource*)buf)->GetGPUVirtualAddress() + offset;
    ibv.SizeInBytes = (UINT)(((ID3D12Resource*)buf)->GetDesc().Width - offset);
    ibv.Format = DXGI_FORMAT_R32_UINT;
    ((ID3D12GraphicsCommandList*)list)->IASetIndexBuffer(&ibv);
    ((ID3D12GraphicsCommandList*)list)->IASetPrimitiveTopology(D3D_PRIMITIVE_TOPOLOGY_TRIANGLELIST);
}

void d3d12_cmd_draw(void* list, uint32_t vertexCount, uint32_t instanceCount,
                     uint32_t firstVertex, uint32_t firstInstance) {
    ((ID3D12GraphicsCommandList*)list)->DrawInstanced(vertexCount, instanceCount, firstVertex, firstInstance);
}

void d3d12_cmd_draw_indexed(void* list, uint32_t indexCount, uint32_t instanceCount,
                              uint32_t firstIndex, int32_t vertexOffset, uint32_t firstInstance) {
    ((ID3D12GraphicsCommandList*)list)->DrawIndexedInstanced(indexCount, instanceCount, firstIndex, vertexOffset, firstInstance);
}

void d3d12_cmd_dispatch(void* list, uint32_t groupsX, uint32_t groupsY, uint32_t groupsZ) {
    ((ID3D12GraphicsCommandList*)list)->Dispatch(groupsX, groupsY, groupsZ);
}

// ============================================================
// Submission
// ============================================================
void d3d12_execute_command_lists(void* queue, void** lists, uint32_t count) {
    ((ID3D12CommandQueue*)queue)->ExecuteCommandLists(count, (ID3D12CommandList**)lists);
}

void d3d12_signal_fence(void* queue, void* fence, uint64_t value) {
    ((ID3D12CommandQueue*)queue)->Signal((ID3D12Fence*)fence, value);
}

void d3d12_wait_fence(void* fence, uint64_t value, void* event) {
    ID3D12Fence* f = (ID3D12Fence*)fence;
    if (f->GetCompletedValue() < value) {
        f->SetEventOnCompletion(value, (HANDLE)event);
        WaitForSingleObject((HANDLE)event, INFINITE);
    }
}

void d3d12_present(void* swapChain, uint32_t syncInterval) {
    ((IDXGISwapChain3*)swapChain)->Present(syncInterval, 0);
}

// ============================================================
// Fence
// ============================================================
void* d3d12_create_fence(void* dev) {
    ID3D12Device* device = (ID3D12Device*)dev;
    ID3D12Fence* fence = nullptr;
    if (FAILED(device->CreateFence(0, D3D12_FENCE_FLAG_NONE, IID_PPV_ARGS(&fence)))) return nullptr;
    return fence;
}

void d3d12_release_fence(void* fence) {
    if (fence) ((ID3D12Fence*)fence)->Release();
}

void d3d12_free_string(char* str) {
    free(str);
}

} // extern "C"

#endif // SOLRA_GPU_D3D12 && _WIN32
