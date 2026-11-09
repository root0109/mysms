package com.mysms.org.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.db.ModuleDatabaseHealthIndicator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * The module's own database: pool, transaction manager, executor and Flyway history for schema "org" only,
 * bound from mysms.org.datasource.* (every transaction binds the caller's tenant for row-level security).
 */
@Configuration(proxyBeanMethods = false)
public class OrgDatabaseConfig {

    /** Bean name; every repository of this module injects the database by this qualifier. */
    public static final String DATABASE = "orgDatabase";

    @Bean(name = DATABASE, destroyMethod = "close")
    public ModuleDatabase orgDatabase(Environment environment) {
        return ModuleDatabase.open("org", environment, true);
    }

    @Bean
    public ModuleDatabaseHealthIndicator orgDatabaseHealthIndicator(@Qualifier(DATABASE) ModuleDatabase database) {
        return new ModuleDatabaseHealthIndicator(database);
    }
}
