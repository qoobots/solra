#pragma once
/// @file npc_dialogue.hpp
/// @brief 端侧NPC对话系统 —— 人格化LLM推理 + 表情动画驱动
/// @ingroup core/inference
/// @priority P2 (原型阶段)

#include <cstdint>
#include <functional>
#include <memory>
#include <string>
#include <vector>
#include <map>
#include <chrono>
#include <mutex>
#include <atomic>

namespace solra::core::inference {

// ============================================================================
// 五维情感模型 (复用自 AVT-004)
// ============================================================================

struct FiveDimensionalEmotion {
  float joy       = 0.5f;  // 愉悦 [0,1]
  float curiosity = 0.5f;  // 好奇 [0,1]
  float coldness  = 0.3f;  // 冷漠 [0,1]
  float jealousy  = 0.1f;  // 嫉妒 [0,1]
  float sadness   = 0.1f;  // 悲伤 [0,1]

  /// 事件驱动的情绪更新（带时间衰减）
  void ApplyEvent(const std::string& event_type, float intensity);
  /// 时间衰减 (每秒衰减率)
  void Decay(double delta_seconds, float decay_rate = 0.01f);
  /// 主导情绪
  std::string DominantEmotion() const;
  /// 归一化
  void Normalize();
};

// ============================================================================
// 好感度系统 (复用自 AVT-009)
// ============================================================================

enum class AffectionLevel : uint8_t {
  kL1_Stranger   = 1,   // 陌生人
  kL2_Acquainted = 2,   // 相识
  kL3_Familiar   = 3,   // 熟悉
  kL4_Friendly   = 4,   // 友好
  kL5_Close      = 5,   // 亲近
  kL6_Trusted    = 6,   // 信任
  kL7_Intimate   = 7,   // 亲密
  kL8_Devoted    = 8,   // 挚爱
  kL9_Soulmate   = 9,   // 灵魂伴侣
  kL10_Bonded    = 10,  // 羁绊
};

struct AffectionState {
  float score            = 0.0f;   // 好感度分数 [0, 1000]
  AffectionLevel level   = AffectionLevel::kL1_Stranger;
  uint64_t interaction_count = 0;
  double last_interaction_time = 0.0; // epoch seconds

  void AddAffection(float points);
  void ApplyTimeDecay(double current_time, float decay_per_hour = 0.5f);
  AffectionLevel ComputeLevel() const;
  static const char* LevelName(AffectionLevel level);
};

// ============================================================================
// NPC 人格定义
// ============================================================================

struct NpcPersonality {
  std::string name;
  std::string display_name;
  std::string background_story;      // 背景故事（注入 system prompt）
  std::string tone;                  // 说话语气描述
  std::vector<std::string> traits;   // 性格特征

  // 情感倾向
  float baseline_joy        = 0.5f;
  float baseline_curiosity  = 0.6f;
  float baseline_coldness   = 0.3f;
  float baseline_jealousy   = 0.1f;
  float baseline_sadness    = 0.1f;

  // 预设人格
  static NpcPersonality FriendlyCompanion();
  static NpcPersonality WiseMentor();
  static NpcPersonality MysteriousStranger();
  static NpcPersonality CheerfulAssistant();
  static NpcPersonality StoicGuardian();
  static NpcPersonality Custom(const std::string& name, const std::string& story,
                                const std::string& tone,
                                const std::vector<std::string>& traits);
};

// ============================================================================
// NPC 记忆条目
// ============================================================================

struct NpcMemory {
  std::string id;
  std::string content;               // 记忆内容
  std::string context;               // 发生场景
  double timestamp;                  // epoch seconds
  float importance;                  // 重要性 [0,1]
  int recall_count;                  // 回忆次数

  bool IsRecent(double current_time, double max_age_seconds = 3600.0) const;
};

// ============================================================================
// 对话消息
// ============================================================================

struct NpcDialogueMessage {
  std::string role;      // "system" | "user" | "assistant"
  std::string content;
  double timestamp;

