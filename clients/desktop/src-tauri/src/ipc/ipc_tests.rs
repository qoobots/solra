#[cfg(test)]
mod tests {
    use super::super::system_cmd::*;

    #[test]
    fn test_system_info_defaults() {
        // 验证 SystemInfo 结构体可以构造
        let info = SystemInfo {
            os: "windows".into(),
            arch: "x86_64".into(),
            cpu_cores: 8,
            total_memory_gb: 16.0,
            gpu_name: "Test GPU".into(),
            core_sdk_loaded: false,
            core_sdk_version: "0.1.0".into(),
        };
        assert_eq!(info.os, "windows");
        assert_eq!(info.cpu_cores, 8);
        assert!(!info.core_sdk_loaded);
    }

    #[test]
    fn test_profile_update_request() {
        let req = ProfileUpdateRequest {
            display_name: Some("TestUser".into()),
            bio: Some("Hello world".into()),
            avatar_url: None,
        };
        assert_eq!(req.display_name.as_deref(), Some("TestUser"));
        assert!(req.avatar_url.is_none());
    }

    #[test]
    fn test_store_item_serialization() {
        let item = StoreItem {
            id: "test-001".into(),
            name: "测试道具".into(),
            description: "用于测试的道具".into(),
            price: 9.99,
            currency: "CNY".into(),
            category: "prop".into(),
            thumbnail_url: "https://example.com/img.jpg".into(),
        };

        let json = serde_json::to_string(&item).expect("序列化失败");
        let back: StoreItem = serde_json::from_str(&json).expect("反序列化失败");
        assert_eq!(back.id, "test-001");
        assert_eq!(back.price, 9.99);
    }

    #[test]
    fn test_message_item_read_status() {
        let msg = MessageItem {
            id: "msg-001".into(),
            msg_type: "system".into(),
            title: "Welcome".into(),
            content: "Welcome to Solra!".into(),
            sender: "System".into(),
            timestamp: "2026-01-01T00:00:00Z".into(),
            read: false,
        };
        assert!(!msg.read);
        assert_eq!(msg.msg_type, "system");
    }
}
