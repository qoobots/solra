/*
 * Solra Core SDK - Rendering API
 *
 * GPU-accelerated 3D rendering pipeline with Metal/Vulkan/OpenGL ES backends.
 *
 * Copyright 2026 Solra Project
 * SPDX-License-Identifier: Apache-2.0
 */

#ifndef SOLRA_RENDER_H
#define SOLRA_RENDER_H

#include <solra/solra_types.h>

#ifdef __cplusplus
extern "C" {
#endif

/* ============================================================
 * Backend Types
 * ============================================================ */

typedef enum SolraRenderBackend {
  SOLRA_RENDER_BACKEND_AUTO = 0,
  SOLRA_RENDER_BACKEND_METAL = 1,
  SOLRA_RENDER_BACKEND_VULKAN = 2,
  SOLRA_RENDER_BACKEND_OPENGLES = 3,
} SolraRenderBackend;

/* ============================================================
 * GPU Device Info
 * ============================================================ */

typedef struct SolraGPUInfo {
  char vendor[128];
  char renderer[128];
  char version[64];
  size_t dedicated_vram_mb;
  size_t shared_vram_mb;
  int max_texture_size;
  int max_compute_workgroup_size;
  int supports_ray_tracing;
  int supports_mesh_shader;
} SolraGPUInfo;

/* ============================================================
 * Renderer Configuration
 * ============================================================ */

typedef struct SolraRenderConfig {
  /** Preferred backend (AUTO = best available) */
  SolraRenderBackend backend;
  /** Viewport width in pixels */
  int width;
  /** Viewport height in pixels */
  int height;
  /** Enable double/triple buffering */
  int vsync;
  /** MSAA sample count (1=off, 2, 4, 8) */
  int msaa_samples;
  /** Whether to enable HDR rendering */
  int enable_hdr;
  /** Clear color (RGBA, linear) */
  SolraColor clear_color;
  /** Native window handle (platform-specific, 0 for offscreen) */
  void *native_window;
} SolraRenderConfig;

/* ============================================================
 * Scene Graph
 * ============================================================ */

/** Opaque handle to a render scene graph */
typedef struct SolraScene *SolraSceneHandle;

/** Opaque handle to a scene node */
typedef struct SolraSceneNode *SolraSceneNodeHandle;

/** Opaque handle to a mesh */
typedef struct SolraMesh *SolraMeshHandle;

/** Opaque handle to a material */
typedef struct SolraMaterial *SolraMaterialHandle;

/** Opaque handle to a texture */
typedef struct SolraTexture *SolraTextureHandle;

/* ============================================================
 * Renderer Lifecycle
 * ============================================================ */

/**
 * Initialize the rendering subsystem.
 *
 * @param config Renderer configuration (NULL = sensible defaults).
 * @return 0 on success, negative error code on failure.
 */
SOLRA_API int solra_render_init(const SolraRenderConfig *config);

/**
 * Query GPU device information.
 *
 * @param info Non-null pointer to receive GPU info.
 * @return 0 on success.
 */
SOLRA_API int solra_render_get_gpu_info(SolraGPUInfo *info);

/**
 * Render one frame.
 *
 * Must be called each frame from the main loop. Blocks until
 * GPU commands are submitted (does not wait for completion).
 *
 * @return 0 on success.
 */
SOLRA_API int solra_render_begin_frame(void);
SOLRA_API int solra_render_end_frame(void);

/**
 * Resize the render viewport.
 *
 * @param width New width in pixels.
 * @param height New height in pixels.
 */
SOLRA_API void solra_render_resize(int width, int height);

/**
 * Shutdown the rendering subsystem.
 */
SOLRA_API void solra_render_shutdown(void);

/* ============================================================
 * Scene Management
 * ============================================================ */

/**
 * Create a new scene graph.
 *
 * @return Scene handle, or NULL on failure.
 */
SOLRA_API SolraSceneHandle solra_scene_create(void);

/**
 * Destroy a scene graph and all its nodes.
 *
 * @param scene Scene to destroy.
 */
SOLRA_API void solra_scene_destroy(SolraSceneHandle scene);

/**
 * Set the active scene for rendering.
 *
 * @param scene Scene to render. NULL clears the active scene.
 */
SOLRA_API void solra_scene_set_active(SolraSceneHandle scene);

/**
 * Create a scene node (empty by default).
 *
 * @param scene Parent scene.
 * @param name Optional node name for debugging.
 * @return Node handle, or NULL on failure.
 */
SOLRA_API SolraSceneNodeHandle solra_scene_node_create(SolraSceneHandle scene, const char *name);

/**
 * Set the local transform of a scene node.
 *
 * @param node Target node.
 * @param position Local position.
 * @param rotation Local rotation (quaternion).
 * @param scale Local scale (xyz).
 */
SOLRA_API void solra_scene_node_set_transform(
  SolraSceneNodeHandle node,
  const SolraVec3 *position,
  const SolraQuat *rotation,
  const SolraVec3 *scale
);

/**
 * Attach a mesh to a scene node.
 *
 * @param node Target node.
 * @param mesh Mesh to attach (can be shared between nodes).
 */
SOLRA_API void solra_scene_node_attach_mesh(SolraSceneNodeHandle node, SolraMeshHandle mesh);

/**
 * Set a child node under a parent node.
 *
 * @param parent Parent node.
 * @param child Child node.
 */
SOLRA_API void solra_scene_node_add_child(SolraSceneNodeHandle parent, SolraSceneNodeHandle child);

/* ============================================================
 * Mesh Management
 * ============================================================ */

/**
 * Create a mesh from vertex and index data.
 *
 * @param vertices Pointer to vertex data (interleaved: pos3+normal3+uv2).
 * @param vertex_count Number of vertices.
 * @param vertex_stride Size of each vertex in bytes.
 * @param indices Pointer to index data (uint16 or uint32).
 * @param index_count Number of indices.
 * @param index_type 16 or 32 bit indices.
 * @return Mesh handle, or NULL on failure.
 */
SOLRA_API SolraMeshHandle solra_mesh_create(
  const void *vertices,
  int vertex_count,
  int vertex_stride,
  const void *indices,
  int index_count,
  int index_type
);

/**
 * Destroy a mesh.
 */
SOLRA_API void solra_mesh_destroy(SolraMeshHandle mesh);

/**
 * Get the axis-aligned bounding box of a mesh.
 */
SOLRA_API SolraAABB solra_mesh_get_bounds(SolraMeshHandle mesh);

/* ============================================================
 * Material Management
 * ============================================================ */

/**
 * Create a PBR material.
 *
 * @return Material handle, or NULL on failure.
 */
SOLRA_API SolraMaterialHandle solra_material_create(void);

/**
 * Set base color factor (albedo) of a material.
 */
SOLRA_API void solra_material_set_base_color(SolraMaterialHandle material, SolraColor color);

/**
 * Set metallic and roughness factors.
 *
 * @param metallic 0.0 (dielectric) to 1.0 (metallic).
 * @param roughness 0.0 (mirror) to 1.0 (diffuse).
 */
SOLRA_API void solra_material_set_metallic_roughness(SolraMaterialHandle material, float metallic, float roughness);

/**
 * Attach a texture to a material slot.
 *
 * Texture slots: "base_color", "normal", "metallic_roughness", "occlusion", "emissive"
 */
SOLRA_API int solra_material_set_texture(SolraMaterialHandle material, const char *slot, SolraTextureHandle texture);

/**
 * Destroy a material.
 */
SOLRA_API void solra_material_destroy(SolraMaterialHandle material);

/* ============================================================
 * Texture Management
 * ============================================================ */

/**
 * Load a texture from file (PNG, JPEG, KTX2, etc.).
 *
 * @param path File path to texture.
 * @param generate_mipmaps Whether to auto-generate mipmap chain.
 * @return Texture handle, or NULL on failure.
 */
SOLRA_API SolraTextureHandle solra_texture_load(const char *path, int generate_mipmaps);

/**
 * Destroy a texture and free GPU memory.
 */
SOLRA_API void solra_texture_destroy(SolraTextureHandle texture);

/**
 * Get texture dimensions.
 */
SOLRA_API void solra_texture_get_size(SolraTextureHandle texture, int *width, int *height);

/* ============================================================
 * Shader Compilation Pipeline
 * ============================================================ */

/**
 * Compile a shader from GLSL/MSL source for the active backend.
 *
 * @param source Shader source code.
 * @param stage "vertex" or "fragment" or "compute".
 * @param entry_point Entry function name (e.g. "main").
 * @return Opaque shader handle, or NULL on failure.
 */
void *solra_shader_compile(const char *source, const char *stage, const char *entry_point);

/**
 * Reload shaders at runtime (hot reload for development).
 *
 * @return 0 on success, negative on failure.
 */
SOLRA_API int solra_shader_reload_all(void);

#ifdef __cplusplus
}
#endif

#endif /* SOLRA_RENDER_H */
