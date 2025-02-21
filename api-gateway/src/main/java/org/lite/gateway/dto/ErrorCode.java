package org.lite.gateway.dto;

import lombok.Getter;

/**
 * Enumeration of all possible error codes in the application.
 * Error codes are categorized by domain and include severity levels.
 */
@Getter
public enum ErrorCode {
    // Team Related Errors (1000-1999)
    INVALID_TEAM("TEAM_1001", "Invalid team data", ErrorSeverity.ERROR, ErrorCategory.VALIDATION),
    TEAM_NAME_REQUIRED("TEAM_1002", "Team name is required", ErrorSeverity.ERROR, ErrorCategory.VALIDATION),
    TEAM_NOT_FOUND("TEAM_1003", "Team not found", ErrorSeverity.ERROR, ErrorCategory.NOT_FOUND),
    TEAM_OPERATION_ERROR("TEAM_1004", "Team operation failed", ErrorSeverity.ERROR, ErrorCategory.OPERATION),
    TEAM_DELETE_ERROR("TEAM_1005", "Team deletion failed", ErrorSeverity.ERROR, ErrorCategory.OPERATION),
    TEAM_ACTIVATE_ERROR("TEAM_1006", "Team activation failed", ErrorSeverity.ERROR, ErrorCategory.OPERATION),
    TEAM_DEACTIVATE_ERROR("TEAM_1007", "Team deactivation failed", ErrorSeverity.ERROR, ErrorCategory.OPERATION),
    TEAM_ALREADY_EXISTS("TEAM_1008", "Team already exists", ErrorSeverity.ERROR, ErrorCategory.VALIDATION),
    TEAM_UPDATE_ERROR("TEAM_1009", "Failed to update team", ErrorSeverity.ERROR, ErrorCategory.OPERATION),
    TEAM_ROUTE_NOT_FOUND("TEAM_1010", "Team route not found", ErrorSeverity.ERROR, ErrorCategory.NOT_FOUND),

    // Member Related Errors (2000-2999)
    INVALID_ROLE("MEMBER_2001", "Invalid member role", ErrorSeverity.ERROR, ErrorCategory.VALIDATION),
    MEMBER_NOT_FOUND("MEMBER_2002", "Member not found", ErrorSeverity.ERROR, ErrorCategory.NOT_FOUND),
    MEMBER_ALREADY_EXISTS("MEMBER_2003", "Member already exists in team", ErrorSeverity.ERROR, ErrorCategory.VALIDATION),
    MEMBER_OPERATION_ERROR("MEMBER_2004", "Member operation failed", ErrorSeverity.ERROR, ErrorCategory.OPERATION),
    MEMBER_REMOVE_ERROR("MEMBER_2005", "Failed to remove team member", ErrorSeverity.ERROR, ErrorCategory.OPERATION),

    // Route Related Errors (3000-3999)
    ROUTE_NOT_FOUND("ROUTE_3001", "Route not found", ErrorSeverity.ERROR, ErrorCategory.NOT_FOUND),
    ROUTE_ALREADY_ASSIGNED("ROUTE_3002", "Route already assigned", ErrorSeverity.ERROR, ErrorCategory.VALIDATION),
    ROUTE_OPERATION_ERROR("ROUTE_3003", "Route operation failed", ErrorSeverity.ERROR, ErrorCategory.OPERATION),
    ROUTE_PATH_REQUIRED("ROUTE_3004", "Route path is required", ErrorSeverity.ERROR, ErrorCategory.VALIDATION),
    ROUTE_IDENTIFIER_REQUIRED("ROUTE_3005", "Route identifier is required", ErrorSeverity.ERROR, ErrorCategory.VALIDATION),
    ROUTE_URI_REQUIRED("ROUTE_3006", "Route URI is required", ErrorSeverity.ERROR, ErrorCategory.VALIDATION),
    ROUTE_ALREADY_EXISTS("ROUTE_3007", "Route already exists", ErrorSeverity.ERROR, ErrorCategory.VALIDATION),
    ROUTE_VERSION_NOT_FOUND("ROUTE_3008", "Route version not found", ErrorSeverity.ERROR, ErrorCategory.NOT_FOUND),
    ROUTE_VERSION_COMPARISON_ERROR("ROUTE_3009", "Route version comparison failed", ErrorSeverity.ERROR, ErrorCategory.OPERATION),

