package com.mysms.comms.business;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mysms.core.db.ModuleDatabase;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * Stand-ins for comms' database and transaction manager in a unit test of a service: every unit of work runs as the
 * service writes it, but nothing reaches a database, so the repositories the test hands the service decide every
 * answer.
 */
final class StandInTransactions {

    final ModuleDatabase database = mock(ModuleDatabase.class);
    final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);

    StandInTransactions() {
        DefaultTransactionDefinition write = new DefaultTransactionDefinition();
        DefaultTransactionDefinition read = new DefaultTransactionDefinition();
        read.setReadOnly(true);
        when(database.writeDefinition()).thenReturn(write);
        when(database.readDefinition()).thenReturn(read);
        when(transactions.getTransaction(any())).thenAnswer(call -> new SimpleTransactionStatus(true));
    }
}
