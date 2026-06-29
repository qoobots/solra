#include "pbr_material.hpp"
#include <algorithm>

namespace solra::render {

std::vector<std::string> PbrMaterial::shaderDefines() const {
    std::vector<std::string> defs;

    // Workflow
    if (workflow == MaterialWorkflow::MetallicRoughness)
        defs.push_back("PBR_METALLIC_ROUGHNESS");
    else
        defs.push_back("PBR_SPECULAR_GLOSSINESS");

    // Texture presence
    if (metallicRoughness.baseColorTexture)  defs.push_back("HAS_BASE_COLOR_MAP");
    if (metallicRoughness.metallicRoughnessTexture) defs.push_back("HAS_METALLIC_ROUGHNESS_MAP");
    if (normalTexture)                       defs.push_back("HAS_NORMAL_MAP");
    if (occlusionTexture)                    defs.push_back("HAS_OCCLUSION_MAP");
    if (emissive.emissiveTexture)            defs.push_back("HAS_EMISSIVE_MAP");

    // Extensions
    if (clearcoat.factor > 0)                defs.push_back("PBR_CLEARCOAT");
    if (sheen.roughnessFactor > 0 || sheen.colorFactor[0] > 0) defs.push_back("PBR_SHEEN");
    if (transmission.factor > 0)             defs.push_back("PBR_TRANSMISSION");
    if (volume.thicknessFactor > 0)          defs.push_back("PBR_VOLUME");
    if (specular.specularFactor != 1.0f)     defs.push_back("PBR_SPECULAR_EXT");

    // Alpha
    switch (alphaMode) {
    case AlphaMode::Mask:  defs.push_back("ALPHA_MASK"); break;
    case AlphaMode::Blend: defs.push_back("ALPHA_BLEND"); break;
    default: break;
    }
    if (doubleSided) defs.push_back("DOUBLE_SIDED");

    return defs;
}

PbrMaterial::GpuUniforms PbrMaterial::buildUniforms() const {
    GpuUniforms u{};
    std::copy(std::begin(metallicRoughness.baseColorFactor),
              std::end(metallicRoughness.baseColorFactor), u.baseColorFactor);
    std::copy(std::begin(emissive.emissiveFactor),
              std::end(emissive.emissiveFactor), u.emissiveFactor);
    u.metallicRoughnessOcclusion[0] = metallicRoughness.metallicFactor;
    u.metallicRoughnessOcclusion[1] = metallicRoughness.roughnessFactor;
    u.metallicRoughnessOcclusion[2] = occlusionStrength;
    u.normalScale = normalScale;
    u.alphaCutoff = alphaCutoff;
    u.flags = textureFlags();
    return u;
}

uint32_t PbrMaterial::textureFlags() const {
    uint32_t f = 0;
    if (metallicRoughness.baseColorTexture)        f |= 1 << 0;
    if (normalTexture)                              f |= 1 << 1;
    if (metallicRoughness.metallicRoughnessTexture) f |= 1 << 2;
    if (occlusionTexture)                           f |= 1 << 3;
    if (emissive.emissiveTexture)                   f |= 1 << 4;
    if (clearcoat.clearcoatTexture)                 f |= 1 << 5;
    if (sheen.sheenColorTexture)                    f |= 1 << 6;
    if (transmission.transmissionTexture)           f |= 1 << 7;
    return f;
}

// ---- MaterialLibrary ----
std::shared_ptr<PbrMaterial> MaterialLibrary::get(const std::string& name) const {
    for (auto& m : materials_)
        if (m->name == name) return m;
    return nullptr;
}

void MaterialLibrary::add(std::shared_ptr<PbrMaterial> mat) {
    materials_.push_back(std::move(mat));
}

void MaterialLibrary::loadFromGLTF(const std::string& path) {
    // Stub: glTF material loader (uses tinygltf or cgltf)
    (void)path;
}

} // namespace solra::render
