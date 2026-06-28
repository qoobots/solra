/*
 * Solra Core SDK - Android platform layer (stub)
 */

#include <solra/solra_core.h>
#include <spdlog/spdlog.h>
#include <android/log.h>
#include <jni.h>

extern "C" {

JNIEXPORT jint JNICALL
Java_com_solra_core_SolraCore_nativeInit(JNIEnv *env, jobject thiz, jstring data_path) {
  const char *path = env->GetStringUTFChars(data_path, nullptr);

  SolraCoreConfig config = {};
  config.data_path = path;
  config.display_width = 1080;
  config.display_height = 1920;
  config.target_fps = 60;
  config.enable_gpu = 1;
  config.log_level = 2;

  int result = solra_core_init(&config);
  env->ReleaseStringUTFChars(data_path, path);

  __android_log_print(ANDROID_LOG_INFO, "SolraCore", "Solra Core SDK initialized: %d", result);
  return result;
}

int solra_platform_android_is_nnapi_available(void) {
  /* TODO: Check for NNAPI runtime via Android NDK */
  return 1; /* Modern Android devices have NNAPI */
}

int solra_platform_android_is_vulkan_available(void) {
  /* TODO: Check for Vulkan loader */
  return 1;
}

} // extern "C"
