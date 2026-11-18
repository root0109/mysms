package com.mysms.iam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Identity & Access microservice: owns schema "iam". In the monolith the same module runs inside {@code app}, whose
 * configuration merges every module's {@code application.properties}.
 */
@SpringBootApplication
public class IamApplication {

    protected IamApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(IamApplication.class, args);
    }
}
