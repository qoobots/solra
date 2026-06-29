# Solra Core SDK - Unit Tests

set(SOLRA_CORE_TEST_SOURCES
  test_core.cpp
  test_types.cpp
  test_types_ext.cpp
  test_streaming.cpp
  test_render.cpp
)

add_executable(solra_core_tests ${SOLRA_CORE_TEST_SOURCES})
target_include_directories(solra_core_tests PRIVATE ${CMAKE_SOURCE_DIR}/include)
target_link_libraries(solra_core_tests
  PRIVATE
    solra_core_core
    solra_core_streaming
    solra_core_render
    GTest::gtest
    GTest::gtest_main
)
target_compile_features(solra_core_tests PRIVATE cxx_std_17)

include(GoogleTest)
gtest_discover_tests(solra_core_tests)
