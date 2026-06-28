/*
 * Solra Core SDK - Type system unit tests
 */

#include <gtest/gtest.h>
#include <solra/solra_types.h>

TEST(SolraTypesTest, Vec2Default) {
  SolraVec2 v = {};
  EXPECT_FLOAT_EQ(v.x, 0.0f);
  EXPECT_FLOAT_EQ(v.y, 0.0f);
}

TEST(SolraTypesTest, Vec3Default) {
  SolraVec3 v = {};
  EXPECT_FLOAT_EQ(v.x, 0.0f);
  EXPECT_FLOAT_EQ(v.y, 0.0f);
  EXPECT_FLOAT_EQ(v.z, 0.0f);
}

TEST(SolraTypesTest, ColorDefault) {
  SolraColor c = {};
  EXPECT_FLOAT_EQ(c.r, 0.0f);
  EXPECT_FLOAT_EQ(c.g, 0.0f);
  EXPECT_FLOAT_EQ(c.b, 0.0f);
  EXPECT_FLOAT_EQ(c.a, 0.0f);
}

TEST(SolraTypesTest, Mat4Identity) {
  SolraMat4 m = {};
  /* All zeros by default - verify size */
  EXPECT_EQ(sizeof(m.m), 64u); /* 16 * 4 bytes */
}

TEST(SolraTypesTest, ErrorCodeEnum) {
  EXPECT_EQ(SOLRA_OK, 0);
  EXPECT_LT(SOLRA_ERROR_UNKNOWN, 0);
  EXPECT_LT(SOLRA_ERROR_GPU_UNAVAILABLE, 0);
}
