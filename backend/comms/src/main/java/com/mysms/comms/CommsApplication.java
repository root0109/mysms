package com.mysms.comms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

/**
 * The Comms microservice: its areas (comms) each own their schema, pool, transaction manager and contract,
 * served by one process. Components are named by fully qualified class name, as in the monolith, so two areas may
 * each have, say, a PersonMapper. In the monolith the same module runs inside {@code app}, whose configuration merges
 * every module's {@code application.properties}.
 */
@SpringBootApplication(nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
public class CommsApplication {

    protected CommsApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(CommsApplication.class, args);
    }
}
