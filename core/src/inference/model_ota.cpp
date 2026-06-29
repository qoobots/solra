#include "model_ota.hpp"

#include <algorithm>
#include <chrono>
#include <cstring>
#include <filesystem>
#include <fstream>
#include <mutex>
#include <thread>

#include <nlohmann/json.hpp>

// SHA256 via OpenSSL / platform
#if defined(__APPLE__)
#include <CommonCrypto/CommonDigest.h>
#define SHA256_CTX CC_SHA256_CTX
#define SHA256_Init CC_SHA256_Init
#define SHA256_Update CC_SHA256_Update
#define SHA256_Final CC_SHA256_Final
#else
#include <openssl/sha.h>
#endif

namespace fs = std::filesystem;
using json = nlohmann::json;

namespace solra::inference {

// ── Impl ────────────────────────────────────────────
struct ModelOTAManager::Impl {
    std::string local_model_dir;
    std::string registry_url;
    std::string platform;

    struct VersionEntry {
        std::string version;
        fs::path path;
        std::chrono::system_clock::time_point installed_at;
    };

    std::mutex mtx;
    // model_id → 版本列表 (最新在前)
    std::unordered_map<std::string, std::vector<VersionEntry>> versions;
    // model_id → 当前活跃版本
    std::unordered_map<std::string, std::string> active;
    // model_id → 是否正在更新
    std::unordered_map<std::string, bool> updating;
    // 后台更新线程
    std::unique_ptr<std::thread> auto_update_thread;
    std::atomic<bool> auto_update_running{false};

