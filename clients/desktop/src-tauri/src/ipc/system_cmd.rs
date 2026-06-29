// 系统信息 Tauri IPC 命令

use serde::{Deserialize, Serialize};

#[derive(Debug, Serialize, Deserialize)]
pub struct SystemInfo {
    pub os: String,
    pub arch: String,
    pub cpu_cores: u32,
    pub total_memory_gb: f32,
    pub gpu_name: String,
    pub core_sdk_loaded: bool,
    pub core_sdk_version: String,
}

/// 获取系统信息
#[tauri::command]
pub async fn get_system_info() -> Result<SystemInfo, String> {
    let core_loaded = super::super::core::ffi::CoreSdk::is_initialized();
    let core_version = if core_loaded {
        "0.1.0".to_string()
    } else {
        "未加载".to_string()
    };

    Ok(SystemInfo {
        os: std::env::consts::OS.to_string(),
        arch: std::env::consts::ARCH.to_string(),
        cpu_cores: num_cpus::get() as u32,
        total_memory_gb: 16.0, // TODO: 实际检测
        gpu_name: "NVIDIA GeForce RTX 3060".into(), // TODO: 实际检测
        core_sdk_loaded: core_loaded,
        core_sdk_version,
    })
}

/// 获取 Core SDK 版本
#[tauri::command]
pub async fn get_core_version() -> Result<String, String> {
    use std::ffi::CStr;

    let sdk = super::super::core::ffi::CoreSdk::get()
        .ok_or("Core SDK 未加载")?;

    let version = unsafe {
        let ptr = (sdk.version)();
        if ptr.is_null() {
            "unknown"
        } else {
            CStr::from_ptr(ptr).to_str().unwrap_or("unknown")
        }
    };

    Ok(version.to_string())
}
