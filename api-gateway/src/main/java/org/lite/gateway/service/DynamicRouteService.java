package org.lite.gateway.service;

import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
public class DynamicRouteService {

    //set initial internal white list paths
    private final Set<String> whitelistedPaths = new CopyOnWriteArraySet<>(){{
        add("/eureka/**");
        add("/mesh/**");
        add("/routes/**");
        add("/favicon.ico");
        add("/circuitbreaker/fallback/**");
        add("/retry/fallback/**");
    }};

    private final PathMatcher pathMatcher = new AntPathMatcher();

    // Get current whitelist paths
    public Set<String> getWhitelistedPaths() {
        return whitelistedPaths;
    }

    // Add a path to the whitelist
    public void addPath(String path) {
        whitelistedPaths.add(path);
    }

    // Remove a path from the whitelist
    public void removePath(String path) {
        whitelistedPaths.remove(path);
    }

    // Check if a path matches any whitelisted pattern
    public boolean isPathWhitelisted(String requestPath) {
        return whitelistedPaths.stream()
                .anyMatch(whitelistedPath -> pathMatcher.match(whitelistedPath, requestPath));
    }
}