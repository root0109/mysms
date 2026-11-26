package com.mysms.identity.iam.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.identity.iam.domain.SettingDefinitionEntity;
import com.mysms.identity.iam.domain.SettingValueEntity;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SettingResolverTest {

    private static final UUID INSTITUTION = UUID.randomUUID();
    private static final UUID DEPARTMENT = UUID.randomUUID();
    private static final UUID SECTION = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();
    private static final SettingResolver.Chain CHAIN = new SettingResolver.Chain(INSTITUTION, DEPARTMENT, SECTION, USER);
    private static final SettingDefinitionEntity THEME = SettingRulesTest.definition("ui.theme", "choice",
            List.of("system", "light", "dark", "high_contrast"), null, null, SettingRules.CHAIN, true);

    private static SettingValueEntity value(String scope, UUID unit, String value, boolean locked) {
        return SettingValueEntity.builder().settingKey("ui.theme").scopeType(scope).scopeId(unit).value(value)
                .locked(locked).build();
    }

    @Test
    void theDefinitionDefaultAppliesWithoutValues() {
        SettingResolver.Effective effective = SettingResolver.resolve(THEME, List.of(), CHAIN);
        assertThat(effective.value()).isEqualTo("d");
        assertThat(effective.sourceScope()).isEqualTo("default");
        assertThat(effective.sourceScopeId()).isEmpty();
        assertThat(effective.locked()).isFalse();
        assertThat(effective.userEditable()).isTrue();
    }

    @Test
    void theMostSpecificScopeWinsWhateverTheOrder() {
        List<SettingValueEntity> values = List.of(
                value("user", USER, "high_contrast", false),
                value("tenant", null, "dark", false),
                value("department", DEPARTMENT, "light", false),
                value("institution", INSTITUTION, "system", false));
        SettingResolver.Effective effective = SettingResolver.resolve(THEME, values, CHAIN);
        assertThat(effective.value()).isEqualTo("high_contrast");
        assertThat(effective.sourceScope()).isEqualTo("user");
        assertThat(effective.sourceScopeId()).contains(USER);
        SettingResolver.Effective withoutUser = SettingResolver.resolve(THEME, values.subList(1, 4), CHAIN);
        assertThat(withoutUser.value()).isEqualTo("light");
        assertThat(withoutUser.sourceScope()).isEqualTo("department");
        assertThat(withoutUser.sourceScopeId()).contains(DEPARTMENT);
    }

    @Test
    void aBroaderLockWinsOverEverythingNarrower() {
        List<SettingValueEntity> values = List.of(
                value("tenant", null, "dark", false),
                value("institution", INSTITUTION, "light", true),
                value("department", DEPARTMENT, "system", true),
                value("user", USER, "high_contrast", false));
        SettingResolver.Effective effective = SettingResolver.resolve(THEME, values, CHAIN);
        assertThat(effective.value()).isEqualTo("light");
        assertThat(effective.sourceScope()).isEqualTo("institution");
        assertThat(effective.locked()).isTrue();
    }

    @Test
    void aTenantLockLocksEveryone() {
        SettingResolver.Effective effective = SettingResolver.resolve(THEME, List.of(
                value("tenant", null, "dark", true), value("user", USER, "light", false)), CHAIN);
        assertThat(effective.value()).isEqualTo("dark");
        assertThat(effective.locked()).isTrue();
    }

    @Test
    void valuesOutsideTheChainOrAtDisallowedScopesOrOfOtherKeysAreIgnored() {
        SettingDefinitionEntity tenantOnly = SettingRulesTest.definition("ui.theme", "choice", List.of("dark", "light"),
                null, null, List.of("tenant"), true);
        List<SettingValueEntity> values = List.of(
                value("tenant", null, "dark", false),
                value("department", UUID.randomUUID(), "light", false),
                value("user", USER, "light", false),
                SettingValueEntity.builder().settingKey("ui.density").scopeType("user").scopeId(USER).value("compact")
                        .build());
        SettingResolver.Effective effective = SettingResolver.resolve(tenantOnly, values, CHAIN);
        assertThat(effective.value()).isEqualTo("dark");
        assertThat(effective.sourceScope()).isEqualTo("tenant");
        assertThat(effective.userEditable()).isFalse();
        SettingResolver.Effective otherDepartment = SettingResolver.resolve(THEME, values.subList(1, 2), CHAIN);
        assertThat(otherDepartment.sourceScope()).isEqualTo("default");
        SettingResolver.Effective noUnits = SettingResolver.resolve(THEME, List.of(value("department", DEPARTMENT,
                "light", false)), new SettingResolver.Chain(null, null, null, USER));
        assertThat(noUnits.sourceScope()).isEqualTo("default");
    }

    @Test
    void aMalformedTenantValueWithAUnitIsIgnored() {
        SettingResolver.Effective effective = SettingResolver.resolve(THEME,
                List.of(value("tenant", UUID.randomUUID(), "dark", false)), CHAIN);
        assertThat(effective.sourceScope()).isEqualTo("default");
    }
}
