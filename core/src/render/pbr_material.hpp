#pragma once
// Physically-Based Rendering Material System
#include <cstdint>
#include <string>
#include <vector>
#include <memory>
#include <array>
#include <variant>

namespace solra::render {

// ---- PBR Texture slots ----
enum class PbrTextureSlot {
    Albedo,          // Base color / diffuse
    Normal,          // Tangent-space normal map
    Metallic,        // Metalness (R channel)
    Roughness,       // Roughness (G channel of metallic-roughness or separate)
    AmbientOcclusion,// AO map
    Emissive,        // Self-illumination
    Height,          // Displacement/parallax
    Opacity,         // Alpha / cutout mask
    DetailNormal,    // Micro detail normal
};

struct PbrTexture {
    PbrTextureSlot slot;
    std::string path;      // asset path or URI
    uint32_t textureId = 0;// runtime handle
    float scale = 1.0f;    // tiling scale
    float offset[2] = {0, 0};
};

// ---- PBR Parameters ----
struct PbrMetallicRoughness {
    float metallicFactor = 1.0f;
    float roughnessFactor = 1.0f;
    float baseColorFactor[4] = {1, 1, 1, 1}; // linear sRGB
    std::shared_ptr<PbrTexture> baseColorTexture;
    std::shared_ptr<PbrTexture> metallicRoughnessTexture;
};

struct PbrSpecularGlossiness {
    float diffuseFactor[4] = {1, 1, 1, 1};
    float specularFactor[3] = {1, 1, 1};
    float glossinessFactor = 1.0f;
    std::shared_ptr<PbrTexture> diffuseTexture;
    std::shared_ptr<PbrTexture> specularGlossinessTexture;
};

// ---- Clearcoat extension (car paint, wet surfaces) ----
struct PbrClearcoat {
    float factor = 0.0f;
    float roughnessFactor = 0.0f;
    std::shared_ptr<PbrTexture> clearcoatTexture;
    std::shared_ptr<PbrTexture> clearcoatRoughnessTexture;
    std::shared_ptr<PbrTexture> clearcoatNormalTexture;
};

// ---- Sheen extension (cloth, velvet) ----
struct PbrSheen {
    float colorFactor[3] = {0, 0, 0};
    float roughnessFactor = 0.0f;
    std::shared_ptr<PbrTexture> sheenColorTexture;
    std::shared_ptr<PbrTexture> sheenRoughnessTexture;
};

// ---- Transmission / thin-walled ----
struct PbrTransmission {
    float factor = 0.0f;
    std::shared_ptr<PbrTexture> transmissionTexture;
};

// ---- Volume (subsurface scattering, thin volume) ----
struct PbrVolume {
    float thicknessFactor = 0.0f;
    float attenuationColor[3] = {1, 1, 1};
    float attenuationDistance = 1.0f;
    std::shared_ptr<PbrTexture> thicknessTexture;
};

// ---- IOR / Specular (KHR_materials_specular) ----
struct PbrSpecular {
    float specularFactor = 1.0f;
    float specularColorFactor[3] = {1, 1, 1};
    float ior = 1.5f; // index of refraction
    std::shared_ptr<PbrTexture> specularTexture;
    std::shared_ptr<PbrTexture> specularColorTexture;
};

// ---- Emissive strength ----
struct PbrEmissive {
    float emissiveFactor[3] = {0, 0, 0};
    float emissiveStrength = 1.0f;
    std::shared_ptr<PbrTexture> emissiveTexture;
};

// ---- PbrMaterial: glTF 2.0 PBR Next compatible ----
enum class AlphaMode { Opaque, Mask, Blend };
enum class MaterialWorkflow { MetallicRoughness, SpecularGlossiness };

class PbrMaterial {
public:
    std::string name;
    MaterialWorkflow workflow = MaterialWorkflow::MetallicRoughness;

    // Core PBR
    PbrMetallicRoughness metallicRoughness;
    PbrSpecularGlossiness specularGlossiness;
    PbrClearcoat clearcoat;
    PbrSheen sheen;
    PbrTransmission transmission;
    PbrVolume volume;
    PbrSpecular specular;
    PbrEmissive emissive;

    // Common
    std::shared_ptr<PbrTexture> normalTexture;
    std::shared_ptr<PbrTexture> occlusionTexture;
    float normalScale = 1.0f;
    float occlusionStrength = 1.0f;

    // Alpha
    AlphaMode alphaMode = AlphaMode::Opaque;
    float alphaCutoff = 0.5f;
    bool doubleSided = false;

    // Generate shader defines for this material variant
    std::vector<std::string> shaderDefines() const;

    // GPU uniform block (std140 layout compatible)
    struct alignas(16) GpuUniforms {
        float baseColorFactor[4];
        float emissiveFactor[4];
        float metallicRoughnessOcclusion[4]; // .x=metallic .y=roughness .z=occlusion .w=pad
        float normalScale;
        float alphaCutoff;
        uint32_t flags; // bitmask: hasBaseColorTex, hasNormalTex, hasMetallicRoughnessTex, etc.
        float pad;
    };
    static_assert(sizeof(GpuUniforms) == 64, "PBR uniforms must be 64 bytes");

    GpuUniforms buildUniforms() const;

private:
    uint32_t textureFlags() const;
};

// ---- Material Library ----
class MaterialLibrary {
public:
    std::shared_ptr<PbrMaterial> get(const std::string& name) const;
    void add(std::shared_ptr<PbrMaterial> mat);
    void loadFromGLTF(const std::string& path);
    size_t count() const { return materials_.size(); }

private:
    std::vector<std::shared_ptr<PbrMaterial>> materials_;
};

} // namespace solra::render
