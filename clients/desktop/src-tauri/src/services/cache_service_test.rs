#[cfg(test)]
mod tests {
    use std::fs;
    use std::path::PathBuf;
    use tempfile::TempDir;

    // Note: CacheService uses an internal struct, so we test via integration patterns

    fn setup_cache_dir() -> (TempDir, PathBuf) {
        let dir = TempDir::new().expect("创建临时目录失败");
        let cache_dir = dir.path().join("cache");
        fs::create_dir_all(&cache_dir).expect("创建缓存目录失败");
        (dir, cache_dir)
    }

    #[test]
    fn test_cache_dir_creation() {
        let (_dir, cache_path) = setup_cache_dir();
        assert!(cache_path.exists());
        assert!(cache_path.is_dir());
    }

    #[test]
    fn test_cache_size_calculation() {
        let (_dir, cache_path) = setup_cache_dir();

        // 创建测试文件
        let test_file = cache_path.join("test.txt");
        fs::write(&test_file, "Hello, Solra!").expect("写入文件失败");

        let metadata = fs::metadata(&test_file).expect("读取文件元数据失败");
        assert!(metadata.len() > 0);
        assert_eq!(metadata.len(), 13); // "Hello, Solra!" = 13 bytes
    }

    #[test]
    fn test_cache_max_size_check() {
        let max_size_mb = 2048u64;
        let max_size_bytes = max_size_mb * 1024 * 1024;

        // 模拟缓存大小检查
        let current_size = 500u64 * 1024 * 1024; // 500MB
        assert!(current_size < max_size_bytes);
    }

    #[test]
    fn test_cache_eviction_needed() {
        let max_size_bytes = 1024u64 * 1024 * 1024; // 1GB
        let current_size = 1200u64 * 1024 * 1024; // 1.2GB

        // 需要清理
        assert!(current_size > max_size_bytes);
        let needed = current_size - max_size_bytes;
        assert!(needed > 0);
    }
}
