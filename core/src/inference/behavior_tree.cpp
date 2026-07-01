#include "behavior_tree.hpp"

#include <algorithm>
#include <spdlog/spdlog.h>

namespace solra::core::inference {

// ============================================================================
// Blackboard 模板实现
// ============================================================================

template<typename T>
void Blackboard::Set(const std::string& key, const T& value) {
  Entry e;
  if constexpr (std::is_same_v<T, int>) {
    e.type = Entry::kInt;
    e.int_val = value;
  } else if constexpr (std::is_same_v<T, float>) {
    e.type = Entry::kFloat;
    e.float_val = value;
  } else if constexpr (std::is_same_v<T, double>) {
    e.type = Entry::kDouble;
    e.double_val = value;
  } else if constexpr (std::is_same_v<T, bool>) {
    e.type = Entry::kBool;
    e.bool_val = value;
  } else if constexpr (std::is_same_v<T, std::string>) {
    e.type = Entry::kString;
    e.str_val = value;
  } else if constexpr (std::is_same_v<T, const char*>) {
    e.type = Entry::kString;
    e.str_val = value;
  }
  data_[key] = e;
}

template<typename T>
T Blackboard::Get(const std::string& key, const T& default_val) const {
  auto it = data_.find(key);
  if (it == data_.end()) return default_val;

  const Entry& e = it->second;
  if constexpr (std::is_same_v<T, int>) {
    if (e.type == Entry::kInt) return e.int_val;
  } else if constexpr (std::is_same_v<T, float>) {
    if (e.type == Entry::kFloat) return e.float_val;
    if (e.type == Entry::kInt) return static_cast<float>(e.int_val);
  } else if constexpr (std::is_same_v<T, double>) {
    if (e.type == Entry::kDouble) return e.double_val;
    if (e.type == Entry::kFloat) return static_cast<double>(e.float_val);
    if (e.type == Entry::kInt) return static_cast<double>(e.int_val);
  } else if constexpr (std::is_same_v<T, bool>) {
    if (e.type == Entry::kBool) return e.bool_val;
  } else if constexpr (std::is_same_v<T, std::string>) {
    if (e.type == Entry::kString) return e.str_val;
  }
  return default_val;
}

// 显式模板实例化
template void Blackboard::Set<int>(const std::string&, const int&);
template void Blackboard::Set<float>(const std::string&, const float&);
template void Blackboard::Set<double>(const std::string&, const double&);
template void Blackboard::Set<bool>(const std::string&, const bool&);
template void Blackboard::Set<std::string>(const std::string&, const std::string&);

template int Blackboard::Get<int>(const std::string&, const int&) const;
template float Blackboard::Get<float>(const std::string&, const float&) const;
template double Blackboard::Get<double>(const std::string&, const double&) const;
template bool Blackboard::Get<bool>(const std::string&, const bool&) const;
template std::string Blackboard::Get<std::string>(const std::string&, const std::string&) const;

bool Blackboard::Has(const std::string& key) const {
  return data_.find(key) != data_.end();
}

void Blackboard::Remove(const std::string& key) {
  data_.erase(key);
}

void Blackboard::Clear() {
  data_.clear();
}

// ============================================================================
// SequenceNode
// ============================================================================

BTStatus SequenceNode::Tick(Blackboard& bb) {
  for (size_t i = current_index_; i < children_.size(); ++i) {
    BTStatus status = children_[i]->Tick(bb);
    if (status == BTStatus::kFailure) {
      current_index_ = 0;
      return BTStatus::kFailure;
    }
    if (status == BTStatus::kRunning) {
      current_index_ = i;
      return BTStatus::kRunning;
    }
  }
  current_index_ = 0;
  return BTStatus::kSuccess;
}

void SequenceNode::Reset() {
  current_index_ = 0;
  for (auto& child : children_) child->Reset();
}

// ============================================================================
// SelectorNode
// ============================================================================

BTStatus SelectorNode::Tick(Blackboard& bb) {
  for (size_t i = current_index_; i < children_.size(); ++i) {
    BTStatus status = children_[i]->Tick(bb);
    if (status == BTStatus::kSuccess) {
      current_index_ = 0;
      return BTStatus::kSuccess;
    }
    if (status == BTStatus::kRunning) {
      current_index_ = i;
      return BTStatus::kRunning;
    }
  }
  current_index_ = 0;
  return BTStatus::kFailure;
}

void SelectorNode::Reset() {
  current_index_ = 0;
  for (auto& child : children_) child->Reset();
}

// ============================================================================
// ParallelNode
// ============================================================================

BTStatus ParallelNode::Tick(Blackboard& bb) {
  int success_count = 0;
  int failure_count = 0;

  for (auto& child : children_) {
    BTStatus status = child->Tick(bb);
    if (status == BTStatus::kSuccess) success_count++;
    else if (status == BTStatus::kFailure) failure_count++;
  }

  int required = required_success_ < 0
      ? static_cast<int>(children_.size()) : required_success_;

  if (success_count >= required) return BTStatus::kSuccess;
  if (failure_count > 0) return BTStatus::kFailure;
  return BTStatus::kRunning;
}

// ============================================================================
// ConditionNode
// ============================================================================

BTStatus ConditionNode::Tick(Blackboard& bb) {
  if (!condition_(bb)) return BTStatus::kFailure;
  if (children_.empty()) return BTStatus::kSuccess;
  return children_[0]->Tick(bb);
}

// ============================================================================
// InverterNode
// ============================================================================

BTStatus InverterNode::Tick(Blackboard& bb) {
  if (children_.empty()) return BTStatus::kFailure;
  BTStatus status = children_[0]->Tick(bb);
  switch (status) {
    case BTStatus::kSuccess: return BTStatus::kFailure;
    case BTStatus::kFailure: return BTStatus::kSuccess;
    default: return BTStatus::kRunning;
  }
}

// ============================================================================
// RepeatNode
// ============================================================================

BTStatus RepeatNode::Tick(Blackboard& bb) {
  if (children_.empty()) return BTStatus::kFailure;

  while (current_count_ < max_count_ || max_count_ < 0) {
    BTStatus status = children_[0]->Tick(bb);
    if (status == BTStatus::kFailure) {
      current_count_ = 0;
      return BTStatus::kFailure;
    }
    if (status == BTStatus::kRunning) return BTStatus::kRunning;
    current_count_++;
    children_[0]->Reset();
  }

  current_count_ = 0;
  return BTStatus::kSuccess;
}

void RepeatNode::Reset() {
  current_count_ = 0;
  if (!children_.empty()) children_[0]->Reset();
}

// ============================================================================
// CooldownNode
// ============================================================================

BTStatus CooldownNode::Tick(Blackboard& bb) {
  auto now = std::chrono::steady_clock::now();
  double elapsed = std::chrono::duration<double>(
      now - last_execution_).count();

  if (elapsed < cooldown_seconds_) return BTStatus::kFailure;

  if (children_.empty()) {
    last_execution_ = now;
    return BTStatus::kSuccess;
  }

  BTStatus status = children_[0]->Tick(bb);
  if (status != BTStatus::kRunning) {
    last_execution_ = now;
  }
  return status;
}

// ============================================================================
// RandomChanceNode
// ============================================================================

BTStatus RandomChanceNode::Tick(Blackboard& bb) {
  std::uniform_real_distribution<float> dist(0.0f, 1.0f);
  if (dist(rng_) > probability_) return BTStatus::kFailure;
  if (children_.empty()) return BTStatus::kSuccess;
  return children_[0]->Tick(bb);
}

// ============================================================================
// WaitNode
// ============================================================================

BTStatus WaitNode::Tick(Blackboard& bb) {
  (void)bb;
  auto now = std::chrono::steady_clock::now();

  if (!started_) {
    start_time_ = now;
    started_ = true;
    return BTStatus::kRunning;
  }

  double elapsed = std::chrono::duration<double>(now - start_time_).count();
  if (elapsed >= wait_seconds_) {
    started_ = false;
    return BTStatus::kSuccess;
  }
  return BTStatus::kRunning;
}

void WaitNode::Reset() {
  started_ = false;
}

// ============================================================================
// BehaviorTree
// ============================================================================

BTStatus BehaviorTree::Tick(Blackboard& bb) {
  if (!root_) return BTStatus::kFailure;
  return root_->Tick(bb);
}

void BehaviorTree::Reset() {
  if (root_) root_->Reset();
}

// ============================================================================
// 预置行为树工厂
// ============================================================================

std::unique_ptr<BehaviorTree> CreateNpcIdleBehavior(
    const std::string& npc_id,
    std::function<bool()> player_nearby,
    std::function<void()> on_greet,
    std::function<void()> on_idle_anim) {

  auto tree = std::make_unique<BehaviorTree>(npc_id + "_idle");

  // Root: Selector (优先级选择)
  auto root = std::make_unique<SelectorNode>("root");

  // 分支1：玩家靠近 → 打招呼（带冷却 10s）
  auto greet_seq = std::make_unique<SequenceNode>("greet_sequence");
  greet_seq->AddChild(std::make_unique<ConditionNode>("player_nearby",
      [player_nearby](Blackboard&) { return player_nearby(); }));
  auto greet_cooldown = std::make_unique<CooldownNode>("greet_cooldown", 10.0);
  greet_cooldown->AddChild(std::make_unique<ActionNode>("do_greet",
      [on_greet](Blackboard&) { on_greet(); return BTStatus::kSuccess; }));
  greet_seq->AddChild(std::move(greet_cooldown));

  // 分支2：随机播放待机动画（30% 概率，冷却 5s）
  auto idle_seq = std::make_unique<SequenceNode>("idle_sequence");
  auto idle_cooldown = std::make_unique<CooldownNode>("idle_cooldown", 5.0);
  auto idle_chance = std::make_unique<RandomChanceNode>("idle_chance", 0.3f);
  idle_chance->AddChild(std::make_unique<ActionNode>("do_idle",
      [on_idle_anim](Blackboard&) { on_idle_anim(); return BTStatus::kSuccess; }));
  idle_cooldown->AddChild(std::move(idle_chance));
  idle_seq->AddChild(std::move(idle_cooldown));

  root->AddChild(std::move(greet_seq));
  root->AddChild(std::move(idle_seq));

  tree->SetRoot(std::move(root));
  return tree;
}

std::unique_ptr<BehaviorTree> CreateNpcDialogueBehavior(
    const std::string& npc_id,
    std::function<bool()> has_pending_message,
    std::function<void()> on_start_talking,
    std::function<void()> on_stop_talking) {

  auto tree = std::make_unique<BehaviorTree>(npc_id + "_dialogue");

  auto root = std::make_unique<SequenceNode>("dialogue_sequence");

  root->AddChild(std::make_unique<ConditionNode>("has_message",
      [has_pending_message](Blackboard&) { return has_pending_message(); }));

  root->AddChild(std::make_unique<ActionNode>("start_talking",
      [on_start_talking](Blackboard&) { on_start_talking(); return BTStatus::kSuccess; }));

  // 等待消息处理完成
  root->AddChild(std::make_unique<ConditionNode>("message_processed",
      [has_pending_message](Blackboard&) { return !has_pending_message(); }));

  root->AddChild(std::make_unique<ActionNode>("stop_talking",
      [on_stop_talking](Blackboard&) { on_stop_talking(); return BTStatus::kSuccess; }));

  tree->SetRoot(std::move(root));
  return tree;
}

std::unique_ptr<BehaviorTree> CreateNpcPatrolBehavior(
    const std::string& npc_id,
    std::function<bool()> reached_waypoint,
    std::function<void()> on_next_waypoint,
    std::function<void()> on_idle_at_waypoint) {

  auto tree = std::make_unique<BehaviorTree>(npc_id + "_patrol");

  auto root = std::make_unique<SequenceNode>("patrol_sequence");

  // 到达路点 → 待机 3 秒 → 前往下一个路点
  root->AddChild(std::make_unique<ConditionNode>("reached_wp",
      [reached_waypoint](Blackboard&) { return reached_waypoint(); }));

  auto idle_at_wp = std::make_unique<ActionNode>("idle_at_wp",
      [on_idle_at_waypoint](Blackboard&) { on_idle_at_waypoint(); return BTStatus::kSuccess; });
  root->AddChild(std::move(idle_at_wp));

  root->AddChild(std::make_unique<WaitNode>("wait_at_wp", 3.0));

  root->AddChild(std::make_unique<ActionNode>("next_waypoint",
      [on_next_waypoint](Blackboard&) { on_next_waypoint(); return BTStatus::kSuccess; }));

  tree->SetRoot(std::move(root));
  return tree;
}

} // namespace solra::core::inference
