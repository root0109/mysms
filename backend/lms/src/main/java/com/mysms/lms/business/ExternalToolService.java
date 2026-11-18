package com.mysms.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
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
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.UpdateMask;
import com.mysms.lms.api.model.ExternalTool;
import com.mysms.lms.api.model.ExternalToolInstitution;
import com.mysms.lms.api.model.ExternalToolInstitutionUpdate;
import com.mysms.lms.api.model.ExternalToolOption;
import com.mysms.lms.api.model.ExternalToolOptionUpdate;
import com.mysms.lms.api.model.ExternalToolRole;
import com.mysms.lms.api.model.ExternalToolRoleUpdate;
import com.mysms.lms.api.model.ExternalToolSetting;
import com.mysms.lms.api.model.ExternalToolSettingUpdate;
import com.mysms.lms.api.model.ExternalToolUpdate;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.ExternalToolEntity;
import com.mysms.lms.domain.ExternalToolInstitutionEntity;
import com.mysms.lms.domain.ExternalToolOptionEntity;
import com.mysms.lms.domain.ExternalToolRoleEntity;
import com.mysms.lms.domain.ExternalToolSettingEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.ExternalToolInstitutionMapper;
import com.mysms.lms.mapper.ExternalToolMapper;
import com.mysms.lms.mapper.ExternalToolOptionMapper;
import com.mysms.lms.mapper.ExternalToolRoleMapper;
import com.mysms.lms.mapper.ExternalToolSettingMapper;
import com.mysms.lms.repository.ExternalToolInstitutionRepository;
import com.mysms.lms.repository.ExternalToolOptionRepository;
import com.mysms.lms.repository.ExternalToolRepository;
import com.mysms.lms.repository.ExternalToolRoleRepository;
import com.mysms.lms.repository.ExternalToolSettingRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.external_tool and of the records that exist only as part of a ExternalTool
 * (ExternalToolInstitution, ExternalToolOption, ExternalToolRole, ExternalToolSetting): audit and domain event commit
 * in the same transaction as every change. Rows outside the caller's institutions are invisible; tenant-level rows
 * change only through a tenant-wide grant.
 */
@Service
public class ExternalToolService extends AppTransactionManager implements InitializingBean {

    public static final String AGGREGATE = ExternalToolEntity.TABLE;
    static final String PERMISSION_LIST = "lms.external_tool.read";
    static final String PERMISSION_GET = "lms.external_tool.read";
    static final String PERMISSION_CREATE = "lms.external_tool.create";
    static final String PERMISSION_UPDATE = "lms.external_tool.update";
    static final String PERMISSION_DELETE = "lms.external_tool.delete";
    /** The contract's ExternalToolUpdate fields, in apply order. */
    static final List<String> WRITABLE = List.of("title", "description", "isIncluded", "isPaid");

    public static final String EXTERNAL_TOOL_INSTITUTION_AGGREGATE = ExternalToolInstitutionEntity.TABLE;
    static final String EXTERNAL_TOOL_INSTITUTION_PERMISSION_LIST = "lms.external_tool_institution.read";
    static final String EXTERNAL_TOOL_INSTITUTION_PERMISSION_GET = "lms.external_tool_institution.read";
    static final String EXTERNAL_TOOL_INSTITUTION_PERMISSION_CREATE = "lms.external_tool_institution.create";
    static final String EXTERNAL_TOOL_INSTITUTION_PERMISSION_UPDATE = "lms.external_tool_institution.update";
    static final String EXTERNAL_TOOL_INSTITUTION_PERMISSION_DELETE = "lms.external_tool_institution.delete";
    /** The contract's ExternalToolInstitutionUpdate fields, in apply order. */
    static final List<String> EXTERNAL_TOOL_INSTITUTION_WRITABLE = List.of("institutionId");

    public static final String EXTERNAL_TOOL_OPTION_AGGREGATE = ExternalToolOptionEntity.TABLE;
    static final String EXTERNAL_TOOL_OPTION_PERMISSION_LIST = "lms.external_tool_option.read";
    static final String EXTERNAL_TOOL_OPTION_PERMISSION_GET = "lms.external_tool_option.read";
    static final String EXTERNAL_TOOL_OPTION_PERMISSION_CREATE = "lms.external_tool_option.create";
    static final String EXTERNAL_TOOL_OPTION_PERMISSION_UPDATE = "lms.external_tool_option.update";
    static final String EXTERNAL_TOOL_OPTION_PERMISSION_DELETE = "lms.external_tool_option.delete";
    /** The contract's ExternalToolOptionUpdate fields, in apply order. */
    static final List<String> EXTERNAL_TOOL_OPTION_WRITABLE = List.of("optionKind", "optionValue");

