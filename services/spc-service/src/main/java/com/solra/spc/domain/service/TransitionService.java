package com.solra.spc.domain.service;

import com.solra.spc.domain.model.*;
import com.solra.spc.domain.repository.SpaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * TransitionService — SPC-006 + SPC-007 空间过渡体验服务。
 *
 * SPC-006: 空间加载过渡动效（卡片→全屏过渡 400-600ms）
 * SPC-007: 空间退出过渡与卡片流（退出+下一个预览无缝衔接 <2秒）
 */
public class TransitionService {

    private static final Logger log = LoggerFactory.getLogger(TransitionService.class);

    /** 标准过渡时长（毫秒） */
    private static final int DEFAULT_TRANSITION_DURATION_MS = 500;

    /** 退出+预览衔接最大时长（毫秒） */
    private static final int MAX_EXIT_FLOW_DURATION_MS = 2000;

    private final SpaceRepository spaceRepo;

    public TransitionService(SpaceRepository spaceRepo) {
        this.spaceRepo = spaceRepo;
    }

    /**
     * SPC-006: Get loading transition configuration for entering a space.
     * Returns animation parameters: duration, easing, effect type.
     */
    public LoadingTransition getLoadingTransition(String spaceId) {
        Space space = spaceRepo.findById(spaceId)
                .orElseThrow(() -> new IllegalArgumentException("Space not found: " + spaceId));

        // Select transition effect based on space category
        TransitionEffect effect = selectLoadingEffect(space);

        // Estimate duration based on space complexity
        int durationMs = estimateLoadingDuration(space);

        return new LoadingTransition(
                spaceId,
                effect,
                durationMs,
                effect.getEasing(),
                generateTransitionKeyframes(space, effect),
                space.getMeta() != null ? space.getMeta().getThumbnailUrl() : null
        );
    }

    /**
     * SPC-007: Get exit transition with next-space preview card flow.
     * Returns the exit animation + recommended next space preview.
     */
    public ExitFlow getExitFlow(String userId, String currentSpaceId,
                                 List<String> nextSpaceCandidates) {
        // 1. Build exit transition
        ExitTransition exitTransition = new ExitTransition(
                currentSpaceId,
                TransitionEffect.FADE_OUT_ZOOM,
                350, // Exit faster than enter
                "ease-in",
                generateExitKeyframes()
        );

        // 2. Select next space preview (first valid candidate)
        Space nextSpace = null;
        for (String candidateId : nextSpaceCandidates) {
            if (candidateId.equals(currentSpaceId)) continue;
            Optional<Space> opt = spaceRepo.findById(candidateId);
            if (opt.isPresent() && opt.get().getStatus() == SpaceStatus.PUBLISHED) {
                nextSpace = opt.get();
                break;
            }
        }

        // 3. Fallback: random popular space
        if (nextSpace == null) {
            List<Space> popular = spaceRepo.findPublished(0, 5, List.of(), "popular");
            nextSpace = popular.stream()
                    .filter(s -> !s.getSpaceId().equals(currentSpaceId))
                    .findFirst()
                    .orElse(null);
        }

        // 4. Build next-space preview card
        PreviewCard nextPreview = null;
        if (nextSpace != null) {
            nextPreview = new PreviewCard();
            nextPreview.setSpaceId(nextSpace.getSpaceId());
            nextPreview.setMeta(nextSpace.getMeta());
            nextPreview.setStats(nextSpace.getStats());
            nextPreview.setTags(nextSpace.getTags());
            if (nextSpace.getMeta() != null && nextSpace.getMeta().getThumbnailUrl() != null) {
                nextPreview.setPreviewImages(List.of(nextSpace.getMeta().getThumbnailUrl()));
            }
        }

        // 5. Compute total flow duration
        int totalDurationMs = exitTransition.durationMs() +
                (nextPreview != null ? 800 : 0); // Preview slide-in: 800ms

        log.debug("SPC-007 exit flow: space={} next={} totalMs={}",
                currentSpaceId, nextSpace != null ? nextSpace.getSpaceId() : "none", totalDurationMs);

        return new ExitFlow(exitTransition, nextPreview, totalDurationMs);
    }

    /**
     * SPC-006: Get transition presets for common scenarios.
     */
    public List<TransitionPreset> getPresets() {
        return List.of(
            new TransitionPreset("smooth_fade", "平滑淡入", TransitionEffect.FADE_IN,
                    400, "ease-out", "通用场景，最平滑的过渡效果"),
            new TransitionPreset("card_expand", "卡片展开", TransitionEffect.CARD_EXPAND,
                    500, "ease-in-out", "从预览卡片展开为全屏空间"),
            new TransitionPreset("zoom_enter", "缩放进入", TransitionEffect.ZOOM_IN,
                    450, "ease-out", "从缩略图放大进入空间"),
            new TransitionPreset("slide_up", "上滑进入", TransitionEffect.SLIDE_UP,
                    400, "ease-out", "从底部滑入，适合移动端手势"),
            new TransitionPreset("quick_switch", "快速切换", TransitionEffect.CROSS_FADE,
                    300, "linear", "空间间快速切换")
        );
    }