  // LLM 输出元数据（仅 assistant 消息）
  std::string suggested_expression;  // 建议表情名称
  std::string emotion_label;         // 情感标签
  float emotion_intensity = 0.5f;    // 情感强度
};

// ============================================================================
// NPC 对话上下文
// ============================================================================

struct NpcDialogueContext {
  std::string npc_id;
  NpcPersonality personality;
  AffectionState affection;
  FiveDimensionalEmotion emotion;
  std::vector<NpcMemory> recent_memories;  // 最近记忆（最多 10 条）
  std::vector<NpcDialogueMessage> history; // 对话历史（最多 20 轮）
  std::string current_location;            // 当前场景/位置
  std::string current_time_of_day;         // 游戏内时间
  double last_activity_time = 0.0;

  /// 构建完整 system prompt（注入人格、好感度、情感、记忆）
  std::string BuildSystemPrompt() const;
  /// 构建聊天消息列表（用于 LlamaEngine::Chat）
  std::vector<struct LlamaEngine::ChatMessage> ToChatMessages() const;
};

// ============================================================================
// NPC 响应（LLM 输出解析结果）
// ============================================================================

struct NpcResponse {
  std::string text;                  // 纯文本回复
  std::string expression;            // 推荐表情（匹配 ExpressionLibrary）
  std::string gesture;               // 推荐手势
  std::string emotion_label;         // 情感标签
  float emotion_intensity = 0.5f;    // 情感强度

  // 元数据
  uint32_t tokens_generated  = 0;
  double ttft_ms             = 0.0;  // Time To First Token
  double total_duration_ms   = 0.0;
  double tokens_per_second   = 0.0;

  /// 从 LLM 原始输出解析结构化响应
  static NpcResponse Parse(const std::string& raw_text, double ttft_ms,
                            double duration_ms, uint32_t token_count);
};

// ============================================================================
// NPC 对话管线
// ============================================================================

using NpcStreamCallback = std::function<void(const std::string& token_text,
                                               uint32_t token_id,
                                               int is_final)>;

class NpcDialoguePipeline {
 public:
  NpcDialoguePipeline();
  ~NpcDialoguePipeline();

  /// 设置底层推理引擎（由 solra_inference_init 注入）
  void SetEngine(class LlamaEngine* engine) { engine_ = engine; }

  /// 创建/获取 NPC 上下文
  NpcDialogueContext* CreateNpc(const std::string& npc_id,
                                 const NpcPersonality& personality);
  NpcDialogueContext* GetNpc(const std::string& npc_id);
  void RemoveNpc(const std::string& npc_id);
  std::vector<std::string> ListNpcs() const;

  /// 发送用户消息，同步返回 NPC 回复
  NpcResponse SendMessage(const std::string& npc_id,
                           const std::string& user_message);

  /// 发送用户消息，流式返回 NPC 回复
  void SendMessageStream(const std::string& npc_id,
                          const std::string& user_message,
                          NpcStreamCallback on_token);

  /// 取消当前流式对话
  void CancelStream();

  /// 更新 NPC 情感（外部事件驱动）
  void UpdateEmotion(const std::string& npc_id,
                      const std::string& event_type, float intensity);

  /// 时间推进（情感衰减 + 好感度衰减）
  void Tick(double delta_seconds);

  /// 获取对话历史
  const std::vector<NpcDialogueMessage>& GetHistory(
      const std::string& npc_id) const;

  /// 清空对话历史
  void ClearHistory(const std::string& npc_id);

  /// 全局单例
  static NpcDialoguePipeline& Instance();

 private:
  LlamaEngine* engine_ = nullptr;
  std::map<std::string, NpcDialogueContext> npcs_;
  mutable std::mutex mutex_;
  std::atomic<int> stream_cancelled_{0};

  NpcResponse DoInference(NpcDialogueContext& ctx,
                           const std::string& user_message,
                           NpcStreamCallback on_token = {});

  void RecordInteraction(NpcDialogueContext& ctx,
                          const std::string& user_msg,
                          const NpcResponse& response);
};

} // namespace solra::core::inference
