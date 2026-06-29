#include "android_platform.hpp"

#if defined(__ANDROID__)

#include <cmath>
#include <chrono>
#include <thread>
#include <fstream>
#include <algorithm>
#include <sys/system_properties.h>
#include <sys/stat.h>
#include <unistd.h>

namespace solra::core::platform::android {

// ============================================================================
// AndroidDeviceInfo
// ============================================================================

AndroidDeviceInfo AndroidDeviceInfo::Collect(AAssetManager* /*amgr*/) {
  AndroidDeviceInfo info{};

  // 系统属性读取
  auto get_prop = [](const char* key, const char* def = "unknown") -> std::string {
    char buf[PROP_VALUE_MAX] = {};
    __system_property_get(key, buf);
    return (buf[0] != '\0') ? std::string(buf) : def;
  };

  info.manufacturer = get_prop("ro.product.manufacturer");
  info.model        = get_prop("ro.product.model");
  info.brand        = get_prop("ro.product.brand");
  info.device       = get_prop("ro.product.device");

  // SoC
  std::string board = get_prop("ro.board.platform");
  info.soc_name     = get_prop("ro.chipname", board.c_str());

  // 厂商识别
  if (info.soc_name.find("msm") != std::string::npos ||
      info.soc_name.find("sdm") != std::string::npos ||
      info.soc_name.find("sm") != std::string::npos)
    info.soc_vendor = "Qualcomm";
  else if (info.soc_name.find("mt") != std::string::npos)
    info.soc_vendor = "MediaTek";
  else if (info.soc_name.find("exynos") != std::string::npos)
    info.soc_vendor = "Samsung";
  else if (info.soc_name.find("kirin") != std::string::npos ||
           info.soc_name.find("hi") != std::string::npos)
    info.soc_vendor = "HiSilicon";
  else if (info.soc_name.find("tensor") != std::string::npos)
    info.soc_vendor = "Google";

  // NPU 检测
  if (info.soc_vendor == "Qualcomm") {
    info.npu_vendor = AndroidDeviceInfo::NpuVendor::kQualcommHexagon;
    info.npu_available = true;
    info.npu_description = "Hexagon DSP (SNPE/QNN)";
  } else if (info.soc_vendor == "MediaTek") {
    info.npu_vendor = AndroidDeviceInfo::NpuVendor::kMediaTekAPU;
    info.npu_available = true;
    info.npu_description = "MediaTek APU (NeuroPilot)";
  } else if (info.soc_vendor == "Samsung") {
    info.npu_vendor = AndroidDeviceInfo::NpuVendor::kSamsungNPU;
    info.npu_available = true;
    info.npu_description = "Samsung NPU (ENN)";
  } else if (info.soc_vendor == "HiSilicon") {
    info.npu_vendor = AndroidDeviceInfo::NpuVendor::kHiSiliconNPU;
    info.npu_available = true;
    info.npu_description = "HiSilicon NPU (HiAI)";
  }

  // CPU
  info.cpu_abi = get_prop("ro.product.cpu.abi");
  info.is_64bit = (info.cpu_abi.find("arm64") != std::string::npos) ||
                  (info.cpu_abi.find("x86_64") != std::string::npos);

  // 核心数 (从 sysfs 读取)
  std::ifstream cpuinfo("/sys/devices/system/cpu/present");
  if (cpuinfo) {
    std::string line;
    std::getline(cpuinfo, line);
    auto dash = line.find('-');
    if (dash != std::string::npos)
      info.cpu_cores = std::stoi(line.substr(dash + 1)) + 1;
  }

  // 内存
  std::ifstream meminfo("/proc/meminfo");
  std::string line;
  while (std::getline(meminfo, line)) {
    if (line.find("MemTotal:") == 0) {
      size_t kb = 0;
      sscanf(line.c_str(), "MemTotal: %zu kB", &kb);
      info.total_ram_bytes = kb * 1024;
    }
    if (line.find("MemAvailable:") == 0) {
      size_t kb = 0;
      sscanf(line.c_str(), "MemAvailable: %zu kB", &kb);
      info.available_ram_bytes = kb * 1024;
    }
  }

  // 系统版本
  info.api_level = android_get_device_api_level();
  info.os_version = get_prop("ro.build.version.release");

  // 屏幕 (从 JNI 获取，此处设置默认值)
  info.density = 2.0f; // 默认 @2x

  return info;
}

std::string AndroidDeviceInfo::ToJson() const {
  char buf[2048];
  snprintf(buf, sizeof(buf),
    R"({"manufacturer":"%s","model":"%s","soc":"%s","gpu":"%s","ram_mb":%llu,)"
    R"("npu":"%s","vulkan":%s,"api":%u})",
    manufacturer.c_str(), model.c_str(), soc_name.c_str(),
    gpu_renderer.c_str(),
    static_cast<unsigned long long>(total_ram_bytes / (1024 * 1024)),
    npu_available ? npu_description.c_str() : "none",
    supports_vulkan ? "true" : "false",
    api_level);
  return std::string(buf);
}

// ============================================================================
// VulkanRenderer
// ============================================================================

struct VulkanRenderer::Impl {
  ANativeWindow* window = nullptr;
  uint32_t width = 0, height = 0;
  bool initialized = false;

