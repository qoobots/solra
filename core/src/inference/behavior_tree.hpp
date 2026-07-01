#pragma once
/// @file behavior_tree.hpp
/// @brief 轻量级行为树引擎 —— NPC 自主行为驱动
/// @ingroup core/inference
/// @priority P2 (原型阶段)

#include <cstdint>
#include <functional>
#include <memory>
#include <string>
#include <vector>
#include <chrono>
#include <random>

namespace solra::core::inference {

// ============================================================================
// 节点状态
// ============================================================================

enum class BTStatus : uint8_t {
  kSuccess = 0,
  kFailure = 1,
  kRunning = 2,
};

inline const char* BTStatusName(BTStatus s) {
  switch (s) {
    case BTStatus::kSuccess: return "SUCCESS";
    case BTStatus::kFailure: return "FAILURE";
    case BTStatus::kRunning: return "RUNNING";
  }
  return "UNKNOWN";
}

// ============================================================================
// 黑板 (Blackboard) —— 行为树上下文数据共享
// ============================================================================

class Blackboard {
 public:
  template<typename T>
  void Set(const std::string& key, const T& value);

  template<typename T>
  T Get(const std::string& key, const T& default_val = T{}) const;

  bool Has(const std::string& key) const;
  void Remove(const std::string& key);
  void Clear();

 private:
  struct Entry {
    enum Type { kInt, kFloat, kDouble, kBool, kString } type;
    union {
      int int_val;
      float float_val;
      double double_val;
      bool bool_val;
    };
    std::string str_val;
  };
  std::map<std::string, Entry> data_;
};

// ============================================================================
// BT 节点基类
// ============================================================================

class BTNode {
 public:
  BTNode(const std::string& name) : name_(name) {}
  virtual ~BTNode() = default;

  /// 执行节点，返回状态
  virtual BTStatus Tick(Blackboard& bb) = 0;

  /// 重置节点内部状态
  virtual void Reset() {}

  const std::string& Name() const { return name_; }

  /// 添加子节点
  void AddChild(std::unique_ptr<BTNode> child) {
    children_.push_back(std::move(child));
  }

 protected:
  std::string name_;
  std::vector<std::unique_ptr<BTNode>> children_;
};

// ============================================================================
// 组合节点
// ============================================================================

/// 顺序节点：依次执行子节点，任一失败则失败，全部成功则成功
class SequenceNode : public BTNode {
 public:
  SequenceNode(const std::string& name) : BTNode(name) {}

  BTStatus Tick(Blackboard& bb) override;
  void Reset() override;

 private:
  size_t current_index_ = 0;
};

/// 选择节点：依次执行子节点，任一成功则成功，全部失败则失败
class SelectorNode : public BTNode {
 public:
  SelectorNode(const std::string& name) : BTNode(name) {}

  BTStatus Tick(Blackboard& bb) override;
  void Reset() override;

 private:
  size_t current_index_ = 0;
};

/// 并行节点：同时执行所有子节点（顺序遍历，任一失败则失败）
class ParallelNode : public BTNode {
 public:
  ParallelNode(const std::string& name, int required_success = -1)
      : BTNode(name), required_success_(required_success) {}

  BTStatus Tick(Blackboard& bb) override;

 private:
  int required_success_; // -1 = 全部成功
};

// ============================================================================
// 装饰节点
// ============================================================================

/// 条件装饰器：条件为真时才执行子节点
class ConditionNode : public BTNode {
 public:
  using ConditionFn = std::function<bool(Blackboard&)>;

  ConditionNode(const std::string& name, ConditionFn condition)
      : BTNode(name), condition_(std::move(condition)) {}

  BTStatus Tick(Blackboard& bb) override;

 private:
  ConditionFn condition_;
};

/// 反相装饰器：反转子节点结果
class InverterNode : public BTNode {
 public:
  InverterNode(const std::string& name) : BTNode(name) {}

  BTStatus Tick(Blackboard& bb) override;
};

/// 重复装饰器：重复执行子节点 N 次或直到失败
class RepeatNode : public BTNode {
 public:
  RepeatNode(const std::string& name, int count)
      : BTNode(name), max_count_(count) {}

  BTStatus Tick(Blackboard& bb) override;
  void Reset() override;

 private:
  int max_count_;
  int current_count_ = 0;
};

/// 冷却装饰器：子节点执行后进入冷却期
class CooldownNode : public BTNode {
 public:
  CooldownNode(const std::string& name, double cooldown_seconds)
      : BTNode(name), cooldown_seconds_(cooldown_seconds) {}

  BTStatus Tick(Blackboard& bb) override;

 private:
  double cooldown_seconds_;
  std::chrono::steady_clock::time_point last_execution_;
};

/// 随机选择装饰器：以概率 p 执行子节点
class RandomChanceNode : public BTNode {
 public:
  RandomChanceNode(const std::string& name, float probability)
      : BTNode(name), probability_(probability) {}

  BTStatus Tick(Blackboard& bb) override;

 private:
  float probability_;
  std::mt19937 rng_{std::random_device{}()};
};

// ============================================================================
// 动作节点
// ============================================================================

/// 通用动作节点
class ActionNode : public BTNode {
 public:
  using ActionFn = std::function<BTStatus(Blackboard&)>;

  ActionNode(const std::string& name, ActionFn action)
      : BTNode(name), action_(std::move(action)) {}

  BTStatus Tick(Blackboard& bb) override { return action_(bb); }

 private:
  ActionFn action_;
};

/// 等待节点
class WaitNode : public BTNode {
 public:
  WaitNode(const std::string& name, double seconds)
      : BTNode(name), wait_seconds_(seconds) {}

  BTStatus Tick(Blackboard& bb) override;
  void Reset() override;

 private:
  double wait_seconds_;
  std::chrono::steady_clock::time_point start_time_;
  bool started_ = false;
};

// ============================================================================
// 行为树
// ============================================================================

class BehaviorTree {
 public:
  BehaviorTree(const std::string& name) : name_(name) {}

  /// 设置根节点
  void SetRoot(std::unique_ptr<BTNode> root) { root_ = std::move(root); }

  /// 执行一次 tick
  BTStatus Tick(Blackboard& bb);

  /// 重置整棵树
  void Reset();

  const std::string& Name() const { return name_; }

  /// 获取黑板
  Blackboard& GetBlackboard() { return blackboard_; }
  const Blackboard& GetBlackboard() const { return blackboard_; }

 private:
  std::string name_;
  std::unique_ptr<BTNode> root_;
  Blackboard blackboard_;
};

// ============================================================================
// 预置 NPC 行为树
// ============================================================================

/// 构建标准 NPC 空闲行为树
/// 逻辑：有玩家靠近? → 打招呼 → 对话/巡逻/待机
std::unique_ptr<BehaviorTree> CreateNpcIdleBehavior(
    const std::string& npc_id,
    std::function<bool()> player_nearby,
    std::function<void()> on_greet,
    std::function<void()> on_idle_anim);

/// 构建标准 NPC 对话行为树
std::unique_ptr<BehaviorTree> CreateNpcDialogueBehavior(
    const std::string& npc_id,
    std::function<bool()> has_pending_message,
    std::function<void()> on_start_talking,
    std::function<void()> on_stop_talking);

/// 构建标准 NPC 巡逻行为树
std::unique_ptr<BehaviorTree> CreateNpcPatrolBehavior(
    const std::string& npc_id,
    std::function<bool()> reached_waypoint,
    std::function<void()> on_next_waypoint,
    std::function<void()> on_idle_at_waypoint);

} // namespace solra::core::inference
