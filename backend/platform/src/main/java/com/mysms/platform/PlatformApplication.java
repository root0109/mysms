package com.mysms.platform;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * The Platform & Tenancy microservice: owns schema "platform" and serves the platform contract.
 * Its configuration is platform-service.yml, which imports the module's own mysms-platform.yml;
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class PlatformApplication {

    public static final String CONFIG_NAME = "platform-service";

    protected PlatformApplication() {
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(PlatformApplication.class).properties("spring.config.name=" + CONFIG_NAME).run(args);
    }
}
