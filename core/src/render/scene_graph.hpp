#pragma once
// 3D Scene Graph: hierarchical spatial management with dirty-flag propagation
#include <cstdint>
#include <functional>
#include <string>
#include <vector>
#include <memory>
#include <array>

namespace solra::render {

// ---- Math primitives ----
struct Vec3 { float x = 0, y = 0, z = 0; };
struct Quat { float x = 0, y = 0, z = 0, w = 1; };
using Mat4 = std::array<float, 16>;

Mat4 mat4Identity();
Mat4 mat4Mul(const Mat4& a, const Mat4& b);
Mat4 translate(const Mat4& m, const Vec3& v);
Mat4 rotate(const Mat4& m, const Quat& q);
Mat4 scale(const Mat4& m, const Vec3& s);

// ---- Transform ----
class Transform {
public:
    Vec3 position{0, 0, 0};
    Quat rotation{0, 0, 0, 1};
    Vec3 scale{1, 1, 1};

    bool dirty = true;      // local transform changed
    bool worldDirty = true; // world matrix needs recompute

    void setPosition(const Vec3& p) { position = p; dirty = true; }
    void setRotation(const Quat& r) { rotation = r; dirty = true; }
    void setScale(const Vec3& s)    { scale = s;    dirty = true; }

    Mat4 localMatrix() const;
};

// ---- Base Node ----
class SceneNode : public std::enable_shared_from_this<SceneNode> {
public:
    std::string name;
    Transform transform;
    bool active = true;

    SceneNode* parent() const { return parent_; }
    const std::vector<std::shared_ptr<SceneNode>>& children() const { return children_; }

    void addChild(std::shared_ptr<SceneNode> child);
    void removeChild(std::shared_ptr<SceneNode> child);
    void removeFromParent();

    // World-space transform (lazy-evaluated with dirty propagation)
    const Mat4& worldMatrix();
    Vec3 worldPosition();
    Quat worldRotation();

    // Recursive visitor
    using VisitorFn = std::function<void(SceneNode*)>;
    void traverse(const VisitorFn& pre, const VisitorFn& post = nullptr);

protected:
    SceneNode* parent_ = nullptr;
    std::vector<std::shared_ptr<SceneNode>> children_;
    mutable Mat4 worldMatrix_ = mat4Identity();
    mutable Vec3 worldPos_{};
    mutable Quat worldRot_{0,0,0,1};
    mutable bool worldValid_ = false;

    void markWorldDirty();
    void recomputeWorldMatrix() const;
};

// ---- Spatial partitioning ----
struct BoundingBox {
    Vec3 min, max;
    BoundingBox merge(const BoundingBox& other) const;
    bool contains(const Vec3& p) const;
    bool intersects(const BoundingBox& other) const;
};

class SceneGraph {
public:
    std::shared_ptr<SceneNode> root;

    explicit SceneGraph(std::shared_ptr<SceneNode> root = nullptr);
    SceneNode* findNode(const std::string& name) const;
    std::vector<SceneNode*> findNodesByTag(const std::string& tag) const;
    std::vector<SceneNode*> frustumCull(const Mat4& viewProj) const;

private:
    void findNodeRecursive(SceneNode* node, const std::string& name,
                          SceneNode*& result) const;
};

} // namespace solra::render
