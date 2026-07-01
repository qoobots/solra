// IPC 命令模块
// Tauri Command 函数 —— Vue 3 前端通过 invoke() 调用

pub mod space_cmd;
pub mod avatar_cmd;
pub mod render_cmd;
pub mod system_cmd;

#[cfg(test)]
mod ipc_tests;
