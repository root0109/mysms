package com.mysms.comms.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.ValidationFailedException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The institution a message is sent from, which the request never names: the one selected with X-Institution-Id, or
 * the caller's only institution. A caller of several institutions who selects none is 422.
 */
final class ActingInstitution {

    private ActingInstitution() {
    }

    /** The institution the caller acts in, where they hold the permission (403 otherwise). */
    static UUID require(String permission) {
        AuthContext caller = AuthContext.require();
        UUID institution = caller.activeInstitutionId().orElse(null);
        if (institution == null && !caller.allInstitutions() && caller.institutionIds().size() == 1) {
            institution = caller.institutionIds().iterator().next();
        }
        if (institution == null) {
            throw new ValidationFailedException(List.of(new FieldError("institutionId",
                    "select the institution with the X-Institution-Id header", "Required")));
        }
        DataScope.current(permission).requireWritable(Optional.of(institution));
        return institution;
    }
}
