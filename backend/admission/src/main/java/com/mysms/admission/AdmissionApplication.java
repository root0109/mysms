package com.mysms.admission;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * The Admissions microservice: owns schema "admission" and serves the admission contract.
 * Its configuration is admission-service.yml, which imports the module's own mysms-admission.yml;
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class AdmissionApplication {

    public static final String CONFIG_NAME = "admission-service";

    protected AdmissionApplication() {
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(AdmissionApplication.class).properties("spring.config.name=" + CONFIG_NAME).run(args);
    }
}
