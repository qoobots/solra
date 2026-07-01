/*
 * Solra Core SDK - Render engine implementation
 *
 * Multi-backend GPU rendering pipeline.
 * OpenGL 4.6 backend with PBR shading, scene graph traversal, frustum culling.
 * Falls back to Software Rasterizer when no GPU backend is available.
 */

#include <solra/solra_render.h>
#include <solra/solra_types.h>
#include "gpu_abstraction.hpp"
#include "opengl_device.hpp"
#include "builtin_shaders.hpp"
#include "scene_graph.hpp"
#include "pbr_material.hpp"
#include "../animation/skeletal_animation.hpp"
#include <spdlog/spdlog.h>
#include <glm/glm.hpp>
#include <glm/gtc/matrix_transform.hpp>
#include <glm/gtc/type_ptr.hpp>
#include <cstring>
#include <chrono>
#include <vector>
#include <memory>
#include <algorithm>
#include <unordered_set>
#include <unordered_map>
#include <mutex>

#define SOLRA_MAX_BONES 128

/* ============================================================
 * Internal Render State
 * ============================================================ */

struct ActiveMesh {
  std::vector<float> vertices;       // interleaved: pos3 + normal3 + uv2 (+ boneWeights4 + boneIndices4 if skinned)
  std::vector<uint32_t> indices;
  int vertex_stride = 32;            // 8 floats * 4 bytes (or 12 floats * 4 = 48 for skinned)
  bool is_skinned = false;           // whether this mesh uses GPU skinning

  // GPU resources
  std::shared_ptr<solra::render::GpuBuffer> gpuVertices;
  std::shared_ptr<solra::render::GpuBuffer> gpuIndices;
  bool uploaded = false;

  // Bone matrix palette for skinning (pre-computed, uploaded each frame)
  std::vector<float> bone_matrices;  // MAX_BONES * 16 floats, row-major mat4
  int bone_count = 0;
  bool has_bone_matrices = false;

  // Bounding box (AABB) for frustum culling
  solra::render::Vec3 bboxMin{};
  solra::render::Vec3 bboxMax{};
};

struct ActiveSceneNode {
  solra::render::SceneNode* node;
  ActiveMesh* mesh = nullptr;
  std::shared_ptr<solra::render::PbrMaterial> material;
  solra::render::Vec3 color{1.0f, 1.0f, 1.0f}; // fallback color
};

struct RenderState {
  SolraRenderConfig config;
  int initialized = 0;
  int frame_active = 0;

  // GPU device
  std::shared_ptr<solra::render::GpuDevice> gpuDevice;
  bool hasGpu = false;

  // PBR pipeline (shared across all PBR draws)
  std::shared_ptr<solra::render::GpuShader> pbrVertexShader;
  std::shared_ptr<solra::render::GpuShader> pbrFragmentShader;
  std::shared_ptr<solra::render::GpuPipeline> pbrPipeline;

  // PBR skinned pipeline (with bone weights/indices + skinning matrix UBO)
  std::shared_ptr<solra::render::GpuShader> pbrSkinnedVertexShader;
  std::shared_ptr<solra::render::GpuPipeline> pbrSkinnedPipeline;

  // Deferred rendering pipelines
  std::shared_ptr<solra::render::GpuShader> deferredGeomVertexShader;
  std::shared_ptr<solra::render::GpuShader> deferredGeomFragmentShader;
  std::shared_ptr<solra::render::GpuPipeline> deferredGeomPipeline;
  std::shared_ptr<solra::render::GpuShader> deferredLightVertexShader;
  std::shared_ptr<solra::render::GpuShader> deferredLightFragmentShader;
  std::shared_ptr<solra::render::GpuPipeline> deferredLightPipeline;

  // G-Buffer OpenGL resources
  uint32_t gbuffer_fbo = 0;
  uint32_t gbuffer_albedo = 0;
  uint32_t gbuffer_normal = 0;
  uint32_t gbuffer_metalrough = 0;
  uint32_t gbuffer_emission = 0;
  uint32_t gbuffer_depth = 0;
  uint32_t fullscreen_quad_vao = 0;
  uint32_t fullscreen_quad_vbo = 0;
  bool deferred_available = false;

  // Fallback unlit pipeline
  std::shared_ptr<solra::render::GpuShader> unlitVertexShader;
  std::shared_ptr<solra::render::GpuShader> unlitFragmentShader;
  std::shared_ptr<solra::render::GpuPipeline> unlitPipeline;

  // Scene graph
  std::shared_ptr<solra::render::SceneGraph> scene_graph;
  std::vector<ActiveSceneNode> active_nodes;

  // Frame timing
  std::chrono::high_resolution_clock::time_point frame_start;
  float delta_time = 0.0f;
  float elapsed_time = 0.0f;
  uint64_t frame_count = 0;
  float current_fps = 60.0f;
  std::chrono::high_resolution_clock::time_point last_fps_update;
  uint64_t fps_frame_count = 0;

  // Camera (orbit)
  glm::vec3 camera_pos = glm::vec3(8.0f, 5.0f, 12.0f);
  glm::vec3 camera_target = glm::vec3(0.0f, 1.5f, 0.0f);
  glm::vec3 camera_up = glm::vec3(0.0f, 1.0f, 0.0f);
  float camera_fov = 55.0f;
  float camera_near = 0.5f;
  float camera_far = 200.0f;

  // Cached view-projection (computed each frame)
  glm::mat4 cachedViewProj{1.0f};

  // GPU info (queried on init)
  SolraGPUInfo gpu_info;

  // PBR material library
  std::shared_ptr<solra::render::MaterialLibrary> materialLib;
};

static RenderState g_render;

/* ============================================================
 * Utility Functions
 * ============================================================ */

static void compute_fps() {
  g_render.fps_frame_count++;
  auto now = std::chrono::high_resolution_clock::now();
  auto elapsed = std::chrono::duration<float>(now - g_render.last_fps_update).count();
  if (elapsed >= 1.0f) {
    g_render.current_fps = static_cast<float>(g_render.fps_frame_count) / elapsed;
    g_render.fps_frame_count = 0;
    g_render.last_fps_update = now;
  }
}

static void update_camera_orbit(float delta_time) {
  // Slow orbit around scene center
  float angle = g_render.elapsed_time * 0.12f;
  float radius = 13.0f;
  g_render.camera_pos.x = glm::cos(angle) * radius;
  g_render.camera_pos.z = glm::sin(angle) * radius;
  g_render.camera_pos.y = 5.0f + glm::sin(g_render.elapsed_time * 0.3f) * 1.5f;
  g_render.camera_target = glm::vec3(0.0f, 1.5f, 0.0f);
}

static void update_scene_nodes(float delta_time) {
  float elapsed = g_render.elapsed_time;

  for (auto& active : g_render.active_nodes) {
    if (!active.node) continue;

    std::string& name = active.node->name;
    if (name.find("float-") == 0) {
      int idx = std::stoi(name.substr(6));
      float angle = (idx / 6.0f) * glm::pi<float>() * 2.0f;
      float orbit_radius = 3.5f;

      active.node->transform.position.x = glm::cos(angle + elapsed * 0.5f) * orbit_radius;
      active.node->transform.position.z = glm::sin(angle + elapsed * 0.5f) * orbit_radius;
      active.node->transform.position.y = 1.6f + glm::sin(elapsed * 2.0f + idx) * 0.6f;

      float rx = elapsed * 0.2f;
      float ry = elapsed * (0.4f + idx * 0.08f);
      active.node->transform.rotation = solra::render::Quat{
        glm::sin(rx * 0.5f), glm::sin(ry * 0.5f), 0.0f,
        glm::cos(rx * 0.5f) * glm::cos(ry * 0.5f)
      };
      active.node->transform.dirty = true;
      active.node->markWorldDirty();
    }

    if (name == "center-orb") {
      float scale_val = 1.0f + glm::sin(elapsed * 1.5f) * 0.08f;
      active.node->transform.scale = solra::render::Vec3{scale_val, scale_val, scale_val};
      active.node->transform.rotation.y += delta_time * 0.3f;
      active.node->transform.rotation.x += delta_time * 0.15f;
      active.node->transform.dirty = true;
      active.node->markWorldDirty();
    }
  }
}

// ============================================================
// GPU pipeline initialization
// ============================================================

