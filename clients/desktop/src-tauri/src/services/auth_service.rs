// 认证服务
// 管理 JWT Token 的存储、刷新、过期处理

use serde::{Deserialize, Serialize};
use std::sync::Mutex;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AuthTokens {
    pub access_token: String,
    pub refresh_token: String,
    pub expires_at: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UserProfile {
    pub id: String,
    pub username: String,
    pub display_name: String,
    pub avatar_url: Option<String>,
    pub subscription_tier: String,
}

/// 认证状态管理
pub struct AuthService {
    tokens: Mutex<Option<AuthTokens>>,
    profile: Mutex<Option<UserProfile>>,
}

impl AuthService {
    pub fn new() -> Self {
        Self {
            tokens: Mutex::new(None),
            profile: Mutex::new(None),
        }
    }

    /// 是否已登录
    pub fn is_authenticated(&self) -> bool {
        self.tokens.lock().unwrap().is_some()
    }

    /// 设置 Token
    pub fn set_tokens(&self, tokens: AuthTokens) {
        *self.tokens.lock().unwrap() = Some(tokens);
    }

    /// 获取 Access Token
    pub fn get_access_token(&self) -> Option<String> {
        self.tokens.lock().unwrap()
            .as_ref()
            .map(|t| t.access_token.clone())
    }

    /// 设置用户信息
    pub fn set_profile(&self, profile: UserProfile) {
        *self.profile.lock().unwrap() = Some(profile);
    }

    /// 登出
    pub fn logout(&self) {
        *self.tokens.lock().unwrap() = None;
        *self.profile.lock().unwrap() = None;
    }
}
