/*
 * Solra Core SDK - Metal 3 Bridge (Objective-C++)
 *
 * Bridges between C++ metal_device.cpp and Apple Metal 3 Objective-C API.
 * Compiled only when SOLRA_GPU_METAL is defined and target is Apple platform.
 *
 * Copyright 2026 Solra Project
 * SPDX-License-Identifier: Apache-2.0
 */

#if defined(SOLRA_GPU_METAL) && defined(__APPLE__)

#import <Metal/Metal.h>
#import <MetalKit/MetalKit.h>
#import <Foundation/Foundation.h>
#include <cstring>
#include <cstdlib>

// ============================================================
// Helper: NSString <-> C string
// ============================================================
static inline NSString* toNSStr(const char* s) {
    return s ? [NSString stringWithUTF8String:s] : @"";
}

static inline char* fromNSStr(NSString* s) {
    if (!s) return nullptr;
    const char* utf8 = [s UTF8String];
    size_t len = strlen(utf8) + 1;
    char* buf = (char*)malloc(len);
    memcpy(buf, utf8, len);
    return buf;
}

// ============================================================
// Device
// ============================================================
extern "C" {

void* metal_create_device() {
    @autoreleasepool {
        id<MTLDevice> device = MTLCreateSystemDefaultDevice();
        if (!device) {
            // Try low-power device on MacBooks
            NSArray<id<MTLDevice>>* devices = MTLCopyAllDevices();
            if (devices.count > 0) {
                device = devices[0];
            }
        }
        return (__bridge_retained void*)device;
    }
}

void metal_release_device(void* dev) {
    if (dev) CFRelease(dev);
}

const char* metal_device_name(void* dev) {
    @autoreleasepool {
        id<MTLDevice> device = (__bridge id<MTLDevice>)dev;
        return strdup([device.name UTF8String]);
    }
}

uint64_t metal_device_vram_budget(void* dev) {
    @autoreleasepool {
        id<MTLDevice> device = (__bridge id<MTLDevice>)dev;
        if (@available(macOS 13.0, iOS 16.0, *)) {
            return device.recommendedMaxWorkingSetSize;
        }
        return 4ULL * 1024 * 1024 * 1024; // 4GB estimate
    }
}

uint64_t metal_device_vram_used(void* dev) {
    @autoreleasepool {
        id<MTLDevice> device = (__bridge id<MTLDevice>)dev;
        if (@available(macOS 13.0, iOS 16.0, *)) {
            return device.currentAllocatedSize;
        }
        return 0;
    }
}

// ============================================================
// Command Queue
// ============================================================
void* metal_create_command_queue(void* dev) {
    @autoreleasepool {
        id<MTLDevice> device = (__bridge id<MTLDevice>)dev;
        id<MTLCommandQueue> queue = [device newCommandQueue];
        return (__bridge_retained void*)queue;
    }
}

void metal_release_command_queue(void* q) {
    if (q) CFRelease(q);
}

// ============================================================
// Buffer
// ============================================================
void* metal_create_buffer(void* dev, uint64_t size, int usageFlags, const void* data) {
    @autoreleasepool {
        id<MTLDevice> device = (__bridge id<MTLDevice>)dev;
        MTLResourceOptions options = MTLResourceStorageModeShared;
        id<MTLBuffer> buffer;

        if (data) {
            buffer = [device newBufferWithBytes:data length:(NSUInteger)size options:options];
        } else {
            buffer = [device newBufferWithLength:(NSUInteger)size options:options];
        }
        return (__bridge_retained void*)buffer;
    }
}

void metal_release_buffer(void* buf) {
    if (buf) CFRelease(buf);
}

void* metal_buffer_contents(void* buf) {
    id<MTLBuffer> buffer = (__bridge id<MTLBuffer>)buf;
    return [buffer contents];
}

uint64_t metal_buffer_length(void* buf) {
    id<MTLBuffer> buffer = (__bridge id<MTLBuffer>)buf;
    return buffer.length;
}

// ============================================================
// Texture
// ============================================================
void* metal_create_texture(void* dev, uint32_t w, uint32_t h, int fmt, uint32_t mips) {
    @autoreleasepool {
        id<MTLDevice> device = (__bridge id<MTLDevice>)dev;
        MTLTextureDescriptor* desc = [MTLTextureDescriptor texture2DDescriptorWithPixelFormat:(MTLPixelFormat)fmt
                                                                                        width:w
                                                                                       height:h
                                                                                    mipmapped:(mips > 1)];
        desc.mipmapLevelCount = mips;
        desc.storageMode = MTLStorageModePrivate;
        desc.usage = MTLTextureUsageShaderRead | MTLTextureUsageRenderTarget;

        id<MTLTexture> tex = [device newTextureWithDescriptor:desc];
        return (__bridge_retained void*)tex;
    }
}

void metal_release_texture(void* tex) {
    if (tex) CFRelease(tex);
}

uint32_t metal_texture_width(void* tex) {
    id<MTLTexture> texture = (__bridge id<MTLTexture>)tex;
    return (uint32_t)texture.width;
}

uint32_t metal_texture_height(void* tex) {
    id<MTLTexture> texture = (__bridge id<MTLTexture>)tex;
    return (uint32_t)texture.height;
}

// ============================================================
// Shader Compilation (MSL → MTLLibrary → MTLFunction)
// ============================================================
void* metal_compile_library(void* dev, const char* source, char** errorOut) {
    @autoreleasepool {
        id<MTLDevice> device = (__bridge id<MTLDevice>)dev;
        NSString* src = toNSStr(source);

        MTLCompileOptions* options = [[MTLCompileOptions alloc] init];
        options.languageVersion = MTLLanguageVersion3_0;
        options.fastMathEnabled = YES;

        NSError* error = nil;
        id<MTLLibrary> library = [device newLibraryWithSource:src options:options error:&error];

        if (error) {
            if (errorOut) {
                *errorOut = fromNSStr([error localizedDescription]);
            }
            return nullptr;
        }

        return (__bridge_retained void*)library;
    }
}

void metal_release_library(void* lib) {
    if (lib) CFRelease(lib);
}

void* metal_get_function(void* lib, const char* name) {
    @autoreleasepool {
        id<MTLLibrary> library = (__bridge id<MTLLibrary>)lib;
        id<MTLFunction> fn = [library newFunctionWithName:toNSStr(name)];
        return (__bridge_retained void*)fn;
    }
}

void metal_release_function(void* fn) {
    if (fn) CFRelease(fn);
}

// ============================================================
// Render Pipeline State
// ============================================================
void* metal_create_render_pipeline(void* dev, void* vsFn, void* fsFn,
    uint32_t colorFormat, uint32_t depthFormat, int blending,
    int depthTest, int depthWrite, char** errorOut) {
    @autoreleasepool {
        id<MTLDevice> device = (__bridge id<MTLDevice>)dev;

        MTLRenderPipelineDescriptor* desc = [[MTLRenderPipelineDescriptor alloc] init];

        if (vsFn) desc.vertexFunction = (__bridge id<MTLFunction>)vsFn;
        if (fsFn) desc.fragmentFunction = (__bridge id<MTLFunction>)fsFn;

        // Color attachment 0
        desc.colorAttachments[0].pixelFormat = (MTLPixelFormat)colorFormat;
        if (blending) {
            desc.colorAttachments[0].blendingEnabled = YES;
            desc.colorAttachments[0].sourceRGBBlendFactor = MTLBlendFactorSourceAlpha;
            desc.colorAttachments[0].destinationRGBBlendFactor = MTLBlendFactorOneMinusSourceAlpha;
            desc.colorAttachments[0].sourceAlphaBlendFactor = MTLBlendFactorOne;
            desc.colorAttachments[0].destinationAlphaBlendFactor = MTLBlendFactorOneMinusSourceAlpha;
        }

        // Depth
        desc.depthAttachmentPixelFormat = depthFormat > 0 ? (MTLPixelFormat)depthFormat : MTLPixelFormatInvalid;

        NSError* error = nil;
        id<MTLRenderPipelineState> ps = [device newRenderPipelineStateWithDescriptor:desc error:&error];

        if (error) {
            if (errorOut) {
                *errorOut = fromNSStr([error localizedDescription]);
            }
            return nullptr;
        }

        return (__bridge_retained void*)ps;
    }
}

void metal_release_render_pipeline(void* ps) {
    if (ps) CFRelease(ps);
}

// ============================================================
// Compute Pipeline State
// ============================================================
void* metal_create_compute_pipeline(void* dev, void* csFn, char** errorOut) {
    @autoreleasepool {
        id<MTLDevice> device = (__bridge id<MTLDevice>)dev;
        id<MTLFunction> fn = (__bridge id<MTLFunction>)csFn;

        NSError* error = nil;
        id<MTLComputePipelineState> ps = [device newComputePipelineStateWithFunction:fn error:&error];

        if (error) {
            if (errorOut) {
                *errorOut = fromNSStr([error localizedDescription]);
            }
            return nullptr;
        }

        return (__bridge_retained void*)ps;
    }
}

void metal_release_compute_pipeline(void* cs) {
    if (cs) CFRelease(cs);
}

// ============================================================
// Depth-Stencil State
// ============================================================
void* metal_create_depth_stencil_state(void* dev, int depthTest, int depthWrite, int compareFunc) {
    @autoreleasepool {
        id<MTLDevice> device = (__bridge id<MTLDevice>)dev;

        MTLDepthStencilDescriptor* desc = [[MTLDepthStencilDescriptor alloc] init];
        desc.depthCompareFunction = (MTLCompareFunction)compareFunc;
        desc.depthWriteEnabled = depthWrite ? YES : NO;

        id<MTLDepthStencilState> ds = [device newDepthStencilStateWithDescriptor:desc];
        return (__bridge_retained void*)ds;
    }
}

void metal_release_depth_stencil_state(void* ds) {
    if (ds) CFRelease(ds);
}

// ============================================================
// Command Buffer
// ============================================================
void* metal_create_command_buffer(void* queue) {
    @autoreleasepool {
        id<MTLCommandQueue> q = (__bridge id<MTLCommandQueue>)queue;
        id<MTLCommandBuffer> cmdBuf = [q commandBuffer];
        return (__bridge_retained void*)cmdBuf;
    }
}

void metal_release_command_buffer(void* cmdBuf) {
    if (cmdBuf) CFRelease(cmdBuf);
}

void metal_commit_command_buffer(void* cmdBuf) {
    id<MTLCommandBuffer> cb = (__bridge id<MTLCommandBuffer>)cmdBuf;
    [cb commit];
}

void metal_wait_command_buffer(void* cmdBuf) {
    id<MTLCommandBuffer> cb = (__bridge id<MTLCommandBuffer>)cmdBuf;
    [cb waitUntilCompleted];
}

// ============================================================
// Render Command Encoder
// ============================================================
void* metal_begin_render_encoder(void* cmdBuf, void* renderPassDesc) {
    @autoreleasepool {
        id<MTLCommandBuffer> cb = (__bridge id<MTLCommandBuffer>)cmdBuf;

        // Create a minimal render pass descriptor if none provided
        MTLRenderPassDescriptor* rpd;
        if (renderPassDesc) {
            rpd = (__bridge MTLRenderPassDescriptor*)renderPassDesc;
        } else {
            rpd = [MTLRenderPassDescriptor renderPassDescriptor];
            // Caller must provide actual color/depth attachments for real rendering
            // This is a placeholder for deferred attachment configuration
            rpd.colorAttachments[0].loadAction = MTLLoadActionClear;
            rpd.colorAttachments[0].storeAction = MTLStoreActionStore;
            rpd.colorAttachments[0].clearColor = MTLClearColorMake(0.1, 0.1, 0.15, 1.0);
        }

        id<MTLRenderCommandEncoder> enc = [cb renderCommandEncoderWithDescriptor:rpd];
        return (__bridge_retained void*)enc;
    }
}

void metal_end_render_encoder(void* enc) {
    id<MTLRenderCommandEncoder> encoder = (__bridge id<MTLRenderCommandEncoder>)enc;
    [encoder endEncoding];
    CFRelease(enc);
}

void metal_render_set_pipeline(void* enc, void* ps) {
    id<MTLRenderCommandEncoder> encoder = (__bridge id<MTLRenderCommandEncoder>)enc;
    id<MTLRenderPipelineState> state = (__bridge id<MTLRenderPipelineState>)ps;
    [encoder setRenderPipelineState:state];
}

void metal_render_set_depth_stencil(void* enc, void* ds) {
    id<MTLRenderCommandEncoder> encoder = (__bridge id<MTLRenderCommandEncoder>)enc;
    id<MTLDepthStencilState> state = (__bridge id<MTLDepthStencilState>)ds;
    [encoder setDepthStencilState:state];
}

void metal_render_set_vertex_buffer(void* enc, void* buf, uint64_t offset, uint32_t index) {
    id<MTLRenderCommandEncoder> encoder = (__bridge id<MTLRenderCommandEncoder>)enc;
    id<MTLBuffer> buffer = (__bridge id<MTLBuffer>)buf;
    [encoder setVertexBuffer:buffer offset:(NSUInteger)offset atIndex:index];
}

void metal_render_draw_primitives(void* enc, uint32_t type, uint32_t vertexStart,
                                   uint32_t vertexCount, uint32_t instanceCount) {
    id<MTLRenderCommandEncoder> encoder = (__bridge id<MTLRenderCommandEncoder>)enc;
    [encoder drawPrimitives:(MTLPrimitiveType)type
                vertexStart:(NSUInteger)vertexStart
                vertexCount:(NSUInteger)vertexCount
              instanceCount:(NSUInteger)instanceCount];
}

void metal_render_draw_indexed(void* enc, uint32_t type, uint32_t indexCount,
                                void* indexBuf, uint64_t indexOffset,
                                uint32_t instanceCount, uint32_t baseVertex) {
    id<MTLRenderCommandEncoder> encoder = (__bridge id<MTLRenderCommandEncoder>)enc;
    id<MTLBuffer> ib = (__bridge id<MTLBuffer>)indexBuf;
    [encoder drawIndexedPrimitives:(MTLPrimitiveType)type
                        indexCount:(NSUInteger)indexCount
                         indexType:MTLIndexTypeUInt32
                       indexBuffer:ib
                 indexBufferOffset:(NSUInteger)indexOffset
                     instanceCount:(NSUInteger)instanceCount
                        baseVertex:(NSInteger)baseVertex
                      baseInstance:0];
}

// ============================================================
// Compute Command Encoder
// ============================================================
void* metal_begin_compute_encoder(void* cmdBuf) {
    @autoreleasepool {
        id<MTLCommandBuffer> cb = (__bridge id<MTLCommandBuffer>)cmdBuf;
        id<MTLComputeCommandEncoder> enc = [cb computeCommandEncoder];
        return (__bridge_retained void*)enc;
    }
}

void metal_end_compute_encoder(void* enc) {
    id<MTLComputeCommandEncoder> encoder = (__bridge id<MTLComputeCommandEncoder>)enc;
    [encoder endEncoding];
    CFRelease(enc);
}

void metal_compute_set_pipeline(void* enc, void* ps) {
    id<MTLComputeCommandEncoder> encoder = (__bridge id<MTLComputeCommandEncoder>)enc;
    id<MTLComputePipelineState> state = (__bridge id<MTLComputePipelineState>)ps;
    [encoder setComputePipelineState:state];
}

void metal_compute_dispatch(void* enc, uint32_t groupsX, uint32_t groupsY, uint32_t groupsZ) {
    id<MTLComputeCommandEncoder> encoder = (__bridge id<MTLComputeCommandEncoder>)enc;
    [encoder dispatchThreadgroups:MTLSizeMake(groupsX, groupsY, groupsZ)
            threadsPerThreadgroup:MTLSizeMake(8, 8, 1)];
}

void metal_free_string(char* str) {
    free(str);
}

} // extern "C"

#endif // SOLRA_GPU_METAL && __APPLE__
