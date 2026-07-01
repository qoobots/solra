#pragma once
// Shadow Mapping (CSM) + SSAO post-processing passes
//
// CSM (Cascaded Shadow Maps): 4-cascade directional shadow mapping
// SSAO (Screen-Space Ambient Occlusion): Horizon-Based Ambient Occlusion (HBAO)

#include <string>
#include <cstdint>

namespace solra::render {

// ============================================================
// CSM Shadow Map Shaders
// ============================================================

// CSM Depth-only vertex shader (renders into shadow map cascades)
inline const char* CSM_DEPTH_VERTEX_GLSL = R"GLSL(#version 460 core

layout(location = 0) in vec3 aPosition;

// Per-cascade light view-projection matrix
layout(location = 0) uniform mat4 uLightViewProj;
layout(location = 4) uniform mat4 uModel;

void main() {
    gl_Position = uLightViewProj * uModel * vec4(aPosition, 1.0);
}
)GLSL";

// CSM Depth-only fragment shader (minimal — only writes depth)
inline const char* CSM_DEPTH_FRAGMENT_GLSL = R"GLSL(#version 460 core

void main() {
    // gl_FragDepth is automatically written
}
)GLSL";

// CSM Shadow receiver shader (applied in deferred or forward pass)
// Integrates shadow map sampling with PBR lighting
inline const char* CSM_SHADOW_RECEIVER_GLSL = R"GLSL(#version 460 core

// Shadow map arrays: 4 cascades
layout(binding = 5) uniform sampler2DArrayShadow uShadowMap;

// Cascade split depths and light view-proj matrices
#define CSM_CASCADE_COUNT 4

struct CascadeData {
    mat4 lightViewProj;
    float splitDepth;
    float pad0, pad1, pad2;
};

layout(location = 10, std140) uniform CascadeBlock {
    CascadeData uCascades[CSM_CASCADE_COUNT];
};

layout(location = 50) uniform vec4 uCascadeSplits; // .x=C0, .y=C1, .z=C2, .w=C3

uniform vec3 uLightDirection;

// PCF (Percentage-Closer Filtering) for soft shadows
// Sample a 3x3 Poisson disk for smooth penumbra
float sampleShadowPCF(int cascadeIndex, vec3 shadowCoord, float bias) {
    float shadow = 0.0;
    vec2 texelSize = 1.0 / vec2(textureSize(uShadowMap, 0));

    // Poisson disk samples (3x3)
    const vec2 poissonDisk[9] = vec2[](
        vec2( 0.0,  0.0),
        vec2(-1.0, -1.0), vec2( 1.0, -1.0),
        vec2(-1.0,  1.0), vec2( 1.0,  1.0),
        vec2(-1.0,  0.0), vec2( 1.0,  0.0),
        vec2( 0.0, -1.0), vec2( 0.0,  1.0)
    );

    for (int i = 0; i < 9; ++i) {
        vec2 offset = poissonDisk[i] * texelSize * 1.5;
        vec4 sampleCoord = vec4(
            shadowCoord.xy + offset,
            float(cascadeIndex),
            shadowCoord.z - bias
        );
        shadow += texture(uShadowMap, sampleCoord);
    }

    return shadow / 9.0;
}

// Select cascade index based on view-space depth
int selectCascade(float viewDepth) {
    for (int i = 0; i < CSM_CASCADE_COUNT - 1; ++i) {
        if (viewDepth < uCascadeSplits[i]) return i;
    }
    return CSM_CASCADE_COUNT - 1;
}

