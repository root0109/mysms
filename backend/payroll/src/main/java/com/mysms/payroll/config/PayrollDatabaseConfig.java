package com.mysms.payroll.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.db.ModuleDatabaseHealthIndicator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * The module's own database: pool, transaction manager, executor and Flyway history for schema "payroll" only,
 * bound from mysms.payroll.datasource.* (every transaction binds the caller's tenant for row-level security).
 */
@Configuration(proxyBeanMethods = false)
public class PayrollDatabaseConfig {

    /** Bean name; every repository of this module injects the database by this qualifier. */
    public static final String DATABASE = "payrollDatabase";

    @Bean(name = DATABASE, destroyMethod = "close")
    public ModuleDatabase payrollDatabase(Environment environment) {
        return ModuleDatabase.open("payroll", environment, true);
    }

    @Bean
    public ModuleDatabaseHealthIndicator payrollDatabaseHealthIndicator(@Qualifier(DATABASE) ModuleDatabase database) {
        return new ModuleDatabaseHealthIndicator(database);
    }
}
