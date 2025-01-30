package org.lite.gateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class KeycloakProperties {
    private final ReactiveClientRegistrationRepository clientRegistrationRepository;

    public Mono<String> getTokenUrl() {
        return clientRegistrationRepository.findByRegistrationId("lite-mesh-gateway-client")
            .map(ClientRegistration::getProviderDetails)
            .map(ClientRegistration.ProviderDetails::getTokenUri);
    }

    public Mono<String> getClientId() {
        return clientRegistrationRepository.findByRegistrationId("lite-mesh-gateway-client")
            .map(ClientRegistration::getClientId);
    }

    public Mono<String> getClientSecret() {
        return clientRegistrationRepository.findByRegistrationId("lite-mesh-gateway-client")
            .map(ClientRegistration::getClientSecret);
    }
} 