package com.mysms.academics.business;

import com.mysms.academics.api.model.TeacherSubject;
import com.mysms.academics.api.model.TeacherSubjectUpdate;
import com.mysms.academics.config.AcademicsDatabaseConfig;
import com.mysms.academics.config.AcademicsEventsConfig;
import com.mysms.academics.domain.TeacherSubjectEntity;
import com.mysms.academics.events.AcademicsEvents;
import com.mysms.academics.mapper.TeacherSubjectMapper;
import com.mysms.academics.repository.TeacherSubjectRepository;
import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
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
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.Patches;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of academics.teacher_subject, the subjects a teacher is competent to teach (the teacher profile,
 * moved from hr's staff record in ARC-7): audit and domain event in the same transaction as every change; a row belongs
 * to its subject's institution, so rows outside the caller's institutions are invisible. The teacher is a logical
 * reference to hr.staff in another module (database/README "logical references"), not checked when written, like
 * teaching_assignment.staff_id; {@code hr.staff.deleted} archives the rows (AcademicsReferenceCleanup), where hr used
 * to refuse deleting a staff record that still had subjects.
 */
@Service
public class TeacherSubjectService extends AppTransactionManager {

    public static final String AGGREGATE = TeacherSubjectEntity.TABLE;
    static final String PERMISSION_READ = "academics.teacher_subject.read";
    static final String PERMISSION_CREATE = "academics.teacher_subject.create";
    static final String PERMISSION_UPDATE = "academics.teacher_subject.update";
    static final String PERMISSION_DELETE = "academics.teacher_subject.delete";

    private final TeacherSubjectRepository repository;
    private final TeacherSubjectMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public TeacherSubjectService(TeacherSubjectRepository repository, TeacherSubjectMapper mapper,
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

    public TeacherSubject getTeacherSubject(UUID id) {
        return doInReadTransaction(() -> mapper.toDto(loadVisible(DataScope.current(PERMISSION_READ), id)));
    }

    public Page<TeacherSubject> listTeacherSubjects(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, TeacherSubjectRepository.SORT_FIELDS,
                TeacherSubjectRepository.DEFAULT_ORDER, TeacherSubjectRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, TeacherSubjectRepository.FILTER_FIELDS)
                .and(TeacherSubjectRepository.scope(DataScope.current(PERMISSION_READ)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> repository.list(page, where).map(mapper::toDto));
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public TeacherSubject createTeacherSubject(TeacherSubject request, boolean validateOnly) {
        validator.validate(request);
        TeacherSubjectEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            assertValid(entity);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            TeacherSubjectEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + AcademicsEvents.CREATED, AGGREGATE, stored.getId(),
                    AcademicsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. A field the request leaves out is kept, a null one is cleared and a
     * value is set (§7.2). The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public TeacherSubject updateTeacherSubject(UUID id, String ifMatch, TeacherSubjectUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            TeacherSubjectEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = apply(entity, changes);
            validator.validate(mapper.toDto(entity));
            assertValid(entity);
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

    /** Soft delete under If-Match. */
    public void deleteTeacherSubject(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            TeacherSubjectEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + AcademicsEvents.DELETED, AGGREGATE, id, AcademicsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: a row outside the caller's scope is 404. */
    private TeacherSubjectEntity loadVisible(DataScope scope, UUID id) {
        TeacherSubjectEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row: its academics.subject's. */
    private Optional<UUID> institutionOf(TeacherSubjectEntity entity) {
        return repository.institutionOf(entity.getSubjectId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(TeacherSubjectEntity entity, TeacherSubjectUpdate changes) {
        Patches patch = new Patches();
        patch.apply("subjectId", changes.getSubjectId(), entity::setSubjectId);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(TeacherSubjectEntity entity) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getStaffId() == null) {
            errors.add(new FieldError("staffId", "must not be null", "NotNull"));
        }
        if (entity.getSubjectId() == null) {
            errors.add(new FieldError("subjectId", "must not be null", "NotNull"));
        } else if (!repository.existsSubjectId(entity.getSubjectId())) {
            errors.add(new FieldError("subjectId", "subject does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(TeacherSubjectEntity entity) {
        if (repository.takenStaffIdSubjectId1(entity)) {
            throw new DuplicateResourceException(
                    "A teacher subject with the same staff id and subject id already exists.");
        }
    }
}
