package com.mysms.privacy.business;

import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
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
import com.mysms.privacy.api.model.Purpose;
import com.mysms.privacy.api.model.PurposeUpdate;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.config.PrivacyEventsConfig;
import com.mysms.privacy.domain.TenantPurposeEntity;
import com.mysms.privacy.events.PrivacyEvents;
import com.mysms.privacy.mapper.PurposeMapper;
import com.mysms.privacy.repository.PurposeRepository;
import com.mysms.privacy.repository.PurposeView;
import com.mysms.privacy.repository.TenantPurposeRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The purpose aggregate: the platform's purpose catalogue (privacy.purpose) as one tenant has set it
 * (privacy.tenant_purpose). The catalogue is the same for every tenant; the setting is tenant-level data, so only
 * holders of privacy.tenant_purpose.* reach it at all and only a tenant-wide grant changes it.
 */
@Service
public class PurposeService extends AppTransactionManager {

    public static final String AGGREGATE = "privacy.purpose";
    public static final String TENANT_PURPOSE_AGGREGATE = TenantPurposeEntity.TABLE;
    static final String TENANT_PURPOSE_PERMISSION_UPDATE = "privacy.tenant_purpose.update";
    /** The contract's PurposeUpdate fields, in apply order. */
    static final List<String> TENANT_PURPOSE_WRITABLE = List.of("enabled", "maxScope", "noticeId");

    private static final String ACTIVE = "active";
    private static final String WIDTH = "width";
    private static final String PLATFORM = "platform";

    private final PurposeRepository repository;
    private final ConsentStateWriter stateWriter;
    private final PurposeMapper mapper;
    private final TenantPurposeRepository tenantPurposeRepository;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;

    public PurposeService(PurposeRepository repository,
            ConsentStateWriter stateWriter,
            PurposeMapper mapper,
            TenantPurposeRepository tenantPurposeRepository,
            @Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PrivacyDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PrivacyEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(PrivacyEventsConfig.AUDIT) AuditWriter audit,
            Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.stateWriter = stateWriter;
        this.mapper = mapper;
        this.tenantPurposeRepository = tenantPurposeRepository;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
    }

    public Purpose getPurpose(UUID id) {
        return doInReadTransaction(() -> mapper.toDto(loadPurpose(id)));
    }