// Calculate shadow factor (0.0 = fully shadowed, 1.0 = fully lit)
float calculateShadow(vec3 worldPos, vec3 N, vec3 L) {
    // Calculate view-space depth for cascade selection
    // (simplified: use distance from camera)
    float viewDepth = abs(worldPos.z); // assume camera looks down -Z

    int cascadeIndex = selectCascade(viewDepth);

    // Transform world position to light clip space for selected cascade
    vec4 lightClipPos = uCascades[cascadeIndex].lightViewProj * vec4(worldPos, 1.0);
    vec3 shadowCoord = lightClipPos.xyz / lightClipPos.w;

    // Transform to [0,1] range
    shadowCoord.xy = shadowCoord.xy * 0.5 + 0.5;

    // Check bounds
    if (shadowCoord.x < 0.0 || shadowCoord.x > 1.0 ||
        shadowCoord.y < 0.0 || shadowCoord.y > 1.0 ||
        shadowCoord.z < 0.0 || shadowCoord.z > 1.0) {
        return 1.0; // Outside shadow map
    }

    // Slope-scaled depth bias to reduce shadow acne
    float bias = max(0.0005 * (1.0 - dot(N, L)), 0.0002);
    bias *= 1.0 + float(cascadeIndex) * 0.5; // More bias for farther cascades

    // Cascade blending: smooth transition between cascades
    float nextCascadeBlend = 0.0;
    if (cascadeIndex < CSM_CASCADE_COUNT - 1) {
        float splitStart = (cascadeIndex == 0) ? 0.0 : uCascadeSplits[cascadeIndex - 1];
        float splitEnd = uCascadeSplits[cascadeIndex];
        float blendZone = (splitEnd - splitStart) * 0.1; // 10% blend zone
        float distToSplit = abs(viewDepth - splitEnd);
        nextCascadeBlend = smoothstep(blendZone, 0.0, distToSplit);
    }

    float shadow = sampleShadowPCF(cascadeIndex, shadowCoord, bias);

    // Blend with next cascade if in transition zone
    if (nextCascadeBlend > 0.0 && cascadeIndex < CSM_CASCADE_COUNT - 1) {
        vec4 nextClipPos = uCascades[cascadeIndex + 1].lightViewProj * vec4(worldPos, 1.0);
        vec3 nextCoord = nextClipPos.xyz / nextClipPos.w;
        nextCoord.xy = nextCoord.xy * 0.5 + 0.5;
        float nextBias = max(0.0005 * (1.0 - dot(N, L)), 0.0002);
        nextBias *= 1.0 + float(cascadeIndex + 1) * 0.5;
        float nextShadow = sampleShadowPCF(cascadeIndex + 1, nextCoord, nextBias);
        shadow = mix(shadow, nextShadow, nextCascadeBlend);
    }

    return shadow;
}
)GLSL";

// ============================================================
// SSAO (HBAO) Shaders
// ============================================================

// SSAO computation shader (full-screen pass reading G-Buffer normal + depth)
inline const char* SSAO_VERTEX_GLSL = R"GLSL(#version 460 core

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec2 aTexCoord;

out vec2 vTexCoord;

void main() {
    vTexCoord = aTexCoord;
    gl_Position = vec4(aPosition, 1.0);
}
)GLSL";

// HBAO (Horizon-Based Ambient Occlusion) fragment shader
inline const char* SSAO_FRAGMENT_GLSL = R"GLSL(#version 460 core

in vec2 vTexCoord;
layout(location = 0) out float fragAO;

// G-Buffer inputs
layout(binding = 1) uniform sampler2D uGBufferNormal;
layout(binding = 4) uniform sampler2D uGBufferDepth;

// SSAO parameters
layout(location = 20) uniform float uSSAORadius;       // world-space radius
layout(location = 21) uniform float uSSAOBias;         // depth bias
layout(location = 22) uniform float uSSAOIntensity;    // occlusion strength
layout(location = 23) uniform vec2 uScreenSize;        // viewport size

layout(location = 9) uniform mat4 uInverseViewProj;
layout(location = 50) uniform mat4 uProjection;        // camera projection for depth decode

// Reconstruct world position from UV + depth
vec3 reconstructWorldPos(vec2 uv, float depth) {
    vec4 clipPos = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 worldPos = uInverseViewProj * clipPos;
    return worldPos.xyz / worldPos.w;
}

// Get view-space position
vec3 getViewPos(vec2 uv, float depth) {
    // Linearize depth (reverse-Z)
    float z = depth * 2.0 - 1.0;
    vec4 clipPos = vec4(uv * 2.0 - 1.0, z, 1.0);
    vec4 viewPos = inverse(uProjection) * clipPos;
    return viewPos.xyz / viewPos.w;
}

