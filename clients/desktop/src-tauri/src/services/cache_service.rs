// 本地缓存管理服务
// 管理空间资产的本地文件缓存

use std::collections::HashMap;
use std::path::PathBuf;
use std::sync::Mutex;

/// 缓存条目
#[derive(Debug, Clone)]
pub struct CacheEntry {
    pub space_id: String,
    pub file_path: PathBuf,
    pub size_bytes: u64,
    pub last_accessed: u64,
}

/// 本地缓存服务
pub struct CacheService {
    cache_dir: PathBuf,
    entries: Mutex<HashMap<String, CacheEntry>>,
    max_size_bytes: u64,
    current_size_bytes: Mutex<u64>,
}

impl CacheService {
    pub fn new(cache_dir: PathBuf, max_size_mb: u64) -> Self {
        std::fs::create_dir_all(&cache_dir).ok();

        Self {
            cache_dir,
            entries: Mutex::new(HashMap::new()),
            max_size_bytes: max_size_mb * 1024 * 1024,
            current_size_bytes: Mutex::new(0),
        }
    }

    /// 检查空间是否已缓存
    pub fn is_cached(&self, space_id: &str) -> bool {
        self.entries.lock().unwrap().contains_key(space_id)
    }

    /// 获取缓存路径
    pub fn get_cache_path(&self, space_id: &str) -> Option<PathBuf> {
        self.entries.lock().unwrap()
            .get(space_id)
            .map(|e| e.file_path.clone())
    }

    /// 注册缓存条目
    pub fn register(&self, space_id: &str, file_path: PathBuf, size_bytes: u64) {
        let entry = CacheEntry {
            space_id: space_id.to_string(),
            file_path,
            size_bytes,
            last_accessed: std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .unwrap()
                .as_secs(),
        };

        let mut current = self.current_size_bytes.lock().unwrap();
        *current += size_bytes;

        // LRU 淘汰
        while *current > self.max_size_bytes {
            // TODO: 实现 LRU 淘汰逻辑
            break;
        }

        self.entries.lock().unwrap().insert(space_id.to_string(), entry);
    }

    /// 获取缓存统计
    pub fn stats(&self) -> (u64, u64, usize) {
        let current = *self.current_size_bytes.lock().unwrap();
        let max = self.max_size_bytes;
        let count = self.entries.lock().unwrap().len();
        (current, max, count)
    }
}
