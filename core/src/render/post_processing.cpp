#include "post_processing.hpp"

#include <algorithm>
#include <cmath>
#include <random>
#include <spdlog/spdlog.h>

namespace solra::core::render {

// ============================================================================
// BloomEffect
// ============================================================================

BloomEffect::BloomEffect() = default;
BloomEffect::~BloomEffect() { DestroyIntermediateTextures(); }

void BloomEffect::Apply(uint32_t input_texture, uint32_t output_texture,
                         int width, int height) {
  if (!config_.enabled) return;
  // 实际 GPU 实现：bright pass → blur → composite
  // 这里提供框架接口，实际 OpenGL/Vulkan 调用由上层处理
  Resize(width, height);
}

void BloomEffect::Resize(int width, int height) {
  if (width_ == width && height_ == height) return;
  width_ = width;
  height_ = height;
  DestroyIntermediateTextures();
  CreateIntermediateTextures();
}

void BloomEffect::CreateIntermediateTextures() {
  if (width_ <= 0 || height_ <= 0) return;
  // 半分辨率纹理（4x 性能优化）
  int hw = width_ / 2;
  int hh = height_ / 2;
  // 实际 GPU 资源创建由上层调用 OpenGL/Vulkan API
  spdlog::debug("Bloom: created intermediate textures {}x{}", hw, hh);
}

void BloomEffect::DestroyIntermediateTextures() {
  // 实际 GPU 资源释放由上层处理
  bright_pass_tex_ = blur_ping_tex_ = blur_pong_tex_ = 0;
  bright_pass_fbo_ = blur_ping_fbo_ = blur_pong_fbo_ = 0;
}

std::string BloomEffect::GetBrightPassShader() {
  return R"(
#version 450 core
in vec2 vUV;
out vec4 fragColor;

uniform sampler2D uSceneColor;
uniform float uThreshold;
uniform float uKnee;

vec3 BrightPass(vec3 color) {
    float brightness = max(color.r, max(color.g, color.b));
    float soft = brightness - uThreshold + uKnee;
    soft = clamp(soft, 0.0, 2.0 * uKnee);
    soft = soft * soft / (4.0 * uKnee + 1e-4);
    float contribution = max(soft, brightness - uThreshold);
    contribution /= max(brightness, 1e-4);
    return color * contribution;
}

void main() {
    vec3 color = texture(uSceneColor, vUV).rgb;
    fragColor = vec4(BrightPass(color), 1.0);
}
)";
}

std::string BloomEffect::GetBlurHorizontalShader() {
  return R"(
#version 450 core
in vec2 vUV;
out vec4 fragColor;

uniform sampler2D uInput;
uniform vec2 uTexelSize;
uniform float uBlurSize;

const float weights[5] = float[](0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);

void main() {
    vec3 result = texture(uInput, vUV).rgb * weights[0];
    for (int i = 1; i < 5; ++i) {
        float offset = float(i) * uBlurSize;
        result += texture(uInput, vUV + vec2(uTexelSize.x * offset, 0.0)).rgb * weights[i];
        result += texture(uInput, vUV - vec2(uTexelSize.x * offset, 0.0)).rgb * weights[i];
    }
    fragColor = vec4(result, 1.0);
}
)";
}

std::string BloomEffect::GetBlurVerticalShader() {
  return R"(
#version 450 core
in vec2 vUV;
out vec4 fragColor;

uniform sampler2D uInput;
uniform vec2 uTexelSize;
uniform float uBlurSize;

const float weights[5] = float[](0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);

void main() {
    vec3 result = texture(uInput, vUV).rgb * weights[0];
    for (int i = 1; i < 5; ++i) {
        float offset = float(i) * uBlurSize;
        result += texture(uInput, vUV + vec2(0.0, uTexelSize.y * offset)).rgb * weights[i];
        result += texture(uInput, vUV - vec2(0.0, uTexelSize.y * offset)).rgb * weights[i];
    }
    fragColor = vec4(result, 1.0);
}
)";
}

std::string BloomEffect::GetCompositeShader() {
  return R"(
#version 450 core
in vec2 vUV;
out vec4 fragColor;

uniform sampler2D uSceneColor;
uniform sampler2D uBloomBlur;
uniform float uIntensity;
uniform float uScatter;

void main() {
    vec3 scene = texture(uSceneColor, vUV).rgb;
    vec3 bloom = texture(uBloomBlur, vUV).rgb;
    // 散射效果：高亮度区域向周围扩散
    float scatter = uScatter * length(bloom);
    vec3 result = mix(scene, bloom, uIntensity) + bloom * scatter;
    fragColor = vec4(result, 1.0);
}
)";
}

