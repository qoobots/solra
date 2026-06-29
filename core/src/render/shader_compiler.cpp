#include "shader_compiler.hpp"
#include <cstring>
#include <fstream>
#include <filesystem>
#include <chrono>

namespace solra::render {

// ---- Inline SPIRV compilation notes ----
// Production builds use shaderc / glslang / spirv-cross as external tools.
// This file provides the offline batch compilation orchestrator + stub.
// Runtime compilation (for dev builds) delegates to platform-specific backend
// via SPIRV-Cross runtime API.

ShaderCompileResult ShaderCompiler::compileGLSLtoSPIRV(const std::string& glsl,
                                                       ShaderStage stage,
                                                       const ShaderCompileOptions& opts) {
    ShaderCompileResult result;
    auto t0 = std::chrono::steady_clock::now();

    // Stub: production code invokes shaderc or glslang
    (void)glsl; (void)stage; (void)opts;
    result.success = false;
    result.errorLog = "ShaderCompiler::compileGLSLtoSPIRV: not yet wired to shaderc/glslang backend. "
                      "Use offline precompilation via build system.";

    auto t1 = std::chrono::steady_clock::now();
    result.compileTimeUs = std::chrono::duration_cast<std::chrono::microseconds>(t1 - t0).count();
    return result;
}

ShaderCompileResult ShaderCompiler::compileSPIRVtoMSL(const std::vector<uint32_t>& spirv,
                                                      const ShaderCompileOptions& opts) {
    ShaderCompileResult result;
    // Stub: invokes spirv-cross --msl
    (void)spirv; (void)opts;
    result.success = false;
    result.errorLog = "SPIRV→MSL: not yet wired to spirv-cross backend.";
    return result;
}

ShaderCompileResult ShaderCompiler::compileSPIRVtoGLSL(const std::vector<uint32_t>& spirv,
                                                       const ShaderCompileOptions& opts) {
    ShaderCompileResult result;
    (void)spirv; (void)opts;
    result.success = false;
    result.errorLog = "SPIRV→GLSL: not yet wired to spirv-cross backend.";
    return result;
}

ShaderCompileResult ShaderCompiler::compileSPIRVtoHLSL(const std::vector<uint32_t>& spirv,
                                                       const ShaderCompileOptions& opts) {
    ShaderCompileResult result;
    (void)spirv; (void)opts;
    result.success = false;
    result.errorLog = "SPIRV→HLSL: not yet wired to spirv-cross backend.";
    return result;
}

std::optional<ShaderReflection> ShaderCompiler::reflect(const std::vector<uint32_t>& spirv) {
    (void)spirv;
    // Stub: SPIRV-Cross reflection
    return std::nullopt;
}

bool ShaderCompiler::compileFile(const std::string& inputPath,
                                  const std::string& outputPath,
                                  const ShaderCompileOptions& options) {
    std::ifstream in(inputPath, std::ios::binary);
    if (!in) return false;
    std::string source((std::istreambuf_iterator<char>(in)),
                        std::istreambuf_iterator<char>());

    auto result = compile(source, options);
    if (!result.success) return false;

    // Write SPIR-V binary
    std::ofstream out(outputPath, std::ios::binary);
    if (!result.spirv.empty()) {
        out.write(reinterpret_cast<const char*>(result.spirv.data()),
                  result.spirv.size() * sizeof(uint32_t));
    }
    return true;
}

ShaderCompiler::BatchResult ShaderCompiler::compileDirectory(
    const std::string& inputDir,
    const std::string& outputDir,
    const ShaderCompileOptions& options) {
    BatchResult batch;
    namespace fs = std::filesystem;

    if (!fs::exists(inputDir)) {
        batch.failed = 1;
        batch.failedFiles.push_back("inputDir not found: " + inputDir);
        return batch;
    }

    fs::create_directories(outputDir);

    for (const auto& entry : fs::recursive_directory_iterator(inputDir)) {
        if (!entry.is_regular_file()) continue;
        auto ext = entry.path().extension().string();
        if (ext != ".vert" && ext != ".frag" && ext != ".comp" && ext != ".glsl") continue;

        batch.total++;
        auto relPath = fs::relative(entry.path(), inputDir);
        auto outPath = (fs::path(outputDir) / relPath).replace_extension(".spv");

        if (compileFile(entry.path().string(), outPath.string(), options)) {
            batch.success++;
        } else {
            batch.failed++;
            batch.failedFiles.push_back(entry.path().string());
        }
    }

    return batch;
}

// Factory: returns SPIRV-Cross based compiler for host platform
std::unique_ptr<ShaderCompiler> createShaderCompiler() {
    // Returns platform-aware compiler that delegates to SPIRV-Cross
    // for MSL (macOS/iOS) / GLSL (Android/GLES) / HLSL (Windows) cross-compilation
    return nullptr; // stub
}

} // namespace solra::render
