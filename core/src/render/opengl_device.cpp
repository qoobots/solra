// OpenGL 4.6 backend implementation
#include "opengl_device.hpp"
#include <spdlog/spdlog.h>
#include <cstring>
#include <stdexcept>
#include <algorithm>

// ============================================================
// Platform-specific GL context creation
// ============================================================
#if defined(_WIN32)
#include <GL/gl.h>
#include <GL/glext.h>
#include <GL/wglext.h>
#pragma comment(lib, "opengl32.lib")

static PFNWGLCREATECONTEXTATTRIBSARBPROC wglCreateContextAttribsARB = nullptr;

static bool load_wgl_extensions() {
    // Create a temporary context to load WGL extensions
    WNDCLASSA wc = {};
    wc.lpfnWndProc = DefWindowProcA;
    wc.hInstance = GetModuleHandle(nullptr);
    wc.lpszClassName = "SolraGLTemp";
    RegisterClassA(&wc);
    HWND tmpWnd = CreateWindowA("SolraGLTemp", "", 0, 0, 0, 1, 1, nullptr, nullptr, wc.hInstance, nullptr);
    HDC tmpDC = GetDC(tmpWnd);
    PIXELFORMATDESCRIPTOR pfd = {sizeof(PIXELFORMATDESCRIPTOR), 1,
        PFD_DRAW_TO_WINDOW | PFD_SUPPORT_OPENGL | PFD_DOUBLEBUFFER, PFD_TYPE_RGBA,
        32, 0,0,0,0,0,0, 0,0,0,0,0,0,0, 24,8,0, PFD_MAIN_PLANE, 0,0,0,0};
    SetPixelFormat(tmpDC, ChoosePixelFormat(tmpDC, &pfd), &pfd);
    HGLRC tmpRC = wglCreateContext(tmpDC);
    wglMakeCurrent(tmpDC, tmpRC);
    wglCreateContextAttribsARB = (PFNWGLCREATECONTEXTATTRIBSARBPROC)wglGetProcAddress("wglCreateContextAttribsARB");
    wglMakeCurrent(nullptr, nullptr);
    wglDeleteContext(tmpRC);
    ReleaseDC(tmpWnd, tmpDC);
    DestroyWindow(tmpWnd);
    return wglCreateContextAttribsARB != nullptr;
}

#elif defined(__APPLE__)
#include <OpenGL/gl3.h>
#include <OpenGL/gl3ext.h>
#include <dlfcn.h>
#else
#include <GL/gl.h>
#include <GL/glext.h>
#include <GL/glx.h>
#include <X11/Xlib.h>
#include <dlfcn.h>
#endif

// GL function loading (simplified - production code would use glad/glad2)
#ifndef __APPLE__
static void* gl_get_proc(const char* name) {
#if defined(_WIN32)
    void* proc = (void*)wglGetProcAddress(name);
    if (!proc) proc = (void*)GetProcAddress(GetModuleHandleA("opengl32.dll"), name);
    return proc;
#else
    return (void*)glXGetProcAddress((const GLubyte*)name);
#endif
}
#endif

