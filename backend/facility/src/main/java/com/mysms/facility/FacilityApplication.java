package com.mysms.facility;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Rooms & Facilities microservice: owns schema "facility". In the monolith the same module runs inside {@code app},
 * whose configuration merges every module's {@code application.properties}.
 */
@SpringBootApplication
public class FacilityApplication {

    protected FacilityApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(FacilityApplication.class, args);
    }
}
