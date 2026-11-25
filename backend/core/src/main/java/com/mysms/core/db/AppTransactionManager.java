package com.mysms.core.db;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Base of every service: a Template Method that runs each {@link CallBack} in a programmatic transaction of the
 * module's {@link PlatformTransactionManager}, with the caller's tenant bound first so row-level security applies to
 * every statement. Any failure (a {@link RuntimeException}, an {@link Error}, or a checked exception thrown sneakily)
 * rolls back and is rethrown unchanged; only work that completes normally commits.
 *
 * <p>Read-only transactions use the replica when the module has one and may lag the primary; a read that must see the
 * latest state (a security decision, a read after a write of the same call) uses a {@code …PrimaryRead…} variant.
 * The {@code …Global…} variants bind no tenant, for global tables and reads before a tenant is known.
 *
 * <p>Nested work joins the running transaction. Writing inside a read-only transaction, and tenant work inside a
 * global one, are programming errors refused with {@link IllegalStateException} before the inner work runs.
 */
public abstract class AppTransactionManager {

    private final ModuleDatabase moduleDatabase;
    private final PlatformTransactionManager platformTransactionManager;

    protected AppTransactionManager(ModuleDatabase moduleDatabase,
                                    PlatformTransactionManager platformTransactionManager) {
        this.moduleDatabase = moduleDatabase;
        this.platformTransactionManager = platformTransactionManager;
    }

    /** Runs the work in a read-only transaction of the caller's tenant and answers its value. */
    protected final <K> K doInReadTransaction(CallBack<K> callBack) {
        return doInTransaction(callBack, moduleDatabase.readDefinition(), true, DatabaseRole.REPLICA);
    }

    /**
     * Runs the work in a read-only transaction of the caller's tenant on the primary, never the replica: for reads
     * that decide on security or follow a write of the same call, which replication lag must not mislead.
     */
    protected final <K> K doInPrimaryReadTransaction(CallBack<K> callBack) {
        return doInTransaction(callBack, moduleDatabase.readDefinition(), true, DatabaseRole.PRIMARY);
    }

    /** Runs the work in a read-write transaction of the caller's tenant and answers its value. */
    protected final <K> K doInWriteTransaction(CallBack<K> callBack) {
        return doInTransaction(callBack, moduleDatabase.writeDefinition(), true, DatabaseRole.PRIMARY);
    }

    /** {@link #doInReadTransaction} for work that answers nothing. */
    protected final void runInReadTransaction(Runnable work) {
        doInTransaction(valueless(work), moduleDatabase.readDefinition(), true, DatabaseRole.REPLICA);
    }

    /** {@link #doInWriteTransaction} for work that answers nothing. */
    protected final void runInWriteTransaction(Runnable work) {
        doInTransaction(valueless(work), moduleDatabase.writeDefinition(), true, DatabaseRole.PRIMARY);
    }

    /**
     * Runs the work in a savepoint of the running write transaction (a new one when none is): its failure rolls back
     * only its own statements and is rethrown while the outer transaction carries on, as an import batch needs.
     */
    protected final <K> K doInNestedWriteTransaction(CallBack<K> callBack) {
        DefaultTransactionDefinition nested = new DefaultTransactionDefinition(moduleDatabase.writeDefinition());
        nested.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        return doInTransaction(callBack, nested, true, DatabaseRole.PRIMARY);
    }

    /** A read-only transaction that binds no tenant. */
    protected final <K> K doInGlobalReadTransaction(CallBack<K> callBack) {
        return doInTransaction(callBack, moduleDatabase.readDefinition(), false, DatabaseRole.REPLICA);
    }

    /** {@link #doInPrimaryReadTransaction} that binds no tenant. */
    protected final <K> K doInGlobalPrimaryReadTransaction(CallBack<K> callBack) {
        return doInTransaction(callBack, moduleDatabase.readDefinition(), false, DatabaseRole.PRIMARY);
    }

    /** A read-write transaction that binds no tenant. */
    protected final <K> K doInGlobalWriteTransaction(CallBack<K> callBack) {
        return doInTransaction(callBack, moduleDatabase.writeDefinition(), false, DatabaseRole.PRIMARY);
    }

