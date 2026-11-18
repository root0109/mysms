package com.mysms.comms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Communication & Community microservice: owns schema "comms". In the monolith the same module runs inside {@code
 * app}, whose configuration merges every module's {@code application.properties}.
 */
@SpringBootApplication
public class CommsApplication {

    protected CommsApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(CommsApplication.class, args);
    }
}
