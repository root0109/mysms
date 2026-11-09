package com.mysms.hr;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * The Workforce / HR microservice: owns schema "hr" and serves the hr contract.
 * Its configuration is hr-service.yml, which imports the module's own mysms-hr.yml;
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class HrApplication {

    public static final String CONFIG_NAME = "hr-service";

    protected HrApplication() {
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(HrApplication.class).properties("spring.config.name=" + CONFIG_NAME).run(args);
    }
}
