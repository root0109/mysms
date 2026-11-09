package com.mysms.finance;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * The Fees & Payments microservice: owns schema "finance" and serves the finance contract.
 * Its configuration is finance-service.yml, which imports the module's own mysms-finance.yml;
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class FinanceApplication {

    public static final String CONFIG_NAME = "finance-service";

    protected FinanceApplication() {
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(FinanceApplication.class).properties("spring.config.name=" + CONFIG_NAME).run(args);
    }
}
