package com.mysms.payroll;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Payroll microservice: owns schema "payroll" and serves the payroll contract.
 * Core configures it from this class: the package it scans names the module (ModuleDefaultsEnvironmentPostProcessor);
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class PayrollApplication {

    protected PayrollApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(PayrollApplication.class, args);
    }
}
