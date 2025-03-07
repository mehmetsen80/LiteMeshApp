package org.lite.gateway.dto;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

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
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String code;
    private String message;
    private Map<String, Object> details;

    /**
     * Creates an ErrorResponse from an ErrorCode.
     */
    public static ErrorResponse fromErrorCode(ErrorCode errorCode, int httpStatus) {
        return ErrorResponse.builder()
            .code(errorCode.name())
            .message(errorCode.getDefaultMessage())
            .details(Map.of("status", httpStatus))
            .build();
    }

    /**
     * Creates an ErrorResponse from an ErrorCode with a custom message.
     */
    public static ErrorResponse fromErrorCode(ErrorCode errorCode, String message, int httpStatus) {
        return ErrorResponse.builder()
            .code(errorCode.name())
            .message(message)
            .details(Map.of("status", httpStatus))
            .build();
    }

    public static ErrorResponse fromError(String code, String message, Map<String, Object> details) {
        return ErrorResponse.builder()
            .code(code)
            .message(message)
            .details(details)
            .build();
    }
} 