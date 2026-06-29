#pragma once
/// @file ios_platform.hpp
/// @brief iOS 平台层 —— Metal渲染+CoreML推理+UIKit集成
/// @ingroup core/platform
/// @priority P0 (工程底线——原型H1必须就绪)

#include <cstdint>
#include <functional>
#include <memory>
#include <string>
#include <vector>

// 条件编译：仅 iOS/macOS (Apple 平台)
#if !defined(__APPLE__)
  #error "This header is for Apple platforms only. Include platform/xxx_platform.hpp for your target."
#endif

#ifdef __OBJC__
  @class MTLDevice;
  @class MTKView;
  @class CAMetalLayer;
  @class UIView;
  @class UIViewController;
  @class MLModel;
  @class EAGLContext;
  @class CADisplayLink;
#else
  using MTLDevice = void;
  using MTKView = void;
  using CAMetalLayer = void;
  using UIView = void;
  using UIViewController = void;
  using MLModel = void;
  using EAGLContext = void;
  using CADisplayLink = void;
#endif

namespace solra::core::platform::ios {

// ============================================================================
// iOS 设备信息
// ============================================================================

enum class iOSDeviceFamily : uint8_t {
  kiPhone,
  kiPad,
  kiPodTouch,
  kAppleTV,
  kMacCatalyst,
  kVisionPro,
  kSimulator,
  kUnknown,
};

struct iOSDeviceInfo {
  iOSDeviceFamily family;
  std::string model_name;        // e.g. "iPhone16,2"
  std::string marketing_name;    // e.g. "iPhone 15 Pro"

  uint32_t cpu_cores;
  uint32_t gpu_cores;           // Apple GPU 核心数
  uint64_t total_ram_bytes;
  uint64_t gpu_ram_bytes;       // 统一内存中分配给GPU的部分

  // 芯片代号
  std::string chip_name;        // e.g. "A17 Pro", "M2"
  bool has_neural_engine;       // ANE (Apple Neural Engine)
  uint32_t neural_engine_cores; // ANE 核心数

  // 屏幕
  float screen_scale;           // @2x / @3x
  uint32_t screen_width_px;
  uint32_t screen_height_px;

  // 系统版本
  std::string os_version;
  uint32_t os_major;
  uint32_t os_minor;
  uint32_t os_patch;

  // 支持特性
  bool supports_metal3;
  bool supports_ray_tracing;    // M3+ / A17 Pro+
  bool supports_mesh_shaders;
  bool supports_dynamic_resolution;

  // 序列化
  static iOSDeviceInfo Collect();
  std::string ToJson() const;
};

// ============================================================================
// Metal 渲染后端
// ============================================================================

class MetalRenderer {
 public:
  MetalRenderer();
  ~MetalRenderer();

  // 初始化
  bool Initialize(UIView* native_view);
  bool InitializeLayer(CAMetalLayer* layer);
  void Shutdown();

  // 设备获取
  static MTLDevice* GetPreferredDevice();    // 优先选择独立GPU (Mac)
  static MTLDevice* GetDefaultDevice();      // 默认设备
  static std::vector<MTLDevice*> GetAllDevices();

  // 视图管理
  MTKView* GetMetalView() const;
  CAMetalLayer* GetMetalLayer() const;

  // 渲染循环
  using RenderCallback = std::function<void(double delta_seconds)>;
  void SetRenderCallback(RenderCallback callback);
  void StartRenderLoop();
  void StopRenderLoop();
  void SetPreferredFPS(uint32_t fps);       // 30/60/120

  // 垂直同步
  void SetVsyncEnabled(bool enabled);

  // 动态分辨率
  void SetDynamicResolutionEnabled(bool enabled);
  void SetResolutionScale(float scale);     // 0.5 - 1.0
  float GetCurrentResolutionScale() const;

  // 暂停/前台/后台
  void OnEnterBackground();
  void OnEnterForeground();

  // 性能查询
  double GetLastFrameTimeMs() const;
  double GetAverageFPS() const;
  uint64_t GetFrameCount() const;

  // 纹理内存统计
  uint64_t GetTextureMemoryUsage() const;

 private:
  struct Impl;
  std::unique_ptr<Impl> impl_;
};

// ============================================================================
// CoreML 推理后端
// ============================================================================

class CoreMLInference {
 public:
  CoreMLInference();
  ~CoreMLInference();

