package org.lite.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize.requestMatchers("/xxxxx/**")//no matter what you put here, if we have the gateway token from oauth2ResourceServer, we'll be authenticated
                        .permitAll()  // Public endpoints (if any)
                        .anyRequest().authenticated()  // Secure all other endpoints
                )
                .oauth2ResourceServer(oauth2-> {
                   oauth2.jwt(Customizer.withDefaults());
                });  // Custom JWT converter (if needed)

        return http.build();
    }
}