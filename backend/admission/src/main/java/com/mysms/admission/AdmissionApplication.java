package com.mysms.admission;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Admissions microservice: owns schema "admission". In the monolith the same module runs inside {@code app}, whose
 * configuration merges every module's {@code application.properties}.
 */
@SpringBootApplication
public class AdmissionApplication {

    protected AdmissionApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(AdmissionApplication.class, args);
    }
}
