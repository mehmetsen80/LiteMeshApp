package org.lite.gateway.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
@RequiredArgsConstructor
public class DynamicRouteService {

    // set Initial internal whitelist paths
    private final Set<String> whitelistedPaths = new CopyOnWriteArraySet<>() {{
        add("/eureka/**");
        add("/mesh/**");
        add("/routes/**");
        add("/favicon.ico");
        add("/fallback/**");
        add("/actuator/**");
    }};

    private final PathMatcher pathMatcher = new AntPathMatcher();
    private final RedisTemplate<String, String> redisTemplate;
    private final ChannelTopic routesTopic;

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        // Load whitelisted paths from Redis
        Set<String> initialRoutes = redisTemplate.opsForSet().members("whitelistedPaths");

        if (initialRoutes == null || initialRoutes.isEmpty()) {
            // If Redis is empty, add all default paths to Redis
            whitelistedPaths.forEach(path -> redisTemplate.opsForSet().add("whitelistedPaths", path));
        } else {
            // Otherwise, add any missing default paths to both Redis and in-memory set
            whitelistedPaths.forEach(path -> {
                if (!initialRoutes.contains(path)) {
                    redisTemplate.opsForSet().add("whitelistedPaths", path);
                }
            });

            // Add Redis-loaded paths to in-memory whitelist
            whitelistedPaths.addAll(initialRoutes);
        }
    }


    // Get current whitelist paths
    public Set<String> getWhitelistedPaths() {
        return whitelistedPaths;
    }

    // Add a path to the whitelist
    public void addPath(String path) {
        // Add path to Redis and publish to notify other instances
        redisTemplate.opsForSet().add("whitelistedPaths", path);
        redisTemplate.convertAndSend(routesTopic.getTopic(), "ADD:" + path);
        whitelistedPaths.add(path);
    }

    // Remove a path from the whitelist
    public void removePath(String path) {
        // Remove path from Redis and publish to notify other instances
        redisTemplate.opsForSet().remove("whitelistedPaths", path);
        redisTemplate.convertAndSend(routesTopic.getTopic(), "REMOVE:" + path);
        whitelistedPaths.remove(path);
    }

    // Check if a path matches any whitelisted pattern
    public boolean isPathWhitelisted(String requestPath) {
        return whitelistedPaths.stream()
                .anyMatch(whitelistedPath -> pathMatcher.match(whitelistedPath, requestPath));
    }
}