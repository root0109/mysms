package com.mysms.iam;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * The Identity & Access microservice: owns schema "iam" and serves the iam contract.
 * Its configuration is iam-service.yml, which imports the module's own mysms-iam.yml;
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class IamApplication {

    public static final String CONFIG_NAME = "iam-service";

    protected IamApplication() {
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(IamApplication.class).properties("spring.config.name=" + CONFIG_NAME).run(args);
    }
}