    public static final String EXTERNAL_TOOL_ROLE_AGGREGATE = ExternalToolRoleEntity.TABLE;
    static final String EXTERNAL_TOOL_ROLE_PERMISSION_LIST = "lms.external_tool_role.read";
    static final String EXTERNAL_TOOL_ROLE_PERMISSION_GET = "lms.external_tool_role.read";
    static final String EXTERNAL_TOOL_ROLE_PERMISSION_CREATE = "lms.external_tool_role.create";
    static final String EXTERNAL_TOOL_ROLE_PERMISSION_UPDATE = "lms.external_tool_role.update";
    static final String EXTERNAL_TOOL_ROLE_PERMISSION_DELETE = "lms.external_tool_role.delete";
    /** The contract's ExternalToolRoleUpdate fields, in apply order. */
    static final List<String> EXTERNAL_TOOL_ROLE_WRITABLE = List.of("roleId");

    public static final String EXTERNAL_TOOL_SETTING_AGGREGATE = ExternalToolSettingEntity.TABLE;
    static final String EXTERNAL_TOOL_SETTING_PERMISSION_LIST = "lms.external_tool_setting.read";
    static final String EXTERNAL_TOOL_SETTING_PERMISSION_GET = "lms.external_tool_setting.read";
    static final String EXTERNAL_TOOL_SETTING_PERMISSION_CREATE = "lms.external_tool_setting.create";
    static final String EXTERNAL_TOOL_SETTING_PERMISSION_UPDATE = "lms.external_tool_setting.update";
    static final String EXTERNAL_TOOL_SETTING_PERMISSION_DELETE = "lms.external_tool_setting.delete";
    /** The contract's ExternalToolSettingUpdate fields, in apply order. */
    static final List<String> EXTERNAL_TOOL_SETTING_WRITABLE = List.of("settingKey", "settingValue", "secretValue");

    /** The write-only secrets: never in a response, so not validated on the merged model. */
    private static final Set<String> EXTERNAL_TOOL_SETTING_SECRETS = Set.of("secretValue");
    private static final String EXTERNAL_TOOL_SETTING_SECRET_SECRET_VALUE_ENC = "lms.external_tool_setting.secret_value_enc";

    private final ExternalToolRepository repository;
    private final ExternalToolMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final ExternalToolInstitutionRepository externalToolInstitutionRepository;
    private final ExternalToolInstitutionMapper externalToolInstitutionMapper;
    private final ExternalToolOptionRepository externalToolOptionRepository;
    private final ExternalToolOptionMapper externalToolOptionMapper;
    private final ExternalToolRoleRepository externalToolRoleRepository;
    private final ExternalToolRoleMapper externalToolRoleMapper;
    private final ExternalToolSettingRepository externalToolSettingRepository;
    private final ExternalToolSettingMapper externalToolSettingMapper;
    private final FieldCipher cipher;

