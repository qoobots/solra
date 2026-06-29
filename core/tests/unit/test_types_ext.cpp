/*
 * Solra Core SDK - Extended type system unit tests
 */

#include <gtest/gtest.h>
#include <solra/solra_types.h>
#include <cstring>

/* ============================================================
 * Vec2 Tests
 * ============================================================ */
TEST(SolraTypesExt, Vec2Values) {
  SolraVec2 v = {1.5f, -2.5f};
  EXPECT_FLOAT_EQ(v.x, 1.5f);
  EXPECT_FLOAT_EQ(v.y, -2.5f);
}

TEST(SolraTypesExt, Vec2Size) {
  EXPECT_EQ(sizeof(SolraVec2), 8u); /* 2 * float */
}

/* ============================================================
 * Vec3 Tests
 * ============================================================ */
TEST(SolraTypesExt, Vec3Values) {
  SolraVec3 v = {1.0f, 2.0f, 3.0f};
  EXPECT_FLOAT_EQ(v.x, 1.0f);
  EXPECT_FLOAT_EQ(v.y, 2.0f);
  EXPECT_FLOAT_EQ(v.z, 3.0f);
}

TEST(SolraTypesExt, Vec3Size) {
  EXPECT_EQ(sizeof(SolraVec3), 12u); /* 3 * float */
}

/* ============================================================
 * Vec4 Tests
 * ============================================================ */
TEST(SolraTypesExt, Vec4Values) {
  SolraVec4 v = {1.0f, 2.0f, 3.0f, 4.0f};
  EXPECT_FLOAT_EQ(v.x, 1.0f);
  EXPECT_FLOAT_EQ(v.y, 2.0f);
  EXPECT_FLOAT_EQ(v.z, 3.0f);
  EXPECT_FLOAT_EQ(v.w, 4.0f);
}

TEST(SolraTypesExt, Vec4Size) {
  EXPECT_EQ(sizeof(SolraVec4), 16u); /* 4 * float */
}

/* ============================================================
 * Quat Tests
 * ============================================================ */
TEST(SolraTypesExt, QuatIdentity) {
  SolraQuat q = {0.0f, 0.0f, 0.0f, 1.0f};
  EXPECT_FLOAT_EQ(q.x, 0.0f);
  EXPECT_FLOAT_EQ(q.y, 0.0f);
  EXPECT_FLOAT_EQ(q.z, 0.0f);
  EXPECT_FLOAT_EQ(q.w, 1.0f);
}

/* ============================================================
 * Mat4 Tests
 * ============================================================ */
TEST(SolraTypesExt, Mat4Size) {
  EXPECT_EQ(sizeof(SolraMat4), 64u); /* 16 * 4 bytes */
}

TEST(SolraTypesExt, Mat4Elements) {
  SolraMat4 m = {};
  /* Identity-like: set diagonal */
  m.m[0] = 1.0f;  m.m[5] = 1.0f;  m.m[10] = 1.0f;  m.m[15] = 1.0f;
  EXPECT_FLOAT_EQ(m.m[0], 1.0f);
  EXPECT_FLOAT_EQ(m.m[5], 1.0f);
  EXPECT_FLOAT_EQ(m.m[10], 1.0f);
  EXPECT_FLOAT_EQ(m.m[15], 1.0f);
}

/* ============================================================
 * AABB Tests
 * ============================================================ */
TEST(SolraTypesExt, AABBDefault) {
  SolraAABB aabb = {};
  EXPECT_FLOAT_EQ(aabb.min.x, 0.0f);
  EXPECT_FLOAT_EQ(aabb.min.y, 0.0f);
  EXPECT_FLOAT_EQ(aabb.min.z, 0.0f);
  EXPECT_FLOAT_EQ(aabb.max.x, 0.0f);
  EXPECT_FLOAT_EQ(aabb.max.y, 0.0f);
  EXPECT_FLOAT_EQ(aabb.max.z, 0.0f);
}

