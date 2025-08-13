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
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String REQUEST_START_TIME = "request_start_time";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        
      
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        
        final String finalCorrelationId = correlationId;
        
        
        response.getHeaders().add(CORRELATION_ID_HEADER, finalCorrelationId);
        
       
        ServerHttpRequest mutatedRequest = request.mutate()
            .header(CORRELATION_ID_HEADER, finalCorrelationId)
            .build();
        
        
        MDC.put("correlationId", finalCorrelationId);
        MDC.put("requestMethod", request.getMethod().name());
        MDC.put("requestPath", request.getPath().toString());
        
    
        exchange.getAttributes().put(REQUEST_START_TIME, Instant.now());
        
        logger.info("Gateway Request - Method: {}, Path: {}, Headers: {}", 
            request.getMethod(), 
            request.getPath(),
            request.getHeaders().toSingleValueMap().keySet());
        
        return chain.filter(exchange.mutate().request(mutatedRequest).build())
            .doOnSuccess(aVoid -> logResponse(exchange, finalCorrelationId))
            .doOnError(throwable -> logError(exchange, finalCorrelationId, throwable))
            .doFinally(signalType -> MDC.clear());
    }
    
    private void logResponse(ServerWebExchange exchange, String correlationId) {
        ServerHttpResponse response = exchange.getResponse();
        Instant startTime = exchange.getAttribute(REQUEST_START_TIME);
        long duration = startTime != null ? 
            Instant.now().toEpochMilli() - startTime.toEpochMilli() : 0;
        
        logger.info("Gateway Response - Status: {}, Duration: {}ms, Correlation-ID: {}", 
            response.getStatusCode(), 
            duration, 
            correlationId);
    }
    
    private void logError(ServerWebExchange exchange, String correlationId, Throwable throwable) {
        ServerHttpRequest request = exchange.getRequest();
        logger.error("Gateway Error - Method: {}, Path: {}, Correlation-ID: {}, Error: {}", 
            request.getMethod(), 
            request.getPath(),
            correlationId,
            throwable.getMessage(), 
            throwable);
    }

    @Override
    public int getOrder() {
        return -1;
    }
} 