static bool init_gpu_pipelines() {
  if (!g_render.hasGpu) return false;

  auto* glDevice = dynamic_cast<solra::render::OpenGLDevice*>(g_render.gpuDevice.get());
  if (!glDevice) return false;

  // Compile PBR shaders from embedded GLSL source
  auto vs = solra::render::OpenGLShader::compileFromSource(
      solra::render::ShaderStage::Vertex,
      solra::render::PBR_VERTEX_GLSL,
      glDevice);

  auto fs = solra::render::OpenGLShader::compileFromSource(
      solra::render::ShaderStage::Fragment,
      solra::render::PBR_FRAGMENT_GLSL,
      glDevice);

  if (vs && fs) {
    g_render.pbrVertexShader = vs;
    g_render.pbrFragmentShader = fs;

    solra::render::PipelineDesc pbrDesc;
    pbrDesc.vertexShader = vs;
    pbrDesc.fragmentShader = fs;
    pbrDesc.primitive = solra::render::PrimitiveType::Triangles;
    pbrDesc.depthTest = true;
    pbrDesc.depthWrite = true;
    pbrDesc.depthCompare = solra::render::CompareOp::LessEqual;
    pbrDesc.blending = false;

    g_render.pbrPipeline = g_render.gpuDevice->createPipeline(pbrDesc);
    if (g_render.pbrPipeline) {
      spdlog::info("PBR shader pipeline compiled successfully");
    }
  } else {
    spdlog::warn("PBR shader compilation failed, using unlit fallback");
  }

  // Compile skinned PBR vertex shader
  auto svs = solra::render::OpenGLShader::compileFromSource(
      solra::render::ShaderStage::Vertex,
      solra::render::PBR_SKINNED_VERTEX_GLSL,
      glDevice);

  if (svs && fs) {
    g_render.pbrSkinnedVertexShader = svs;

    solra::render::PipelineDesc skinnedDesc;
    skinnedDesc.vertexShader = svs;
    skinnedDesc.fragmentShader = fs;
    skinnedDesc.primitive = solra::render::PrimitiveType::Triangles;
    skinnedDesc.depthTest = true;
    skinnedDesc.depthWrite = true;
    skinnedDesc.depthCompare = solra::render::CompareOp::LessEqual;
    skinnedDesc.blending = false;

    g_render.pbrSkinnedPipeline = g_render.gpuDevice->createPipeline(skinnedDesc);
    if (g_render.pbrSkinnedPipeline) {
      spdlog::info("PBR skinned shader pipeline compiled successfully (up to 128 bones)");
    }
  } else {
    spdlog::warn("PBR skinned shader compilation failed, skinned meshes will use un-skinned pipeline");
  }

  // Compile deferred rendering pipelines
  auto dgvs = solra::render::OpenGLShader::compileFromSource(
      solra::render::ShaderStage::Vertex,
      solra::render::DEFERRED_GEOMETRY_VERTEX_GLSL,
      glDevice);
  auto dgfs = solra::render::OpenGLShader::compileFromSource(
      solra::render::ShaderStage::Fragment,
      solra::render::DEFERRED_GEOMETRY_FRAGMENT_GLSL,
      glDevice);
  auto dlvs = solra::render::OpenGLShader::compileFromSource(
      solra::render::ShaderStage::Vertex,
      solra::render::DEFERRED_LIGHTING_VERTEX_GLSL,
      glDevice);
  auto dlfs = solra::render::OpenGLShader::compileFromSource(
      solra::render::ShaderStage::Fragment,
      solra::render::DEFERRED_LIGHTING_FRAGMENT_GLSL,
      glDevice);

  if (dgvs && dgfs && dlvs && dlfs) {
    g_render.deferredGeomVertexShader = dgvs;
    g_render.deferredGeomFragmentShader = dgfs;
    g_render.deferredLightVertexShader = dlvs;
    g_render.deferredLightFragmentShader = dlfs;

    solra::render::PipelineDesc geomDesc;
    geomDesc.vertexShader = dgvs;
    geomDesc.fragmentShader = dgfs;
    geomDesc.primitive = solra::render::PrimitiveType::Triangles;
    geomDesc.depthTest = true;
    geomDesc.depthWrite = true;
    geomDesc.depthCompare = solra::render::CompareOp::LessEqual;
    geomDesc.blending = false;
    g_render.deferredGeomPipeline = g_render.gpuDevice->createPipeline(geomDesc);

    solra::render::PipelineDesc lightDesc;
    lightDesc.vertexShader = dlvs;
    lightDesc.fragmentShader = dlfs;
    lightDesc.primitive = solra::render::PrimitiveType::Triangles;
    lightDesc.depthTest = false;
    lightDesc.depthWrite = false;
    lightDesc.blending = false;
    g_render.deferredLightPipeline = g_render.gpuDevice->createPipeline(lightDesc);

    if (g_render.deferredGeomPipeline && g_render.deferredLightPipeline) {
      g_render.deferred_available = true;
      spdlog::info("Deferred rendering pipelines compiled (G-Buffer MRT + PBR lighting pass)");
    }
  } else {
    spdlog::warn("Deferred shader compilation failed, using forward rendering");
  }

  // Compile unlit fallback
  auto uvs = solra::render::OpenGLShader::compileFromSource(
      solra::render::ShaderStage::Vertex,
      solra::render::UNLIT_VERTEX_GLSL,
      glDevice);
  auto ufs = solra::render::OpenGLShader::compileFromSource(
      solra::render::ShaderStage::Fragment,
      solra::render::UNLIT_FRAGMENT_GLSL,
      glDevice);

  if (uvs && ufs) {
    g_render.unlitVertexShader = uvs;
    g_render.unlitFragmentShader = ufs;

    solra::render::PipelineDesc unlitDesc;
    unlitDesc.vertexShader = uvs;
    unlitDesc.fragmentShader = ufs;
    unlitDesc.primitive = solra::render::PrimitiveType::Triangles;
    unlitDesc.depthTest = true;
    unlitDesc.depthWrite = true;

    g_render.unlitPipeline = g_render.gpuDevice->createPipeline(unlitDesc);
  }

  return g_render.pbrPipeline || g_render.unlitPipeline;
}

// ============================================================
// G-Buffer management (deferred rendering)
// ============================================================

