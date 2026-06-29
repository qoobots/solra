#pragma once
/**
 * @file model_ota.hpp
 * @brief 模型热更新 (Over-The-Air) — E-085
 *
 * 支持运行时模型下载、校验、热切换，不中断推理服务。
 * 工作流：版本检测 → 增量下载 → SHA256校验 → 原子切换 → 旧模型延迟淘汰
 */

#include <atomic>
#include <functional>
#include <memory>
#include <optional>
#include <string>
#include <string_view>
#include <unordered_map>
#include <vector>

#include <nlohmann/json.hpp>
using json = nlohmann::json;

namespace solra::inference {

// ── 模型元信息 ──────────────────────────────────────
struct ModelMeta {
    std::string model_id;        // 模型唯一标识，如 "qwen2.5-1.5b-gguf"
    std::string version;         // 语义版本，如 "2.1.0"
    std::string format;          // GGUF / ONNX / CoreML
    std::string sha256;          // 文件 SHA256
    int64_t size_bytes = 0;      // 模型文件大小
    std::string download_url;    // OTA 下载地址
    std::vector<std::string> tags; // 标签：端侧 / 云端 / 多模态

    json to_json() const {
        return {{"model_id", model_id},
                {"version", version},
                {"format", format},
                {"sha256", sha256},
                {"size_bytes", size_bytes},
                {"download_url", download_url},
                {"tags", tags}};
    }

    static ModelMeta from_json(const json& j) {
        ModelMeta m;
        m.model_id = j.at("model_id");
        m.version = j.at("version");
        m.format = j.value("format", "gguf");
        m.sha256 = j.at("sha256");
        m.size_bytes = j.value("size_bytes", 0);
        m.download_url = j.at("download_url");
        if (j.contains("tags")) m.tags = j["tags"].get<std::vector<std::string>>();
        return m;
    }
};

// ── 下载进度回调 ────────────────────────────────────
using ProgressCallback = std::function<void(int64_t downloaded, int64_t total)>;
using ReadyCallback = std::function<void(const std::string& model_id, const std::string& version)>;
using ErrorCallback = std::function<void(const std::string& error)>;

// ── 模型 OTA 管理器 ─────────────────────────────────
class ModelOTAManager {
public:
    static ModelOTAManager& instance();

    // 初始化：设置本地模型目录 & 注册中心 URL
    bool init(std::string_view local_model_dir,
              std::string_view registry_url,
              std::string_view platform = "android");

    // 检查更新：对比本地版本与注册中心版本
    std::vector<ModelMeta> check_updates();

    // 下载模型到本地存储
    bool download_model(const ModelMeta& meta,
                        ProgressCallback on_progress = nullptr,
                        int timeout_seconds = 300);

    // 下载并安装（校验 + 热切换）
    bool install_model(const ModelMeta& meta,
                       ProgressCallback on_progress = nullptr,
                       ReadyCallback on_ready = nullptr,
                       ErrorCallback on_error = nullptr);

    // 热切换到指定版本（需模型已下载）
    bool hotswap(const std::string& model_id, const std::string& version);

    // 回滚到上一个版本
    bool rollback(const std::string& model_id);

    // 获取当前活跃模型版本
    std::optional<std::string> active_version(const std::string& model_id) const;

    // 获取本地已安装模型列表
    std::vector<ModelMeta> installed_models() const;

    // 清理过期模型（保留最近 N 个版本）
    int cleanup_old_versions(const std::string& model_id, int keep_recent = 3);

    // 后台自动更新（定时检查 + 自动下载）
    void start_auto_update(int check_interval_seconds = 3600);
    void stop_auto_update();

    // 是否正在更新
    bool is_updating(const std::string& model_id) const;

private:
    ModelOTAManager() = default;
    ~ModelOTAManager();

    ModelOTAManager(const ModelOTAManager&) = delete;
    ModelOTAManager& operator=(const ModelOTAManager&) = delete;

    // 内部实现
    bool validate_sha256(const std::string& file_path, const std::string& expected_hash);
    json fetch_registry_manifest();
    std::string make_model_path(const std::string& model_id, const std::string& version) const;
    bool atomic_symlink_switch(const std::string& model_id, const std::string& version);

    struct Impl;
    std::unique_ptr<Impl> impl_;
};

// ── 平台特定下载器接口 ──────────────────────────────
class IModelDownloader {
public:
    virtual ~IModelDownloader() = default;

    // 带断点续传的下载
    virtual bool download(const std::string& url,
                          const std::string& dest_path,
                          const std::string& expected_sha256,
                          ProgressCallback on_progress = nullptr,
                          int timeout_seconds = 300) = 0;

    // 取消下载
    virtual void cancel() = 0;

    // 下载速度 (bytes/s)
    virtual int64_t download_speed() const = 0;
};

// Android 平台使用 OkHttp + 断点续传
// iOS 平台使用 URLSession + background download
// Desktop 使用 libcurl

}  // namespace solra::inference
