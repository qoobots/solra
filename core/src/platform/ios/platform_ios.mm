/*
 * Solra Core SDK - iOS platform layer (stub)
 */

#include <solra/solra_core.h>
#include <spdlog/spdlog.h>
#include <TargetConditionals.h>

#if TARGET_OS_IOS

#ifdef __OBJC__
#import <Metal/Metal.h>
#import <MetalKit/MetalKit.h>
#import <CoreML/CoreML.h>
#import <Foundation/Foundation.h>
#endif

extern "C" {

int solra_platform_ios_get_metal_device(void **device) {
#ifdef __OBJC__
  id<MTLDevice> mtlDevice = MTLCreateSystemDefaultDevice();
  if (mtlDevice) {
    spdlog::info("iOS: Metal device found: {}", [[mtlDevice name] UTF8String]);
    if (device) *device = (__bridge_retained void *)mtlDevice;
    return 0;
  }
#endif
  return -1;
}

int solra_platform_ios_is_coreml_available(void) {
#ifdef __OBJC__
  return [MLModel class] != nil ? 1 : 0;
#else
  return 0;
#endif
}

} // extern "C"

#endif /* TARGET_OS_IOS */
