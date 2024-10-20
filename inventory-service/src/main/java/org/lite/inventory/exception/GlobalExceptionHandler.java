package org.lite.inventory.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        // Log the exception for debugging purposes
        log.debug("Handling RuntimeException: {}", ex.getMessage());

        // Return 500 Internal Server Error with a custom message
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("[Inventory-Service] Error occurred: " + ex.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<?> handleIOException(IOException ex) {
        log.error("IOException: {}", ex.getMessage());
        Map<String, String> response = new HashMap<>();
        response.put("error", "IOException");
        response.put("message", ex.getMessage());//.body("Custom error message: Database connection failed");
        return new ResponseEntity<>(response, HttpStatus.HTTP_VERSION_NOT_SUPPORTED);
    }

    // You can add more specific handlers for different exceptions
}
