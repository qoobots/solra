#pragma once
/// @file post_processing.hpp
/// @brief 后处理效果 —— Bloom / TAA / ToneMapping
/// @ingroup core/render
/// @priority P3 (优化阶段)

#include <cstdint>
#include <memory>
#include <string>
#include <vector>
#include <glm/glm.hpp>

namespace solra::core::render {

// ============================================================================
// 后处理统一接口
// ============================================================================

class IPostProcessEffect {
 public:
  virtual ~IPostProcessEffect() = default;
  virtual void Apply(uint32_t input_texture, uint32_t output_texture,
                      int width, int height) = 0;
  virtual void Resize(int width, int height) = 0;
  virtual const char* Name() const = 0;
};

// ============================================================================
// Bloom 泛光效果
// ============================================================================

struct BloomConfig {
  float threshold        = 1.0f;    // 亮度阈值（超过此值的像素参与泛光）
  float knee             = 0.5f;    // 软阈值过渡
  float intensity        = 0.8f;    // 泛光强度
  float scatter          = 0.6f;    // 散射系数
  int   blur_passes      = 5;       // 模糊迭代次数
  float blur_size        = 1.0f;    // 模糊核大小
  bool  enabled          = true;
};

class BloomEffect : public IPostProcessEffect {
 public:
  BloomEffect();
  ~BloomEffect() override;

  void SetConfig(const BloomConfig& config) { config_ = config; }
  const BloomConfig& GetConfig() const { return config_; }

  void Apply(uint32_t input_texture, uint32_t output_texture,
              int width, int height) override;
  void Resize(int width, int height) override;
  const char* Name() const override { return "Bloom"; }

  /// 生成 Bloom 所需的着色器源码
  static std::string GetBrightPassShader();
  static std::string GetBlurHorizontalShader();
  static std::string GetBlurVerticalShader();
  static std::string GetCompositeShader();

 private:
  BloomConfig config_;
  int width_ = 0, height_ = 0;

  // 中间纹理（实际使用 GPU 纹理句柄）
  uint32_t bright_pass_tex_  = 0;
  uint32_t blur_ping_tex_    = 0;
  uint32_t blur_pong_tex_    = 0;
  uint32_t bright_pass_fbo_  = 0;
  uint32_t blur_ping_fbo_    = 0;
  uint32_t blur_pong_fbo_    = 0;

  void CreateIntermediateTextures();
  void DestroyIntermediateTextures();
  void ApplyBlurPasses(int blur_count);
};

// ============================================================================
// TAA 时间抗锯齿
// ============================================================================

struct TAAConfig {
  float feedback_min       = 0.85f;  // 最小历史混合系数
  float feedback_max       = 0.95f;  // 最大历史混合系数
  float motion_amp         = 1.0f;   // 运动放大系数
  bool  use_ycocg          = true;   // YCoCg 颜色空间（减少鬼影）
  bool  use_clamping       = true;   // 邻域裁剪（减少鬼影）
  bool  use_sharpen        = true;   // 锐化
  float sharpen_strength   = 0.25f;  // 锐化强度
  int   sample_count       = 8;      // 采样数（TAA 使用 jitter）
  bool  enabled            = true;
};

class TAAEffect : public IPostProcessEffect {
 public:
  TAAEffect();
  ~TAAEffect() override;

  void SetConfig(const TAAConfig& config) { config_ = config; }
  const TAAConfig& GetConfig() const { return config_; }

  void Apply(uint32_t input_texture, uint32_t output_texture,
              int width, int height) override;
  void Resize(int width, int height) override;
  const char* Name() const override { return "TAA"; }

  /// 获取当前帧的 jitter offset（用于投影矩阵）
  glm::vec2 GetJitter() const;
  /// 设置上一帧的 VP 矩阵（用于 motion vector 计算）
  void SetPreviousViewProjection(const glm::mat4& prev_vp);

  /// 生成 TAA 所需的着色器源码
  static std::string GetTAAShader();
  static std::string GetSharpenShader();

 private:
  TAAConfig config_;
  int width_ = 0, height_ = 0;
  int frame_index_ = 0;

  uint32_t history_tex_  = 0;
  uint32_t history_fbo_  = 0;
  uint32_t velocity_tex_ = 0;
  uint32_t velocity_fbo_ = 0;

  glm::mat4 prev_view_projection_{1.0f};
  glm::mat4 prev_view_projection_no_jitter_{1.0f};

  void CreateHistoryTextures();
  void DestroyHistoryTextures();
  glm::vec2 GenerateJitter(int frame_idx) const;
};

// ============================================================================
// 后处理管线管理器
// ============================================================================

class PostProcessingPipeline {
 public:
  PostProcessingPipeline();
  ~PostProcessingPipeline();

  // 效果管理
  void EnableBloom(bool enable);
  void EnableTAA(bool enable);
  bool IsBloomEnabled() const;
  bool IsTAAEnabled() const;

  // 配置
  void SetBloomConfig(const BloomConfig& config);
  void SetTAAConfig(const TAAConfig& config);
  const BloomConfig& GetBloomConfig() const { return bloom_config_; }
  const TAAConfig& GetTAAConfig() const { return taa_config_; }

  // 渲染
  /// 应用完整的后处理管线
  void Process(uint32_t scene_color_texture,
                uint32_t scene_depth_texture,
                uint32_t output_texture,
                int width, int height);

  /// 获取当前帧的 TAA jitter offset
  glm::vec2 GetTAAJitter() const;

  /// 设置上一帧 VP 矩阵（TAA 需要）
  void SetPreviousViewProjection(const glm::mat4& prev_vp);

  /// 调整尺寸
  void Resize(int width, int height);

  // 全局单例
  static PostProcessingPipeline& Instance();

 private:
  BloomConfig bloom_config_;
  TAAConfig taa_config_;
  bool bloom_enabled_ = false;
  bool taa_enabled_ = false;

  std::unique_ptr<BloomEffect> bloom_;
  std::unique_ptr<TAAEffect> taa_;

  uint32_t ping_tex_ = 0, pong_tex_ = 0;
  uint32_t ping_fbo_ = 0, pong_fbo_ = 0;
  int width_ = 0, height_ = 0;

  void CreatePingPongTextures();
  void DestroyPingPongTextures();
};

} // namespace solra::core::render