    /** {@link #doInGlobalWriteTransaction} under the caller's own definition (the outbox relay's batches). */
    protected final <K> K doInGlobalWriteTransaction(TransactionDefinition definition, CallBack<K> callBack) {
        return doInTransaction(callBack, definition, false, DatabaseRole.PRIMARY);
    }

    /** {@link #doInGlobalWriteTransaction} for work that answers nothing. */
    protected final void runInGlobalWriteTransaction(Runnable work) {
        doInTransaction(valueless(work), moduleDatabase.writeDefinition(), false, DatabaseRole.PRIMARY);
    }

    private <K> K doInTransaction(CallBack<K> callBack, TransactionDefinition definition, boolean bindTenant,
            DatabaseRole role) {
        // the role only matters while a connection is opened: a joined transaction keeps the one it has
        TransactionStatus transaction = DatabaseRoleHolder.callWith(role,
                () -> platformTransactionManager.getTransaction(definition));
        boolean outermost = transaction.isNewTransaction();
        K value;
        boolean settled = false;
        try {
            try {
                if (outermost) {
                    TransactionSynchronizationManager.bindResource(platformTransactionManager,
                            new Running(definition.isReadOnly(), bindTenant));
                    if (bindTenant) {
                        moduleDatabase.bindTenant();
                    }
                } else if (!fitsTheRunningWork(definition.isReadOnly(), bindTenant) && bindTenant) {
                    moduleDatabase.bindTenant();
                }
                value = callBack.execute();
            } finally {
                if (outermost) {
                    TransactionSynchronizationManager.unbindResourceIfPossible(platformTransactionManager);
                }
            }
            settled = true;
        } catch (Exception | Error failure) {
            // every failure rolls back, a checked exception thrown sneakily included, and nothing commits after it;
            // the precise rethrow keeps it unchanged (the work declares no checked exception, so none is added)
            settled = true;
            rollback(transaction, failure);
            throw failure;
        } finally {
            if (!settled) {
                // a throwable that is neither an exception nor an error: still rolled back, never committed
                abandon(transaction);
            }
        }
        commit(transaction);
        return value;
    }

    /**
     * Commits work that completed normally, unless it completed the transaction itself; a failing commit propagates
     * instead of the value. Never called after a failure, so a commit can neither follow a failed rollback nor hide
     * the failure that caused it.
     */
    private void commit(TransactionStatus transaction) {
        if (!transaction.isCompleted()) {
            platformTransactionManager.commit(transaction);
        }
    }

    /**
     * Rolls back after {@code failure}; a failing rollback (an exception or an error) is attached to it as suppressed,
     * so it stays the cause, and the transaction is never committed afterwards.
     */
    private void rollback(TransactionStatus transaction, Throwable failure) {
        if (transaction.isCompleted()) {
            return;
        }
        try {
            platformTransactionManager.rollback(transaction);
        } catch (RuntimeException | Error rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
    }

    /**
     * Rolls back after a throwable that is neither an exception nor an error (only a sneaky throw makes one); a
     * failing rollback propagates in its place.
     */
    private void abandon(TransactionStatus transaction) {
        if (!transaction.isCompleted()) {
            platformTransactionManager.rollback(transaction);
        }
    }

    /** What the outermost work of this thread asked for, kept while its transaction runs. */
    private record Running(boolean readOnly, boolean tenantBound) {
    }

    /**
     * Whether the transaction being joined was begun by this class, in which case the tenant is already bound as the
     * outer work asked. False for one begun elsewhere (the inbox processor, a test): nothing is known about it, so
     * the caller binds the tenant itself.
     */
    private boolean fitsTheRunningWork(boolean readOnly, boolean bindTenant) {
        if (!(TransactionSynchronizationManager.getResource(platformTransactionManager) instanceof Running outer)) {
            return false;
        }
        if (outer.readOnly() && !readOnly) {
            throw new IllegalStateException("A write transaction was started inside a read-only one; "
                    + "begin the outer work with doInWriteTransaction.");
        }
        if (!outer.tenantBound() && bindTenant) {
            throw new IllegalStateException("Tenant work was started inside a global transaction; "
                    + "begin the outer work as tenant work, or keep the two apart.");
        }
        return true;
    }

    private static CallBack<Void> valueless(Runnable work) {
        return () -> {
            work.run();
            return null;
        };
    }
}
