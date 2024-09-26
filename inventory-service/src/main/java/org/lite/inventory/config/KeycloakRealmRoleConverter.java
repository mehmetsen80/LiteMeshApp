package org.lite.inventory.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        // Extract roles from 'realm_access' -> 'roles'
        List<String> realmRoles = (List<String>) jwt.getClaimAsMap("realm_access").get("roles");

        // Extract client-specific roles from 'resource_access'
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        List<String> clientRoles = null;

        if (resourceAccess != null) {
            Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get("lite-mesh-gateway-client");
            if (clientAccess != null) {
                clientRoles = (List<String>) clientAccess.get("roles");
            }
        }

        if (realmRoles == null && clientRoles == null) {
            return Collections.emptyList();
        }

        // Combine realm roles and client roles
        List<String> allRoles = Stream.concat(
                realmRoles != null ? realmRoles.stream() : Stream.empty(),
                clientRoles != null ? clientRoles.stream() : Stream.empty()
        ).toList();

        // Convert roles to Spring Security authorities (e.g., ROLE_GATEWAY_ADMIN)
        return allRoles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))  // Prefix roles with 'ROLE_'
                .collect(Collectors.toList());
    }
}
