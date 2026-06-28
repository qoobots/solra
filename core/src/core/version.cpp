/*
 * Solra Core SDK - Version information
 */

#include <solra/solra_core.h>

static const char version_string[] = "0.1.0";

const char *solra_core_get_version(void) {
  return version_string;
}

/* Error string lookup */
const char *solra_error_string(int error_code) {
  switch (error_code) {
    case 0:  return "OK";
    case -1:  return "Unknown error";
    case -2:  return "Invalid argument";
    case -3:  return "Not initialized";
    case -4:  return "Already initialized";
    case -5:  return "Out of memory";
    case -6:  return "I/O error";
    case -7:  return "Network error";
    case -8:  return "Timeout";
    case -9:  return "Unsupported operation";
    case -10: return "GPU unavailable";
    case -11: return "NPU unavailable";
    case -12: return "Model load failed";
    case -13: return "Inference failed";
    case -14: return "Asset not found";
    case -15: return "Cache full";
    default:  return "Unknown";
  }
}