  // TODO(kkfu): Vulkan 对象
  // VkInstance       instance;
  // VkPhysicalDevice physical_device;
  // VkDevice         device;
  // VkQueue          graphics_queue;
  // VkSurfaceKHR     surface;
  // VkSwapchainKHR   swapchain;

  RenderCallback render_callback;
  bool render_loop_running = false;
  uint32_t target_fps = 60;
  bool vsync = true;
  bool dynamic_resolution = false;
  float resolution_scale = 1.0f;

  double last_frame_time_ms = 0;
  double avg_fps = 0;
  uint64_t frame_count = 0;
};

VulkanRenderer::VulkanRenderer() : impl_(std::make_unique<Impl>()) {}
VulkanRenderer::~VulkanRenderer() { Shutdown(); }

bool VulkanRenderer::Initialize(ANativeWindow* window,
                                 uint32_t width, uint32_t height) {
  impl_->window = window;
  impl_->width = width;
  impl_->height = height;

  // TODO(kkfu): Vulkan 初始化
  // 1. vkCreateInstance (含 VK_KHR_android_surface)
  // 2. 选择独立GPU设备 (VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU)
  // 3. vkCreateDevice
  // 4. vkCreateAndroidSurfaceKHR
  // 5. 创建 Swapchain

  LOGI("[VulkanRenderer] Initialized %ux%u", width, height);
  impl_->initialized = true;
  return true;
}

bool VulkanRenderer::InitializeOffscreen() {
  // 离屏渲染: 不需要 surface/swapchain
  impl_->initialized = true;
  return true;
}

void VulkanRenderer::Shutdown() {
  StopRenderLoop();
  DestroySurface();
  impl_->initialized = false;
}

bool VulkanRenderer::CreateSurface(ANativeWindow* window) {
  impl_->window = window;
  return true;
}

void VulkanRenderer::DestroySurface() {
  impl_->window = nullptr;
}

void VulkanRenderer::ResizeSurface(uint32_t width, uint32_t height) {
  impl_->width = width;
  impl_->height = height;
}

std::vector<VulkanRenderer::VulkanDeviceInfo> VulkanRenderer::EnumerateDevices() {
  return {}; // TODO(kkfu): vkEnumeratePhysicalDevices
}

int32_t VulkanRenderer::SelectBestDevice() {
  // 选择优先级: 独立GPU > 集成GPU > CPU
  return 0;
}

void VulkanRenderer::SetRenderCallback(RenderCallback callback) {
  impl_->render_callback = std::move(callback);
}

void VulkanRenderer::StartRenderLoop() {
  impl_->render_loop_running = true;
  // TODO(kkfu): Choreographer 驱动渲染循环
}

void VulkanRenderer::StopRenderLoop() {
  impl_->render_loop_running = false;
}

void VulkanRenderer::SetTargetFPS(uint32_t fps) { impl_->target_fps = fps; }
void VulkanRenderer::SetVsyncEnabled(bool enabled) { impl_->vsync = enabled; }
void VulkanRenderer::SetDynamicResolutionEnabled(bool enabled) { impl_->dynamic_resolution = enabled; }
void VulkanRenderer::SetResolutionScale(float scale) { impl_->resolution_scale = scale; }
void VulkanRenderer::OnPause() { StopRenderLoop(); }
void VulkanRenderer::OnResume() { StartRenderLoop(); }

double VulkanRenderer::GetLastFrameTimeMs() const { return impl_->last_frame_time_ms; }
double VulkanRenderer::GetAverageFPS() const { return impl_->avg_fps; }
uint64_t VulkanRenderer::GetFrameCount() const { return impl_->frame_count; }

// ============================================================================
// GLESRenderer (降级方案)
// ============================================================================

struct GLESRenderer::Impl {
  ANativeWindow* window = nullptr;
  uint32_t width = 0, height = 0;
  bool initialized = false;
};

GLESRenderer::GLESRenderer() : impl_(std::make_unique<Impl>()) {}
GLESRenderer::~GLESRenderer() { Shutdown(); }

bool GLESRenderer::Initialize(ANativeWindow* window,
                               uint32_t width, uint32_t height,
                               uint32_t version) {
  impl_->window = window;
  impl_->width = width;
  impl_->height = height;

  // TODO(kkfu): EGL + GLES 初始化
  // eglGetDisplay(EGL_DEFAULT_DISPLAY)
  // eglInitialize + eglChooseConfig
  // eglCreateWindowSurface + eglCreateContext
  // eglMakeCurrent

  LOGI("[GLESRenderer] Initialized GLES %u.0 %ux%u", version, width, height);
  impl_->initialized = true;
  return true;
}

void GLESRenderer::Shutdown() {
  impl_->initialized = false;
  impl_->window = nullptr;
}

void GLESRenderer::BeginFrame() {
  // glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
}

void GLESRenderer::EndFrame() {
  // eglSwapBuffers
}

void GLESRenderer::Resize(uint32_t width, uint32_t height) {
  impl_->width = width;
  impl_->height = height;
  glViewport(0, 0, width, height);
}

std::string GLESRenderer::GetGLVersion() const {
  return "OpenGL ES 3.2"; // glGetString(GL_VERSION)
}

std::string GLESRenderer::GetGLRenderer() const {
  return "Adreno"; // glGetString(GL_RENDERER)
}

std::string GLESRenderer::GetGLVendor() const {
  return "Qualcomm"; // glGetString(GL_VENDOR)
}

bool GLESRenderer::HasExtension(const std::string& ext) const {
  return false;
}

void* GLESRenderer::GetEGLDisplay() const { return nullptr; }
void* GLESRenderer::GetEGLContext() const { return nullptr; }

// ============================================================================
// NnapiInference
// ============================================================================

struct NnapiInference::Impl {
  bool loaded = false;
  ModelFormat format = ModelFormat::kTFLite;
  bool npu_enabled = true;
  ExecutionPreference preference = ExecutionPreference::kSustainedSpeed;
};

NnapiInference::NnapiInference() : impl_(std::make_unique<Impl>()) {}
NnapiInference::~NnapiInference() { UnloadModel(); }

bool NnapiInference::LoadModel(const std::string& model_path,
                                ModelFormat format) {
  impl_->format = format;

  // TODO(kkfu): NNAPI 模型加载
  // TFLite: ANeuralNetworksModel_create + ANeuralNetworksCompilation_create
  // SNPE: QC SNPE SDK 加载 .dlc
  // HiAI: Huawei HiAI Foundation 加载 .om

  LOGI("[NNAPI] Model loaded: %s", model_path.c_str());
  impl_->loaded = true;
  return true;
}

bool NnapiInference::LoadFromAsset(AAssetManager* mgr,
                                    const std::string& asset_path) {
  AAsset* asset = AAssetManager_open(mgr, asset_path.c_str(), AASSET_MODE_BUFFER);
  if (!asset) {
    LOGE("[NNAPI] Asset not found: %s", asset_path.c_str());
    return false;
  }

  size_t size = AAsset_getLength(asset);
  const void* data = AAsset_getBuffer(asset);

  bool ok = LoadFromBuffer(static_cast<const uint8_t*>(data), size,
                            ModelFormat::kTFLite);
  AAsset_close(asset);
  return ok;
}

bool NnapiInference::LoadFromBuffer(const uint8_t* data, size_t size,
                                     ModelFormat format) {
  impl_->format = format;
  impl_->loaded = true;
  return true;
}

void NnapiInference::UnloadModel() {
  impl_->loaded = false;
}

bool NnapiInference::IsLoaded() const { return impl_->loaded; }

std::vector<NnapiInference::Tensor> NnapiInference::Predict(
    const std::vector<Tensor>& inputs) {
  std::vector<Tensor> outputs;

  // TODO(kkfu): NNAPI 推理
  // 1. ANeuralNetworksExecution_create
  // 2. ANeuralNetworksExecution_setInput
  // 3. ANeuralNetworksExecution_setOutput
  // 4. ANeuralNetworksExecution_compute (阻塞)
  // 5. 读取输出

  LOGD("[NNAPI] Predict with %zu input(s)", inputs.size());
  return outputs;
}

void NnapiInference::PredictAsync(const std::vector<Tensor>& inputs,
                                   PredictCallback callback) {
  std::thread([this, inputs, cb = std::move(callback)]() {
    auto results = Predict(inputs);
    if (cb) cb(results);
  }).detach();
}

void NnapiInference::SetPreference(ExecutionPreference pref) {
  impl_->preference = pref;
}

void NnapiInference::SetNpuAcceleration(bool enable) {
  impl_->npu_enabled = enable;
}

bool NnapiInference::IsUsingNpu() const { return impl_->npu_enabled; }

std::string NnapiInference::GetModelInfo() const { return "NNAPI Model"; }

uint64_t NnapiInference::GetModelSize() const { return 0; }

uint32_t NnapiInference::GetNnapiVersion() {
  return ANEURALNETWORKS_FEATURE_LEVEL_8; // Android 14+
}

// ============================================================================
// AndroidPlatform (Singleton)
// ============================================================================

struct AndroidPlatform::Impl {
  void* java_vm = nullptr;
  void* jni_env = nullptr;
  void* native_activity = nullptr;
  AAssetManager* asset_mgr = nullptr;