// ============================================================================
// TAAEffect
// ============================================================================

TAAEffect::TAAEffect() = default;
TAAEffect::~TAAEffect() { DestroyHistoryTextures(); }

void TAAEffect::Apply(uint32_t input_texture, uint32_t output_texture,
                       int width, int height) {
  if (!config_.enabled) return;
  Resize(width, height);
  frame_index_++;
}

void TAAEffect::Resize(int width, int height) {
  if (width_ == width && height_ == height) return;
  width_ = width;
  height_ = height;
  frame_index_ = 0;
  DestroyHistoryTextures();
  CreateHistoryTextures();
}

void TAAEffect::SetPreviousViewProjection(const glm::mat4& prev_vp) {
  prev_view_projection_ = prev_vp;
}

glm::vec2 TAAEffect::GetJitter() const {
  return GenerateJitter(frame_index_);
}

glm::vec2 TAAEffect::GenerateJitter(int frame_idx) const {
  // Halton 序列 (2,3) — 低差异采样模式
  auto halton = [](int index, int base) -> float {
    float result = 0.0f;
    float f = 1.0f / base;
    int i = index;
    while (i > 0) {
      result += f * (i % base);
      i /= base;
      f /= base;
    }
    return result;
  };

  float jx = (halton(frame_idx % 8 + 1, 2) - 0.5f) * 2.0f;
  float jy = (halton(frame_idx % 8 + 1, 3) - 0.5f) * 2.0f;

  // 缩放 jitter 到亚像素范围
  if (width_ > 0 && height_ > 0) {
    jx /= static_cast<float>(width_);
    jy /= static_cast<float>(height_);
  }

  return glm::vec2(jx, jy);
}

void TAAEffect::CreateHistoryTextures() {
  if (width_ <= 0 || height_ <= 0) return;
  spdlog::debug("TAA: created history textures {}x{}", width_, height_);
}

void TAAEffect::DestroyHistoryTextures() {
  history_tex_ = velocity_tex_ = 0;
  history_fbo_ = velocity_fbo_ = 0;
}

std::string TAAEffect::GetTAAShader() {
  return R"(
#version 450 core
in vec2 vUV;
out vec4 fragColor;

uniform sampler2D uCurrentColor;
uniform sampler2D uHistoryColor;
uniform sampler2D uVelocity;
uniform sampler2D uDepth;
uniform float uFeedback;
uniform float uMotionAmp;
uniform bool uUseYCoCg;
uniform bool uUseClamping;

vec3 RGB2YCoCg(vec3 rgb) {
    float y  = 0.25 * rgb.r + 0.5 * rgb.g + 0.25 * rgb.b;
    float co = 0.5  * rgb.r - 0.5 * rgb.b;
    float cg = -0.25 * rgb.r + 0.5 * rgb.g - 0.25 * rgb.b;
    return vec3(y, co, cg);
}

vec3 YCoCg2RGB(vec3 ycocg) {
    float r = ycocg.x + ycocg.y - ycocg.z;
    float g = ycocg.x + ycocg.z;
    float b = ycocg.x - ycocg.y - ycocg.z;
    return vec3(r, g, b);
}

vec3 ClipAABB(vec3 color, vec3 min_color, vec3 max_color) {
    vec3 center = 0.5 * (max_color + min_color);
    vec3 extent = 0.5 * (max_color - min_color);
    vec3 offset = color - center;
    vec3 v_unit = offset / max(extent, 1e-4);
    float max_unit = max(abs(v_unit.x), max(abs(v_unit.y), abs(v_unit.z)));
    if (max_unit > 1.0) offset /= max_unit;
    return center + offset;
}

void main() {
    vec2 velocity = texture(uVelocity, vUV).xy * uMotionAmp;
    vec2 history_uv = vUV - velocity;

    vec3 current = texture(uCurrentColor, vUV).rgb;
    vec3 history = texture(uHistoryColor, history_uv).rgb;

    if (uUseYCoCg) {
        current = RGB2YCoCg(current);
        history = RGB2YCoCg(history);
    }

    if (uUseClamping) {
        // 3x3 邻域裁剪
        vec3 neighbors[9];
        vec2 texel_size = 1.0 / vec2(textureSize(uCurrentColor, 0));
        for (int i = 0; i < 9; ++i) {
            int x = (i % 3) - 1;
            int y = (i / 3) - 1;
            neighbors[i] = texture(uCurrentColor, vUV + vec2(x, y) * texel_size).rgb;
            if (uUseYCoCg) neighbors[i] = RGB2YCoCg(neighbors[i]);
        }
        vec3 mn = neighbors[0], mx = neighbors[0];
        for (int i = 1; i < 9; ++i) {
            mn = min(mn, neighbors[i]);
            mx = max(mx, neighbors[i]);
        }
        history = ClipAABB(history, mn, mx);
    }

    float depth = texture(uDepth, vUV).r;
    float feedback = mix(uFeedback, 0.95, depth); // 远处更多历史

    vec3 result = mix(current, history, feedback);

    if (uUseYCoCg) {
        result = YCoCg2RGB(result);
    }

    fragColor = vec4(result, 1.0);
}
)";
}

