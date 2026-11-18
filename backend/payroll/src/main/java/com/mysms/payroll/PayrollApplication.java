package com.mysms.payroll;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Payroll microservice: owns schema "payroll". In the monolith the same module runs inside {@code app}, whose
 * configuration merges every module's {@code application.properties}.
 */
@SpringBootApplication
public class PayrollApplication {

    protected PayrollApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(PayrollApplication.class, args);
    }
}
