package com.mysms.attendance.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.db.ModuleDatabaseHealthIndicator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * The module's own database: pool, transaction manager, executor and Flyway history for schema "attendance" only,
 * bound from mysms.attendance.datasource.* (every transaction binds the caller's tenant for row-level security).
 */
@Configuration(proxyBeanMethods = false)
public class AttendanceDatabaseConfig {

    /** Bean name; every repository of this module injects the database by this qualifier. */
    public static final String DATABASE = "attendanceDatabase";

    @Bean(name = DATABASE, destroyMethod = "close")
    public ModuleDatabase attendanceDatabase(Environment environment) {
        return ModuleDatabase.open("attendance", environment, true);
    }

    @Bean
    public ModuleDatabaseHealthIndicator attendanceDatabaseHealthIndicator(@Qualifier(DATABASE) ModuleDatabase database) {
        return new ModuleDatabaseHealthIndicator(database);
    }
}
