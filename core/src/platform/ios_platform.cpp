#include "ios_platform.hpp"

#if defined(__APPLE__)

#include <cstdio>
#include <string>
#include <chrono>
#include <thread>

// Objective-C 桥接
#import <Foundation/Foundation.h>
#import <Metal/Metal.h>
#import <MetalKit/MetalKit.h>
#import <CoreML/CoreML.h>
#import <UIKit/UIKit.h>
#import <QuartzCore/CAMetalLayer.h>
#import <sys/sysctl.h>
#import <sys/utsname.h>
#import <mach/mach.h>
#import <SystemConfiguration/SystemConfiguration.h>

namespace solra::core::platform::ios {

// ============================================================================
// iOSDeviceInfo
// ============================================================================

iOSDeviceInfo iOSDeviceInfo::Collect() {
  iOSDeviceInfo info{};

  // 设备型号
  struct utsname system_info;
  uname(&system_info);
  info.model_name = system_info.machine;

  // 芯片名称映射
  static const std::unordered_map<std::string, std::string> kChipMap = {
    {"iPhone14,2", "A15 Bionic"}, {"iPhone14,3", "A15 Bionic"},
    {"iPhone14,7", "A15 Bionic"}, {"iPhone14,8", "A15 Bionic"},
    {"iPhone15,2", "A16 Bionic"}, {"iPhone15,3", "A16 Bionic"},
    {"iPhone15,4", "A16 Bionic"}, {"iPhone15,5", "A16 Bionic"},
    {"iPhone16,1", "A17 Pro"},    {"iPhone16,2", "A17 Pro"},
    {"iPhone17,1", "A18"},        {"iPhone17,2", "A18 Pro"},
    {"iPad13,4",  "M1"},          {"iPad13,8", "M1"},
    {"iPad14,3",  "M2"},          {"iPad14,8", "M2"},
  };
  auto it = kChipMap.find(info.model_name);
  info.chip_name = (it != kChipMap.end()) ? it->second : "Apple Silicon";

  // 判断是否支持 NE (A12+)
  info.has_neural_engine = true; // 所有支持CoreML的设备都有ANE (A11+)
  if (info.chip_name.find("M4") != std::string::npos) info.neural_engine_cores = 16;
  else if (info.chip_name.find("M3") != std::string::npos) info.neural_engine_cores = 16;
  else if (info.chip_name.find("M2") != std::string::npos) info.neural_engine_cores = 16;
  else if (info.chip_name.find("M1") != std::string::npos) info.neural_engine_cores = 16;
  else if (info.chip_name.find("A18") != std::string::npos) info.neural_engine_cores = 16;
  else if (info.chip_name.find("A17") != std::string::npos) info.neural_engine_cores = 16;
  else info.neural_engine_cores = 16; // A12-A16 均为 16核

  // GPU 核心数
  if (info.chip_name.find("M4") != std::string::npos) info.gpu_cores = 10;
  else if (info.chip_name.find("M3") != std::string::npos) info.gpu_cores = 10;
  else if (info.chip_name.find("M2") != std::string::npos) info.gpu_cores = 10;
  else if (info.chip_name.find("M1") != std::string::npos) info.gpu_cores = 8;
  else if (info.chip_name.find("A18 Pro") != std::string::npos) info.gpu_cores = 6;
  else if (info.chip_name.find("A17 Pro") != std::string::npos) info.gpu_cores = 6;
  else info.gpu_cores = 5;

  // 内存
  int64_t memsize = 0;
  size_t len = sizeof(memsize);
  sysctlbyname("hw.memsize", &memsize, &len, nullptr, 0);
  info.total_ram_bytes = static_cast<uint64_t>(memsize);
  info.gpu_ram_bytes = info.total_ram_bytes / 2; // 统一内存, GPU约占用50%

  // CPU 核心数
  int32_t ncpu = 0;
  len = sizeof(ncpu);
  sysctlbyname("hw.ncpu", &ncpu, &len, nullptr, 0);
  info.cpu_cores = static_cast<uint32_t>(ncpu);

  // 屏幕
  UIScreen* mainScreen = [UIScreen mainScreen];
  info.screen_scale = mainScreen.scale;
  CGSize size = mainScreen.bounds.size;
  info.screen_width_px  = static_cast<uint32_t>(size.width * mainScreen.scale);
  info.screen_height_px = static_cast<uint32_t>(size.height * mainScreen.scale);

  // 系统版本
  NSString* version = [[UIDevice currentDevice] systemVersion];
  info.os_version = [version UTF8String];
  NSArray* parts = [version componentsSeparatedByString:@"."];
  info.os_major = [parts[0] intValue];
  info.os_minor = [parts count] > 1 ? [parts[1] intValue] : 0;
  info.os_patch = [parts count] > 2 ? [parts[2] intValue] : 0;

  // 设备族
  switch ([[UIDevice currentDevice] userInterfaceIdiom]) {
    case UIUserInterfaceIdiomPhone:  info.family = iOSDeviceFamily::kiPhone; break;
    case UIUserInterfaceIdiomPad:    info.family = iOSDeviceFamily::kiPad; break;
    case UIUserInterfaceIdiomTV:     info.family = iOSDeviceFamily::kAppleTV; break;
    default:                          info.family = iOSDeviceFamily::kUnknown;
  }

  // 特性检测
  id<MTLDevice> device = MTLCreateSystemDefaultDevice();
  info.supports_metal3 = [device supportsFamily:MTLGPUFamilyMetal3];
  #if __IPHONE_OS_VERSION_MAX_ALLOWED >= 170000
    info.supports_ray_tracing = [device supportsRaytracing];
  #else
    info.supports_ray_tracing = false;
  #endif

  return info;
}

std::string iOSDeviceInfo::ToJson() const {
  char buf[1024];
  snprintf(buf, sizeof(buf),
    R"({"chip":"%s","gpu_cores":%u,"ram_mb":%llu,"ane":%s,"metal3":%s,"rt":%s})",
    chip_name.c_str(), gpu_cores,
    static_cast<unsigned long long>(total_ram_bytes / (1024*1024)),
    has_neural_engine ? "true" : "false",
    supports_metal3 ? "true" : "false",
    supports_ray_tracing ? "true" : "false");
  return std::string(buf);
}

// ============================================================================
// MetalRenderer
// ============================================================================

struct MetalRenderer::Impl {
  id<MTLDevice> device = nil;
  MTKView* metal_view = nil;
  CAMetalLayer* metal_layer = nil;

