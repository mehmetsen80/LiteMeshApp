package org.lite.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Component
class ClientAuthoritiesConverter implements Converter<Jwt, Flux<GrantedAuthority>> {

    // we append "ROLE_" prefix to the client roles so that we can use it as method annotation later if needed.  i.e. @Authorized("ROLE_gateway_admin")
    // right now we don't use it anywhere
    @Override
    @SuppressWarnings("unchecked")
    public Flux<GrantedAuthority> convert(Jwt source) {
        final var resourceAccess = (Map<String, Object>) source.getClaims().getOrDefault("resource_access", Map.of());
        final var clientAccess = (Map<String, Object>) resourceAccess.getOrDefault("lite-mesh-gateway-client", Map.of());
        final var roles = (List<String>) clientAccess.getOrDefault("roles", List.of());
        return Flux.fromStream(roles.stream())
                .map("ROLE_%s"::formatted)
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast);
    }


    //this bean calls above converter method when an API is called
    @Bean
    ReactiveJwtAuthenticationConverter authenticationConverter(Converter<Jwt, Flux<GrantedAuthority>> authoritiesConverter) {
        final var authenticationConverter = new ReactiveJwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        authenticationConverter.setPrincipalClaimName(StandardClaimNames.PREFERRED_USERNAME);
        return authenticationConverter;
    }

}