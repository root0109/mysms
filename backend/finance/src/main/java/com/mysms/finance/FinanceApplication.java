package com.mysms.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

/**
 * The Finance microservice: its areas (finance) each own their schema, pool, transaction manager and contract,
 * served by one process. Components are named by fully qualified class name, as in the monolith, so two areas may
 * each have, say, a PersonMapper. In the monolith the same module runs inside {@code app}, whose configuration merges
 * every module's {@code application.properties}.
 */
@SpringBootApplication(nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
public class FinanceApplication {

    protected FinanceApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(FinanceApplication.class, args);
    }
}
