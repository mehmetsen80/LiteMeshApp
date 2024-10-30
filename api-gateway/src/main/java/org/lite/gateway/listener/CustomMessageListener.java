package org.lite.gateway.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class CustomMessageListener {

    private final Set<String> whitelistedPaths;

    // Method to handle incoming messages and update whitelisted paths
    public void handleMessage(String pathMessage) {
        if (pathMessage.startsWith("ADD:")) {
            whitelistedPaths.add(pathMessage.substring(4));
        } else if (pathMessage.startsWith("REMOVE:")) {
            whitelistedPaths.remove(pathMessage.substring(7));
        }
    }
}