  // 模型加载
  bool LoadModel(const std::string& mlmodel_path);
  bool LoadCompiledModel(const std::string& mlmodelc_path);
  void UnloadModel();
  bool IsLoaded() const;

  // 推理
  // 输入/输出: MultiArray (适合 LLM token / embedding)
  struct MultiArray {
    std::vector<float> data;
    std::vector<int64_t> shape;
    std::string name;
  };

  // 同步推理
  std::vector<MultiArray> Predict(const std::vector<MultiArray>& inputs);

  // 异步推理 (ANE 线程)
  using PredictCallback = std::function<void(std::vector<MultiArray> outputs)>;
  void PredictAsync(const std::vector<MultiArray>& inputs,
                    PredictCallback callback);

  // ANE (Apple Neural Engine) 控制
  bool IsUsingANE() const;
  void SetPreferANE(bool prefer);

  // GPU 推理 (Metal Performance Shaders)
  bool IsUsingGPU() const;
  void SetAllowGPU(bool allow);

  // 计算单元选择 (iOS 14+)
  enum class ComputeUnits : uint8_t {
    kCPUOnly,
    kCPUAndGPU,
    kAll,           // CPU + GPU + ANE
    kCPUAndANE,     // 仅 CPU + ANE
  };
  void SetComputeUnits(ComputeUnits units);
  ComputeUnits GetComputeUnits() const;

  // 模型信息
  std::string GetModelDescription() const;
  uint64_t GetModelSize() const;

 private:
  struct Impl;
  std::unique_ptr<Impl> impl_;
};

// ============================================================================
// iOS 窗口管理
// ============================================================================

class iOSWindow {
 public:
  iOSWindow();
  ~iOSWindow();

  // 创建窗口
  bool Create(const std::string& title,
              uint32_t width, uint32_t height,
              UIViewController* parent_vc = nullptr);

  void Destroy();

  // 视图获取
  UIView* GetRootView() const;

  // 触摸事件
  struct TouchEvent {
    enum class Phase : uint8_t { kBegan, kMoved, kEnded, kCancelled };
    Phase phase;
    float x, y;              // 归一化坐标 [0,1]
    uint64_t timestamp_ms;
    float force;             // 3D Touch / Pencil pressure
    float major_radius;      // 触摸半径
  };

  using TouchCallback = std::function<void(const TouchEvent& event)>;
  void SetTouchCallback(TouchCallback callback);

  // 手势
  using GestureCallback = std::function<void(const std::string& gesture_type,
                                              float x, float y, float scale)>;
  void SetGestureCallback(GestureCallback callback);

  // 生命周期
  using LifecycleCallback = std::function<void(bool is_active)>;
  void SetLifecycleCallback(LifecycleCallback callback);

 private:
  struct Impl;
  std::unique_ptr<Impl> impl_;
};

// ============================================================================
// iOS 基础设施
// ============================================================================

class iOSPlatform {
 public:
  static iOSPlatform& Instance();

  // 初始化
  bool Initialize();
  void Shutdown();

  // 组件获取
  MetalRenderer* GetRenderer();
  CoreMLInference* GetInference();
  iOSWindow* GetWindow();

  // 设备信息
  const iOSDeviceInfo& GetDeviceInfo() const;

  // 文件系统
  std::string GetBundlePath() const;
  std::string GetDocumentsPath() const;
  std::string GetCachesPath() const;
  std::string GetTemporaryPath() const;
  uint64_t GetFreeDiskSpace() const;

  // 网络
  bool IsNetworkAvailable() const;
  bool IsWiFiConnected() const;
  bool IsCellularConnected() const;

  // 电量
  float GetBatteryLevel() const;          // 0-1
  bool IsCharging() const;
  bool IsLowPowerModeEnabled() const;

  // 系统
  void KeepScreenOn(bool keep_on);
  void SetIdleTimerDisabled(bool disabled);

  // 应用生命周期
  void OnApplicationDidFinishLaunching();
  void OnApplicationWillResignActive();
  void OnApplicationDidEnterBackground();
  void OnApplicationWillEnterForeground();
  void OnApplicationDidBecomeActive();
  void OnApplicationWillTerminate();

  // 内存警告
  using MemoryWarningCallback = std::function<void(uint64_t used_bytes)>;
  void SetMemoryWarningCallback(MemoryWarningCallback callback);

 private:
  iOSPlatform();
  struct Impl;
  std::unique_ptr<Impl> impl_;
};

} // namespace solra::core::platform::ios