namespace solra::render {

// ============================================================
// OpenGLDevice
// ============================================================

OpenGLDevice::OpenGLDevice() = default;

OpenGLDevice::~OpenGLDevice() {
    shutdown();
}

bool OpenGLDevice::initialize() {
    if (initialized_) return true;

#if defined(_WIN32)
    if (!load_wgl_extensions()) {
        spdlog::error("OpenGL: Failed to load WGL extensions");
        return false;
    }

    // Create dummy window for off-screen context
    HINSTANCE hInst = GetModuleHandle(nullptr);
    WNDCLASSA wc = {};
    wc.lpfnWndProc = DefWindowProcA;
    wc.hInstance = hInst;
    wc.lpszClassName = "SolraGLOffscreen";
    RegisterClassA(&wc);

    dummy_window_ = CreateWindowA("SolraGLOffscreen", "Solra GL", 0,
        CW_USEDEFAULT, CW_USEDEFAULT, 1, 1, nullptr, nullptr, hInst, nullptr);
    device_context_ = GetDC(dummy_window_);

    // Set pixel format
    PIXELFORMATDESCRIPTOR pfd = {};
    pfd.nSize = sizeof(pfd);
    pfd.nVersion = 1;
    pfd.dwFlags = PFD_DRAW_TO_WINDOW | PFD_SUPPORT_OPENGL | PFD_DOUBLEBUFFER;
    pfd.iPixelType = PFD_TYPE_RGBA;
    pfd.cColorBits = 32;
    pfd.cDepthBits = 24;
    pfd.cStencilBits = 8;
    pfd.iLayerType = PFD_MAIN_PLANE;
    int pf = ChoosePixelFormat(device_context_, &pfd);
    SetPixelFormat(device_context_, pf, &pfd);

    // Create GL 4.6 core context
    int attribs[] = {
        0x2091, 4,      // WGL_CONTEXT_MAJOR_VERSION_ARB
        0x2092, 6,      // WGL_CONTEXT_MINOR_VERSION_ARB
        0x9126, 0x0001, // WGL_CONTEXT_PROFILE_MASK_ARB → core
        0x200B, 0,      // WGL_CONTEXT_FLAGS_ARB → debug off
        0
    };
    gl_context_ = wglCreateContextAttribsARB(device_context_, nullptr, attribs);
    if (!gl_context_) {
        spdlog::warn("OpenGL 4.6 core failed, trying compatibility");
        gl_context_ = wglCreateContext(device_context_);
    }
    if (!gl_context_) {
        spdlog::error("OpenGL: Failed to create any GL context");
        return false;
    }
    wglMakeCurrent(device_context_, gl_context_);

#elif defined(__APPLE__)
    // macOS: CGL off-screen context
    CGLPixelFormatAttribute attrs[] = {
        kCGLPFAOpenGLProfile, (CGLPixelFormatAttribute)kCGLOGLPVersion_GL4_Core,
        kCGLPFAAccelerated,
        kCGLPFAColorSize, (CGLPixelFormatAttribute)32,
        kCGLPFADepthSize, (CGLPixelFormatAttribute)24,
        kCGLPFAStencilSize, (CGLPixelFormatAttribute)8,
        kCGLPFAOffScreen,
        (CGLPixelFormatAttribute)0
    };
    CGLPixelFormatObj pix;
    GLint npix;
    CGLChoosePixelFormat(attrs, &pix, &npix);
    if (!pix) {
        spdlog::error("OpenGL: Failed to choose pixel format");
        return false;
    }
    CGLCreateContext(pix, nullptr, (CGLContextObj*)&gl_context_);
    CGLDestroyPixelFormat(pix);
    if (!gl_context_) {
        spdlog::error("OpenGL: Failed to create CGL context");
        return false;
    }
    CGLSetCurrentContext((CGLContextObj)gl_context_);

#else
    // Linux: GLX off-screen context
    display_ = XOpenDisplay(nullptr);
    if (!display_) {
        spdlog::error("OpenGL: Failed to open X display");
        return false;
    }

    static int visAttribs[] = {
        GLX_RENDER_TYPE, GLX_RGBA_BIT,
        GLX_DRAWABLE_TYPE, GLX_PBUFFER_BIT,
        GLX_RED_SIZE, 8, GLX_GREEN_SIZE, 8, GLX_BLUE_SIZE, 8, GLX_ALPHA_SIZE, 8,
        GLX_DEPTH_SIZE, 24, GLX_STENCIL_SIZE, 8,
        GLX_DOUBLEBUFFER, True,
        None
    };

    int fbcount;
    GLXFBConfig* fbc = glXChooseFBConfig((Display*)display_, DefaultScreen((Display*)display_), visAttribs, &fbcount);
    if (!fbc || fbcount == 0) {
        spdlog::error("OpenGL: No suitable FB config");
        return false;
    }

    int ctxAttribs[] = {
        GLX_CONTEXT_MAJOR_VERSION_ARB, 4,
        GLX_CONTEXT_MINOR_VERSION_ARB, 6,
        GLX_CONTEXT_PROFILE_MASK_ARB, GLX_CONTEXT_CORE_PROFILE_BIT_ARB,
        None
    };
    typedef GLXContext (*glXCreateContextAttribsARBProc)(Display*, GLXFBConfig, GLXContext, Bool, const int*);
    auto createCtx = (glXCreateContextAttribsARBProc)glXGetProcAddress((const GLubyte*)"glXCreateContextAttribsARB");

    if (createCtx) {
        gl_context_ = createCtx((Display*)display_, fbc[0], nullptr, True, ctxAttribs);
    }
    if (!gl_context_) {
        spdlog::error("OpenGL: Failed to create GLX context");
        XFree(fbc);
        return false;
    }

    // Create pbuffer
    int pbAttribs[] = {GLX_PBUFFER_WIDTH, 1, GLX_PBUFFER_HEIGHT, 1, None};
    dummy_window_ = glXCreatePbuffer((Display*)display_, fbc[0], pbAttribs);
    XFree(fbc);

    glXMakeContextCurrent((Display*)display_, (GLXDrawable)dummy_window_, (GLXDrawable)dummy_window_, (GLXContext)gl_context_);
#endif

    // Query GPU info
    const char* vendor = (const char*)glGetString(GL_VENDOR);
    const char* renderer = (const char*)glGetString(GL_RENDERER);
    const char* version = (const char*)glGetString(GL_VERSION);

    vendor_name_ = vendor ? vendor : "Unknown";
    renderer_name_ = renderer ? renderer : "Unknown";
    gl_version_ = version ? version : "Unknown";

    spdlog::info("OpenGL device initialized:");
    spdlog::info("  Vendor:   {}", vendor_name_);
    spdlog::info("  Renderer: {}", renderer_name_);
    spdlog::info("  Version:  {}", gl_version_);

    initialized_ = true;
    return true;
}

void OpenGLDevice::shutdown() {
    if (!initialized_) return;

#if defined(_WIN32)
    if (gl_context_) {
        wglMakeCurrent(nullptr, nullptr);
        wglDeleteContext(gl_context_);
        gl_context_ = nullptr;
    }
    if (device_context_) {
        ReleaseDC(dummy_window_, device_context_);
        device_context_ = nullptr;
    }
    if (dummy_window_) {
        DestroyWindow(dummy_window_);
        dummy_window_ = nullptr;
    }
#elif defined(__APPLE__)
    if (gl_context_) {
        CGLSetCurrentContext(nullptr);
        CGLDestroyContext((CGLContextObj)gl_context_);
        gl_context_ = nullptr;
    }
#else
    if (gl_context_) {
        glXMakeContextCurrent((Display*)display_, None, None, nullptr);
        glXDestroyContext((Display*)display_, (GLXContext)gl_context_);
        gl_context_ = nullptr;
    }
    if (dummy_window_) {
        glXDestroyPbuffer((Display*)display_, (GLXDrawable)dummy_window_);
        dummy_window_ = nullptr;
    }
    if (display_) {
        XCloseDisplay((Display*)display_);
        display_ = nullptr;
    }
#endif

    spdlog::info("OpenGL device shutdown");
    initialized_ = false;
}

std::string OpenGLDevice::name() const {
    return "OpenGL 4.6 - " + renderer_name_;
}

Backend OpenGLDevice::backend() const {
    return Backend::OpenGLES;
}

std::shared_ptr<GpuBuffer> OpenGLDevice::createBuffer(const BufferDesc& desc, const void* data) {
    uint32_t target = 0;
    switch (desc.usage) {
        case BufferUsage::Vertex:  target = 0x8892; break; // GL_ARRAY_BUFFER
        case BufferUsage::Index:   target = 0x8893; break; // GL_ELEMENT_ARRAY_BUFFER
        case BufferUsage::Uniform: target = 0x8A11; break; // GL_UNIFORM_BUFFER
        case BufferUsage::Storage: target = 0x90D2; break; // GL_SHADER_STORAGE_BUFFER
        case BufferUsage::Staging: target = 0x88EB; break; // GL_PIXEL_PACK_BUFFER (reuse)
    }
    return std::make_shared<OpenGLBuffer>(target, desc, data, this);
}

std::shared_ptr<GpuTexture> OpenGLDevice::createTexture(const TextureDesc& desc) {
    return std::make_shared<OpenGLTexture>(desc, this);
}

std::shared_ptr<GpuShader> OpenGLDevice::createShader(ShaderStage stage, const std::vector<uint32_t>& spirv) {
    return std::make_shared<OpenGLShader>(stage, spirv, this);
}

std::shared_ptr<GpuPipeline> OpenGLDevice::createPipeline(const PipelineDesc& desc) {
    return std::make_shared<OpenGLPipeline>(desc, this);
}

std::shared_ptr<GpuCommandBuffer> OpenGLDevice::createCommandBuffer() {
    return std::make_shared<OpenGLCommandBuffer>(this);
}

void OpenGLDevice::submit(std::shared_ptr<GpuCommandBuffer> cmd) {
    // OpenGL executes immediately; glFlush for explicit submission
    glFlush();
}

void OpenGLDevice::present() {
    // Off-screen: no swap buffers needed
    glFlush();
}

void OpenGLDevice::waitIdle() {
    glFinish();
}

uint64_t OpenGLDevice::gpuMemoryUsed() const {
    return memory_used_;
}

uint64_t OpenGLDevice::gpuMemoryBudget() const {
    return memory_budget_;
}

void OpenGLDevice::trackAllocation(uint64_t bytes) {
    std::lock_guard<std::mutex> lock(resource_mutex_);
    memory_used_ += bytes;
}

void OpenGLDevice::trackDeallocation(uint64_t bytes) {
    std::lock_guard<std::mutex> lock(resource_mutex_);
    if (bytes <= memory_used_) memory_used_ -= bytes;
}

// ============================================================
// OpenGLBuffer
// ============================================================

OpenGLBuffer::OpenGLBuffer(uint32_t target, const BufferDesc& desc, const void* data, OpenGLDevice* device)
    : target_(target), size_(desc.size), device_(device) {
    glGenBuffers(1, &handle_);
    glBindBuffer(target_, handle_);
    glBufferData(target_, (GLsizeiptr)size_, data,
                 desc.hostVisible ? 0x88E8 : 0x88E4); // DYNAMIC_DRAW : STATIC_DRAW
    device_->trackAllocation(size_);
}

OpenGLBuffer::~OpenGLBuffer() {
    if (handle_) {
        glDeleteBuffers(1, &handle_);
        device_->trackDeallocation(size_);
    }
}

void* OpenGLBuffer::map() {
    glBindBuffer(target_, handle_);
    void* ptr = glMapBuffer(target_, 0x88BA); // GL_READ_WRITE
    mapped_ = (ptr != nullptr);
    return ptr;
}

void OpenGLBuffer::unmap() {
    glBindBuffer(target_, handle_);
    glUnmapBuffer(target_);
    mapped_ = false;
}

// ============================================================
// OpenGLTexture
// ============================================================

OpenGLTexture::OpenGLTexture(const TextureDesc& desc, OpenGLDevice* device)
    : width_(desc.width), height_(desc.height), format_(desc.format), device_(device) {

    target_ = (desc.depth > 1) ? 0x806F : 0x0DE1; // GL_TEXTURE_3D : GL_TEXTURE_2D
    glGenTextures(1, &handle_);
    glBindTexture(target_, handle_);

    // Determine internal format
    uint32_t internalFmt = 0x8058; // GL_RGBA8
    uint32_t dataFmt = 0x1908;     // GL_RGBA
    uint32_t dataType = 0x1401;    // GL_UNSIGNED_BYTE

    switch (desc.format) {
        case TextureFormat::RGBA8:
            internalFmt = 0x8058; dataFmt = 0x1908; break;
        case TextureFormat::BGRA8:
            internalFmt = 0x8058; dataFmt = 0x80E1; break;
        case TextureFormat::Depth32F:
            internalFmt = 0x8CAC; dataFmt = 0x1902; dataType = 0x1406; break;
        case TextureFormat::Depth24Stencil8:
            internalFmt = 0x88F0; dataFmt = 0x84F9; dataType = 0x84FA; break;
        default: break;
    }

    glTexImage2D(target_, 0, internalFmt, width_, height_, 0, dataFmt, dataType, nullptr);

    // Default sampling
    glTexParameteri(target_, 0x2801, 0x2601); // GL_TEXTURE_MIN_FILTER → LINEAR_MIPMAP_LINEAR
    glTexParameteri(target_, 0x2800, 0x2601); // GL_TEXTURE_MAG_FILTER → LINEAR
    glTexParameteri(target_, 0x2802, 0x812F); // GL_TEXTURE_WRAP_S → CLAMP_TO_EDGE
    glTexParameteri(target_, 0x2803, 0x812F); // GL_TEXTURE_WRAP_T → CLAMP_TO_EDGE

    // Estimate memory
    uint64_t bytes = (uint64_t)width_ * height_ * 4;
    device_->trackAllocation(bytes);
}

OpenGLTexture::~OpenGLTexture() {
    if (handle_) {
        glDeleteTextures(1, &handle_);
        uint64_t bytes = (uint64_t)width_ * height_ * 4;
        device_->trackDeallocation(bytes);
    }
}

void OpenGLTexture::bind(uint32_t slot) {
    glActiveTexture(0x84C0 + slot); // GL_TEXTURE0 + slot
    glBindTexture(target_, handle_);
}

// ============================================================
// OpenGLShader
// ============================================================

OpenGLShader::OpenGLShader(ShaderStage stage, const std::vector<uint32_t>& spirv, OpenGLDevice* device)
    : stage_(stage), device_(device) {
    // SPIR-V binary → GL shader (requires GL_ARB_gl_spirv, OpenGL 4.6 core)
    uint32_t glStage = 0;
    switch (stage) {
        case ShaderStage::Vertex:   glStage = 0x8B31; break; // GL_VERTEX_SHADER
        case ShaderStage::Fragment: glStage = 0x8B30; break; // GL_FRAGMENT_SHADER
        case ShaderStage::Compute:  glStage = 0x91B9; break; // GL_COMPUTE_SHADER
        case ShaderStage::Geometry: glStage = 0x8DD9; break; // GL_GEOMETRY_SHADER
    }

    handle_ = glCreateShader(glStage);
    if (handle_ && !spirv.empty()) {
        glShaderBinary(1, &handle_, 0x8DFA, spirv.data(), (GLsizei)(spirv.size() * sizeof(uint32_t)));
        glSpecializeShader(handle_, "main", 0, nullptr, nullptr);
    }
}

OpenGLShader::~OpenGLShader() {
    if (handle_) glDeleteShader(handle_);
}

std::shared_ptr<OpenGLShader> OpenGLShader::compileFromSource(
    ShaderStage stage, const std::string& glslSource, OpenGLDevice* device) {

    uint32_t glStage = 0;
    switch (stage) {
        case ShaderStage::Vertex:   glStage = 0x8B31; break;
        case ShaderStage::Fragment: glStage = 0x8B30; break;
        case ShaderStage::Compute:  glStage = 0x91B9; break;
        case ShaderStage::Geometry: glStage = 0x8DD9; break;
    }

    auto shader = std::shared_ptr<OpenGLShader>(new OpenGLShader(stage, {}, device));
    shader->handle_ = glCreateShader(glStage);

    const char* src = glslSource.c_str();
    GLint len = (GLint)glslSource.size();
    glShaderSource(shader->handle_, 1, &src, &len);
    glCompileShader(shader->handle_);

    GLint compiled = 0;
    glGetShaderiv(shader->handle_, 0x8B81, &compiled); // GL_COMPILE_STATUS
    if (!compiled) {
        char log[1024];
        glGetShaderInfoLog(shader->handle_, sizeof(log), nullptr, log);
        spdlog::error("OpenGL shader compile failed: {}", log);
        return nullptr;
    }

    return shader;
}

// ============================================================
// OpenGLPipeline
// ============================================================

OpenGLPipeline::OpenGLPipeline(const PipelineDesc& desc, OpenGLDevice* device)
    : desc_(desc), device_(device) {

    auto vs = std::dynamic_pointer_cast<OpenGLShader>(desc.vertexShader);
    auto fs = std::dynamic_pointer_cast<OpenGLShader>(desc.fragmentShader);

    if (vs && fs) {
        if (!linkProgram(vs, fs)) {
            spdlog::error("OpenGL: Failed to link pipeline program");
        }
    }
}

OpenGLPipeline::~OpenGLPipeline() {
    if (program_) glDeleteProgram(program_);
}

bool OpenGLPipeline::linkProgram(std::shared_ptr<OpenGLShader> vs, std::shared_ptr<OpenGLShader> fs) {
    program_ = glCreateProgram();
    glAttachShader(program_, vs->handle());
    glAttachShader(program_, fs->handle());
    glLinkProgram(program_);

    GLint linked = 0;
    glGetProgramiv(program_, 0x8B82, &linked); // GL_LINK_STATUS
    if (!linked) {
        char log[1024];
        glGetProgramInfoLog(program_, sizeof(log), nullptr, log);
        spdlog::error("OpenGL program link failed: {}", log);
        glDeleteProgram(program_);
        program_ = 0;
        return false;
    }
    return true;
}

void OpenGLPipeline::bind() {
    glUseProgram(program_);

    // Apply pipeline state
    if (desc_.depthTest) {
        glEnable(0x0B71); // GL_DEPTH_TEST
    } else {
        glDisable(0x0B71);
    }
    glDepthMask(desc_.depthWrite ? 0x1702 : 0x1701); // GL_TRUE : GL_FALSE

    uint32_t depthFunc = 0x0203; // GL_LEQUAL default
    switch (desc_.depthCompare) {
        case CompareOp::Never:        depthFunc = 0x0200; break;
        case CompareOp::Less:         depthFunc = 0x0201; break;
        case CompareOp::Equal:        depthFunc = 0x0202; break;
        case CompareOp::LessEqual:    depthFunc = 0x0203; break;
        case CompareOp::Greater:      depthFunc = 0x0204; break;
        case CompareOp::NotEqual:     depthFunc = 0x0205; break;
        case CompareOp::GreaterEqual: depthFunc = 0x0206; break;
        case CompareOp::Always:       depthFunc = 0x0207; break;
    }
    glDepthFunc(depthFunc);

    if (desc_.blending) {
        glEnable(0x0BE2); // GL_BLEND
        uint32_t srcBlend = 0x0302, dstBlend = 0x0303; // GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA
        auto glBlend = [](BlendFactor f) -> uint32_t {
            switch (f) {
                case BlendFactor::Zero:              return 0;
                case BlendFactor::One:               return 1;
                case BlendFactor::SrcAlpha:          return 0x0302;
                case BlendFactor::OneMinusSrcAlpha:  return 0x0303;
                case BlendFactor::DstAlpha:          return 0x0304;
                case BlendFactor::OneMinusDstAlpha:  return 0x0305;
                default: return 0x0302;
            }
        };
        glBlendFunc(glBlend(desc_.srcBlend), glBlend(desc_.dstBlend));
    } else {
        glDisable(0x0BE2);
    }

    // Culling: back-face cull by default for triangles
    glEnable(0x0B44); // GL_CULL_FACE
    glCullFace(0x0405); // GL_BACK
    glFrontFace(0x0901); // GL_CCW
}

// ============================================================
// OpenGLCommandBuffer
// ============================================================

OpenGLCommandBuffer::OpenGLCommandBuffer(OpenGLDevice* device)
    : device_(device) {}

OpenGLCommandBuffer::~OpenGLCommandBuffer() {
    if (current_vao_) glDeleteVertexArrays(1, &current_vao_);
    if (skinning_ubo_) glDeleteBuffers(1, &skinning_ubo_);
}

uint32_t OpenGLCommandBuffer::createVAO() {
    uint32_t vao = 0;
    glGenVertexArrays(1, &vao);
    return vao;
}

void OpenGLCommandBuffer::begin() {
    recording_ = true;
    if (!current_vao_) current_vao_ = createVAO();
    glBindVertexArray(current_vao_);
}

void OpenGLCommandBuffer::end() {
    recording_ = false;
    glBindVertexArray(0);
}

void OpenGLCommandBuffer::bindPipeline(std::shared_ptr<GpuPipeline> pipeline) {
    current_pipeline_ = std::dynamic_pointer_cast<OpenGLPipeline>(pipeline);
    if (current_pipeline_) {
        current_pipeline_->bind();
    }
}

void OpenGLCommandBuffer::bindVertexBuffer(std::shared_ptr<GpuBuffer> buffer, uint64_t offset, bool skinned) {
    auto glBuf = std::dynamic_pointer_cast<OpenGLBuffer>(buffer);
    if (!glBuf) return;
    glBindBuffer(glBuf->target(), glBuf->handle());

    if (skinned) {
        // Skinned vertex layout: pos3 + normal3 + uv2 + boneWeights4 + boneIndices4 = 12 floats = 48 bytes
        const int stride = 48;
        glVertexAttribPointer(0, 3, 0x1406, 0x1702, stride, (void*)(uintptr_t)offset);      // position
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, 0x1406, 0x1702, stride, (void*)(uintptr_t)(offset + 12)); // normal
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, 0x1406, 0x1702, stride, (void*)(uintptr_t)(offset + 24)); // uv
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(3, 4, 0x1406, 0x1702, stride, (void*)(uintptr_t)(offset + 32)); // boneWeights
        glEnableVertexAttribArray(3);
        glVertexAttribIPointer(4, 4, 0x1405, stride, (void*)(uintptr_t)(offset + 48));         // boneIndices (uint)
        glEnableVertexAttribArray(4);
    } else {
        // Standard interleaved: pos3 + normal3 + uv2 = 8 floats = 32 bytes stride
        const int stride = 32;
        glVertexAttribPointer(0, 3, 0x1406, 0x1702, stride, (void*)(uintptr_t)offset);      // position
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, 0x1406, 0x1702, stride, (void*)(uintptr_t)(offset + 12)); // normal
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, 0x1406, 0x1702, stride, (void*)(uintptr_t)(offset + 24)); // uv
        glEnableVertexAttribArray(2);
        // Disable skinning attributes
        glDisableVertexAttribArray(3);
        glDisableVertexAttribArray(4);
    }
}

