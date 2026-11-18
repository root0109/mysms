package com.mysms.hr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Workforce / HR microservice: owns schema "hr". In the monolith the same module runs inside {@code app}, whose
 * configuration merges every module's {@code application.properties}.
 */
@SpringBootApplication
public class HrApplication {

    protected HrApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(HrApplication.class, args);
    }
}
