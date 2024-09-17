package org.lite.gateway.service;

import org.springframework.stereotype.Service;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
public class DynamicRouteService {

    //set initial white list paths
    private final Set<String> whitelistedPaths = new CopyOnWriteArraySet<>(){{
        add("/eureka/**");
        add("/mesh/**");
        add("routes/**");
    }};

    // Get current whitelist paths
    public Set<String> getWhitelistedPaths() {
        return whitelistedPaths;
    }

    // Dynamically add a path to the whitelist
    public void addPath(String path) {
        whitelistedPaths.add(path);
    }

    // Dynamically remove a path from the whitelist
    public void removePath(String path) {
        whitelistedPaths.remove(path);
    }
}