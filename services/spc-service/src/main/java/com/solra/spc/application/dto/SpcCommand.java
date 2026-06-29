package com.solra.spc.application.dto;

import com.solra.spc.domain.model.SpaceCategory;
import java.util.List;

public class SpcCommand {

    public record ListSpacesCommand(String userId, String mode, int offset, int limit, List<SpaceCategory> categories) {}

    public record ReportActionCommand(String userId, String spaceId, String actionType, long dwellDurationMs) {}
}
