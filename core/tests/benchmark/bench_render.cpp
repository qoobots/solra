/*
 * Solra Core SDK - Render performance benchmarks
 */

#include <benchmark/benchmark.h>
#include <solra/solra_render.h>
#include <solra/solra_types.h>
#include <glm/glm.hpp>
#include <glm/gtc/matrix_transform.hpp>

/** Benchmark: Scene graph node hierarchy creation and traversal */
static void BM_SceneGraphCreation(benchmark::State &state) {
  const int node_count = state.range(0);
  for (auto _ : state) {
    /* Simulate scene graph creation */
    std::vector<glm::mat4> transforms(node_count, glm::mat4(1.0f));
    for (int i = 0; i < node_count; i++) {
      transforms[i] = glm::translate(glm::mat4(1.0f), glm::vec3(i * 0.1f, 0.0f, 0.0f));
      transforms[i] = glm::rotate(transforms[i], glm::radians(i * 10.0f), glm::vec3(0.0f, 1.0f, 0.0f));
    }
    benchmark::DoNotOptimize(transforms.data());
  }
  state.SetItemsProcessed(state.iterations() * node_count);
}
BENCHMARK(BM_SceneGraphCreation)->Range(8, 1024);

/** Benchmark: Matrix multiply (simulating transform hierarchy computation) */
static void BM_MatrixMultiply(benchmark::State &state) {
  glm::mat4 a = glm::rotate(glm::mat4(1.0f), 0.5f, glm::vec3(0.0f, 1.0f, 0.0f));
  glm::mat4 b = glm::translate(glm::mat4(1.0f), glm::vec3(1.0f, 2.0f, 3.0f));
  for (auto _ : state) {
    glm::mat4 c = a * b;
    benchmark::DoNotOptimize(c);
  }
}
BENCHMARK(BM_MatrixMultiply);

/** Benchmark: AABB computation from vertices */
static void BM_AABBCompute(benchmark::State &state) {
  const int count = state.range(0);
  std::vector<glm::vec3> verts(count);
  for (int i = 0; i < count; i++) {
    verts[i] = glm::vec3(sinf(i * 0.1f), cosf(i * 0.1f), i * 0.01f);
  }
  for (auto _ : state) {
    glm::vec3 vmin(FLT_MAX), vmax(-FLT_MAX);
    for (const auto &v : verts) {
      vmin = glm::min(vmin, v);
      vmax = glm::max(vmax, v);
    }
    benchmark::DoNotOptimize(vmin);
    benchmark::DoNotOptimize(vmax);
  }
  state.SetItemsProcessed(state.iterations() * count);
}
BENCHMARK(BM_AABBCompute)->Range(1 << 10, 1 << 20);
