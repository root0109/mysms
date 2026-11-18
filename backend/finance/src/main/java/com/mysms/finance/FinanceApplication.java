package com.mysms.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Fees & Payments microservice: owns schema "finance" and serves the finance contract. In the monolith it runs
 * inside {@code app}, which combines its {@code application.properties} with every other module's.
 */
@SpringBootApplication
public class FinanceApplication {

    protected FinanceApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(FinanceApplication.class, args);
    }
}
