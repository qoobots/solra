package com.solra.saf.application.dto;

import com.solra.saf.domain.model.ContentType;

public record FilterRequest(
    String userId,
    ContentType contentType,
    String content,
    String contentUrl,
    String filterLevel    // LENIENT / NORMAL / STRICT
) {}
