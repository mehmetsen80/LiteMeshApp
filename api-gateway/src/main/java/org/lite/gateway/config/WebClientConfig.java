package org.lite.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

//WebClient is not being used right now, keep this for future
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(ReactiveOAuth2AuthorizedClientManager authorizedClientServiceReactiveOAuth2AuthorizedClientManager) {
        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2FilterFunction =
                new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientServiceReactiveOAuth2AuthorizedClientManager);
        oauth2FilterFunction.setDefaultClientRegistrationId("lite-mesh-gateway-client");

        return WebClient.builder()
                .filter(oauth2FilterFunction)
                .build();
    }
}
