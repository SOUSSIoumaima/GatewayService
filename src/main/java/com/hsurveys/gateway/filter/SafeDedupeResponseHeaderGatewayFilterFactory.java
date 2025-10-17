package com.hsurveys.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.DedupeResponseHeaderGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class SafeDedupeResponseHeaderGatewayFilterFactory extends AbstractGatewayFilterFactory<SafeDedupeResponseHeaderGatewayFilterFactory.Config> {

    private static final Logger logger = LoggerFactory.getLogger(SafeDedupeResponseHeaderGatewayFilterFactory.class);
    private final DedupeResponseHeaderGatewayFilterFactory dedupeFilterFactory;

    public SafeDedupeResponseHeaderGatewayFilterFactory(DedupeResponseHeaderGatewayFilterFactory dedupeFilterFactory) {
        super(Config.class);
        this.dedupeFilterFactory = dedupeFilterFactory;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {
                    try {
                        // Only process if response is not committed and status is successful
                        if (!exchange.getResponse().isCommitted() && 
                            exchange.getResponse().getStatusCode() != null &&
                            exchange.getResponse().getStatusCode().is2xxSuccessful()) {
                            
                            // Use the built-in dedupe filter logic
                            dedupeHeaders(exchange.getResponse().getHeaders(), config.getNames(), config.getStrategy());
                            
                            logger.debug("Headers deduplicated safely for: {}", 
                                exchange.getRequest().getURI().getPath());
                        } else {
                            logger.debug("Skipping header deduplication - response committed: {}, status: {}", 
                                exchange.getResponse().isCommitted(),
                                exchange.getResponse().getStatusCode());
                        }
                    } catch (Exception e) {
                        logger.debug("Skipping header deduplication due to: {}", e.getMessage());
                    }
                }));
        };
    }

    private void dedupeHeaders(HttpHeaders headers, String headerNames, String strategy) {
        String[] names = headerNames.split(" ");
        for (String name : names) {
            try {
                List<String> values = headers.get(name);
                if (values != null && values.size() > 1) {
                    String valueToKeep = "RETAIN_FIRST".equals(strategy) ? values.get(0) : values.get(values.size() - 1);
                    headers.remove(name);
                    headers.add(name, valueToKeep);
                }
            } catch (Exception e) {
                logger.debug("Could not dedupe header {}: {}", name, e.getMessage());
            }
        }
    }

    public static class Config {
        private String names;
        private String strategy = "RETAIN_FIRST";

        public String getNames() {
            return names;
        }

        public void setNames(String names) {
            this.names = names;
        }

        public String getStrategy() {
            return strategy;
        }

        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }
    }
} 