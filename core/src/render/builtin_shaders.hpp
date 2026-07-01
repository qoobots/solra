#pragma once
// Built-in PBR shaders embedded as GLSL 4.60 source strings
// These provide the default rendering pipeline without external shader files.

#include <string>

namespace solra::render {

// PBR vertex shader — standard transform + normal pass-through
inline const char* PBR_VERTEX_GLSL = R"GLSL(#version 460 core

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aTexCoord;

layout(location = 0) uniform mat4 uModel;
layout(location = 4) uniform mat4 uViewProj;

out vec3 vWorldPos;
out vec3 vNormal;
out vec2 vTexCoord;

void main() {
    vec4 worldPos = uModel * vec4(aPosition, 1.0);
    vWorldPos = worldPos.xyz;
    vNormal = mat3(uModel) * aNormal;
    vTexCoord = aTexCoord;
    gl_Position = uViewProj * worldPos;
}
)GLSL";

// PBR vertex shader with GPU skinning — supports up to 128 bones
inline const char* PBR_SKINNED_VERTEX_GLSL = R"GLSL(#version 460 core

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aTexCoord;
layout(location = 3) in vec4 aBoneWeights;
layout(location = 4) in uvec4 aBoneIndices;

layout(location = 0) uniform mat4 uModel;
layout(location = 4) uniform mat4 uViewProj;

// Skinning matrix palette: up to 128 bones
#define MAX_BONES 128
layout(location = 16, std140) uniform SkinningBlock {
    mat4 uSkinningMatrices[MAX_BONES];
};

out vec3 vWorldPos;
out vec3 vNormal;
out vec2 vTexCoord;

void main() {
    // Compute skinned position and normal
    mat4 skinMatrix =
        aBoneWeights.x * uSkinningMatrices[aBoneIndices.x] +
        aBoneWeights.y * uSkinningMatrices[aBoneIndices.y] +
        aBoneWeights.z * uSkinningMatrices[aBoneIndices.z] +
        aBoneWeights.w * uSkinningMatrices[aBoneIndices.w];

    vec4 skinnedPos4 = skinMatrix * vec4(aPosition, 1.0);
    vec4 skinnedNrm4 = skinMatrix * vec4(aNormal, 0.0);

    vec4 worldPos = uModel * skinnedPos4;
    vWorldPos = worldPos.xyz;
    vNormal = normalize(mat3(uModel) * skinnedNrm4.xyz);
    vTexCoord = aTexCoord;
    gl_Position = uViewProj * worldPos;
}
)GLSL";

// PBR fragment shader — GGX metallic-roughness with single directional light
inline const char* PBR_FRAGMENT_GLSL = R"GLSL(#version 460 core

layout(location = 0) out vec4 fragColor;

in vec3 vWorldPos;
in vec3 vNormal;
in vec2 vTexCoord;

// PBR material uniforms (std140 layout, 64 bytes)
layout(location = 1, std140) uniform MaterialBlock {
    vec4 baseColorFactor;
    vec4 emissiveFactor;
    vec4 metallicRoughnessOcclusion; // .x=metallic .y=roughness .z=occlusion .w=pad
    float normalScale;
    float alphaCutoff;
    uint flags;
    float pad;
} uMaterial;

// Light uniforms
layout(location = 3) uniform vec3 uLightDirection;
layout(location = 5) uniform vec3 uLightColor;
layout(location = 6) uniform float uLightIntensity;
layout(location = 7) uniform vec3 uAmbientColor;
layout(location = 8) uniform vec3 uCameraPosition;

const float PI = 3.14159265359;

// GGX distribution
float distributionGGX(vec3 N, vec3 H, float roughness) {
    float a = roughness * roughness;
    float a2 = a * a;
    float NdotH = max(dot(N, H), 0.0);
    float NdotH2 = NdotH * NdotH;
    float denom = NdotH2 * (a2 - 1.0) + 1.0;
    return a2 / (PI * denom * denom);
}

// Schlick-GGX geometry
float geometrySchlickGGX(float NdotV, float roughness) {
    float r = roughness + 1.0;
    float k = (r * r) / 8.0;
    return NdotV / (NdotV * (1.0 - k) + k);
}

float geometrySmith(vec3 N, vec3 V, vec3 L, float roughness) {
    return geometrySchlickGGX(max(dot(N, V), 0.0), roughness) *
           geometrySchlickGGX(max(dot(N, L), 0.0), roughness);
}