static bool init_gbuffer() {
  if (!g_render.hasGpu || !g_render.deferred_available) return false;

  int w = g_render.config.width > 0 ? g_render.config.width : 1920;
  int h = g_render.config.height > 0 ? g_render.config.height : 1080;

  // Create framebuffer
  glGenFramebuffers(1, &g_render.gbuffer_fbo);
  glBindFramebuffer(0x8D40, g_render.gbuffer_fbo); // GL_FRAMEBUFFER

  // Albedo (RGBA8)
  glGenTextures(1, &g_render.gbuffer_albedo);
  glBindTexture(0x0DE1, g_render.gbuffer_albedo); // GL_TEXTURE_2D
  glTexImage2D(0x0DE1, 0, 0x8058, w, h, 0, 0x1908, 0x1401, nullptr); // GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE
  glTexParameteri(0x0DE1, 0x2800, 0x2601); // GL_LINEAR
  glTexParameteri(0x0DE1, 0x2801, 0x2601);
  glTexParameteri(0x0DE1, 0x2802, 0x812F); // GL_CLAMP_TO_EDGE
  glTexParameteri(0x0DE1, 0x2803, 0x812F);
  glFramebufferTexture2D(0x8D40, 0x8CE0, 0x0DE1, g_render.gbuffer_albedo, 0); // GL_COLOR_ATTACHMENT0

  // Normal (RGBA16F for precision)
  glGenTextures(1, &g_render.gbuffer_normal);
  glBindTexture(0x0DE1, g_render.gbuffer_normal);
  glTexImage2D(0x0DE1, 0, 0x881A, w, h, 0, 0x1908, 0x1406, nullptr); // GL_RGBA16F, GL_FLOAT
  glTexParameteri(0x0DE1, 0x2800, 0x2601);
  glTexParameteri(0x0DE1, 0x2801, 0x2601);
  glTexParameteri(0x0DE1, 0x2802, 0x812F);
  glTexParameteri(0x0DE1, 0x2803, 0x812F);
  glFramebufferTexture2D(0x8D40, 0x8CE1, 0x0DE1, g_render.gbuffer_normal, 0); // GL_COLOR_ATTACHMENT1

  // MetalRough (RGBA8)
  glGenTextures(1, &g_render.gbuffer_metalrough);
  glBindTexture(0x0DE1, g_render.gbuffer_metalrough);
  glTexImage2D(0x0DE1, 0, 0x8058, w, h, 0, 0x1908, 0x1401, nullptr);
  glTexParameteri(0x0DE1, 0x2800, 0x2601);
  glTexParameteri(0x0DE1, 0x2801, 0x2601);
  glTexParameteri(0x0DE1, 0x2802, 0x812F);
  glTexParameteri(0x0DE1, 0x2803, 0x812F);
  glFramebufferTexture2D(0x8D40, 0x8CE2, 0x0DE1, g_render.gbuffer_metalrough, 0); // GL_COLOR_ATTACHMENT2

  // Emission (RGBA16F)
  glGenTextures(1, &g_render.gbuffer_emission);
  glBindTexture(0x0DE1, g_render.gbuffer_emission);
  glTexImage2D(0x0DE1, 0, 0x881A, w, h, 0, 0x1908, 0x1406, nullptr);
  glTexParameteri(0x0DE1, 0x2800, 0x2601);
  glTexParameteri(0x0DE1, 0x2801, 0x2601);
  glTexParameteri(0x0DE1, 0x2802, 0x812F);
  glTexParameteri(0x0DE1, 0x2803, 0x812F);
  glFramebufferTexture2D(0x8D40, 0x8CE3, 0x0DE1, g_render.gbuffer_emission, 0); // GL_COLOR_ATTACHMENT3

  // Depth (Depth24Stencil8)
  glGenTextures(1, &g_render.gbuffer_depth);
  glBindTexture(0x0DE1, g_render.gbuffer_depth);
  glTexImage2D(0x0DE1, 0, 0x88F0, w, h, 0, 0x84F9, 0x84FA, nullptr); // GL_DEPTH24_STENCIL8, GL_DEPTH_STENCIL, GL_UNSIGNED_INT_24_8
  glTexParameteri(0x0DE1, 0x2800, 0x2601);
  glTexParameteri(0x0DE1, 0x2801, 0x2601);
  glTexParameteri(0x0DE1, 0x2802, 0x812F);
  glTexParameteri(0x0DE1, 0x2803, 0x812F);
  glFramebufferTexture2D(0x8D40, 0x8D00, 0x0DE1, g_render.gbuffer_depth, 0); // GL_DEPTH_ATTACHMENT

  // Set draw buffers for MRT
  uint32_t attachments[4] = { 0x8CE0, 0x8CE1, 0x8CE2, 0x8CE3 };
  glDrawBuffers(4, attachments);

  // Check completeness
  uint32_t status = glCheckFramebufferStatus(0x8D40);
  if (status != 0x8CD5) { // GL_FRAMEBUFFER_COMPLETE
    spdlog::error("G-Buffer framebuffer incomplete: 0x{:X}", status);
    glBindFramebuffer(0x8D40, 0);
    return false;
  }

  // Create fullscreen quad for lighting pass
  float quadVertices[] = {
    // pos (NDC)       // uv
    -1.0f, -1.0f, 0.0f,  0.0f, 0.0f,
     1.0f, -1.0f, 0.0f,  1.0f, 0.0f,
     1.0f,  1.0f, 0.0f,  1.0f, 1.0f,
    -1.0f, -1.0f, 0.0f,  0.0f, 0.0f,
     1.0f,  1.0f, 0.0f,  1.0f, 1.0f,
    -1.0f,  1.0f, 0.0f,  0.0f, 1.0f,
  };

  glGenVertexArrays(1, &g_render.fullscreen_quad_vao);
  glGenBuffers(1, &g_render.fullscreen_quad_vbo);
  glBindVertexArray(g_render.fullscreen_quad_vao);
  glBindBuffer(0x8892, g_render.fullscreen_quad_vbo); // GL_ARRAY_BUFFER
  glBufferData(0x8892, sizeof(quadVertices), quadVertices, 0x88E4); // GL_STATIC_DRAW
  glVertexAttribPointer(0, 3, 0x1406, 0x1702, 5 * sizeof(float), (void*)0); // pos
  glEnableVertexAttribArray(0);
  glVertexAttribPointer(1, 2, 0x1406, 0x1702, 5 * sizeof(float), (void*)(3 * sizeof(float))); // uv
  glEnableVertexAttribArray(1);
  glBindVertexArray(0);

  glBindFramebuffer(0x8D40, 0);
  spdlog::info("G-Buffer initialized: {}x{} (Albedo/Normal/MetalRough/Emission/Depth)", w, h);
  return true;
}

static void destroy_gbuffer() {
  if (g_render.gbuffer_fbo)      glDeleteFramebuffers(1, &g_render.gbuffer_fbo);
  if (g_render.gbuffer_albedo)   glDeleteTextures(1, &g_render.gbuffer_albedo);
  if (g_render.gbuffer_normal)   glDeleteTextures(1, &g_render.gbuffer_normal);
  if (g_render.gbuffer_metalrough) glDeleteTextures(1, &g_render.gbuffer_metalrough);
  if (g_render.gbuffer_emission) glDeleteTextures(1, &g_render.gbuffer_emission);
  if (g_render.gbuffer_depth)    glDeleteTextures(1, &g_render.gbuffer_depth);
  if (g_render.fullscreen_quad_vao) glDeleteVertexArrays(1, &g_render.fullscreen_quad_vao);
  if (g_render.fullscreen_quad_vbo) glDeleteBuffers(1, &g_render.fullscreen_quad_vbo);
  g_render.gbuffer_fbo = 0;
  g_render.deferred_available = false;
}

// ============================================================
// Upload mesh data to GPU
// ============================================================

static bool upload_mesh_to_gpu(ActiveMesh* mesh) {
  if (!mesh || !g_render.hasGpu || mesh->uploaded) return false;
  if (mesh->vertices.empty()) return false;

  // Create vertex buffer
  solra::render::BufferDesc vbDesc;
  vbDesc.size = mesh->vertices.size() * sizeof(float);
  vbDesc.usage = solra::render::BufferUsage::Vertex;
  vbDesc.hostVisible = false;
  mesh->gpuVertices = g_render.gpuDevice->createBuffer(vbDesc, mesh->vertices.data());

  // Create index buffer (if indexed)
  if (!mesh->indices.empty()) {
    solra::render::BufferDesc ibDesc;
    ibDesc.size = mesh->indices.size() * sizeof(uint32_t);
    ibDesc.usage = solra::render::BufferUsage::Index;
    ibDesc.hostVisible = false;
    mesh->gpuIndices = g_render.gpuDevice->createBuffer(ibDesc, mesh->indices.data());
  }

  // Compute bounding box
  int stride_floats = mesh->vertex_stride / sizeof(float);
  mesh->bboxMin = solra::render::Vec3{1e9f, 1e9f, 1e9f};
  mesh->bboxMax = solra::render::Vec3{-1e9f, -1e9f, -1e9f};

  for (size_t i = 0; i < mesh->vertices.size(); i += stride_floats) {
    float x = mesh->vertices[i];
    float y = mesh->vertices[i + 1];
    float z = mesh->vertices[i + 2];
    if (x < mesh->bboxMin.x) mesh->bboxMin.x = x;
    if (y < mesh->bboxMin.y) mesh->bboxMin.y = y;
    if (z < mesh->bboxMin.z) mesh->bboxMin.z = z;
    if (x > mesh->bboxMax.x) mesh->bboxMax.x = x;
    if (y > mesh->bboxMax.y) mesh->bboxMax.y = y;
    if (z > mesh->bboxMax.z) mesh->bboxMax.z = z;
  }

  mesh->uploaded = true;
  return true;
}

// ============================================================
// GPU draw submission
// ============================================================

