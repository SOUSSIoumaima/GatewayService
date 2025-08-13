package com.hsurveys.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.cloud.gateway.routes[0].id=test-route",
    "spring.cloud.gateway.routes[0].uri=http://localhost:8081",
    "spring.cloud.gateway.routes[0].predicates[0]=Path=/test/**",
    "jwt.secret=test-secret-key-for-testing-purposes-only",
    "redis.host=localhost",
    "redis.port=6379"
})
class GatewayApplicationTests {

    @Test
    void contextLoads() {
        // Test that the Spring context loads successfully
    }

    @Test
    void gatewayStartsSuccessfully() {
        // Test that the gateway application starts without errors
    }
}