TEST(SolraTypesExt, AABBValid) {
  SolraAABB aabb = {};
  aabb.min = {-1.0f, -1.0f, -1.0f};
  aabb.max = {1.0f, 1.0f, 1.0f};
  EXPECT_LT(aabb.min.x, aabb.max.x);
  EXPECT_LT(aabb.min.y, aabb.max.y);
  EXPECT_LT(aabb.min.z, aabb.max.z);
}

/* ============================================================
 * Sphere Tests
 * ============================================================ */
TEST(SolraTypesExt, SphereDefault) {
  SolraSphere s = {};
  EXPECT_FLOAT_EQ(s.center.x, 0.0f);
  EXPECT_FLOAT_EQ(s.center.y, 0.0f);
  EXPECT_FLOAT_EQ(s.center.z, 0.0f);
  EXPECT_FLOAT_EQ(s.radius, 0.0f);
}

TEST(SolraTypesExt, SphereValues) {
  SolraSphere s = {{1.0f, 2.0f, 3.0f}, 5.0f};
  EXPECT_FLOAT_EQ(s.center.x, 1.0f);
  EXPECT_FLOAT_EQ(s.center.y, 2.0f);
  EXPECT_FLOAT_EQ(s.center.z, 3.0f);
  EXPECT_FLOAT_EQ(s.radius, 5.0f);
}

/* ============================================================
 * Color Tests
 * ============================================================ */
TEST(SolraTypesExt, ColorValues) {
  SolraColor c = {0.5f, 0.25f, 0.75f, 1.0f};
  EXPECT_FLOAT_EQ(c.r, 0.5f);
  EXPECT_FLOAT_EQ(c.g, 0.25f);
  EXPECT_FLOAT_EQ(c.b, 0.75f);
  EXPECT_FLOAT_EQ(c.a, 1.0f);
}

TEST(SolraTypesExt, ColorSize) {
  EXPECT_EQ(sizeof(SolraColor), 16u); /* 4 * float */
}

/* ============================================================
 * Error Code Tests
 * ============================================================ */
TEST(SolraTypesExt, AllErrorCodesDistinct) {
  /* Verify all error codes are unique negative values */
  EXPECT_EQ(SOLRA_OK, 0);
  EXPECT_LT(SOLRA_ERROR_UNKNOWN, 0);
  EXPECT_LT(SOLRA_ERROR_INVALID_ARGUMENT, 0);
  EXPECT_LT(SOLRA_ERROR_NOT_INITIALIZED, 0);
  EXPECT_LT(SOLRA_ERROR_ALREADY_INITIALIZED, 0);
  EXPECT_LT(SOLRA_ERROR_OUT_OF_MEMORY, 0);
  EXPECT_LT(SOLRA_ERROR_IO, 0);
  EXPECT_LT(SOLRA_ERROR_NETWORK, 0);
  EXPECT_LT(SOLRA_ERROR_TIMEOUT, 0);
  EXPECT_LT(SOLRA_ERROR_UNSUPPORTED, 0);
  EXPECT_LT(SOLRA_ERROR_GPU_UNAVAILABLE, 0);
  EXPECT_LT(SOLRA_ERROR_NPU_UNAVAILABLE, 0);
  EXPECT_LT(SOLRA_ERROR_MODEL_LOAD_FAILED, 0);
  EXPECT_LT(SOLRA_ERROR_INFERENCE_FAILED, 0);
  EXPECT_LT(SOLRA_ERROR_ASSET_NOT_FOUND, 0);
  EXPECT_LT(SOLRA_ERROR_CACHE_FULL, 0);
}

