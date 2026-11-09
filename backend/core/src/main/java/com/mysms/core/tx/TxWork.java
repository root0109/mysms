package com.mysms.core.tx;

/** The body of a transaction. Runs on the module's own connection; returns the result handed back to the caller. */
@FunctionalInterface
public interface TxWork<T> {
    T run();
}
