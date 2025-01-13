package org.lite.gateway.dto;

public record ErrorResponse(
    String code,
    String message,
    int status
) {} 