package com.hsurveys.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String REQUEST_START_TIME = "request_start_time";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        
      
                String requestId = request.getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        
        final String finalRequestId = requestId;
        
        
        response.getHeaders().add(REQUEST_ID_HEADER, finalRequestId);
        
        
        ServerHttpRequest mutatedRequest = request.mutate()
            .header(REQUEST_ID_HEADER, finalRequestId)
            .build();
        
        
        MDC.put("requestId", finalRequestId);
        MDC.put("requestMethod", request.getMethod().name());
        MDC.put("requestPath", request.getPath().toString());
        
    
        exchange.getAttributes().put(REQUEST_START_TIME, Instant.now());
        
        logger.info("Gateway Request - Method: {}, Path: {}, Headers: {}", 
            request.getMethod(), 
            request.getPath(),
            request.getHeaders().toSingleValueMap().keySet());
        
        return chain.filter(exchange.mutate().request(mutatedRequest).build())
            .doOnSuccess(aVoid -> logResponse(exchange, finalRequestId))
            .doOnError(throwable -> logError(exchange, finalRequestId, throwable))
            .doFinally(signalType -> MDC.clear());
    }
    
    private void logResponse(ServerWebExchange exchange, String requestId) {
        ServerHttpResponse response = exchange.getResponse();
        Instant startTime = exchange.getAttribute(REQUEST_START_TIME);
        long duration = startTime != null ? 
            Instant.now().toEpochMilli() - startTime.toEpochMilli() : 0;
        
        logger.info("Gateway Response - Status: {}, Duration: {}ms, Request-ID: {}", 
            response.getStatusCode(), 
            duration, 
            requestId);
    }
    
    private void logError(ServerWebExchange exchange, String requestId, Throwable throwable) {
        ServerHttpRequest request = exchange.getRequest();
        logger.error("Gateway Error - Method: {}, Path: {}, Request-ID: {}, Error: {}", 
            request.getMethod(), 
            request.getPath(),
            requestId,
            throwable.getMessage(), 
            throwable);
    }

    @Override
    public int getOrder() {
        return -1;
    }
} 