package com.mysms.core.tx;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

/**
 * The two endings of a transaction opened on a module's {@link PlatformTransactionManager}: a failure rolls back
 * first, so the {@code finally} finds the transaction completed and commits nothing; on the way out of a successful
 * body the transaction is committed, and a failing commit propagates instead of the value. Services do not write
 * this themselves: they extend {@link com.mysms.core.db.AppTransactionManager}, which is built on it; infrastructure
 * that needs a transaction definition of its own (the idempotency store, the outbox relay) uses it directly.
 */
public final class Transactions {

    private Transactions() {
    }

    /** Commits the transaction unless a rollback already completed it. */
    public static void commit(PlatformTransactionManager transactions, TransactionStatus transaction) {
        if (!transaction.isCompleted()) {
            transactions.commit(transaction);
        }
    }

    /**
     * Rolls the transaction back after {@code failure}; a failing rollback is attached to {@code failure} as suppressed,
     * so the original cause is what the caller sees.
     */
    public static void rollback(PlatformTransactionManager transactions, TransactionStatus transaction,
                                Throwable failure) {
        if (transaction.isCompleted()) {
            return;
        }
        try {
            transactions.rollback(transaction);
        } catch (RuntimeException rollbackFailure) {
            failure.addSuppressed(rollbackFailure);
        }
    }
}
