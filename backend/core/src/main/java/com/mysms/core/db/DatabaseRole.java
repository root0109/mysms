package com.mysms.core.db;

/** Which copy of a module's database a connection comes from: the primary, or the optional read replica. */
public enum DatabaseRole {

    /** The database every write and every read inside a write goes to. */
    PRIMARY,

    /** A streaming replica of the primary, for read-only transactions; may lag the primary by its replication delay. */
    REPLICA
}
