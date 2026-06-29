#include "macos_platform.hpp"
#include <cstdlib>
#include <sys/sysctl.h>
#include <mach/mach.h>

namespace solra::platform::macos {

static bool g_initialized = false;

bool initialize() {
    g_initialized = true;
    return true;
}

void shutdown() { g_initialized = false; }
bool isInitialized() { return g_initialized; }

// ---- Metal ----
std::vector<MetalDeviceInfo> enumerateMetalDevices() {
    // Stub: MTLCopyAllDevices() via Objective-C bridge
    // Production: uses Metal.framework C API or Objective-C++ wrapper
    return {};
}

MetalDeviceInfo defaultMetalDevice() {
    // MTLCreateSystemDefaultDevice()
    return {};
}

// ---- Window ----
std::unique_ptr<MacWindow> createWindow(const MacWindowConfig& config) {
    // Stub: NSWindow + NSView + CAMetalLayer via Objective-C
    (void)config;
    return nullptr;
}

// ---- Filesystem ----
std::string applicationSupportPath() {
    const char* home = getenv("HOME");
    if (home) return std::string(home) + "/Library/Application Support/Solra";
    return {};
}

std::string cachesPath() {
    const char* home = getenv("HOME");
    if (home) return std::string(home) + "/Library/Caches/Solra";
    return {};
}

std::string documentsPath() {
    const char* home = getenv("HOME");
    if (home) return std::string(home) + "/Documents/Solra";
    return {};
}

// ---- System Info ----
MacSystemInfo getSystemInfo() {
    MacSystemInfo info;

    // OS version via sw_vers
    // Production: [[NSProcessInfo processInfo] operatingSystemVersionString]
    info.osVersion = "macOS";

    // CPU info
    size_t size = sizeof(info.cpuCoreCount);
    sysctlbyname("hw.logicalcpu", &info.cpuCoreCount, &size, nullptr, 0);

    size = sizeof(info.performanceCores);
    sysctlbyname("hw.perflevel0.logicalcpu", &info.performanceCores, &size, nullptr, 0);

    size = sizeof(info.efficiencyCores);
    sysctlbyname("hw.perflevel1.logicalcpu", &info.efficiencyCores, &size, nullptr, 0);

    // Memory
    size = sizeof(info.totalRamBytes);
    sysctlbyname("hw.memsize", &info.totalRamBytes, &size, nullptr, 0);

    // Available memory
    mach_port_t host = mach_host_self();
    vm_size_t pageSize;
    host_page_size(host, &pageSize);

    vm_statistics64_data_t vmStats;
    mach_msg_type_number_t count = HOST_VM_INFO64_COUNT;
    if (host_statistics64(host, HOST_VM_INFO64, (host_info_t)&vmStats, &count) == KERN_SUCCESS) {
        info.availableRamBytes = (vmStats.free_count + vmStats.inactive_count) * pageSize;
    }

    return info;
}

// ---- CI ----
bool isRunningInCI() {
    return getenv("CI") != nullptr ||
           getenv("JENKINS_HOME") != nullptr ||
           getenv("GITHUB_ACTIONS") != nullptr;
}

void setEnv(const std::string& key, const std::string& value) {
    setenv(key.c_str(), value.c_str(), 1);
}

std::string executablePath() {
    // Stub: [[NSBundle mainBundle] executablePath]
    return {};
}

std::string bundleResourcePath() {
    // Stub: [[NSBundle mainBundle] resourcePath]
    return {};
}

} // namespace solra::platform::macos
