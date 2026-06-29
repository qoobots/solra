package com.solra.saf.application.dto;

import java.util.List;

public record FilterResultDTO(
    boolean passed,
    float overallScore,
    String filteredContent,
    List<ViolationDTO> violations
) {
    public record ViolationDTO(String category, String severity, String description) {}
}
