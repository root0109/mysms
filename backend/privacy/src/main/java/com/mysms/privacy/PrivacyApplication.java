package com.mysms.privacy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Privacy and Consent Centre microservice: owns schema "privacy". In the monolith the same module runs inside
 * {@code app}, whose configuration merges every module's {@code application.properties}.
 */
@SpringBootApplication
public class PrivacyApplication {

    protected PrivacyApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(PrivacyApplication.class, args);
    }
}
