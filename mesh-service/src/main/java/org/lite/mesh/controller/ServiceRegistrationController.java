package org.lite.mesh.controller;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.lite.mesh.enums.PreferredDiscovery;
import org.lite.mesh.request.ServiceRegistrationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mesh")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ServiceRegistrationController {

    private final ApplicationEventPublisher eventPublisher;

    @PostMapping("/register")
    public ResponseEntity<String> registerService(@RequestBody ServiceRegistrationRequest request) {
        PreferredDiscovery preferredDiscovery;

        if(StringUtils.isBlank(request.getApplication().getUsername())){
            return ResponseEntity.badRequest().body("Application username must be provided.");
        }

        // Handle null preferredDiscovery separately
        if (request.getDiscovery().getPreferred() == null) {
            return ResponseEntity.ok("No preferred discovery service defined for application " + request.getApplication().getUsername());
        }

        try {
            // Convert Discovery Service String to Enum safely
            preferredDiscovery = PreferredDiscovery.valueOf(request.getDiscovery().getPreferred().toUpperCase());

            // Use switch statement for clearer logic handling
            switch (preferredDiscovery) {
                case NONE -> {
                    return ResponseEntity.ok(request.getApplication().getUsername() + " preferred not to register any discovery service.");
                }
                case EUREKA -> {
                    //TODO: complete this later, might be removed as well
                    //eventPublisher.publishEvent(new RegisterEurekaEvent(request)); // Publish the registration event
                    return ResponseEntity.ok(request.getApplication().getUsername() + " registered successfully with " + preferredDiscovery);
                }
                default -> {
                    return ResponseEntity.ok(request.getApplication().getUsername() + " preferred discovery setting " + preferredDiscovery + " does not match with Lite Gateway.");
                }
            }
        } catch (IllegalArgumentException e) {
            // If the provided value does not match any PreferredDiscovery enum constants
            return ResponseEntity.badRequest().body("Invalid preferred discovery setting for " + request.getApplication().getUsername() + ": " + request.getDiscovery().getPreferred());
        } catch (Exception e) {
            // General catch block for other exceptions
            return ResponseEntity.status(500).body("Service registration failed for " + request.getApplication().getUsername() + ": " + e.getMessage());
        }
    }

    @PostMapping("/deregister")
    public ResponseEntity<String> deregisterService(@RequestBody ServiceRegistrationRequest request) {
        PreferredDiscovery preferredDiscovery;

        if(StringUtils.isBlank(request.getApplication().getUsername())){
            return ResponseEntity.badRequest().body("Application username must be provided.");
        }

        // Handle null preferredDiscovery separately
        if (request.getDiscovery().getPreferred() == null) {
            return ResponseEntity.ok("No preferred discovery service defined for application " + request.getApplication().getUsername());
        }

        try {
            // Convert Discovery Service String to Enum safely
            preferredDiscovery = PreferredDiscovery.valueOf(request.getDiscovery().getPreferred().toUpperCase());

            // Use switch statement for clearer logic handling
            switch (preferredDiscovery) {
                case NONE -> {
                    return ResponseEntity.ok(request.getApplication().getUsername() + " preferred not to register any discovery service.");
                }
                case EUREKA -> {
                    //TODO: complete this later, might be removed as well
                    //eventPublisher.publishEvent(new DeregisterEurekaEvent(request)); // Publish the deregistration event
                    return ResponseEntity.ok(request.getApplication().getUsername() + " deregistered successfully from " + preferredDiscovery);
                }
                default -> {
                    return ResponseEntity.ok(request.getApplication().getUsername() + " preferred discovery setting " + preferredDiscovery + " does not match with Lite Gateway.");
                }
            }
        } catch (IllegalArgumentException e) {
            // If the provided value does not match any PreferredDiscovery enum constants
            return ResponseEntity.badRequest().body("Invalid preferred discovery setting for " + request.getApplication().getUsername() + ": " + request.getDiscovery().getPreferred());
        } catch (Exception e) {
            // General catch block for other exceptions
            return ResponseEntity.status(500).body("Service deregistration failed for " + request.getApplication().getUsername() + ": " + e.getMessage());
        }
    }
}
