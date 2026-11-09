package com.mysms.core.id;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.UUID;

/**
 * UUID version 7 (RFC 9562): 48-bit Unix epoch milliseconds, version, 74 random bits. Ids sort by creation time,
 * which keeps B-tree inserts append-mostly. Every MySMS primary key is generated here, never by the database.
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {
    }

    /** A new UUIDv7 for the current time. */
    public static UUID next() {
        return next(Clock.systemUTC());
    }

    /** A new UUIDv7 for the clock's current time (tests pass a fixed clock). */
    public static UUID next(Clock clock) {
        long millis = clock.millis();
        long randA = RANDOM.nextInt(1 << 12);
        long msb = (millis & 0xFFFF_FFFF_FFFFL) << 16 | 0x7000L | randA;
        long lsb = RANDOM.nextLong() & 0x3FFF_FFFF_FFFF_FFFFL | 0x8000_0000_0000_0000L;
        return new UUID(msb, lsb);
    }

    /** The creation time encoded in a UUIDv7, in epoch milliseconds. */
    public static long timestampOf(UUID uuid) {
        if (uuid.version() != 7) {
            throw new IllegalArgumentException("not a UUIDv7");
        }
        return uuid.getMostSignificantBits() >>> 16;
    }
}
