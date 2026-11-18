package com.mysms.facility.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.db.ModuleDatabaseHealthIndicator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The module's own database: pool, transaction manager and Flyway history for schema "facility" only,
 * bound from mysms.facility.datasource.* (every transaction binds the caller's tenant for row-level security).
 */
@Configuration(proxyBeanMethods = false)
public class FacilityDatabaseConfig {

    /** Bean name; every repository of this module injects the database by this qualifier. */
    public static final String DATABASE = "facilityDatabase";

    /** Bean name of the module's transaction manager; every service injects it by this qualifier. */
    public static final String TRANSACTIONS = "facilityTransactions";

    @Bean(name = DATABASE, destroyMethod = "close")
    public ModuleDatabase facilityDatabase(Environment environment) {
        return ModuleDatabase.open("facility", environment, true);
    }

    /**
     * The transaction manager of the module's own pool; the database binds the call's tenant inside each transaction.
     */
    @Bean(name = TRANSACTIONS)
    public PlatformTransactionManager facilityTransactions(@Qualifier(DATABASE) ModuleDatabase database) {
        return database.transactionManager();
    }

    @Bean
    public ModuleDatabaseHealthIndicator facilityDatabaseHealthIndicator(@Qualifier(DATABASE) ModuleDatabase database) {
        return new ModuleDatabaseHealthIndicator(database);
    }
}
