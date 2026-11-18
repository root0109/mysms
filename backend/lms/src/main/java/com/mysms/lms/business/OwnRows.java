package com.mysms.lms.business;

import com.mysms.authclient.AuthContext;
import com.mysms.core.error.ResourceNotFoundException;
import java.util.UUID;

/**
 * The rule of the records a person writes for themself (bookmarks, notes, votes, reviews…): whose row it is comes
 * from the token, never from the body, and anybody else's row is answered as absent.
 */
final class OwnRows {

    private OwnRows() {
    }

    /** The caller, who becomes the row's owner. */
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
