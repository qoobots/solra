package com.solra.crt.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LightingPreset 单元测试 (CRT-006)。
 */
@DisplayName("LightingPreset 灯光预设")
class LightingPresetTest {

    @Test
    @DisplayName("应提供≥10种预设")
    void shouldHaveAtLeast10Presets() {
        List<LightingPreset.PresetSummary> presets = LightingPreset.listAll();
        assertTrue(presets.size() >= 10,
                "Expected ≥10 presets, got " + presets.size());
    }

    @Test
    @DisplayName("所有预设应生成有效的 LightingConfig")
    void allPresetsShouldGenerateValidConfig() {
        for (LightingPreset.PresetId presetId : LightingPreset.PresetId.values()) {
            LightingPreset preset = LightingPreset.from(presetId);
            assertNotNull(preset.getConfig(), "Config should not be null for " + presetId);
            assertNotNull(preset.getConfig().getLights(), "Lights should not be null for " + presetId);
            assertFalse(preset.getConfig().getLights().isEmpty(),
                    "Lights should not be empty for " + presetId);
        }
    }

    @Test
    @DisplayName("每个预设应有标签")
    void eachPresetShouldHaveTags() {
        for (LightingPreset.PresetId presetId : LightingPreset.PresetId.values()) {
            LightingPreset preset = LightingPreset.from(presetId);
            assertNotNull(preset.getTags(), "Tags should not be null for " + presetId);
            assertFalse(preset.getTags().isEmpty(),
                    "Tags should not be empty for " + presetId);
        }
    }

    @Test
    @DisplayName("每个预设应有预览参数")
    void eachPresetShouldHavePreviewParams() {
        for (LightingPreset.PresetId presetId : LightingPreset.PresetId.values()) {
            LightingPreset preset = LightingPreset.from(presetId);
            assertTrue(preset.getPreviewRotationSpeed() > 0,
                    "PreviewRotationSpeed should be positive for " + presetId);
            assertTrue(preset.getPreviewExposure() > 0,
                    "PreviewExposure should be positive for " + presetId);
        }
    }

    @Test
    @DisplayName("可按氛围类型筛选预设")
    void shouldFilterByAtmosphere() {
        List<LightingPreset.PresetSummary> warmPresets =
                LightingPreset.listByAtmosphere(LightingPreset.Atmosphere.WARM);
        assertFalse(warmPresets.isEmpty());
        assertTrue(warmPresets.stream()
                .allMatch(p -> p.getAtmosphere().equals("WARM")));
    }

    @Test
    @DisplayName("暖居预设应包含暖色光源")
    void warmHomeShouldHaveWarmLights() {
        LightingPreset preset = LightingPreset.from(LightingPreset.PresetId.WARM_HOME);
        ProjectConfig.LightingConfig config = preset.getConfig();
        // 至少有一个暖色灯光
        boolean hasWarm = config.getLights().stream()
                .anyMatch(l -> l.getColor()[0] >= 0.9f && l.getColor()[1] >= 0.7f);
        assertTrue(hasWarm, "Warm home should have warm-colored lights");
    }

    @Test
    @DisplayName("霓虹赛博预设应有多色光源")
    void neonCyberpunkShouldHaveMultipleColors() {
        LightingPreset preset = LightingPreset.from(LightingPreset.PresetId.NEON_CYBERPUNK);
        ProjectConfig.LightingConfig config = preset.getConfig();
        assertTrue(config.getLights().size() >= 3,
                "Neon cyberpunk should have at least 3 lights");
    }

    @Test
    @DisplayName("listAll 返回完整预设摘要")
    void listAllShouldReturnCompleteSummary() {
        List<LightingPreset.PresetSummary> summaries = LightingPreset.listAll();
        for (LightingPreset.PresetSummary s : summaries) {
            assertNotNull(s.getId());
            assertNotNull(s.getDisplayName());
            assertNotNull(s.getDescription());
            assertNotNull(s.getAtmosphere());
        }
    }
}