  RenderCallback render_callback;
  CADisplayLink* display_link = nil;

  bool vsync_enabled = true;
  bool dynamic_resolution = false;
  float resolution_scale = 1.0f;
  uint32_t preferred_fps = 60;

  double last_frame_time_ms = 0;
  double avg_fps = 0;
  uint64_t frame_count = 0;

  std::chrono::steady_clock::time_point last_frame_time;
};

MetalRenderer::MetalRenderer() : impl_(std::make_unique<Impl>()) {}

MetalRenderer::~MetalRenderer() { Shutdown(); }

bool MetalRenderer::Initialize(UIView* native_view) {
  impl_->device = MTLCreateSystemDefaultDevice();
  if (!impl_->device) {
    fprintf(stderr, "[MetalRenderer] Metal is not supported on this device\n");
    return false;
  }

  impl_->metal_view = [[MTKView alloc] initWithFrame:native_view.bounds
                                              device:impl_->device];
  impl_->metal_view.colorPixelFormat = MTLPixelFormatBGRA8Unorm_sRGB;
  impl_->metal_view.depthStencilPixelFormat = MTLPixelFormatDepth32Float_Stencil8;
  impl_->metal_view.clearColor = MTLClearColorMake(0.0, 0.0, 0.0, 1.0);
  impl_->metal_view.preferredFramesPerSecond = impl_->preferred_fps;

  [native_view addSubview:impl_->metal_view];
  return true;
}

bool MetalRenderer::InitializeLayer(CAMetalLayer* layer) {
  impl_->device = MTLCreateSystemDefaultDevice();
  if (!impl_->device) return false;

  impl_->metal_layer = layer;
  impl_->metal_layer.device = impl_->device;
  impl_->metal_layer.pixelFormat = MTLPixelFormatBGRA8Unorm_sRGB;
  impl_->metal_layer.framebufferOnly = NO;
  return true;
}

void MetalRenderer::Shutdown() {
  StopRenderLoop();
  if (impl_->metal_view) {
    [impl_->metal_view release];
    impl_->metal_view = nil;
  }
  if (impl_->metal_layer) {
    [impl_->metal_layer release];
    impl_->metal_layer = nil;
  }
}

MTLDevice* MetalRenderer::GetPreferredDevice() {
  NSArray<id<MTLDevice>>* devices = MTLCopyAllDevices();
  // 优先选择独立GPU (macOS), iOS只有一个
  for (id<MTLDevice> d in devices) {
    if (d.isRemovable) return d;
  }
  return MTLCreateSystemDefaultDevice();
}

MTLDevice* MetalRenderer::GetDefaultDevice() {
  return MTLCreateSystemDefaultDevice();
}

std::vector<MTLDevice*> MetalRenderer::GetAllDevices() {
  NSArray<id<MTLDevice>>* devices = MTLCopyAllDevices();
  std::vector<MTLDevice*> result;
  for (id<MTLDevice> d in devices) result.push_back(d);
  return result;
}

MTKView* MetalRenderer::GetMetalView() const { return impl_->metal_view; }
CAMetalLayer* MetalRenderer::GetMetalLayer() const { return impl_->metal_layer; }

void MetalRenderer::SetRenderCallback(RenderCallback callback) {
  impl_->render_callback = std::move(callback);
}

void MetalRenderer::StartRenderLoop() {
  StopRenderLoop();

  impl_->display_link = [CADisplayLink
      displayLinkWithTarget:[NSObject new]
                   selector:@selector(description)]; // 占位

  [impl_->display_link addToRunLoop:[NSRunLoop mainRunLoop]
                            forMode:NSRunLoopCommonModes];

  impl_->last_frame_time = std::chrono::steady_clock::now();
}

void MetalRenderer::StopRenderLoop() {
  if (impl_->display_link) {
    [impl_->display_link invalidate];
    impl_->display_link = nil;
  }
}

void MetalRenderer::SetPreferredFPS(uint32_t fps) {
  impl_->preferred_fps = fps;
  if (impl_->metal_view) {
    impl_->metal_view.preferredFramesPerSecond = fps;
  }
}

void MetalRenderer::SetVsyncEnabled(bool enabled) {
  impl_->vsync_enabled = enabled;
}

void MetalRenderer::SetDynamicResolutionEnabled(bool enabled) {
  impl_->dynamic_resolution = enabled;
}

void MetalRenderer::SetResolutionScale(float scale) {
  impl_->resolution_scale = std::clamp(scale, 0.25f, 1.0f);
  if (impl_->metal_layer) {
    CGSize drawable = impl_->metal_layer.bounds.size;
    impl_->metal_layer.drawableSize = CGSizeMake(
        drawable.width * impl_->resolution_scale,
        drawable.height * impl_->resolution_scale);
  }
}

float MetalRenderer::GetCurrentResolutionScale() const {
  return impl_->resolution_scale;
}

void MetalRenderer::OnEnterBackground() { StopRenderLoop(); }

void MetalRenderer::OnEnterForeground() {
  impl_->last_frame_time = std::chrono::steady_clock::now();
  StartRenderLoop();
}

double MetalRenderer::GetLastFrameTimeMs() const {
  return impl_->last_frame_time_ms;
}

double MetalRenderer::GetAverageFPS() const { return impl_->avg_fps; }

uint64_t MetalRenderer::GetFrameCount() const { return impl_->frame_count; }

uint64_t MetalRenderer::GetTextureMemoryUsage() const {
  // TODO(kkfu): MTLDevice.currentAllocatedSize 查询
  return 0;
}

// ============================================================================
// CoreMLInference
// ============================================================================

struct CoreMLInference::Impl {
  MLModel* ml_model = nil;
  MLModelConfiguration* config = nil;

