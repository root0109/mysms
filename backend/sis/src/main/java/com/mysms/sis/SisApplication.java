package com.mysms.sis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Student Information microservice: owns schema "sis" and serves the sis contract. In the monolith it runs inside
 * {@code app}, which combines its {@code application.properties} with every other module's.
 */
@SpringBootApplication
public class SisApplication {

    protected SisApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(SisApplication.class, args);
    }
}
