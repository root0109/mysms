package com.mysms.learning.lms.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.AuthFilter;
import com.mysms.authclient.Guest;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.paging.FieldSpec;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.repository.SettingsViewRepository;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Who reads a public operation: a member holding its permission reads the tenant's rows in their data scope; a guest,
 * or a signed-in user without the permission, reads published rows only, in the guest projection. Every guest read
 * passes {@link #requireTenant()}, which is where the school's {@code allowGuestAccess} setting is enforced: a school
 * that keeps guests out answers them 404, as if it had no such page.
 */
@Component
public class PublicReads extends AppTransactionManager {

    private final SettingsViewRepository settings;

    public PublicReads(SettingsViewRepository settings, @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions) {
        super(database, transactions);
        this.settings = settings;
    }

    /** True when the caller reads as a member: signed in and holding the permission. */
    static boolean member(String permission) {
        return AuthContext.current().map(c -> c.hasPermission(permission)).orElse(false);
    }

    /**
     * For a public reader: a guest must have named a school that lets guests in (404 otherwise); a signed-in caller
     * reads its own tenant.
     */
    void requireTenant() {
        if (requireSchoolNamed() && !guestsAllowed()) {
            throw new ResourceNotFoundException("school", AuthFilter.TENANT_CODE_HEADER);
        }
    }

    /**
     * The sign-in page's own check: a guest must have named a school (404 otherwise), whether or not it lets guests
     * browse. True when the caller is a guest.
     */
    static boolean requireSchoolNamed() {
        if (!Guest.isGuest()) {
            return false;
        }
        Guest.requireTenant();
        return true;
    }

    /**
     * The filter fields of a public list without the ones its guest projection leaves out: the public cannot learn a
     * hidden value by filtering on it.
     */
    static Map<String, FieldSpec> guestFields(Map<String, FieldSpec> fields, Set<String> hidden) {
        return fields.entrySet().stream().filter(e -> !hidden.contains(e.getKey()))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /** Whether the named school lets guests browse (its lms settings; the default when it saved none). */
    boolean guestsAllowed() {
        return doInReadTransaction(() -> settings.find().allowGuestAccess());
    }

    /**
     * Decides how the caller reads: true for a member; false for the public, after making sure the public reader
     * may read the school's public pages.
     */
    boolean asMember(String permission) {
        if (member(permission)) {
            return true;
        }
        requireTenant();
        return false;
    }
}
