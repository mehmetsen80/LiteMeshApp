package org.lite.gateway.util;

import org.lite.gateway.dto.ErrorCode;
import org.lite.gateway.exception.ValidationException;
import org.lite.gateway.dto.RegisterRequest;

public class UserValidationUtil {
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@(.+)$";
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final String PASSWORD_REGEX = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9])(?=\\S+$).{8,}$";
    private static final String USERNAME_REGEX = "^[a-zA-Z0-9_-]{6,}$";

    public static void validateRegistration(RegisterRequest request) {
        validatePassword(request.getPassword());
        validateEmail(request.getEmail());
        validateUsername(request.getUsername());
    }

    public static void validatePassword(String password) {
        if (password == null || !password.matches(PASSWORD_REGEX)) {
            throw new ValidationException(ErrorCode.USER_INVALID_PASSWORD.getDefaultMessage());
        }
    }

    public static void validateEmail(String email) {
        if (email == null || !email.matches(EMAIL_REGEX)) {
            throw new ValidationException(ErrorCode.USER_INVALID_EMAIL.getDefaultMessage());
        }
    }

    public static void validateUsername(String username) {
        if (username == null || !username.matches(USERNAME_REGEX)) {
            throw new ValidationException(ErrorCode.USER_INVALID_USERNAME.getDefaultMessage());
        }
    }

    /**
     * Validates password strength and returns specific validation errors
     * @param password the password to validate
     * @return validation error message if password is weak, null if password is strong
     */
    public static String getPasswordStrengthError(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Password must contain at least one uppercase letter";
        }
        if (!password.matches(".*[0-9].*")) {
            return "Password must contain at least one number";
        }
        if (!password.matches(".*[^A-Za-z0-9].*")) {
            return "Password must contain at least one special character";
        }
        if (password.contains(" ")) {
            return "Password cannot contain spaces";
        }
        return null;
    }

    /**
     * Checks if a password meets the minimum strength requirements
     * @param password the password to check
     * @return true if password is strong enough, false otherwise
     */
    public static boolean isStrongPassword(String password) {
        return password != null && password.matches(PASSWORD_REGEX);
    }
} 