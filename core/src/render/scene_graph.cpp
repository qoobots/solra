#include "scene_graph.hpp"
#include <algorithm>
#include <array>
#include <cmath>
#include <functional>
#include <stdexcept>

namespace solra::render {

// ---- Math ----
Mat4 mat4Identity() {
    return {1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1};
}

Mat4 mat4Mul(const Mat4& a, const Mat4& b) {
    Mat4 r{};
    for (int col = 0; col < 4; ++col)
        for (int row = 0; row < 4; ++row)
            for (int k = 0; k < 4; ++k)
                r[col * 4 + row] += a[k * 4 + row] * b[col * 4 + k];
    return r;
}

// ---- Transform ----
Mat4 Transform::localMatrix() const {
    Mat4 m = mat4Identity();
    m = ::solra::render::translate(m, position);
    // Simplified rotation→matrix (full impl uses quaternion-to-matrix)
    m = ::solra::render::scale(m, this->scale);
    // ...
    return m;
}

Mat4 translate(const Mat4& m, const Vec3& v) {
    Mat4 r = m;
    r[12] += v.x; r[13] += v.y; r[14] += v.z;
    return r;
}

Mat4 rotate(const Mat4& m, const Quat& q) {
    // Quaternion to rotation matrix (column-major)
    float xx = q.x*q.x, yy = q.y*q.y, zz = q.z*q.z;
    float xy = q.x*q.y, xz = q.x*q.z, yz = q.y*q.z;
    float wx = q.w*q.x, wy = q.w*q.y, wz = q.w*q.z;

    Mat4 rot = {
        1-2*(yy+zz), 2*(xy+wz),   2*(xz-wy),   0,
        2*(xy-wz),   1-2*(xx+zz), 2*(yz+wx),   0,
        2*(xz+wy),   2*(yz-wx),   1-2*(xx+yy), 0,
        0,           0,           0,           1
    };
    return mat4Mul(m, rot);
}

Mat4 scale(const Mat4& m, const Vec3& s) {
    Mat4 r = m;
    r[0]*=s.x; r[1]*=s.x; r[2]*=s.x;
    r[4]*=s.y; r[5]*=s.y; r[6]*=s.y;
    r[8]*=s.z; r[9]*=s.z; r[10]*=s.z;
    return r;
}

// ---- SceneNode ----
void SceneNode::addChild(std::shared_ptr<SceneNode> child) {
    if (child->parent_) child->removeFromParent();
    child->parent_ = this;
    child->markWorldDirty();
    children_.push_back(std::move(child));
}

void SceneNode::removeChild(std::shared_ptr<SceneNode> child) {
    auto it = std::find(children_.begin(), children_.end(), child);
    if (it != children_.end()) {
        (*it)->parent_ = nullptr;
        children_.erase(it);
    }
}

void SceneNode::removeFromParent() {
    if (parent_) parent_->removeChild(shared_from_this());
}

void SceneNode::markWorldDirty() {
    transform.worldDirty = true;
    worldValid_ = false;
    for (auto& child : children_) child->markWorldDirty();
}

const Mat4& SceneNode::worldMatrix() {
    if (!worldValid_) recomputeWorldMatrix();
    return worldMatrix_;
}

void SceneNode::recomputeWorldMatrix() const {
    if (parent_) {
        worldMatrix_ = mat4Mul(parent_->worldMatrix(), transform.localMatrix());
    } else {
        worldMatrix_ = transform.localMatrix();
    }
    worldValid_ = true;
}

Vec3 SceneNode::worldPosition() {
    const Mat4& wm = worldMatrix();
    return {wm[12], wm[13], wm[14]};
}

Quat SceneNode::worldRotation() {
    recomputeWorldMatrix();
    // Extract quaternion from world matrix (simplified)
    return transform.rotation;
}

void SceneNode::traverse(const VisitorFn& pre, const VisitorFn& post) {
    if (!active) return;
    if (pre) pre(this);
    for (auto& child : children_) child->traverse(pre, post);
    if (post) post(this);
}

// ---- SceneGraph ----
SceneGraph::SceneGraph(std::shared_ptr<SceneNode> rt) : root(rt ? rt : std::make_shared<SceneNode>()) {
    root->name = "Root";
}

SceneNode* SceneGraph::findNode(const std::string& name) const {
    SceneNode* result = nullptr;
    std::function<void(SceneNode*)> pre = [&](SceneNode* n) {
        if (n->name == name) result = n;
    };
    root->traverse(pre);
    return result;
}

std::vector<SceneNode*> SceneGraph::findNodesByTag(const std::string& tag) const {
    std::vector<SceneNode*> results;
    std::function<void(SceneNode*)> pre = [&](SceneNode* n) {
        if (n->name.find(tag) != std::string::npos) results.push_back(n);
    };
    root->traverse(pre);
    return results;
}

// ============================================================
// Frustum plane extraction from view-projection matrix
// ============================================================
struct FrustumPlane {
    float nx, ny, nz, d; // plane equation: nx*x + ny*y + nz*z + d >= 0 (inside)
};

static std::array<FrustumPlane, 6> extractFrustumPlanes(const Mat4& vp) {
    // VP is column-major: vp[col*4 + row]
    // Left plane:   row3 + row0
    // Right plane:  row3 - row0
    // Bottom plane: row3 + row1
    // Top plane:    row3 - row1
    // Near plane:   row3 + row2
    // Far plane:    row3 - row2
    std::array<FrustumPlane, 6> planes;

    auto normalize = [](FrustumPlane& p) {
        float len = std::sqrt(p.nx*p.nx + p.ny*p.ny + p.nz*p.nz);
        if (len > 0.0001f) {
            p.nx /= len; p.ny /= len; p.nz /= len; p.d /= len;
        }
    };

    // Left
    planes[0] = {
        vp[3] + vp[0], vp[7] + vp[4], vp[11] + vp[8], vp[15] + vp[12]
    };
    // Right
    planes[1] = {
        vp[3] - vp[0], vp[7] - vp[4], vp[11] - vp[8], vp[15] - vp[12]
    };
    // Bottom
    planes[2] = {
        vp[3] + vp[1], vp[7] + vp[5], vp[11] + vp[9], vp[15] + vp[13]
    };
    // Top
    planes[3] = {
        vp[3] - vp[1], vp[7] - vp[5], vp[11] - vp[9], vp[15] - vp[13]
    };
    // Near
    planes[4] = {
        vp[3] + vp[2], vp[7] + vp[6], vp[11] + vp[10], vp[15] + vp[14]
    };
    // Far
    planes[5] = {
        vp[3] - vp[2], vp[7] - vp[6], vp[11] - vp[10], vp[15] - vp[14]
    };

    for (auto& p : planes) normalize(p);
    return planes;
}

// Test if an AABB is inside (or intersecting) the frustum
static bool aabbInFrustum(const std::array<FrustumPlane, 6>& planes,
                          const Vec3& min, const Vec3& max) {
    for (const auto& p : planes) {
        // Test the most-positive corner against the plane
        float px = p.nx > 0 ? max.x : min.x;
        float py = p.ny > 0 ? max.y : min.y;
        float pz = p.nz > 0 ? max.z : min.z;

        if (p.nx * px + p.ny * py + p.nz * pz + p.d < 0) {
            return false; // outside this plane
        }
    }
    return true; // inside all planes
}

std::vector<SceneNode*> SceneGraph::frustumCull(const Mat4& viewProj) const {
    std::vector<SceneNode*> visible;
    auto planes = extractFrustumPlanes(viewProj);

    std::function<void(SceneNode*)> pre = [&](SceneNode* n) {
        // Leaf nodes with meshes: test their world-space AABB
        // For now, use a simple sphere approximation around world position
        Vec3 pos = n->worldPosition();
        float radius = 1.5f; // default bounding sphere radius

        // Create AABB from sphere
        Vec3 aabbMin{pos.x - radius, pos.y - radius, pos.z - radius};
        Vec3 aabbMax{pos.x + radius, pos.y + radius, pos.z + radius};

        if (aabbInFrustum(planes, aabbMin, aabbMax)) {
            visible.push_back(n);
        }
    };

    root->traverse(pre);
    return visible;
}

} // namespace solra::render
