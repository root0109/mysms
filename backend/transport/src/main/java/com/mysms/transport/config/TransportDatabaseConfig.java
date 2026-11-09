package com.mysms.transport.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.db.ModuleDatabaseHealthIndicator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * The module's own database: pool, transaction manager, executor and Flyway history for schema "transport" only,
 * bound from mysms.transport.datasource.* (every transaction binds the caller's tenant for row-level security).
 */
@Configuration(proxyBeanMethods = false)
public class TransportDatabaseConfig {

    /** Bean name; every repository of this module injects the database by this qualifier. */
    public static final String DATABASE = "transportDatabase";

    @Bean(name = DATABASE, destroyMethod = "close")
    public ModuleDatabase transportDatabase(Environment environment) {
        return ModuleDatabase.open("transport", environment, true);
    }

    @Bean
    public ModuleDatabaseHealthIndicator transportDatabaseHealthIndicator(@Qualifier(DATABASE) ModuleDatabase database) {
        return new ModuleDatabaseHealthIndicator(database);
    }
}
