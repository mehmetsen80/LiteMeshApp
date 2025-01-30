package org.lite.gateway.dto;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Represents a standardized error response.
 * @param code The unique error code
 * @param message The human-readable error message
 * @param status The HTTP status code
 * @param severity The error severity level
 * @param category The error category
 */
@Data
@Builder
public class ErrorResponse {
    private ErrorCode code;
    private String message;
    private Map<String, Object> details;

    /**
     * Creates an ErrorResponse from an ErrorCode.
     */
    public static ErrorResponse fromErrorCode(ErrorCode errorCode, int httpStatus) {
        return ErrorResponse.builder()
            .code(errorCode)
            .message(errorCode.getDefaultMessage())
            .details(Map.of("status", httpStatus))
            .build();
    }

    /**
     * Creates an ErrorResponse from an ErrorCode with a custom message.
     */
    public static ErrorResponse fromErrorCode(ErrorCode errorCode, String message, int httpStatus) {
        return ErrorResponse.builder()
            .code(errorCode)
            .message(message)
            .details(Map.of("status", httpStatus))
            .build();
    }
} 