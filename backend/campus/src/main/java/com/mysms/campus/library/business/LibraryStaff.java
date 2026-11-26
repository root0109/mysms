package com.mysms.campus.library.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Where the caller works as library staff: the institutions where they hold one of the permissions only staff are
 * given (issuing or checking in loans, issuing cards), united. A member's read permission does not tell staff and
 * borrowers apart, so rows outside this reach are shown only when they are the caller's own.
 */
final class LibraryStaff {

    private LibraryStaff() {
    }

    /** The united scope of the staff permissions the caller holds; no institution at all when they hold none. */
    static DataScope reach(String... staffPermissions) {
        AuthContext caller = AuthContext.require();
        boolean all = false;
        boolean tenantWide = false;
        Set<UUID> institutions = new HashSet<>();
        for (String permission : staffPermissions) {
            if (caller.hasPermission(permission)) {
                DataScope scope = DataScope.of(caller, permission);
                all |= scope.all();
                tenantWide |= scope.tenantWide();
                institutions.addAll(scope.institutions());
            }
        }
        return new DataScope(all, institutions, tenantWide);
    }

    /** Whether the caller staffs the institution (never a row of no institution). */
    static boolean staffs(Optional<UUID> institution, String... staffPermissions) {
        return institution.isPresent() && reach(staffPermissions).canRead(institution);
    }
}
