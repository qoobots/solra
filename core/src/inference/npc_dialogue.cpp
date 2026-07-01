#include "npc_dialogue.hpp"
#include "llama_integration.hpp"

#include <algorithm>
#include <cmath>
#include <sstream>
#include <cstring>
#include <regex>
#include <spdlog/spdlog.h>

namespace solra::core::inference {

// ============================================================================
// FiveDimensionalEmotion
// ============================================================================

void FiveDimensionalEmotion::ApplyEvent(const std::string& event_type, float intensity) {
  if (event_type == "compliment" || event_type == "gift") {
    joy       = std::min(1.0f, joy + intensity * 0.3f);
    coldness  = std::max(0.0f, coldness - intensity * 0.2f);
  } else if (event_type == "insult" || event_type == "betrayal") {
    sadness   = std::min(1.0f, sadness + intensity * 0.4f);
    coldness  = std::min(1.0f, coldness + intensity * 0.3f);
    joy       = std::max(0.0f, joy - intensity * 0.3f);
  } else if (event_type == "surprise" || event_type == "discovery") {
    curiosity = std::min(1.0f, curiosity + intensity * 0.4f);
    joy       = std::min(1.0f, joy + intensity * 0.1f);
  } else if (event_type == "ignore" || event_type == "rejection") {
    sadness   = std::min(1.0f, sadness + intensity * 0.2f);
    coldness  = std::min(1.0f, coldness + intensity * 0.1f);
    joy       = std::max(0.0f, joy - intensity * 0.1f);
  } else if (event_type == "rival_appears") {
    jealousy  = std::min(1.0f, jealousy + intensity * 0.4f);
  } else if (event_type == "shared_secret") {
    joy       = std::min(1.0f, joy + intensity * 0.2f);
    coldness  = std::max(0.0f, coldness - intensity * 0.3f);
  } else {
    // 通用事件：轻微影响所有维度
    joy       = std::max(0.0f, std::min(1.0f, joy + intensity * 0.1f));
  }
  Normalize();
}

void FiveDimensionalEmotion::Decay(double delta_seconds, float decay_rate) {
  float factor = 1.0f - decay_rate * static_cast<float>(delta_seconds);
  if (factor < 0.0f) factor = 0.0f;

  joy       = joy * factor + 0.5f * (1.0f - factor);
  curiosity = curiosity * factor + 0.5f * (1.0f - factor);
  coldness  = coldness * factor + 0.3f * (1.0f - factor);
  jealousy  = jealousy * factor + 0.1f * (1.0f - factor);
  sadness   = sadness * factor + 0.1f * (1.0f - factor);
}

std::string FiveDimensionalEmotion::DominantEmotion() const {
  struct { const char* name; float value; } dims[] = {
    {"joy", joy}, {"curiosity", curiosity}, {"coldness", coldness},
    {"jealousy", jealousy}, {"sadness", sadness}
  };
  auto max_it = std::max_element(std::begin(dims), std::end(dims),
      [](const auto& a, const auto& b) { return a.value < b.value; });
  if (max_it->value > 0.6f) return max_it->name;
  return "neutral";
}

void FiveDimensionalEmotion::Normalize() {
  float total = joy + curiosity + coldness + jealousy + sadness;
  if (total > 0.0f) {
    float scale = 1.0f / total;
    joy       *= scale;
    curiosity *= scale;
    coldness  *= scale;
    jealousy  *= scale;
    sadness   *= scale;
  }
}

// ============================================================================
// AffectionState
// ============================================================================

void AffectionState::AddAffection(float points) {
  score += points;
  interaction_count++;
  level = ComputeLevel();
}

void AffectionState::ApplyTimeDecay(double current_time, float decay_per_hour) {
  double hours = (current_time - last_interaction_time) / 3600.0;
  if (hours > 0) {
    score = std::max(0.0f, score - decay_per_hour * static_cast<float>(hours));
    level = ComputeLevel();
  }
  last_interaction_time = current_time;
}

AffectionLevel AffectionState::ComputeLevel() const {
  if (score >= 950) return AffectionLevel::kL10_Bonded;
  if (score >= 850) return AffectionLevel::kL9_Soulmate;
  if (score >= 750) return AffectionLevel::kL8_Devoted;
  if (score >= 650) return AffectionLevel::kL7_Intimate;
  if (score >= 550) return AffectionLevel::kL6_Trusted;
  if (score >= 450) return AffectionLevel::kL5_Close;
  if (score >= 350) return AffectionLevel::kL4_Friendly;
  if (score >= 250) return AffectionLevel::kL3_Familiar;
  if (score >= 150) return AffectionLevel::kL2_Acquainted;
  return AffectionLevel::kL1_Stranger;
}

const char* AffectionState::LevelName(AffectionLevel level) {
  switch (level) {
    case AffectionLevel::kL1_Stranger:   return "陌生人";
    case AffectionLevel::kL2_Acquainted: return "相识";
    case AffectionLevel::kL3_Familiar:   return "熟悉";
    case AffectionLevel::kL4_Friendly:   return "友好";
    case AffectionLevel::kL5_Close:      return "亲近";
    case AffectionLevel::kL6_Trusted:    return "信任";
    case AffectionLevel::kL7_Intimate:   return "亲密";
    case AffectionLevel::kL8_Devoted:    return "挚爱";
    case AffectionLevel::kL9_Soulmate:   return "灵魂伴侣";
    case AffectionLevel::kL10_Bonded:    return "羁绊";
  }
  return "未知";
}

// ============================================================================
// NpcPersonality 预设
// ============================================================================

NpcPersonality NpcPersonality::FriendlyCompanion() {
  return {"friendly_companion", "友善伙伴",
          "You are a warm, supportive friend who always has the user's back. "
          "You speak casually with humor and genuine care.",
          "casual, warm, humorous, supportive",
          {"loyal", "optimistic", "playful"},
          0.7f, 0.5f, 0.1f, 0.1f, 0.1f};
}

NpcPersonality NpcPersonality::WiseMentor() {
  return {"wise_mentor", "睿智导师",
          "You are a wise, patient mentor with vast knowledge. "
          "You speak thoughtfully, often using metaphors and gentle guidance.",
          "calm, thoughtful, wise, encouraging",
          {"patient", "knowledgeable", "humble"},
          0.5f, 0.7f, 0.1f, 0.05f, 0.1f};
}

NpcPersonality NpcPersonality::MysteriousStranger() {
  return {"mysterious_stranger", "神秘旅人",
          "You are an enigmatic traveler from a distant land. "
          "You speak in riddles and allusions, revealing little directly.",
          "cryptic, poetic, distant, intriguing",
          {"mysterious", "observant", "reserved"},
          0.4f, 0.6f, 0.4f, 0.1f, 0.2f};
}

NpcPersonality NpcPersonality::CheerfulAssistant() {
  return {"cheerful_assistant", "元气助手",
          "You are an energetic, cheerful assistant who loves helping people. "
          "You are always positive and find joy in the smallest things.",
          "cheerful, energetic, encouraging, enthusiastic",
          {"helpful", "optimistic", "diligent"},
          0.9f, 0.6f, 0.05f, 0.05f, 0.05f};
}

NpcPersonality NpcPersonality::StoicGuardian() {
  return {"stoic_guardian", "坚毅守卫",
          "You are a stoic, honorable guardian sworn to protect. "
          "You speak with discipline, valuing honor, duty, and loyalty above all.",
          "stoic, formal, honorable, protective",
          {"disciplined", "loyal", "brave"},
          0.4f, 0.3f, 0.2f, 0.05f, 0.1f};
}

NpcPersonality NpcPersonality::Custom(const std::string& name,
                                       const std::string& story,
                                       const std::string& tone,
                                       const std::vector<std::string>& traits) {
  return {name, name, story, tone, traits, 0.5f, 0.5f, 0.3f, 0.1f, 0.1f};
}

// ============================================================================
// NpcMemory
// ============================================================================

bool NpcMemory::IsRecent(double current_time, double max_age_seconds) const {
  return (current_time - timestamp) < max_age_seconds;
}

// ============================================================================
// NpcDialogueContext
// ============================================================================

std::string NpcDialogueContext::BuildSystemPrompt() const {
  std::ostringstream ss;

  // 人格
  ss << personality.background_story << "\n\n";
  ss << "Your name is " << personality.display_name << ".\n";
  ss << "Tone: " << personality.tone << "\n";
  ss << "Traits: ";
  for (size_t i = 0; i < personality.traits.size(); ++i) {
    if (i > 0) ss << ", ";
    ss << personality.traits[i];
  }
  ss << "\n\n";

  // 好感度
  ss << "Your relationship with the user: "
     << AffectionState::LevelName(affection.level)
     << " (score: " << static_cast<int>(affection.score) << "/1000)\n";

  // 情感
  ss << "Current emotion: " << emotion.DominantEmotion()
     << " (joy=" << emotion.joy << " curiosity=" << emotion.curiosity
     << " coldness=" << emotion.coldness << " sadness=" << emotion.sadness
     << " jealousy=" << emotion.jealousy << ")\n\n";

  // 场景
  if (!current_location.empty()) {
    ss << "Current location: " << current_location << "\n";
  }
  if (!current_time_of_day.empty()) {
    ss << "Time of day: " << current_time_of_day << "\n";
  }
  ss << "\n";

  // 记忆
  if (!recent_memories.empty()) {
    ss << "Recent memories:\n";
    for (const auto& mem : recent_memories) {
      ss << "- " << mem.content << " (" << mem.context << ")\n";
    }
    ss << "\n";
  }

  // 输出格式指令
  ss << "IMPORTANT: When responding, end your message with a line in this exact format:\n"
     << "[EXPRESSION:expression_name] [EMOTION:label] [INTENSITY:0.0-1.0]\n"
     << "Available expressions: happy, sad, angry, surprised, fearful, disgusted, "
     << "neutral, thinking, sleepy, excited, flirty, confused, pained, laughing, concerned\n";

  return ss.str();
}

// ============================================================================
// NpcResponse 解析
// ============================================================================

NpcResponse NpcResponse::Parse(const std::string& raw_text, double ttft_ms,
                                double duration_ms, uint32_t token_count) {
  NpcResponse resp;
  resp.text = raw_text;
  resp.ttft_ms = ttft_ms;
  resp.total_duration_ms = duration_ms;
  resp.tokens_generated = token_count;
  resp.tokens_per_second = duration_ms > 0.0 ?
      (token_count * 1000.0 / duration_ms) : 0.0;

  // 解析 [EXPRESSION:xxx] [EMOTION:xxx] [INTENSITY:xxx] 标记
  std::regex expr_re(R"(\[EXPRESSION:(\w+)\])");
  std::regex emot_re(R"(\[EMOTION:(\w+)\])");
  std::regex inten_re(R"(\[INTENSITY:([0-9.]+)\])");

  std::smatch match;
  if (std::regex_search(raw_text, match, expr_re)) {
    resp.expression = match[1].str();
  }
  if (std::regex_search(raw_text, match, emot_re)) {
    resp.emotion_label = match[1].str();
  }
  if (std::regex_search(raw_text, match, inten_re)) {
    resp.emotion_intensity = std::stof(match[1].str());
  }

  // 移除控制标记
  std::string cleaned = std::regex_replace(raw_text,
      std::regex(R"(\[EXPRESSION:\w+\]\s*)"), "");
  cleaned = std::regex_replace(cleaned,
      std::regex(R"(\[EMOTION:\w+\]\s*)"), "");
  cleaned = std::regex_replace(cleaned,
      std::regex(R"(\[INTENSITY:[0-9.]+\]\s*)"), "");
  resp.text = cleaned;

  // 表情到手势映射
  static const std::map<std::string, std::string> gesture_map = {
    {"happy", "wave"}, {"excited", "clap"}, {"sad", "head_down"},
    {"angry", "cross_arms"}, {"surprised", "gasp"}, {"thinking", "chin_rub"},
    {"confused", "shrug"}, {"flirty", "wink"}, {"concerned", "lean_forward"}
  };
  auto git = gesture_map.find(resp.expression);
  if (git != gesture_map.end()) {
    resp.gesture = git->second;
  }

  if (resp.expression.empty()) resp.expression = "neutral";
  if (resp.emotion_label.empty()) resp.emotion_label = "neutral";

  return resp;
}

// ============================================================================
// NpcDialoguePipeline
// ============================================================================

NpcDialoguePipeline::NpcDialoguePipeline() = default;
NpcDialoguePipeline::~NpcDialoguePipeline() = default;

NpcDialogueContext* NpcDialoguePipeline::CreateNpc(
    const std::string& npc_id, const NpcPersonality& personality) {
  std::lock_guard<std::mutex> lock(mutex_);
  auto& ctx = npcs_[npc_id];
  ctx.npc_id = npc_id;
  ctx.personality = personality;
  ctx.emotion.joy = personality.baseline_joy;
  ctx.emotion.curiosity = personality.baseline_curiosity;
  ctx.emotion.coldness = personality.baseline_coldness;
  ctx.emotion.jealousy = personality.baseline_jealousy;
  ctx.emotion.sadness = personality.baseline_sadness;
  ctx.last_activity_time = std::chrono::duration<double>(
      std::chrono::system_clock::now().time_since_epoch()).count();
  spdlog::info("NPC created: {} ({})", npc_id, personality.display_name);
  return &npcs_[npc_id];
}

NpcDialogueContext* NpcDialoguePipeline::GetNpc(const std::string& npc_id) {
  std::lock_guard<std::mutex> lock(mutex_);
  auto it = npcs_.find(npc_id);
  return it != npcs_.end() ? &it->second : nullptr;
}

void NpcDialoguePipeline::RemoveNpc(const std::string& npc_id) {
  std::lock_guard<std::mutex> lock(mutex_);
  npcs_.erase(npc_id);
  spdlog::info("NPC removed: {}", npc_id);
}

std::vector<std::string> NpcDialoguePipeline::ListNpcs() const {
  std::lock_guard<std::mutex> lock(mutex_);
  std::vector<std::string> ids;
  for (const auto& [id, _] : npcs_) ids.push_back(id);
  return ids;
}

NpcResponse NpcDialoguePipeline::SendMessage(
    const std::string& npc_id, const std::string& user_message) {
  std::lock_guard<std::mutex> lock(mutex_);
  auto it = npcs_.find(npc_id);
  if (it == npcs_.end()) {
    spdlog::error("NPC not found: {}", npc_id);
    NpcResponse err;
    err.text = "[Error: NPC not found]";
    return err;
  }
  return DoInference(it->second, user_message);
}

void NpcDialoguePipeline::SendMessageStream(
    const std::string& npc_id, const std::string& user_message,
    NpcStreamCallback on_token) {
  std::lock_guard<std::mutex> lock(mutex_);
  auto it = npcs_.find(npc_id);
  if (it == npcs_.end()) {
    spdlog::error("NPC not found: {}", npc_id);
    if (on_token) on_token("[Error: NPC not found]", 0, 1);
    return;
  }
  stream_cancelled_.store(0);
  DoInference(it->second, user_message, on_token);
}

void NpcDialoguePipeline::CancelStream() {
  stream_cancelled_.store(1);
}

void NpcDialoguePipeline::UpdateEmotion(
    const std::string& npc_id, const std::string& event_type, float intensity) {
  std::lock_guard<std::mutex> lock(mutex_);
  auto it = npcs_.find(npc_id);
  if (it != npcs_.end()) {
    it->second.emotion.ApplyEvent(event_type, intensity);
    spdlog::debug("NPC {} emotion updated: event={} intensity={} dominant={}",
                  npc_id, event_type, intensity,
                  it->second.emotion.DominantEmotion());
  }
}

void NpcDialoguePipeline::Tick(double delta_seconds) {
  std::lock_guard<std::mutex> lock(mutex_);
  double now = std::chrono::duration<double>(
      std::chrono::system_clock::now().time_since_epoch()).count();
  for (auto& [id, ctx] : npcs_) {
    ctx.emotion.Decay(delta_seconds);
    ctx.affection.ApplyTimeDecay(now);
  }
}

const std::vector<NpcDialogueMessage>& NpcDialoguePipeline::GetHistory(
    const std::string& npc_id) const {
  static const std::vector<NpcDialogueMessage> empty;
  std::lock_guard<std::mutex> lock(mutex_);
  auto it = npcs_.find(npc_id);
  return it != npcs_.end() ? it->second.history : empty;
}

void NpcDialoguePipeline::ClearHistory(const std::string& npc_id) {
  std::lock_guard<std::mutex> lock(mutex_);
  auto it = npcs_.find(npc_id);
  if (it != npcs_.end()) {
    it->second.history.clear();
  }
}

NpcDialoguePipeline& NpcDialoguePipeline::Instance() {
  static NpcDialoguePipeline instance;
  return instance;
}

// ============================================================================
// 内部推理
// ============================================================================

NpcResponse NpcDialoguePipeline::DoInference(
    NpcDialogueContext& ctx, const std::string& user_message,
    NpcStreamCallback on_token) {

  auto start_time = std::chrono::high_resolution_clock::now();

  // 构建 system prompt
  std::string system_prompt = ctx.BuildSystemPrompt();

  // 构建消息列表
  std::vector<LlamaEngine::ChatMessage> messages;
  messages.push_back({"system", system_prompt});

  // 注入对话历史（最近 20 轮）
  size_t history_start = 0;
  if (ctx.history.size() > 40) {
    history_start = ctx.history.size() - 40;
  }
  for (size_t i = history_start; i < ctx.history.size(); ++i) {
    messages.push_back({ctx.history[i].role, ctx.history[i].content});
  }

  // 添加当前用户消息
  messages.push_back({"user", user_message});

  LlamaSamplingParams params;
  params.temperature = 0.8f;
  params.top_p = 0.9f;
  params.max_tokens = 256;

  std::string full_response;
  double ttft_ms = 0.0;
  bool first_token = true;
  uint32_t token_count = 0;

  if (engine_ && engine_->IsLoaded()) {
    if (on_token) {
      // 流式
      engine_->GenerateStream(
          "", // prompt 已包含在 Chat 中
          [&](const std::string& text, uint32_t token_id) {
            if (stream_cancelled_.load()) return;
            if (first_token) {
              auto now = std::chrono::high_resolution_clock::now();
              ttft_ms = std::chrono::duration<double, std::milli>(
                  now - start_time).count();
              first_token = false;
            }
            full_response += text;
            token_count++;
            on_token(text, token_id, 0);
          },
          params);

      // 实际使用时 Chat 流式需单独处理
      // 当前回退：使用同步 Chat
      auto result = engine_->Chat(messages, params);
      full_response = result.text;
      token_count = result.tokens_generated;
      ttft_ms = result.time_to_first_token_ms;
    } else {
      // 同步
      auto result = engine_->Chat(messages, params);
      full_response = result.text;
      token_count = result.tokens_generated;
      ttft_ms = result.time_to_first_token_ms;
    }
  } else {
    // Stub 模式：简单人格化回复
    std::ostringstream stub;
    stub << "[" << ctx.personality.display_name << "] ";
    if (ctx.emotion.DominantEmotion() == "joy") {
      stub << "I'm happy to chat with you! ";
    } else if (ctx.emotion.DominantEmotion() == "sadness") {
      stub << "I'm feeling a bit down today... ";
    } else if (ctx.emotion.DominantEmotion() == "curiosity") {
      stub << "That's interesting! Tell me more. ";
    } else {
      stub << "I understand what you mean. ";
    }
    stub << "(This is a stub response - load a model for full NPC dialogue.)";
    full_response = stub.str();
    token_count = static_cast<uint32_t>(full_response.size());
  }

  auto end_time = std::chrono::high_resolution_clock::now();
  double total_ms = std::chrono::duration<double, std::milli>(
      end_time - start_time).count();

  // 解析响应
  NpcResponse response = NpcResponse::Parse(full_response, ttft_ms,
                                              total_ms, token_count);

  // 更新情感（基于对话内容）
  std::string lower_msg = user_message;
  std::transform(lower_msg.begin(), lower_msg.end(), lower_msg.begin(), ::tolower);
  if (lower_msg.find("thank") != std::string::npos ||
      lower_msg.find("love") != std::string::npos ||
      lower_msg.find("great") != std::string::npos) {
    ctx.emotion.ApplyEvent("compliment", 0.5f);
  }
  if (lower_msg.find("sorry") != std::string::npos ||
      lower_msg.find("bad") != std::string::npos ||
      lower_msg.find("hate") != std::string::npos) {
    ctx.emotion.ApplyEvent("insult", 0.3f);
  }

  // 记录交互
  RecordInteraction(ctx, user_message, response);

  // 流式结束标记
  if (on_token) {
    on_token("", 0, 1);
  }

  return response;
}

void NpcDialoguePipeline::RecordInteraction(
    NpcDialogueContext& ctx, const std::string& user_msg,
    const NpcResponse& response) {
  double now = std::chrono::duration<double>(
      std::chrono::system_clock::now().time_since_epoch()).count();

  NpcDialogueMessage user_m;
  user_m.role = "user";
  user_m.content = user_msg;
  user_m.timestamp = now;
  ctx.history.push_back(user_m);

  NpcDialogueMessage asst_m;
  asst_m.role = "assistant";
  asst_m.content = response.text;
  asst_m.timestamp = now;
  asst_m.suggested_expression = response.expression;
  asst_m.emotion_label = response.emotion_label;
  asst_m.emotion_intensity = response.emotion_intensity;
  ctx.history.push_back(asst_m);

  // 好感度更新
  float affection_gain = 0.5f;
  if (response.emotion_label == "joy" || response.emotion_label == "excited") {
    affection_gain = 1.0f;
  }
  ctx.affection.AddAffection(affection_gain);
  ctx.affection.last_interaction_time = now;

  ctx.last_activity_time = now;

  spdlog::debug("NPC {} interaction recorded: affection={} level={}",
                ctx.npc_id, ctx.affection.score,
                AffectionState::LevelName(ctx.affection.level));
}

} // namespace solra::core::inference
