package com.mysms.assessment.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Where the caller works as staff on a kind of student record: the institutions where they hold one of the permissions
 * only staff are given (grading, checking, reading marksheet lines), united. The standard read permission does not tell
 * staff and students apart — a student's role carries it in the student's own institution — so rows outside this reach
 * are shown only when they belong to the caller's own students.
 */
final class StaffReach {

    private StaffReach() {
    }

    /** The united scope of the staff permissions the caller holds; no institution at all when they hold none. */
    static DataScope of(AuthContext caller, String... staffPermissions) {
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

    /** Whether the reach covers every row the read scope shows, so no row needs an ownership check. */
    static boolean covers(DataScope reach, DataScope read) {
        return reach.all() || !read.all() && reach.institutions().containsAll(read.institutions());
    }
}
