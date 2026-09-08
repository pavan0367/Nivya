package com.nivya;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Nivya - Consent-Based Family Safety & Device-Status Monitoring System
 * Main Spring Boot Application Entry Point.
 */
@SpringBootApplication
@EnableScheduling
public class NivyaApplication {

    private static final Logger log = LoggerFactory.getLogger(NivyaApplication.class);

    public static void main(String[] args) {
        log.info("Starting Nivya Backend Application on Java 21...");
        SpringApplication.run(NivyaApplication.class, args);
        log.info("Nivya Backend Application successfully initialized.");
    }
}
