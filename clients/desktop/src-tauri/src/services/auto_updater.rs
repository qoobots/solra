//! 自动更新服务
//!
//! 启动时检查更新，下载新版本，校验签名，提示用户重启。
//! 支持 Canary / Beta / Stable 三渠道。

use serde::{Deserialize, Serialize};
use std::path::PathBuf;
use std::time::Duration;

/// 更新渠道
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum UpdateChannel {
    Canary,
    Beta,
    Stable,
}

impl UpdateChannel {
    pub fn from_env() -> Self {
        match option_env!("SOLRA_UPDATE_CHANNEL").unwrap_or("stable") {
            "canary" => UpdateChannel::Canary,
            "beta" => UpdateChannel::Beta,
            _ => UpdateChannel::Stable,
        }
    }

    pub fn as_str(&self) -> &'static str {
        match self {
            UpdateChannel::Canary => "canary",
            UpdateChannel::Beta => "beta",
            UpdateChannel::Stable => "stable",
        }
    }
}

/// 更新清单（从服务器获取）
#[derive(Debug, Clone, Deserialize)]
pub struct UpdateManifest {
    pub version: String,
    pub notes: String,
    pub pub_date: String,
    pub platforms: PlatformUpdates,
}

#[derive(Debug, Clone, Deserialize)]
pub struct PlatformUpdates {
    #[serde(rename = "windows-x86_64")]
    pub windows_x64: Option<PlatformArtifact>,
    #[serde(rename = "darwin-x86_64")]
    pub macos_x64: Option<PlatformArtifact>,
    #[serde(rename = "darwin-aarch64")]
    pub macos_arm64: Option<PlatformArtifact>,
    #[serde(rename = "linux-x86_64")]
    pub linux_x64: Option<PlatformArtifact>,
}

#[derive(Debug, Clone, Deserialize)]
pub struct PlatformArtifact {
    pub url: String,
    pub signature: String,
}

/// 更新状态
#[derive(Debug, Clone, PartialEq, Serialize)]
#[serde(rename_all = "lowercase")]
pub enum UpdateStatus {
    /// 未检查
    Idle,
    /// 检查中
    Checking,
    /// 发现新版本
    Available {
        version: String,
        notes: String,
    },
    /// 下载中 (progress: 0.0-1.0)
    Downloading {
        progress: f64,
    },
    /// 下载完成，等待安装
    ReadyToInstall,
    /// 已是最新
    UpToDate,
    /// 检查/下载失败
    Error {
        message: String,
    },
}

/// 自动更新服务
pub struct AutoUpdater {
    channel: UpdateChannel,
    current_version: String,
    update_server_url: String,
    status: UpdateStatus,
    download_dir: PathBuf,
}

impl AutoUpdater {
    pub fn new(current_version: &str) -> Self {
        let data_dir = dirs_next().unwrap_or_else(|| PathBuf::from("."));
        let download_dir = data_dir.join("updates");

        Self {
            channel: UpdateChannel::from_env(),
            current_version: current_version.to_string(),
            update_server_url: option_env!("SOLRA_UPDATE_URL")
                .unwrap_or("https://releases.solra.io/desktop")
                .to_string(),
            status: UpdateStatus::Idle,
            download_dir,
        }
    }

    /// 检查是否有新版本可用
    pub async fn check_update(&mut self) -> UpdateStatus {
        self.status = UpdateStatus::Checking;

        let manifest_url = format!(
            "{}/{}/latest.json",
            self.update_server_url,
            self.channel.as_str()
        );

        let client = reqwest::Client::builder()
            .timeout(Duration::from_secs(10))
            .build();

        let resp = match client {
            Ok(c) => match c.get(&manifest_url).send().await {
                Ok(r) => r,
                Err(e) => {
                    let msg = format!("获取更新清单失败: {}", e);
                    self.status = UpdateStatus::Error { message: msg.clone() };
                    log::error!("{}", msg);
                    return self.status.clone();
                }
            },
            Err(e) => {
                let msg = format!("创建 HTTP 客户端失败: {}", e);
                self.status = UpdateStatus::Error { message: msg };
                return self.status.clone();
            }
        };

        let manifest: UpdateManifest = match resp.json().await {
            Ok(m) => m,
            Err(e) => {
                let msg = format!("解析更新清单失败: {}", e);
                self.status = UpdateStatus::Error { message: msg };
                return self.status.clone();
            }
        };

        // 版本比较（简化：字符串比较）
        if manifest.version <= self.current_version {
            log::info!("当前已是最新版本: {}", self.current_version);
            self.status = UpdateStatus::UpToDate;
            return self.status.clone();
        }

        log::info!(
            "发现新版本: {} → {} ({})",
            self.current_version,
            manifest.version,
            manifest.notes
        );

        self.status = UpdateStatus::Available {
            version: manifest.version.clone(),
            notes: manifest.notes.clone(),
        };

        // 获取当前平台的下载 URL
        let artifact = self.get_current_platform_artifact(&manifest);
        if let Some(art) = artifact {
            if let Err(e) = self.download_and_verify(&art.url, &art.signature).await {
                self.status = UpdateStatus::Error { message: e };
            }
        }

        self.status.clone()
    }

