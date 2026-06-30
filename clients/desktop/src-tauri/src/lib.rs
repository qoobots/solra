// Solra Desktop — Rust 库入口
// 负责模块注册、Tauri Builder 配置、Core SDK 初始化

pub mod core;
pub mod ipc;
pub mod services;

use tauri::Manager;

/// 应用启动时的初始化逻辑
/// - 初始化日志
/// - 初始化服务（ApiClient、AuthService、CacheService）
/// - 尝试加载 Core SDK (libsolracore.dll)
/// - 注册 IPC 命令
pub fn run() {
    env_logger::init();

    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_notification::init())
        .setup(|app| {
            log::info!("Solra Desktop 启动中...");

            // 初始化 API 客户端
            let api_client = services::api_client::ApiClient::new(
                option_env!("SOLRA_API_BASE_URL").unwrap_or("https://api.solra.io")
            );
            app.manage(api_client);

            // 初始化认证服务
            let auth_service = services::auth_service::AuthService::new();
            app.manage(auth_service);

            // 初始化缓存服务（最大 2GB）
            let cache_dir = app.path().app_cache_dir()
                .map_err(|e| format!("获取缓存目录失败: {}", e))
                .unwrap_or_else(|_| std::path::PathBuf::from("./cache"));
            let cache_service = services::cache_service::CacheService::new(cache_dir, 2048);
            app.manage(cache_service);

            // 尝试加载 Core SDK
            match core::ffi::load_core_sdk() {
                Ok(_) => log::info!("Core SDK 加载成功"),
                Err(e) => log::warn!("Core SDK 加载失败 (开发模式可忽略): {}", e),
            }

            // 获取主窗口引用
            let _window = app.get_webview_window("main")
                .expect("主窗口未找到");

            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            ipc::space_cmd::get_spaces,
            ipc::space_cmd::get_space_detail,
            ipc::space_cmd::enter_space,
            ipc::space_cmd::exit_space,
            ipc::space_cmd::create_space,
            ipc::space_cmd::get_leaderboard,
            ipc::avatar_cmd::start_conversation,
            ipc::avatar_cmd::send_message,
            ipc::avatar_cmd::stop_conversation,
            ipc::render_cmd::init_renderer,
            ipc::render_cmd::resize_renderer,
            ipc::render_cmd::get_fps,
            ipc::system_cmd::get_system_info,
            ipc::system_cmd::get_core_version,
            ipc::system_cmd::update_profile,
            ipc::system_cmd::get_profile,
            ipc::system_cmd::get_store_items,
            ipc::system_cmd::get_messages,
        ])
        .run(tauri::generate_context!())
        .expect("启动 Solra Desktop 失败");
}