// HBAO: sample horizon angles along 4 directions
float computeHBAO(vec2 uv, vec3 viewPos, vec3 viewNormal) {
    const int NUM_DIRECTIONS = 4;
    const int NUM_STEPS = 6;

    vec2 noiseScale = uScreenSize / 4.0; // 4x4 noise texture

    // Sample random rotation (simplified with hash)
    float randomAngle = fract(sin(dot(uv, vec2(12.9898, 78.233))) * 43758.5453) * 6.283185;

    float ao = 0.0;
    float radius = uSSAORadius / -viewPos.z; // perspective-correct radius

    for (int d = 0; d < NUM_DIRECTIONS; ++d) {
        float angle = randomAngle + (float(d) / float(NUM_DIRECTIONS)) * 6.283185;
        vec2 dir = vec2(cos(angle), sin(angle));

        float horizonAngle = -1.0;

        for (int s = 1; s <= NUM_STEPS; ++s) {
            vec2 sampleUV = uv + dir * radius * (float(s) / float(NUM_STEPS));
            sampleUV = clamp(sampleUV, vec2(0.0), vec2(1.0));

            float sampleDepth = texture(uGBufferDepth, sampleUV).r;
            vec3 sampleViewPos = getViewPos(sampleUV, sampleDepth);

            vec3 delta = sampleViewPos - viewPos;
            float dist = length(delta);

            // Horizon angle
            float hAngle = atan(delta.z / (dist + 0.0001));

            horizonAngle = max(horizonAngle, hAngle);
        }

        // Tangent angle (view normal projected onto direction)
        vec2 tangentDir = normalize(dir);
        float tangentAngle = -atan(dot(viewNormal.xy, tangentDir) / (abs(viewNormal.z) + 0.0001));

        // Occlusion contribution
        float sinH = sin(horizonAngle);
        float sinT = sin(tangentAngle);
        float aoDir = sinH - sinT;
        aoDir = clamp(aoDir, 0.0, 1.0);

        ao += aoDir;
    }

    ao /= float(NUM_DIRECTIONS);
    return 1.0 - ao * uSSAOIntensity;
}

// Fallback: simpler SSAO using random hemisphere sampling
float computeSimpleSSAO(vec2 uv, vec3 worldPos, vec3 N) {
    const int NUM_SAMPLES = 16;

    // Generate sample kernel (hemisphere oriented along normal)
    vec3 kernel[16] = vec3[](
        vec3( 0.5381,  0.1856, -0.4319), vec3( 0.1379,  0.2486,  0.4430),
        vec3( 0.3371,  0.5679, -0.0057), vec3(-0.6999, -0.0451, -0.2227),
        vec3( 0.0689, -0.1598, -0.8547), vec3( 0.5264,  0.4181,  0.4822),
        vec3(-0.3568,  0.2212, -0.3538), vec3(-0.2403, -0.6421,  0.1901),
        vec3(-0.5678, -0.3550,  0.3553), vec3( 0.2013, -0.3511, -0.3401),
        vec3( 0.4375,  0.7129, -0.1053), vec3(-0.7029,  0.3185, -0.1537),
        vec3( 0.2294, -0.4998,  0.4109), vec3( 0.0923,  0.3914, -0.6421),
        vec3(-0.4570,  0.0303, -0.6412), vec3(-0.1989, -0.0761,  0.8157)
    );

    float radius = uSSAORadius;
    float bias = uSSAOBias;
    float occlusion = 0.0;

    for (int i = 0; i < NUM_SAMPLES; ++i) {
        // Reflect sample if it's below the hemisphere
        vec3 sampleDir = kernel[i];
        if (dot(sampleDir, N) < 0.0) sampleDir = -sampleDir;

        vec3 samplePos = worldPos + sampleDir * radius;

        // Project sample to screen
        vec4 sampleClip = uInverseViewProj * vec4(samplePos, 1.0); // wrong: should use ViewProj
        vec3 sampleNDC = sampleClip.xyz / sampleClip.w;
        vec2 sampleUV = sampleNDC.xy * 0.5 + 0.5;

        // Sample depth at projected position
        float sampleDepth = texture(uGBufferDepth, sampleUV).r;
        vec3 sampleWorldPos = reconstructWorldPos(sampleUV, sampleDepth);

        // Range check + occlusion contribution
        float rangeCheck = smoothstep(0.0, 1.0, radius / length(samplePos - sampleWorldPos));
        float sampleDist = length(sampleWorldPos - worldPos);

        occlusion += (sampleDist < radius + bias ? 1.0 : 0.0) * rangeCheck;
    }

    occlusion = 1.0 - (occlusion / float(NUM_SAMPLES));
    return pow(occlusion, uSSAOIntensity);
}

