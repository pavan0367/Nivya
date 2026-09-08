package com.nivya;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic Spring Boot Context Load Test.
 * Validates that Spring beans, security filter chains, and configuration wiring initialize successfully.
 */
@SpringBootTest
@ActiveProfiles("test")
class NivyaApplicationTests {

    @Test
    void contextLoads() {
        // Verification that Spring Application Context initializes without configuration errors
    }
}
