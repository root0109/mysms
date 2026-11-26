package com.mysms.identity.iam.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.identity.iam.repository.RoleGrantRow;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** How the caller's chain picks its units from role grants. */
class MySettingsServiceTest {

    private static RoleGrantRow grant(String type, UUID unit, boolean primary) {
        return new RoleGrantRow("teacher", type, Optional.ofNullable(unit), primary);
    }

    @Test
    void thePrimaryGrantWinsThenTheOnlyOne() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        assertThat(MySettingsService.unit(List.of(grant("department", a, false), grant("department", b, true)),
                "department")).isEqualTo(b);
        assertThat(MySettingsService.unit(List.of(grant("department", a, false)), "department")).isEqualTo(a);
        assertThat(MySettingsService.unit(List.of(grant("department", a, false), grant("department", a, false)),
                "department")).isEqualTo(a);
    }

    @Test
    void anOpenChoiceOrNoGrantGivesNoUnit() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        assertThat(MySettingsService.unit(List.of(grant("department", a, false), grant("department", b, false)),
                "department")).isNull();
        assertThat(MySettingsService.unit(List.of(grant("tenant", null, false)), "department")).isNull();
        assertThat(MySettingsService.unit(List.of(), "class_section")).isNull();
    }
}
