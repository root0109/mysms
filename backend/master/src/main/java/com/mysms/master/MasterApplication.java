package com.mysms.master;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Master Data microservice: owns schema "master" and serves the master contract. In the monolith it runs inside
 * {@code app}, which combines its {@code application.properties} with every other module's.
 */
@SpringBootApplication
public class MasterApplication {

    protected MasterApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(MasterApplication.class, args);
    }
}
