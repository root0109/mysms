package com.mysms.facility.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.db.ModuleDatabaseHealthIndicator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * The module's own database: pool, transaction manager, executor and Flyway history for schema "facility" only,
 * bound from mysms.facility.datasource.* (every transaction binds the caller's tenant for row-level security).
 */
@Configuration(proxyBeanMethods = false)
public class FacilityDatabaseConfig {

    /** Bean name; every repository of this module injects the database by this qualifier. */
    public static final String DATABASE = "facilityDatabase";

    @Bean(name = DATABASE, destroyMethod = "close")
    public ModuleDatabase facilityDatabase(Environment environment) {
        return ModuleDatabase.open("facility", environment, true);
    }

    @Bean
    public ModuleDatabaseHealthIndicator facilityDatabaseHealthIndicator(@Qualifier(DATABASE) ModuleDatabase database) {
        return new ModuleDatabaseHealthIndicator(database);
    }
}
