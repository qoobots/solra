#pragma once
/// @file android_platform.hpp
/// @brief Android 平台层 —— Vulkan/GLES渲染+NNAPI推理+JNI桥接
/// @ingroup core/platform
/// @priority P0 (工程底线——原型H1必须就绪)

#include <cstdint>
#include <functional>
#include <memory>
#include <string>
#include <vector>
#include <unordered_map>

// 条件编译：仅 Android
#if !defined(__ANDROID__)
  #error "This header is for Android platform only."
#endif

#include <android/native_window.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>

#define LOG_TAG "SolraCore"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace solra::core::platform::android {

// ============================================================================
// Android 设备信息
// ============================================================================

struct AndroidDeviceInfo {
  std::string manufacturer;      // 厂商: Samsung, Xiaomi, OPPO...
  std::string model;             // 型号: SM-S9280, 23013RK75C...
  std::string brand;             // 品牌
  std::string device;            // 代号

  std::string soc_name;          // 芯片: Snapdragon 8 Gen 3, Dimensity 9300
  std::string soc_vendor;        // 芯片厂商: Qualcomm, MediaTek, Samsung, HiSilicon
  uint32_t cpu_cores;
  std::string cpu_abi;           // arm64-v8a, armeabi-v7a, x86_64
  uint32_t cpu_max_freq_khz;

  uint64_t total_ram_bytes;
  uint64_t available_ram_bytes;

  // GPU
  std::string gpu_renderer;      // Adreno (TM) 750
  std::string gpu_vendor;        // Qualcomm
  bool supports_vulkan;
  uint32_t vulkan_api_version;   // VK_API_VERSION_1_3
  bool supports_opengl_es3;

  // NPU
  enum class NpuVendor : uint8_t {
    kNone,
    kQualcommHexagon,   // SNPE/QNN
    kMediaTekAPU,       // NeuroPilot
    kSamsungNPU,        // ENN
    kHiSiliconNPU,      // HiAI
    kGoogleEdgeTPU,     // Tensor SoC (Pixel)
  };
  NpuVendor npu_vendor = NpuVendor::kNone;
  bool npu_available = false;
  std::string npu_description;

  // 屏幕
  float density;                 // dpi 密度
  float density_dpi;
  uint32_t screen_width_px;
  uint32_t screen_height_px;

  // 系统版本
  uint32_t api_level;            // SDK_INT
  std::string os_version;

  // 特性
  bool supports_astc_texture;    // ASTC纹理压缩
  bool supports_etc2_texture;    // ETC2纹理压缩
  bool is_64bit;

  // 序列化
  static AndroidDeviceInfo Collect(AAssetManager* amgr = nullptr);
  std::string ToJson() const;
};

// ============================================================================
// Vulkan 渲染后端
// ============================================================================

class VulkanRenderer {
 public:
  VulkanRenderer();
  ~VulkanRenderer();

  // 初始化
  bool Initialize(ANativeWindow* window,
                  uint32_t width, uint32_t height);
  bool InitializeOffscreen();    // 离屏渲染 (推理/计算)
  void Shutdown();

  // 窗口曲面
  bool CreateSurface(ANativeWindow* window);
  void DestroySurface();
  void ResizeSurface(uint32_t width, uint32_t height);

  // 设备查询
  struct VulkanDeviceInfo {
    std::string device_name;
    std::string driver_version;
    uint32_t api_version;
    uint32_t vendor_id;
    uint32_t device_id;
    uint64_t total_vram_bytes;
    bool supports_ray_tracing;
    bool supports_mesh_shaders;
  };

  static std::vector<VulkanDeviceInfo> EnumerateDevices();
  int32_t SelectBestDevice();  // 返回最佳设备索引

  // 渲染循环
  using RenderCallback = std::function<void(double delta_seconds)>;
  void SetRenderCallback(RenderCallback callback);
  void StartRenderLoop();
  void StopRenderLoop();
  void SetTargetFPS(uint32_t fps);

  // 垂直同步
  void SetVsyncEnabled(bool enabled);

  // 动态分辨率 (Qualcomm Adreno / ARM Mali 特性)
  void SetDynamicResolutionEnabled(bool enabled);
  void SetResolutionScale(float scale);

  // 暂停/恢复
  void OnPause();
  void OnResume();

