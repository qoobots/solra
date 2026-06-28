/*
 * Solra Core SDK - Custom memory allocator (stub)
 */

#include <solra/solra_types.h>
#include <cstdlib>

static SolraAllocFn g_alloc_fn = nullptr;
static SolraFreeFn g_free_fn = nullptr;
static void *g_alloc_user_data = nullptr;

void solra_set_allocator(SolraAllocFn alloc_fn, SolraFreeFn free_fn, void *user_data) {
  g_alloc_fn = alloc_fn;
  g_free_fn = free_fn;
  g_alloc_user_data = user_data;
}

/* Internal allocation helpers (used by SDK internally) */
void *solra_alloc(size_t size) {
  if (g_alloc_fn) {
    return g_alloc_fn(size, g_alloc_user_data);
  }
  return std::malloc(size);
}

void solra_free(void *ptr) {
  if (g_free_fn) {
    g_free_fn(ptr, g_alloc_user_data);
  } else {
    std::free(ptr);
  }
}
