package org.lite.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

@Configuration
public class OAuth2ClientConfig {

    @Value("${spring.security.oauth2.client.provider.keycloak.token-uri}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.registration.lite-mesh-gateway-client.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.lite-mesh-gateway-client.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.lite-mesh-gateway-client.scope}")
    private String scope;

    @Bean
    public ReactiveClientRegistrationRepository customClientRegistrationRepository() {
        ClientRegistration registration = ClientRegistration
                .withRegistrationId("lite-mesh-gateway-client")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope(scope.split(","))
                .tokenUri(tokenUri)
                .build();

        return new InMemoryReactiveClientRegistrationRepository(registration);
    }

    @Bean
    public ReactiveOAuth2AuthorizedClientService customAuthorizedClientService(
            ReactiveClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrationRepository);
    }
} 