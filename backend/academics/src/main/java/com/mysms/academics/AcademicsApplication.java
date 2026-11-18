package com.mysms.academics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Academic Structure microservice: owns schema "academics" and serves the academics contract. In the monolith it
 * runs inside {@code app}, which combines its {@code application.properties} with every other module's.
 */
@SpringBootApplication
public class AcademicsApplication {

    protected AcademicsApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(AcademicsApplication.class, args);
    }
}