  AndroidDeviceInfo device_info;
  std::unique_ptr<VulkanRenderer> vulkan_renderer;
  std::unique_ptr<GLESRenderer> gles_renderer;
  std::unique_ptr<NnapiInference> nnapi_inference;

  bool use_vulkan = true;  // 优先 Vulkan
  bool initialized = false;

  LowMemoryCallback low_memory_cb;
  ThermalCallback thermal_cb;
};

AndroidPlatform& AndroidPlatform::Instance() {
  static AndroidPlatform platform;
  return platform;
}

AndroidPlatform::AndroidPlatform() : impl_(std::make_unique<Impl>()) {}

bool AndroidPlatform::Initialize(void* java_vm, void* jni_env,
                                  void* native_activity,
                                  AAssetManager* asset_mgr) {
  impl_->java_vm = java_vm;
  impl_->jni_env = jni_env;
  impl_->native_activity = native_activity;
  impl_->asset_mgr = asset_mgr;

  impl_->device_info = AndroidDeviceInfo::Collect(asset_mgr);

  impl_->vulkan_renderer = std::make_unique<VulkanRenderer>();
  impl_->gles_renderer = std::make_unique<GLESRenderer>();
  impl_->nnapi_inference = std::make_unique<NnapiInference>();

  // 优先尝试 Vulkan
  impl_->use_vulkan = impl_->device_info.supports_vulkan;

  impl_->initialized = true;

  LOGI("[AndroidPlatform] Initialized: %s %s (SoC: %s, NPU: %s, Vulkan: %s)",
       impl_->device_info.manufacturer.c_str(),
       impl_->device_info.model.c_str(),
       impl_->device_info.soc_name.c_str(),
       impl_->device_info.npu_available ? "yes" : "no",
       impl_->use_vulkan ? "yes" : "no (GLES fallback)");

  return true;
}

void AndroidPlatform::Shutdown() {
  impl_->vulkan_renderer.reset();
  impl_->gles_renderer.reset();
  impl_->nnapi_inference.reset();
  impl_->initialized = false;
}

VulkanRenderer* AndroidPlatform::GetVulkanRenderer() { return impl_->vulkan_renderer.get(); }
GLESRenderer* AndroidPlatform::GetGLESRenderer() { return impl_->gles_renderer.get(); }
NnapiInference* AndroidPlatform::GetNnapiInference() { return impl_->nnapi_inference.get(); }

bool AndroidPlatform::UseVulkan() const { return impl_->use_vulkan; }

bool AndroidPlatform::FallbackToGLES() {
  LOGE("[AndroidPlatform] Vulkan not available, falling back to GLES");
  impl_->use_vulkan = false;
  return true;
}

const AndroidDeviceInfo& AndroidPlatform::GetDeviceInfo() const {
  return impl_->device_info;
}

std::string AndroidPlatform::GetInternalPath() const {
  // 需要通过 JNI 获取 Context.getFilesDir()
  return "/data/data/com.solra.app/files";
}

std::string AndroidPlatform::GetExternalPath() const { return "/sdcard/Android/data/com.solra.app/files"; }
std::string AndroidPlatform::GetCachePath() const { return "/data/data/com.solra.app/cache"; }
std::string AndroidPlatform::GetObbPath() const { return "/sdcard/Android/obb/com.solra.app"; }

uint64_t AndroidPlatform::GetFreeDiskSpace() const {
  struct statvfs stat;
  if (statvfs(GetInternalPath().c_str(), &stat) == 0)
    return stat.f_bavail * stat.f_frsize;
  return 0;
}

AAssetManager* AndroidPlatform::GetAssetManager() const { return impl_->asset_mgr; }

std::vector<uint8_t> AndroidPlatform::ReadAsset(const std::string& path) const {
  std::vector<uint8_t> data;
  AAsset* asset = AAssetManager_open(impl_->asset_mgr, path.c_str(),
                                      AASSET_MODE_BUFFER);
  if (asset) {
    size_t size = AAsset_getLength(asset);
    data.resize(size);
    memcpy(data.data(), AAsset_getBuffer(asset), size);
    AAsset_close(asset);
  }
  return data;
}

std::string AndroidPlatform::ReadAssetText(const std::string& path) const {
  auto data = ReadAsset(path);
  return std::string(data.begin(), data.end());
}

AndroidPlatform::NetworkType AndroidPlatform::GetNetworkType() const {
  return NetworkType::kWiFi;  // TODO: ConnectivityManager
}

bool AndroidPlatform::IsNetworkAvailable() const { return true; }

float AndroidPlatform::GetBatteryLevel() const {
  return 0.8f; // TODO: BatteryManager
}

bool AndroidPlatform::IsCharging() const { return false; }
bool AndroidPlatform::IsBatterySaverEnabled() const { return false; }

void AndroidPlatform::OnCreate() {}
void AndroidPlatform::OnStart() {}
void AndroidPlatform::OnResume() {
  if (impl_->vulkan_renderer) impl_->vulkan_renderer->OnResume();
}
void AndroidPlatform::OnPause() {
  if (impl_->vulkan_renderer) impl_->vulkan_renderer->OnPause();
}
void AndroidPlatform::OnStop() {}
void AndroidPlatform::OnDestroy() { Shutdown(); }

void AndroidPlatform::SetLowMemoryCallback(LowMemoryCallback callback) {
  impl_->low_memory_cb = std::move(callback);
}

void AndroidPlatform::OnTrimMemory(int32_t level) {
  LOGW("[AndroidPlatform] onTrimMemory level=%d", level);

  switch (level) {
    case 5:  // TRIM_MEMORY_RUNNING_LOW
    case 10: // TRIM_MEMORY_RUNNING_CRITICAL
    case 15: // TRIM_MEMORY_RUNNING_LOW (background)
    case 20: // TRIM_MEMORY_UI_HIDDEN
      if (impl_->low_memory_cb) {
        uint64_t used = 0;
        impl_->low_memory_cb(used);
      }
      break;
  }
}

AndroidPlatform::ThermalStatus AndroidPlatform::GetThermalStatus() const {
  return ThermalStatus::kNone;
}

void AndroidPlatform::SetThermalCallback(ThermalCallback callback) {
  impl_->thermal_cb = std::move(callback);
}

// ============================================================================
// JNI 实现
// ============================================================================

extern "C" {

JNIEXPORT void JNICALL
Java_com_solra_core_SolraCore_nativeInit(
    JNIEnv* env, jobject /*thiz*/,
    jobject asset_manager, jobject surface) {

  // 获取 JavaVM
  JavaVM* jvm = nullptr;
  env->GetJavaVM(&jvm);

  AAssetManager* mgr = AAssetManager_fromJava(env, asset_manager);
  ANativeWindow* window = ANativeWindow_fromSurface(env, surface);

  AndroidPlatform::Instance().Initialize(jvm, env, nullptr, mgr);

  if (AndroidPlatform::Instance().UseVulkan()) {
    auto* renderer = AndroidPlatform::Instance().GetVulkanRenderer();
    renderer->Initialize(window,
        ANativeWindow_getWidth(window),
        ANativeWindow_getHeight(window));
  } else {
    auto* renderer = AndroidPlatform::Instance().GetGLESRenderer();
    renderer->Initialize(window,
        ANativeWindow_getWidth(window),
        ANativeWindow_getHeight(window), 3);
  }
}

JNIEXPORT void JNICALL
Java_com_solra_core_SolraCore_nativeRenderFrame(JNIEnv* /*env*/, jobject /*thiz*/) {
  // 每帧调用
  if (AndroidPlatform::Instance().UseVulkan()) {
    // Vulkan 渲染帧
  } else {
    auto* renderer = AndroidPlatform::Instance().GetGLESRenderer();
    renderer->BeginFrame();
    // ... 渲染命令 ...
    renderer->EndFrame();
  }
}

JNIEXPORT void JNICALL
Java_com_solra_core_SolraCore_nativeOnTouch(
    JNIEnv* /*env*/, jobject /*thiz*/,
    jint action, jfloat x, jfloat y, jint pointer_id) {

  AndroidWindow::TouchEvent event;
  event.action = static_cast<AndroidWindow::TouchEvent::Action>(action);
  event.x = x; event.y = y; event.pointer_id = pointer_id;
  event.event_time_ms = 0;

  // 转发到窗口
}

JNIEXPORT void JNICALL
Java_com_solra_core_SolraCore_nativeOnResume(JNIEnv* /*env*/, jobject /*thiz*/) {
  AndroidPlatform::Instance().OnResume();
}

JNIEXPORT void JNICALL
Java_com_solra_core_SolraCore_nativeOnPause(JNIEnv* /*env*/, jobject /*thiz*/) {
  AndroidPlatform::Instance().OnPause();
}

JNIEXPORT void JNICALL
Java_com_solra_core_SolraCore_nativeOnDestroy(JNIEnv* /*env*/, jobject /*thiz*/) {
  AndroidPlatform::Instance().OnDestroy();
}

JNIEXPORT void JNICALL
Java_com_solra_core_SolraCore_nativeOnSurfaceChanged(
    JNIEnv* env, jobject /*thiz*/,
    jobject surface, jint width, jint height) {

  ANativeWindow* window = ANativeWindow_fromSurface(env, surface);
  if (AndroidPlatform::Instance().UseVulkan()) {
    AndroidPlatform::Instance().GetVulkanRenderer()->ResizeSurface(width, height);
  } else {
    AndroidPlatform::Instance().GetGLESRenderer()->Resize(width, height);
  }
}

} // extern "C"

} // namespace solra::core::platform::android

#else
// 非 Android 平台占位
namespace solra::core::platform::android {
  // 空实现
}
#endif // __ANDROID__
