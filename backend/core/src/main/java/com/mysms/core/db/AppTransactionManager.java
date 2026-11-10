package com.mysms.core.db;

import com.mysms.core.tx.Transactions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * The base of every class that works in a database transaction (owner decision of 2026-11-10; JAVA-CODING-RULES §4):
 * a service extends it and hands each unit of work to {@link #doInReadTransaction} or {@link #doInWriteTransaction}
 * as a {@link CallBack} (a Template Method over the module's own {@link PlatformTransactionManager}; transactions
 * stay programmatic, never annotation-driven). The transaction is the module's
 * ({@link ModuleDatabase#readDefinition()} / {@link ModuleDatabase#writeDefinition()}: READ COMMITTED, the module's
 * timeout, read-only for reads), and the caller's tenant is bound to it before the work starts, so row-level security
 * applies to every statement of the work.
 *
 * <p>A read-only transaction reads from the module's replica when it has one ({@link RoutingDataSource}); every
 * write, and every read inside a write, uses the primary. A read from the replica may lag the primary by the
 * replication delay: a client that acts on what it read sends the ETag back, so a stale read is refused (412)
 * instead of overwriting newer data. A read that must see the latest committed state — a security decision such as
 * sign-in (a password just changed, an account just locked, a tenant just suspended), or a read right after a write
 * of the same call — uses {@link #doInPrimaryReadTransaction} / {@link #doInGlobalPrimaryReadTransaction}.
 *
 * <p>The work's value is committed and returned; any {@link RuntimeException} or {@link Error} rolls the transaction
 * back and is rethrown as it is — a failing rollback never hides it, and a failing commit is thrown instead of the
 * value. The {@code run…} variants are the same for work that answers nothing. The {@code …Global…} variants bind no
 * tenant: they are for global tables and for reads that happen before a tenant is known (database/README "Global
 * tables"); they do not set the caller's tenant aside — work on a global table runs inside
 * {@code CallContextHolder.withoutTenant()} where its policy requires that.
 *
 * <p><b>Work that calls other work.</b> A unit of work that calls another service of the same module joins the
 * transaction already running: nothing is committed until the outermost work ends, and a failure anywhere rolls all
 * of it back. Two combinations are programming errors and are refused with an {@link IllegalStateException} before
 * the inner work runs: writing inside a read-only transaction (the database would refuse the first statement with a
 * far less clear message), and tenant work inside a global transaction (the tenant would be bound half-way through
 * work that was meant to run without one). Global work inside a tenant transaction is allowed; the tenant stays
 * bound, which global tables do not look at.
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
            return callBack.execute();
        } catch (RuntimeException | Error failure) {
            Transactions.rollback(platformTransactionManager, transaction, failure);
            throw failure;
        } finally {
            if (outermost) {
                TransactionSynchronizationManager.unbindResourceIfPossible(platformTransactionManager);
            }
            Transactions.commit(platformTransactionManager, transaction);
        }
    }

    /** What the outermost work of this thread asked for, kept while its transaction runs (see the class comment). */
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
