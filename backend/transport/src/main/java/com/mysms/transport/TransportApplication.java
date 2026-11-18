package com.mysms.transport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Transport microservice: owns schema "transport". In the monolith the same module runs inside {@code app}, whose
 * configuration merges every module's {@code application.properties}.
 */
@SpringBootApplication
public class TransportApplication {

    protected TransportApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(TransportApplication.class, args);
    }
}
