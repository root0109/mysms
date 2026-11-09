package com.mysms.iam.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.error.FieldError;
import com.mysms.iam.domain.SettingDefinitionEntity;
import java.util.List;
import org.junit.jupiter.api.Test;

class SettingRulesTest {

    static SettingDefinitionEntity definition(String key, String type, List<String> allowed, Integer min, Integer max,
                                              List<String> scopes, boolean userEditable) {
        return SettingDefinitionEntity.builder().settingKey(key).valueType(type).allowedValues(allowed).minValue(min)
                .maxValue(max).allowedScopes(scopes).userEditable(userEditable).defaultValue("d").build();
    }

    private static List<String> codes(List<FieldError> errors) {
        return errors.stream().map(FieldError::code).toList();
    }

    @Test
    void booleansAreTrueOrFalseOnly() {
        SettingDefinitionEntity flag = definition("ui.reducedMotion", "boolean", null, null, null, SettingRules.CHAIN, true);
        assertThat(SettingRules.valueErrors(flag, "true", "value")).isEmpty();
        assertThat(SettingRules.valueErrors(flag, "false", "value")).isEmpty();
        assertThat(codes(SettingRules.valueErrors(flag, "TRUE", "value"))).containsExactly("Pattern");
        assertThat(codes(SettingRules.valueErrors(flag, "1", "value"))).containsExactly("Pattern");
    }

    @Test
    void integersAreCanonicalAndInRange() {
        SettingDefinitionEntity scale = definition("ui.textScale", "integer", null, 100, 150, SettingRules.CHAIN, true);
        assertThat(SettingRules.valueErrors(scale, "100", "value")).isEmpty();
        assertThat(SettingRules.valueErrors(scale, "150", "value")).isEmpty();
        assertThat(codes(SettingRules.valueErrors(scale, "151", "value"))).containsExactly("Range");
        assertThat(codes(SettingRules.valueErrors(scale, "99", "value"))).containsExactly("Range");
        assertThat(codes(SettingRules.valueErrors(scale, "0120", "value"))).containsExactly("Pattern");
        assertThat(codes(SettingRules.valueErrors(scale, "+120", "value"))).containsExactly("Pattern");
        assertThat(codes(SettingRules.valueErrors(scale, "big", "value"))).containsExactly("Type");
        SettingDefinitionEntity open = definition("x.count", "integer", null, null, null, SettingRules.CHAIN, true);
        assertThat(SettingRules.valueErrors(open, "-5", "value")).isEmpty();
    }

    @Test
    void choicesComeFromTheCatalogue() {
        SettingDefinitionEntity theme = definition("ui.theme", "choice", List.of("light", "dark"), null, null,
                SettingRules.CHAIN, true);
        assertThat(SettingRules.valueErrors(theme, "dark", "value")).isEmpty();
        List<FieldError> errors = SettingRules.valueErrors(theme, "purple", "value");
        assertThat(codes(errors)).containsExactly("Choice");
        assertThat(errors.get(0).message()).contains("light, dark");
        assertThat(errors.get(0).field()).isEqualTo("value");
    }

    @Test
    void stringsWithAShapeOfTheirOwn() {
        SettingDefinitionEntity zone = definition("ui.timezone", "string", null, null, null, SettingRules.CHAIN, true);
        assertThat(SettingRules.valueErrors(zone, "Asia/Kolkata", "value")).isEmpty();
        assertThat(codes(SettingRules.valueErrors(zone, "Mars/Olympus", "value"))).containsExactly("Pattern");
        SettingDefinitionEntity landing = definition("ui.landingScreen", "string", null, null, null, SettingRules.CHAIN,
                true);
        assertThat(SettingRules.valueErrors(landing, "", "value")).isEmpty();
        assertThat(SettingRules.valueErrors(landing, "ACC-07", "value")).isEmpty();
        assertThat(codes(SettingRules.valueErrors(landing, "javascript:alert(1)", "value"))).containsExactly("Pattern");
        SettingDefinitionEntity free = definition("x.note", "string", null, null, null, SettingRules.CHAIN, true);
        assertThat(SettingRules.valueErrors(free, "anything at all", "value")).isEmpty();
    }

    @Test
    void nullOverlongControlCharactersAndUnknownTypesAreRejected() {
        SettingDefinitionEntity free = definition("x.note", "string", null, null, null, SettingRules.CHAIN, true);
        assertThat(codes(SettingRules.valueErrors(free, null, "value"))).containsExactly("NotNull");
        assertThat(codes(SettingRules.valueErrors(free, "x".repeat(501), "value"))).containsExactly("Size");
        assertThat(codes(SettingRules.valueErrors(free, "a\nb", "value"))).containsExactly("Pattern");
        SettingDefinitionEntity odd = definition("x.odd", "json", null, null, null, SettingRules.CHAIN, true);
        assertThat(codes(SettingRules.valueErrors(odd, "{}", "value"))).containsExactly("Type");
    }

    @Test
    void scopesAndUserEditability() {
        SettingDefinitionEntity adminOnly = definition("security.sessionTimeoutMinutes", "integer", null, 5, 480,
                List.of("tenant", "institution"), false);
        assertThat(SettingRules.scopeAllowed(adminOnly, "tenant")).isTrue();
        assertThat(SettingRules.scopeAllowed(adminOnly, "department")).isFalse();
        assertThat(SettingRules.userEditable(adminOnly)).isFalse();
        SettingDefinitionEntity flaggedButNoUserScope = definition("ui.timezone", "string", null, null, null,
                List.of("tenant"), true);
        assertThat(SettingRules.userEditable(flaggedButNoUserScope)).isFalse();
        SettingDefinitionEntity theme = definition("ui.theme", "choice", List.of("dark"), null, null, SettingRules.CHAIN,
                true);
        assertThat(SettingRules.userEditable(theme)).isTrue();
    }

    @Test
    void administratorOnlySettingsAreVisibleOnlyToAdministratorsOfTheirScopes() {
        SettingDefinitionEntity timeout = definition("security.sessionTimeoutMinutes", "integer", null, 5, 480,
                List.of("tenant", "institution"), false);
        SettingDefinitionEntity theme = definition("ui.theme", "choice", List.of("dark"), null, null, SettingRules.CHAIN,
                true);
        assertThat(SettingRules.visibleTo(theme, java.util.Set.of())).isTrue();
        assertThat(SettingRules.visibleTo(timeout, java.util.Set.of())).as("a normal user").isFalse();
        assertThat(SettingRules.visibleTo(timeout, java.util.Set.of("department"))).as("a department head").isFalse();
        assertThat(SettingRules.visibleTo(timeout, java.util.Set.of("institution"))).isTrue();
        assertThat(SettingRules.visibleTo(timeout, java.util.Set.copyOf(SettingRules.CHAIN))).isTrue();
    }
}
