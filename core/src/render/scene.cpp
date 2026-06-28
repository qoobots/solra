/*
 * Solra Core SDK - Scene graph implementation (stub)
 *
 * Hierarchical spatial structure for 3D content management.
 */

#include <solra/solra_render.h>
#include <spdlog/spdlog.h>
#include <glm/glm.hpp>
#include <glm/gtc/matrix_transform.hpp>
#include <vector>
#include <string>

namespace solra {
namespace render {

struct SceneNode {
  std::string name;
  glm::vec3 position{0.0f};
  glm::quat rotation{1.0f, 0.0f, 0.0f, 0.0f};
  glm::vec3 scale{1.0f};
  void *mesh{nullptr};  /* SolraMeshHandle */
  SceneNode *parent{nullptr};
  std::vector<SceneNode *> children;
  bool dirty{true};
  glm::mat4 local_transform{1.0f};
  glm::mat4 world_transform{1.0f};

  void update_transform() {
    if (!dirty) return;

    glm::mat4 T = glm::translate(glm::mat4(1.0f), position);
    glm::mat4 R = glm::mat4_cast(rotation);
    glm::mat4 S = glm::scale(glm::mat4(1.0f), scale);
    local_transform = T * R * S;

    if (parent) {
      world_transform = parent->world_transform * local_transform;
    } else {
      world_transform = local_transform;
    }

    dirty = false;
    for (auto *child : children) {
      child->dirty = true;
      child->update_transform();
    }
  }
};

struct Scene {
  SceneNode *root{nullptr};
  std::vector<SceneNode *> nodes;
};

} // namespace render
} // namespace solra
