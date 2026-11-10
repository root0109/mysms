package com.mysms.core.db;

/**
 * The work of one transaction, handed to {@link AppTransactionManager}: it runs inside the transaction and its value
 * is the transaction's result.
 *
 * @param <K> what the work answers
 */
@FunctionalInterface
public interface CallBack<K> {

    K execute();
}
