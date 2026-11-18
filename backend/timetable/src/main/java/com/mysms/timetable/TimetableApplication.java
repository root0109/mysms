package com.mysms.timetable;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The Timetable microservice: owns schema "timetable". In the monolith the same module runs inside {@code app}, whose
 * configuration merges every module's {@code application.properties}.
 */
@SpringBootApplication
public class TimetableApplication {

    protected TimetableApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.run(TimetableApplication.class, args);
    }
}
