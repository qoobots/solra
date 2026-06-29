// Solra Desktop — 入口点
// 调用 lib.rs 中的 run() 启动应用

#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

fn main() {
    solra_desktop_lib::run();
}
