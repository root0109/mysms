package com.mysms.library;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * The Library microservice: owns schema "library" and serves the library contract.
 * Its configuration is library-service.yml, which imports the module's own mysms-library.yml;
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class LibraryApplication {

    public static final String CONFIG_NAME = "library-service";

    protected LibraryApplication() {
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(LibraryApplication.class).properties("spring.config.name=" + CONFIG_NAME).run(args);
    }
}