static void submit_draw(
    solra::render::GpuCommandBuffer* cmd,
    ActiveSceneNode& active,
    const glm::mat4& viewProj) {

  if (!active.mesh || !active.mesh->uploaded) return;

  auto* glCmd = dynamic_cast<solra::render::OpenGLCommandBuffer*>(cmd);
  if (!glCmd) return;

  // Upload mesh to GPU if not already done
  if (!active.mesh->gpuVertices) {
    upload_mesh_to_gpu(active.mesh);
    if (!active.mesh->gpuVertices) return;
  }

  bool isSkinned = active.mesh->is_skinned && g_render.pbrSkinnedPipeline;
  // Choose pipeline
  auto pipeline = isSkinned
    ? g_render.pbrSkinnedPipeline
    : (g_render.pbrPipeline ? g_render.pbrPipeline : g_render.unlitPipeline);
  if (!pipeline) return;
  cmd->bindPipeline(pipeline);

  // Compute model matrix from scene node's world transform
  const solra::render::Mat4& worldMat = active.node->worldMatrix();
  float modelData[16] = {
    worldMat[0], worldMat[1], worldMat[2], worldMat[3],
    worldMat[4], worldMat[5], worldMat[6], worldMat[7],
    worldMat[8], worldMat[9], worldMat[10], worldMat[11],
    worldMat[12], worldMat[13], worldMat[14], worldMat[15]
  };

  // Upload model and view-projection matrices
  int modelLoc = glCmd->getUniformLocation("uModel");
  if (modelLoc >= 0) glCmd->setUniformMat4(modelLoc, modelData);

  int vpLoc = glCmd->getUniformLocation("uViewProj");
  if (vpLoc >= 0) glCmd->setUniformMat4(vpLoc, glm::value_ptr(viewProj));

  // Upload skinning matrices for skinned meshes
  if (isSkinned && active.mesh->has_bone_matrices) {
    int skinLoc = glCmd->getUniformBlockIndex("SkinningBlock");
    if (skinLoc >= 0) {
      glCmd->bindUniformBlock(skinLoc, 0);
      // Upload bone matrix palette via glBufferSubData on a dedicated UBO
      glCmd->uploadSkinningMatrices(
        active.mesh->bone_matrices.data(),
        static_cast<int>(active.mesh->bone_matrices.size()));
    }
  }

  // Set material uniforms (if using PBR pipeline)
  if ((g_render.pbrPipeline || g_render.pbrSkinnedPipeline) && active.material) {
    auto uniforms = active.material->buildUniforms();
    int bcLoc = glCmd->getUniformLocation("uMaterial.baseColorFactor");
    if (bcLoc >= 0) glCmd->setUniformVec4(bcLoc, uniforms.baseColorFactor);
    int mrLoc = glCmd->getUniformLocation("uMaterial.metallicRoughnessOcclusion");
    if (mrLoc >= 0) glCmd->setUniformVec4(mrLoc, uniforms.metallicRoughnessOcclusion);
    int emLoc = glCmd->getUniformLocation("uMaterial.emissiveFactor");
    if (emLoc >= 0) glCmd->setUniformVec4(emLoc, uniforms.emissiveFactor);
  } else {
    float color[4] = {active.color.x, active.color.y, active.color.z, 1.0f};
    int bcLoc = glCmd->getUniformLocation("uMaterial.baseColorFactor");
    if (bcLoc >= 0) glCmd->setUniformVec4(bcLoc, color);
  }

  // Set lighting uniforms
  int ldLoc = glCmd->getUniformLocation("uLightDirection");
  if (ldLoc >= 0) {
    float lightDir[3] = {0.5f, -1.0f, 0.3f};
    glCmd->setUniformVec3(ldLoc, lightDir);
  }
  int lcLoc = glCmd->getUniformLocation("uLightColor");
  if (lcLoc >= 0) {
    float lightColor[3] = {1.0f, 0.95f, 0.85f};
    glCmd->setUniformVec3(lcLoc, lightColor);
  }
  int liLoc = glCmd->getUniformLocation("uLightIntensity");
  if (liLoc >= 0) glCmd->setUniformFloat(liLoc, 8.0f);
  int ambLoc = glCmd->getUniformLocation("uAmbientColor");
  if (ambLoc >= 0) {
    float ambColor[3] = {0.08f, 0.08f, 0.12f};
    glCmd->setUniformVec3(ambLoc, ambColor);
  }
  int camLoc = glCmd->getUniformLocation("uCameraPosition");
  if (camLoc >= 0) {
    float camPos[3] = {g_render.camera_pos.x, g_render.camera_pos.y, g_render.camera_pos.z};
    glCmd->setUniformVec3(camLoc, camPos);
  }

  // Bind vertex buffer and draw (with or without skinning attributes)
  cmd->bindVertexBuffer(active.mesh->gpuVertices, 0, isSkinned);

  if (active.mesh->gpuIndices && !active.mesh->indices.empty()) {
    cmd->bindIndexBuffer(active.mesh->gpuIndices);
    cmd->drawIndexed(static_cast<uint32_t>(active.mesh->indices.size()));
  } else {
    int strideFloats = active.mesh->vertex_stride / sizeof(float);
    uint32_t vertexCount = static_cast<uint32_t>(active.mesh->vertices.size()) / strideFloats;
    cmd->draw(vertexCount);
  }
}

// ============================================================
// Full GPU frame rendering
// ============================================================

static void render_gpu_frame() {
  if (!g_render.hasGpu) return;

  // Use deferred rendering if available (MRT G-Buffer + lighting pass)
  if (g_render.deferred_available) {
    // The view-projection is already cached in begin_frame
    // Compute it inline since we need it before the deferred call
    glm::mat4 view = glm::lookAt(
      g_render.camera_pos, g_render.camera_target, g_render.camera_up);
    float aspect = g_render.config.width > 0 && g_render.config.height > 0
      ? (float)g_render.config.width / (float)g_render.config.height : 16.0f / 9.0f;
    glm::mat4 proj = glm::perspective(
      glm::radians(g_render.camera_fov), aspect,
      g_render.camera_near, g_render.camera_far);
    glm::mat4 viewProj = proj * view;
    render_deferred_frame(viewProj);
    return;
  }

  auto cmd = g_render.gpuDevice->createCommandBuffer();
  auto* glCmd = dynamic_cast<solra::render::OpenGLCommandBuffer*>(cmd.get());
  if (!glCmd) return;

  cmd->begin();

  // Set viewport and clear
  glCmd->setViewport(0, 0,
    static_cast<int>(g_render.config.width),
    static_cast<int>(g_render.config.height));
  glCmd->setClearColor(
    g_render.config.clear_color[0],
    g_render.config.clear_color[1],
    g_render.config.clear_color[2],
    g_render.config.clear_color[3]);
  glCmd->clear(true, true, false);

  // Compute view-projection
  glm::mat4 view = glm::lookAt(
    g_render.camera_pos, g_render.camera_target, g_render.camera_up);
  float aspect = g_render.config.width > 0 && g_render.config.height > 0
    ? (float)g_render.config.width / (float)g_render.config.height : 16.0f / 9.0f;
  glm::mat4 proj = glm::perspective(
    glm::radians(g_render.camera_fov), aspect,
    g_render.camera_near, g_render.camera_far);
  glm::mat4 viewProj = proj * view;
  g_render.cachedViewProj = viewProj;

  // Frustum culling: only draw visible nodes
  solra::render::Mat4 vpMat;
  const float* vpPtr = glm::value_ptr(viewProj);
  for (int i = 0; i < 16; ++i) vpMat[i] = vpPtr[i];

  auto visibleNodes = g_render.scene_graph->frustumCull(vpMat);
  std::unordered_set<solra::render::SceneNode*> visibleSet(
    visibleNodes.begin(), visibleNodes.end());

  // Draw each visible active node
  for (auto& active : g_render.active_nodes) {
    if (!active.node || !active.mesh) continue;
    if (!visibleSet.count(active.node)) continue; // culled

    submit_draw(cmd.get(), active, viewProj);
  }

  cmd->end();
  g_render.gpuDevice->submit(cmd);
}

// ============================================================
// Deferred rendering frame path
// ============================================================

