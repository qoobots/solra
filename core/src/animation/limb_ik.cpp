#include "limb_ik.hpp"

#include <algorithm>
#include <cmath>
#include <map>

namespace solra::animation {

// ── Impl (LimbIKSolver) ─────────────────────────────
struct LimbIKSolver::Impl {
    IKSolverConfig config;
    std::map<std::string, IKChain> chains;
    std::vector<Joint> joints;
    bool joints_dirty = true;
};

LimbIKSolver::LimbIKSolver()
    : impl_(std::make_unique<Impl>()) {}

LimbIKSolver::LimbIKSolver(const IKSolverConfig& config)
    : LimbIKSolver() {
    impl_->config = config;
}

LimbIKSolver::~LimbIKSolver() = default;

int LimbIKSolver::register_chain(const IKChain& chain) {
    impl_->chains[chain.name] = chain;
    return static_cast<int>(impl_->chains.size());
}

bool LimbIKSolver::remove_chain(const std::string& name) {
    return impl_->chains.erase(name) > 0;
}

bool LimbIKSolver::set_target(const std::string& chain_name,
                                const Vec3& position) {
    auto it = impl_->chains.find(chain_name);
    if (it == impl_->chains.end()) return false;
    it->second.target = position;
    return true;
}

bool LimbIKSolver::set_target_rotation(const std::string& chain_name,
                                         const Quat& rotation) {
    auto it = impl_->chains.find(chain_name);
    if (it == impl_->chains.end()) return false;
    it->second.target_rotation = rotation;
    it->second.rotation_ik = true;
    return true;
}

void LimbIKSolver::update_joints(const std::vector<Joint>& joints) {
    impl_->joints = joints;
    impl_->joints_dirty = false;

    // 更新父子关系和长度
    for (auto& [name, chain] : impl_->chains) {
        for (size_t i = 0; i < chain.joint_indices.size(); i++) {
            int idx = chain.joint_indices[i];
            if (idx >= 0 && static_cast<size_t>(idx) < joints.size()) {
                if (i + 1 < chain.joint_indices.size()) {
                    int next = chain.joint_indices[i + 1];
                    if (next >= 0 && static_cast<size_t>(next) < joints.size()) {
                        impl_->joints[idx].length =
                            (joints[next].position -
                             joints[idx].position).length();
                        impl_->joints[idx].parent_idx = (i == 0) ? -1 :
                            chain.joint_indices[i - 1];
                    }
                }
            }
        }
    }
}

std::vector<IKResult> LimbIKSolver::solve() {
    std::vector<IKResult> results;
    for (auto& [name, chain] : impl_->chains) {
        results.push_back(solve_chain(name));
    }
    return results;
}

IKResult LimbIKSolver::solve_chain(const std::string& chain_name) {
    auto it = impl_->chains.find(chain_name);
    if (it == impl_->chains.end()) return IKResult{};

    auto& chain = it->second;

    switch (impl_->config.solver) {
    case IKSolverType::CCD:
        return solve_ccd(chain, impl_->joints);
    case IKSolverType::FABRIK:
        return solve_fabrik(chain, impl_->joints);
    case IKSolverType::Jacobian:
        return solve_jacobian(chain, impl_->joints);
    default:
        return IKResult{};
    }
}

// ── CCD 求解器 ──────────────────────────────────────
IKResult LimbIKSolver::solve_ccd(IKChain& chain,
                                   std::vector<Joint>& joints) {
    IKResult result;
    Vec3 effector_pos = compute_effector_position(chain, joints);

    for (int iter = 0; iter < chain.max_iterations; iter++) {
        // 从末端倒数第二个关节向根迭代
        for (int i = static_cast<int>(chain.joint_indices.size()) - 2; i >= 0; i--) {
            int jidx = chain.joint_indices[i];
            Joint& joint = joints[jidx];

            Vec3 to_effector = (effector_pos - joint.position).normalized();
            Vec3 to_target = (chain.target - joint.position).normalized();

            // 旋转轴
            Vec3 axis = to_effector.cross(to_target);
            float axis_len = axis.length();
            if (axis_len < 1e-6f) continue;

            axis = axis / axis_len;
            float angle = std::acos(
                std::clamp(to_effector.dot(to_target), -1.0f, 1.0f));

            // 构建旋转四元数
            Quat rotation;
            rotation.from_axis_angle(axis, angle);
            joint.rotation = rotation * joint.rotation;
            joint.rotation.normalize();

            // 正向传播: 更新下游关节位置
            for (size_t j = i + 1; j < chain.joint_indices.size(); j++) {
                int curr = chain.joint_indices[j];
                int prev = chain.joint_indices[j - 1];
                Vec3 dir = (joints[curr].position -
                            joints[prev].position).normalized();
                joints[curr].position =
                    joints[prev].position + dir * joints[prev].length;
            }

            // 约束关节限制
            if (impl_->config.enforce_joint_limits && joint.has_limit) {
                enforce_limits(joints);
            }
        }

        // 检查收敛
        effector_pos = compute_effector_position(chain, joints);
        result.final_error = (chain.target - effector_pos).length();
        result.iterations_used = iter + 1;

        if (result.final_error < chain.tolerance) {
            result.converged = true;
            break;
        }
    }

    return result;
}

// ── FABRIK 求解器 ───────────────────────────────────
IKResult LimbIKSolver::solve_fabrik(IKChain& chain,
                                      std::vector<Joint>& joints) {
    IKResult result;
    const auto& indices = chain.joint_indices;
    size_t n = indices.size();

    // 收集关节位置 (局部副本)
    std::vector<Vec3> positions(n);
    std::vector<float> lengths(n - 1);
    for (size_t i = 0; i < n; i++) {
        positions[i] = joints[indices[i]].position;
    }
    for (size_t i = 0; i < n - 1; i++) {
        lengths[i] = (positions[i + 1] - positions[i]).length();
    }

    Vec3 root_pos = positions[0];

    for (int iter = 0; iter < chain.max_iterations; iter++) {
        // ── 前向 (从末端到根) ──
        positions.back() = chain.target;
        for (int i = static_cast<int>(n) - 2; i >= 0; i--) {
            Vec3 dir = (positions[i] - positions[i + 1]).normalized();
            positions[i] = positions[i + 1] + dir * lengths[i];
        }

        // ── 后向 (从根到末端) ──
        positions[0] = root_pos;
        for (size_t i = 0; i < n - 1; i++) {
            Vec3 dir = (positions[i + 1] - positions[i]).normalized();
            positions[i + 1] = positions[i] + dir * lengths[i];
        }

        // 收敛检查
        result.final_error =
            (chain.target - positions.back()).length();
        result.iterations_used = iter + 1;

        if (result.final_error < chain.tolerance) {
            result.converged = true;
            break;
        }
    }

    // 回写关节位置
    for (size_t i = 0; i < n; i++) {
        joints[indices[i]].position = positions[i];
    }

    return result;
}

// ── Jacobian Inverse 求解器 ─────────────────────────
IKResult LimbIKSolver::solve_jacobian(IKChain& chain,
                                        std::vector<Joint>& joints) {
    IKResult result;
    const auto& indices = chain.joint_indices;
    size_t n = indices.size() - 1; // 自由度数量 (不包括末端)

    Vec3 effector = compute_effector_position(chain, joints);
    Vec3 error_vec = chain.target - effector;

    for (int iter = 0; iter < chain.max_iterations; iter++) {
        float error = error_vec.length();
        result.final_error = error;
        result.iterations_used = iter + 1;

        if (error < chain.tolerance) {
            result.converged = true;
            break;
        }

        // 构建 Jacobian (3 x n)
        for (size_t j = 0; j < n; j++) {
            int jidx = indices[j];
            Vec3 axis(0, 0, 1); // 简化：假设 Z 轴旋转

            // 计算末端相对于该关节的位置
            Vec3 rel_pos = effector - joints[jidx].position;
            Vec3 j_col = axis.cross(rel_pos);

            // 伪逆更新 (阻尼最小二乘)
            float j_dot_e = j_col.dot(error_vec);
            float j_dot_j = j_col.dot(j_col) +
                           impl_->config.dampening * impl_->config.dampening;
            if (j_dot_j < 1e-8f) continue;

            float dtheta = j_dot_e / j_dot_j;
            Quat dq;
            dq.from_axis_angle(axis, dtheta);
            joints[jidx].rotation = dq * joints[jidx].rotation;
            joints[jidx].rotation.normalize();
        }

        // 正运动学更新
        effector = compute_effector_position(chain, joints);
        error_vec = chain.target - effector;
    }

    return result;
}

// ── 关节限制 ────────────────────────────────────────
void LimbIKSolver::enforce_limits(std::vector<Joint>& joints) {
    for (auto& joint : joints) {
        if (!joint.has_limit) continue;
        Vec3 euler = joint.rotation.to_euler();
        euler.x = std::clamp(euler.x, joint.min_euler.x, joint.max_euler.x);
        euler.y = std::clamp(euler.y, joint.min_euler.y, joint.max_euler.y);
        euler.z = std::clamp(euler.z, joint.min_euler.z, joint.max_euler.z);
        joint.rotation = Quat::from_euler(euler);
    }
}

// ── 末端效应器位置 ──────────────────────────────────
Vec3 LimbIKSolver::compute_effector_position(
    const IKChain& chain,
    const std::vector<Joint>& joints) const {
    if (chain.joint_indices.empty()) return Vec3{};
    int last_idx = chain.joint_indices.back();
    return joints[last_idx].position;
}

const std::vector<Joint>& LimbIKSolver::get_joints() const {
    return impl_->joints;
}

void LimbIKSolver::set_config(const IKSolverConfig& config) {
    impl_->config = config;
}

void LimbIKSolver::reset() {
    impl_->joints.clear();
    impl_->chains.clear();
}

// ── 工厂函数 ────────────────────────────────────────
IKChain create_arm_chain(const std::string& side,
                          const std::vector<Joint>& skeleton) {
    IKChain chain;
    chain.name = side + "_arm";

    // 查找关节索引 (名称约定)
    std::string prefix = (side == "Left") ? "Left" : "Right";
    for (size_t i = 0; i < skeleton.size(); i++) {
        const auto& j = skeleton[i];
        if (j.name == prefix + "Shoulder") chain.joint_indices.push_back(i);
        if (j.name == prefix + "UpperArm") chain.joint_indices.push_back(i);
        if (j.name == prefix + "LowerArm") chain.joint_indices.push_back(i);
        if (j.name == prefix + "Hand") chain.joint_indices.push_back(i);
    }

    if (chain.joint_indices.size() >= 2) {
        chain.root = chain.joint_indices.front();
        chain.effector = chain.joint_indices.back();
    }

    return chain;
}

IKChain create_leg_chain(const std::string& side,
                          const std::vector<Joint>& skeleton) {
    IKChain chain;
    chain.name = side + "_leg";

    std::string prefix = (side == "Left") ? "Left" : "Right";
    for (size_t i = 0; i < skeleton.size(); i++) {
        const auto& j = skeleton[i];
        if (j.name == prefix + "UpperLeg") chain.joint_indices.push_back(i);
        if (j.name == prefix + "LowerLeg") chain.joint_indices.push_back(i);
        if (j.name == prefix + "Foot") chain.joint_indices.push_back(i);
        if (j.name == prefix + "Toes") chain.joint_indices.push_back(i);
    }

    if (chain.joint_indices.size() >= 2) {
        chain.root = chain.joint_indices.front();
        chain.effector = chain.joint_indices.back();
    }

    return chain;
}

IKChain create_spine_chain(const std::vector<Joint>& skeleton) {
    IKChain chain;
    chain.name = "spine";

    for (size_t i = 0; i < skeleton.size(); i++) {
        const auto& j = skeleton[i];
        if (j.name.find("Spine") != std::string::npos ||
            j.name == "Pelvis" ||
            j.name == "Hips") {
            chain.joint_indices.push_back(i);
        }
    }

    if (chain.joint_indices.size() >= 2) {
        chain.root = chain.joint_indices.front();
        chain.effector = chain.joint_indices.back();
    }

    return chain;
}

}  // namespace solra::animation
