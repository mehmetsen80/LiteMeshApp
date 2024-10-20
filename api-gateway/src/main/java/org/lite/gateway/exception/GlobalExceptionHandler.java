package org.lite.gateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        // Log the exception for debugging purposes
        String message = ex.getMessage() != null ? ex.getMessage() : "";
        log.error("Handling RuntimeException: {}", message);

        // Return 500 Internal Server Error with a custom message
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                //.header(HttpHeaders.CONTENT_TYPE, MediaType.ALL_VALUE)
                .body(message);
    }

    private final ObjectMapper objectMapper = new ObjectMapper(); // Jackson object mapper to convert objects to JSON

//    @ExceptionHandler(RuntimeException.class)
//    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
//        // Log the exception for debugging purposes
//        log.error("Handling RuntimeException: {}", ex.getMessage());
//
//        // Build a custom JSON error response
//        Map<String, Object> errorResponse = new HashMap<>();
//        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
//        errorResponse.put("error", "Internal Server Error");
//        errorResponse.put("message", ex.getMessage());
//
//        try {
//            // Convert the error response to JSON
//            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
//
//            // Return 500 Internal Server Error with a JSON response
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
//                    .body(jsonResponse);
//
//        } catch (Exception e) {
//            // In case of any JSON conversion error, log it and return a fallback message
//            log.error("Error serializing error response to JSON", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
//                    .body("{\"error\": \"Failed to process error response\"}");
//        }
//    }

    // You can add more specific handlers for different exceptions
}