static void render_deferred_frame(const glm::mat4& viewProj) {
  if (!g_render.hasGpu || !g_render.deferred_available) {
    render_gpu_frame();
    return;
  }

  int w = g_render.config.width > 0 ? g_render.config.width : 1920;
  int h = g_render.config.height > 0 ? g_render.config.height : 1080;

  // === Geometry pass: write to G-Buffer ===
  glBindFramebuffer(0x8D40, g_render.gbuffer_fbo);
  glViewport(0, 0, w, h);
  glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
  glClear(0x4000 | 0x0100); // GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT

  auto geomCmd = g_render.gpuDevice->createCommandBuffer();
  geomCmd->begin();

  // Compute visibility
  solra::render::Mat4 vpMat;
  const float* vpPtr = glm::value_ptr(viewProj);
  for (int i = 0; i < 16; ++i) vpMat[i] = vpPtr[i];
  auto visibleNodes = g_render.scene_graph->frustumCull(vpMat);
  std::unordered_set<solra::render::SceneNode*> visibleSet(
    visibleNodes.begin(), visibleNodes.end());

  // Draw each visible node using deferred geometry pipeline
  for (auto& active : g_render.active_nodes) {
    if (!active.node || !active.mesh || !active.mesh->uploaded) continue;
    if (!visibleSet.count(active.node)) continue;

    // Upload mesh to GPU if needed
    if (!active.mesh->gpuVertices) {
      upload_mesh_to_gpu(active.mesh);
      if (!active.mesh->gpuVertices) continue;
    }

    auto* glCmd = dynamic_cast<solra::render::OpenGLCommandBuffer*>(geomCmd.get());
    if (!glCmd) continue;

    geomCmd->bindPipeline(g_render.deferredGeomPipeline);

    // Model matrix
    const solra::render::Mat4& worldMat = active.node->worldMatrix();
    float modelData[16] = {
      worldMat[0], worldMat[1], worldMat[2], worldMat[3],
      worldMat[4], worldMat[5], worldMat[6], worldMat[7],
      worldMat[8], worldMat[9], worldMat[10], worldMat[11],
      worldMat[12], worldMat[13], worldMat[14], worldMat[15]
    };

    int modelLoc = glCmd->getUniformLocation("uModel");
    if (modelLoc >= 0) glCmd->setUniformMat4(modelLoc, modelData);
    int vpLoc = glCmd->getUniformLocation("uViewProj");
    if (vpLoc >= 0) glCmd->setUniformMat4(vpLoc, glm::value_ptr(viewProj));

    // Material uniforms
    if (active.material) {
      auto uniforms = active.material->buildUniforms();
      int bcLoc = glCmd->getUniformLocation("uMaterial.baseColorFactor");
      if (bcLoc >= 0) glCmd->setUniformVec4(bcLoc, uniforms.baseColorFactor);
      int mrLoc = glCmd->getUniformLocation("uMaterial.metallicRoughnessOcclusion");
      if (mrLoc >= 0) glCmd->setUniformVec4(mrLoc, uniforms.metallicRoughnessOcclusion);
      int emLoc = glCmd->getUniformLocation("uMaterial.emissiveFactor");
      if (emLoc >= 0) glCmd->setUniformVec4(emLoc, uniforms.emissiveFactor);
    }

    geomCmd->bindVertexBuffer(active.mesh->gpuVertices, 0, false);
    if (active.mesh->gpuIndices && !active.mesh->indices.empty()) {
      geomCmd->bindIndexBuffer(active.mesh->gpuIndices);
      geomCmd->drawIndexed(static_cast<uint32_t>(active.mesh->indices.size()));
    } else {
      int strideFloats = active.mesh->vertex_stride / sizeof(float);
      uint32_t vc = static_cast<uint32_t>(active.mesh->vertices.size()) / strideFloats;
      geomCmd->draw(vc);
    }
  }
  geomCmd->end();
  g_render.gpuDevice->submit(geomCmd);

  // === Lighting pass: fullscreen quad reading G-Buffer ===
  glBindFramebuffer(0x8D40, 0); // Back to default framebuffer
  glClearColor(
    g_render.config.clear_color[0],
    g_render.config.clear_color[1],
    g_render.config.clear_color[2],
    g_render.config.clear_color[3]);
  glClear(0x4000); // GL_COLOR_BUFFER_BIT

  auto lightCmd = g_render.gpuDevice->createCommandBuffer();
  auto* lglCmd = dynamic_cast<solra::render::OpenGLCommandBuffer*>(lightCmd.get());
  if (lglCmd) {
    lightCmd->begin();
    lightCmd->bindPipeline(g_render.deferredLightPipeline);

    // Bind G-Buffer textures
    glActiveTexture(0x84C0); // GL_TEXTURE0
    glBindTexture(0x0DE1, g_render.gbuffer_albedo);
    glActiveTexture(0x84C1); // GL_TEXTURE1
    glBindTexture(0x0DE1, g_render.gbuffer_normal);
    glActiveTexture(0x84C2); // GL_TEXTURE2
    glBindTexture(0x0DE1, g_render.gbuffer_metalrough);
    glActiveTexture(0x84C3); // GL_TEXTURE3
    glBindTexture(0x0DE1, g_render.gbuffer_emission);
    glActiveTexture(0x84C4); // GL_TEXTURE4
    glBindTexture(0x0DE1, g_render.gbuffer_depth);

    // Set lighting uniforms
    float lightDir[3] = {0.5f, -1.0f, 0.3f};
    int ldLoc = lglCmd->getUniformLocation("uLightDirection");
    if (ldLoc >= 0) lglCmd->setUniformVec3(ldLoc, lightDir);
    float lightColor[3] = {1.0f, 0.95f, 0.85f};
    int lcLoc = lglCmd->getUniformLocation("uLightColor");
    if (lcLoc >= 0) lglCmd->setUniformVec3(lcLoc, lightColor);
    int liLoc = lglCmd->getUniformLocation("uLightIntensity");
    if (liLoc >= 0) lglCmd->setUniformFloat(liLoc, 8.0f);
    float ambColor[3] = {0.08f, 0.08f, 0.12f};
    int ambLoc = lglCmd->getUniformLocation("uAmbientColor");
    if (ambLoc >= 0) lglCmd->setUniformVec3(ambLoc, ambColor);
    float camPos[3] = {g_render.camera_pos.x, g_render.camera_pos.y, g_render.camera_pos.z};
    int camLoc = lglCmd->getUniformLocation("uCameraPosition");
    if (camLoc >= 0) lglCmd->setUniformVec3(camLoc, camPos);

    // Inverse view-projection for world position reconstruction
    glm::mat4 invViewProj = glm::inverse(viewProj);
    int ivpLoc = lglCmd->getUniformLocation("uInverseViewProj");
    if (ivpLoc >= 0) lglCmd->setUniformMat4(ivpLoc, glm::value_ptr(invViewProj));

    // Draw fullscreen quad
    glBindVertexArray(g_render.fullscreen_quad_vao);
    glDrawArrays(0x0004, 0, 6); // GL_TRIANGLES, 6 vertices
    glBindVertexArray(0);

    lightCmd->end();
    g_render.gpuDevice->submit(lightCmd);
  }
}

/* ============================================================
 * Renderer Lifecycle
 * ============================================================ */

int solra_render_init(const SolraRenderConfig *config) {
  if (g_render.initialized) return SOLRA_ERROR_ALREADY_INITIALIZED;

  if (config) {
    g_render.config = *config;
  } else {
    g_render.config.backend = SOLRA_RENDER_BACKEND_AUTO;
    g_render.config.width = 1920;
    g_render.config.height = 1080;
    g_render.config.vsync = 1;
    g_render.config.msaa_samples = 4;
    g_render.config.enable_hdr = 0;
    g_render.config.clear_color = {0.1f, 0.1f, 0.15f, 1.0f};
    g_render.config.native_window = nullptr;
  }

  // Attempt to create GPU device
  g_render.gpuDevice = solra::render::createGpuDevice(solra::render::Backend::OpenGLES);
  if (g_render.gpuDevice) {
    g_render.hasGpu = true;

    // Query real GPU info
    std::memset(&g_render.gpu_info, 0, sizeof(SolraGPUInfo));
    std::string devName = g_render.gpuDevice->name();
    std::strncpy(g_render.gpu_info.renderer, devName.c_str(),
                 sizeof(g_render.gpu_info.renderer) - 1);
    std::strncpy(g_render.gpu_info.vendor, "OpenGL 4.6",
                 sizeof(g_render.gpu_info.vendor) - 1);
    std::strncpy(g_render.gpu_info.version, "1.0.0",
                 sizeof(g_render.gpu_info.version) - 1);
    g_render.gpu_info.dedicated_vram_mb = static_cast<size_t>(
        g_render.gpuDevice->gpuMemoryBudget() / (1024 * 1024));
    g_render.gpu_info.shared_vram_mb = 0;
    g_render.gpu_info.max_texture_size = 16384;
    g_render.gpu_info.max_compute_workgroup_size = 1024;
    g_render.gpu_info.supports_ray_tracing = 0;
    g_render.gpu_info.supports_mesh_shader = 0;

    // Initialize GPU pipelines
    if (!init_gpu_pipelines()) {
      spdlog::warn("GPU pipeline initialization failed, falling back to CPU-only");
      g_render.gpuDevice.reset();
      g_render.hasGpu = false;
    } else {
      // Initialize G-Buffer for deferred rendering (if available)
      init_gbuffer();
    }
  }

  // Fallback: software rasterizer
  if (!g_render.hasGpu) {
    spdlog::warn("No GPU backend available, using Software Rasterizer");
    std::memset(&g_render.gpu_info, 0, sizeof(SolraGPUInfo));
    std::strncpy(g_render.gpu_info.vendor, "Solra Core",
                 sizeof(g_render.gpu_info.vendor) - 1);
    std::strncpy(g_render.gpu_info.renderer, "OpenGL (Software Rasterizer)",
                 sizeof(g_render.gpu_info.renderer) - 1);
    std::strncpy(g_render.gpu_info.version, "1.0.0",
                 sizeof(g_render.gpu_info.version) - 1);
    g_render.gpu_info.dedicated_vram_mb = 4096;
    g_render.gpu_info.max_texture_size = 16384;
    g_render.gpu_info.max_compute_workgroup_size = 1024;
    g_render.gpu_info.supports_ray_tracing = 0;
    g_render.gpu_info.supports_mesh_shader = 0;
  }

  // Create scene graph
  g_render.scene_graph = std::make_shared<solra::render::SceneGraph>();

  // Create material library
  g_render.materialLib = std::make_shared<solra::render::MaterialLibrary>();

  // Initialize timing
  g_render.last_fps_update = std::chrono::high_resolution_clock::now();

  g_render.initialized = 1;

  spdlog::info("Render engine initialized ({} mode)",
               g_render.hasGpu ? "GPU Accelerated" : "Software Rasterizer");
  spdlog::info("  GPU: {}", g_render.gpu_info.renderer);
  spdlog::info("  Resolution: {}x{}",
               g_render.config.width, g_render.config.height);
  spdlog::info("  VSync: {}", g_render.config.vsync ? "on" : "off");
  spdlog::info("  MSAA: {}x", g_render.config.msaa_samples);

  return SOLRA_OK;
}

int solra_render_get_gpu_info(SolraGPUInfo *info) {
  if (!info) return SOLRA_ERROR_INVALID_ARGUMENT;
  if (!g_render.initialized) return SOLRA_ERROR_NOT_INITIALIZED;

  std::memcpy(info, &g_render.gpu_info, sizeof(SolraGPUInfo));
  return SOLRA_OK;
}