void OpenGLCommandBuffer::bindIndexBuffer(std::shared_ptr<GpuBuffer> buffer, uint64_t offset) {
    auto glBuf = std::dynamic_pointer_cast<OpenGLBuffer>(buffer);
    if (glBuf) {
        glBindBuffer(glBuf->target(), glBuf->handle());
    }
}

void OpenGLCommandBuffer::draw(uint32_t vertexCount, uint32_t instanceCount,
                                uint32_t firstVertex, uint32_t firstInstance) {
    if (instanceCount > 1) {
        glDrawArraysInstanced(0x0004, (GLint)firstVertex, (GLsizei)vertexCount, (GLsizei)instanceCount); // GL_TRIANGLES
    } else {
        glDrawArrays(0x0004, (GLint)firstVertex, (GLsizei)vertexCount);
    }
}

void OpenGLCommandBuffer::drawIndexed(uint32_t indexCount, uint32_t instanceCount,
                                       uint32_t firstIndex, int32_t vertexOffset,
                                       uint32_t firstInstance) {
    uint32_t indexType = 0x1405; // GL_UNSIGNED_INT (we always use uint32)
    void* offsetPtr = (void*)(uintptr_t)(firstIndex * sizeof(uint32_t));
    if (instanceCount > 1) {
        glDrawElementsInstanced(0x0004, (GLsizei)indexCount, indexType, offsetPtr, (GLsizei)instanceCount);
    } else {
        glDrawElements(0x0004, (GLsizei)indexCount, indexType, offsetPtr);
    }
}

