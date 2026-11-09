package com.mysms.assessment;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * The Assessment & Results microservice: owns schema "assessment" and serves the assessment contract.
 * Its configuration is assessment-service.yml, which imports the module's own mysms-assessment.yml;
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class AssessmentApplication {

    public static final String CONFIG_NAME = "assessment-service";

    protected AssessmentApplication() {
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(AssessmentApplication.class).properties("spring.config.name=" + CONFIG_NAME).run(args);
    }
}