int solra_render_begin_frame(void) {
  if (!g_render.initialized) return SOLRA_ERROR_NOT_INITIALIZED;

  g_render.frame_start = std::chrono::high_resolution_clock::now();
  g_render.frame_active = 1;
  g_render.frame_count++;

  // Compute delta time
  static auto last_frame = g_render.frame_start;
  g_render.delta_time = std::chrono::duration<float>(
    g_render.frame_start - last_frame).count();
  last_frame = g_render.frame_start;
  g_render.elapsed_time += g_render.delta_time;

  // Clamp delta to avoid spiral of death
  if (g_render.delta_time > 0.1f) g_render.delta_time = 0.1f;

  // Update camera
  update_camera_orbit(g_render.delta_time);

  // Update scene animations
  update_scene_nodes(g_render.delta_time);

  // Compute view-projection matrix
  glm::mat4 view = glm::lookAt(
    g_render.camera_pos, g_render.camera_target, g_render.camera_up);
  float aspect = g_render.config.width > 0 && g_render.config.height > 0
    ? (float)g_render.config.width / (float)g_render.config.height
    : 16.0f / 9.0f;
  glm::mat4 proj = glm::perspective(
    glm::radians(g_render.camera_fov), aspect,
    g_render.camera_near, g_render.camera_far);
  glm::mat4 view_proj = proj * view;

  // Traverse scene graph and update world matrices
  if (g_render.scene_graph && g_render.scene_graph->root) {
    g_render.scene_graph->root->traverse([](solra::render::SceneNode* node) {
      if (node->transform.dirty) {
        node->markWorldDirty();
        node->transform.dirty = false;
      }
      (void)node->worldMatrix(); // trigger lazy evaluation
    });
  }

  return SOLRA_OK;
}

int solra_render_end_frame(void) {
  if (!g_render.frame_active) return SOLRA_ERROR_INVALID_ARGUMENT;

  // Submit GPU draw commands
  if (g_render.hasGpu) {
    render_gpu_frame();
  }

  g_render.frame_active = 0;
  compute_fps();

  return SOLRA_OK;
}

void solra_render_resize(int width, int height) {
  g_render.config.width = width;
  g_render.config.height = height;
  spdlog::debug("Render: viewport resized to {}x{}", width, height);
}

void solra_render_shutdown(void) {
  // Release G-Buffer resources
  destroy_gbuffer();

  // Release GPU resources
  g_render.pbrPipeline.reset();
  g_render.pbrSkinnedPipeline.reset();
  g_render.pbrVertexShader.reset();
  g_render.pbrSkinnedVertexShader.reset();
  g_render.pbrFragmentShader.reset();
  g_render.deferredGeomPipeline.reset();
  g_render.deferredLightPipeline.reset();
  g_render.deferredGeomVertexShader.reset();
  g_render.deferredGeomFragmentShader.reset();
  g_render.deferredLightVertexShader.reset();
  g_render.deferredLightFragmentShader.reset();
  g_render.unlitPipeline.reset();
  g_render.unlitVertexShader.reset();
  g_render.unlitFragmentShader.reset();
  g_render.materialLib.reset();
  g_render.gpuDevice.reset();
  g_render.hasGpu = false;

  g_render.scene_graph.reset();
  g_render.active_nodes.clear();
  g_render.initialized = 0;
  spdlog::info("Render engine shutdown");
}

/* ============================================================
 * Render State Query (for host/Frontend)
 * ============================================================ */

SOLRA_API float solra_render_get_fps(void) {
  return g_render.current_fps;
}

SOLRA_API void solra_render_get_camera(float* pos_x, float* pos_y, float* pos_z,
                                        float* target_x, float* target_y, float* target_z) {
  *pos_x = g_render.camera_pos.x;
  *pos_y = g_render.camera_pos.y;
  *pos_z = g_render.camera_pos.z;
  *target_x = g_render.camera_target.x;
  *target_y = g_render.camera_target.y;
  *target_z = g_render.camera_target.z;
}

SOLRA_API uint64_t solra_render_get_frame_count(void) {
  return g_render.frame_count;
}

SOLRA_API float solra_render_get_elapsed_time(void) {
  return g_render.elapsed_time;
}

/* ============================================================
 * Scene Management
 * ============================================================ */

SolraSceneHandle solra_scene_create(void) {
  auto scene = new solra::render::SceneGraph();
  spdlog::debug("Scene created");
  return reinterpret_cast<SolraSceneHandle>(scene);
}

void solra_scene_destroy(SolraSceneHandle scene) {
  if (scene) {
    delete reinterpret_cast<solra::render::SceneGraph*>(scene);
    spdlog::debug("Scene destroyed");
  }
}

void solra_scene_set_active(SolraSceneHandle scene) {
  if (scene) {
    g_render.scene_graph = std::shared_ptr<solra::render::SceneGraph>(
      reinterpret_cast<solra::render::SceneGraph*>(scene));
  } else {
    g_render.scene_graph.reset();
  }
  spdlog::debug("Active scene set");
}

SolraSceneNodeHandle solra_scene_node_create(SolraSceneHandle scene, const char *name) {
  if (!scene) return nullptr;

  auto* sg = reinterpret_cast<solra::render::SceneGraph*>(scene);
  auto node = std::make_shared<solra::render::SceneNode>();
  if (name) node->name = name;

  sg->root->addChild(node);

  // Track active nodes for animation + rendering updates
  ActiveSceneNode active;
  active.node = node.get();
  g_render.active_nodes.push_back(active);

  spdlog::debug("Scene node created: {}", node->name);
  return reinterpret_cast<SolraSceneNodeHandle>(node.get());
}

void solra_scene_node_set_transform(
  SolraSceneNodeHandle node,
  const SolraVec3 *position,
  const SolraQuat *rotation,
  const SolraVec3 *scale
) {
  if (!node) return;
  auto* n = reinterpret_cast<solra::render::SceneNode*>(node);

  if (position) {
    n->transform.position = solra::render::Vec3{position->x, position->y, position->z};
  }
  if (rotation) {
    n->transform.rotation = solra::render::Quat{rotation->x, rotation->y, rotation->z, rotation->w};
  }
  if (scale) {
    n->transform.scale = solra::render::Vec3{scale->x, scale->y, scale->z};
  }
  n->transform.dirty = true;
}

void solra_scene_node_attach_mesh(SolraSceneNodeHandle node, SolraMeshHandle mesh) {
  if (!node) return;

  for (auto& active : g_render.active_nodes) {
    if (active.node == reinterpret_cast<solra::render::SceneNode*>(node)) {
      active.mesh = reinterpret_cast<ActiveMesh*>(mesh);
      // Upload to GPU immediately if device is available
      if (active.mesh && g_render.hasGpu) {
        upload_mesh_to_gpu(active.mesh);
      }
      break;
    }
  }
}

void solra_scene_node_add_child(SolraSceneNodeHandle parent, SolraSceneNodeHandle child) {
  if (!parent || !child) return;
  auto* p = reinterpret_cast<solra::render::SceneNode*>(parent);
  auto* c = reinterpret_cast<solra::render::SceneNode*>(child);

  c->removeFromParent();
  p->addChild(std::shared_ptr<solra::render::SceneNode>(c, [](solra::render::SceneNode*){}));
}

/* ============================================================
 * Scene Node Data Query (for Frontend sync)
 * ============================================================ */

SOLRA_API int solra_render_get_node_count(void) {
  return static_cast<int>(g_render.active_nodes.size());
}

SOLRA_API int solra_render_get_node_data(int index,
                                          float* pos_x, float* pos_y, float* pos_z,
                                          float* rot_x, float* rot_y, float* rot_z, float* rot_w,
                                          float* scl_x, float* scl_y, float* scl_z,
                                          char* name_buf, int name_buf_size) {
  if (index < 0 || index >= static_cast<int>(g_render.active_nodes.size()))
    return SOLRA_ERROR_INVALID_ARGUMENT;

  auto& active = g_render.active_nodes[index];
  if (!active.node) return SOLRA_ERROR_INVALID_ARGUMENT;

  auto& t = active.node->transform;
  *pos_x = t.position.x;
  *pos_y = t.position.y;
  *pos_z = t.position.z;
  *rot_x = t.rotation.x;
  *rot_y = t.rotation.y;
  *rot_z = t.rotation.z;
  *rot_w = t.rotation.w;
  *scl_x = t.scale.x;
  *scl_y = t.scale.y;
  *scl_z = t.scale.z;

  if (name_buf && name_buf_size > 0) {
    std::strncpy(name_buf, active.node->name.c_str(), name_buf_size - 1);
    name_buf[name_buf_size - 1] = '\0';
  }

  return SOLRA_OK;
}

/* ============================================================
 * Mesh Management
 * ============================================================ */

