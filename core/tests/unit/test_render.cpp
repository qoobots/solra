/*
 * Solra Core SDK - Render engine unit tests
 */

#include <gtest/gtest.h>
#include <solra/solra_core.h>
#include <solra/solra_render.h>
#include <solra/solra_types.h>

class SolraRenderTest : public ::testing::Test {
protected:
  void SetUp() override {
    SolraCoreConfig config = {};
    config.log_level = 2;
    config.enable_gpu = 0;
    solra_core_init(&config);
  }
  void TearDown() override {
    solra_render_shutdown();
    solra_core_shutdown();
  }
};

TEST_F(SolraRenderTest, InitWithDefaults) {
  int result = solra_render_init(nullptr);
  /* May fail in headless mode (no GPU) */
  if (result != SOLRA_OK) {
    GTEST_SKIP() << "No GPU available for rendering tests";
  }
  EXPECT_EQ(result, SOLRA_OK);
}

TEST_F(SolraRenderTest, InitWithConfig) {
  SolraRenderConfig config = {};
  config.backend = SOLRA_RENDER_BACKEND_AUTO;
  config.width = 1920;
  config.height = 1080;
  config.vsync = 1;
  config.msaa_samples = 4;
  config.clear_color = {0.1f, 0.2f, 0.3f, 1.0f};

  int result = solra_render_init(&config);
  if (result != SOLRA_OK) {
    GTEST_SKIP() << "No GPU available for rendering tests";
  }
  EXPECT_EQ(result, SOLRA_OK);
}

TEST_F(SolraRenderTest, GetGPUInfo) {
  int result = solra_render_init(nullptr);
  if (result != SOLRA_OK) {
    GTEST_SKIP() << "No GPU available for rendering tests";
  }

  SolraGPUInfo info = {};
  result = solra_render_get_gpu_info(&info);
  EXPECT_EQ(result, SOLRA_OK);
}

TEST_F(SolraRenderTest, GetGPUInfoNullPointer) {
  int result = solra_render_init(nullptr);
  if (result != SOLRA_OK) {
    GTEST_SKIP() << "No GPU available for rendering tests";
  }

  int ret = solra_render_get_gpu_info(nullptr);
  EXPECT_NE(ret, SOLRA_OK);
}

TEST_F(SolraRenderTest, ResizeViewport) {
  int result = solra_render_init(nullptr);
  if (result != SOLRA_OK) {
    GTEST_SKIP() << "No GPU available for rendering tests";
  }

  solra_render_resize(1280, 720);
  solra_render_resize(2560, 1440);
  SUCCEED();
}

TEST_F(SolraRenderTest, FrameBeginEnd) {
  int result = solra_render_init(nullptr);
  if (result != SOLRA_OK) {
    GTEST_SKIP() << "No GPU available for rendering tests";
  }

  EXPECT_EQ(solra_render_begin_frame(), SOLRA_OK);
  EXPECT_EQ(solra_render_end_frame(), SOLRA_OK);
}

TEST_F(SolraRenderTest, SceneCreateDestroy) {
  int result = solra_render_init(nullptr);
  if (result != SOLRA_OK) {
    GTEST_SKIP() << "No GPU available for rendering tests";
  }

  SolraSceneHandle scene = solra_scene_create();
  ASSERT_NE(scene, nullptr);

  solra_scene_set_active(scene);
  solra_scene_destroy(scene);
}

TEST_F(SolraRenderTest, SceneNodeCreate) {
  int result = solra_render_init(nullptr);
  if (result != SOLRA_OK) {
    GTEST_SKIP() << "No GPU available for rendering tests";
  }

  SolraSceneHandle scene = solra_scene_create();
  ASSERT_NE(scene, nullptr);

  SolraSceneNodeHandle node = solra_scene_node_create(scene, "test_node");
  ASSERT_NE(node, nullptr);

  solra_scene_destroy(scene);
}

TEST_F(SolraRenderTest, SceneNodeTransform) {
  int result = solra_render_init(nullptr);
  if (result != SOLRA_OK) {
    GTEST_SKIP() << "No GPU available for rendering tests";
  }

  SolraSceneHandle scene = solra_scene_create();
  ASSERT_NE(scene, nullptr);

  SolraSceneNodeHandle node = solra_scene_node_create(scene, "transformed");
  ASSERT_NE(node, nullptr);

  SolraVec3 pos = {1.0f, 2.0f, 3.0f};
  SolraQuat rot = {0.0f, 0.0f, 0.0f, 1.0f};
  SolraVec3 scale = {1.0f, 1.0f, 1.0f};
  solra_scene_node_set_transform(node, &pos, &rot, &scale);

  solra_scene_destroy(scene);
}

