#include "windows_platform.hpp"
#include <windows.h>
#include <shlobj.h>
#include <vector>

namespace solra::platform::windows {

static bool g_initialized = false;

bool initialize() {
    // Initialize COM for shell APIs
    HRESULT hr = CoInitializeEx(nullptr, COINIT_MULTITHREADED);
    g_initialized = SUCCEEDED(hr) || hr == S_FALSE || hr == RPC_E_CHANGED_MODE;
    return g_initialized;
}

void shutdown() {
    CoUninitialize();
    g_initialized = false;
}

bool isInitialized() { return g_initialized; }

// ---- Window ----
class Win32Window : public NativeWindow {
public:
    void* hwnd() const override { return hwnd_; }
    void* hinstance() const override { return hinst_; }
    void setTitle(const std::string& title) override {
        SetWindowTextA(static_cast<HWND>(hwnd_), title.c_str());
    }
    void setSize(int width, int height) override {
        SetWindowPos(static_cast<HWND>(hwnd_), nullptr, 0, 0, width, height,
                     SWP_NOMOVE | SWP_NOZORDER);
    }
    void show() override { ShowWindow(static_cast<HWND>(hwnd_), SW_SHOW); }
    void hide() override { ShowWindow(static_cast<HWND>(hwnd_), SW_HIDE); }
    bool shouldClose() const override { return shouldClose_; }
    void pollEvents() override {
        MSG msg;
        while (PeekMessage(&msg, static_cast<HWND>(hwnd_), 0, 0, PM_REMOVE)) {
            TranslateMessage(&msg);
            DispatchMessage(&msg);
            if (msg.message == WM_CLOSE) shouldClose_ = true;
        }
    }

private:
    void* hwnd_ = nullptr;
    void* hinst_ = nullptr;
    bool shouldClose_ = false;
};

std::unique_ptr<NativeWindow> createWindow(const WindowConfig& config) {
    // Stub: RegisterClassEx + CreateWindowEx
    (void)config;
    return nullptr;
}

// ---- Filesystem ----
std::string appDataPath() {
    char path[MAX_PATH];
    if (SUCCEEDED(SHGetFolderPathA(nullptr, CSIDL_APPDATA, nullptr, 0, path)))
        return std::string(path) + "\\Solra";
    return {};
}

std::string tempPath() {
    char path[MAX_PATH];
    GetTempPathA(MAX_PATH, path);
    return std::string(path) + "Solra";
}

std::string documentsPath() {
    char path[MAX_PATH];
    if (SUCCEEDED(SHGetFolderPathA(nullptr, CSIDL_PERSONAL, nullptr, 0, path)))
        return std::string(path) + "\\Solra";
    return {};
}

// ---- GPU ----
std::vector<GpuInfo> enumerateGpus() {
    // Stub: DXGI factory enumeration
    return {};
}

GpuInfo primaryGpu() {
    auto gpus = enumerateGpus();
    return gpus.empty() ? GpuInfo{} : gpus[0];
}

// ---- System Info ----
SystemInfo getSystemInfo() {
    SystemInfo info;
    SYSTEM_INFO si; GetSystemInfo(&si);
    info.cpuCoreCount = si.dwNumberOfProcessors;

    MEMORYSTATUSEX mem; mem.dwLength = sizeof(mem);
    if (GlobalMemoryStatusEx(&mem)) {
        info.totalRamBytes = mem.ullTotalPhys;
        info.availableRamBytes = mem.ullAvailPhys;
    }

    SYSTEM_POWER_STATUS ps;
    if (GetSystemPowerStatus(&ps) && ps.BatteryFlag != 128) {
        info.batteryPercent = ps.BatteryLifePercent;
        info.isLaptop = true;
    }
    return info;
}

InputState pollInput() {
    InputState state{};
    // Stub: GetKeyboardState + GetCursorPos
    return state;
}

NetworkType currentNetworkType() {
    // Stub: NetworkListManager
    return NetworkType::Unknown;
}

bool hasInternetConnection() {
    DWORD flags;
    return InternetGetConnectedState(&flags, 0);
}

} // namespace solra::platform::windows