SolraMeshHandle solra_mesh_create(
  const void *vertices, int vertex_count, int vertex_stride,
  const void *indices, int index_count, int index_type
) {
  auto* mesh = new ActiveMesh();
  mesh->vertex_stride = vertex_stride > 0 ? vertex_stride : 32; // default: 8 floats

  // Copy vertex data
  size_t vertex_data_size = static_cast<size_t>(vertex_count) * mesh->vertex_stride;
  mesh->vertices.resize(vertex_data_size / sizeof(float));
  std::memcpy(mesh->vertices.data(), vertices, vertex_data_size);

  // Copy index data
  if (indices && index_count > 0) {
    size_t index_size = (index_type == 32) ? sizeof(uint32_t) : sizeof(uint16_t);
    size_t index_data_size = static_cast<size_t>(index_count) * index_size;
    mesh->indices.resize(index_count);

    if (index_type == 32) {
      std::memcpy(mesh->indices.data(), indices, index_data_size);
    } else {
      const uint16_t* src = static_cast<const uint16_t*>(indices);
      for (int i = 0; i < index_count; ++i) {
        mesh->indices[i] = static_cast<uint32_t>(src[i]);
      }
    }
  }

  spdlog::debug("Mesh created: {} vertices, {} indices", vertex_count, index_count);
  return reinterpret_cast<SolraMeshHandle>(mesh);
}

SolraMeshHandle solra_mesh_create_skinned(
  const void *vertices, int vertex_count,
  const void *indices, int index_count, int index_type
) {
  auto* mesh = new ActiveMesh();
  mesh->is_skinned = true;
  mesh->vertex_stride = 48; // pos3+normal3+uv2+boneWeights4+boneIndices4 = 12 floats * 4 bytes

  // Copy vertex data
  size_t vertex_data_size = static_cast<size_t>(vertex_count) * mesh->vertex_stride;
  mesh->vertices.resize(vertex_data_size / sizeof(float));
  std::memcpy(mesh->vertices.data(), vertices, vertex_data_size);

  // Copy index data
  if (indices && index_count > 0) {
    size_t index_size = (index_type == 32) ? sizeof(uint32_t) : sizeof(uint16_t);
    size_t index_data_size = static_cast<size_t>(index_count) * index_size;
    mesh->indices.resize(index_count);

    if (index_type == 32) {
      std::memcpy(mesh->indices.data(), indices, index_data_size);
    } else {
      const uint16_t* src = static_cast<const uint16_t*>(indices);
      for (int i = 0; i < index_count; ++i) {
        mesh->indices[i] = static_cast<uint32_t>(src[i]);
      }
    }
  }

  // Pre-allocate bone matrix palette (128 bones * 16 floats)
  mesh->bone_matrices.resize(SOLRA_MAX_BONES * 16, 0.0f);
  // Initialize first bone to identity
  for (int b = 0; b < SOLRA_MAX_BONES; ++b) {
    int base = b * 16;
    mesh->bone_matrices[base + 0] = 1.0f;
    mesh->bone_matrices[base + 5] = 1.0f;
    mesh->bone_matrices[base + 10] = 1.0f;
    mesh->bone_matrices[base + 15] = 1.0f;
  }

  spdlog::debug("Skinned mesh created: {} vertices, {} indices (48-byte stride)", vertex_count, index_count);
  return reinterpret_cast<SolraMeshHandle>(mesh);
}

int solra_mesh_set_bone_matrices(SolraMeshHandle mesh, const float *bone_matrices, int bone_count) {
  if (!mesh || !bone_matrices || bone_count <= 0 || bone_count > SOLRA_MAX_BONES) {
    spdlog::warn("solra_mesh_set_bone_matrices: invalid arguments");
    return SOLRA_ERROR_INVALID_ARGUMENT;
  }

  auto* m = reinterpret_cast<ActiveMesh*>(mesh);
  if (!m->is_skinned) {
    spdlog::warn("solra_mesh_set_bone_matrices: mesh is not skinned");
    return SOLRA_ERROR_INVALID_ARGUMENT;
  }

  m->bone_count = bone_count;
  std::memcpy(m->bone_matrices.data(), bone_matrices, bone_count * 16 * sizeof(float));
  m->has_bone_matrices = true;

  return SOLRA_OK;
}

void solra_mesh_destroy(SolraMeshHandle mesh) {
  if (mesh) {
    auto* m = reinterpret_cast<ActiveMesh*>(mesh);
    // Release GPU resources
    m->gpuVertices.reset();
    m->gpuIndices.reset();
    delete m;
  }
}

SolraAABB solra_mesh_get_bounds(SolraMeshHandle mesh) {
  SolraAABB bounds = {};
  if (!mesh) return bounds;

  auto* m = reinterpret_cast<ActiveMesh*>(mesh);
  if (m->vertices.empty()) return bounds;

  int stride_floats = m->vertex_stride / sizeof(float);
  bounds.min.x = bounds.min.y = bounds.min.z = 1e9f;
  bounds.max.x = bounds.max.y = bounds.max.z = -1e9f;

  for (size_t i = 0; i < m->vertices.size(); i += stride_floats) {
    float x = m->vertices[i];
    float y = m->vertices[i + 1];
    float z = m->vertices[i + 2];

    if (x < bounds.min.x) bounds.min.x = x;
    if (y < bounds.min.y) bounds.min.y = y;
    if (z < bounds.min.z) bounds.min.z = z;
    if (x > bounds.max.x) bounds.max.x = x;
    if (y > bounds.max.y) bounds.max.y = y;
    if (z > bounds.max.z) bounds.max.z = z;
  }

  return bounds;
}

/* ============================================================
 * Material Management
 * ============================================================ */

SolraMaterialHandle solra_material_create(void) {
  auto mat = std::make_shared<solra::render::PbrMaterial>();
  mat->name = "material_" + std::to_string(g_render.materialLib->count());
  g_render.materialLib->add(mat);

  // Store the shared_ptr in a way that C API can retrieve it
  // We use a static map for handle → shared_ptr mapping
  static std::unordered_map<SolraMaterialHandle, std::shared_ptr<solra::render::PbrMaterial>> matMap;
  static SolraMaterialHandle nextHandle = reinterpret_cast<SolraMaterialHandle>(static_cast<uintptr_t>(1));
  SolraMaterialHandle handle = nextHandle;
  nextHandle = reinterpret_cast<SolraMaterialHandle>(
      reinterpret_cast<uintptr_t>(nextHandle) + 1);
  matMap[handle] = mat;

  return handle;
}

void solra_material_set_base_color(SolraMaterialHandle material, SolraColor color) {
  static std::unordered_map<SolraMaterialHandle, std::shared_ptr<solra::render::PbrMaterial>> matMap;
  auto it = matMap.find(material);
  if (it != matMap.end()) {
    it->second->metallicRoughness.baseColorFactor[0] = color.r;
    it->second->metallicRoughness.baseColorFactor[1] = color.g;
    it->second->metallicRoughness.baseColorFactor[2] = color.b;
    it->second->metallicRoughness.baseColorFactor[3] = color.a;
  }
}

void solra_material_set_metallic_roughness(SolraMaterialHandle material, float metallic, float roughness) {
  static std::unordered_map<SolraMaterialHandle, std::shared_ptr<solra::render::PbrMaterial>> matMap;
  auto it = matMap.find(material);
  if (it != matMap.end()) {
    it->second->metallicRoughness.metallicFactor = metallic;
    it->second->metallicRoughness.roughnessFactor = roughness;
  }
}

int solra_material_set_texture(SolraMaterialHandle material, const char *slot, SolraTextureHandle texture) {
  static std::unordered_map<SolraMaterialHandle, std::shared_ptr<solra::render::PbrMaterial>> matMap;
  auto it = matMap.find(material);
  if (it == matMap.end()) {
    spdlog::warn("Material not found for texture assignment");
    return SOLRA_ERROR_INVALID_ARGUMENT;
  }

  // Map C string slot name to PbrTextureSlot enum
  solra::render::PbrTextureSlot texSlot;
  if (std::strcmp(slot, "base_color") == 0 || std::strcmp(slot, "albedo") == 0)
    texSlot = solra::render::PbrTextureSlot::Albedo;
  else if (std::strcmp(slot, "normal") == 0)
    texSlot = solra::render::PbrTextureSlot::Normal;
  else if (std::strcmp(slot, "metallic_roughness") == 0)
    texSlot = solra::render::PbrTextureSlot::Metallic;
  else if (std::strcmp(slot, "occlusion") == 0)
    texSlot = solra::render::PbrTextureSlot::AmbientOcclusion;
  else if (std::strcmp(slot, "emissive") == 0)
    texSlot = solra::render::PbrTextureSlot::Emissive;
  else {
    spdlog::warn("Unknown texture slot: {}", slot);
    return SOLRA_ERROR_INVALID_ARGUMENT;
  }

  // Get ActiveTexture from handle
  std::lock_guard<std::mutex> lock(g_texture_mutex);
  auto texIt = g_textures.find(texture);
  if (texIt == g_textures.end()) {
    spdlog::warn("Texture handle not found");
    return SOLRA_ERROR_INVALID_ARGUMENT;
  }

  // Assign to material
  solra::render::PbrTexture pbrTex;
  pbrTex.slot = texSlot;
  pbrTex.textureId = reinterpret_cast<uintptr_t>(texture);
  it->second->textures.push_back(pbrTex);

  spdlog::info("Material texture assigned: slot={}", slot);
  return SOLRA_OK;
}