    public ExternalToolService(ExternalToolRepository repository,
            ExternalToolMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            ExternalToolInstitutionRepository externalToolInstitutionRepository,
            ExternalToolInstitutionMapper externalToolInstitutionMapper,
            ExternalToolOptionRepository externalToolOptionRepository,
            ExternalToolOptionMapper externalToolOptionMapper,
            ExternalToolRoleRepository externalToolRoleRepository,
            ExternalToolRoleMapper externalToolRoleMapper,
            ExternalToolSettingRepository externalToolSettingRepository,
            ExternalToolSettingMapper externalToolSettingMapper,
            FieldCipher cipher) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.externalToolInstitutionRepository = externalToolInstitutionRepository;
        this.externalToolInstitutionMapper = externalToolInstitutionMapper;
        this.externalToolOptionRepository = externalToolOptionRepository;
        this.externalToolOptionMapper = externalToolOptionMapper;
        this.externalToolRoleRepository = externalToolRoleRepository;
        this.externalToolRoleMapper = externalToolRoleMapper;
        this.externalToolSettingRepository = externalToolSettingRepository;
        this.externalToolSettingMapper = externalToolSettingMapper;
        this.cipher = cipher;
    }

    public ExternalTool getExternalTool(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<ExternalTool> listExternalTools(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ExternalToolRepository.SORT_FIELDS, ExternalToolRepository.DEFAULT_ORDER,
                ExternalToolRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ExternalToolRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<ExternalToolEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ExternalTool createExternalTool(ExternalTool request, boolean validateOnly) {
        validator.validate(request);
        ExternalToolEntity entity = mapper.toEntity(request);
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
            ExternalToolEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public ExternalTool updateExternalTool(UUID id, String ifMatch, ExternalToolUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            ExternalToolEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + LmsEvents.UPDATED, AGGREGATE, id, LmsEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteExternalTool(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            ExternalToolEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByExternalToolInstitutionExternalToolId(id)) {
                throw new InvalidStateException(
                        "The external tool is still referenced by lms.external_tool_institution rows; remove those "
                                + "first.");
            }
            if (repository.referencedByExternalToolLinkExternalToolId(id)) {
                throw new InvalidStateException(
                        "The external tool is still referenced by lms.external_tool_link rows; remove those first.");
            }
            if (repository.referencedByExternalToolOptionExternalToolId(id)) {
                throw new InvalidStateException(
                        "The external tool is still referenced by lms.external_tool_option rows; remove those first.");
            }
            if (repository.referencedByExternalToolRoleExternalToolId(id)) {
                throw new InvalidStateException(
                        "The external tool is still referenced by lms.external_tool_role rows; remove those first.");
            }
            if (repository.referencedByExternalToolSettingExternalToolId(id)) {
                throw new InvalidStateException(
                        "The external tool is still referenced by lms.external_tool_setting rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + LmsEvents.DELETED, AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ExternalToolEntity loadVisible(DataScope scope, UUID id) {
        ExternalToolEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOf(ExternalToolEntity entity) {
        return Optional.empty();
    }

    /** Applies the selected ExternalToolUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(ExternalToolEntity entity, ExternalToolUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "title" -> entity.setTitle(changes.getTitle());
                case "description" -> entity.setDescription(changes.getDescription());
                case "isIncluded" -> entity.setIsIncluded(changes.getIsIncluded());
                case "isPaid" -> entity.setIsPaid(changes.getIsPaid());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(ExternalToolEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsIncluded() == null) {
            errors.add(new FieldError("isIncluded", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsPaid() == null) {
            errors.add(new FieldError("isPaid", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(ExternalToolEntity entity) {

    }

    // ---- ExternalToolInstitution: a record of the ExternalTool aggregate ----

    public ExternalToolInstitution getExternalToolInstitution(UUID externalToolId, UUID id) {
        return doInReadTransaction(() -> {
            return externalToolInstitutionMapper.toDto(loadVisibleExternalToolInstitution(DataScope.current(
                    EXTERNAL_TOOL_INSTITUTION_PERMISSION_GET), externalToolId, id));
        });
    }

    public Page<ExternalToolInstitution> listExternalToolInstitutions(UUID externalToolId, Integer pageNumber,
            Integer pageSize, String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ExternalToolInstitutionRepository.SORT_FIELDS,
                        ExternalToolInstitutionRepository.DEFAULT_ORDER,
                ExternalToolInstitutionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ExternalToolInstitutionRepository.FILTER_FIELDS)
                .and(ExternalToolInstitutionRepository.ofParent(externalToolId))
                        .and(ExternalToolInstitutionRepository.scope(DataScope.current(
                                EXTERNAL_TOOL_INSTITUTION_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + externalToolId, clock);
        return doInReadTransaction(() -> {
            Page<ExternalToolInstitutionEntity> rows = externalToolInstitutionRepository.list(page, where);
            return rows.map(externalToolInstitutionMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ExternalToolInstitution createExternalToolInstitution(UUID externalToolId, ExternalToolInstitution request,
            boolean validateOnly) {
        validator.validate(request);
        ExternalToolInstitutionEntity entity = externalToolInstitutionMapper.toEntity(request);
        entity.setExternalToolId(externalToolId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(EXTERNAL_TOOL_INSTITUTION_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!externalToolInstitutionRepository.existsExternalToolId(entity.getExternalToolId())) {
                throw new ResourceNotFoundException("lms.external_tool", entity.getExternalToolId());
            }
            assertValidExternalToolInstitution(entity, true);
            scope.requireWritable(institutionOfExternalToolInstitution(entity));
            assertUniqueExternalToolInstitution(entity);
            if (validateOnly) {
                return externalToolInstitutionMapper.toDto(entity);
            }
            ExternalToolInstitutionEntity stored = externalToolInstitutionRepository.insert(entity);
            audit.recordCreate(EXTERNAL_TOOL_INSTITUTION_AGGREGATE, stored.getId());
            outbox.publish(EXTERNAL_TOOL_INSTITUTION_AGGREGATE + LmsEvents.CREATED, EXTERNAL_TOOL_INSTITUTION_AGGREGATE,
                    stored.getId(), LmsEvents.payload(stored.getId()));
            return externalToolInstitutionMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public ExternalToolInstitution updateExternalToolInstitution(UUID externalToolId, UUID id, String ifMatch,
            ExternalToolInstitutionUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, EXTERNAL_TOOL_INSTITUTION_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(EXTERNAL_TOOL_INSTITUTION_PERMISSION_UPDATE);
            ExternalToolInstitutionEntity entity = loadVisibleExternalToolInstitution(scope, externalToolId, id);
            scope.requireWritable(institutionOfExternalToolInstitution(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyExternalToolInstitution(entity, changes, fields);
            validator.validate(externalToolInstitutionMapper.toDto(entity));
            assertValidExternalToolInstitution(entity, false);
            scope.requireWritable(institutionOfExternalToolInstitution(entity));
            assertUniqueExternalToolInstitution(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return externalToolInstitutionMapper.toDto(entity);
            }
            if (externalToolInstitutionRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(EXTERNAL_TOOL_INSTITUTION_AGGREGATE, id, fields);
            outbox.publish(EXTERNAL_TOOL_INSTITUTION_AGGREGATE + LmsEvents.UPDATED, EXTERNAL_TOOL_INSTITUTION_AGGREGATE, id,
                    LmsEvents.payload(id));
            return externalToolInstitutionMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteExternalToolInstitution(UUID externalToolId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(EXTERNAL_TOOL_INSTITUTION_PERMISSION_DELETE);
            ExternalToolInstitutionEntity entity = loadVisibleExternalToolInstitution(scope, externalToolId, id);
            scope.requireWritable(institutionOfExternalToolInstitution(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (externalToolInstitutionRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(EXTERNAL_TOOL_INSTITUTION_AGGREGATE, id);
            outbox.publish(EXTERNAL_TOOL_INSTITUTION_AGGREGATE + LmsEvents.DELETED, EXTERNAL_TOOL_INSTITUTION_AGGREGATE, id,
                    LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ExternalToolInstitutionEntity loadVisibleExternalToolInstitution(DataScope scope, UUID externalToolId, UUID id) {
        ExternalToolInstitutionEntity entity = externalToolInstitutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(EXTERNAL_TOOL_INSTITUTION_AGGREGATE, id));
        if (!scope.canRead(institutionOfExternalToolInstitution(entity)) || !externalToolId.equals(entity.getExternalToolId())) {
            throw new ResourceNotFoundException(EXTERNAL_TOOL_INSTITUTION_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOfExternalToolInstitution(ExternalToolInstitutionEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected ExternalToolInstitutionUpdate fields; explicit per field so a mask can clear values. */
    private static void applyExternalToolInstitution(ExternalToolInstitutionEntity entity, ExternalToolInstitutionUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidExternalToolInstitution(ExternalToolInstitutionEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getExternalToolId() == null) {
            errors.add(new FieldError("externalToolId", "must not be null", "NotNull"));
        }
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getExternalToolId() != null
                && !externalToolInstitutionRepository.existsExternalToolId(entity.getExternalToolId())) {
            errors.add(new FieldError("externalToolId", "external tool does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueExternalToolInstitution(ExternalToolInstitutionEntity entity) {
        if (externalToolInstitutionRepository.takenExternalToolIdInstitutionId1(entity)) {
            throw new DuplicateResourceException(
                    "A external tool institution with the same external tool id and institution id already exists.");
        }
    }

    // ---- ExternalToolOption: a record of the ExternalTool aggregate ----

    public ExternalToolOption getExternalToolOption(UUID externalToolId, UUID id) {
        return doInReadTransaction(() -> {
            return externalToolOptionMapper.toDto(loadVisibleExternalToolOption(DataScope.current(
                    EXTERNAL_TOOL_OPTION_PERMISSION_GET), externalToolId, id));
        });
    }

    public Page<ExternalToolOption> listExternalToolOptions(UUID externalToolId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ExternalToolOptionRepository.SORT_FIELDS,
                        ExternalToolOptionRepository.DEFAULT_ORDER,
                ExternalToolOptionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ExternalToolOptionRepository.FILTER_FIELDS)
                .and(ExternalToolOptionRepository.ofParent(externalToolId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + externalToolId, clock);
        return doInReadTransaction(() -> {
            Page<ExternalToolOptionEntity> rows = externalToolOptionRepository.list(page, where);
            return rows.map(externalToolOptionMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ExternalToolOption createExternalToolOption(UUID externalToolId, ExternalToolOption request,
            boolean validateOnly) {
        validator.validate(request);
        ExternalToolOptionEntity entity = externalToolOptionMapper.toEntity(request);
        entity.setExternalToolId(externalToolId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(EXTERNAL_TOOL_OPTION_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!externalToolOptionRepository.existsExternalToolId(entity.getExternalToolId())) {
                throw new ResourceNotFoundException("lms.external_tool", entity.getExternalToolId());
            }
            assertValidExternalToolOption(entity, true);
            scope.requireWritable(institutionOfExternalToolOption(entity));
            assertUniqueExternalToolOption(entity);
            if (validateOnly) {
                return externalToolOptionMapper.toDto(entity);
            }
            ExternalToolOptionEntity stored = externalToolOptionRepository.insert(entity);
            audit.recordCreate(EXTERNAL_TOOL_OPTION_AGGREGATE, stored.getId());
            outbox.publish(EXTERNAL_TOOL_OPTION_AGGREGATE + LmsEvents.CREATED, EXTERNAL_TOOL_OPTION_AGGREGATE,
                    stored.getId(), LmsEvents.payload(stored.getId()));
            return externalToolOptionMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public ExternalToolOption updateExternalToolOption(UUID externalToolId, UUID id, String ifMatch,
            ExternalToolOptionUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, EXTERNAL_TOOL_OPTION_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(EXTERNAL_TOOL_OPTION_PERMISSION_UPDATE);
            ExternalToolOptionEntity entity = loadVisibleExternalToolOption(scope, externalToolId, id);
            scope.requireWritable(institutionOfExternalToolOption(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyExternalToolOption(entity, changes, fields);
            validator.validate(externalToolOptionMapper.toDto(entity));
            assertValidExternalToolOption(entity, false);
            scope.requireWritable(institutionOfExternalToolOption(entity));
            assertUniqueExternalToolOption(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return externalToolOptionMapper.toDto(entity);
            }
            if (externalToolOptionRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(EXTERNAL_TOOL_OPTION_AGGREGATE, id, fields);
            outbox.publish(EXTERNAL_TOOL_OPTION_AGGREGATE + LmsEvents.UPDATED, EXTERNAL_TOOL_OPTION_AGGREGATE, id,
                    LmsEvents.payload(id));
            return externalToolOptionMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteExternalToolOption(UUID externalToolId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(EXTERNAL_TOOL_OPTION_PERMISSION_DELETE);
            ExternalToolOptionEntity entity = loadVisibleExternalToolOption(scope, externalToolId, id);
            scope.requireWritable(institutionOfExternalToolOption(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (externalToolOptionRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(EXTERNAL_TOOL_OPTION_AGGREGATE, id);
            outbox.publish(EXTERNAL_TOOL_OPTION_AGGREGATE + LmsEvents.DELETED, EXTERNAL_TOOL_OPTION_AGGREGATE, id,
                    LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ExternalToolOptionEntity loadVisibleExternalToolOption(DataScope scope, UUID externalToolId, UUID id) {
        ExternalToolOptionEntity entity = externalToolOptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(EXTERNAL_TOOL_OPTION_AGGREGATE, id));
        if (!scope.canRead(institutionOfExternalToolOption(entity)) || !externalToolId.equals(entity.getExternalToolId())) {
            throw new ResourceNotFoundException(EXTERNAL_TOOL_OPTION_AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOfExternalToolOption(ExternalToolOptionEntity entity) {
        return Optional.empty();
    }

    /** Applies the selected ExternalToolOptionUpdate fields; explicit per field so a mask can clear values. */
    private static void applyExternalToolOption(ExternalToolOptionEntity entity, ExternalToolOptionUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "optionKind" -> entity.setOptionKind(Optional.ofNullable(changes.getOptionKind())
                        .map(v -> v.getValue()).orElse(null));
                case "optionValue" -> entity.setOptionValue(changes.getOptionValue());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidExternalToolOption(ExternalToolOptionEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getExternalToolId() == null) {
            errors.add(new FieldError("externalToolId", "must not be null", "NotNull"));
        }
        if (entity.getOptionKind() == null) {
            errors.add(new FieldError("optionKind", "must not be null", "NotNull"));
        }
        if (entity.getOptionValue() == null) {
            errors.add(new FieldError("optionValue", "must not be null", "NotNull"));
        }
        if (entity.getExternalToolId() != null
                && !externalToolOptionRepository.existsExternalToolId(entity.getExternalToolId())) {
            errors.add(new FieldError("externalToolId", "external tool does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueExternalToolOption(ExternalToolOptionEntity entity) {
        if (externalToolOptionRepository.takenExternalToolIdOptionKindOptionValue1(entity)) {
            throw new DuplicateResourceException(
                    "A external tool option with the same external tool id and option kind and option value already "
                            + "exists.");
        }
    }

    // ---- ExternalToolRole: a record of the ExternalTool aggregate ----

    public ExternalToolRole getExternalToolRole(UUID externalToolId, UUID id) {
        return doInReadTransaction(() -> {
            return externalToolRoleMapper.toDto(loadVisibleExternalToolRole(DataScope.current(
                    EXTERNAL_TOOL_ROLE_PERMISSION_GET), externalToolId, id));
        });
    }

    public Page<ExternalToolRole> listExternalToolRoles(UUID externalToolId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ExternalToolRoleRepository.SORT_FIELDS,
                        ExternalToolRoleRepository.DEFAULT_ORDER,
                ExternalToolRoleRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ExternalToolRoleRepository.FILTER_FIELDS)
                .and(ExternalToolRoleRepository.ofParent(externalToolId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + externalToolId, clock);
        return doInReadTransaction(() -> {
            Page<ExternalToolRoleEntity> rows = externalToolRoleRepository.list(page, where);
            return rows.map(externalToolRoleMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ExternalToolRole createExternalToolRole(UUID externalToolId, ExternalToolRole request,
            boolean validateOnly) {
        validator.validate(request);
        ExternalToolRoleEntity entity = externalToolRoleMapper.toEntity(request);
        entity.setExternalToolId(externalToolId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(EXTERNAL_TOOL_ROLE_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!externalToolRoleRepository.existsExternalToolId(entity.getExternalToolId())) {
                throw new ResourceNotFoundException("lms.external_tool", entity.getExternalToolId());
            }
            assertValidExternalToolRole(entity, true);
            scope.requireWritable(institutionOfExternalToolRole(entity));
            assertUniqueExternalToolRole(entity);
            if (validateOnly) {
                return externalToolRoleMapper.toDto(entity);
            }
            ExternalToolRoleEntity stored = externalToolRoleRepository.insert(entity);
            audit.recordCreate(EXTERNAL_TOOL_ROLE_AGGREGATE, stored.getId());
            outbox.publish(EXTERNAL_TOOL_ROLE_AGGREGATE + LmsEvents.CREATED, EXTERNAL_TOOL_ROLE_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return externalToolRoleMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public ExternalToolRole updateExternalToolRole(UUID externalToolId, UUID id, String ifMatch,
            ExternalToolRoleUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, EXTERNAL_TOOL_ROLE_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(EXTERNAL_TOOL_ROLE_PERMISSION_UPDATE);
            ExternalToolRoleEntity entity = loadVisibleExternalToolRole(scope, externalToolId, id);
            scope.requireWritable(institutionOfExternalToolRole(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyExternalToolRole(entity, changes, fields);
            validator.validate(externalToolRoleMapper.toDto(entity));
            assertValidExternalToolRole(entity, false);
            scope.requireWritable(institutionOfExternalToolRole(entity));
            assertUniqueExternalToolRole(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return externalToolRoleMapper.toDto(entity);
            }
            if (externalToolRoleRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(EXTERNAL_TOOL_ROLE_AGGREGATE, id, fields);
            outbox.publish(EXTERNAL_TOOL_ROLE_AGGREGATE + LmsEvents.UPDATED, EXTERNAL_TOOL_ROLE_AGGREGATE, id,
                    LmsEvents.payload(id));
            return externalToolRoleMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteExternalToolRole(UUID externalToolId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(EXTERNAL_TOOL_ROLE_PERMISSION_DELETE);
            ExternalToolRoleEntity entity = loadVisibleExternalToolRole(scope, externalToolId, id);
            scope.requireWritable(institutionOfExternalToolRole(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (externalToolRoleRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(EXTERNAL_TOOL_ROLE_AGGREGATE, id);
            outbox.publish(EXTERNAL_TOOL_ROLE_AGGREGATE + LmsEvents.DELETED, EXTERNAL_TOOL_ROLE_AGGREGATE, id,
                    LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ExternalToolRoleEntity loadVisibleExternalToolRole(DataScope scope, UUID externalToolId, UUID id) {
        ExternalToolRoleEntity entity = externalToolRoleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(EXTERNAL_TOOL_ROLE_AGGREGATE, id));
        if (!scope.canRead(institutionOfExternalToolRole(entity)) || !externalToolId.equals(entity.getExternalToolId())) {
            throw new ResourceNotFoundException(EXTERNAL_TOOL_ROLE_AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOfExternalToolRole(ExternalToolRoleEntity entity) {
        return Optional.empty();
    }

    /** Applies the selected ExternalToolRoleUpdate fields; explicit per field so a mask can clear values. */
    private static void applyExternalToolRole(ExternalToolRoleEntity entity, ExternalToolRoleUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "roleId" -> entity.setRoleId(changes.getRoleId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidExternalToolRole(ExternalToolRoleEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getExternalToolId() == null) {
            errors.add(new FieldError("externalToolId", "must not be null", "NotNull"));
        }
        if (entity.getRoleId() == null) {
            errors.add(new FieldError("roleId", "must not be null", "NotNull"));
        }
        if (entity.getExternalToolId() != null && !externalToolRoleRepository.existsExternalToolId(entity.getExternalToolId())) {
            errors.add(new FieldError("externalToolId", "external tool does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueExternalToolRole(ExternalToolRoleEntity entity) {
        if (externalToolRoleRepository.takenExternalToolIdRoleId1(entity)) {
            throw new DuplicateResourceException(
                    "A external tool role with the same external tool id and role id already exists.");
        }
    }

    // ---- ExternalToolSetting: a record of the ExternalTool aggregate ----

    /** Refuses to start without a master key: this resource stores secret columns. */
    @Override
    public void afterPropertiesSet() {
        cipher.requireConfigured();
    }

    public ExternalToolSetting getExternalToolSetting(UUID externalToolId, UUID id) {
        return doInReadTransaction(() -> {
            return dtoExternalToolSetting(loadVisibleExternalToolSetting(DataScope.current(
                    EXTERNAL_TOOL_SETTING_PERMISSION_GET), externalToolId, id));
        });
    }

    public Page<ExternalToolSetting> listExternalToolSettings(UUID externalToolId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ExternalToolSettingRepository.SORT_FIELDS,
                        ExternalToolSettingRepository.DEFAULT_ORDER,
                ExternalToolSettingRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ExternalToolSettingRepository.FILTER_FIELDS)
                .and(ExternalToolSettingRepository.ofParent(externalToolId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + externalToolId, clock);
        return doInReadTransaction(() -> {
            Page<ExternalToolSettingEntity> rows = externalToolSettingRepository.list(page, where);
            return rows.map(this::dtoExternalToolSetting);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ExternalToolSetting createExternalToolSetting(UUID externalToolId, ExternalToolSetting request,
            boolean validateOnly) {
        validator.validate(request);
        ExternalToolSettingEntity entity = externalToolSettingMapper.toEntity(request);
        entity.setExternalToolId(externalToolId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        sealSecretValueExternalToolSetting(entity, request.getSecretValue());
        DataScope scope = DataScope.current(EXTERNAL_TOOL_SETTING_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!externalToolSettingRepository.existsExternalToolId(entity.getExternalToolId())) {
                throw new ResourceNotFoundException("lms.external_tool", entity.getExternalToolId());
            }
            assertValidExternalToolSetting(entity, true);
            scope.requireWritable(institutionOfExternalToolSetting(entity));
            assertUniqueExternalToolSetting(entity);
            if (validateOnly) {
                return dtoExternalToolSetting(entity);
            }
            ExternalToolSettingEntity stored = externalToolSettingRepository.insert(entity);
            audit.recordCreate(EXTERNAL_TOOL_SETTING_AGGREGATE, stored.getId());
            outbox.publish(EXTERNAL_TOOL_SETTING_AGGREGATE + LmsEvents.CREATED, EXTERNAL_TOOL_SETTING_AGGREGATE,
                    stored.getId(), LmsEvents.payload(stored.getId()));
            return dtoExternalToolSetting(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public ExternalToolSetting updateExternalToolSetting(UUID externalToolId, UUID id, String ifMatch,
            ExternalToolSettingUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, EXTERNAL_TOOL_SETTING_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(EXTERNAL_TOOL_SETTING_PERMISSION_UPDATE);
            ExternalToolSettingEntity entity = loadVisibleExternalToolSetting(scope, externalToolId, id);
            scope.requireWritable(institutionOfExternalToolSetting(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyExternalToolSetting(entity, changes, fields);
            validator.validateExcept(dtoExternalToolSetting(entity), EXTERNAL_TOOL_SETTING_SECRETS);
            assertValidExternalToolSetting(entity, false);
            scope.requireWritable(institutionOfExternalToolSetting(entity));
            assertUniqueExternalToolSetting(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return dtoExternalToolSetting(entity);
            }
            if (externalToolSettingRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(EXTERNAL_TOOL_SETTING_AGGREGATE, id, fields);
            outbox.publish(EXTERNAL_TOOL_SETTING_AGGREGATE + LmsEvents.UPDATED, EXTERNAL_TOOL_SETTING_AGGREGATE, id,
                    LmsEvents.payload(id));
            return dtoExternalToolSetting(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteExternalToolSetting(UUID externalToolId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(EXTERNAL_TOOL_SETTING_PERMISSION_DELETE);
            ExternalToolSettingEntity entity = loadVisibleExternalToolSetting(scope, externalToolId, id);
            scope.requireWritable(institutionOfExternalToolSetting(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (externalToolSettingRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(EXTERNAL_TOOL_SETTING_AGGREGATE, id);
            outbox.publish(EXTERNAL_TOOL_SETTING_AGGREGATE + LmsEvents.DELETED, EXTERNAL_TOOL_SETTING_AGGREGATE, id,
                    LmsEvents.payload(id));
        });
    }

    /** Stores secretValue encrypted for the row's tenant and column; never the plaintext. */
    private void sealSecretValueExternalToolSetting(ExternalToolSettingEntity entity, String plaintext) {
        entity.setSecretValueEnc(cipher.encrypt(plaintext, entity.getTenantId(), EXTERNAL_TOOL_SETTING_SECRET_SECRET_VALUE_ENC));
    }

    /** The response DTO with each secret's display form ("••••1234"); the plaintext itself is never returned. */
    private ExternalToolSetting dtoExternalToolSetting(ExternalToolSettingEntity entity) {
        ExternalToolSetting dto = externalToolSettingMapper.toDto(entity);
        dto.setSecretValueMasked(
                cipher.maskStored(entity.getSecretValueEnc(), entity.getTenantId(),
                        EXTERNAL_TOOL_SETTING_SECRET_SECRET_VALUE_ENC));
        return dto;
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ExternalToolSettingEntity loadVisibleExternalToolSetting(DataScope scope, UUID externalToolId, UUID id) {
        ExternalToolSettingEntity entity = externalToolSettingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(EXTERNAL_TOOL_SETTING_AGGREGATE, id));
        if (!scope.canRead(institutionOfExternalToolSetting(entity)) || !externalToolId.equals(entity.getExternalToolId())) {
            throw new ResourceNotFoundException(EXTERNAL_TOOL_SETTING_AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOfExternalToolSetting(ExternalToolSettingEntity entity) {
        return Optional.empty();
    }

    /** Applies the selected ExternalToolSettingUpdate fields; explicit per field so a mask can clear values. */
    private void applyExternalToolSetting(ExternalToolSettingEntity entity, ExternalToolSettingUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "settingKey" -> entity.setSettingKey(changes.getSettingKey());
                case "settingValue" -> entity.setSettingValue(changes.getSettingValue());
                case "secretValue" -> sealSecretValueExternalToolSetting(entity, changes.getSecretValue());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidExternalToolSetting(ExternalToolSettingEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getExternalToolId() == null) {
            errors.add(new FieldError("externalToolId", "must not be null", "NotNull"));
        }
        if (entity.getSettingKey() == null) {
            errors.add(new FieldError("settingKey", "must not be null", "NotNull"));
        }
        if (entity.getExternalToolId() != null
                && !externalToolSettingRepository.existsExternalToolId(entity.getExternalToolId())) {
            errors.add(new FieldError("externalToolId", "external tool does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueExternalToolSetting(ExternalToolSettingEntity entity) {
        if (externalToolSettingRepository.takenExternalToolIdSettingKey1(entity)) {
            throw new DuplicateResourceException(
                    "A external tool setting with the same external tool id and setting key already exists.");
        }
    }
}
