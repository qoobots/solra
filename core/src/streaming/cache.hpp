#pragma once
/// @file cache.hpp
/// @brief LRU cache for streaming asset data
/// @ingroup core/streaming

#include <string>
#include <list>
#include <unordered_map>
#include <mutex>
#include <cstddef>

namespace solra::core::streaming {

class LRUCache {
public:
  explicit LRUCache(size_t max_size_bytes)
    : max_size_(max_size_bytes), current_size_(0) {}

  bool contains(const std::string& key) {
    std::lock_guard<std::mutex> lock(mutex_);
    return map_.count(key) > 0;
  }

  void touch(const std::string& key) {
    std::lock_guard<std::mutex> lock(mutex_);
    auto it = map_.find(key);
    if (it != map_.end()) {
      list_.splice(list_.begin(), list_, it->second);
    }
  }

  void insert(const std::string& key, size_t size_bytes) {
    std::lock_guard<std::mutex> lock(mutex_);
    while (current_size_ + size_bytes > max_size_ && !list_.empty()) {
      const std::string& evict_key = list_.back();
      list_.pop_back();
      map_.erase(evict_key);
      if (current_size_ >= size_bytes) current_size_ -= size_bytes;
    }
    if (current_size_ + size_bytes <= max_size_) {
      list_.push_front(key);
      map_[key] = list_.begin();
      current_size_ += size_bytes;
    }
  }

  void clear() {
    std::lock_guard<std::mutex> lock(mutex_);
    list_.clear();
    map_.clear();
    current_size_ = 0;
  }

  size_t size() const { return current_size_; }

private:
  size_t max_size_;
  size_t current_size_;
  std::list<std::string> list_;
  std::unordered_map<std::string, std::list<std::string>::iterator> map_;
  mutable std::mutex mutex_;
};

} // namespace solra::core::streaming
