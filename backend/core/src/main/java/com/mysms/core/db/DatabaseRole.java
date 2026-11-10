package com.mysms.core.db;

/**
 * Which copy of a module's database a connection comes from (the connection identity of the owner's {@code common}
 * module, reduced to what a module has: one primary it writes to and, optionally, one replica it reads from).
 */
public enum DatabaseRole {

    /** The database every write and every read inside a write goes to. */
    PRIMARY,

    /** A streaming replica of the primary, for read-only transactions; may lag the primary by its replication delay. */
    REPLICA
}
