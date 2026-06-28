/*
 * Solra Core SDK - LRU Cache implementation
 */

#include <solra/solra_streaming.h>
#include <spdlog/spdlog.h>
#include <string>
#include <list>
#include <unordered_map>
#include <mutex>

namespace {

class LRUCache {
public:
  explicit LRUCache(size_t max_size_bytes)
    : m_max_size(max_size_bytes), m_current_size(0) {}

  bool contains(const std::string& key) {
    std::lock_guard<std::mutex> lock(m_mutex);
    return m_map.count(key) > 0;
  }

  void touch(const std::string& key) {
    std::lock_guard<std::mutex> lock(m_mutex);
    auto it = m_map.find(key);
    if (it != m_map.end()) {
      m_list.splice(m_list.begin(), m_list, it->second);
    }
  }

  void insert(const std::string& key, size_t size_bytes) {
    std::lock_guard<std::mutex> lock(m_mutex);

    /* Evict least recently used items until we have space */
    while (m_current_size + size_bytes > m_max_size && !m_list.empty()) {
      const std::string& evict_key = m_list.back();
      spdlog::debug("Cache: evicting '{}' (LRU)", evict_key);
      m_list.pop_back();
      m_map.erase(evict_key);
      /* Assume all entries are the same size for now */
      m_current_size -= size_bytes;
    }

    if (m_current_size + size_bytes <= m_max_size) {
      m_list.push_front(key);
      m_map[key] = m_list.begin();
      m_current_size += size_bytes;
    }
  }

  void clear() {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_list.clear();
    m_map.clear();
    m_current_size = 0;
  }

  size_t size() const { return m_current_size; }

private:
  size_t m_max_size;
  size_t m_current_size;
  std::list<std::string> m_list;
  std::unordered_map<std::string, std::list<std::string>::iterator> m_map;
  std::mutex m_mutex;
};

} // anonymous namespace
