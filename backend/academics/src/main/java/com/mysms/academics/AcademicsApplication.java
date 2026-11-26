package com.mysms.academics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

/**
 * The Academics microservice: its areas (academics, timetable, attendance, assessment) each own their schema, pool,
 * transaction manager and contract, served by one process. Components are named by fully qualified class name, as in
 * the monolith, so two areas may each have, say, a PersonMapper. In the monolith the same module runs inside
 * {@code app}, whose configuration merges every module's {@code application.properties}.
 */
@SpringBootApplication(nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
public class AcademicsApplication {

    protected AcademicsApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(AcademicsApplication.class, args);
    }
}
