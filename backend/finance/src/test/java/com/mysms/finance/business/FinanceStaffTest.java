package com.mysms.finance.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Where a caller works as the fee office, and when that covers what they may read. */
class FinanceStaffTest {

    private final UUID school = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();

    @Test
    void theReachIsWhereTheCallerHoldsAFeeOfficePermission() {
        AuthContext office = caller(Set.of(school), Set.of("finance.payment.record", "finance.invoice.read"));
        DataScope reach = FinanceStaff.reach(office);
        assertThat(reach.institutions()).containsExactly(school);
        assertThat(FinanceStaff.covers(reach, new DataScope(false, Set.of(school), false))).isTrue();
        assertThat(FinanceStaff.covers(reach, new DataScope(false, Set.of(school, other), false))).isFalse();
        assertThat(FinanceStaff.reaches(reach, DataScope.of(office, "finance.invoice.read"), Optional.of(school)))
                .isTrue();
        assertThat(FinanceStaff.reaches(reach, DataScope.of(office, "finance.invoice.read"), Optional.empty()))
                .as("a tenant-level row needs a tenant-wide grant").isFalse();

        AuthContext family = caller(Set.of(school), Set.of("finance.invoice.read", "finance.payment.read"));
        DataScope none = FinanceStaff.reach(family);
        assertThat(none.all()).isFalse();
        assertThat(none.institutions()).isEmpty();
        assertThat(FinanceStaff.reaches(none, DataScope.of(family, "finance.invoice.read"), Optional.of(school)))
                .isFalse();
    }

    private static AuthContext caller(Set<UUID> institutions, Set<String> permissions) {
        return new AuthContext(UUID.randomUUID(), Optional.of(UUID.randomUUID()), institutions, Optional.empty(),
                permissions, "jti", Optional.empty(), false, Optional.empty(), Optional.empty(), false, Set.of());
    }
}