// Fresnel-Schlick
vec3 fresnelSchlick(float cosTheta, vec3 F0) {
    return F0 + (1.0 - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
}

void main() {
    vec3 albedo = uMaterial.baseColorFactor.rgb;
    float metallic = uMaterial.metallicRoughnessOcclusion.x;
    float roughness = uMaterial.metallicRoughnessOcclusion.y;
    float ao = uMaterial.metallicRoughnessOcclusion.z;

    vec3 N = normalize(vNormal);
    vec3 V = normalize(uCameraPosition - vWorldPos);

    // F0: reflectance at normal incidence
    vec3 F0 = mix(vec3(0.04), albedo, metallic);

    // Directional light
    vec3 L = normalize(-uLightDirection);
    vec3 H = normalize(V + L);
    vec3 radiance = uLightColor * uLightIntensity;

    // Cook-Torrance BRDF
    float NDF = distributionGGX(N, H, roughness);
    float G = geometrySmith(N, V, L, roughness);
    vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);

    vec3 numerator = NDF * G * F;
    float denominator = 4.0 * max(dot(N, V), 0.0) * max(dot(N, L), 0.0) + 0.0001;
    vec3 specular = numerator / denominator;

    // Energy conservation
    vec3 kD = (1.0 - F) * (1.0 - metallic);

    float NdotL = max(dot(N, L), 0.0);
    vec3 Lo = (kD * albedo / PI + specular) * radiance * NdotL;

    // Ambient
    vec3 ambient = uAmbientColor * albedo * ao;

    vec3 color = ambient + Lo + uMaterial.emissiveFactor.rgb;

    // Simple tone mapping (Reinhard)
    color = color / (color + 1.0);

    // Gamma correction
    color = pow(color, vec3(1.0 / 2.2));

    fragColor = vec4(color, uMaterial.baseColorFactor.a);
}
)GLSL";

// Simple unlit vertex shader (for debug/UI)
inline const char* UNLIT_VERTEX_GLSL = R"GLSL(#version 460 core

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aTexCoord;

layout(location = 0) uniform mat4 uModel;
layout(location = 4) uniform mat4 uViewProj;

out vec3 vColor;

void main() {
    gl_Position = uViewProj * uModel * vec4(aPosition, 1.0);
    vColor = aNormal * 0.5 + 0.5; // debug: visualize normals
}
)GLSL";

inline const char* UNLIT_FRAGMENT_GLSL = R"GLSL(#version 460 core

in vec3 vColor;
layout(location = 0) out vec4 fragColor;

void main() {
    fragColor = vec4(vColor, 1.0);
}
)GLSL";

// Skybox / background vertex shader
inline const char* SKYBOX_VERTEX_GLSL = R"GLSL(#version 460 core

layout(location = 0) in vec3 aPosition;

layout(location = 4) uniform mat4 uViewProj;

out vec3 vTexCoord;

void main() {
    vTexCoord = aPosition;
    vec4 pos = uViewProj * vec4(aPosition * 100.0, 1.0);
    gl_Position = pos.xyww; // always at far plane
}
)GLSL";

inline const char* SKYBOX_FRAGMENT_GLSL = R"GLSL(#version 460 core

in vec3 vTexCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    // Simple gradient sky
    float t = clamp(vTexCoord.y * 0.5 + 0.5, 0.0, 1.0);
    vec3 topColor = vec3(0.1, 0.2, 0.4);
    vec3 bottomColor = vec3(0.4, 0.5, 0.7);
    vec3 color = mix(bottomColor, topColor, t);
    fragColor = vec4(color, 1.0);
}
)GLSL";

// ============================================================
// Deferred Rendering Shaders
// ============================================================

// Deferred geometry pass — writes to G-Buffer MRT
inline const char* DEFERRED_GEOMETRY_VERTEX_GLSL = R"GLSL(#version 460 core

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aTexCoord;

layout(location = 0) uniform mat4 uModel;
layout(location = 4) uniform mat4 uViewProj;

out vec3 vWorldPos;
out vec3 vNormal;
out vec2 vTexCoord;

void main() {
    vec4 worldPos = uModel * vec4(aPosition, 1.0);
    vWorldPos = worldPos.xyz;
    vNormal = normalize(mat3(uModel) * aNormal);
    vTexCoord = aTexCoord;
    gl_Position = uViewProj * worldPos;
}
)GLSL";

// Deferred geometry fragment shader — outputs to 4 render targets
inline const char* DEFERRED_GEOMETRY_FRAGMENT_GLSL = R"GLSL(#version 460 core

in vec3 vWorldPos;
in vec3 vNormal;
in vec2 vTexCoord;

// G-Buffer outputs
layout(location = 0) out vec4 gAlbedo;
layout(location = 1) out vec4 gNormal;
layout(location = 2) out vec4 gMetalRough;
layout(location = 3) out vec4 gEmission;

// Material uniforms (std140 layout)
layout(location = 1, std140) uniform MaterialBlock {
    vec4 baseColorFactor;
    vec4 emissiveFactor;
    vec4 metallicRoughnessOcclusion;
    float normalScale;
    float alphaCutoff;
    uint flags;
    float pad;
} uMaterial;

void main() {
    // Albedo (RGB) + alpha
    gAlbedo = uMaterial.baseColorFactor;

    // World-space normal (RGB) encoded as [0,1]
    vec3 N = normalize(vNormal);
    gNormal = vec4(N * 0.5 + 0.5, 1.0);

    // Metallic (R) + Roughness (G) + Occlusion (B)
    gMetalRough = vec4(
        uMaterial.metallicRoughnessOcclusion.x,
        uMaterial.metallicRoughnessOcclusion.y,
        uMaterial.metallicRoughnessOcclusion.z,
        1.0
    );

    // Emission (RGB)
    gEmission = uMaterial.emissiveFactor;
}
)GLSL";