  // 性能
  double GetLastFrameTimeMs() const;
  double GetAverageFPS() const;
  uint64_t GetFrameCount() const;

  // 底层句柄
  void* GetVkInstance() const;      // VkInstance
  void* GetVkDevice() const;        // VkDevice
  void* GetVkPhysicalDevice() const;

 private:
  struct Impl;
  std::unique_ptr<Impl> impl_;
};

// ============================================================================
// OpenGL ES 渲染后端 (降级方案)
// ============================================================================

class GLESRenderer {
 public:
  GLESRenderer();
  ~GLESRenderer();

  bool Initialize(ANativeWindow* window,
                  uint32_t width, uint32_t height,
                  uint32_t version = 3);  // GLES 2.0 or 3.0
  void Shutdown();

  void BeginFrame();
  void EndFrame();

  void Resize(uint32_t width, uint32_t height);

  // 查询
  std::string GetGLVersion() const;
  std::string GetGLRenderer() const;
  std::string GetGLVendor() const;
  bool HasExtension(const std::string& ext) const;

  // EGL 句柄
  void* GetEGLDisplay() const;
  void* GetEGLContext() const;

 private:
  struct Impl;
  std::unique_ptr<Impl> impl_;
};

// ============================================================================
// NNAPI 推理后端
// ============================================================================

class NnapiInference {
 public:
  NnapiInference();
  ~NnapiInference();

  // 模型格式
  enum class ModelFormat : uint8_t {
    kTFLite,            // TensorFlow Lite (.tflite)
    kONNX,              // ONNX (需转换)
    kSNPE,              // Qualcomm SNPE (.dlc)
    kHiAI,              // Huawei HiAI (.om)
  };

  // 加载模型
  bool LoadModel(const std::string& model_path, ModelFormat format);
  bool LoadFromAsset(AAssetManager* mgr, const std::string& asset_path);
  bool LoadFromBuffer(const uint8_t* data, size_t size, ModelFormat format);
  void UnloadModel();
  bool IsLoaded() const;

  // 推理
  struct Tensor {
    enum class Type : uint8_t { kFloat32, kFloat16, kInt32, kUint8, kInt8 };

    std::string name;
    Type type;
    std::vector<int32_t> shape;
    std::vector<uint8_t> data;  // 原始字节
    size_t size_bytes;
  };

  std::vector<Tensor> Predict(const std::vector<Tensor>& inputs);

  // 异步推理
  using PredictCallback = std::function<void(std::vector<Tensor> outputs)>;
  void PredictAsync(const std::vector<Tensor>& inputs, PredictCallback callback);

  // NNAPI 偏好
  enum class ExecutionPreference : uint8_t {
    kFastSingleAnswer,   // 低延迟
    kLowPower,           // 省电
    kSustainedSpeed,     // 持续速度 (推荐)
  };
  void SetPreference(ExecutionPreference pref);

  // 厂商特定
  void SetNpuAcceleration(bool enable);  // 启用NPU加速
  bool IsUsingNpu() const;

  // 模型信息
  std::string GetModelInfo() const;
  uint64_t GetModelSize() const;

  // NNAPI 版本
  static uint32_t GetNnapiVersion(); // ANEURALNETWORKS_*

 private:
  struct Impl;
  std::unique_ptr<Impl> impl_;
};

// ============================================================================
// Android 窗口管理
// ============================================================================

class AndroidWindow {
 public:
  AndroidWindow();
  ~AndroidWindow();

  bool Create(ANativeWindow* native_window);
  void Destroy();

  // 输入事件
  struct TouchEvent {
    enum class Action : uint8_t { kDown, kUp, kMove, kCancel, kPointerDown, kPointerUp };
    Action action;
    uint32_t pointer_id;
    float x, y;              // 归一化坐标
    float pressure;
    float size;              // 触摸区域
    uint64_t event_time_ms;
  };

  using TouchCallback = std::function<void(const TouchEvent& event)>;
  void SetTouchCallback(TouchCallback callback);
  bool HandleTouchEvent(const TouchEvent& event);

  // 手势
  using GestureCallback = std::function<void(float pinch_scale,
                                              float rotation,
                                              float pan_x, float pan_y)>;
  void SetGestureCallback(GestureCallback callback);