    public Page<Purpose> listPurposes(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, PurposeRepository.SORT_FIELDS, PurposeRepository.DEFAULT_ORDER,
                PurposeRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PurposeRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> repository.list(page, where).map(mapper::toDto));
    }

    private PurposeView loadPurpose(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
    }

    // ---- TenantPurpose: a record of the Purpose aggregate ----

    /**
     * Partial update of the tenant's setting under If-Match ("0" while the tenant has never set the purpose: the first
     * write inserts the row). Switching a purpose on needs an active tenant-decided purpose and a notice whose active
     * version lists it; a width ceiling applies to width purposes only. A written setting has the purpose's consent
     * answers recomputed for every subject in the same transaction ({@link ConsentStateWriter#recomputePurpose}).
     */
    public Purpose updatePurpose(UUID id, String ifMatch, PurposeUpdate changes, String updateMask,
            boolean validateOnly) {
        DataScope.current(TENANT_PURPOSE_PERMISSION_UPDATE).requireWritable(Optional.empty());
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, TENANT_PURPOSE_WRITABLE);
        return doInWriteTransaction(() -> {
            PurposeView purpose = loadPurpose(id);
            if (purpose.rowVersion() != expected) {
                throw stale();
            }
            Optional<TenantPurposeEntity> stored = tenantPurposeRepository.findByPurposeId(id);
            TenantPurposeEntity setting = stored.orElseGet(() -> unsetTenantPurpose(id));
            applyTenantPurpose(setting, changes, fields);
            assertValidTenantPurpose(purpose, setting);
            if (stored.isPresent()) {
                setting.onUpdatingData(Actor.current(), clock);
            } else {
                setting.onCreatingData(Actor.current(), clock);
                setting.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
            }
            if (validateOnly) {
                return mapper.toDto(withSetting(purpose, setting));
            }
            writeTenantPurpose(setting, stored.isPresent(), expected, fields);
            stateWriter.recomputePurpose(purpose.code());
            return mapper.toDto(withSetting(purpose, setting));
        });
    }

    /** The first write inserts, later ones update under the row_version guard; a lost race is a stale ETag. */
    private void writeTenantPurpose(TenantPurposeEntity setting, boolean exists, int expected, List<String> fields) {
        if (exists) {
            if (tenantPurposeRepository.update(setting, expected) == 0) {
                throw stale();
            }
            setting.setRowVersion(expected + 1);
            audit.recordUpdate(TENANT_PURPOSE_AGGREGATE, setting.getId(), fields);
        } else {
            if (tenantPurposeRepository.insert(setting) == 0) {
                throw stale();
            }
            audit.recordCreate(TENANT_PURPOSE_AGGREGATE, setting.getId());
        }
        outbox.publish(TENANT_PURPOSE_AGGREGATE + PrivacyEvents.UPDATED, TENANT_PURPOSE_AGGREGATE, setting.getId(),
                PrivacyEvents.tenantPurposeUpdated(setting.getId(), setting.getPurposeId(),
                        Boolean.TRUE.equals(setting.getEnabled())));
    }

    /** What a purpose the tenant has never set means: switched off, no ceiling, no notice. */
    private static TenantPurposeEntity unsetTenantPurpose(UUID purposeId) {
        return TenantPurposeEntity.builder().purposeId(purposeId).enabled(false).build();
    }

    /** Applies the selected PurposeUpdate fields; explicit per field so a mask can clear values. */
    private static void applyTenantPurpose(TenantPurposeEntity setting, PurposeUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "enabled" -> setting.setEnabled(changes.getEnabled());
                case "maxScope" -> setting.setMaxScope(Optional.ofNullable(changes.getMaxScope())
                        .map(PurposeUpdate.MaxScopeEnum::getValue).orElse(null));
                case "noticeId" -> setting.setNoticeId(changes.getNoticeId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** The merged setting against the catalogue purpose and the tenant's notices (one 422 with every failing field). */
    private void assertValidTenantPurpose(PurposeView purpose, TenantPurposeEntity setting) {
        List<FieldError> errors = new ArrayList<>();
        if (setting.getEnabled() == null) {
            errors.add(new FieldError("enabled", "must not be null", "NotNull"));
        }
        if (setting.getMaxScope() != null && !WIDTH.equals(purpose.scopeKind())) {
            errors.add(new FieldError("maxScope",
                    "applies only to a purpose whose consents name a width", "MAX_SCOPE_NOT_APPLICABLE"));
        }
        boolean noticeExists = setting.getNoticeId() != null
                && tenantPurposeRepository.existsNotice(setting.getNoticeId());
        if (setting.getNoticeId() != null && !noticeExists) {
            errors.add(new FieldError("noticeId", "notice does not exist", "NOT_FOUND"));
        }
        if (Boolean.TRUE.equals(setting.getEnabled())) {
            if (PLATFORM.equals(purpose.fiduciary())) {
                errors.add(new FieldError("enabled",
                        "is decided by the platform and cannot be switched on by a school", "PURPOSE_PLATFORM_FIDUCIARY"));
            } else if (!ACTIVE.equals(purpose.status())) {
                errors.add(new FieldError("enabled", "must name an active purpose of the catalogue", "PURPOSE_NOT_ACTIVE"));
            }
            boolean listed = noticeExists
                    && tenantPurposeRepository.noticeListsPurpose(setting.getNoticeId(), purpose.code());
            if (setting.getNoticeId() == null || noticeExists && !listed) {
                errors.add(new FieldError("noticeId",
                        "must name a notice whose active version lists this purpose", "CONSENT_NOTICE_NOT_ACTIVE"));
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    /** The purpose as the merged setting makes it, for the answer. */
    private static PurposeView withSetting(PurposeView purpose, TenantPurposeEntity setting) {
        return new PurposeView(purpose.id(), purpose.code(), purpose.scopeKind(), purpose.fiduciary(),
                purpose.forbiddenForMinors(), purpose.majorityGrace(), purpose.defaultValidMonths(), purpose.status(),
                purpose.version(), Boolean.TRUE.equals(setting.getEnabled()), setting.getMaxScope(),
                setting.getNoticeId(), setting.getUpdatedAt(), setting.getRowVersion());
    }

    private static PreconditionFailedException stale() {
        return new PreconditionFailedException("If-Match does not match the current ETag.");
    }
}