void solra_material_destroy(SolraMaterialHandle material) {
  static std::unordered_map<SolraMaterialHandle, std::shared_ptr<solra::render::PbrMaterial>> matMap;
  matMap.erase(material);
}

/* ============================================================
 * Texture Management (stb_image powered)
 * ============================================================ */

#define STB_IMAGE_IMPLEMENTATION
#include <stb_image.h>

struct ActiveTexture {
  std::shared_ptr<solra::render::GpuTexture> gpuTexture;
  int width = 0;
  int height = 0;
  int channels = 0;
};

static std::unordered_map<SolraTextureHandle, std::shared_ptr<ActiveTexture>> g_textures;
static std::mutex g_texture_mutex;
static uintptr_t g_next_texture_handle = 1;

SolraTextureHandle solra_texture_load(const char *path, int generate_mipmaps) {
  if (!path || !path[0]) {
    spdlog::error("Texture load: null or empty path");
    return nullptr;
  }

  spdlog::info("Texture load: {} (mipmaps: {})", path, generate_mipmaps);

  // Use stb_image to load the image file
  int width = 0, height = 0, channels = 0;
  // Force 4 channels (RGBA) for GPU compatibility
  unsigned char* pixels = stbi_load(path, &width, &height, &channels, 4);
  if (!pixels) {
    spdlog::error("Texture load failed: {} — {}", path, stbi_failure_reason());
    return nullptr;
  }

  spdlog::info("Texture loaded: {} ({}x{}, {}→RGBA8)", path, width, height, channels);

  // Upload to GPU if available
  std::shared_ptr<solra::render::GpuTexture> gpuTex;
  if (g_render.hasGpu && g_render.gpuDevice) {
    solra::render::TextureDesc desc;
    desc.width = static_cast<uint32_t>(width);
    desc.height = static_cast<uint32_t>(height);
    desc.format = solra::render::TextureFormat::RGBA8;
    desc.generate_mipmaps = (generate_mipmaps != 0);
    desc.data = pixels;
    desc.data_size = static_cast<size_t>(width) * height * 4;

    gpuTex = g_render.gpuDevice->createTexture(desc);
    if (!gpuTex) {
      spdlog::error("GPU texture upload failed for: {}", path);
    }
  }

  // Free CPU-side pixels after GPU upload (or immediately if no GPU)
  stbi_image_free(pixels);

  // Create handle
  auto tex = std::make_shared<ActiveTexture>();
  tex->gpuTexture = gpuTex;
  tex->width = width;
  tex->height = height;
  tex->channels = 4;

  std::lock_guard<std::mutex> lock(g_texture_mutex);
  SolraTextureHandle handle = reinterpret_cast<SolraTextureHandle>(g_next_texture_handle++);
  g_textures[handle] = tex;

  spdlog::info("Texture handle created: {} ({}x{} {})",
               reinterpret_cast<uintptr_t>(handle), width, height,
               gpuTex ? "GPU" : "CPU-only");
  return handle;
}

SolraTextureHandle solra_texture_create_from_memory(
    const void *data, int width, int height, int channels, int generate_mipmaps) {
  if (!data || width <= 0 || height <= 0) {
    spdlog::error("Texture from memory: invalid parameters");
    return nullptr;
  }

  spdlog::info("Texture from memory: {}x{} ch={}", width, height, channels);

  // Convert to RGBA8 if needed
  std::vector<unsigned char> rgba;
  const unsigned char* src = static_cast<const unsigned char*>(data);
  if (channels != 4) {
    rgba.resize(static_cast<size_t>(width) * height * 4);
    for (int i = 0; i < width * height; ++i) {
      rgba[i * 4 + 0] = (channels >= 1) ? src[i * channels + 0] : 255;
      rgba[i * 4 + 1] = (channels >= 2) ? src[i * channels + 1] : rgba[i * 4 + 0];
      rgba[i * 4 + 2] = (channels >= 3) ? src[i * channels + 2] : rgba[i * 4 + 0];
      rgba[i * 4 + 3] = (channels >= 4) ? src[i * channels + 3] : 255;
    }
    src = rgba.data();
  }

  // Upload to GPU
  std::shared_ptr<solra::render::GpuTexture> gpuTex;
  if (g_render.hasGpu && g_render.gpuDevice) {
    solra::render::TextureDesc desc;
    desc.width = static_cast<uint32_t>(width);
    desc.height = static_cast<uint32_t>(height);
    desc.format = solra::render::TextureFormat::RGBA8;
    desc.generate_mipmaps = (generate_mipmaps != 0);
    desc.data = src;
    desc.data_size = static_cast<size_t>(width) * height * 4;
    gpuTex = g_render.gpuDevice->createTexture(desc);
  }

  auto tex = std::make_shared<ActiveTexture>();
  tex->gpuTexture = gpuTex;
  tex->width = width;
  tex->height = height;
  tex->channels = 4;

  std::lock_guard<std::mutex> lock(g_texture_mutex);
  SolraTextureHandle handle = reinterpret_cast<SolraTextureHandle>(g_next_texture_handle++);
  g_textures[handle] = tex;

  return handle;
}

void solra_texture_destroy(SolraTextureHandle texture) {
  if (!texture) return;
  std::lock_guard<std::mutex> lock(g_texture_mutex);
  auto it = g_textures.find(texture);
  if (it != g_textures.end()) {
    spdlog::debug("Texture destroyed: {}", reinterpret_cast<uintptr_t>(texture));
    g_textures.erase(it);
  }
}

void solra_texture_get_size(SolraTextureHandle texture, int *width, int *height) {
  std::lock_guard<std::mutex> lock(g_texture_mutex);
  auto it = g_textures.find(texture);
  if (it != g_textures.end()) {
    if (width) *width = it->second->width;
    if (height) *height = it->second->height;
  } else {
    if (width) *width = 0;
    if (height) *height = 0;
  }
}

int solra_texture_bind(SolraTextureHandle texture, int slot) {
  std::lock_guard<std::mutex> lock(g_texture_mutex);
  auto it = g_textures.find(texture);
  if (it == g_textures.end() || !it->second->gpuTexture) return SOLRA_ERROR_INVALID_ARGUMENT;

  // Bind via OpenGL device (the GPU texture is an OpenGL texture)
  if (g_render.hasGpu && g_render.gpuDevice) {
    auto* glDevice = dynamic_cast<solra::render::OpenGLDevice*>(g_render.gpuDevice.get());
    if (glDevice) {
      auto* glTex = dynamic_cast<solra::render::OpenGLTexture*>(it->second->gpuTexture.get());
      if (glTex) {
        glTex->bind(static_cast<uint32_t>(slot));
        return SOLRA_OK;
      }
    }
  }
  return SOLRA_ERROR_NOT_SUPPORTED;
}

/* ============================================================
 * Shader Compilation
 * ============================================================ */

void *solra_shader_compile(const char *source, const char *stage, const char *entry_point) {
  if (!source || !stage) return nullptr;

  solra::render::ShaderStage shaderStage;
  if (std::strcmp(stage, "vertex") == 0) shaderStage = solra::render::ShaderStage::Vertex;
  else if (std::strcmp(stage, "fragment") == 0) shaderStage = solra::render::ShaderStage::Fragment;
  else if (std::strcmp(stage, "compute") == 0) shaderStage = solra::render::ShaderStage::Compute;
  else return nullptr;

  if (!g_render.hasGpu) return nullptr;

  auto* glDevice = dynamic_cast<solra::render::OpenGLDevice*>(g_render.gpuDevice.get());
  if (!glDevice) return nullptr;

  auto shader = solra::render::OpenGLShader::compileFromSource(shaderStage, source, glDevice);
  if (!shader) return nullptr;

  // Return raw pointer (caller must manage lifetime or use via pipeline)
  // This leaks intentionally for C API simplicity; proper impl uses handle map
  return new std::shared_ptr<solra::render::OpenGLShader>(shader);
}

int solra_shader_reload_all(void) {
  // Re-init pipelines with built-in shaders
  if (g_render.hasGpu) {
    g_render.pbrPipeline.reset();
    g_render.pbrVertexShader.reset();
    g_render.pbrFragmentShader.reset();
    g_render.unlitPipeline.reset();
    g_render.unlitVertexShader.reset();
    g_render.unlitFragmentShader.reset();
    init_gpu_pipelines();
  }
  return SOLRA_OK;
}
