package com.mysms.timetable.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.db.ModuleDatabaseHealthIndicator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * The module's own database: pool, transaction manager, executor and Flyway history for schema "timetable" only,
 * bound from mysms.timetable.datasource.* (every transaction binds the caller's tenant for row-level security).
 */
@Configuration(proxyBeanMethods = false)
public class TimetableDatabaseConfig {

    /** Bean name; every repository of this module injects the database by this qualifier. */
    public static final String DATABASE = "timetableDatabase";

    @Bean(name = DATABASE, destroyMethod = "close")
    public ModuleDatabase timetableDatabase(Environment environment) {
        return ModuleDatabase.open("timetable", environment, true);
    }

    @Bean
    public ModuleDatabaseHealthIndicator timetableDatabaseHealthIndicator(@Qualifier(DATABASE) ModuleDatabase database) {
        return new ModuleDatabaseHealthIndicator(database);
    }
}