    // 本地 manifest 缓存
    json cached_manifest;
    std::chrono::system_clock::time_point manifest_fetched_at;
};

// ── 单例 ────────────────────────────────────────────
ModelOTAManager& ModelOTAManager::instance() {
    static ModelOTAManager mgr;
    return mgr;
}

ModelOTAManager::~ModelOTAManager() {
    stop_auto_update();
}

// ── 初始化 ──────────────────────────────────────────
bool ModelOTAManager::init(std::string_view local_model_dir,
                           std::string_view registry_url,
                           std::string_view platform) {
    impl_ = std::make_unique<Impl>();
    impl_->local_model_dir = local_model_dir;
    impl_->registry_url = registry_url;
    impl_->platform = platform;

    fs::create_directories(impl_->local_model_dir);

    // 扫描本地已安装模型
    for (const auto& entry : fs::directory_iterator(impl_->local_model_dir)) {
        if (!entry.is_directory()) continue;

        auto version_file = entry.path() / "version.json";
        if (!fs::exists(version_file)) continue;

        try {
            std::ifstream f(version_file);
            json j = json::parse(f);
            auto meta = ModelMeta::from_json(j);

            Impl::VersionEntry ve;
            ve.version = meta.version;
            ve.path = entry.path();
            ve.installed_at = std::chrono::system_clock::now();

            impl_->active[meta.model_id] = meta.version;
            impl_->versions[meta.model_id].push_back(std::move(ve));
        } catch (...) {
            // Skip corrupted installs
        }
    }

    return true;
}

// ── 检查更新 ────────────────────────────────────────
json ModelOTAManager::fetch_registry_manifest() {
    // TODO: HTTP GET impl_->registry_url + "/manifest.json"
    // 此处为骨架，实际使用平台特定的 HTTP 客户端
    return json::object();
}

std::vector<ModelMeta> ModelOTAManager::check_updates() {
    std::vector<ModelMeta> updates;

    try {
        json manifest = fetch_registry_manifest();
        auto models = manifest.value("models", json::array());

        std::lock_guard<std::mutex> lock(impl_->mtx);

        for (const auto& model_json : models) {
            auto remote = ModelMeta::from_json(model_json);
            auto it = impl_->active.find(remote.model_id);

            if (it == impl_->active.end() || it->second != remote.version) {
                updates.push_back(std::move(remote));
            }
        }
    } catch (...) {
        // Network errors are expected; return empty
    }

    return updates;
}

// ── 下载模型 ────────────────────────────────────────
bool ModelOTAManager::download_model(const ModelMeta& meta,
                                     ProgressCallback on_progress,
                                     int timeout_seconds) {
    std::lock_guard<std::mutex> lock(impl_->mtx);
    impl_->updating[meta.model_id] = true;

    std::string dest_path = make_model_path(meta.model_id, meta.version);
    fs::create_directories(dest_path);

    // TODO: Platform-specific download with resume support
    // IModelDownloader::download(meta.download_url, dest_path + "/model.bin",
    //                             meta.sha256, on_progress, timeout_seconds);

    // Write version.json
    json meta_json = meta.to_json();
    std::ofstream f(dest_path + "/version.json");
    f << meta_json.dump(2);

    impl_->updating[meta.model_id] = false;
    return true;
}

// ── 安装 (下载 + 校验 + 热切换) ─────────────────────
bool ModelOTAManager::install_model(const ModelMeta& meta,
                                    ProgressCallback on_progress,
                                    ReadyCallback on_ready,
                                    ErrorCallback on_error) {
    try {
        if (!download_model(meta, on_progress)) {
            if (on_error) on_error("Download failed for " + meta.model_id);
            return false;
        }

        std::string model_path = make_model_path(meta.model_id, meta.version);

        if (!validate_sha256(model_path + "/model.bin", meta.sha256)) {
            if (on_error) on_error("SHA256 mismatch for " + meta.model_id);
            return false;
        }

        if (!hotswap(meta.model_id, meta.version)) {
            if (on_error) on_error("Hotswap failed for " + meta.model_id);
            return false;
        }

        if (on_ready) on_ready(meta.model_id, meta.version);
        return true;
    } catch (const std::exception& e) {
        if (on_error) on_error(e.what());
        return false;
    }
}

// ── 热切换 ──────────────────────────────────────────
bool ModelOTAManager::hotswap(const std::string& model_id,
                              const std::string& version) {
    std::lock_guard<std::mutex> lock(impl_->mtx);

    // 原子符号链接切换
    if (!atomic_symlink_switch(model_id, version)) {
        return false;
    }

    // 更新活跃版本记录
    std::string old_version = impl_->active[model_id];
    impl_->active[model_id] = version;

    // 记录新版本
    Impl::VersionEntry ve;
    ve.version = version;
    ve.path = make_model_path(model_id, version);
    ve.installed_at = std::chrono::system_clock::now();

    auto& vec = impl_->versions[model_id];
    vec.insert(vec.begin(), std::move(ve));

    return true;
}

// ── 回滚 ────────────────────────────────────────────
bool ModelOTAManager::rollback(const std::string& model_id) {
    std::lock_guard<std::mutex> lock(impl_->mtx);

    auto it = impl_->versions.find(model_id);
    if (it == impl_->versions.end() || it->second.size() < 2) {
        return false; // 没有可回滚的版本
    }

    // 切换到第二个版本 (索引1 = 上一个版本)
    const auto& prev = it->second[1];
    return hotswap(model_id, prev.version);
}

// ── 活跃版本 ────────────────────────────────────────
std::optional<std::string> ModelOTAManager::active_version(
    const std::string& model_id) const {
    std::lock_guard<std::mutex> lock(impl_->mtx);
    auto it = impl_->active.find(model_id);
    if (it != impl_->active.end()) return it->second;
    return std::nullopt;
}

// ── 已安装模型 ──────────────────────────────────────
std::vector<ModelMeta> ModelOTAManager::installed_models() const {
    std::vector<ModelMeta> models;
    std::lock_guard<std::mutex> lock(impl_->mtx);

    for (const auto& [model_id, ver_list] : impl_->versions) {
        if (ver_list.empty()) continue;
        try {
            auto version_file = ver_list[0].path / "version.json";
            if (fs::exists(version_file)) {
                std::ifstream f(version_file);
                models.push_back(ModelMeta::from_json(json::parse(f)));
            }
        } catch (...) {}
    }
    return models;
}

// ── 清理旧版本 ──────────────────────────────────────
int ModelOTAManager::cleanup_old_versions(const std::string& model_id,
                                          int keep_recent) {
    std::lock_guard<std::mutex> lock(impl_->mtx);
    int removed = 0;

    auto it = impl_->versions.find(model_id);
    if (it == impl_->versions.end()) return 0;

    auto& vec = it->second;
    std::string active_ver = impl_->active[model_id];

    for (size_t i = keep_recent; i < vec.size(); ) {
        if (vec[i].version != active_ver) {
            fs::remove_all(vec[i].path);
            vec.erase(vec.begin() + i);
            removed++;
        } else {
            i++;
        }
    }

    return removed;
}

// ── 后台自动更新 ────────────────────────────────────
void ModelOTAManager::start_auto_update(int check_interval_seconds) {
    if (impl_->auto_update_running) return;

    impl_->auto_update_running = true;
    impl_->auto_update_thread = std::make_unique<std::thread>(
        [this, check_interval_seconds]() {
            while (impl_->auto_update_running) {
                try {
                    auto updates = check_updates();
                    for (const auto& meta : updates) {
                        if (meta.tags.empty() ||
                            std::find(meta.tags.begin(), meta.tags.end(),
                                      "auto-update") != meta.tags.end()) {
                            download_model(meta);
                        }
                    }
                } catch (...) {}

                // 间隔检查
                for (int i = 0;
                     i < check_interval_seconds && impl_->auto_update_running;
                     i++) {
                    std::this_thread::sleep_for(std::chrono::seconds(1));
                }
            }
        });
}

void ModelOTAManager::stop_auto_update() {
    impl_->auto_update_running = false;
    if (impl_->auto_update_thread && impl_->auto_update_thread->joinable()) {
        impl_->auto_update_thread->join();
    }
    impl_->auto_update_thread.reset();
}

// ── 查询状态 ────────────────────────────────────────
bool ModelOTAManager::is_updating(const std::string& model_id) const {
    std::lock_guard<std::mutex> lock(impl_->mtx);
    auto it = impl_->updating.find(model_id);
    return it != impl_->updating.end() && it->second;
}

// ── 私有辅助 ────────────────────────────────────────
std::string ModelOTAManager::make_model_path(const std::string& model_id,
                                              const std::string& version) const {
    return impl_->local_model_dir + "/" + model_id + "/" + version;
}

bool ModelOTAManager::validate_sha256(const std::string& file_path,
                                       const std::string& expected_hash) {
    std::ifstream file(file_path, std::ios::binary);
    if (!file) return false;

    unsigned char hash[SHA256_DIGEST_LENGTH];
    SHA256_CTX ctx;
    SHA256_Init(&ctx);

    char buf[8192];
    while (file.read(buf, sizeof(buf)) || file.gcount() > 0) {
        SHA256_Update(&ctx, buf, file.gcount());
    }

    SHA256_Final(hash, &ctx);

    // 转为 hex string
    char hex[65];
    for (int i = 0; i < SHA256_DIGEST_LENGTH; i++) {
        snprintf(hex + i * 2, 3, "%02x", hash[i]);
    }

    return strncmp(hex, expected_hash.c_str(), 64) == 0;
}

bool ModelOTAManager::atomic_symlink_switch(const std::string& model_id,
                                             const std::string& version) {
    std::string current_link =
        impl_->local_model_dir + "/" + model_id + "/current";
    std::string target_path = make_model_path(model_id, version);
    std::string tmp_link = current_link + ".tmp";

    try {
        // 原子操作：先建临时链接，再 rename
        fs::remove(tmp_link);
        fs::create_symlink(target_path, tmp_link);
        fs::rename(tmp_link, current_link);
        return true;
    } catch (const fs::filesystem_error&) {
        return false;
    }
}

}  // namespace solra::inference