  bool prefer_ane = true;
  bool allow_gpu  = true;
  ComputeUnits compute_units = ComputeUnits::kAll;
};

CoreMLInference::CoreMLInference() : impl_(std::make_unique<Impl>()) {
  impl_->config = [[MLModelConfiguration alloc] init];
  impl_->config.computeUnits = MLComputeUnitsAll; // ANE + GPU + CPU
}

CoreMLInference::~CoreMLInference() { UnloadModel(); }

bool CoreMLInference::LoadModel(const std::string& mlmodel_path) {
  UnloadModel();

  NSString* path = [NSString stringWithUTF8String:mlmodel_path.c_str()];
  NSURL* url = [NSURL fileURLWithPath:path];

  NSError* error = nil;
  MLModel* model = [MLModel modelWithContentsOfURL:url
                                    configuration:impl_->config
                                            error:&error];
  if (error) {
    fprintf(stderr, "[CoreML] Failed to load model: %s\n",
            [[error localizedDescription] UTF8String]);
    return false;
  }

  impl_->ml_model = model;
  return true;
}

bool CoreMLInference::LoadCompiledModel(const std::string& mlmodelc_path) {
  // .mlmodelc 目录直接作为编译后模型加载
  return LoadModel(mlmodelc_path);
}

void CoreMLInference::UnloadModel() {
  if (impl_->ml_model) {
    impl_->ml_model = nil;
  }
}

bool CoreMLInference::IsLoaded() const {
  return impl_->ml_model != nil;
}

std::vector<CoreMLInference::MultiArray> CoreMLInference::Predict(
    const std::vector<MultiArray>& inputs) {

  std::vector<MultiArray> results;

  if (!impl_->ml_model) return results;

  // TODO(kkfu): CoreML 推理
  // 1. 构造 MLMultiArray 输入
  // 2. 创建 MLPredictionOptions
  // 3. [model predictionFromFeatures:error:]
  // 4. 提取输出 MLMultiArray

  printf("[CoreML] Predict called with %zu input(s)\n", inputs.size());
  return results;
}

void CoreMLInference::PredictAsync(
    const std::vector<MultiArray>& inputs,
    PredictCallback callback) {
  // CoreML 推理本身是同步的，使用 dispatch_async 到后台队列
  dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
    auto results = Predict(inputs);
    if (callback) {
      dispatch_async(dispatch_get_main_queue(), ^{
        callback(results);
      });
    }
  });
}

