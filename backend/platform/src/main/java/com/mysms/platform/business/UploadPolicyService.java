package com.mysms.platform.business;

import com.mysms.authclient.DataScope;
import com.mysms.platform.api.model.UploadPolicy;
import com.mysms.platform.api.model.UploadPolicyUpdate;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.UpdateMask;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.config.PlatformEventsConfig;
import com.mysms.platform.domain.UploadPolicyEntity;
import com.mysms.platform.events.PlatformEvents;
import com.mysms.platform.mapper.UploadPolicyMapper;
import com.mysms.platform.repository.UploadPolicyRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of platform.upload_policy: audit and domain event in the same transaction as every change; rows
 * outside the caller's institutions are invisible, and tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class UploadPolicyService extends AppTransactionManager {

    public static final String AGGREGATE = UploadPolicyEntity.TABLE;
    static final String PERMISSION_LIST = "platform.upload_policy.read";
    static final String PERMISSION_GET = "platform.upload_policy.read";
    static final String PERMISSION_CREATE = "platform.upload_policy.create";
    static final String PERMISSION_UPDATE = "platform.upload_policy.update";
    static final String PERMISSION_DELETE = "platform.upload_policy.delete";
    /** The contract's UploadPolicyUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("roleId", "moduleCode", "operationCode", "maxSizeBytes", "allowedExtensions");


    private final UploadPolicyRepository repository;
    private final UploadPolicyMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public UploadPolicyService(UploadPolicyRepository repository, UploadPolicyMapper mapper,
            @Qualifier(PlatformDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PlatformDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PlatformEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(PlatformEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public UploadPolicy getUploadPolicy(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<UploadPolicy> listUploadPolicys(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, UploadPolicyRepository.SORT_FIELDS, UploadPolicyRepository.DEFAULT_ORDER,
                UploadPolicyRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, UploadPolicyRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<UploadPolicyEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public UploadPolicy createUploadPolicy(UploadPolicy request, boolean validateOnly) {
        validator.validate(request);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkInt64("maxSizeBytes", request.getMaxSizeBytes(), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        UploadPolicyEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            assertValid(entity, true);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            UploadPolicyEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + PlatformEvents.CREATED, AGGREGATE, stored.getId(),
                    PlatformEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public UploadPolicy updateUploadPolicy(UUID id, String ifMatch, UploadPolicyUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkInt64("maxSizeBytes", changes.getMaxSizeBytes(), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            UploadPolicyEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            apply(entity, changes, fields);
            validator.validate(mapper.toDto(entity));
            assertValid(entity, false);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            if (repository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(AGGREGATE, id, fields);
            outbox.publish(AGGREGATE + PlatformEvents.UPDATED, AGGREGATE, id, PlatformEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteUploadPolicy(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            UploadPolicyEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + PlatformEvents.DELETED, AGGREGATE, id, PlatformEvents.payload(id));
        });
    }

    /**
     * The upload limits of a feature for everyone (the policy naming the operation wins over the feature-wide one),
     * when the tenant set any. Policies for one role are not applied yet: the token carries no role ids.
     */
    public Optional<UploadPolicyEntity> limitsFor(String moduleCode, String operationCode) {
        return doInReadTransaction(() -> repository.forFeature(moduleCode, operationCode));
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private UploadPolicyEntity loadVisible(DataScope scope, UUID id) {
        UploadPolicyEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOf(UploadPolicyEntity entity) {
        return Optional.empty();
    }

    /**
     * Applies the selected UploadPolicyUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void apply(UploadPolicyEntity entity, UploadPolicyUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "roleId" -> entity.setRoleId(changes.getRoleId());
                case "moduleCode" -> entity.setModuleCode(changes.getModuleCode());
                case "operationCode" -> entity.setOperationCode(changes.getOperationCode());
                case "maxSizeBytes" -> entity.setMaxSizeBytes(WireValues.int64(changes.getMaxSizeBytes()));
                case "allowedExtensions" -> entity.setAllowedExtensions(changes.getAllowedExtensions());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(UploadPolicyEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getModuleCode() == null) {
            errors.add(new FieldError("moduleCode", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(UploadPolicyEntity entity) {

    }
}
