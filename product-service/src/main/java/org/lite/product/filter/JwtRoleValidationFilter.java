package org.lite.product.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class JwtRoleValidationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Retrieve the JWT token from the security context
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            // Log the JWT token for debugging purposes
            log.info("JWT Token: {}", jwt.getTokenValue());
            // Log roles for debugging
            List<String> realmRoles = (List<String>) jwt.getClaimAsMap("realm_access").get("roles");
            log.info("Realm Roles: {}", realmRoles);

            Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
            log.info("Client Roles: {}", resourceAccess);

            if (hasRequiredRole(jwt)) {
                filterChain.doFilter(request, response); // Continue the request processing
            } else {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN); // Return 403 Forbidden if role check fails
            }
        } else {
            //It's up to you which line do you want to enable, the latter one is more secure, as you don't force the call to use a jwt token
            //filterChain.doFilter(request, response); // No JWT token, continue request processing, i.e. calling the GET from browser
            response.setStatus(HttpServletResponse.SC_FORBIDDEN); // Return 403 Forbidden if role check fails, you are forcing to use the token
        }
    }

    //We force both realm and resource roles to exist in the token
    private boolean hasRequiredRole(Jwt jwt) {
        // Check realm roles
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        boolean hasRealmRole = false;
        if (realmAccess != null) {
            List<String> realmRoles = (List<String>) realmAccess.get("roles");
            if (realmRoles != null && realmRoles.contains("gateway_admin_realm")) {
                hasRealmRole = true;
            }
        }

        // Check client roles for lite-mesh-gateway-client
        boolean hasClientRole = false;
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null && resourceAccess.containsKey("lite-mesh-gateway-client")) {
            Map<String, List<String>> clientRoles = (Map<String, List<String>>) resourceAccess.get("lite-mesh-gateway-client");
            if (clientRoles.get("roles").contains("gateway_admin")) {
                hasClientRole = true;
            }
        }

        // Both roles must be present
        return hasRealmRole && hasClientRole;
    }
}

