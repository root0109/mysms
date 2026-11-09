package com.mysms.org;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * The Organisation & Calendar microservice: owns schema "org" and serves the org contract.
 * Its configuration is org-service.yml, which imports the module's own mysms-org.yml;
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class OrgApplication {

    public static final String CONFIG_NAME = "org-service";

    protected OrgApplication() {
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(OrgApplication.class).properties("spring.config.name=" + CONFIG_NAME).run(args);
    }
}
