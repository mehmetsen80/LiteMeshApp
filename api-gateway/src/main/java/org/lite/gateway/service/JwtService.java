package org.lite.gateway.service;

import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;

public interface JwtService {
    /**
     * Generates a JWT token for the given username
     * @param username the username to generate token for
     * @return the generated JWT token
     */
    String generateToken(String username);

    /**
     * Generates a refresh token for the given username
     * @param username the username to generate refresh token for
     * @return the generated refresh token
     */
    String generateRefreshToken(String username);

    /**
     * Validates the given JWT token
     * @param token the token to validate
     * @return true if token is valid, false otherwise
     */
    boolean validateToken(String token);

    /**
     * Extracts username from a JWT token
     * @param token the token to extract username from
     * @return the username from the token
     */
    String getUsernameFromToken(String token);

    /**
     * Extracts username from a token (specifically for Keycloak tokens)
     * @param token the token to extract username from
     * @return the extracted username
     */
    String extractUsername(String token);

    /**
     * Extracts all claims from a token
     * @param token the token to extract claims from
     * @return Mono containing the claims
     */
    Mono<Claims> extractClaims(String token);
} 