std::string TAAEffect::GetSharpenShader() {
  return R"(
#version 450 core
in vec2 vUV;
out vec4 fragColor;

uniform sampler2D uInput;
uniform float uStrength;
uniform vec2 uTexelSize;

void main() {
    vec3 color = texture(uInput, vUV).rgb;
    vec3 n = texture(uInput, vUV + vec2(0, -uTexelSize.y)).rgb;
    vec3 s = texture(uInput, vUV + vec2(0,  uTexelSize.y)).rgb;
    vec3 e = texture(uInput, vUV + vec2( uTexelSize.x, 0)).rgb;
    vec3 w = texture(uInput, vUV + vec2(-uTexelSize.x, 0)).rgb;

    vec3 laplacian = 4.0 * color - n - s - e - w;
    vec3 result = color - uStrength * laplacian;

    fragColor = vec4(result, 1.0);
}
)";
}

// ============================================================================
// PostProcessingPipeline
// ============================================================================

PostProcessingPipeline::PostProcessingPipeline()
    : bloom_(std::make_unique<BloomEffect>()),
      taa_(std::make_unique<TAAEffect>()) {}

PostProcessingPipeline::~PostProcessingPipeline() {
  DestroyPingPongTextures();
}

void PostProcessingPipeline::EnableBloom(bool enable) {
  bloom_enabled_ = enable;
  bloom_config_.enabled = enable;
  bloom_->SetConfig(bloom_config_);
}

void PostProcessingPipeline::EnableTAA(bool enable) {
  taa_enabled_ = enable;
  taa_config_.enabled = enable;
  taa_->SetConfig(taa_config_);
}

bool PostProcessingPipeline::IsBloomEnabled() const { return bloom_enabled_; }
bool PostProcessingPipeline::IsTAAEnabled() const { return taa_enabled_; }

void PostProcessingPipeline::SetBloomConfig(const BloomConfig& config) {
  bloom_config_ = config;
  bloom_->SetConfig(config);
}

void PostProcessingPipeline::SetTAAConfig(const TAAConfig& config) {
  taa_config_ = config;
  taa_->SetConfig(config);
}

void PostProcessingPipeline::Process(uint32_t scene_color_texture,
                                      uint32_t scene_depth_texture,
                                      uint32_t output_texture,
                                      int width, int height) {
  if (width != width_ || height != height_) {
    Resize(width, height);
  }

  uint32_t current_input = scene_color_texture;
  uint32_t current_output = ping_tex_;

  // 1. TAA (如果启用)
  if (taa_enabled_) {
    taa_->Apply(current_input, current_output, width, height);
    std::swap(current_input, current_output);
    current_output = (current_output == ping_tex_) ? pong_tex_ : ping_tex_;
  }

  // 2. Bloom (如果启用)
  if (bloom_enabled_) {
    bloom_->Apply(current_input, current_output, width, height);
    std::swap(current_input, current_output);
  }

  // 最终输出
  // current_input 现在包含最终结果
  // 实际 GPU blit 由上层处理
  (void)output_texture;
  (void)scene_depth_texture;
}

glm::vec2 PostProcessingPipeline::GetTAAJitter() const {
  return taa_->GetJitter();
}

void PostProcessingPipeline::SetPreviousViewProjection(const glm::mat4& prev_vp) {
  taa_->SetPreviousViewProjection(prev_vp);
}

void PostProcessingPipeline::Resize(int width, int height) {
  width_ = width;
  height_ = height;
  bloom_->Resize(width, height);
  taa_->Resize(width, height);
  DestroyPingPongTextures();
  CreatePingPongTextures();
}

void PostProcessingPipeline::CreatePingPongTextures() {
  if (width_ <= 0 || height_ <= 0) return;
  spdlog::debug("PostProcessing: created ping-pong textures {}x{}", width_, height_);
}

void PostProcessingPipeline::DestroyPingPongTextures() {
  ping_tex_ = pong_tex_ = 0;
  ping_fbo_ = pong_fbo_ = 0;
}

PostProcessingPipeline& PostProcessingPipeline::Instance() {
  static PostProcessingPipeline instance;
  return instance;
}

} // namespace solra::core::render
