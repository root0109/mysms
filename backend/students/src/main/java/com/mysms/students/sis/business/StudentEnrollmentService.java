package com.mysms.students.sis.business;

import com.mysms.authclient.DataScope;
import com.mysms.students.sis.api.model.StudentEnrollment;
import com.mysms.students.sis.api.model.StudentEnrollmentUpdate;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
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
import com.mysms.core.web.Patches;
import com.mysms.students.sis.config.SisDatabaseConfig;
import com.mysms.students.sis.config.SisEventsConfig;
import com.mysms.students.sis.domain.StudentEnrollmentEntity;
import com.mysms.students.sis.events.SisEvents;
import com.mysms.students.sis.mapper.StudentEnrollmentMapper;
import com.mysms.students.sis.repository.StudentEnrollmentRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of sis.student_enrollment: audit and domain event in the same transaction as every change; rows
 * outside the caller's institutions are invisible, and tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class StudentEnrollmentService extends AppTransactionManager {

    public static final String AGGREGATE = StudentEnrollmentEntity.TABLE;
    static final String PERMISSION_LIST = "sis.student_enrollment.read";
    static final String PERMISSION_GET = "sis.student_enrollment.read";
    static final String PERMISSION_CREATE = "sis.student_enrollment.create";
    static final String PERMISSION_UPDATE = "sis.student_enrollment.update";
    static final String PERMISSION_DELETE = "sis.student_enrollment.delete";
    static final String PERMISSION_LIST_CLASS_ROSTER = "sis.student_enrollment.list_class_roster";


    private final StudentEnrollmentRepository repository;
    private final StudentEnrollmentMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public StudentEnrollmentService(StudentEnrollmentRepository repository, StudentEnrollmentMapper mapper,
            @Qualifier(SisDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(SisDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(SisEventsConfig.OUTBOX) OutboxWriter outbox, @Qualifier(SisEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public StudentEnrollment getStudentEnrollment(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<StudentEnrollment> listStudentEnrollments(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, StudentEnrollmentRepository.SORT_FIELDS,
                        StudentEnrollmentRepository.DEFAULT_ORDER,
                StudentEnrollmentRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, StudentEnrollmentRepository.FILTER_FIELDS)
                .and(StudentEnrollmentRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<StudentEnrollmentEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /**
     * sis.ListClassRoster: the students placed in a class section now — its enrolled enrollments, oldest first — for
     * the holder of the roster permission (a teacher taking attendance needs the roster, not every enrollment of the
     * school). The institution scope applies as on every list: a class of another institution has an empty roster.
     */
    public Page<StudentEnrollment> listClassRoster(UUID classSectionId, Integer pageNumber, Integer pageSize,
            String cursor) {
        SortSpec sort = SortSpec.parse(null, StudentEnrollmentRepository.SORT_FIELDS,
                StudentEnrollmentRepository.DEFAULT_ORDER, StudentEnrollmentRepository.ID_COLUMN);
        SqlFragment where = StudentEnrollmentRepository.roster(classSectionId)
                .and(StudentEnrollmentRepository.scope(DataScope.current(PERMISSION_LIST_CLASS_ROSTER)));
        // the token is bound to this roster, so a cursor of one class cannot page another
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, "roster:" + classSectionId, clock);
        return doInReadTransaction(() -> {
            Page<StudentEnrollmentEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public StudentEnrollment createStudentEnrollment(StudentEnrollment request, boolean validateOnly) {
        validator.validate(request);
        StudentEnrollmentEntity entity = mapper.toEntity(request);
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
            StudentEnrollmentEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + SisEvents.CREATED, AGGREGATE, stored.getId(), SisEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. A field the request leaves out is kept, a null one is cleared and a
     * value is set (§7.2). The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public StudentEnrollment updateStudentEnrollment(UUID id, String ifMatch, StudentEnrollmentUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            StudentEnrollmentEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = apply(entity, changes);
            validator.validate(mapper.toDto(entity));
            assertValid(entity, false);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (fields.isEmpty()) {
                return mapper.toDto(entity);
            }
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            if (repository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(AGGREGATE, id, fields);
            outbox.publish(AGGREGATE + SisEvents.UPDATED, AGGREGATE, id, SisEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteStudentEnrollment(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            StudentEnrollmentEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByStudentEnrollmentPreviousEnrollmentId(id)) {
                throw new InvalidStateException(
                        "The student enrollment is still referenced by sis.student_enrollment rows; remove those "
                                + "first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + SisEvents.DELETED, AGGREGATE, id, SisEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private StudentEnrollmentEntity loadVisible(DataScope scope, UUID id) {
        StudentEnrollmentEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the sis.student it belongs to). */
    private Optional<UUID> institutionOf(StudentEnrollmentEntity entity) {
        return repository.institutionOf(entity.getStudentId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(StudentEnrollmentEntity entity, StudentEnrollmentUpdate changes) {
        Patches patch = new Patches();
        patch.apply("studentId", changes.getStudentId(), entity::setStudentId);
        patch.apply("academicYearId", changes.getAcademicYearId(), entity::setAcademicYearId);
        patch.apply("classSectionId", changes.getClassSectionId(), entity::setClassSectionId);
        patch.apply("rollNumber", changes.getRollNumber(), entity::setRollNumber);
        patch.apply("idCardNumber", changes.getIdCardNumber(), entity::setIdCardNumber);
        patch.apply("boardRegistrationNumber", changes.getBoardRegistrationNumber(), entity::setBoardRegistrationNumber);
        patch.apply("enrollmentStatus", changes.getEnrollmentStatus(),
                v -> entity.setEnrollmentStatus(v == null ? null : v.getValue()));
        patch.apply("resultStatus", changes.getResultStatus(), v -> entity.setResultStatus(v == null ? null : v.getValue()));
        patch.apply("enrolledOn", changes.getEnrolledOn(), entity::setEnrolledOn);
        patch.apply("leftOn", changes.getLeftOn(), entity::setLeftOn);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(StudentEnrollmentEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getStudentId() == null) {
            errors.add(new FieldError("studentId", "must not be null", "NotNull"));
        }
        if (entity.getAcademicYearId() == null) {
            errors.add(new FieldError("academicYearId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getEnrollmentStatus() == null) {
            errors.add(new FieldError("enrollmentStatus", "must not be null", "NotNull"));
        }
        if (!creating && entity.getResultStatus() == null) {
            errors.add(new FieldError("resultStatus", "must not be null", "NotNull"));
        }
        if (entity.getPreviousEnrollmentId() != null
                && !repository.existsPreviousEnrollmentId(entity.getPreviousEnrollmentId())) {
            errors.add(new FieldError("previousEnrollmentId", "student enrollment does not exist", "NOT_FOUND"));
        }
        if (entity.getStudentId() != null && !repository.existsStudentId(entity.getStudentId())) {
            errors.add(new FieldError("studentId", "student does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(StudentEnrollmentEntity entity) {

    }
}
