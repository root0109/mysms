package com.mysms.frontoffice;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * The Front Office microservice: owns schema "frontoffice" and serves the frontoffice contract.
 * Its configuration is frontoffice-service.yml, which imports the module's own mysms-frontoffice.yml;
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class FrontofficeApplication {

    public static final String CONFIG_NAME = "frontoffice-service";

    protected FrontofficeApplication() {
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(FrontofficeApplication.class).properties("spring.config.name=" + CONFIG_NAME).run(args);
    }
}
