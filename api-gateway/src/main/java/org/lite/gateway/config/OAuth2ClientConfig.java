//package org.lite.gateway.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
//import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
//import org.springframework.security.oauth2.client.registration.ClientRegistration;
//import org.springframework.security.oauth2.core.AuthorizationGrantType;
//
//@Configuration
//public class OAuth2ClientConfig {
//
//    @Bean
//    public ClientRegistrationRepository clientRegistrationRepository() {
//        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("lite-mesh-gateway-client")
//                .clientId("your-client-id")
//                .clientSecret("your-client-secret")
//                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
//                .tokenUri("http://localhost:8281/realms/LiteMesh/protocol/openid-connect/token")
//                //.scope("gateway_admin")
//                .build();
//
//        return new InMemoryClientRegistrationRepository(clientRegistration);
//    }
//}