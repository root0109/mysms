package com.mysms.core.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** The transaction base class against stand-ins: which definition is used, when the tenant is bound, how work ends. */
class AppTransactionManagerTest {

    private final ModuleDatabase database = mock(ModuleDatabase.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final TransactionDefinition read = definition(true);
    private final TransactionDefinition write = definition(false);
    private final SimpleTransactionStatus status = new SimpleTransactionStatus();
    private final Work work = new Work(database, transactions);

    /** A service as the modules write them: it only hands its work to the base class. */
    private static final class Work extends AppTransactionManager {

        Work(ModuleDatabase database, PlatformTransactionManager transactions) {
            super(database, transactions);
        }

        String read(CallBack<String> callBack) {
            return doInReadTransaction(callBack);
        }

        String write(CallBack<String> callBack) {
            return doInWriteTransaction(callBack);
        }

        String globalRead(CallBack<String> callBack) {
            return doInGlobalReadTransaction(callBack);
        }

        String globalWrite(CallBack<String> callBack) {
            return doInGlobalWriteTransaction(callBack);
        }

        void run(Runnable task) {
            runInWriteTransaction(task);
        }

        void runRead(Runnable task) {
            runInReadTransaction(task);
        }

        void runGlobal(Runnable task) {
            runInGlobalWriteTransaction(task);
        }

        String nested(CallBack<String> callBack) {
            return doInNestedWriteTransaction(callBack);
        }
    }

    private static TransactionDefinition definition(boolean readOnly) {
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setReadOnly(readOnly);
        return definition;
    }

    private void given() {
        when(database.readDefinition()).thenReturn(read);
        when(database.writeDefinition()).thenReturn(write);
        when(transactions.getTransaction(any())).thenReturn(status);
    }

    @Test
    void theWorkRunsInTheModulesTransactionWithTheTenantBoundAndItsValueIsCommitted() {
        given();
        assertThat(work.write(() -> "stored")).isEqualTo("stored");

        InOrder order = inOrder(transactions, database);
        order.verify(transactions).getTransaction(write);
        order.verify(database).bindTenant();
        order.verify(transactions).commit(status);
        verify(transactions, never()).rollback(any());

        assertThat(work.read(() -> "found")).isEqualTo("found");
        verify(transactions).getTransaction(read);
    }

    @Test
    void nestedWorkRunsInASavepointOfTheWriteTransaction() {
        given();
        assertThat(work.nested(() -> "row")).isEqualTo("row");
        ArgumentCaptor<TransactionDefinition> used = ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(transactions).getTransaction(used.capture());
        assertThat(used.getValue().getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_NESTED);
        assertThat(used.getValue().isReadOnly()).isFalse();
        verify(database).bindTenant();
        verify(transactions).commit(status);
    }

    @Test
    void globalWorkBindsNoTenant() {
        given();
        assertThat(work.globalRead(() -> "catalogue")).isEqualTo("catalogue");
        assertThat(work.globalWrite(() -> "changed")).isEqualTo("changed");
        work.runGlobal(() -> { });
        verify(database, never()).bindTenant();
        verify(transactions).getTransaction(read);
    }

    @Test
    void workThatAnswersNothingIsCommittedTheSameWay() {
        given();
        StringBuilder done = new StringBuilder();
        work.run(() -> done.append("w"));
        work.runRead(() -> done.append("r"));
        assertThat(done).hasToString("wr");
        verify(database, times(2)).bindTenant();
        verify(transactions, times(2)).commit(status);
    }

    @Test
    void aFailureRollsBackAndIsRethrownAsItIs() {
        given();
        IllegalStateException failure = new IllegalStateException("no");
        // the real manager marks the transaction completed when it rolls back; the stand-in is told to
        doAnswer(call -> {
            status.setCompleted();
            return null;
        }).when(transactions).rollback(status);

        assertThatThrownBy(() -> work.write(() -> {
            throw failure;
        })).isSameAs(failure);
        verify(transactions).rollback(status);
        verify(transactions, never()).commit(any());
    }

    @Test
    void anErrorRollsBackTooAndAFailingRollbackDoesNotHideTheCause() {
        given();
        StackOverflowError error = new StackOverflowError();
        IllegalStateException broken = new IllegalStateException("connection lost");
        // the real manager completes the transaction even when its rollback fails; the stand-in is told to
        doAnswer(call -> {
            status.setCompleted();
            throw broken;
        }).when(transactions).rollback(status);

        assertThatThrownBy(() -> work.write(() -> {
            throw error;
        })).isSameAs(error);
        assertThat(error.getSuppressed()).containsExactly(broken);
        verify(transactions).rollback(status);
        verify(transactions, never()).commit(any());
    }

    @Test
    void workCalledFromOtherWorkJoinsItsTransactionAndOnlyTheOutermostBindsTheTenant() {
        given();
        SimpleTransactionStatus joined = new SimpleTransactionStatus(false);
        assertThat(work.write(() -> {
            when(transactions.getTransaction(any())).thenReturn(joined);
            return work.read(() -> "inner") + work.globalRead(() -> "+catalogue") + work.write(() -> "+stored");
        })).isEqualTo("inner+catalogue+stored");
        verify(database, times(1)).bindTenant();
        verify(transactions).commit(status);
    }

    @Test
    void aWriteInsideAReadAndTenantWorkInsideGlobalWorkAreRefusedBeforeTheyRun() {
        given();
        SimpleTransactionStatus joined = new SimpleTransactionStatus(false);
        StringBuilder ran = new StringBuilder();
        assertThatThrownBy(() -> work.read(() -> {
            when(transactions.getTransaction(any())).thenReturn(joined);
            return work.write(() -> ran.append("write").toString());
        })).isInstanceOf(IllegalStateException.class).hasMessageContaining("read-only");

        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        assertThatThrownBy(() -> work.globalWrite(() -> {
            when(transactions.getTransaction(any())).thenReturn(joined);
            return work.read(() -> ran.append("tenant").toString());
        })).isInstanceOf(IllegalStateException.class).hasMessageContaining("global");
        assertThat(ran).isEmpty();
        verify(database, times(1)).bindTenant(); // the outer read's; the refused tenant work bound nothing

        // nothing is left behind on the thread: the next outermost work starts clean
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        assertThat(work.write(() -> "next")).isEqualTo("next");
    }

    @Test
    void aTransactionBegunElsewhereIsJoinedAndTheTenantIsBoundForTheWork() {
        given();
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus(false));
        assertThat(work.write(() -> "joined")).isEqualTo("joined");
        assertThat(work.globalRead(() -> "catalogue")).isEqualTo("catalogue");
        verify(database, times(1)).bindTenant();
    }

