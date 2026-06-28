/*
 * Solra Core SDK - macOS platform layer (stub)
 */

#include <solra/solra_core.h>
#include <spdlog/spdlog.h>

#if defined(__APPLE__) && !TARGET_OS_IOS

#ifdef __OBJC__
#import <Metal/Metal.h>
#import <Foundation/Foundation.h>
#endif

extern "C" {

int solra_platform_macos_init(void) {
  spdlog::info("macOS platform layer initialized");
  return 0;
}

} // extern "C"

#endif