  // 键盘 (外接键盘)
  using KeyCallback = std::function<void(int32_t keycode, bool is_down)>;
  void SetKeyCallback(KeyCallback callback);

 private:
  struct Impl;
  std::unique_ptr<Impl> impl_;
};

// ============================================================================
// Android 平台基础设施
// ============================================================================

class AndroidPlatform {
 public:
  static AndroidPlatform& Instance();

  // 初始化 (需JNI传递JavaVM和Activity)
  bool Initialize(void* java_vm, void* jni_env,
                  void* native_activity, AAssetManager* asset_mgr);
  void Shutdown();

  // 组件
  VulkanRenderer* GetVulkanRenderer();
  GLESRenderer* GetGLESRenderer();
  NnapiInference* GetNnapiInference();
  AndroidWindow* GetWindow();

  // 渲染器选择
  bool UseVulkan() const;
  bool FallbackToGLES();

  // 设备信息
  const AndroidDeviceInfo& GetDeviceInfo() const;

  // 文件系统
  std::string GetInternalPath() const;       // /data/data/<pkg>/files
  std::string GetExternalPath() const;       // /sdcard/Android/data/<pkg>/files
  std::string GetCachePath() const;
  std::string GetObbPath() const;            // APK Expansion
  uint64_t GetFreeDiskSpace() const;

  // Asset 管理
  AAssetManager* GetAssetManager() const;
  std::vector<uint8_t> ReadAsset(const std::string& path) const;
  std::string ReadAssetText(const std::string& path) const;
  std::vector<std::string> ListAssets(const std::string& dir) const;

  // 网络
  enum class NetworkType { kNone, kWiFi, kCellular, kEthernet, kVPN };
  NetworkType GetNetworkType() const;
  bool IsNetworkAvailable() const;
  int32_t GetNetworkBandwidthKbps() const;

  // 电量
  float GetBatteryLevel() const;
  bool IsCharging() const;
  bool IsBatterySaverEnabled() const;

  // 应用生命周期
  void OnCreate();
  void OnStart();
  void OnResume();
  void OnPause();
  void OnStop();
  void OnDestroy();

  // 内存
  using LowMemoryCallback = std::function<void(uint64_t used_bytes)>;
  void SetLowMemoryCallback(LowMemoryCallback callback);
  void OnTrimMemory(int32_t level);  // ComponentCallbacks2

  // 热管理 (Android 10+)
  enum class ThermalStatus : uint8_t {
    kNone, kLight, kModerate, kSevere, kCritical, kEmergency, kShutdown
  };
  ThermalStatus GetThermalStatus() const;
  using ThermalCallback = std::function<void(ThermalStatus status)>;
  void SetThermalCallback(ThermalCallback callback);

 private:
  AndroidPlatform();
  struct Impl;
  std::unique_ptr<Impl> impl_;
};

// ============================================================================
// JNI 接口 (供 Java 层调用)
// ============================================================================

extern "C" {

// 初始化
JNIEXPORT void JNICALL
Java_com_solra_core_SolraCore_nativeInit(
    JNIEnv* env, jobject thiz,
    jobject asset_manager, jobject surface);

// 渲染帧
JNIEXPORT void JNICALL
Java_com_solra_core_SolraCore_nativeRenderFrame(JNIEnv* env, jobject thiz);

// 触摸
JNIEXPORT void JNICALL
Java_com_solra_core_SolraCore_nativeOnTouch(
    JNIEnv* env, jobject thiz,
    jint action, jfloat x, jfloat y, jint pointer_id);

// 生命周期
JNIEXPORT void JNICALL
Java_com_solra_core_SolraCore_nativeOnResume(JNIEnv* env, jobject thiz);
JNIEXPORT void JNICALL
Java_com_solra_core_SolraCore_nativeOnPause(JNIEnv* env, jobject thiz);
JNIEXPORT void JNICALL
Java_com_solra_core_SolraCore_nativeOnDestroy(JNIEnv* env, jobject thiz);

// 表面变更
JNIEXPORT void JNICALL
Java_com_solra_core_SolraCore_nativeOnSurfaceChanged(
    JNIEnv* env, jobject thiz,
    jobject surface, jint width, jint height);

} // extern "C"

} // namespace solra::core::platform::android
