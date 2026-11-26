package com.mysms.learning.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
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
import com.mysms.core.web.Patches;
import com.mysms.learning.lms.api.model.Rubric;
import com.mysms.learning.lms.api.model.RubricCell;
import com.mysms.learning.lms.api.model.RubricCellUpdate;
import com.mysms.learning.lms.api.model.RubricCriterion;
import com.mysms.learning.lms.api.model.RubricCriterionUpdate;
import com.mysms.learning.lms.api.model.RubricLevel;
import com.mysms.learning.lms.api.model.RubricLevelUpdate;
import com.mysms.learning.lms.api.model.RubricUpdate;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.config.LmsEventsConfig;
import com.mysms.learning.lms.domain.RubricCellEntity;
import com.mysms.learning.lms.domain.RubricCriterionEntity;
import com.mysms.learning.lms.domain.RubricEntity;
import com.mysms.learning.lms.domain.RubricLevelEntity;
import com.mysms.learning.lms.events.LmsEvents;
import com.mysms.learning.lms.mapper.RubricCellMapper;
import com.mysms.learning.lms.mapper.RubricCriterionMapper;
import com.mysms.learning.lms.mapper.RubricLevelMapper;
import com.mysms.learning.lms.mapper.RubricMapper;
import com.mysms.learning.lms.repository.RubricCellRepository;
import com.mysms.learning.lms.repository.RubricCriterionRepository;
import com.mysms.learning.lms.repository.RubricLevelRepository;
import com.mysms.learning.lms.repository.RubricRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.rubric and of the records that exist only as part of a Rubric (RubricCell, RubricCriterion,
 * RubricLevel): audit and domain event commit in the same transaction as every change. Rows outside the caller's
 * institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class RubricService extends AppTransactionManager {

    public static final String AGGREGATE = RubricEntity.TABLE;
    static final String PERMISSION_LIST = "lms.rubric.read";
    static final String PERMISSION_GET = "lms.rubric.read";
    static final String PERMISSION_CREATE = "lms.rubric.create";
    static final String PERMISSION_UPDATE = "lms.rubric.update";
    static final String PERMISSION_DELETE = "lms.rubric.delete";

    public static final String RUBRIC_CELL_AGGREGATE = RubricCellEntity.TABLE;
    static final String RUBRIC_CELL_PERMISSION_LIST = "lms.rubric_cell.read";
    static final String RUBRIC_CELL_PERMISSION_GET = "lms.rubric_cell.read";
    static final String RUBRIC_CELL_PERMISSION_CREATE = "lms.rubric_cell.create";
    static final String RUBRIC_CELL_PERMISSION_UPDATE = "lms.rubric_cell.update";
    static final String RUBRIC_CELL_PERMISSION_DELETE = "lms.rubric_cell.delete";

    public static final String RUBRIC_CRITERION_AGGREGATE = RubricCriterionEntity.TABLE;
    static final String RUBRIC_CRITERION_PERMISSION_LIST = "lms.rubric_criterion.read";
    static final String RUBRIC_CRITERION_PERMISSION_GET = "lms.rubric_criterion.read";
    static final String RUBRIC_CRITERION_PERMISSION_CREATE = "lms.rubric_criterion.create";
    static final String RUBRIC_CRITERION_PERMISSION_UPDATE = "lms.rubric_criterion.update";
    static final String RUBRIC_CRITERION_PERMISSION_DELETE = "lms.rubric_criterion.delete";

    public static final String RUBRIC_LEVEL_AGGREGATE = RubricLevelEntity.TABLE;
    static final String RUBRIC_LEVEL_PERMISSION_LIST = "lms.rubric_level.read";
    static final String RUBRIC_LEVEL_PERMISSION_GET = "lms.rubric_level.read";
    static final String RUBRIC_LEVEL_PERMISSION_CREATE = "lms.rubric_level.create";
    static final String RUBRIC_LEVEL_PERMISSION_UPDATE = "lms.rubric_level.update";
    static final String RUBRIC_LEVEL_PERMISSION_DELETE = "lms.rubric_level.delete";

    private final RubricRepository repository;
    private final RubricMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final RubricCellRepository rubricCellRepository;
    private final RubricCellMapper rubricCellMapper;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricCriterionMapper rubricCriterionMapper;
    private final RubricLevelRepository rubricLevelRepository;
    private final RubricLevelMapper rubricLevelMapper;

    public RubricService(RubricRepository repository,
            RubricMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            RubricCellRepository rubricCellRepository,
            RubricCellMapper rubricCellMapper,
            RubricCriterionRepository rubricCriterionRepository,
            RubricCriterionMapper rubricCriterionMapper,
            RubricLevelRepository rubricLevelRepository,
            RubricLevelMapper rubricLevelMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.rubricCellRepository = rubricCellRepository;
        this.rubricCellMapper = rubricCellMapper;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.rubricCriterionMapper = rubricCriterionMapper;
        this.rubricLevelRepository = rubricLevelRepository;
        this.rubricLevelMapper = rubricLevelMapper;
    }

    public Rubric getRubric(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Rubric> listRubrics(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, RubricRepository.SORT_FIELDS, RubricRepository.DEFAULT_ORDER,
                RubricRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, RubricRepository.FILTER_FIELDS)
                .and(RubricRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<RubricEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Rubric createRubric(Rubric request, boolean validateOnly) {
        validator.validate(request);
        RubricEntity entity = mapper.toEntity(request);
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
            RubricEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public Rubric updateRubric(UUID id, String ifMatch, RubricUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            RubricEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = apply(entity, changes);
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
    public void deleteRubric(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            RubricEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByContentItemRubricId(id)) {
                throw new InvalidStateException(
                        "The rubric is still referenced by lms.content_item rows; remove those first.");
            }
            if (repository.referencedByRubricSupersededById(id)) {
                throw new InvalidStateException(
                        "The rubric is still referenced by lms.rubric rows; remove those first.");
            }
            if (repository.referencedByRubricCellRubricId(id)) {
                throw new InvalidStateException(
                        "The rubric is still referenced by lms.rubric_cell rows; remove those first.");
            }
            if (repository.referencedByRubricCriterionRubricId(id)) {
                throw new InvalidStateException(
                        "The rubric is still referenced by lms.rubric_criterion rows; remove those first.");
            }
            if (repository.referencedByRubricLevelRubricId(id)) {
                throw new InvalidStateException(
                        "The rubric is still referenced by lms.rubric_level rows; remove those first.");
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
    private RubricEntity loadVisible(DataScope scope, UUID id) {
        RubricEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(RubricEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(RubricEntity entity, RubricUpdate changes) {
        Patches patch = new Patches();
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("title", changes.getTitle(), entity::setTitle);
        patch.apply("isDefault", changes.getIsDefault(), entity::setIsDefault);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(RubricEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsDefault() == null) {
            errors.add(new FieldError("isDefault", "must not be null", "NotNull"));
        }
        if (entity.getSupersededById() != null && !repository.existsSupersededById(entity.getSupersededById())) {
            errors.add(new FieldError("supersededById", "rubric does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(RubricEntity entity) {

    }

    // ---- RubricCell: a record of the Rubric aggregate ----

    public RubricCell getRubricCell(UUID rubricId, UUID id) {
        return doInReadTransaction(() -> {
            return rubricCellMapper.toDto(loadVisibleRubricCell(DataScope.current(RUBRIC_CELL_PERMISSION_GET), rubricId, id));
        });
    }

    public Page<RubricCell> listRubricCells(UUID rubricId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, RubricCellRepository.SORT_FIELDS, RubricCellRepository.DEFAULT_ORDER,
                RubricCellRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, RubricCellRepository.FILTER_FIELDS)
                .and(RubricCellRepository.ofParent(rubricId))
                        .and(RubricCellRepository.scope(DataScope.current(RUBRIC_CELL_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + rubricId, clock);
        return doInReadTransaction(() -> {
            Page<RubricCellEntity> rows = rubricCellRepository.list(page, where);
            return rows.map(rubricCellMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public RubricCell createRubricCell(UUID rubricId, RubricCell request, boolean validateOnly) {
        validator.validate(request);
        RubricCellEntity entity = rubricCellMapper.toEntity(request);
        entity.setRubricId(rubricId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(RUBRIC_CELL_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!rubricCellRepository.existsRubricId(entity.getRubricId())) {
                throw new ResourceNotFoundException("lms.rubric", entity.getRubricId());
            }
            if (!scope.canRead(institutionOfRubricCell(entity))) {
                throw new ResourceNotFoundException("lms.rubric", entity.getRubricId());
            }
            assertValidRubricCell(entity, true);
            scope.requireWritable(institutionOfRubricCell(entity));
            assertUniqueRubricCell(entity);
            if (validateOnly) {
                return rubricCellMapper.toDto(entity);
            }
            RubricCellEntity stored = rubricCellRepository.insert(entity);
            audit.recordCreate(RUBRIC_CELL_AGGREGATE, stored.getId());
            outbox.publish(RUBRIC_CELL_AGGREGATE + LmsEvents.CREATED, RUBRIC_CELL_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return rubricCellMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public RubricCell updateRubricCell(UUID rubricId, UUID id, String ifMatch, RubricCellUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(RUBRIC_CELL_PERMISSION_UPDATE);
            RubricCellEntity entity = loadVisibleRubricCell(scope, rubricId, id);
            scope.requireWritable(institutionOfRubricCell(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyRubricCell(entity, changes);
            validator.validate(rubricCellMapper.toDto(entity));
            assertValidRubricCell(entity, false);
            scope.requireWritable(institutionOfRubricCell(entity));
            assertUniqueRubricCell(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return rubricCellMapper.toDto(entity);
            }
            if (rubricCellRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(RUBRIC_CELL_AGGREGATE, id, fields);
            outbox.publish(RUBRIC_CELL_AGGREGATE + LmsEvents.UPDATED, RUBRIC_CELL_AGGREGATE, id, LmsEvents.payload(id));
            return rubricCellMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteRubricCell(UUID rubricId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(RUBRIC_CELL_PERMISSION_DELETE);
            RubricCellEntity entity = loadVisibleRubricCell(scope, rubricId, id);
            scope.requireWritable(institutionOfRubricCell(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (rubricCellRepository.referencedByRubricAssessmentRubricCellId(id)) {
                throw new InvalidStateException(
                        "The rubric cell is still referenced by lms.rubric_assessment rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (rubricCellRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(RUBRIC_CELL_AGGREGATE, id);
            outbox.publish(RUBRIC_CELL_AGGREGATE + LmsEvents.DELETED, RUBRIC_CELL_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private RubricCellEntity loadVisibleRubricCell(DataScope scope, UUID rubricId, UUID id) {
        RubricCellEntity entity = rubricCellRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RUBRIC_CELL_AGGREGATE, id));
        if (!scope.canRead(institutionOfRubricCell(entity)) || !rubricId.equals(entity.getRubricId())) {
            throw new ResourceNotFoundException(RUBRIC_CELL_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.rubric the row belongs to. */
    private Optional<UUID> institutionOfRubricCell(RubricCellEntity entity) {
        return rubricCellRepository.institutionOf(entity.getRubricId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyRubricCell(RubricCellEntity entity, RubricCellUpdate changes) {
        Patches patch = new Patches();
        patch.apply("rubricCriterionId", changes.getRubricCriterionId(), entity::setRubricCriterionId);
        patch.apply("rubricLevelId", changes.getRubricLevelId(), entity::setRubricLevelId);
        patch.apply("description", changes.getDescription(), entity::setDescription);
        patch.apply("sortOrder", changes.getSortOrder(), entity::setSortOrder);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidRubricCell(RubricCellEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getRubricId() == null) {
            errors.add(new FieldError("rubricId", "must not be null", "NotNull"));
        }
        if (entity.getRubricCriterionId() == null) {
            errors.add(new FieldError("rubricCriterionId", "must not be null", "NotNull"));
        }
        if (entity.getRubricLevelId() == null) {
            errors.add(new FieldError("rubricLevelId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getRubricCriterionId() != null
                && !rubricCellRepository.existsRubricCriterionId(entity.getRubricCriterionId())) {
            errors.add(new FieldError("rubricCriterionId", "rubric criterion does not exist", "NOT_FOUND"));
        }
        if (entity.getRubricId() != null && !rubricCellRepository.existsRubricId(entity.getRubricId())) {
            errors.add(new FieldError("rubricId", "rubric does not exist", "NOT_FOUND"));
        }
        if (entity.getRubricLevelId() != null && !rubricCellRepository.existsRubricLevelId(entity.getRubricLevelId())) {
            errors.add(new FieldError("rubricLevelId", "rubric level does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueRubricCell(RubricCellEntity entity) {
        if (rubricCellRepository.takenRubricCriterionIdRubricLevelId1(entity)) {
            throw new DuplicateResourceException(
                    "A rubric cell with the same rubric criterion id and rubric level id already exists.");
        }
    }

    // ---- RubricCriterion: a record of the Rubric aggregate ----

    public RubricCriterion getRubricCriterion(UUID rubricId, UUID id) {
        return doInReadTransaction(() -> {
            return rubricCriterionMapper.toDto(loadVisibleRubricCriterion(DataScope.current(RUBRIC_CRITERION_PERMISSION_GET),
                    rubricId, id));
        });
    }

    public Page<RubricCriterion> listRubricCriterions(UUID rubricId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, RubricCriterionRepository.SORT_FIELDS, RubricCriterionRepository.DEFAULT_ORDER,
                RubricCriterionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, RubricCriterionRepository.FILTER_FIELDS)
                .and(RubricCriterionRepository.ofParent(rubricId))
                        .and(RubricCriterionRepository.scope(DataScope.current(RUBRIC_CRITERION_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + rubricId, clock);
        return doInReadTransaction(() -> {
            Page<RubricCriterionEntity> rows = rubricCriterionRepository.list(page, where);
            return rows.map(rubricCriterionMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public RubricCriterion createRubricCriterion(UUID rubricId, RubricCriterion request, boolean validateOnly) {
        validator.validate(request);
        RubricCriterionEntity entity = rubricCriterionMapper.toEntity(request);
        entity.setRubricId(rubricId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(RUBRIC_CRITERION_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!rubricCriterionRepository.existsRubricId(entity.getRubricId())) {
                throw new ResourceNotFoundException("lms.rubric", entity.getRubricId());
            }
            if (!scope.canRead(institutionOfRubricCriterion(entity))) {
                throw new ResourceNotFoundException("lms.rubric", entity.getRubricId());
            }
            assertValidRubricCriterion(entity, true);
            scope.requireWritable(institutionOfRubricCriterion(entity));
            assertUniqueRubricCriterion(entity);
            if (validateOnly) {
                return rubricCriterionMapper.toDto(entity);
            }
            RubricCriterionEntity stored = rubricCriterionRepository.insert(entity);
            audit.recordCreate(RUBRIC_CRITERION_AGGREGATE, stored.getId());
            outbox.publish(RUBRIC_CRITERION_AGGREGATE + LmsEvents.CREATED, RUBRIC_CRITERION_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return rubricCriterionMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public RubricCriterion updateRubricCriterion(UUID rubricId, UUID id, String ifMatch, RubricCriterionUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(RUBRIC_CRITERION_PERMISSION_UPDATE);
            RubricCriterionEntity entity = loadVisibleRubricCriterion(scope, rubricId, id);
            scope.requireWritable(institutionOfRubricCriterion(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyRubricCriterion(entity, changes);
            validator.validate(rubricCriterionMapper.toDto(entity));
            assertValidRubricCriterion(entity, false);
            scope.requireWritable(institutionOfRubricCriterion(entity));
            assertUniqueRubricCriterion(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return rubricCriterionMapper.toDto(entity);
            }
            if (rubricCriterionRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(RUBRIC_CRITERION_AGGREGATE, id, fields);
            outbox.publish(RUBRIC_CRITERION_AGGREGATE + LmsEvents.UPDATED, RUBRIC_CRITERION_AGGREGATE, id, LmsEvents.payload(id));
            return rubricCriterionMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteRubricCriterion(UUID rubricId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(RUBRIC_CRITERION_PERMISSION_DELETE);
            RubricCriterionEntity entity = loadVisibleRubricCriterion(scope, rubricId, id);
            scope.requireWritable(institutionOfRubricCriterion(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (rubricCriterionRepository.referencedByRubricCellRubricCriterionId(id)) {
                throw new InvalidStateException(
                        "The rubric criterion is still referenced by lms.rubric_cell rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (rubricCriterionRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(RUBRIC_CRITERION_AGGREGATE, id);
            outbox.publish(RUBRIC_CRITERION_AGGREGATE + LmsEvents.DELETED, RUBRIC_CRITERION_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private RubricCriterionEntity loadVisibleRubricCriterion(DataScope scope, UUID rubricId, UUID id) {
        RubricCriterionEntity entity = rubricCriterionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RUBRIC_CRITERION_AGGREGATE, id));
        if (!scope.canRead(institutionOfRubricCriterion(entity)) || !rubricId.equals(entity.getRubricId())) {
            throw new ResourceNotFoundException(RUBRIC_CRITERION_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.rubric the row belongs to. */
    private Optional<UUID> institutionOfRubricCriterion(RubricCriterionEntity entity) {
        return rubricCriterionRepository.institutionOf(entity.getRubricId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyRubricCriterion(RubricCriterionEntity entity, RubricCriterionUpdate changes) {
        Patches patch = new Patches();
        patch.apply("name", changes.getName(), entity::setName);
        patch.apply("groupName", changes.getGroupName(), entity::setGroupName);
        patch.apply("weightPercent", changes.getWeightPercent(), v -> entity.setWeightPercent(WireValues.decimal(v)));
        patch.apply("groupWeightPercent", changes.getGroupWeightPercent(),
                                                v -> entity.setGroupWeightPercent(WireValues.decimal(v)));
        patch.apply("sortOrder", changes.getSortOrder(), entity::setSortOrder);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidRubricCriterion(RubricCriterionEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getRubricId() == null) {
            errors.add(new FieldError("rubricId", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("weightPercent", WireValues.decimal(entity.getWeightPercent()), 6, 2, errors);
        WireValues.checkDecimal("groupWeightPercent", WireValues.decimal(entity.getGroupWeightPercent()), 6, 2, errors);
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getRubricId() != null && !rubricCriterionRepository.existsRubricId(entity.getRubricId())) {
            errors.add(new FieldError("rubricId", "rubric does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueRubricCriterion(RubricCriterionEntity entity) {

    }

    // ---- RubricLevel: a record of the Rubric aggregate ----

    public RubricLevel getRubricLevel(UUID rubricId, UUID id) {
        return doInReadTransaction(() -> {
            return rubricLevelMapper.toDto(loadVisibleRubricLevel(DataScope.current(RUBRIC_LEVEL_PERMISSION_GET), rubricId, id));
        });
    }

    public Page<RubricLevel> listRubricLevels(UUID rubricId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, RubricLevelRepository.SORT_FIELDS, RubricLevelRepository.DEFAULT_ORDER,
                RubricLevelRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, RubricLevelRepository.FILTER_FIELDS)
                .and(RubricLevelRepository.ofParent(rubricId))
                        .and(RubricLevelRepository.scope(DataScope.current(RUBRIC_LEVEL_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + rubricId, clock);
        return doInReadTransaction(() -> {
            Page<RubricLevelEntity> rows = rubricLevelRepository.list(page, where);
            return rows.map(rubricLevelMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public RubricLevel createRubricLevel(UUID rubricId, RubricLevel request, boolean validateOnly) {
        validator.validate(request);
        RubricLevelEntity entity = rubricLevelMapper.toEntity(request);
        entity.setRubricId(rubricId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(RUBRIC_LEVEL_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!rubricLevelRepository.existsRubricId(entity.getRubricId())) {
                throw new ResourceNotFoundException("lms.rubric", entity.getRubricId());
            }
            if (!scope.canRead(institutionOfRubricLevel(entity))) {
                throw new ResourceNotFoundException("lms.rubric", entity.getRubricId());
            }
            assertValidRubricLevel(entity, true);
            scope.requireWritable(institutionOfRubricLevel(entity));
            assertUniqueRubricLevel(entity);
            if (validateOnly) {
                return rubricLevelMapper.toDto(entity);
            }
            RubricLevelEntity stored = rubricLevelRepository.insert(entity);
            audit.recordCreate(RUBRIC_LEVEL_AGGREGATE, stored.getId());
            outbox.publish(RUBRIC_LEVEL_AGGREGATE + LmsEvents.CREATED, RUBRIC_LEVEL_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return rubricLevelMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public RubricLevel updateRubricLevel(UUID rubricId, UUID id, String ifMatch, RubricLevelUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(RUBRIC_LEVEL_PERMISSION_UPDATE);
            RubricLevelEntity entity = loadVisibleRubricLevel(scope, rubricId, id);
            scope.requireWritable(institutionOfRubricLevel(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyRubricLevel(entity, changes);
            validator.validate(rubricLevelMapper.toDto(entity));
            assertValidRubricLevel(entity, false);
            scope.requireWritable(institutionOfRubricLevel(entity));
            assertUniqueRubricLevel(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return rubricLevelMapper.toDto(entity);
            }
            if (rubricLevelRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(RUBRIC_LEVEL_AGGREGATE, id, fields);
            outbox.publish(RUBRIC_LEVEL_AGGREGATE + LmsEvents.UPDATED, RUBRIC_LEVEL_AGGREGATE, id, LmsEvents.payload(id));
            return rubricLevelMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteRubricLevel(UUID rubricId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(RUBRIC_LEVEL_PERMISSION_DELETE);
            RubricLevelEntity entity = loadVisibleRubricLevel(scope, rubricId, id);
            scope.requireWritable(institutionOfRubricLevel(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (rubricLevelRepository.referencedByRubricCellRubricLevelId(id)) {
                throw new InvalidStateException(
                        "The rubric level is still referenced by lms.rubric_cell rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (rubricLevelRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(RUBRIC_LEVEL_AGGREGATE, id);
            outbox.publish(RUBRIC_LEVEL_AGGREGATE + LmsEvents.DELETED, RUBRIC_LEVEL_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private RubricLevelEntity loadVisibleRubricLevel(DataScope scope, UUID rubricId, UUID id) {
        RubricLevelEntity entity = rubricLevelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RUBRIC_LEVEL_AGGREGATE, id));
        if (!scope.canRead(institutionOfRubricLevel(entity)) || !rubricId.equals(entity.getRubricId())) {
            throw new ResourceNotFoundException(RUBRIC_LEVEL_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.rubric the row belongs to. */
    private Optional<UUID> institutionOfRubricLevel(RubricLevelEntity entity) {
        return rubricLevelRepository.institutionOf(entity.getRubricId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyRubricLevel(RubricLevelEntity entity, RubricLevelUpdate changes) {
        Patches patch = new Patches();
        patch.apply("label", changes.getLabel(), entity::setLabel);
        patch.apply("description", changes.getDescription(), entity::setDescription);
        patch.apply("scorePercent", changes.getScorePercent(), v -> entity.setScorePercent(WireValues.decimal(v)));
        patch.apply("sortOrder", changes.getSortOrder(), entity::setSortOrder);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidRubricLevel(RubricLevelEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getRubricId() == null) {
            errors.add(new FieldError("rubricId", "must not be null", "NotNull"));
        }
        if (entity.getLabel() == null) {
            errors.add(new FieldError("label", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("scorePercent", WireValues.decimal(entity.getScorePercent()), 6, 2, errors);
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getRubricId() != null && !rubricLevelRepository.existsRubricId(entity.getRubricId())) {
            errors.add(new FieldError("rubricId", "rubric does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueRubricLevel(RubricLevelEntity entity) {

    }
}
