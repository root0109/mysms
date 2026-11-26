package com.mysms.privacy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

/**
 * The Privacy microservice: its areas (privacy) each own their schema, pool, transaction manager and contract,
 * served by one process. Components are named by fully qualified class name, as in the monolith, so two areas may
 * each have, say, a PersonMapper. In the monolith the same module runs inside {@code app}, whose configuration merges
 * every module's {@code application.properties}.
 */
@SpringBootApplication(nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
public class PrivacyApplication {

    protected PrivacyApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(PrivacyApplication.class, args);
    }
}
