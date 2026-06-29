/*
 * Solra Core SDK - Streaming engine unit tests
 */

#include <gtest/gtest.h>
#include <solra/solra_core.h>
#include <solra/solra_streaming.h>
#include <solra/solra_types.h>

class SolraStreamingTest : public ::testing::Test {
protected:
  void SetUp() override {
    /* Initialize core before streaming */
    SolraCoreConfig config = {};
    config.log_level = 2;
    config.enable_gpu = 0;
    solra_core_init(&config);
  }
  void TearDown() override {
    solra_streaming_shutdown();
    solra_core_shutdown();
  }
};

TEST_F(SolraStreamingTest, InitWithDefaults) {
  int result = solra_streaming_init(nullptr);
  EXPECT_EQ(result, SOLRA_OK);
}

TEST_F(SolraStreamingTest, InitWithConfig) {
  SolraStreamingConfig config = {};
  config.max_concurrent_downloads = 4;
  config.max_cache_size_mb = 128;
  config.chunk_size_bytes = 65536;
  config.enable_http3 = 1;
  config.enable_prefetch = 1;
  config.enable_compression = 1;

  int result = solra_streaming_init(&config);
  EXPECT_EQ(result, SOLRA_OK);
}

TEST_F(SolraStreamingTest, DoubleInitPrevented) {
  ASSERT_EQ(solra_streaming_init(nullptr), SOLRA_OK);
  EXPECT_EQ(solra_streaming_init(nullptr), SOLRA_ERROR_ALREADY_INITIALIZED);
}

TEST_F(SolraStreamingTest, CacheInitiallyEmpty) {
  ASSERT_EQ(solra_streaming_init(nullptr), SOLRA_OK);

  EXPECT_EQ(solra_streaming_is_cached("test_asset"), 0);
  EXPECT_EQ(solra_streaming_get_cache_size(), 0u);
}

TEST_F(SolraStreamingTest, ClearCacheWhenEmpty) {
  ASSERT_EQ(solra_streaming_init(nullptr), SOLRA_OK);

  /* Should not crash */
  solra_streaming_clear_cache(0);
  solra_streaming_clear_cache(60);

  EXPECT_EQ(solra_streaming_get_cache_size(), 0u);
}

TEST_F(SolraStreamingTest, GetStatusNullInfo) {
  ASSERT_EQ(solra_streaming_init(nullptr), SOLRA_OK);

  SolraDownloadHandle dl = solra_streaming_load_async("test", "http://test/asset", SOLRA_ASSET_TYPE_SCENE, 5);
  int result = solra_streaming_get_status(dl, nullptr);
  EXPECT_EQ(result, SOLRA_ERROR_INVALID_ARGUMENT);
}

TEST_F(SolraStreamingTest, GetStatusValid) {
  ASSERT_EQ(solra_streaming_init(nullptr), SOLRA_OK);

  SolraDownloadHandle dl = solra_streaming_load_async("test", "http://test/asset", SOLRA_ASSET_TYPE_SCENE, 5);

  SolraAssetInfo info = {};
  int result = solra_streaming_get_status(dl, &info);
  EXPECT_EQ(result, SOLRA_OK);
}

TEST_F(SolraStreamingTest, CancelDoesNotCrash) {
  ASSERT_EQ(solra_streaming_init(nullptr), SOLRA_OK);

  SolraDownloadHandle dl = solra_streaming_load_async("test", "http://test/asset", SOLRA_ASSET_TYPE_SCENE, 5);
  solra_streaming_cancel(dl);
  /* Should not crash */
  SUCCEED();
}

TEST_F(SolraStreamingTest, SetPriorityDoesNotCrash) {
  ASSERT_EQ(solra_streaming_init(nullptr), SOLRA_OK);

  SolraDownloadHandle dl = solra_streaming_load_async("test", "http://test/asset", SOLRA_ASSET_TYPE_SCENE, 5);
  solra_streaming_set_priority(dl, 10);
  solra_streaming_set_priority(dl, 0);
  SUCCEED();
}

TEST_F(SolraStreamingTest, LodOperations) {
  ASSERT_EQ(solra_streaming_init(nullptr), SOLRA_OK);

  solra_streaming_set_lod("test_asset", 2);
  solra_streaming_set_lod("test_asset", 0);

  solra_streaming_set_lod_bias(0.5f);
  solra_streaming_set_lod_bias(-0.5f);
  solra_streaming_set_lod_bias(0.0f);

  SUCCEED();
}

TEST_F(SolraStreamingTest, PrefetchDoesNotCrash) {
  ASSERT_EQ(solra_streaming_init(nullptr), SOLRA_OK);

  const char *ids[] = {"asset_a", "asset_b"};
  const char *urls[] = {"http://cdn/a", "http://cdn/b"};
  solra_streaming_prefetch(ids, urls, 2);

  SUCCEED();
}

TEST_F(SolraStreamingTest, ProgressCallback) {
  ASSERT_EQ(solra_streaming_init(nullptr), SOLRA_OK);

  solra_streaming_set_progress_callback(nullptr, nullptr);
  SUCCEED();
}

TEST_F(SolraStreamingTest, SyncLoadReturnsNull) {
  ASSERT_EQ(solra_streaming_init(nullptr), SOLRA_OK);

  SolraAssetHandle handle = solra_streaming_load_sync("test", "http://test", SOLRA_ASSET_TYPE_SCENE, 1000);
  EXPECT_EQ(handle, nullptr);
}

TEST_F(SolraStreamingTest, AsyncLoadReturnsHandle) {
  ASSERT_EQ(solra_streaming_init(nullptr), SOLRA_OK);

  SolraDownloadHandle dl = solra_streaming_load_async("test", "http://test/asset", SOLRA_ASSET_TYPE_SCENE, 5);
  EXPECT_NE(dl, nullptr);
}

TEST_F(SolraStreamingTest, ShutdownAndReinit) {
  ASSERT_EQ(solra_streaming_init(nullptr), SOLRA_OK);
  solra_streaming_shutdown();

  /* Re-initialize should work */
  EXPECT_EQ(solra_streaming_init(nullptr), SOLRA_OK);
}
