# Solra Core SDK - Performance Benchmarks

set(SOLRA_CORE_BENCH_SOURCES
  bench_render.cpp
)

add_executable(solra_core_benchmarks ${SOLRA_CORE_BENCH_SOURCES})
target_include_directories(solra_core_benchmarks PRIVATE ${CMAKE_SOURCE_DIR}/include)
target_link_libraries(solra_core_benchmarks
  PRIVATE
    solra_core_core
    benchmark::benchmark
    benchmark::benchmark_main
)
target_compile_features(solra_core_benchmarks PRIVATE cxx_std_17)
