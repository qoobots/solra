// 本地缓存管理服务
// 管理空间资产的本地文件缓存，支持 LRU 淘汰策略

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

    /// 注册缓存条目（包含 LRU 淘汰逻辑）
    pub fn register(&self, space_id: &str, file_path: PathBuf, size_bytes: u64) {
        let now = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap()
            .as_secs();

        let entry = CacheEntry {
            space_id: space_id.to_string(),
            file_path,
            size_bytes,
            last_accessed: now,
        };

        // 先插入新条目
        let mut entries = self.entries.lock().unwrap();
        // 如果已有旧条目，先减去旧的大小
        if let Some(old) = entries.get(space_id) {
            let mut current = self.current_size_bytes.lock().unwrap();
            *current = current.saturating_sub(old.size_bytes);
        }
        entries.insert(space_id.to_string(), entry);
        drop(entries);

        // 更新当前大小
        {
            let mut current = self.current_size_bytes.lock().unwrap();
            *current += size_bytes;
        }

        // LRU 淘汰：当缓存超出上限时，删除最久未访问的条目
        self.evict_lru();

        log::info!(
            "缓存注册: space_id={}, size={:.1}MB, current_total={:.1}MB",
            space_id,
            size_bytes as f64 / (1024.0 * 1024.0),
            *self.current_size_bytes.lock().unwrap() as f64 / (1024.0 * 1024.0),
        );
    }

    /// 标记空间为已访问（更新 last_accessed 时间戳）
    pub fn touch(&self, space_id: &str) {
        if let Some(entry) = self.entries.lock().unwrap().get_mut(space_id) {
            entry.last_accessed = std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .unwrap()
                .as_secs();
        }
    }

    /// 手动删除缓存条目
    pub fn remove(&self, space_id: &str) -> bool {
        if let Some(entry) = self.entries.lock().unwrap().remove(space_id) {
            // 删除磁盘文件
            if entry.file_path.exists() {
                let _ = std::fs::remove_file(&entry.file_path);
            }
            let mut current = self.current_size_bytes.lock().unwrap();
            *current = current.saturating_sub(entry.size_bytes);
            log::info!("缓存已删除: space_id={}, freed={:.1}MB", space_id, entry.size_bytes as f64 / (1024.0 * 1024.0));
            true
        } else {
            false
        }
    }

    /// 清空所有缓存
    pub fn clear_all(&self) {
        let entries = std::mem::take(&mut *self.entries.lock().unwrap());
        for (_, entry) in entries {
            if entry.file_path.exists() {
                let _ = std::fs::remove_file(&entry.file_path);
            }
        }
        *self.current_size_bytes.lock().unwrap() = 0;
        log::info!("所有缓存已清空");
    }

    /// 获取缓存目录路径
    pub fn cache_dir(&self) -> &PathBuf {
        &self.cache_dir
    }

    /// 获取缓存统计
    pub fn stats(&self) -> (u64, u64, usize) {
        let current = *self.current_size_bytes.lock().unwrap();
        let max = self.max_size_bytes;
        let count = self.entries.lock().unwrap().len();
        (current, max, count)
    }

    /// LRU 淘汰：删除最久未访问的条目直到缓存大小在限制内
    fn evict_lru(&self) {
        loop {
            let current = *self.current_size_bytes.lock().unwrap();
            if current <= self.max_size_bytes {
                break;
            }

            // 找到最久未访问的条目
            let mut entries = self.entries.lock().unwrap();
            let oldest = entries.iter()
                .min_by_key(|(_, e)| e.last_accessed)
                .map(|(k, _)| k.clone());

            if let Some(space_id) = oldest {
                if let Some(entry) = entries.remove(&space_id) {
                    // 删除磁盘文件
                    if entry.file_path.exists() {
                        if let Err(e) = std::fs::remove_file(&entry.file_path) {
                            log::warn!("删除缓存文件失败: {:?} - {}", entry.file_path, e);
                        }
                    }
                    let mut current = self.current_size_bytes.lock().unwrap();
                    *current = current.saturating_sub(entry.size_bytes);
                    log::info!(
                        "LRU 淘汰: space_id={}, freed={:.1}MB, remaining={:.1}MB/{}MB",
                        space_id,
                        entry.size_bytes as f64 / (1024.0 * 1024.0),
                        *current as f64 / (1024.0 * 1024.0),
                        self.max_size_bytes / (1024 * 1024),
                    );
                }
            } else {
                // 没有可淘汰的条目，退出循环防止死循环
                break;
            }
        }
    }
}
