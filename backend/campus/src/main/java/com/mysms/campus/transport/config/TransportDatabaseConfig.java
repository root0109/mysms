package com.mysms.campus.transport.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.db.ModuleDatabaseHealthIndicator;
import com.mysms.core.jobs.JobSchedule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The module's own database: pool, transaction manager and Flyway history for schema "transport" only,
 * bound from mysms.transport.datasource.* (every transaction binds the caller's tenant for row-level security).
 */
@Configuration(proxyBeanMethods = false)
public class TransportDatabaseConfig {

    /** Bean name; every repository of this module injects the database by this qualifier. */
    public static final String DATABASE = "transportDatabase";

    /** Bean name of the module's transaction manager; every service injects it by this qualifier. */
    public static final String TRANSACTIONS = "transportTransactions";

    /** Bean name of the module's job schedule; the module's scheduled jobs inject it by this qualifier. */
    public static final String JOBS = "transportJobSchedule";

    @Bean(name = DATABASE, destroyMethod = "close")
    public ModuleDatabase transportDatabase(Environment environment) {
        return ModuleDatabase.open("transport", environment, true);
    }

    /**
     * The transaction manager of the module's own pool; the database binds the call's tenant inside each transaction.
     */
    @Bean(name = TRANSACTIONS)
    public PlatformTransactionManager transportTransactions(@Qualifier(DATABASE) ModuleDatabase database) {
        return database.transactionManager();
    }

    /** transport.job_schedule: the tenants enrolled in the module's scheduled jobs (core JobRuntime runs them). */
    @Bean(name = JOBS)
    public JobSchedule transportJobSchedule(@Qualifier(DATABASE) ModuleDatabase database) {
        return new JobSchedule(database);
    }

    @Bean
    public ModuleDatabaseHealthIndicator transportDatabaseHealthIndicator(@Qualifier(DATABASE) ModuleDatabase database) {
        return new ModuleDatabaseHealthIndicator(database);
    }
}
