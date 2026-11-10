package com.mysms.iam.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.db.ModuleDatabaseHealthIndicator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The module's own database: pool, transaction manager and Flyway history for schema "iam" only,
 * bound from mysms.iam.datasource.* (every transaction binds the caller's tenant for row-level security).
 */
@Configuration(proxyBeanMethods = false)
public class IamDatabaseConfig {

    /** Bean name; every repository of this module injects the database by this qualifier. */
    public static final String DATABASE = "iamDatabase";

    /** Bean name of the module's transaction manager; every service injects it by this qualifier (§4). */
    public static final String TRANSACTIONS = "iamTransactions";

    @Bean(name = DATABASE, destroyMethod = "close")
    public ModuleDatabase iamDatabase(Environment environment) {
        return ModuleDatabase.open("iam", environment, true);
    }

    /**
     * The transaction manager of this module's own connection pool. Services open every transaction on it
     * themselves; the database binds the call's tenant inside each one (JAVA-CODING-RULES §4).
     */
    @Bean(name = TRANSACTIONS)
    public PlatformTransactionManager iamTransactions(@Qualifier(DATABASE) ModuleDatabase database) {
        return database.transactionManager();
    }

    @Bean
    public ModuleDatabaseHealthIndicator iamDatabaseHealthIndicator(@Qualifier(DATABASE) ModuleDatabase database) {
        return new ModuleDatabaseHealthIndicator(database);
    }
}
