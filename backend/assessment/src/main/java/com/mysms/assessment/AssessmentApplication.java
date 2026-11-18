package com.mysms.assessment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Assessment & Results microservice: owns schema "assessment". In the monolith the same module runs inside {@code
 * app}, whose configuration merges every module's {@code application.properties}.
 */
@SpringBootApplication
public class AssessmentApplication {

    protected AssessmentApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(AssessmentApplication.class, args);
    }
}
