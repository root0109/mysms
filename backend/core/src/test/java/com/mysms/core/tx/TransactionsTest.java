package com.mysms.core.tx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

class TransactionsTest {

    /** A transaction manager that records what happened, failing its rollback when asked to. */
    private static final class Recording extends AbstractPlatformTransactionManager {
        private static final long serialVersionUID = 1L;
        private final List<String> calls = new ArrayList<>();
        private final boolean rollbackFails;

        private Recording(boolean rollbackFails) {
            this.rollbackFails = rollbackFails;
        }

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            calls.add("begin");
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            calls.add("commit");
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            calls.add("rollback");
            if (rollbackFails) {
                throw new IllegalStateException("connection lost");
            }
        }
    }

    /** The service pattern of JAVA-CODING-RULES §4, around a body that returns or throws. */
    private static String run(Recording transactions, boolean fail) {
        TransactionStatus transaction = transactions.getTransaction(TransactionDefinition.withDefaults());
        try {
            if (fail) {
                throw new IllegalArgumentException("invalid");
            }
            return "done";
        } catch (RuntimeException | Error failure) {
            Transactions.rollback(transactions, transaction, failure);
            throw failure;
        } finally {
            Transactions.commit(transactions, transaction);
        }
    }

    @Test
    void aBodyThatReturnsCommitsOnTheWayOut() {
        Recording transactions = new Recording(false);

        assertThat(run(transactions, false)).isEqualTo("done");
        assertThat(transactions.calls).containsExactly("begin", "commit");
    }

    @Test
    void aFailingBodyRollsBackAndCommitsNothing() {
        Recording transactions = new Recording(false);

        assertThatThrownBy(() -> run(transactions, true)).isInstanceOf(IllegalArgumentException.class);
        assertThat(transactions.calls).containsExactly("begin", "rollback");
    }

    @Test
    void aFailingRollbackKeepsTheOriginalFailure() {
        Recording transactions = new Recording(true);

        assertThatThrownBy(() -> run(transactions, true)).isInstanceOf(IllegalArgumentException.class)
                .satisfies(e -> assertThat(e.getSuppressed()).singleElement()
                        .satisfies(suppressed -> assertThat(suppressed).isInstanceOf(IllegalStateException.class)
                                .hasMessage("connection lost")));
        assertThat(transactions.calls).containsExactly("begin", "rollback");
    }
}
