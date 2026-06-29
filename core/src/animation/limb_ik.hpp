#pragma once
/**
 * @file limb_ik.hpp
 * @brief 肢体反向运动学 (Inverse Kinematics) — E-087
 *
 * 支持：
 * - 两关节 IK (手臂/腿)
 * - CCD (Cyclic Coordinate Descent) 求解器
 * - FABRIK 求解器
 * - 多末端约束 (双手、双脚)
 * - 关节旋转限制 (hinge/ball joint limits)
 * - 骨骼链缓存与增量更新
 */

#include <array>
#include <functional>
#include <memory>
#include <string>
#include <vector>

#include "solra_math.hpp" // Vec3, Quat, Mat4 (项目内数学库)

namespace solra::animation {

// ── 关节 ────────────────────────────────────────────
struct Joint {
    std::string name;            // 关节名称 (如 "LeftUpperArm")
    Vec3 position;               // 世界空间位置
    Quat rotation;               // 世界空间旋转

    // 旋转限制
    Vec3 min_euler;              // 最小欧拉角 (度)
    Vec3 max_euler;              // 最大欧拉角 (度)
    bool has_limit = false;

    float length = 0.f;          // 到下一关节的长度
    int parent_idx = -1;         // 父关节索引
};

// ── IK 链 ───────────────────────────────────────────
struct IKChain {
    std::string name;            // 链名称
    std::vector<int> joint_indices; // 关节索引 (从根到末端)

    int root;                    // 根关节
    int effector;                // 末端效应器

    Vec3 target;                 // 目标位置 (世界空间)
    Quat target_rotation;        // 目标旋转 (可选)

    bool position_ik = true;     // 启用位置 IK
    bool rotation_ik = false;    // 启用旋转 IK

    float tolerance = 0.001f;    // 收敛容差
    int max_iterations = 20;     // 最大迭代次数
};

// ── IK 求解策略 ─────────────────────────────────────
enum class IKSolverType {
    CCD,       // Cyclic Coordinate Descent — 快速，关节无限制
    FABRIK,    // Forward And Backward Reaching — 自然，保持长度
    Jacobian,  // Jacobian Inverse — 精确，更昂贵
};

// ── 求解配置 ────────────────────────────────────────
struct IKSolverConfig {
    IKSolverType solver = IKSolverType::FABRIK;
    float dampening = 0.1f;      // Jacobian 阻尼
    bool enforce_joint_limits = true;
    bool allow_stretching = false;
};

// ── 结果 ────────────────────────────────────────────
struct IKResult {
    bool converged = false;
    int iterations_used = 0;
    float final_error = 0.f;
};

// ── 肢体 IK 求解器 ──────────────────────────────────
class LimbIKSolver {
public:
    LimbIKSolver();
    explicit LimbIKSolver(const IKSolverConfig& config);
    ~LimbIKSolver();

    // 注册骨骼链
    int register_chain(const IKChain& chain);

    // 移除骨骼链
    bool remove_chain(const std::string& name);

    // 设置目标位置
    bool set_target(const std::string& chain_name, const Vec3& position);
    bool set_target_rotation(const std::string& chain_name, const Quat& rotation);

    // 更新关节位置 (需要在求解前调用)
    void update_joints(const std::vector<Joint>& joints);

    // 求解所有链
    std::vector<IKResult> solve();

    // 获取求解后的关节
    const std::vector<Joint>& get_joints() const;

    // 单链求解
    IKResult solve_chain(const std::string& chain_name);

    // 设置求解配置
    void set_config(const IKSolverConfig& config);

    // 重置求解器状态
    void reset();

private:
    // ── CCD 求解器 ──
    IKResult solve_ccd(IKChain& chain, std::vector<Joint>& joints);

    // ── FABRIK 求解器 ──
    IKResult solve_fabrik(IKChain& chain, std::vector<Joint>& joints);

    // ── Jacobian Inverse 求解器 ──
    IKResult solve_jacobian(IKChain& chain, std::vector<Joint>& joints);

    // 关节限制约束
    void enforce_limits(std::vector<Joint>& joints);

    // 计算末端效应器位置
    Vec3 compute_effector_position(const IKChain& chain,
                                    const std::vector<Joint>& joints) const;

    struct Impl;
    std::unique_ptr<Impl> impl_;
};

// ── 高级：全身 IK ────────────────────────────────────
class FullBodyIK {
public:
    FullBodyIK();
    ~FullBodyIK();

    // 添加肢体链
    bool add_limb(const std::string& name, const IKChain& chain);

    // 设置多目标
    void set_left_hand_target(const Vec3& target);
    void set_right_hand_target(const Vec3& target);
    void set_left_foot_target(const Vec3& target);
    void set_right_foot_target(const Vec3& target);

    // 设置骨盆目标 (用于蹲下/跳跃)
    void set_pelvis_target(const Vec3& target);

    // 设置头部朝向
    void set_head_look_at(const Vec3& target, float weight = 1.0f);

    // 一步求解（支持混合权重）
    bool solve_step(float delta_time);

    // 获取当前姿态关节
    const std::vector<Joint>& get_pose() const;

    // 启用/禁用链
    void set_chain_enabled(const std::string& name, bool enabled);

private:
    struct Impl;
    std::unique_ptr<Impl> impl_;
};

// ── 便捷工厂函数 ────────────────────────────────────
IKChain create_arm_chain(const std::string& side,
                          const std::vector<Joint>& skeleton);
IKChain create_leg_chain(const std::string& side,
                          const std::vector<Joint>& skeleton);
IKChain create_spine_chain(const std::vector<Joint>& skeleton);

}  // namespace solra::animation
