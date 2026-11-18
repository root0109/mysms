package com.mysms.people;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The People & Profiles microservice: owns schema "people" and serves the people contract. In the monolith it runs
 * inside {@code app}, which combines its {@code application.properties} with every other module's.
 */
@SpringBootApplication
public class PeopleApplication {

    protected PeopleApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(PeopleApplication.class, args);
    }
}