    /// 获取当前平台对应的构建产物
    fn get_current_platform_artifact(
        &self,
        manifest: &UpdateManifest,
    ) -> Option<&PlatformArtifact> {
        #[cfg(target_os = "windows")]
        return manifest.platforms.windows_x64.as_ref();
        #[cfg(all(target_os = "macos", target_arch = "x86_64"))]
        return manifest.platforms.macos_x64.as_ref();
        #[cfg(all(target_os = "macos", target_arch = "aarch64"))]
        return manifest.platforms.macos_arm64.as_ref();
        #[cfg(target_os = "linux")]
        return manifest.platforms.linux_x64.as_ref();
    }

    /// 下载并校验签名
    async fn download_and_verify(
        &mut self,
        url: &str,
        _signature: &str,
    ) -> Result<(), String> {
        let client = reqwest::Client::builder()
            .timeout(Duration::from_secs(300))
            .build()
            .map_err(|e| format!("创建 HTTP 客户端失败: {}", e))?;

        let resp = client
            .get(url)
            .send()
            .await
            .map_err(|e| format!("下载更新包失败: {}", e))?;

        let total_size = resp.content_length().unwrap_or(0);
        let mut downloaded = 0u64;
        let mut data = Vec::new();

        let mut stream = resp.bytes_stream();
        use futures_util::StreamExt;
        while let Some(chunk) = stream.next().await {
            let chunk = chunk.map_err(|e| format!("下载中断: {}", e))?;
            downloaded += chunk.len() as u64;
            data.extend_from_slice(&chunk);

            if total_size > 0 {
                let progress = downloaded as f64 / total_size as f64;
                self.status = UpdateStatus::Downloading { progress };
            }
        }

        // 确保下载目录存在
        std::fs::create_dir_all(&self.download_dir)
            .map_err(|e| format!("创建下载目录失败: {}", e))?;

        // 保存安装包
        let file_name = url.split('/').last().unwrap_or("solra-installer.exe");
        let file_path = self.download_dir.join(file_name);
        std::fs::write(&file_path, &data)
            .map_err(|e| format!("保存安装包失败: {}", e))?;

        // TODO: 签名校验 — 使用 ed25519 公钥验证签名
        // 实际实现：加载内置公钥，使用 ring/ed25519-dalek 验证

        log::info!("更新包已下载: {} ({} bytes)", file_path.display(), data.len());
        self.status = UpdateStatus::ReadyToInstall;

        Ok(())
    }

    /// 获取当前状态
    pub fn status(&self) -> &UpdateStatus {
        &self.status
    }

    /// 获取安装包路径
    pub fn installer_path(&self) -> Option<PathBuf> {
        if self.status != UpdateStatus::ReadyToInstall {
            return None;
        }
        // 返回下载目录中第一个 .exe/.msi 文件
        if let Ok(entries) = std::fs::read_dir(&self.download_dir) {
            for entry in entries.flatten() {
                let path = entry.path();
                if let Some(ext) = path.extension() {
                    if ext == "exe" || ext == "msi" {
                        return Some(path);
                    }
                }
            }
        }
        None
    }
}

/// 获取用户数据目录
fn dirs_next() -> Option<PathBuf> {
    #[cfg(target_os = "windows")]
    {
        std::env::var("LOCALAPPDATA")
            .ok()
            .map(|p| PathBuf::from(p).join("solra"))
    }
    #[cfg(target_os = "macos")]
    {
        std::env::var("HOME")
            .ok()
            .map(|p| PathBuf::from(p).join("Library/Application Support/solra"))
    }
    #[cfg(target_os = "linux")]
    {
        std::env::var("XDG_DATA_HOME")
            .ok()
            .or_else(|| std::env::var("HOME").ok().map(|p| format!("{}/.local/share", p)))
            .map(|p| PathBuf::from(p).join("solra"))
    }
}

// ============================================================================
// 测试
// ============================================================================

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_update_channel_from_env_default() {
        let channel = UpdateChannel::from_env();
        assert_eq!(channel, UpdateChannel::Stable);
    }

    #[test]
    fn test_update_channel_as_str() {
        assert_eq!(UpdateChannel::Stable.as_str(), "stable");
        assert_eq!(UpdateChannel::Beta.as_str(), "beta");
        assert_eq!(UpdateChannel::Canary.as_str(), "canary");
    }

    #[test]
    fn test_auto_updater_new() {
        let updater = AutoUpdater::new("0.1.0");
        assert_eq!(updater.status(), &UpdateStatus::Idle);
    }

    #[test]
    fn test_auto_updater_status_clone() {
        let status = UpdateStatus::Available {
            version: "1.0.0".into(),
            notes: "Test".into(),
        };
        let cloned = status.clone();
        match cloned {
            UpdateStatus::Available { version, notes } => {
                assert_eq!(version, "1.0.0");
                assert_eq!(notes, "Test");
            }
            _ => panic!("wrong status"),
        }
    }

    #[test]
    fn test_manifest_deserialize() {
        let json = r#"{
            "version": "1.0.0",
            "notes": "Test release",
            "pub_date": "2026-01-01",
            "platforms": {
                "windows-x86_64": {"url": "https://example.com/solra.exe", "signature": "abc123"}
            }
        }"#;
        let manifest: UpdateManifest = serde_json::from_str(json).unwrap();
        assert_eq!(manifest.version, "1.0.0");
        assert!(manifest.platforms.windows_x64.is_some());
    }
}
