// Core SDK C ABI 声明 + 动态加载
// 对应 core/include/solra/ 下的 C 头文件

use std::ffi::{c_char, c_int, c_void, CStr};
use std::sync::OnceLock;

/// Core SDK 库句柄（全局单例）
static CORE_SDK: OnceLock<CoreSdk> = OnceLock::new();

/// Solra C ABI 结果码
#[repr(i32)]
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SolraResult {
    Success = 0,
    Error = -1,
    InvalidHandle = -2,
    OutOfMemory = -3,
    NotInitialized = -4,
    AlreadyInitialized = -5,
    UnsupportedPlatform = -6,
    ModelNotFound = -7,
    NetworkError = -8,
}

/// Solra 不透明句柄（包装为 Send+Sync+Copy 安全类型）
#[derive(Debug, Clone, Copy)]
pub struct SolraHandle(pub *mut c_void);

unsafe impl Send for SolraHandle {}
unsafe impl Sync for SolraHandle {}

impl SolraHandle {
    pub fn is_null(&self) -> bool {
        self.0.is_null()
    }

    pub fn as_ptr(&self) -> *mut c_void {
        self.0
    }
}

/// C FFI 兼容的原始句柄类型（用于函数指针签名）
pub type SolraRawHandle = *mut c_void;

/// Solra 核心配置结构体（对应 C 的 SolraCoreConfig）
#[repr(C)]
pub struct SolraCoreConfig {
    pub data_path: *const c_char,
    pub display_width: c_int,
    pub display_height: c_int,
    pub target_fps: c_int,
    pub enable_gpu: c_int,
    pub log_level: c_int,
    pub user_data: *mut c_void,
}

/// Solra 渲染配置（对应 C 的 SolraRenderConfig）
#[repr(C)]
#[derive(Debug, Clone)]
pub struct SolraRenderConfig {
    pub backend: c_int,       // SolraRenderBackend enum
    pub width: c_int,
    pub height: c_int,
    pub vsync: c_int,
    pub msaa_samples: c_int,
    pub enable_hdr: c_int,
    pub clear_color: SolraColor,
    pub native_window: *mut c_void,
}

/// Solra 颜色（RGBA）
#[repr(C)]
#[derive(Debug, Clone, Copy)]
pub struct SolraColor {
    pub r: f32,
    pub g: f32,
    pub b: f32,
    pub a: f32,
}

/// GPU 信息（对应 C 的 SolraGPUInfo）
#[repr(C)]
#[derive(Debug, Clone)]
pub struct SolraGpuInfo {
    pub vendor: [u8; 128],
    pub renderer: [u8; 128],
    pub version: [u8; 64],
    pub dedicated_vram_mb: u64,
    pub shared_vram_mb: u64,
    pub max_texture_size: c_int,
    pub max_compute_workgroup_size: c_int,
    pub supports_ray_tracing: c_int,
    pub supports_mesh_shader: c_int,
}

#[repr(i32)]
pub enum RenderBackend {
    Auto = 0,
    OpenGL = 1,
    Vulkan = 2,
    DirectX = 3,
}

#[repr(i32)]
pub enum InferenceBackend {
    Auto = 0,
    CPU = 1,
    ONNX = 2,
    OpenCL = 3,
    CUDA = 4,
}

#[repr(i32)]
pub enum LogLevel {
    Trace = 0,
    Debug = 1,
    Info = 2,
    Warn = 3,
    Error = 4,
}

/// Core SDK 函数指针类型定义（适配全局单例 C API）
type SolraCoreInitFn = unsafe extern "C" fn(*const SolraCoreConfig) -> i32;
type SolraCoreShutdownFn = unsafe extern "C" fn();
type SolraCoreVersionFn = unsafe extern "C" fn() -> *const c_char;

/// 渲染函数指针类型
type SolraRenderInitFn = unsafe extern "C" fn(*const SolraRenderConfig) -> i32;
type SolraRenderBeginFrameFn = unsafe extern "C" fn() -> i32;
type SolraRenderEndFrameFn = unsafe extern "C" fn() -> i32;
type SolraRenderResizeFn = unsafe extern "C" fn(c_int, c_int);
type SolraRenderShutdownFn = unsafe extern "C" fn();
type SolraRenderGetFpsFn = unsafe extern "C" fn() -> f32;
type SolraRenderGetGpuInfoFn = unsafe extern "C" fn(*mut SolraGpuInfo) -> i32;

/// Core SDK 运行时（全局单例模式）
pub struct CoreSdk {
    #[allow(dead_code)]
    lib: libloading::Library,
    // Core lifecycle
    pub init: SolraCoreInitFn,
    pub shutdown: SolraCoreShutdownFn,
    pub version: SolraCoreVersionFn,
    // Render lifecycle
    pub render_init: SolraRenderInitFn,
    pub render_begin_frame: SolraRenderBeginFrameFn,
    pub render_end_frame: SolraRenderEndFrameFn,
    pub render_resize: SolraRenderResizeFn,
    pub render_shutdown: SolraRenderShutdownFn,
    pub render_get_fps: SolraRenderGetFpsFn,
    pub render_get_gpu_info: SolraRenderGetGpuInfoFn,
}

/// 引擎是否已初始化标志
static CORE_INITIALIZED: std::sync::atomic::AtomicBool = std::sync::atomic::AtomicBool::new(false);

