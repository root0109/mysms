package com.mysms.iam.business;

import com.mysms.iam.domain.SettingDefinitionEntity;
import com.mysms.iam.domain.SettingValueEntity;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The effective value of a setting for one user (pure, no I/O): the most specific of user > class section >
 * primary department > institution > tenant > definition default, except that a value locked at a scope wins over
 * everything narrower. Values at scopes the definition does not allow (left over from an older catalogue) and values
 * of units outside the user's chain are ignored.
 */
public final class SettingResolver {

    private SettingResolver() {
    }

    /**
     * The units a user's values come from; null = the user has no such unit.
     *
     * @param institution  the institution the user acts in
     * @param department   the user's primary department
     * @param classSection the user's primary class section
     * @param user         the user account
     */
    public record Chain(UUID institution, UUID department, UUID classSection, UUID user) {

        UUID unit(String scopeType) {
            return switch (scopeType) {
                case SettingRules.INSTITUTION -> institution;
                case SettingRules.DEPARTMENT -> department;
                case SettingRules.CLASS_SECTION -> classSection;
                case SettingRules.USER -> user;
                default -> null;
            };
        }
    }

    /**
     * @param value         effective value
     * @param sourceScope   default, tenant, institution, department, class_section or user
     * @param sourceScopeId the unit the value comes from (empty for default and tenant)
     * @param locked        a broader scope locked the value: the user cannot change it
     * @param userEditable  the user may set it for themselves when not locked
     */
    public record Effective(String value, String sourceScope, Optional<UUID> sourceScopeId, boolean locked,
                            boolean userEditable) {
    }

    /** Resolves one definition against the values found for the user's chain (any order, any keys). */
    public static Effective resolve(SettingDefinitionEntity definition, Collection<SettingValueEntity> values,
                                    Chain chain) {
        Map<String, SettingValueEntity> byScope = new HashMap<>();
        for (SettingValueEntity value : values) {
            if (definition.getSettingKey().equals(value.getSettingKey())
                    && SettingRules.scopeAllowed(definition, value.getScopeType())
                    && belongsToChain(value, chain)) {
                byScope.put(value.getScopeType(), value);
            }
        }
        String effective = definition.getDefaultValue() == null ? "" : definition.getDefaultValue();
        String source = "default";
        Optional<UUID> sourceId = Optional.empty();
        boolean locked = false;
        for (String scope : SettingRules.CHAIN) {
            SettingValueEntity value = byScope.get(scope);
            if (value == null) {
                continue;
            }
            effective = value.getValue();
            source = scope;
            sourceId = Optional.ofNullable(value.getScopeId());
            if (value.isLocked() && !SettingRules.USER.equals(scope)) {
                locked = true;
                break;
            }
        }
        return new Effective(effective, source, sourceId, locked, SettingRules.userEditable(definition));
    }

    private static boolean belongsToChain(SettingValueEntity value, Chain chain) {
        if (SettingRules.TENANT.equals(value.getScopeType())) {
            return value.getScopeId() == null;
        }
        UUID unit = chain.unit(value.getScopeType());
        return unit != null && unit.equals(value.getScopeId());
    }
}
