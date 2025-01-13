package org.lite.gateway.dto;

import org.lite.gateway.exception.GlobalExceptionHandler.ErrorCategory;
import java.util.Map;

public record DetailedErrorResponse(
    String code,
    String message,
    int status,
    String trackingId,
    String errorType,
    ErrorCategory category,
    Map<String, Object> details
) {} 