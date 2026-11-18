package com.mysms.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Platform & Tenancy microservice: owns schema "platform" and serves the platform contract. In the monolith it runs
 * inside {@code app}, which combines its {@code application.properties} with every other module's.
 */
@SpringBootApplication
public class PlatformApplication {

    protected PlatformApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(PlatformApplication.class, args);
    }
}
