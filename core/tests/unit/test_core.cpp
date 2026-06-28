/*
 * Solra Core SDK - Core unit tests
 */

#include <gtest/gtest.h>
#include <solra/solra_core.h>
#include <solra/solra_types.h>

class SolraCoreTest : public ::testing::Test {
protected:
  void SetUp() override {
    /* Tests should not depend on full SDK initialization */
  }
  void TearDown() override {}
};

TEST_F(SolraCoreTest, VersionString) {
  const char *version = solra_core_get_version();
  ASSERT_NE(version, nullptr);
  ASSERT_GT(strlen(version), 0u);
}

TEST_F(SolraCoreTest, ErrorString) {
  EXPECT_STREQ(solra_error_string(0), "OK");
  EXPECT_STREQ(solra_error_string(-2), "Invalid argument");
  EXPECT_STREQ(solra_error_string(-3), "Not initialized");
}

TEST_F(SolraCoreTest, InitWithoutConfig) {
  int result = solra_core_init(nullptr);
  EXPECT_EQ(result, SOLRA_ERROR_INVALID_ARGUMENT);
}

TEST_F(SolraCoreTest, InitWithValidConfig) {
  SolraCoreConfig config = {};
  config.display_width = 1920;
  config.display_height = 1080;
  config.target_fps = 60;
  config.enable_gpu = 0; /* Headless for testing */
  config.log_level = 2;

  int result = solra_core_init(&config);
  EXPECT_EQ(result, SOLRA_OK);

  SolraCoreState state;
  result = solra_core_get_state(&state);
  EXPECT_EQ(result, SOLRA_OK);
  EXPECT_EQ(state.initialized, 1);

  solra_core_shutdown();
}

TEST_F(SolraCoreTest, DoubleInitPrevented) {
  SolraCoreConfig config = {};
  config.log_level = 2;
  config.enable_gpu = 0;

  ASSERT_EQ(solra_core_init(&config), SOLRA_OK);
  EXPECT_EQ(solra_core_init(&config), SOLRA_ERROR_ALREADY_INITIALIZED);

  solra_core_shutdown();
}

TEST_F(SolraCoreTest, StateBeforeInit) {
  SolraCoreState state;
  int result = solra_core_get_state(&state);
  EXPECT_EQ(result, SOLRA_OK);
  EXPECT_EQ(state.initialized, 0);
}
