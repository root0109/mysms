package com.mysms.careers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Careers microservice: owns schema "careers". In the monolith the same module runs inside {@code app}, whose
 * configuration merges every module's {@code application.properties}.
 */
@SpringBootApplication
public class CareersApplication {

    protected CareersApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(CareersApplication.class, args);
    }
}
