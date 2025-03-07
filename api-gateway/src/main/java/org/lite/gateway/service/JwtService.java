package org.lite.gateway.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import org.lite.gateway.entity.User;
import org.lite.gateway.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

import java.security.Key;

import reactor.core.publisher.Mono;


@Service
@Slf4j
public class JwtService {


    @Value("${jwt.secret}")
    private String secretKey;
    
    @Value("${jwt.expiration}") // 24 hours in milliseconds
    private long jwtExpiration;

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final UserContextService userContextService;

    public JwtService(ObjectMapper objectMapper, UserRepository userRepository, UserContextService userContextService) {
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.userContextService = userContextService;
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public String generateToken(String username) {
        return generateToken(new HashMap<>(), username, false);
    }

    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("typ", "Refresh");
        claims.put("sub", username);
        claims.put("username", username);
        return generateToken(claims, username, true);
    }

    private String generateToken(Map<String, Object> extraClaims, String username, boolean isRefresh) {
        long expirationTime = isRefresh ? 7 * 24 * 60 * 60 * 1000 : 24 * 60 * 60 * 1000; // 7 days for refresh, 24h for access
        
        return Jwts.builder()
            .setClaims(extraClaims)
            .setSubject(username)
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            // For Keycloak tokens, use a different validation approach
            if (userContextService.isKeycloakToken(token)) {
                // Let the Keycloak decoder handle validation
                return true;  // The security filter will validate it
            }

            // For standard tokens, validate using our secret
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
        
        return claims.getSubject();
    }

    public String extractUsername(String token) {
        try {
            String[] chunks = token.split("\\.");
            String payload = new String(Base64.getDecoder().decode(chunks[1]));
            JsonNode jsonNode = objectMapper.readTree(payload);
            return jsonNode.get("preferred_username").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract username from token", e);
        }
    }

    public Mono<Claims> extractClaims(String token) {
        return Mono.fromCallable(() -> {
            try {
                String[] chunks = token.split("\\.");
                
                // First check the header
                String header = new String(Base64.getDecoder().decode(chunks[0]));
                JsonNode headerNode = objectMapper.readTree(header);
                
                // Get the payload
                String payload = new String(Base64.getDecoder().decode(chunks[1]));
                JsonNode jsonNode = objectMapper.readTree(payload);
                Claims claims = Jwts.claims();

                // Check if it's a Keycloak token (RS256 algorithm)
                if (headerNode.has("alg") && "RS256".equals(headerNode.get("alg").asText())) {
                    // Keycloak token processing
                    claims.put("preferred_username", jsonNode.get("preferred_username").asText());
                    claims.put("email", jsonNode.has("email") ? jsonNode.get("email").asText() : null);
                    if (jsonNode.has("realm_access")) {
                        claims.put("realm_access", objectMapper.convertValue(jsonNode.get("realm_access"), Map.class));
                    }
                    if (jsonNode.has("resource_access")) {
                        claims.put("resource_access", objectMapper.convertValue(jsonNode.get("resource_access"), Map.class));
                    }
                } 
                // Standard token (HS256 algorithm)
                else if (headerNode.has("alg") && "HS256".equals(headerNode.get("alg").asText())) {
                    String username = jsonNode.get("sub").asText();
                    claims.put("preferred_username", username);
                    claims.put("email", findEmailForUser(username));
                    claims.put("realm_access", Map.of("roles", getUserRoles(username)));
                }
                else {
                    throw new RuntimeException("Unknown token algorithm: " + headerNode.get("alg"));
                }
                
                claims.put("exp", jsonNode.get("exp").asLong());
                
                return claims;
            } catch (Exception e) {
                throw new RuntimeException("Failed to extract claims from token", e);
            }
        });
    }

    // Helper method to get user email
    private String findEmailForUser(String username) {
        // You can autowire UserRepository and use it here
        return userRepository.findByUsername(username)
            .map(User::getEmail)
            .block();  // Note: blocking call, consider restructuring if needed
    }

    // Helper method to get user roles
    private Set<String> getUserRoles(String username) {
        return userRepository.findByUsername(username)
            .map(User::getRoles)
            .defaultIfEmpty(Collections.emptySet())
            .block();  // Note: blocking call, consider restructuring if needed
    }
} 