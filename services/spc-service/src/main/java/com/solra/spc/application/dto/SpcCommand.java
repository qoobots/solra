package com.solra.spc.application.dto;

import com.solra.spc.domain.model.SpaceCategory;
import java.util.List;

public class SpcCommand {

    public record ListSpacesCommand(String userId, String mode, int offset, int limit, List<SpaceCategory> categories) {}

    public record ReportActionCommand(String userId, String spaceId, String actionType, long dwellDurationMs) {}

    /** SPC-004: 搜索空间命令 */
    public record SearchSpacesCommand(String keyword, List<SpaceCategory> categories,
                                       String sortBy, int offset, int limit) {}

    /** SPC-005: 预加载命令 */
    public record PreloadCommand(String userId, String currentSpaceId, int count) {}

    /** SPC-007: 退出流命令 */
    public record ExitFlowCommand(String userId, String currentSpaceId, List<String> nextCandidates) {}

    /** SPC-008: 排行榜查询命令 */
    public record LeaderboardCommand(String period, int topN, List<SpaceCategory> categories) {}
}