TEST(SolraTypesExt, ErrorStringAllCodes) {
  /* All error codes should return non-null strings */
  EXPECT_NE(solra_error_string(0), nullptr);
  EXPECT_NE(solra_error_string(SOLRA_ERROR_UNKNOWN), nullptr);
  EXPECT_NE(solra_error_string(SOLRA_ERROR_INVALID_ARGUMENT), nullptr);
  EXPECT_NE(solra_error_string(SOLRA_ERROR_NOT_INITIALIZED), nullptr);
  EXPECT_NE(solra_error_string(SOLRA_ERROR_ALREADY_INITIALIZED), nullptr);
  EXPECT_NE(solra_error_string(SOLRA_ERROR_OUT_OF_MEMORY), nullptr);
  EXPECT_NE(solra_error_string(SOLRA_ERROR_IO), nullptr);
  EXPECT_NE(solra_error_string(SOLRA_ERROR_NETWORK), nullptr);
  EXPECT_NE(solra_error_string(SOLRA_ERROR_TIMEOUT), nullptr);
  EXPECT_NE(solra_error_string(SOLRA_ERROR_UNSUPPORTED), nullptr);
  EXPECT_NE(solra_error_string(SOLRA_ERROR_GPU_UNAVAILABLE), nullptr);
  EXPECT_NE(solra_error_string(SOLRA_ERROR_NPU_UNAVAILABLE), nullptr);
  EXPECT_NE(solra_error_string(SOLRA_ERROR_MODEL_LOAD_FAILED), nullptr);
  EXPECT_NE(solra_error_string(SOLRA_ERROR_INFERENCE_FAILED), nullptr);
  EXPECT_NE(solra_error_string(SOLRA_ERROR_ASSET_NOT_FOUND), nullptr);
  EXPECT_NE(solra_error_string(SOLRA_ERROR_CACHE_FULL), nullptr);
}

/* ============================================================
 * Constants Tests
 * ============================================================ */
TEST(SolraTypesExt, MaxLengths) {
  EXPECT_GE(SOLRA_ASSET_ID_MAX_LEN, 32u);
  EXPECT_GE(SOLRA_ASSET_URL_MAX_LEN, 256u);
}

/* ============================================================
 * AssetType Enum Tests
 * ============================================================ */
TEST(SolraTypesExt, AssetTypeValues) {
  EXPECT_EQ(SOLRA_ASSET_TYPE_UNKNOWN, 0);
  EXPECT_EQ(SOLRA_ASSET_TYPE_SCENE, 1);
  EXPECT_EQ(SOLRA_ASSET_TYPE_MESH, 2);
  EXPECT_EQ(SOLRA_ASSET_TYPE_TEXTURE, 3);
  EXPECT_EQ(SOLRA_ASSET_TYPE_MATERIAL, 4);
  EXPECT_EQ(SOLRA_ASSET_TYPE_AUDIO, 5);
  EXPECT_EQ(SOLRA_ASSET_TYPE_ANIMATION, 6);
  EXPECT_EQ(SOLRA_ASSET_TYPE_SCRIPT, 7);
}

/* ============================================================
 * AssetStatus Enum Tests
 * ============================================================ */
TEST(SolraTypesExt, AssetStatusValues) {
  EXPECT_EQ(SOLRA_ASSET_STATUS_NOT_LOADED, 0);
  EXPECT_EQ(SOLRA_ASSET_STATUS_QUEUED, 1);
  EXPECT_EQ(SOLRA_ASSET_STATUS_DOWNLOADING, 2);
  EXPECT_EQ(SOLRA_ASSET_STATUS_READY, 3);
  EXPECT_EQ(SOLRA_ASSET_STATUS_FAILED, 4);
}

/* ============================================================
 * RenderBackend Enum Tests
 * ============================================================ */
TEST(SolraTypesExt, RenderBackendValues) {
  EXPECT_EQ(SOLRA_RENDER_BACKEND_AUTO, 0);
  EXPECT_EQ(SOLRA_RENDER_BACKEND_METAL, 1);
  EXPECT_EQ(SOLRA_RENDER_BACKEND_VULKAN, 2);
  EXPECT_EQ(SOLRA_RENDER_BACKEND_OPENGLES, 3);
}