void OpenGLCommandBuffer::dispatch(uint32_t groupsX, uint32_t groupsY, uint32_t groupsZ) {
    glDispatchCompute(groupsX, groupsY, groupsZ);
}

void OpenGLCommandBuffer::setViewport(int x, int y, int w, int h) {
    glViewport(x, y, w, h);
}

void OpenGLCommandBuffer::setClearColor(float r, float g, float b, float a) {
    glClearColor(r, g, b, a);
}

void OpenGLCommandBuffer::clear(bool color, bool depth, bool stencil) {
    uint32_t mask = 0;
    if (color)   mask |= 0x4000;  // GL_COLOR_BUFFER_BIT
    if (depth)   mask |= 0x0100;  // GL_DEPTH_BUFFER_BIT
    if (stencil) mask |= 0x0400;  // GL_STENCIL_BUFFER_BIT
    glClear(mask);
}

void OpenGLCommandBuffer::setUniformMat4(int location, const float* data) {
    glUniformMatrix4fv(location, 1, 0x1702, data); // GL_FALSE
}

void OpenGLCommandBuffer::setUniformVec4(int location, const float* data) {
    glUniform4fv(location, 1, data);
}

void OpenGLCommandBuffer::setUniformVec3(int location, const float* data) {
    glUniform3fv(location, 1, data);
}