// Deferred lighting pass — full-screen quad reading G-Buffer
inline const char* DEFERRED_LIGHTING_VERTEX_GLSL = R"GLSL(#version 460 core

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec2 aTexCoord;

out vec2 vTexCoord;

void main() {
    vTexCoord = aTexCoord;
    gl_Position = vec4(aPosition, 1.0);
}
)GLSL";

// Deferred lighting fragment shader — PBR lighting from G-Buffer
inline const char* DEFERRED_LIGHTING_FRAGMENT_GLSL = R"GLSL(#version 460 core

in vec2 vTexCoord;
layout(location = 0) out vec4 fragColor;

// G-Buffer textures
layout(binding = 0) uniform sampler2D uGBufferAlbedo;
layout(binding = 1) uniform sampler2D uGBufferNormal;
layout(binding = 2) uniform sampler2D uGBufferMetalRough;
layout(binding = 3) uniform sampler2D uGBufferEmission;
layout(binding = 4) uniform sampler2D uGBufferDepth;

// Lighting uniforms
layout(location = 3) uniform vec3 uLightDirection;
layout(location = 5) uniform vec3 uLightColor;
layout(location = 6) uniform float uLightIntensity;
layout(location = 7) uniform vec3 uAmbientColor;
layout(location = 8) uniform vec3 uCameraPosition;
layout(location = 9) uniform mat4 uInverseViewProj;

const float PI = 3.14159265359;

float distributionGGX(vec3 N, vec3 H, float roughness) {
    float a = roughness * roughness;
    float a2 = a * a;
    float NdotH = max(dot(N, H), 0.0);
    float NdotH2 = NdotH * NdotH;
    float denom = NdotH2 * (a2 - 1.0) + 1.0;
    return a2 / (PI * denom * denom);
}

float geometrySchlickGGX(float NdotV, float roughness) {
    float r = roughness + 1.0;
    float k = (r * r) / 8.0;
    return NdotV / (NdotV * (1.0 - k) + k);
}

float geometrySmith(vec3 N, vec3 V, vec3 L, float roughness) {
    return geometrySchlickGGX(max(dot(N, V), 0.0), roughness) *
           geometrySchlickGGX(max(dot(N, L), 0.0), roughness);
}

vec3 fresnelSchlick(float cosTheta, vec3 F0) {
    return F0 + (1.0 - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
}

// Reconstruct world position from depth and UV
vec3 reconstructWorldPos(vec2 uv, float depth) {
    vec4 clipPos = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 worldPos = uInverseViewProj * clipPos;
    return worldPos.xyz / worldPos.w;
}

void main() {
    // Sample G-Buffer
    vec4 albedoAlpha = texture(uGBufferAlbedo, vTexCoord);
    vec3 albedo = albedoAlpha.rgb;
    vec4 normalEncoded = texture(uGBufferNormal, vTexCoord);
    vec3 N = normalize(normalEncoded.rgb * 2.0 - 1.0);
    vec4 metalRough = texture(uGBufferMetalRough, vTexCoord);
    float metallic = metalRough.r;
    float roughness = metalRough.g;
    float ao = metalRough.b;
    vec3 emission = texture(uGBufferEmission, vTexCoord).rgb;
    float depth = texture(uGBufferDepth, vTexCoord).r;

    // Reconstruct world position
    vec3 worldPos = reconstructWorldPos(vTexCoord, depth);
    vec3 V = normalize(uCameraPosition - worldPos);

    // F0: reflectance at normal incidence
    vec3 F0 = mix(vec3(0.04), albedo, metallic);

    // Directional light
    vec3 L = normalize(-uLightDirection);
    vec3 H = normalize(V + L);
    vec3 radiance = uLightColor * uLightIntensity;

    // Cook-Torrance BRDF
    float NDF = distributionGGX(N, H, roughness);
    float G = geometrySmith(N, V, L, roughness);
    vec3 F = fresnelSchlick(max(dot(H, V), 0.0), F0);

    vec3 numerator = NDF * G * F;
    float denominator = 4.0 * max(dot(N, V), 0.0) * max(dot(N, L), 0.0) + 0.0001;
    vec3 specular = numerator / denominator;

    vec3 kD = (1.0 - F) * (1.0 - metallic);
    float NdotL = max(dot(N, L), 0.0);
    vec3 Lo = (kD * albedo / PI + specular) * radiance * NdotL;

    vec3 ambient = uAmbientColor * albedo * ao;
    vec3 color = ambient + Lo + emission;

    // Tone mapping + gamma
    color = color / (color + 1.0);
    color = pow(color, vec3(1.0 / 2.2));

    fragColor = vec4(color, albedoAlpha.a);
}
)GLSL";

// Full-screen quad vertex shader (used by post-processing passes)
inline const char* FULLSCREEN_QUAD_VERTEX_GLSL = R"GLSL(#version 460 core

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec2 aTexCoord;

out vec2 vTexCoord;

void main() {
    vTexCoord = aTexCoord;
    gl_Position = vec4(aPosition, 1.0);
}
)GLSL";

} // namespace solra::render