bool CoreMLInference::IsUsingANE() const {
  return impl_->prefer_ane;
}

void CoreMLInference::SetPreferANE(bool prefer) {
  impl_->prefer_ane = prefer;
  if (prefer && impl_->allow_gpu) {
    impl_->config.computeUnits = MLComputeUnitsAll;
  } else if (prefer && !impl_->allow_gpu) {
    impl_->config.computeUnits = MLComputeUnitsCPUAndNeuralEngine;
  } else if (!prefer && impl_->allow_gpu) {
    impl_->config.computeUnits = MLComputeUnitsCPUAndGPU;
  } else {
    impl_->config.computeUnits = MLComputeUnitsCPUOnly;
  }
}

bool CoreMLInference::IsUsingGPU() const { return impl_->allow_gpu; }

void CoreMLInference::SetAllowGPU(bool allow) {
  impl_->allow_gpu = allow;
}

void CoreMLInference::SetComputeUnits(ComputeUnits units) {
  impl_->compute_units = units;
  switch (units) {
    case ComputeUnits::kCPUOnly:     impl_->config.computeUnits = MLComputeUnitsCPUOnly; break;
    case ComputeUnits::kCPUAndGPU:   impl_->config.computeUnits = MLComputeUnitsCPUAndGPU; break;
    case ComputeUnits::kAll:         impl_->config.computeUnits = MLComputeUnitsAll; break;
    case ComputeUnits::kCPUAndANE:   impl_->config.computeUnits = MLComputeUnitsCPUAndNeuralEngine; break;
  }
}

