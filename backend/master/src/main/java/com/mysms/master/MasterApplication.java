package com.mysms.master;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * The Master Data microservice: owns schema "master" and serves the master contract.
 * Its configuration is master-service.yml, which imports the module's own mysms-master.yml;
 * in the monolith the same module runs inside `app`.
 */
@SpringBootApplication
public class MasterApplication {

    public static final String CONFIG_NAME = "master-service";

    protected MasterApplication() {
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(MasterApplication.class).properties("spring.config.name=" + CONFIG_NAME).run(args);
    }
}
