package org.lite.gateway.dto;

import java.util.Map;

public record ValidationErrorResponse(
    String code,
    String message,
    int status,
    Map<String, String> errors
) {} 