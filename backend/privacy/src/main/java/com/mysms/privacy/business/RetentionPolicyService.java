package com.mysms.privacy.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.web.Etags;
import com.mysms.core.web.UpdateMask;
import com.mysms.privacy.api.model.RetentionPolicy;
import com.mysms.privacy.api.model.RetentionPolicyUpdate;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.config.PrivacyEventsConfig;
import com.mysms.privacy.domain.TenantRetentionPolicyEntity;
import com.mysms.privacy.events.PrivacyEvents;
import com.mysms.privacy.mapper.RetentionMapper;
import com.mysms.privacy.repository.RetentionPolicyRepository;
import com.mysms.privacy.repository.RetentionPolicyView;
import com.mysms.privacy.repository.TenantRetentionPolicyRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The retention policies (design §5.6, §10): the platform's catalogue (privacy.retention_policy, seeded by the
 * baseline, read only) with each tenant's own period and action (privacy.tenant_retention_policy, a record of this
 * aggregate; no row = the catalogue's). The pattern of purposes and the tenant's setting of each.
 *
 * <p>Only tenant administrators set a policy (a tenant-wide grant; refused while impersonating, 403), under If-Match
 * ("0" while never set: the first write inserts the row); a period below the policy's legal floor is refused (422
 * RETENTION_BELOW_LEGAL_FLOOR). The setting's audit record and event go in the same transaction; the daily run
 * ({@link RetentionRunService}) uses the period and action in force on its day.
 */
@Service
public class RetentionPolicyService extends AppTransactionManager {

    public static final String AGGREGATE = "privacy.retention_policy";
    public static final String TENANT_AGGREGATE = TenantRetentionPolicyEntity.TABLE;
    static final String PERMISSION_UPDATE = "privacy.retention_policy.update";
    /** The contract's RetentionPolicyUpdate fields, in apply order. */
    static final List<String> WRITABLE = List.of("periodDays", "action");

    private final RetentionPolicyRepository repository;
    private final TenantRetentionPolicyRepository settings;
    private final RetentionMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;
    private final RetentionJob retentionJob;

    public RetentionPolicyService(RetentionPolicyRepository repository,
            TenantRetentionPolicyRepository settings,
            RetentionMapper mapper,
            @Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PrivacyDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PrivacyEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(PrivacyEventsConfig.AUDIT) AuditWriter audit,
            Clock clock,
            RetentionJob retentionJob) {
        super(database, transactions);
        this.repository = repository;
        this.settings = settings;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
        this.retentionJob = retentionJob;
    }

    public RetentionPolicy getRetentionPolicy(UUID id) {
        return doInReadTransaction(() -> mapper.toDto(load(id)));
    }

    public Page<RetentionPolicy> listRetentionPolicies(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, RetentionPolicyRepository.SORT_FIELDS,
                RetentionPolicyRepository.DEFAULT_ORDER, RetentionPolicyRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, RetentionPolicyRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> repository.list(page, where).map(mapper::toDto));
    }

    /**
     * privacy.UpdateRetentionPolicy: the tenant's own period and action of the policy, never below its legal floor
     * (422), under If-Match ("0" while the tenant has never set it: the first write inserts the setting).
     */
    public RetentionPolicy updateRetentionPolicy(UUID id, String ifMatch, RetentionPolicyUpdate changes,
            String updateMask) {
        DataScope.current(PERMISSION_UPDATE).requireWritable(Optional.empty());
        if (AuthContext.require().impersonatorId().isPresent()) {
            throw new PermissionDeniedException("Retention policies cannot be set while acting as another user.");
        }
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            RetentionPolicyView policy = load(id);
            if (policy.rowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            Optional<TenantRetentionPolicyEntity> stored = settings.findByPolicyId(id);
            TenantRetentionPolicyEntity setting = stored.orElseGet(() -> TenantRetentionPolicyEntity.builder()
                    .policyId(id).periodDays(policy.periodDays()).action(policy.action()).build());
            apply(setting, changes, fields);
            requireValid(policy, setting);
            if (stored.isPresent()) {
                setting.onUpdatingData(Actor.current(), clock);
                if (settings.update(setting, expected) == 0) {
                    throw new PreconditionFailedException("If-Match does not match the current ETag.");
                }
                setting.setRowVersion(expected + 1);
                audit.recordUpdate(TENANT_AGGREGATE, setting.getId(), fields);
            } else {
                setting.onCreatingData(Actor.current(), clock);
                setting.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
                if (settings.insert(setting) == 0) {
                    throw new PreconditionFailedException("If-Match does not match the current ETag.");
                }
                audit.recordCreate(TENANT_AGGREGATE, setting.getId());
            }
            outbox.publish(TENANT_AGGREGATE + PrivacyEvents.UPDATED, TENANT_AGGREGATE, setting.getId(),
                    PrivacyEvents.retentionSetting(setting, policy.code()));
            // a tenant that sets a policy runs retention even before any person or student event reached privacy
            retentionJob.enrol(OffsetDateTime.now(clock));
            return mapper.toDto(new RetentionPolicyView(policy.id(), policy.code(), policy.categoryCode(),
                    policy.module(), policy.trigger(), setting.getPeriodDays(), policy.minimumPeriodDays(),
                    setting.getAction(), policy.notifyBeforeDays(), policy.legalReference(), true,
                    setting.getRowVersion()));
        });
    }

    /** Applies the selected RetentionPolicyUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(TenantRetentionPolicyEntity setting, RetentionPolicyUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "periodDays" -> setting.setPeriodDays(changes.getPeriodDays());
                case "action" -> {
                    RetentionPolicyUpdate.ActionEnum action = changes.getAction();
                    setting.setAction(action == null ? null : action.getValue());
                }
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** The merged setting against the policy (one 422 with every failing field): set, and never below the floor. */
    private static void requireValid(RetentionPolicyView policy, TenantRetentionPolicyEntity setting) {
        List<FieldError> errors = new ArrayList<>();
        if (setting.getPeriodDays() == null) {
            errors.add(new FieldError("periodDays", "must not be null", "NotNull"));
        } else if (setting.getPeriodDays() < policy.minimumPeriodDays()) {
            errors.add(new FieldError("periodDays", "must not be shorter than the policy's legal floor",
                    "RETENTION_BELOW_LEGAL_FLOOR"));
        }
        if (setting.getAction() == null) {
            errors.add(new FieldError("action", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private RetentionPolicyView load(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
    }
}
