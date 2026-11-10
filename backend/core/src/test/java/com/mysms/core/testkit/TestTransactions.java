package com.mysms.core.testkit;

import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.tx.Transactions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

/**
 * Test kit: runs a test's own setup or check in a transaction of a module's database, on that module's
 * {@link PlatformTransactionManager} with the same definitions and tenant binding the services use. Production code
 * opens its transactions itself (JAVA-CODING-RULES §4); tests use this so setup stays one readable line.
 */
public final class TestTransactions {

    /** The work of one transaction; returns its result (or null). */
    @FunctionalInterface
    public interface Work<T> {
        T run();
    }

    private final ModuleDatabase database;
    private final PlatformTransactionManager transactions;

    private TestTransactions(ModuleDatabase database) {
        this.database = database;
        this.transactions = database.transactionManager();
    }

    public static TestTransactions of(ModuleDatabase database) {
        return new TestTransactions(database);
    }

    /** A read-write transaction bound to the call context's tenant. */
    public <T> T write(Work<T> work) {
        return run(database.writeDefinition(), true, work);
    }

    /** A read-only transaction bound to the call context's tenant. */
    public <T> T read(Work<T> work) {
        return run(database.readDefinition(), true, work);
    }

    /** A read-only transaction that binds no tenant (global tables). */
    public <T> T readWithoutTenant(Work<T> work) {
        return run(database.readDefinition(), false, work);
    }

    /** A read-write transaction that binds no tenant, with the call context's tenant set aside (global tables). */
    public <T> T writeWithoutTenant(Work<T> work) {
        try (CallContextHolder.Scope global = CallContextHolder.withoutTenant()) {
            return run(database.writeDefinition(), false, work);
        }
    }

    private <T> T run(TransactionDefinition definition, boolean bindTenant, Work<T> work) {
        TransactionStatus transaction = transactions.getTransaction(definition);
        try {
            if (bindTenant) {
                database.bindTenant();
            }
            return work.run();
        } catch (RuntimeException | Error failure) {
            Transactions.rollback(transactions, transaction, failure);
            throw failure;
        } finally {
            Transactions.commit(transactions, transaction);
        }
    }
}
