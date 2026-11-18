package com.mysms.org;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Organisation & Calendar microservice: owns schema "org" and serves the org contract. In the monolith it runs
 * inside {@code app}, which combines its {@code application.properties} with every other module's.
 */
@SpringBootApplication
public class OrgApplication {

    protected OrgApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(OrgApplication.class, args);
    }
}
