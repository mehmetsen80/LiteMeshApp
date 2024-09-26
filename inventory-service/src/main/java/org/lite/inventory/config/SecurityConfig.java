package org.lite.inventory.config;

import lombok.extern.slf4j.Slf4j;
import org.lite.inventory.filter.JwtRoleValidationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Collection;


@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig  {


    private final JwtRoleValidationFilter jwtRoleValidationFilter;

    public SecurityConfig(JwtRoleValidationFilter jwtRoleValidationFilter) {
        this.jwtRoleValidationFilter = jwtRoleValidationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(authorize -> authorize
                                .requestMatchers("/inventory/**")//no matter what you put here, if we have the gateway token from oauth2ResourceServer, we'll be authenticated
                                .permitAll()  // Public endpoints (if any)
                                .anyRequest()
                                .authenticated()
                                //.hasRole("gateway_admin")
                )
                .oauth2ResourceServer(oauth2-> {
                    oauth2.jwt(Customizer.withDefaults());
                });
                //.addFilterBefore(jwtRoleValidationFilter, UsernamePasswordAuthenticationFilter.class);
//                .oauth2ResourceServer(oauth2 -> oauth2
//                        .jwt(jwt -> jwt
//                                .jwtAuthenticationConverter(jwtAuthenticationConverter())  // Convert JWT to Spring Security authorities
//                        )
//                ); // Custom JWT converter (if needed)

        return http.build();
    }

    /**
     * Configures how roles are extracted from the JWT token.
     * This custom converter extracts roles from "realm_access" or "resource_access" claims.
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;
    }
}