auto CoreMLInference::GetComputeUnits() const -> ComputeUnits {
  return impl_->compute_units;
}

std::string CoreMLInference::GetModelDescription() const {
  if (!impl_->ml_model) return "No model loaded";
  return [[impl_->ml_model.modelDescription description] UTF8String];
}

uint64_t CoreMLInference::GetModelSize() const {
  // TODO(kkfu): 获取 .mlmodelc 目录大小
  return 0;
}

// ============================================================================
// iOSPlatform (Singleton)
// ============================================================================

struct iOSPlatform::Impl {
  iOSDeviceInfo device_info;
  std::unique_ptr<MetalRenderer> renderer;
  std::unique_ptr<CoreMLInference> inference;
  std::unique_ptr<iOSWindow> window;
  bool initialized = false;
  MemoryWarningCallback memory_warning_cb;
};

iOSPlatform& iOSPlatform::Instance() {
  static iOSPlatform platform;
  return platform;
}

iOSPlatform::iOSPlatform() : impl_(std::make_unique<Impl>()) {}

bool iOSPlatform::Initialize() {
  if (impl_->initialized) return true;

  impl_->device_info = iOSDeviceInfo::Collect();
  impl_->renderer = std::make_unique<MetalRenderer>();
  impl_->inference = std::make_unique<CoreMLInference>();

  impl_->initialized = true;

  printf("[iOSPlatform] Initialized: %s (%s) GPU=%u ANE=%u RAM=%lluMB\n",
         impl_->device_info.marketing_name.c_str(),
         impl_->device_info.chip_name.c_str(),
         impl_->device_info.gpu_cores,
         impl_->device_info.neural_engine_cores,
         static_cast<unsigned long long>(impl_->device_info.total_ram_bytes / (1024*1024)));

  return true;
}

void iOSPlatform::Shutdown() {
  impl_->renderer.reset();
  impl_->inference.reset();
  impl_->window.reset();
  impl_->initialized = false;
}

MetalRenderer* iOSPlatform::GetRenderer() { return impl_->renderer.get(); }
CoreMLInference* iOSPlatform::GetInference() { return impl_->inference.get(); }
iOSWindow* iOSPlatform::GetWindow() { return impl_->window.get(); }
const iOSDeviceInfo& iOSPlatform::GetDeviceInfo() const { return impl_->device_info; }

std::string iOSPlatform::GetBundlePath() const {
  return [[[NSBundle mainBundle] bundlePath] UTF8String];
}

std::string iOSPlatform::GetDocumentsPath() const {
  NSArray* paths = NSSearchPathForDirectoriesInDomains(
      NSDocumentDirectory, NSUserDomainMask, YES);
  return [[paths firstObject] UTF8String];
}

std::string iOSPlatform::GetCachesPath() const {
  NSArray* paths = NSSearchPathForDirectoriesInDomains(
      NSCachesDirectory, NSUserDomainMask, YES);
  return [[paths firstObject] UTF8String];
}

std::string iOSPlatform::GetTemporaryPath() const {
  return [NSTemporaryDirectory() UTF8String];
}

