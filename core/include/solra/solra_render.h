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
 * Render State Query (Host/Frontend Integration)
 * ============================================================ */

/**
 * Get current frames per second.
 */
SOLRA_API float solra_render_get_fps(void);

/**
 * Get current camera position and target.
 */
SOLRA_API void solra_render_get_camera(float *pos_x, float *pos_y, float *pos_z,
                                        float *target_x, float *target_y, float *target_z);

/* ============================================================
 * Camera Control API
 * ============================================================ */

/** Camera mode */
typedef enum SolraCameraMode {
  SOLRA_CAMERA_MODE_FIRST_PERSON = 0,
  SOLRA_CAMERA_MODE_THIRD_PERSON = 1,
  SOLRA_CAMERA_MODE_ORBIT        = 2,
  SOLRA_CAMERA_MODE_FREE_FLY     = 3,
  SOLRA_CAMERA_MODE_CINEMATIC    = 4,
} SolraCameraMode;

/**
 * Set camera mode.
 */
SOLRA_API void solra_camera_set_mode(SolraCameraMode mode);

/**
 * Get current camera mode.
 */
SOLRA_API SolraCameraMode solra_camera_get_mode(void);

/**
 * Set camera position (free fly / first person).
 */
SOLRA_API void solra_camera_set_position(float x, float y, float z);

/**
 * Set camera look-at target.
 */
SOLRA_API void solra_camera_set_target(float x, float y, float z);

/**
 * Set camera FOV (degrees).
 */
SOLRA_API void solra_camera_set_fov(float fov_degrees);

/**
 * Set camera clip planes.
 */
SOLRA_API void solra_camera_set_clip_planes(float near_plane, float far_plane);

/**
 * Set first-person yaw/pitch (degrees).
 */
SOLRA_API void solra_camera_set_yaw_pitch(float yaw_degrees, float pitch_degrees);

/**
 * Get first-person yaw/pitch (degrees).
 */
SOLRA_API void solra_camera_get_yaw_pitch(float *yaw_degrees, float *pitch_degrees);

/**
 * Process mouse movement input (delta in pixels).
 */
SOLRA_API void solra_camera_on_mouse_move(float delta_x, float delta_y);

/**
 * Process mouse scroll input.
 */
SOLRA_API void solra_camera_on_mouse_scroll(float delta);

/**
 * Process keyboard movement (WASD-style).
 *
 * @param forward,backward,left,right,up,down Movement flags.
 * @param sprint Sprint modifier.
 * @param delta_time Frame delta time in seconds.
 */
SOLRA_API void solra_camera_on_keyboard_move(int forward, int backward,
                                              int left, int right,
                                              int up, int down,
                                              int sprint, float delta_time);

/**
 * Set third-person follow target.
 */
SOLRA_API void solra_camera_set_follow_target(float x, float y, float z,
                                                float height_offset);

/**
 * Smoothly transition camera to a new position/target.
 *
 * @param pos_x,pos_y,pos_z Target camera position.
 * @param target_x,target_y,target_z Target look-at point.
 * @param duration_seconds Transition duration in seconds.
 */
SOLRA_API void solra_camera_transition_to(
    float pos_x, float pos_y, float pos_z,
    float target_x, float target_y, float target_z,
    float duration_seconds);

/**
 * Check if camera is currently transitioning.
 *
 * @return 1 if transitioning, 0 if idle.
 */
SOLRA_API int solra_camera_is_transitioning(void);

/**
 * Cancel current camera transition.
 */
SOLRA_API void solra_camera_cancel_transition(void);

/**
 * Apply camera shake effect.
 *
 * @param intensity Shake intensity (world units).
 * @param duration_seconds Duration in seconds.
 */
SOLRA_API void solra_camera_shake(float intensity, float duration_seconds);

/**
 * Set camera smoothing parameters.
 *
 * @param position_smooth 0=instant, 1=max smooth.
 * @param rotation_smooth 0=instant, 1=max smooth.
 */
SOLRA_API void solra_camera_set_smoothing(float position_smooth, float rotation_smooth);

/**
 * Update camera (call once per frame).
 *
 * @param delta_time Frame delta time in seconds.
 */
SOLRA_API void solra_camera_update(float delta_time);

/**
 * Get total rendered frame count.
 */
SOLRA_API uint64_t solra_render_get_frame_count(void);

/**
 * Get elapsed time since renderer init (seconds).
 */
SOLRA_API float solra_render_get_elapsed_time(void);

/**
 * Get number of active scene nodes.
 */
SOLRA_API int solra_render_get_node_count(void);

/**
 * Get scene node data by index (for frontend sync).
 *
 * @param index Node index [0, node_count).
 * @param pos_x,pos_y,pos_z Output position.
 * @param rot_x,rot_y,rot_z,rot_w Output rotation quaternion.
 * @param scl_x,scl_y,scl_z Output scale.
 * @param name_buf Output buffer for node name.
 * @param name_buf_size Size of name_buf.
 * @return 0 on success.
 */
