package com.mysms.master.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.db.ModuleDatabaseHealthIndicator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * The module's own database: pool, transaction manager, executor and Flyway history for schema "master" only,
 * bound from mysms.master.datasource.* (master holds global reference data: no tenant column, so transactions bind no tenant).
 */
@Configuration(proxyBeanMethods = false)
public class MasterDatabaseConfig {

    /** Bean name; every repository of this module injects the database by this qualifier. */
    public static final String DATABASE = "masterDatabase";

    @Bean(name = DATABASE, destroyMethod = "close")
    public ModuleDatabase masterDatabase(Environment environment) {
        return ModuleDatabase.open("master", environment, false);
    }

    @Bean
    public ModuleDatabaseHealthIndicator masterDatabaseHealthIndicator(@Qualifier(DATABASE) ModuleDatabase database) {
        return new ModuleDatabaseHealthIndicator(database);
    }
}
