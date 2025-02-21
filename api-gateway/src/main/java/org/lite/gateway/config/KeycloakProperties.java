package org.lite.gateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakProperties {
    private final ReactiveClientRegistrationRepository clientRegistrationRepository;

    @Value("${spring.security.oauth2.client.registration.lite-mesh-gateway-client.redirect-uri}")
    private String configuredRedirectUri;

    private static final String CLIENT_ID = "lite-mesh-gateway-client";

    public Mono<String> getTokenUrl() {
        return clientRegistrationRepository.findByRegistrationId(CLIENT_ID)
            .doOnNext(registration -> log.debug("Found client registration: {}", registration))
            .doOnError(error -> log.error("Error finding client registration: {}", error.getMessage()))
            .map(ClientRegistration::getProviderDetails)
            .doOnNext(details -> log.debug("Provider details: {}", details))
            .map(ClientRegistration.ProviderDetails::getTokenUri)
            .doOnNext(uri -> log.debug("Token URL: {}", uri))
            .doOnError(error -> log.error("Error getting token URL: {}", error.getMessage()))
            .switchIfEmpty(Mono.error(new IllegalStateException("Token URL not found")));
    }

    public Mono<String> getClientId() {
        return clientRegistrationRepository.findByRegistrationId(CLIENT_ID)
            .doOnNext(registration -> log.debug("Found client registration for client ID: {}", registration))
            .map(ClientRegistration::getClientId)
            .doOnNext(id -> log.debug("Client ID: {}", id))
            .switchIfEmpty(Mono.error(new IllegalStateException("Client ID not found")));
    }

    public Mono<String> getClientSecret() {
        return clientRegistrationRepository.findByRegistrationId(CLIENT_ID)
            .doOnNext(registration -> log.debug("Found client registration for secret"))
            .map(ClientRegistration::getClientSecret)
            .doOnNext(secret -> log.debug("Client Secret found (length: {})", 
                secret != null ? secret.length() : 0))
            .switchIfEmpty(Mono.error(new IllegalStateException("Client Secret not found")));
    }

    public Mono<String> getRealm() {
        return clientRegistrationRepository.findByRegistrationId(CLIENT_ID)
            .doOnNext(registration -> log.debug("Found client registration for realm"))
            .map(ClientRegistration::getProviderDetails)
            .map(ClientRegistration.ProviderDetails::getIssuerUri)
            .map(uri -> {
                String[] parts = uri.split("/realms/");
                if (parts.length < 2) {
                    throw new IllegalStateException("Invalid issuer URI format: " + uri);
                }
                return parts[1];  // Returns just the realm name
            })
            .doOnNext(uri -> log.debug("Realm: {}", uri))
            .switchIfEmpty(Mono.error(new IllegalStateException("Realm not found")));
    }

    public Mono<String> getRedirectUri() {
        return clientRegistrationRepository.findByRegistrationId(CLIENT_ID)
            .doOnNext(registration -> log.debug("Found client registration for redirect URI"))
            .map(registration -> {
                String uri = registration.getRedirectUri();
                if (uri == null || uri.trim().isEmpty()) {
                    log.debug("Redirect URI not found in registration, using configured value: {}", configuredRedirectUri);
                    return configuredRedirectUri;
                }
                return uri;
            })
            .doOnNext(uri -> log.debug("Redirect URI: {}", uri))
            .switchIfEmpty(Mono.just(configuredRedirectUri));
    }
}