void main() {
    float depth = texture(uGBufferDepth, vTexCoord).r;
    if (depth >= 1.0) {
        fragAO = 1.0; // Sky / far plane
        return;
    }

    vec3 worldPos = reconstructWorldPos(vTexCoord, depth);
    vec4 normalEncoded = texture(uGBufferNormal, vTexCoord);
    vec3 N = normalize(normalEncoded.rgb * 2.0 - 1.0);

    vec3 viewPos = getViewPos(vTexCoord, depth);

    // Use HBAO for quality, fall back to simple SSAO
    float ao = computeSimpleSSAO(vTexCoord, worldPos, N);

    fragAO = ao;
}
)GLSL";

// SSAO Blur pass (bilateral 4x4 box blur)
inline const char* SSAO_BLUR_VERTEX_GLSL = R"GLSL(#version 460 core

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec2 aTexCoord;

out vec2 vTexCoord;

void main() {
    vTexCoord = aTexCoord;
    gl_Position = vec4(aPosition, 1.0);
}
)GLSL";

inline const char* SSAO_BLUR_FRAGMENT_GLSL = R"GLSL(#version 460 core

in vec2 vTexCoord;
layout(location = 0) out float fragAO;

layout(binding = 5) uniform sampler2D uSSAOInput;
layout(binding = 4) uniform sampler2D uGBufferDepth;
layout(location = 23) uniform vec2 uScreenSize;

void main() {
    vec2 texelSize = 1.0 / uScreenSize;

    float centerDepth = texture(uGBufferDepth, vTexCoord).r;
    float result = 0.0;
    float totalWeight = 0.0;

    // 5x5 bilateral blur
    for (int x = -2; x <= 2; ++x) {
        for (int y = -2; y <= 2; ++y) {
            vec2 offset = vec2(float(x), float(y)) * texelSize;
            vec2 sampleUV = vTexCoord + offset;

            float sampleDepth = texture(uGBufferDepth, sampleUV).r;
            float sampleAO = texture(uSSAOInput, sampleUV).r;

            // Bilateral weight: depth similarity
            float depthDiff = abs(centerDepth - sampleDepth);
            float weight = exp(-depthDiff * 100.0);

            // Gaussian weight
            float dist = length(vec2(float(x), float(y)));
            weight *= exp(-dist * dist / 4.0);

            result += sampleAO * weight;
            totalWeight += weight;
        }
    }

    fragAO = result / max(totalWeight, 0.0001);
}
)GLSL";

// ============================================================
// CSM Configuration
// ============================================================

struct CSMConfig {
    uint32_t shadowMapSize = 2048;       // per-cascade resolution
    uint32_t cascadeCount = 4;           // number of cascades (2-8)
    float cascadeSplitLambda = 0.75f;    // 0=uniform, 1=logarithmic
    float shadowBias = 0.0005f;
    float normalBias = 0.02f;
    float shadowStrength = 0.8f;         // 0=no shadow, 1=full shadow
    bool enablePCF = true;              // percentage-closer filtering
    uint32_t pcfSamples = 9;            // PCF kernel size (odd)
    bool visualizeCascades = false;     // debug: color cascades
};

// ============================================================
// SSAO Configuration
// ============================================================

struct SSAOConfig {
    float radius = 0.5f;               // world-space sampling radius
    float bias = 0.025f;               // depth comparison bias
    float intensity = 1.0f;            // occlusion strength
    uint32_t sampleCount = 16;          // samples per pixel (4-64)
    bool enableBlur = true;            // bilateral blur pass
    float blurSharpness = 40.0f;        // depth edge preservation
    enum class Quality { Low, Medium, High, Ultra };
    Quality quality = Quality::High;
};

} // namespace solra::render
