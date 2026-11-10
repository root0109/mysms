package com.mysms.platform.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.db.ModuleDatabaseHealthIndicator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The module's own database: pool, transaction manager and Flyway history for schema "platform" only,
 * bound from mysms.platform.datasource.* (every transaction binds the caller's tenant for row-level security).
 */
@Configuration(proxyBeanMethods = false)
public class PlatformDatabaseConfig {

    /** Bean name; every repository of this module injects the database by this qualifier. */
    public static final String DATABASE = "platformDatabase";

    /** Bean name of the module's transaction manager; every service injects it by this qualifier (§4). */
    public static final String TRANSACTIONS = "platformTransactions";

    @Bean(name = DATABASE, destroyMethod = "close")
    public ModuleDatabase platformDatabase(Environment environment) {
        return ModuleDatabase.open("platform", environment, true);
    }

    /**
     * The transaction manager of this module's own connection pool. Services open every transaction on it
     * themselves; the database binds the call's tenant inside each one (JAVA-CODING-RULES §4).
     */
    @Bean(name = TRANSACTIONS)
    public PlatformTransactionManager platformTransactions(@Qualifier(DATABASE) ModuleDatabase database) {
        return database.transactionManager();
    }

    @Bean
    public ModuleDatabaseHealthIndicator platformDatabaseHealthIndicator(@Qualifier(DATABASE) ModuleDatabase database) {
        return new ModuleDatabaseHealthIndicator(database);
    }
}