uint64_t iOSPlatform::GetFreeDiskSpace() const {
  NSDictionary* attrs = [[NSFileManager defaultManager]
      attributesOfFileSystemForPath:NSHomeDirectory() error:nil];
  return [[attrs objectForKey:NSFileSystemFreeSize] unsignedLongLongValue];
}

bool iOSPlatform::IsNetworkAvailable() const {
  // TODO(kkfu): SCNetworkReachability
  return true;
}

bool iOSPlatform::IsWiFiConnected() const { return true; }
bool iOSPlatform::IsCellularConnected() const { return false; }

float iOSPlatform::GetBatteryLevel() const {
  [[UIDevice currentDevice] setBatteryMonitoringEnabled:YES];
  return [[UIDevice currentDevice] batteryLevel];
}

bool iOSPlatform::IsCharging() const {
  return [[UIDevice currentDevice] batteryState] == UIDeviceBatteryStateCharging ||
         [[UIDevice currentDevice] batteryState] == UIDeviceBatteryStateFull;
}

bool iOSPlatform::IsLowPowerModeEnabled() const {
  return [[NSProcessInfo processInfo] isLowPowerModeEnabled];
}

void iOSPlatform::KeepScreenOn(bool keep_on) {
  [[UIApplication sharedApplication] setIdleTimerDisabled:keep_on];
}

void iOSPlatform::SetIdleTimerDisabled(bool disabled) {
  KeepScreenOn(disabled);
}

void iOSPlatform::OnApplicationDidFinishLaunching() {}
void iOSPlatform::OnApplicationWillResignActive() {}
void iOSPlatform::OnApplicationDidEnterBackground() {
  if (impl_->renderer) impl_->renderer->OnEnterBackground();
}
void iOSPlatform::OnApplicationWillEnterForeground() {
  if (impl_->renderer) impl_->renderer->OnEnterForeground();
}
void iOSPlatform::OnApplicationDidBecomeActive() {}
void iOSPlatform::OnApplicationWillTerminate() { Shutdown(); }

void iOSPlatform::SetMemoryWarningCallback(MemoryWarningCallback callback) {
  impl_->memory_warning_cb = std::move(callback);
}

// ============================================================================
// iOSWindow (简化实现)
// ============================================================================

struct iOSWindow::Impl {
  UIView* root_view = nil;
  UIViewController* parent_vc = nil;
  TouchCallback touch_cb;
  GestureCallback gesture_cb;
  LifecycleCallback lifecycle_cb;
};

iOSWindow::iOSWindow() : impl_(std::make_unique<Impl>()) {}

iOSWindow::~iOSWindow() { Destroy(); }

bool iOSWindow::Create(const std::string& title,
                        uint32_t width, uint32_t height,
                        UIViewController* parent_vc) {
  impl_->parent_vc = parent_vc;
  CGRect frame = CGRectMake(0, 0, width, height);
  impl_->root_view = [[UIView alloc] initWithFrame:frame];
  impl_->root_view.backgroundColor = [UIColor blackColor];
  return true;
}

void iOSWindow::Destroy() {
  if (impl_->root_view) {
    [impl_->root_view removeFromSuperview];
    [impl_->root_view release];
    impl_->root_view = nil;
  }
}

UIView* iOSWindow::GetRootView() const { return impl_->root_view; }

void iOSWindow::SetTouchCallback(TouchCallback callback) {
  impl_->touch_cb = std::move(callback);
}

void iOSWindow::SetGestureCallback(GestureCallback callback) {
  impl_->gesture_cb = std::move(callback);
}

void iOSWindow::SetLifecycleCallback(LifecycleCallback callback) {
  impl_->lifecycle_cb = std::move(callback);
}

} // namespace solra::core::platform::ios

#else
// 非 Apple 平台的占位实现 (链接兼容)
namespace solra::core::platform::ios {
  // 空实现，确保非Apple平台编译通过
}
#endif // __APPLE__