void OpenGLCommandBuffer::setUniformFloat(int location, float value) {
    glUniform1f(location, value);
}

int OpenGLCommandBuffer::getUniformLocation(const std::string& name) {
    if (!current_pipeline_) return -1;
    return glGetUniformLocation(current_pipeline_->handle(), name.c_str());
}

int OpenGLCommandBuffer::getUniformBlockIndex(const std::string& name) {
    if (!current_pipeline_) return -1;
    return glGetUniformBlockIndex(current_pipeline_->handle(), name.c_str());
}

void OpenGLCommandBuffer::bindUniformBlock(int blockIndex, int bindingPoint) {
    if (blockIndex < 0) return;
    glUniformBlockBinding(current_pipeline_->handle(), blockIndex, bindingPoint);
}

void OpenGLCommandBuffer::ensureSkinningUBO() {
    if (skinning_ubo_ == 0) {
        glGenBuffers(1, &skinning_ubo_);
        glBindBuffer(0x8A11, skinning_ubo_); // GL_UNIFORM_BUFFER
        // Allocate for 128 bones * 16 floats * 4 bytes = 8192 bytes
        glBufferData(0x8A11, 128 * 16 * sizeof(float), nullptr, 0x88E4); // GL_DYNAMIC_DRAW
        glBindBuffer(0x8A11, 0);
    }
}

void OpenGLCommandBuffer::uploadSkinningMatrices(const float* matrices, int matrixCount) {
    ensureSkinningUBO();
    // Bind UBO to binding point 0 (matches the shader's SkinningBlock)
    glBindBufferBase(0x8A11, 0, skinning_ubo_);
    glBindBuffer(0x8A11, skinning_ubo_);
    int uploadCount = matrixCount < 128 ? matrixCount : 128;
    glBufferSubData(0x8A11, 0, uploadCount * 16 * sizeof(float), matrices);
    glBindBuffer(0x8A11, 0);
}

// ============================================================
// Factory
// ============================================================

std::shared_ptr<GpuDevice> createOpenGLESDevice() {
    auto device = std::make_shared<OpenGLDevice>();
    if (!device->initialize()) {
        spdlog::error("Failed to initialize OpenGL device");
        return nullptr;
    }
    return device;
}

} // namespace solra::render