SOLRA_API int solra_render_get_node_data(int index,
                                          float *pos_x, float *pos_y, float *pos_z,
                                          float *rot_x, float *rot_y, float *rot_z, float *rot_w,
                                          float *scl_x, float *scl_y, float *scl_z,
                                          char *name_buf, int name_buf_size);

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
 * Scene Serialization
 * ============================================================ */

/**
 * Serialize a scene graph to a JSON string.
 *
 * @param scene Scene to serialize.
 * @param out_json Buffer to receive JSON string. May be NULL to query required size.
 * @param buf_size Size of out_json buffer.
 * @return Required buffer size (including null terminator), or 0 on error.
 *         If out_json is non-NULL and buf_size is sufficient, the JSON is written.
 *         Caller should call with out_json=NULL first to get size, then allocate and call again.
 */
SOLRA_API int solra_scene_serialize(SolraSceneHandle scene, char *out_json, int buf_size);

/**
 * Deserialize a scene graph from a JSON string, replacing current content.
 *
 * @param scene Scene to populate.
 * @param json Null-terminated JSON string.
 * @return 0 on success, negative on error.
 */
SOLRA_API int solra_scene_deserialize(SolraSceneHandle scene, const char *json);

/**
 * Save a scene graph to a file.
 *
 * @param scene Scene to save.
 * @param path File path (.json or .scn for binary).
 * @return 0 on success, negative on error.
 */
SOLRA_API int solra_scene_save_to_file(SolraSceneHandle scene, const char *path);

/**
 * Load a scene graph from a file, replacing current content.
 *
 * @param scene Scene to populate.
 * @param path File path.
 * @return 0 on success, negative on error.
 */
SOLRA_API int solra_scene_load_from_file(SolraSceneHandle scene, const char *path);

/**
 * Set a key-value user data on a scene node (persisted via serialization).
 *
 * @param node Target node.
 * @param key Metadata key.
 * @param value Metadata value.
 */
SOLRA_API void solra_scene_node_set_user_data(SolraSceneNodeHandle node, const char *key, const char *value);

/**
 * Get a key-value user data from a scene node.
 *
 * @param node Target node.
 * @param key Metadata key.
 * @param out_value Buffer for value string. May be NULL to query required size.
 * @param buf_size Size of out_value buffer.
 * @return Required buffer size, or 0 if key not found.
 */
SOLRA_API int solra_scene_node_get_user_data(SolraSceneNodeHandle node, const char *key, char *out_value, int buf_size);

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
 * Create a skinned mesh from vertex and index data.
 *
 * Skinned vertex layout: pos3 + normal3 + uv2 + boneWeights4 + boneIndices4 = 48 bytes.
 *
 * @param vertices Pointer to vertex data (interleaved: pos3+normal3+uv2+boneWeights4+boneIndices4).
 * @param vertex_count Number of vertices.
 * @param indices Pointer to index data (uint16 or uint32).
 * @param index_count Number of indices.
 * @param index_type 16 or 32 bit indices.
 * @return Mesh handle, or NULL on failure.
 */
SOLRA_API SolraMeshHandle solra_mesh_create_skinned(
  const void *vertices,
  int vertex_count,
  const void *indices,
  int index_count,
  int index_type
);

/**
 * Set the bone matrix palette for a skinned mesh.
 *
 * Bone matrices should be pre-computed as: boneMatrix = globalTransform * inverseBindMatrix.
 * Each matrix is 16 floats (column-major for GLSL, row-major stored).
 *
 * @param mesh Target mesh (must be created with solra_mesh_create_skinned).
 * @param bone_matrices Array of 4x4 bone matrices (16 floats per bone).
 * @param bone_count Number of bones in the palette (max 128).
 * @return 0 on success, negative on error.
 */
SOLRA_API int solra_mesh_set_bone_matrices(
  SolraMeshHandle mesh,
  const float *bone_matrices,
  int bone_count
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
 * Create a texture from raw pixel data in memory.
 *
 * @param data Raw pixel data (RGBA8 or RGB8).
 * @param width Image width in pixels.
 * @param height Image height in pixels.
 * @param channels Number of color channels (1, 2, 3, or 4).
 * @param generate_mipmaps Whether to auto-generate mipmap chain.
 * @return Texture handle, or NULL on failure.
 */
SOLRA_API SolraTextureHandle solra_texture_create_from_memory(
    const void *data, int width, int height, int channels, int generate_mipmaps);

/**
 * Bind a texture to a texture unit slot for the current draw call.
 *
 * @param texture Texture handle.
 * @param slot Texture unit index (0-based).
 * @return 0 on success, negative on error.
 */
SOLRA_API int solra_texture_bind(SolraTextureHandle texture, int slot);

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