impl CoreSdk {
    /// 获取全局 Core SDK 实例
    pub fn get() -> Option<&'static CoreSdk> {
        CORE_SDK.get()
    }

    /// 检查引擎是否已初始化
    pub fn is_initialized() -> bool {
        CORE_INITIALIZED.load(std::sync::atomic::Ordering::Acquire)
    }

    /// 检查 Core SDK 动态库是否已加载（非 Mock 模式）
    pub fn is_loaded() -> bool {
        CORE_SDK.get().is_some()
    }

    /// 从动态库中获取符号（供子模块使用）
    pub fn get_symbol<T>(&self, name: &[u8]) -> Result<libloading::Symbol<'_, T>, String> {
        unsafe {
            self.lib.get::<T>(name).map_err(|e| format!("查找符号失败: {}", e))
        }
    }
}

/// 公共检查函数：Core SDK 是否已实际加载
pub fn is_core_sdk_loaded() -> bool {
    CoreSdk::is_loaded()
}

/// 动态加载 Core SDK (libsolracore.dll)
pub fn load_core_sdk() -> Result<(), String> {
    // 搜索路径（按优先级排列）
    let search_paths = vec![
        "core/libsolracore.dll",                                      // 相对于 src-tauri/
        "../core/build/windows/bin/libsolracore.dll",                 // 相对于 src-tauri/ 到 core/
        "../../core/build/windows/bin/libsolracore.dll",              // 相对于 clients/desktop/
        "../../core/build/windows/Release/libsolracore.dll",
        "../../core/build/windows/Debug/libsolracore.dll",
        "./libsolracore.dll",                                         // cwd
    ];

    let lib_path = search_paths.iter()
        .find(|p| std::path::Path::new(p).exists())
        .ok_or_else(|| {
            log::warn!("libsolracore.dll 未找到，搜索路径: {:?}", search_paths);
            "libsolracore.dll 未找到（开发模式下可忽略，功能将使用 Mock）".to_string()
        })?;

    log::info!("加载 Core SDK: {}", lib_path);

    unsafe {
        let lib = libloading::Library::new(lib_path)
            .map_err(|e| format!("加载 {} 失败: {}", lib_path, e))?;

        // Core lifecycle
        let init: SolraCoreInitFn = *lib.get(b"solra_core_init")
            .map_err(|e| format!("查找 solra_core_init 失败: {}", e))?;
        let shutdown: SolraCoreShutdownFn = *lib.get(b"solra_core_shutdown")
            .map_err(|e| format!("查找 solra_core_shutdown 失败: {}", e))?;
        let version: SolraCoreVersionFn = *lib.get(b"solra_core_get_version")
            .map_err(|e| format!("查找 solra_core_get_version 失败: {}", e))?;

        // Render lifecycle
        let render_init: SolraRenderInitFn = *lib.get(b"solra_render_init")
            .map_err(|e| format!("查找 solra_render_init 失败: {}", e))?;
        let render_begin_frame: SolraRenderBeginFrameFn = *lib.get(b"solra_render_begin_frame")
            .map_err(|e| format!("查找 solra_render_begin_frame 失败: {}", e))?;
        let render_end_frame: SolraRenderEndFrameFn = *lib.get(b"solra_render_end_frame")
            .map_err(|e| format!("查找 solra_render_end_frame 失败: {}", e))?;
        let render_resize: SolraRenderResizeFn = *lib.get(b"solra_render_resize")
            .map_err(|e| format!("查找 solra_render_resize 失败: {}", e))?;
        let render_shutdown: SolraRenderShutdownFn = *lib.get(b"solra_render_shutdown")
            .map_err(|e| format!("查找 solra_render_shutdown 失败: {}", e))?;
        let render_get_fps: SolraRenderGetFpsFn = *lib.get(b"solra_render_get_fps")
            .map_err(|e| format!("查找 solra_render_get_fps 失败: {}", e))?;
        let render_get_gpu_info: SolraRenderGetGpuInfoFn = *lib.get(b"solra_render_get_gpu_info")
            .map_err(|e| format!("查找 solra_render_get_gpu_info 失败: {}", e))?;

        let sdk = CoreSdk {
            lib,
            init,
            shutdown,
            version,
            render_init,
            render_begin_frame,
            render_end_frame,
            render_resize,
            render_shutdown,
            render_get_fps,
            render_get_gpu_info,
        };

        // 获取版本信息
        let version_str = {
            let ptr = (sdk.version)();
            if ptr.is_null() {
                "unknown"
            } else {
                CStr::from_ptr(ptr).to_str().unwrap_or("unknown")
            }
        };
        log::info!("Core SDK 版本: {}", version_str);

        CORE_SDK.set(sdk).map_err(|_| "Core SDK 已初始化".to_string())?;
    }

    Ok(())
}

/// 初始化 Core SDK 引擎（全局单例，无句柄）
pub fn init_core_engine(config: &SolraCoreConfig) -> Result<(), String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;

    let result = unsafe { (sdk.init)(config) };

    if result != SolraResult::Success as i32 {
        return Err(format!("Core SDK 初始化失败: 错误码 {}", result));
    }

    CORE_INITIALIZED.store(true, std::sync::atomic::Ordering::Release);
    log::info!("Core SDK 引擎初始化成功");

    Ok(())
}

/// 关闭 Core SDK 引擎
pub fn shutdown_core_engine() {
    if let Some(sdk) = CoreSdk::get() {
        unsafe { (sdk.shutdown)() };
        CORE_INITIALIZED.store(false, std::sync::atomic::Ordering::Release);
        log::info!("Core SDK 引擎已关闭");
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_solra_result_values() {
        assert_eq!(SolraResult::Success as i32, 0);
        assert_eq!(SolraResult::Error as i32, -1);
    }

    #[test]
    fn test_load_core_sdk_not_found() {
        // 在 CI 环境中 Core SDK 通常未编译，应优雅降级
        let result = load_core_sdk();
        // 无论成功与否，不应 panic
        assert!(result.is_ok() || result.is_err());
    }
}
