package com.hsurveys.gateway.filter;

import com.hsurveys.gateway.utils.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<JwtAuthenticationGatewayFilterFactory.Config> {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationGatewayFilterFactory.class);
    private final JwtUtil jwtUtil;

    public JwtAuthenticationGatewayFilterFactory(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            
            logger.debug("Processing request: {}", path);

          
            if (isPublicEndpoint(path)) {
                logger.debug("Skipping authentication for public endpoint: {}", path);
                return chain.filter(exchange);
            }

            
            String token = extractTokenFromRequest(request);
            
            if (token == null) {
                logger.warn("No token found in request: {}", path);
                return onError(exchange, "No authentication token found", HttpStatus.UNAUTHORIZED);
            }

            try {
            
                if (!jwtUtil.validateToken(token)) {
                    logger.warn("Invalid token for request: {}", path);
                    return onError(exchange, "Invalid authentication token", HttpStatus.UNAUTHORIZED);
                }

               
                String username = jwtUtil.extractUsername(token);
                UUID userId = jwtUtil.extractUserId(token);
                UUID organizationId = jwtUtil.extractOrganizationId(token);
                UUID departmentId = jwtUtil.extractDepartmentId(token);
                UUID teamId = jwtUtil.extractTeamId(token);
                List<String> authorities = jwtUtil.extractAuthorities(token);
                List<String> roles = jwtUtil.extractRoles(token);

                logger.debug("Token validated for user: {} in organization: {}", username, organizationId);

                
                ServerHttpRequest mutatedRequest = request.mutate()
                    .headers(httpHeaders -> {
                        if (userId != null) {
                            httpHeaders.add("X-User-Id", userId.toString());
                        }
                        if (username != null) {
                            httpHeaders.add("X-Username", username);
                            httpHeaders.add("X-User-Name", username);
                        }
                        if (organizationId != null) {
                            httpHeaders.add("X-Organization-Id", organizationId.toString());
                        }
                        if (departmentId != null) {
                            httpHeaders.add("X-Department-Id", departmentId.toString());
                        }
                        if (teamId != null) {
                            httpHeaders.add("X-Team-Id", teamId.toString());
                        }
                        if (authorities != null && !authorities.isEmpty()) {
                            String authoritiesStr = String.join(",", authorities);
                            httpHeaders.add("X-Authorities", authoritiesStr);
                            httpHeaders.add("X-User-Authorities", authoritiesStr);
                        }
                        if (roles != null && !roles.isEmpty()) {
                            String rolesStr = String.join(",", roles);
                            httpHeaders.add("X-Roles", rolesStr);
                            httpHeaders.add("X-User-Roles", rolesStr);
                        }
                        httpHeaders.add("X-Authenticated", "true");
                    })
                    .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (Exception e) {
                logger.error("Error processing token for request: {}", path, e);
                return onError(exchange, "Token processing error", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/register") ||
               path.startsWith("/api/auth/refresh") ||
               path.startsWith("/api/organizations/register") ||
               path.startsWith("/actuator/");
    }

    private String extractTokenFromRequest(ServerHttpRequest request) {
      
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

      
        List<String> cookies = request.getHeaders().get(HttpHeaders.COOKIE);
        if (cookies != null) {
            for (String cookie : cookies) {
                if (cookie.contains("access_token=")) {
                    String[] parts = cookie.split("access_token=");
                    if (parts.length > 1) {
                        String token = parts[1];
                        int endIndex = token.indexOf(';');
                        return endIndex != -1 ? token.substring(0, endIndex) : token;
                    }
                }
            }
        }

        return null;
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        
        String body = String.format("{\"error\": \"%s\", \"message\": \"%s\"}", status.getReasonPhrase(), message);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    public static class Config {

    }
} 