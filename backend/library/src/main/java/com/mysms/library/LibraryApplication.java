package com.mysms.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Library microservice: owns schema "library". In the monolith the same module runs inside {@code app}, whose
 * configuration merges every module's {@code application.properties}.
 */
@SpringBootApplication
public class LibraryApplication {

    protected LibraryApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(LibraryApplication.class, args);
    }
}
