#pragma once
// macOS Platform Layer: CI support + Metal rendering backend
#include <cstdint>
#include <string>
#include <vector>
#include <memory>

namespace solra::platform::macos {

// ---- Platform lifecycle ----
bool initialize();
void shutdown();
bool isInitialized();

// ---- Metal device query ----
struct MetalDeviceInfo {
    std::string name;               // "Apple M3 Pro"
    std::string gpuFamily;          // "Apple9"
    uint64_t vramBytes = 0;         // unified memory allocated for GPU
    bool isHeadless = false;        // CI / server mode (no display)
    bool supportsRayTracing = false;
    bool supportsArgumentBuffersTier2 = false;
};

std::vector<MetalDeviceInfo> enumerateMetalDevices();
MetalDeviceInfo defaultMetalDevice();

// ---- Window management (for CI/debug builds) ----
struct MacWindowConfig {
    std::string title = "Solra";
    int width = 1280, height = 720;
    bool resizable = true;
    bool metal = true; // CA::MetalLayer
};

class MacWindow {
public:
    virtual ~MacWindow() = default;
    virtual void* nsWindow() const = 0;     // NSWindow*
    virtual void* metalLayer() const = 0;   // CAMetalLayer*
    virtual void setTitle(const std::string& title) = 0;
    virtual void setSize(int width, int height) = 0;
    virtual void show() = 0;
    virtual void hide() = 0;
    virtual void pollEvents() = 0;
};

std::unique_ptr<MacWindow> createWindow(const MacWindowConfig& config);

// ---- Filesystem ----
std::string applicationSupportPath();  // ~/Library/Application Support/Solra
std::string cachesPath();              // ~/Library/Caches/Solra
std::string documentsPath();           // ~/Documents/Solra

// ---- System Info ----
struct MacSystemInfo {
    std::string osVersion;             // "14.5"
    std::string modelIdentifier;       // "Mac15,6"
    std::string cpuName;               // "Apple M3 Pro"
    uint32_t cpuCoreCount = 0;
    uint32_t performanceCores = 0;
    uint32_t efficiencyCores = 0;
    uint64_t totalRamBytes = 0;
    uint64_t availableRamBytes = 0;
    int batteryPercent = -1;           // -1 = desktop
    bool isLaptop = false;
};

MacSystemInfo getSystemInfo();

// ---- CI utilities ----
bool isRunningInCI();                  // check CI=true or JENKINS_HOME
void setEnv(const std::string& key, const std::string& value);

// ---- Process ----
std::string executablePath();
std::string bundleResourcePath();

} // namespace solra::platform::macos
