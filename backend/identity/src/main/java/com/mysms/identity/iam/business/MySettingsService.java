package com.mysms.identity.iam.business;

import com.mysms.authclient.AuthContext;
import com.mysms.identity.iam.api.model.EffectiveSetting;
import com.mysms.identity.iam.api.model.EffectiveSettingList;
import com.mysms.identity.iam.api.model.SetMySettingRequest;
import com.mysms.core.context.Actor;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ConflictException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.validation.DtoValidator;
import com.mysms.identity.iam.config.IamDatabaseConfig;
import com.mysms.identity.iam.config.IamEventsConfig;
import com.mysms.identity.iam.domain.SettingDefinitionEntity;
import com.mysms.identity.iam.domain.SettingValueEntity;
import com.mysms.identity.iam.events.IamEvents;
import com.mysms.identity.iam.repository.GrantRepository;
import com.mysms.identity.iam.repository.RoleGrantRow;
import com.mysms.identity.iam.repository.SettingDefinitionRepository;
import com.mysms.identity.iam.repository.SettingValueRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The caller's own settings (iam.ListMySettings, iam.SetMySetting, iam.ResetMySetting). The chain is built from the
 * caller's grants: the institution selected with X-Institution-Id (else the primary or only institution grant), the
 * primary (else only) department grant and the primary (else only) class-section grant. Users see and change only
 * the settings they may set themselves; administrator-only settings (and the ids of the units values come from)
 * are visible only to callers who administer settings at a scope they can be set at
 * ({@value SettingRules#ADMINISTER}).
 */
@Service
public class MySettingsService extends AppTransactionManager {

    public static final String SETTING_LOCKED = "SETTING_LOCKED";
    private static final String AGGREGATE = SettingValueEntity.TABLE;

    private final SettingDefinitionRepository definitions;
    private final SettingValueRepository values;
    private final GrantRepository grants;
    private final SettingScopes scopes;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public MySettingsService(SettingDefinitionRepository definitions, SettingValueRepository values,
                             GrantRepository grants, SettingScopes scopes,
                             @Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(IamDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
                             @Qualifier(IamEventsConfig.OUTBOX) OutboxWriter outbox,
                             @Qualifier(IamEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator,
                             Clock clock) {

        super(database, transactions);
        this.definitions = definitions;
        this.values = values;
        this.grants = grants;
        this.scopes = scopes;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public EffectiveSettingList list() {
        return doInReadTransaction(() -> {
            SettingResolver.Chain chain = chain();
            List<SettingValueEntity> found = values.findForChain(chain.institution(), chain.department(),
                    chain.classSection(), chain.user());
            java.util.Set<String> administered = scopes.administeredScopeTypes(SettingRules.ADMINISTER);
            return new EffectiveSettingList(definitions.findAll().stream()
                    .filter(definition -> SettingRules.visibleTo(definition, administered))
                    .map(definition -> dto(definition, SettingResolver.resolve(definition, found, chain),
                            administered.contains(SettingRules.TENANT)))
                    .toList());
        });
    }

    /** Sets the caller's user-level value: 404 unknown key, 422 not user-editable or invalid, 409 locked above. */
    public EffectiveSetting set(String key, SetMySettingRequest request) {
        validator.validate(request);
        return doInWriteTransaction(() -> {
            SettingDefinitionEntity definition = definition(key);
            if (!SettingRules.userEditable(definition)) {
                throw new ValidationFailedException(List.of(
                        new FieldError("settingKey", "users cannot change this setting themselves", "UserEditable")));
            }
            List<FieldError> errors = SettingRules.valueErrors(definition, request.getValue(), "value");
            if (!errors.isEmpty()) {
                throw new ValidationFailedException(errors);
            }
            SettingResolver.Chain chain = chain();
            List<SettingValueEntity> found = new ArrayList<>(values.findForChain(chain.institution(),
                    chain.department(), chain.classSection(), chain.user()));
            if (SettingResolver.resolve(definition, found, chain).locked()) {
                throw new ConflictException(SETTING_LOCKED, "A broader scope locks this setting.");
            }
            Optional<SettingValueEntity> own = values.findAtScope(SettingRules.USER, chain.user(), key);
            SettingValueEntity saved;
            if (own.isPresent()) {
                saved = own.get();
                int expected = saved.getRowVersion();
                saved.setValue(request.getValue());
                saved.onUpdatingData(Actor.current(), clock);
                if (values.update(saved, expected) == 0) {
                    throw new IllegalStateException("own setting value changed concurrently");
                }
                saved.setRowVersion(expected + 1);
                audit.recordUpdate(AGGREGATE, saved.getId(), List.of("value"));
                outbox.publish(AGGREGATE + IamEvents.UPDATED, AGGREGATE, saved.getId(), IamEvents.payload(saved.getId()));
            } else {
                saved = SettingValueEntity.builder().scopeType(SettingRules.USER).scopeId(chain.user()).settingKey(key)
                        .value(request.getValue()).locked(false).build();
                saved.onCreatingData(Actor.current(), clock);
                saved.setTenantId(AuthContext.require().tenantId().orElseThrow());
                values.insert(saved);
                audit.recordCreate(AGGREGATE, saved.getId());
                outbox.publish(AGGREGATE + IamEvents.CREATED, AGGREGATE, saved.getId(), IamEvents.payload(saved.getId()));
            }
            found.removeIf(v -> SettingRules.USER.equals(v.getScopeType()) && key.equals(v.getSettingKey()));
            found.add(saved);
            return dto(definition, SettingResolver.resolve(definition, found, chain),
                    scopes.administeredScopeTypes(SettingRules.ADMINISTER).contains(SettingRules.TENANT));
        });
    }

    /** Removes the caller's user-level value, if any (idempotent); 404 for a key not in the catalogue. */
    public void reset(String key) {
        runInWriteTransaction(() -> {
            definition(key);
            UUID user = AuthContext.require().userId();
            values.findAtScope(SettingRules.USER, user, key).ifPresent(own -> {
                int expected = own.getRowVersion();
                own.onDeletingData(Actor.current(), clock);
                if (values.softDelete(own, expected) == 0) {
                    throw new IllegalStateException("own setting value changed concurrently");
                }
                audit.recordDelete(AGGREGATE, own.getId());
                outbox.publish(AGGREGATE + IamEvents.DELETED, AGGREGATE, own.getId(), IamEvents.payload(own.getId()));
            });
        });
    }

    /** The definition, or 404 when it does not exist or the caller may not see it. Inside a transaction. */
    private SettingDefinitionEntity definition(String key) {
        java.util.Set<String> administered = scopes.administeredScopeTypes(SettingRules.ADMINISTER);
        return definitions.findByKey(key)
                .filter(definition -> SettingRules.visibleTo(definition, administered))
                .orElseThrow(() -> new ResourceNotFoundException(SettingDefinitionEntity.TABLE, key));
    }

    /** The caller's units, from the token's selected institution and the primary (or only) grants. */
    private SettingResolver.Chain chain() {
        AuthContext caller = AuthContext.require();
        List<RoleGrantRow> rows = grants.activeGrants(caller.userId(), LocalDate.ofInstant(clock.instant(),
                ZoneOffset.UTC));
        UUID institution = caller.activeInstitutionId().orElseGet(() -> unit(rows, SettingRules.INSTITUTION));
        return new SettingResolver.Chain(institution, unit(rows, SettingRules.DEPARTMENT),
                unit(rows, SettingRules.CLASS_SECTION), caller.userId());
    }

    /** The primary grant's unit of this type, else the only one; null when there is none or the choice is open. */
    static UUID unit(List<RoleGrantRow> rows, String scopeType) {
        List<RoleGrantRow> ofType = rows.stream().filter(r -> scopeType.equals(r.scopeType()) && r.scopeId().isPresent())
                .toList();
        Optional<UUID> primary = ofType.stream().filter(RoleGrantRow::primary).map(r -> r.scopeId().get()).findFirst();
        if (primary.isPresent()) {
            return primary.get();
        }
        List<UUID> distinct = ofType.stream().map(r -> r.scopeId().get()).distinct().toList();
        return distinct.size() == 1 ? distinct.get(0) : null;
    }

    /** Which unit a value comes from is administration data: only tenant-wide setting administrators see its id. */
    private static EffectiveSetting dto(SettingDefinitionEntity definition, SettingResolver.Effective effective,
                                        boolean administrator) {
        EffectiveSetting dto = new EffectiveSetting(definition.getSettingKey(), effective.value(),
                EffectiveSetting.SourceScopeEnum.fromValue(effective.sourceScope()), effective.locked(),
                effective.userEditable());
        if (administrator || SettingRules.USER.equals(effective.sourceScope())) {
            dto.setSourceScopeId(effective.sourceScopeId().orElse(null));
        }
        return dto;
    }
}
