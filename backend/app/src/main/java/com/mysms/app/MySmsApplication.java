package com.mysms.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

/**
 * The monolith: every module's components in one process. The modules' own {@code @SpringBootApplication} classes are
 * excluded and each module still opens its own database; components are named by fully qualified class name so two
 * modules may each have, say, a PersonMapper. Configuration is every module's application.properties, overridden by
 * this module's.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = {
        "com.mysms.master",
        "com.mysms.platform",
        "com.mysms.iam",
        "com.mysms.org",
        "com.mysms.people",
        "com.mysms.academics",
        "com.mysms.sis",
        "com.mysms.hr",
        "com.mysms.payroll",
        "com.mysms.admission",
        "com.mysms.attendance",
        "com.mysms.facility",
        "com.mysms.timetable",
        "com.mysms.assessment",
        "com.mysms.lms",
        "com.mysms.finance",
        "com.mysms.library",
        "com.mysms.transport",
        "com.mysms.comms",
        "com.mysms.frontoffice",
        "com.mysms.careers"},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = SpringBootApplication.class),
        nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
public class MySmsApplication {

    protected MySmsApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(MySmsApplication.class, args);
    }
}
