package com.mysms.core.testkit;

import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;

/**
 * Runs a test's setup or check in a module transaction exactly as a service does, with the same definitions, tenant
 * binding and nesting rules. Tests only.
 */
public final class TestTransactions extends AppTransactionManager {

    /** The work of one transaction; returns its result (or null). */
    @FunctionalInterface
    public interface Work<T> {
        T run();
    }

    private TestTransactions(ModuleDatabase database) {
        super(database, database.transactionManager());
    }

    public static TestTransactions of(ModuleDatabase database) {
        return new TestTransactions(database);
    }

    /** A read-write transaction bound to the call context's tenant. */
    public <T> T write(Work<T> work) {
        return doInWriteTransaction(work::run);
    }

    /** A read-only transaction bound to the call context's tenant. */
    public <T> T read(Work<T> work) {
        return doInReadTransaction(work::run);
    }

    /** A read-only transaction that binds no tenant (global tables). */
    public <T> T readWithoutTenant(Work<T> work) {
        return doInGlobalReadTransaction(work::run);
    }

    /** A read-write transaction that binds no tenant, with the call context's tenant set aside (global tables). */
    public <T> T writeWithoutTenant(Work<T> work) {
        try (CallContextHolder.Scope global = CallContextHolder.withoutTenant()) {
            return doInGlobalWriteTransaction(work::run);
        }
    }
}
