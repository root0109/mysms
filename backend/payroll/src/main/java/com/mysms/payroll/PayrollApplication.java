package com.mysms.payroll;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * The Payroll microservice: owns schema "payroll" and serves the payroll contract.
 * Its configuration is payroll-service.yml, which imports the module's own mysms-payroll.yml;
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class PayrollApplication {

    public static final String CONFIG_NAME = "payroll-service";

    protected PayrollApplication() {
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(PayrollApplication.class).properties("spring.config.name=" + CONFIG_NAME).run(args);
    }
}
