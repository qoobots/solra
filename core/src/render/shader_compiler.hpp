#pragma once
// Cross-platform shader compilation pipeline
// Input: GLSL/HLSL/Metal Shading Language → Output: SPIR-V + platform-specific binaries
#include <cstdint>
#include <string>
#include <vector>
#include <optional>

namespace solra::render {

enum class ShaderSourceLang { GLSL, HLSL, MSL };  // Metal Shading Language
enum class ShaderTarget {
    SPIRV,              // Vulkan / OpenGL ES 3.1+
    MSL_Binary,         // Metal compiled library
    DXIL,               // DirectX Intermediate Language
    GLSL_ES_300,        // OpenGL ES 3.0 source passthrough
};

struct ShaderCompileOptions {
    ShaderSourceLang sourceLang = ShaderSourceLang::GLSL;
    ShaderTarget target = ShaderTarget::SPIRV;
    std::string entryPoint = "main";
    bool optimize = true;            // -O for shader compilers
    bool debugInfo = false;          // embed debug symbols
    bool flattenUBOs = false;        // flatten uniform blocks for GLES compat
    std::vector<std::string> defines; // preprocessor defines
    std::vector<std::string> includeDirs;
};

struct ShaderCompileResult {
    bool success = false;
    std::vector<uint32_t> spirv;
    std::string platformBinary; // MSL .metallib / DXIL blob
    std::string errorLog;
    std::string warningLog;
    uint64_t compileTimeUs = 0;
};

// ---- SPIRV-Cross based reflection ----
struct UniformBinding {
    uint32_t set = 0, binding = 0;
    std::string name;
    std::string type; // "sampler2D", "uniform", "storage", etc.
    uint64_t size = 0;
};

struct ShaderReflection {
    std::vector<UniformBinding> uniforms;
    std::vector<uint32_t> vertexInputLocations;
    uint64_t pushConstantSize = 0;
    bool usesPushConstants = false;
};

class ShaderCompiler {
public:
    virtual ~ShaderCompiler() = default;

    // Compile source to target binary
    virtual ShaderCompileResult compile(const std::string& source,
                                        const ShaderCompileOptions& options) = 0;

    // GLSL → SPIR-V (using glslangValidator equivalent)
    ShaderCompileResult compileGLSLtoSPIRV(const std::string& glsl,
                                           ShaderStage stage,
                                           const ShaderCompileOptions& opts = {});

    // SPIR-V cross-compile to platform target
    ShaderCompileResult compileSPIRVtoMSL(const std::vector<uint32_t>& spirv,
                                          const ShaderCompileOptions& opts = {});
    ShaderCompileResult compileSPIRVtoGLSL(const std::vector<uint32_t>& spirv,
                                           const ShaderCompileOptions& opts = {});
    ShaderCompileResult compileSPIRVtoHLSL(const std::vector<uint32_t>& spirv,
                                           const ShaderCompileOptions& opts = {});

    // Reflection
    std::optional<ShaderReflection> reflect(const std::vector<uint32_t>& spirv);

    // Offline compilation: loads .vert/.frag/.comp and produces platform binaries
    bool compileFile(const std::string& inputPath,
                     const std::string& outputPath,
                     const ShaderCompileOptions& options);

    // Batch offline compilation for all shaders in a directory
    struct BatchResult {
        int total = 0, success = 0, failed = 0;
        std::vector<std::string> failedFiles;
    };
    BatchResult compileDirectory(const std::string& inputDir,
                                 const std::string& outputDir,
                                 const ShaderCompileOptions& options);
};

// Factory: returns platform-appropriate compiler
std::unique_ptr<ShaderCompiler> createShaderCompiler();

} // namespace solra::render
