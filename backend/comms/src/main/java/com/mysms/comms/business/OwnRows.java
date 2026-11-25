package com.mysms.comms.business;

import com.mysms.authclient.AuthContext;
import com.mysms.core.error.ResourceNotFoundException;
import java.util.UUID;

/**
 * The rule of a person's own rows (notifications, mail, reactions, read and follow state): whose row it is comes from
 * the token, never from the body, and anybody else's row is answered as absent — a tenant-wide grant does not widen it.
 */
final class OwnRows {

    private OwnRows() {
    }

    /** The caller, who owns the rows the operation reads or writes. */
    static UUID caller() {
        return AuthContext.require().userId();
    }

    /** 404 unless the caller owns the row. */
    static void require(UUID owner, String aggregate, UUID id) {
        if (!caller().equals(owner)) {
            throw new ResourceNotFoundException(aggregate, id);
        }
    }
}
