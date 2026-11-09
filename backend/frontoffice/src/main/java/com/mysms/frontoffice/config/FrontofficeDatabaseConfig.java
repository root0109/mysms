package com.mysms.frontoffice.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.db.ModuleDatabaseHealthIndicator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * The module's own database: pool, transaction manager, executor and Flyway history for schema "frontoffice" only,
 * bound from mysms.frontoffice.datasource.* (every transaction binds the caller's tenant for row-level security).
 */
@Configuration(proxyBeanMethods = false)
public class FrontofficeDatabaseConfig {

    /** Bean name; every repository of this module injects the database by this qualifier. */
    public static final String DATABASE = "frontofficeDatabase";

    @Bean(name = DATABASE, destroyMethod = "close")
    public ModuleDatabase frontofficeDatabase(Environment environment) {
        return ModuleDatabase.open("frontoffice", environment, true);
    }

    @Bean
    public ModuleDatabaseHealthIndicator frontofficeDatabaseHealthIndicator(@Qualifier(DATABASE) ModuleDatabase database) {
        return new ModuleDatabaseHealthIndicator(database);
    }
}
