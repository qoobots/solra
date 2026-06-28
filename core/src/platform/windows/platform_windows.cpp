/*
 * Solra Core SDK - Windows platform layer (stub)
 */

#include <solra/solra_core.h>
#include <spdlog/spdlog.h>

#ifdef _WIN32
#include <windows.h>
#endif

extern "C" {

int solra_platform_windows_init(void) {
  spdlog::info("Windows platform layer initialized");
  return 0;
}

HWND solra_platform_windows_get_hwnd(void *native_window) {
  if (native_window) {
    return static_cast<HWND>(native_window);
  }
  return nullptr;
}

} // extern "C"
