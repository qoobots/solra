#pragma once
// Windows Platform Layer: editor/debugging support + DirectX backend
#include <cstdint>
#include <memory>
#include <string>
#include <vector>

namespace solra::platform::windows {

// ---- Platform initialization ----
bool initialize();
void shutdown();
bool isInitialized();

// ---- Window management (editor/debug) ----
struct WindowConfig {
    std::string title = "Solra Debug";
    int width = 1280, height = 720;
    bool resizable = true;
    bool fullscreen = false;
};

class NativeWindow {
public:
    virtual ~NativeWindow() = default;
    virtual void* hwnd() const = 0;         // HWND
    virtual void* hinstance() const = 0;    // HINSTANCE
    virtual void setTitle(const std::string& title) = 0;
    virtual void setSize(int width, int height) = 0;
    virtual void show() = 0;
    virtual void hide() = 0;
    virtual bool shouldClose() const = 0;
    virtual void pollEvents() = 0;
};

std::unique_ptr<NativeWindow> createWindow(const WindowConfig& config);

// ---- Filesystem ----
std::string appDataPath();       // %APPDATA%/Solra
std::string tempPath();          // %TEMP%/Solra
std::string documentsPath();     // %USERPROFILE%/Documents/Solra

// ---- GPU detection ----
struct GpuInfo {
    std::string name;
    std::string vendor;
    uint64_t vramBytes = 0;
    bool supportsRayTracing = false;
    bool supportsMeshShaders = false;
};

std::vector<GpuInfo> enumerateGpus();
GpuInfo primaryGpu();

// ---- Performance ----
struct SystemInfo {
    std::string osVersion;
    uint32_t cpuCoreCount = 0;
    uint64_t totalRamBytes = 0;
    uint64_t availableRamBytes = 0;
    float cpuUsagePercent = 0;
    int batteryPercent = -1; // -1 = desktop (no battery)
    bool isLaptop = false;
};

SystemInfo getSystemInfo();

// ---- Input ----
enum class KeyCode { W,A,S,D, Space, Shift, Ctrl, Alt, Escape, Enter, /* ... */ Count };
enum class MouseButton { Left, Right, Middle };

struct InputState {
    bool keysDown[256] = {};
    float mouseX = 0, mouseY = 0;
    float mouseDeltaX = 0, mouseDeltaY = 0;
    float scrollDelta = 0;
    bool mouseButtons[3] = {};
};

InputState pollInput();

// ---- Networking ----
enum class NetworkType { None, Ethernet, WiFi, Cellular, Unknown };
NetworkType currentNetworkType();
bool hasInternetConnection();

} // namespace solra::platform::windows
