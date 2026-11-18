package com.mysms.lms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Learning Management microservice: owns schema "lms". In the monolith the same module runs inside {@code app},
 * whose configuration merges every module's {@code application.properties}.
 */
@SpringBootApplication
public class LmsApplication {

    protected LmsApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(LmsApplication.class, args);
    }
}
