package com.ecommerce.kafkahighconcurrencyproject.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class GatewayFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            UserDetails user = getUserDetailsFromRequest(request);
            if (user != null) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            logger.error("Authentication unsuccessfull", ex);
        }
        filterChain.doFilter(request, response);
        GatewayUser.removeUser();
    }

    private GatewayUser getUserDetailsFromRequest(HttpServletRequest request) {
        HttpHeaders httpHeaders = Collections.list(request.getHeaderNames()).stream()
                .collect(Collectors.toMap(Function.identity(), h -> Collections.list(request.getHeaders(h)), (oldValue, newValue) -> newValue, HttpHeaders::new));
        if (httpHeaders.getFirst("gateway-id") != null) {

            GatewayUser gatewayUser = new GatewayUser();
            gatewayUser.setId(httpHeaders.getFirst("gateway-id"));
            gatewayUser.setLastName(httpHeaders.getFirst("gateway-lastName"));
            gatewayUser.setFirstName(httpHeaders.getFirst("gateway-firstName"));
            gatewayUser.setEmail(httpHeaders.getFirst("gateway-email"));
            gatewayUser.setAccessToken(httpHeaders.getFirst("authorization"));
            gatewayUser.setPath(httpHeaders.getFirst("gateway-path"));
            String permissions = httpHeaders.getFirst("gateway-permission");
            if (permissions != null) {
                gatewayUser.setPermission(Arrays.asList(permissions.split(",")));
            }
            gatewayUser.setOrganizationId(httpHeaders.getFirst("gateway-organizationId"));
            GatewayUser.setUser(gatewayUser);
            return gatewayUser;
        }
        return null;
    }
}

