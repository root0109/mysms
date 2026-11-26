package com.mysms.campus.library.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.db.ModuleDatabaseHealthIndicator;
import com.mysms.core.jobs.JobSchedule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The module's own database: pool, transaction manager and Flyway history for schema "library" only,
 * bound from mysms.library.datasource.* (every transaction binds the caller's tenant for row-level security).
 */
@Configuration(proxyBeanMethods = false)
public class LibraryDatabaseConfig {

    /** Bean name; every repository of this module injects the database by this qualifier. */
    public static final String DATABASE = "libraryDatabase";

    /** Bean name of the module's transaction manager; every service injects it by this qualifier. */
    public static final String TRANSACTIONS = "libraryTransactions";

    /** Bean name of the module's job schedule; the module's scheduled jobs inject it by this qualifier. */
    public static final String JOBS = "libraryJobSchedule";

    @Bean(name = DATABASE, destroyMethod = "close")
    public ModuleDatabase libraryDatabase(Environment environment) {
        return ModuleDatabase.open("library", environment, true);
    }

    /**
     * The transaction manager of the module's own pool; the database binds the call's tenant inside each transaction.
     */
    @Bean(name = TRANSACTIONS)
    public PlatformTransactionManager libraryTransactions(@Qualifier(DATABASE) ModuleDatabase database) {
        return database.transactionManager();
    }

    /** library.job_schedule: the tenants enrolled in the module's scheduled jobs (core JobRuntime runs them). */
    @Bean(name = JOBS)
    public JobSchedule libraryJobSchedule(@Qualifier(DATABASE) ModuleDatabase database) {
        return new JobSchedule(database);
    }

    @Bean
    public ModuleDatabaseHealthIndicator libraryDatabaseHealthIndicator(@Qualifier(DATABASE) ModuleDatabase database) {
        return new ModuleDatabaseHealthIndicator(database);
    }
}