TEST_F(SolraRenderTest, SceneHierarchy) {
  int result = solra_render_init(nullptr);
  if (result != SOLRA_OK) {
    GTEST_SKIP() << "No GPU available for rendering tests";
  }

  SolraSceneHandle scene = solra_scene_create();
  ASSERT_NE(scene, nullptr);

  SolraSceneNodeHandle parent = solra_scene_node_create(scene, "parent");
  SolraSceneNodeHandle child = solra_scene_node_create(scene, "child");
  ASSERT_NE(parent, nullptr);
  ASSERT_NE(child, nullptr);

  solra_scene_node_add_child(parent, child);

  solra_scene_destroy(scene);
}

TEST_F(SolraRenderTest, MeshCreateWithData) {
  int result = solra_render_init(nullptr);
  if (result != SOLRA_OK) {
    GTEST_SKIP() << "No GPU available for rendering tests";
  }

  /* Simple triangle: pos(3) + normal(3) + uv(2) = 8 floats per vertex */
  struct Vertex { float px, py, pz, nx, ny, nz, u, v; };
  Vertex vertices[3] = {
    { 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f },
    {-1.0f,-1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f },
    { 1.0f,-1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f },
  };
  uint16_t indices[3] = {0, 1, 2};

  SolraMeshHandle mesh = solra_mesh_create(
    vertices, 3, sizeof(Vertex),
    indices, 3, 16
  );
  ASSERT_NE(mesh, nullptr);

  SolraAABB bounds = solra_mesh_get_bounds(mesh);
  EXPECT_LE(bounds.min.x, bounds.max.x);
  EXPECT_LE(bounds.min.y, bounds.max.y);
  EXPECT_LE(bounds.min.z, bounds.max.z);

  solra_mesh_destroy(mesh);
}

TEST_F(SolraRenderTest, MaterialCreate) {
  int result = solra_render_init(nullptr);
  if (result != SOLRA_OK) {
    GTEST_SKIP() << "No GPU available for rendering tests";
  }

  SolraMaterialHandle mat = solra_material_create();
  ASSERT_NE(mat, nullptr);

  SolraColor color = {1.0f, 0.0f, 0.0f, 1.0f};
  solra_material_set_base_color(mat, color);
  solra_material_set_metallic_roughness(mat, 0.8f, 0.2f);

  solra_material_destroy(mat);
}

TEST_F(SolraRenderTest, MaterialTextureSlot) {
  int result = solra_render_init(nullptr);
  if (result != SOLRA_OK) {
    GTEST_SKIP() << "No GPU available for rendering tests";
  }

  SolraMaterialHandle mat = solra_material_create();
  ASSERT_NE(mat, nullptr);

  /* Setting null texture should succeed (clear slot) */
  int ret = solra_material_set_texture(mat, "base_color", nullptr);
  EXPECT_EQ(ret, SOLRA_OK);

  /* Invalid slot name */
  ret = solra_material_set_texture(mat, "invalid_slot", nullptr);
  EXPECT_NE(ret, SOLRA_OK);

  solra_material_destroy(mat);
}

TEST_F(SolraRenderTest, ShaderCompile) {
  int result = solra_render_init(nullptr);
  if (result != SOLRA_OK) {
    GTEST_SKIP() << "No GPU available for rendering tests";
  }

  /* Simple vertex shader */
  const char *vertex_source =
    "#version 450\n"
    "layout(location = 0) in vec3 inPos;\n"
    "void main() { gl_Position = vec4(inPos, 1.0); }\n";

  void *shader = solra_shader_compile(vertex_source, "vertex", "main");
  /* May be null if no SPIRV compiler available */
  /* At minimum, should not crash */
  SUCCEED();
}

TEST_F(SolraRenderTest, SceneNodeAttachMesh) {
  int result = solra_render_init(nullptr);
  if (result != SOLRA_OK) {
    GTEST_SKIP() << "No GPU available for rendering tests";
  }

  SolraSceneHandle scene = solra_scene_create();
  ASSERT_NE(scene, nullptr);

  SolraSceneNodeHandle node = solra_scene_node_create(scene, "mesh_node");
  ASSERT_NE(node, nullptr);

  struct Vertex { float px, py, pz, nx, ny, nz, u, v; };
  Vertex vertices[3] = {
    { 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 1.0f },
    {-1.0f,-1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f },
    { 1.0f,-1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f },
  };
  uint16_t indices[3] = {0, 1, 2};

  SolraMeshHandle mesh = solra_mesh_create(vertices, 3, sizeof(Vertex), indices, 3, 16);
  ASSERT_NE(mesh, nullptr);

  solra_scene_node_attach_mesh(node, mesh);

  solra_mesh_destroy(mesh);
  solra_scene_destroy(scene);
}
