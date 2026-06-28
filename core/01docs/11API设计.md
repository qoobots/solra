# 核心引擎层（Core）API设计

> Core SDK 对外C API设计——稳定ABI、不透明句柄、错误码返回

---

## 一、API设计原则

| 原则 | 说明 |
|------|------|
| **C ABI稳定** | 纯C接口，跨语言FFI兼容 |
| **不透明句柄** | `SolraHandle` 封装内部状态 |
| **返回值指示成功** | 所有函数返回 `SolraResult` |
| **输出参数出参** | 结果通过指针参数返回 |
| **线程安全标注** | [thread-safe] / [main-thread-only] |
| **版本后缀** | 函数名不包含版本号，通过SDK版本管理 |

---

## 二、核心API分类

### 2.1 生命周期

```c
SolraResult solra_core_init(const SolraConfig* config, SolraHandle* handle);
SolraResult solra_core_destroy(SolraHandle handle);
const char* solra_core_version(void);
```

### 2.2 渲染

```c
SolraResult solra_render_create_scene(SolraHandle, const char* scene_id);
SolraResult solra_render_update(SolraHandle, float delta_time);
SolraResult solra_render_get_fps(SolraHandle, float* fps);
SolraResult solra_render_set_camera(SolraHandle, const SolraCamera* camera);
SolraResult solra_render_resize(SolraHandle, int width, int height);
```

### 2.3 推理

```c
SolraResult solra_inference_load_model(SolraHandle, const char* model_path);
SolraResult solra_inference_send_message(SolraHandle, const char* conversation_id, 
                                          const char* message, SolraCallback callback, void* user_data);
SolraResult solra_inference_stop(SolraHandle);
```

### 2.4 流式加载

```c
SolraResult solra_streaming_load_space(SolraHandle, const char* space_id, 
                                        SolraProgressCallback callback, void* user_data);
SolraResult solra_streaming_cancel(SolraHandle);
SolraResult solra_streaming_get_cache_size(SolraHandle, size_t* size);
```

### 2.5 WebRTC

```c
SolraResult solra_webrtc_connect(SolraHandle, const char* room_id, const char* token);
SolraResult solra_webrtc_send_data(SolraHandle, const uint8_t* data, size_t len);
SolraResult solra_webrtc_disconnect(SolraHandle);
```
