package com.mysms.academics.business;

import com.mysms.academics.config.AcademicsDatabaseConfig;
import com.mysms.academics.config.AcademicsEventsConfig;
import com.mysms.academics.domain.TeachingAssignmentEntity;
import com.mysms.academics.events.AcademicsEvents;
import com.mysms.academics.mapper.TeachingAssignmentMapper;
import com.mysms.academics.repository.TeachingAssignmentRepository;
import com.mysms.authclient.DataScope;
import com.mysms.academics.api.model.TeachingAssignment;
import com.mysms.academics.api.model.TeachingAssignmentUpdate;
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
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.UpdateMask;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of academics.teaching_assignment (JAVA-CODING-RULES §4): one plain class, explicit methods,
 * transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 */
@Service
public class TeachingAssignmentService extends AppTransactionManager {

    public static final String AGGREGATE = TeachingAssignmentEntity.TABLE;
    static final String PERMISSION_LIST = "academics.teaching_assignment.read";
    static final String PERMISSION_GET = "academics.teaching_assignment.read";
    static final String PERMISSION_CREATE = "academics.teaching_assignment.create";
    static final String PERMISSION_UPDATE = "academics.teaching_assignment.update";
    static final String PERMISSION_DELETE = "academics.teaching_assignment.delete";
    /** The contract's TeachingAssignmentUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("classSubjectId", "staffId", "periodsPerWeek", "periodsRemaining", "teacherRole", "isActive");


    private final TeachingAssignmentRepository repository;
    private final TeachingAssignmentMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public TeachingAssignmentService(TeachingAssignmentRepository repository, TeachingAssignmentMapper mapper,
            @Qualifier(AcademicsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AcademicsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AcademicsEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(AcademicsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public TeachingAssignment getTeachingAssignment(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<TeachingAssignment> listTeachingAssignments(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, TeachingAssignmentRepository.SORT_FIELDS,
                        TeachingAssignmentRepository.DEFAULT_ORDER,
                TeachingAssignmentRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, TeachingAssignmentRepository.FILTER_FIELDS)
                .and(TeachingAssignmentRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<TeachingAssignmentEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public TeachingAssignment createTeachingAssignment(TeachingAssignment request, boolean validateOnly) {
        validator.validate(request);
        TeachingAssignmentEntity entity = mapper.toEntity(request);
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
            TeachingAssignmentEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + AcademicsEvents.CREATED, AGGREGATE, stored.getId(),
                    AcademicsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public TeachingAssignment updateTeachingAssignment(UUID id, String ifMatch, TeachingAssignmentUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            TeachingAssignmentEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + AcademicsEvents.UPDATED, AGGREGATE, id, AcademicsEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteTeachingAssignment(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            TeachingAssignmentEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByLessonPlanTeachingAssignmentId(id)) {
                throw new InvalidStateException(
                        "The teaching assignment is still referenced by academics.lesson_plan rows; remove those "
                                + "first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + AcademicsEvents.DELETED, AGGREGATE, id, AcademicsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private TeachingAssignmentEntity loadVisible(DataScope scope, UUID id) {
        TeachingAssignmentEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /**
     * The institution that owns the row (the institution of the academics.class_subject → academics.class_section it
     * belongs to).
     */
    private Optional<UUID> institutionOf(TeachingAssignmentEntity entity) {
        return repository.institutionOf(entity.getClassSubjectId());
    }

    /**
     * Applies the selected TeachingAssignmentUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void apply(TeachingAssignmentEntity entity, TeachingAssignmentUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "classSubjectId" -> entity.setClassSubjectId(changes.getClassSubjectId());
                case "staffId" -> entity.setStaffId(changes.getStaffId());
                case "periodsPerWeek" -> entity.setPeriodsPerWeek(changes.getPeriodsPerWeek());
                case "periodsRemaining" -> entity.setPeriodsRemaining(changes.getPeriodsRemaining());
                case "teacherRole" -> entity.setTeacherRole(changes.getTeacherRole());
                case "isActive" -> entity.setIsActive(changes.getIsActive());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(TeachingAssignmentEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getClassSubjectId() == null) {
            errors.add(new FieldError("classSubjectId", "must not be null", "NotNull"));
        }
        if (entity.getStaffId() == null) {
            errors.add(new FieldError("staffId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (entity.getClassSubjectId() != null && !repository.existsClassSubjectId(entity.getClassSubjectId())) {
            errors.add(new FieldError("classSubjectId", "class subject does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(TeachingAssignmentEntity entity) {
        if (repository.takenClassSubjectIdStaffId1(entity)) {
            throw new DuplicateResourceException(
                    "A teaching assignment with the same class subject id and staff id already exists.");
        }
    }
}
