package com.mysms.assessment.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.db.ModuleDatabaseHealthIndicator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The module's own database: pool, transaction manager and Flyway history for schema "assessment" only,
 * bound from mysms.assessment.datasource.* (every transaction binds the caller's tenant for row-level security).
 */
@Configuration(proxyBeanMethods = false)
public class AssessmentDatabaseConfig {

    /** Bean name; every repository of this module injects the database by this qualifier. */
    public static final String DATABASE = "assessmentDatabase";

    /** Bean name of the module's transaction manager; every service injects it by this qualifier. */
    public static final String TRANSACTIONS = "assessmentTransactions";

    @Bean(name = DATABASE, destroyMethod = "close")
    public ModuleDatabase assessmentDatabase(Environment environment) {
        return ModuleDatabase.open("assessment", environment, true);
    }

    /**
     * The transaction manager of the module's own pool; the database binds the call's tenant inside each transaction.
     */
    @Bean(name = TRANSACTIONS)
    public PlatformTransactionManager assessmentTransactions(@Qualifier(DATABASE) ModuleDatabase database) {
        return database.transactionManager();
    }

    @Bean
    public ModuleDatabaseHealthIndicator assessmentDatabaseHealthIndicator(@Qualifier(DATABASE) ModuleDatabase database) {
        return new ModuleDatabaseHealthIndicator(database);
    }
}
