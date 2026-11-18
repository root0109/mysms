package com.mysms.finance.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Where the caller works on fees as staff: the institutions where they hold one of the permissions only the fee office
 * is given (generating dues, raising and posting invoices, taking and refunding payments), united. The read permissions
 * do not tell the office and families apart — a student's or guardian's role may carry them so the family can read its
 * own statement — so a row outside this reach is shown only when it belongs to the caller's own student, or (a payment)
 * when the caller paid it (personal data is owner-only).
 */
final class FinanceStaff {

    /** The permissions of the fee office. */
    static final String[] PERMISSIONS = {
        "finance.student_fee_due.generate_fee_dues",
        "finance.student_fee_due.update",
        "finance.invoice.create_invoice_from_dues",
        "finance.invoice.post_invoice",
        "finance.payment.record",
        "finance.payment.refund_payment",
    };

    private FinanceStaff() {
    }

    /** The united scope of the fee-office permissions the caller holds; no institution at all when they hold none. */
    static DataScope reach(AuthContext caller) {
        boolean all = false;
        boolean tenantWide = false;
        Set<UUID> institutions = new HashSet<>();
        for (String permission : PERMISSIONS) {
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

    /** Whether a row of this institution is the office's to read for the caller (read scope and reach both allow). */
    static boolean reaches(DataScope reach, DataScope read, Optional<UUID> institution) {
        return read.canRead(institution) && reach.canRead(institution) && (institution.isPresent() || reach.all());
    }
}