    // Authentication/Authorization Errors (4000-4999)
    UNAUTHORIZED("AUTH_4001", "Unauthorized access", ErrorSeverity.ERROR, ErrorCategory.SECURITY),
    FORBIDDEN("AUTH_4002", "Forbidden operation", ErrorSeverity.ERROR, ErrorCategory.SECURITY),
    INVALID_TOKEN("AUTH_4003", "Invalid or expired token", ErrorSeverity.ERROR, ErrorCategory.SECURITY),
    AUTHENTICATION_ERROR("AUTH_4004", "Authentication failed", ErrorSeverity.ERROR, ErrorCategory.SECURITY),
    MISSING_TOKEN("AUTH_4005", "No authentication token found", ErrorSeverity.ERROR, ErrorCategory.SECURITY),
    INVALID_JWT_CLAIMS("AUTH_4006", "Invalid JWT claims", ErrorSeverity.ERROR, ErrorCategory.SECURITY),
    TOKEN_EXPIRED("AUTH_4007", "Authentication token has expired", ErrorSeverity.ERROR, ErrorCategory.SECURITY),

    // General System Errors (5000-5999)
    INTERNAL_ERROR("SYS_5001", "Internal server error", ErrorSeverity.CRITICAL, ErrorCategory.SYSTEM),
    SERVICE_UNAVAILABLE("SYS_5002", "Service temporarily unavailable", ErrorSeverity.CRITICAL, ErrorCategory.SYSTEM),
    VALIDATION_ERROR("SYS_5003", "Validation error", ErrorSeverity.WARNING, ErrorCategory.VALIDATION),

    // User Related Errors (6000-6999)
    USER_NOT_FOUND("USER_6001", "User not found: %s", ErrorSeverity.ERROR, ErrorCategory.NOT_FOUND),
    USER_INVALID_CREDENTIALS("USER_6002", "Invalid username or password", ErrorSeverity.ERROR, ErrorCategory.SECURITY),
    USER_ALREADY_EXISTS("USER_6003", "User already exists", ErrorSeverity.ERROR, ErrorCategory.VALIDATION),
    USER_EMAIL_EXISTS("USER_6004", "Email already exists", ErrorSeverity.ERROR, ErrorCategory.VALIDATION),
    USER_INVALID_PASSWORD("USER_6005", "Password must be at least 8 characters long and contain at least one uppercase letter, one number, and one special character", ErrorSeverity.ERROR, ErrorCategory.VALIDATION),
    USER_INVALID_EMAIL("USER_6006", "Invalid email format", ErrorSeverity.ERROR, ErrorCategory.VALIDATION),
    USER_INVALID_USERNAME("USER_6007", "Username must be at least 6 characters long", ErrorSeverity.ERROR, ErrorCategory.VALIDATION),

    // Organization Related Errors (7000-7999)
    INVALID_ORGANIZATION("ORG_7001", "Invalid organization data", ErrorSeverity.ERROR, ErrorCategory.VALIDATION),
    ORGANIZATION_NAME_REQUIRED("ORG_7002", "Organization name is required", ErrorSeverity.ERROR, ErrorCategory.VALIDATION),
    ORGANIZATION_NOT_FOUND("ORG_7003", "Organization not found", ErrorSeverity.ERROR, ErrorCategory.NOT_FOUND),
    ORGANIZATION_OPERATION_ERROR("ORG_7004", "Organization operation failed", ErrorSeverity.ERROR, ErrorCategory.OPERATION),
    ORGANIZATION_DELETE_ERROR("ORG_7005", "Organization deletion failed", ErrorSeverity.ERROR, ErrorCategory.OPERATION),
    ORGANIZATION_ALREADY_EXISTS("ORG_7006", "Organization already exists", ErrorSeverity.ERROR, ErrorCategory.VALIDATION),
    ORGANIZATION_UPDATE_ERROR("ORG_7007", "Failed to update organization", ErrorSeverity.ERROR, ErrorCategory.OPERATION),

    // New errors
    CODE_ALREADY_USED("AUTH_4008", "Code already used", ErrorSeverity.ERROR, ErrorCategory.SECURITY),
    INVALID_CODE("AUTH_4009", "Invalid code", ErrorSeverity.ERROR, ErrorCategory.SECURITY),
    AUTHENTICATION_FAILED("AUTH_4010", "Authentication failed", ErrorSeverity.ERROR, ErrorCategory.SECURITY);

    private final String code;
    private final String defaultMessage;
    private final ErrorSeverity severity;
    private final ErrorCategory category;

    ErrorCode(String code, String defaultMessage, ErrorSeverity severity, ErrorCategory category) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.severity = severity;
        this.category = category;
    }

    ErrorCode(String code) {
        this.code = code;
        this.defaultMessage = null;
        this.severity = null;
        this.category = null;
    }

    public String getCode() {
        return code;
    }
}

/**
 * Categorizes errors by their domain or type.
 */
enum ErrorCategory {
    VALIDATION,
    OPERATION,
    NOT_FOUND,
    SECURITY,
    SYSTEM
}

/**
 * Indicates the severity level of an error.
 */
enum ErrorSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
} 