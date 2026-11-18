package com.mysms.frontoffice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Front Office microservice: owns schema "frontoffice". In the monolith the same module runs inside {@code app},
 * whose configuration merges every module's {@code application.properties}.
 */
@SpringBootApplication
public class FrontofficeApplication {

    protected FrontofficeApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(FrontofficeApplication.class, args);
    }
}
