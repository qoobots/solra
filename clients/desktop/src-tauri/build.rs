// Tauri 构建脚本
// 检测 Core SDK 库文件是否存在，设置链接路径

fn main() {
    tauri_build::build();

    // 检测 libsolracore.dll 是否存在
    let search_paths = [
        "core/libsolracore.dll",
        "../core/libsolracore.dll",
        "../../core/build/windows/Release/libsolracore.dll",
        "../../core/build/windows/Debug/libsolracore.dll",
    ];

    let found = search_paths.iter().any(|p| std::path::Path::new(p).exists());

    if found {
        println!("cargo:rustc-cfg=feature=\"core_sdk_available\"");
        println!("cargo:info=Core SDK 已就绪");
    } else {
        println!("cargo:warning=Core SDK (libsolracore.dll) 未找到——将以 Mock 模式运行");
        println!("cargo:rustc-cfg=feature=\"mock_mode\"");
    }
}
