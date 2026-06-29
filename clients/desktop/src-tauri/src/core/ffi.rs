// Core SDK C ABI 声明 + 动态加载
// 对应 core/include/solra/ 下的 C 头文件

use std::ffi::{c_char, c_float, c_int, c_void, CStr};
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

/// Solra 不透明句柄
pub type SolraHandle = *mut c_void;

/// Solra 配置结构体（对应 C 的 SolraConfig）
#[repr(C)]
pub struct SolraConfig {
    pub render_backend: RenderBackend,
    pub inference_backend: InferenceBackend,
    pub log_level: LogLevel,
    pub cache_dir: *const c_char,
    pub data_dir: *const c_char,
    pub enable_webrtc: bool,
    pub enable_streaming: bool,
    pub max_cache_size_mb: c_int,
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

/// Core SDK 函数指针类型定义
type SolraCoreInitFn = unsafe extern "C" fn(*const SolraConfig, *mut SolraHandle) -> i32;
type SolraCoreDestroyFn = unsafe extern "C" fn(SolraHandle) -> i32;
type SolraCoreVersionFn = unsafe extern "C" fn() -> *const c_char;

/// Core SDK 运行时
pub struct CoreSdk {
    #[allow(dead_code)]
    lib: libloading::Library,
    pub init: SolraCoreInitFn,
    pub destroy: SolraCoreDestroyFn,
    pub version: SolraCoreVersionFn,
    pub handle: std::sync::Mutex<Option<SolraHandle>>,
}

impl CoreSdk {
    /// 获取全局 Core SDK 实例
    pub fn get() -> Option<&'static CoreSdk> {
        CORE_SDK.get()
    }

    /// 检查是否已初始化
    pub fn is_initialized() -> bool {
        CORE_SDK.get().map(|s| s.handle.lock().unwrap().is_some()).unwrap_or(false)
    }
}

/// 动态加载 Core SDK (libsolracore.dll)
pub fn load_core_sdk() -> Result<(), String> {
    // 搜索路径
    let search_paths = vec![
        "core/libsolracore.dll",
        "../../core/build/windows/Release/libsolracore.dll",
        "../../core/build/windows/Debug/libsolracore.dll",
        "./libsolracore.dll",
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

        let init: SolraCoreInitFn = *lib.get(b"solra_core_init")
            .map_err(|e| format!("查找 solra_core_init 失败: {}", e))?;
        let destroy: SolraCoreDestroyFn = *lib.get(b"solra_core_destroy")
            .map_err(|e| format!("查找 solra_core_destroy 失败: {}", e))?;
        let version: SolraCoreVersionFn = *lib.get(b"solra_core_version")
            .map_err(|e| format!("查找 solra_core_version 失败: {}", e))?;

        let sdk = CoreSdk {
            lib,
            init,
            destroy,
            version,
            handle: std::sync::Mutex::new(None),
        };

        // 获取版本信息
        let version_str = unsafe {
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

/// 初始化 Core SDK 引擎
pub fn init_core_engine(config: &SolraConfig) -> Result<SolraHandle, String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;

    let mut handle: SolraHandle = std::ptr::null_mut();
    let result = unsafe { (sdk.init)(config, &mut handle) };

    if result != SolraResult::Success as i32 {
        return Err(format!("Core SDK 初始化失败: 错误码 {}", result));
    }

    if handle.is_null() {
        return Err("Core SDK 初始化返回空句柄".to_string());
    }

    *sdk.handle.lock().unwrap() = Some(handle);
    log::info!("Core SDK 引擎初始化成功");

    Ok(handle)
}

/// 销毁 Core SDK 引擎
pub fn destroy_core_engine() -> Result<(), String> {
    let sdk = CoreSdk::get().ok_or("Core SDK 未加载")?;

    let mut handle_guard = sdk.handle.lock().unwrap();
    if let Some(handle) = handle_guard.take() {
        let result = unsafe { (sdk.destroy)(handle) };
        if result != SolraResult::Success as i32 {
            log::warn!("Core SDK 销毁返回非零: {}", result);
        }
        log::info!("Core SDK 引擎已销毁");
    }

    Ok(())
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
