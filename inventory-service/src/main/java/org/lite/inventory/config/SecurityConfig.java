package org.lite.inventory.config;

import lombok.extern.slf4j.Slf4j;
import org.lite.inventory.filter.JwtRoleValidationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
@Slf4j
public class SecurityConfig  {

    //It will be called even though you don't use it here, so don't remove it
    private final JwtRoleValidationFilter jwtRoleValidationFilter;

    public SecurityConfig(JwtRoleValidationFilter jwtRoleValidationFilter) {
        this.jwtRoleValidationFilter = jwtRoleValidationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .x509(x509 -> x509
                        .x509PrincipalExtractor((principal -> { //Enable mTLS (client certificate authentication)
                                    // Extract the CN from the certificate (adjust this logic as needed)
                                    String dn = principal.getSubjectX500Principal().getName();
                                    log.info("dn: {}", dn);
                                    String cn = dn.split(",")[0].replace("CN=", "");
                                    return cn;  // Return the Common Name (CN) as the principal
                                })
                        ))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                                .requestMatchers("/inventory-service/**")//no matter what you put here, if we have the gateway token from oauth2ResourceServer, we'll be authenticated
                                .permitAll()  // Public endpoints (if any)
                                .anyRequest()
                                .authenticated()
                )
                .oauth2ResourceServer(oauth2-> {  // Enable OAuth2-based authentication (via JWT tokens)
                    oauth2.jwt(Customizer.withDefaults());
                });

        return http.build();
    }
}