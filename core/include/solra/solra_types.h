/*
 * Solra Core SDK - Common types
 *
 * Shared type definitions used across all SDK modules.
 *
 * Copyright 2026 Solra Project
 * SPDX-License-Identifier: Apache-2.0
 */

#ifndef SOLRA_TYPES_H
#define SOLRA_TYPES_H

#include <stdint.h>
#include <stddef.h>

/* ============================================================
 * Export / Import Macros
 * ============================================================ */
#if defined(_WIN32) || defined(_WIN64)
  #ifdef SOLRA_BUILD_DLL
    #define SOLRA_API __declspec(dllexport)
  #else
    #define SOLRA_API __declspec(dllimport)
  #endif
#elif defined(__GNUC__) && __GNUC__ >= 4
  #define SOLRA_API __attribute__((visibility("default")))
#else
  #define SOLRA_API
#endif

#ifdef __cplusplus
extern "C" {
#endif

/* ============================================================
 * Error Codes
 * ============================================================ */

typedef enum SolraErrorCode {
  SOLRA_OK = 0,
  SOLRA_ERROR_UNKNOWN = -1,
  SOLRA_ERROR_INVALID_ARGUMENT = -2,
  SOLRA_ERROR_NOT_INITIALIZED = -3,
  SOLRA_ERROR_ALREADY_INITIALIZED = -4,
  SOLRA_ERROR_OUT_OF_MEMORY = -5,
  SOLRA_ERROR_IO = -6,
  SOLRA_ERROR_NETWORK = -7,
  SOLRA_ERROR_TIMEOUT = -8,
  SOLRA_ERROR_UNSUPPORTED = -9,
  SOLRA_ERROR_GPU_UNAVAILABLE = -10,
  SOLRA_ERROR_NPU_UNAVAILABLE = -11,
  SOLRA_ERROR_MODEL_LOAD_FAILED = -12,
  SOLRA_ERROR_INFERENCE_FAILED = -13,
  SOLRA_ERROR_ASSET_NOT_FOUND = -14,
  SOLRA_ERROR_CACHE_FULL = -15,
} SolraErrorCode;

/**
 * Convert an error code to a human-readable string.
 */
const char *solra_error_string(int error_code);

/* ============================================================
 * Math Types
 * ============================================================ */

typedef struct SolraVec2 {
  float x, y;
} SolraVec2;

typedef struct SolraVec3 {
  float x, y, z;
} SolraVec3;

typedef struct SolraVec4 {
  float x, y, z, w;
} SolraVec4;

typedef struct SolraQuat {
  float x, y, z, w;
} SolraQuat;

typedef struct SolraMat4 {
  float m[16];
} SolraMat4;

typedef struct SolraAABB {
  SolraVec3 min;
  SolraVec3 max;
} SolraAABB;

typedef struct SolraSphere {
  SolraVec3 center;
  float radius;
} SolraSphere;

/* ============================================================
 * Color
 * ============================================================ */

typedef struct SolraColor {
  float r, g, b, a;
} SolraColor;

/* ============================================================
 * Asset Identifiers
 * ============================================================ */

/** Maximum length of an asset ID string (including null terminator) */
#define SOLRA_ASSET_ID_MAX_LEN 64

/** Maximum length of an asset URL (including null terminator) */
#define SOLRA_ASSET_URL_MAX_LEN 2048

/* ============================================================
 * Memory Management
 * ============================================================ */

/**
 * Custom allocation callback.
 * @param size Number of bytes to allocate.
 * @param user_data Opaque user data pointer.
 * @return Allocated memory block, or NULL on failure.
 */
typedef void *(*SolraAllocFn)(size_t size, void *user_data);

/**
 * Custom deallocation callback.
 * @param ptr Pointer to memory block to free.
 * @param user_data Opaque user data pointer.
 */
typedef void (*SolraFreeFn)(void *ptr, void *user_data);

/**
 * Set custom memory allocators for the SDK.
 *
 * Must be called before solra_core_init() to take effect.
 *
 * @param alloc_fn Allocation function (NULL = use default malloc).
 * @param free_fn Deallocation function (NULL = use default free).
 * @param user_data Opaque pointer passed to alloc/free callbacks.
 */
SOLRA_API void solra_set_allocator(SolraAllocFn alloc_fn, SolraFreeFn free_fn, void *user_data);

#ifdef __cplusplus
}
#endif

#endif /* SOLRA_TYPES_H */
