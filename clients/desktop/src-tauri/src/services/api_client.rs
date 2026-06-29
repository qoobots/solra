// HTTP/gRPC API 客户端
// 封装与后端服务的通信

use reqwest::Client;
use serde::de::DeserializeOwned;

/// API 客户端
pub struct ApiClient {
    http: Client,
    base_url: String,
}

impl ApiClient {
    pub fn new(base_url: &str) -> Self {
        Self {
            http: Client::new(),
            base_url: base_url.to_string(),
        }
    }

    /// GET 请求
    pub async fn get<T: DeserializeOwned>(&self, path: &str, token: Option<&str>) -> Result<T, String> {
        let url = format!("{}{}", self.base_url, path);
        let mut req = self.http.get(&url);

        if let Some(t) = token {
            req = req.header("Authorization", format!("Bearer {}", t));
        }

        let resp = req.send().await.map_err(|e| format!("请求失败: {}", e))?;
        let status = resp.status();

        if !status.is_success() {
            return Err(format!("HTTP {}: {}", status.as_u16(), resp.text().await.unwrap_or_default()));
        }

        resp.json::<T>().await.map_err(|e| format!("JSON解析失败: {}", e))
    }

    /// POST 请求
    pub async fn post<T: DeserializeOwned, B: serde::Serialize>(
        &self,
        path: &str,
        body: &B,
        token: Option<&str>,
    ) -> Result<T, String> {
        let url = format!("{}{}", self.base_url, path);
        let mut req = self.http.post(&url).json(body);

        if let Some(t) = token {
            req = req.header("Authorization", format!("Bearer {}", t));
        }

        let resp = req.send().await.map_err(|e| format!("请求失败: {}", e))?;
        let status = resp.status();

        if !status.is_success() {
            return Err(format!("HTTP {}: {}", status.as_u16(), resp.text().await.unwrap_or_default()));
        }

        resp.json::<T>().await.map_err(|e| format!("JSON解析失败: {}", e))
    }
}