    @Test
    void aFailingCommitIsThrownInsteadOfTheValue() {
        given();
        IllegalStateException refused = new IllegalStateException("serialization failure");
        doThrow(refused).when(transactions).commit(status);
        assertThatThrownBy(() -> work.write(() -> "lost")).isSameAs(refused);
    }

    @Test
    void aRollbackThatFailsBeforeCompletingIsNeverFollowedByACommit() {
        given();
        IllegalStateException failure = new IllegalStateException("no");
        IllegalStateException broken = new IllegalStateException("connection lost");
        // the transaction stays open: a commit attempted now would replace the cause with its own exception
        doThrow(broken).when(transactions).rollback(status);

        assertThatThrownBy(() -> work.write(() -> {
            throw failure;
        })).isSameAs(failure);
        assertThat(failure.getSuppressed()).containsExactly(broken);
        verify(transactions).rollback(status);
        verify(transactions, never()).commit(any());
        assertThat(TransactionSynchronizationManager.getResource(transactions)).isNull();
    }

    @Test
    void anErrorThrownByTheRollbackIsSuppressedUnderTheCause() {
        given();
        IllegalStateException failure = new IllegalStateException("no");
        OutOfMemoryError broken = new OutOfMemoryError("rollback");
        doThrow(broken).when(transactions).rollback(status);

        assertThatThrownBy(() -> work.write(() -> {
            throw failure;
        })).isSameAs(failure);
        assertThat(failure.getSuppressed()).containsExactly(broken);
        verify(transactions, never()).commit(any());
    }

    @Test
    void aCheckedExceptionThrownSneakilyRollsBackAndIsRethrownAsItIs() {
        given();
        IOException failure = new IOException("disk full");

        assertThatThrownBy(() -> work.write(() -> {
            throw AppTransactionManagerTest.<RuntimeException>sneaky(failure);
        })).isSameAs(failure);
        verify(transactions).rollback(status);
        verify(transactions, never()).commit(any());
        assertThat(TransactionSynchronizationManager.getResource(transactions)).isNull();
    }

    @Test
    void aThrowableThatIsNeitherAnExceptionNorAnErrorStillRollsBackAndNeverCommits() {
        given();
        Throwable odd = new Throwable("neither");

        assertThatThrownBy(() -> work.write(() -> {
            throw AppTransactionManagerTest.<RuntimeException>sneaky(odd);
        })).isSameAs(odd);
        verify(transactions).rollback(status);
        verify(transactions, never()).commit(any());
        assertThat(TransactionSynchronizationManager.getResource(transactions)).isNull();

        // the thread is clean: the next work commits as usual
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        assertThat(work.write(() -> "next")).isEqualTo("next");
    }

    /** Throws any throwable from code that declares none, as a library compiled elsewhere can. */
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException sneaky(Throwable throwable) throws T {
        throw (T) throwable;
    }
}