    private TransitionEffect selectLoadingEffect(Space space) {
        if (space.getMeta() == null || space.getMeta().getCategory() == null) {
            return TransitionEffect.FADE_IN;
        }
        return switch (space.getMeta().getCategory()) {
            case PERSONAL -> TransitionEffect.CARD_EXPAND;
            case SOCIAL -> TransitionEffect.ZOOM_IN;
            case ENTERTAINMENT -> TransitionEffect.SLIDE_UP;
            case ART -> TransitionEffect.FADE_IN;
            case BUSINESS -> TransitionEffect.CROSS_FADE;
            case EDUCATION -> TransitionEffect.CARD_EXPAND;
        };
    }

    private int estimateLoadingDuration(Space space) {
        // Based on total asset size
        long totalBytes = space.getContent() != null ? space.getContent().totalAssetBytes() : 0;

        if (totalBytes > 500_000_000) return 600; // >500MB
        if (totalBytes > 200_000_000) return 500; // >200MB
        if (totalBytes > 50_000_000) return 400;  // >50MB
        return 350; // Small spaces
    }

    private List<TransitionKeyframe> generateTransitionKeyframes(Space space,
                                                                   TransitionEffect effect) {
        return switch (effect) {
            case FADE_IN -> List.of(
                    new TransitionKeyframe(0f, "opacity:0;transform:scale(0.95)"),
                    new TransitionKeyframe(1f, "opacity:1;transform:scale(1)")
            );
            case CARD_EXPAND -> List.of(
                    new TransitionKeyframe(0f, "transform:scale(0.3);border-radius:16px"),
                    new TransitionKeyframe(0.6f, "transform:scale(1.02);border-radius:4px"),
                    new TransitionKeyframe(1f, "transform:scale(1);border-radius:0")
            );
            case ZOOM_IN -> List.of(
                    new TransitionKeyframe(0f, "transform:scale(0.5);opacity:0"),
                    new TransitionKeyframe(1f, "transform:scale(1);opacity:1")
            );
            case SLIDE_UP -> List.of(
                    new TransitionKeyframe(0f, "transform:translateY(100%);opacity:0"),
                    new TransitionKeyframe(1f, "transform:translateY(0);opacity:1")
            );
            case CROSS_FADE -> List.of(
                    new TransitionKeyframe(0f, "opacity:0"),
                    new TransitionKeyframe(0.5f, "opacity:0.5"),
                    new TransitionKeyframe(1f, "opacity:1")
            );
            default -> List.of(
                    new TransitionKeyframe(0f, "opacity:0"),
                    new TransitionKeyframe(1f, "opacity:1")
            );
        };
    }

    private List<TransitionKeyframe> generateExitKeyframes() {
        return List.of(
                new TransitionKeyframe(0f, "opacity:1;transform:scale(1)"),
                new TransitionKeyframe(0.7f, "opacity:0.3;transform:scale(0.9)"),
                new TransitionKeyframe(1f, "opacity:0;transform:scale(0.8)")
        );
    }

    // -- Inner types --

    /** SPC-006: 空间进入加载过渡配置 */
    public record LoadingTransition(String spaceId, TransitionEffect effect,
                                     int durationMs, String easing,
                                     List<TransitionKeyframe> keyframes,
                                     String thumbnailUrl) {}

    /** SPC-007: 空间退出+下一预览卡片流 */
    public record ExitFlow(ExitTransition exit, PreviewCard nextPreview, int totalDurationMs) {}

    public record ExitTransition(String spaceId, TransitionEffect effect,
                                  int durationMs, String easing,
                                  List<TransitionKeyframe> keyframes) {}

    /** 过渡动效关键帧 */
    public record TransitionKeyframe(float progress, String cssTransform) {}

    /** 过渡预设 */
    public record TransitionPreset(String id, String name, TransitionEffect effect,
                                    int defaultDurationMs, String easing, String description) {}

    /** 过渡动效类型 */
    public enum TransitionEffect {
        FADE_IN("ease-out"),
        CARD_EXPAND("ease-in-out"),
        ZOOM_IN("ease-out"),
        SLIDE_UP("ease-out"),
        FADE_OUT_ZOOM("ease-in"),
        CROSS_FADE("linear");

        private final String defaultEasing;

        TransitionEffect(String defaultEasing) {
            this.defaultEasing = defaultEasing;
        }

        public String getEasing() { return defaultEasing; }
    }
}
