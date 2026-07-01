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

} // namespace solra::render
