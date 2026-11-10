package com.mysms.sis.business;

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
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.UpdateMask;
import com.mysms.sis.api.model.ProgramEnrollment;
import com.mysms.sis.api.model.ProgramEnrollmentSubject;
import com.mysms.sis.api.model.ProgramEnrollmentSubjectUpdate;
import com.mysms.sis.api.model.ProgramEnrollmentUpdate;
import com.mysms.sis.config.SisDatabaseConfig;
import com.mysms.sis.config.SisEventsConfig;
import com.mysms.sis.domain.ProgramEnrollmentEntity;
import com.mysms.sis.domain.ProgramEnrollmentSubjectEntity;
import com.mysms.sis.events.SisEvents;
import com.mysms.sis.mapper.ProgramEnrollmentMapper;
import com.mysms.sis.mapper.ProgramEnrollmentSubjectMapper;
import com.mysms.sis.repository.ProgramEnrollmentRepository;
import com.mysms.sis.repository.ProgramEnrollmentSubjectRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of sis.program_enrollment (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a ProgramEnrollment
 * are served here too — ProgramEnrollmentSubject.
 */
@Service
public class ProgramEnrollmentService extends AppTransactionManager {

    public static final String AGGREGATE = ProgramEnrollmentEntity.TABLE;
    static final String PERMISSION_LIST = "sis.program_enrollment.read";
    static final String PERMISSION_GET = "sis.program_enrollment.read";
    static final String PERMISSION_CREATE = "sis.program_enrollment.create";
    static final String PERMISSION_UPDATE = "sis.program_enrollment.update";
    static final String PERMISSION_DELETE = "sis.program_enrollment.delete";
    /** The contract's ProgramEnrollmentUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("studentId", "courseId", "batchId", "academicYearId", "academicTermId", "rollNumber", "status");

    public static final String PROGRAM_ENROLLMENT_SUBJECT_AGGREGATE = ProgramEnrollmentSubjectEntity.TABLE;
    static final String PROGRAM_ENROLLMENT_SUBJECT_PERMISSION_LIST = "sis.program_enrollment_subject.read";
    static final String PROGRAM_ENROLLMENT_SUBJECT_PERMISSION_GET = "sis.program_enrollment_subject.read";
    static final String PROGRAM_ENROLLMENT_SUBJECT_PERMISSION_CREATE = "sis.program_enrollment_subject.create";
    static final String PROGRAM_ENROLLMENT_SUBJECT_PERMISSION_UPDATE = "sis.program_enrollment_subject.update";
    static final String PROGRAM_ENROLLMENT_SUBJECT_PERMISSION_DELETE = "sis.program_enrollment_subject.delete";
    /** The contract's ProgramEnrollmentSubjectUpdate fields, in apply order. */
    static final List<String> PROGRAM_ENROLLMENT_SUBJECT_WRITABLE = List.of("subjectId");

    private final ProgramEnrollmentRepository repository;
    private final ProgramEnrollmentMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final ProgramEnrollmentSubjectRepository programEnrollmentSubjectRepository;
    private final ProgramEnrollmentSubjectMapper programEnrollmentSubjectMapper;

    public ProgramEnrollmentService(ProgramEnrollmentRepository repository,
            ProgramEnrollmentMapper mapper,
            @Qualifier(SisDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(SisDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(SisEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(SisEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            ProgramEnrollmentSubjectRepository programEnrollmentSubjectRepository,
            ProgramEnrollmentSubjectMapper programEnrollmentSubjectMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.programEnrollmentSubjectRepository = programEnrollmentSubjectRepository;
        this.programEnrollmentSubjectMapper = programEnrollmentSubjectMapper;
    }

    public ProgramEnrollment getProgramEnrollment(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<ProgramEnrollment> listProgramEnrollments(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ProgramEnrollmentRepository.SORT_FIELDS,
                        ProgramEnrollmentRepository.DEFAULT_ORDER,
                ProgramEnrollmentRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ProgramEnrollmentRepository.FILTER_FIELDS)
                .and(ProgramEnrollmentRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<ProgramEnrollmentEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ProgramEnrollment createProgramEnrollment(ProgramEnrollment request, boolean validateOnly) {
        validator.validate(request);
        ProgramEnrollmentEntity entity = mapper.toEntity(request);
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
            ProgramEnrollmentEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + SisEvents.CREATED, AGGREGATE, stored.getId(), SisEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public ProgramEnrollment updateProgramEnrollment(UUID id, String ifMatch, ProgramEnrollmentUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            ProgramEnrollmentEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + SisEvents.UPDATED, AGGREGATE, id, SisEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteProgramEnrollment(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            ProgramEnrollmentEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByProgramEnrollmentSubjectProgramEnrollmentId(id)) {
                throw new InvalidStateException(
                        "The program enrollment is still referenced by sis.program_enrollment_subject rows; remove "
                                + "those first.");
            }
            if (repository.referencedBySubjectRegistrationProgramEnrollmentId(id)) {
                throw new InvalidStateException(
                        "The program enrollment is still referenced by sis.subject_registration rows; remove those "
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
    private ProgramEnrollmentEntity loadVisible(DataScope scope, UUID id) {
        ProgramEnrollmentEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the sis.student it belongs to). */
    private Optional<UUID> institutionOf(ProgramEnrollmentEntity entity) {
        return repository.institutionOf(entity.getStudentId());
    }

    /**
     * Applies the selected ProgramEnrollmentUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void apply(ProgramEnrollmentEntity entity, ProgramEnrollmentUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "studentId" -> entity.setStudentId(changes.getStudentId());
                case "courseId" -> entity.setCourseId(changes.getCourseId());
                case "batchId" -> entity.setBatchId(changes.getBatchId());
                case "academicYearId" -> entity.setAcademicYearId(changes.getAcademicYearId());
                case "academicTermId" -> entity.setAcademicTermId(changes.getAcademicTermId());
                case "rollNumber" -> entity.setRollNumber(changes.getRollNumber());
                case "status" -> entity.setStatus(Optional.ofNullable(changes.getStatus()).map(v -> v.getValue())
                        .orElse(null));
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(ProgramEnrollmentEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getStudentId() == null) {
            errors.add(new FieldError("studentId", "must not be null", "NotNull"));
        }
        if (entity.getCourseId() == null) {
            errors.add(new FieldError("courseId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getStatus() == null) {
            errors.add(new FieldError("status", "must not be null", "NotNull"));
        }
        if (entity.getStudentId() != null && !repository.existsStudentId(entity.getStudentId())) {
            errors.add(new FieldError("studentId", "student does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(ProgramEnrollmentEntity entity) {
        if (entity.getBatchId() != null && repository.takenStudentIdCourseIdBatchId1(entity)) {
            throw new DuplicateResourceException(
                    "A program enrollment with the same student id and course id and batch id already exists.");
        }
    }

    // ---- ProgramEnrollmentSubject: a record of the ProgramEnrollment aggregate ----

    public ProgramEnrollmentSubject getProgramEnrollmentSubject(UUID programEnrollmentId, UUID id) {
        return doInReadTransaction(() -> {
            return programEnrollmentSubjectMapper.toDto(loadVisibleProgramEnrollmentSubject(DataScope.current(
                    PROGRAM_ENROLLMENT_SUBJECT_PERMISSION_GET), programEnrollmentId, id));
        });
    }

    public Page<ProgramEnrollmentSubject> listProgramEnrollmentSubjects(UUID programEnrollmentId, Integer pageNumber,
            Integer pageSize, String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ProgramEnrollmentSubjectRepository.SORT_FIELDS,
                        ProgramEnrollmentSubjectRepository.DEFAULT_ORDER,
                ProgramEnrollmentSubjectRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ProgramEnrollmentSubjectRepository.FILTER_FIELDS)
                .and(ProgramEnrollmentSubjectRepository.ofParent(programEnrollmentId))
                        .and(ProgramEnrollmentSubjectRepository.scope(DataScope.current(
                                PROGRAM_ENROLLMENT_SUBJECT_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + programEnrollmentId, clock);
        return doInReadTransaction(() -> {
            Page<ProgramEnrollmentSubjectEntity> rows = programEnrollmentSubjectRepository.list(page, where);
            return rows.map(programEnrollmentSubjectMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ProgramEnrollmentSubject createProgramEnrollmentSubject(UUID programEnrollmentId,
            ProgramEnrollmentSubject request, boolean validateOnly) {
        validator.validate(request);
        ProgramEnrollmentSubjectEntity entity = programEnrollmentSubjectMapper.toEntity(request);
        entity.setProgramEnrollmentId(programEnrollmentId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(PROGRAM_ENROLLMENT_SUBJECT_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!programEnrollmentSubjectRepository.existsProgramEnrollmentId(entity.getProgramEnrollmentId())) {
                throw new ResourceNotFoundException("sis.program_enrollment", entity.getProgramEnrollmentId());
            }
            if (!scope.canRead(institutionOfProgramEnrollmentSubject(entity))) {
                throw new ResourceNotFoundException("sis.program_enrollment", entity.getProgramEnrollmentId());
            }
            assertValidProgramEnrollmentSubject(entity, true);
            scope.requireWritable(institutionOfProgramEnrollmentSubject(entity));
            assertUniqueProgramEnrollmentSubject(entity);
            if (validateOnly) {
                return programEnrollmentSubjectMapper.toDto(entity);
            }
            ProgramEnrollmentSubjectEntity stored = programEnrollmentSubjectRepository.insert(entity);
            audit.recordCreate(PROGRAM_ENROLLMENT_SUBJECT_AGGREGATE, stored.getId());
            outbox.publish(PROGRAM_ENROLLMENT_SUBJECT_AGGREGATE + SisEvents.CREATED, PROGRAM_ENROLLMENT_SUBJECT_AGGREGATE,
                    stored.getId(), SisEvents.payload(stored.getId()));
            return programEnrollmentSubjectMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public ProgramEnrollmentSubject updateProgramEnrollmentSubject(UUID programEnrollmentId, UUID id, String ifMatch,
            ProgramEnrollmentSubjectUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, PROGRAM_ENROLLMENT_SUBJECT_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PROGRAM_ENROLLMENT_SUBJECT_PERMISSION_UPDATE);
            ProgramEnrollmentSubjectEntity entity = loadVisibleProgramEnrollmentSubject(scope, programEnrollmentId, id);
            scope.requireWritable(institutionOfProgramEnrollmentSubject(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyProgramEnrollmentSubject(entity, changes, fields);
            validator.validate(programEnrollmentSubjectMapper.toDto(entity));
            assertValidProgramEnrollmentSubject(entity, false);
            scope.requireWritable(institutionOfProgramEnrollmentSubject(entity));
            assertUniqueProgramEnrollmentSubject(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return programEnrollmentSubjectMapper.toDto(entity);
            }
            if (programEnrollmentSubjectRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(PROGRAM_ENROLLMENT_SUBJECT_AGGREGATE, id, fields);
            outbox.publish(PROGRAM_ENROLLMENT_SUBJECT_AGGREGATE + SisEvents.UPDATED, PROGRAM_ENROLLMENT_SUBJECT_AGGREGATE,
                    id, SisEvents.payload(id));
            return programEnrollmentSubjectMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteProgramEnrollmentSubject(UUID programEnrollmentId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PROGRAM_ENROLLMENT_SUBJECT_PERMISSION_DELETE);
            ProgramEnrollmentSubjectEntity entity = loadVisibleProgramEnrollmentSubject(scope, programEnrollmentId, id);
            scope.requireWritable(institutionOfProgramEnrollmentSubject(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (programEnrollmentSubjectRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(PROGRAM_ENROLLMENT_SUBJECT_AGGREGATE, id);
            outbox.publish(PROGRAM_ENROLLMENT_SUBJECT_AGGREGATE + SisEvents.DELETED, PROGRAM_ENROLLMENT_SUBJECT_AGGREGATE,
                    id, SisEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ProgramEnrollmentSubjectEntity loadVisibleProgramEnrollmentSubject(DataScope scope, UUID programEnrollmentId,
            UUID id) {
        ProgramEnrollmentSubjectEntity entity = programEnrollmentSubjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PROGRAM_ENROLLMENT_SUBJECT_AGGREGATE, id));
        if (!scope.canRead(institutionOfProgramEnrollmentSubject(entity))
                || !programEnrollmentId.equals(entity.getProgramEnrollmentId())) {
            throw new ResourceNotFoundException(PROGRAM_ENROLLMENT_SUBJECT_AGGREGATE, id);
        }
        return entity;
    }

    /**
     * The institution that owns the row (the institution of the sis.program_enrollment → sis.student it belongs to).
     */
    private Optional<UUID> institutionOfProgramEnrollmentSubject(ProgramEnrollmentSubjectEntity entity) {
        return programEnrollmentSubjectRepository.institutionOf(entity.getProgramEnrollmentId());
    }

    /**
     * Applies the selected ProgramEnrollmentSubjectUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void applyProgramEnrollmentSubject(ProgramEnrollmentSubjectEntity entity,
            ProgramEnrollmentSubjectUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "subjectId" -> entity.setSubjectId(changes.getSubjectId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidProgramEnrollmentSubject(ProgramEnrollmentSubjectEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getProgramEnrollmentId() == null) {
            errors.add(new FieldError("programEnrollmentId", "must not be null", "NotNull"));
        }
        if (entity.getSubjectId() == null) {
            errors.add(new FieldError("subjectId", "must not be null", "NotNull"));
        }
        if (entity.getProgramEnrollmentId() != null
                && !programEnrollmentSubjectRepository.existsProgramEnrollmentId(entity.getProgramEnrollmentId())) {
            errors.add(new FieldError("programEnrollmentId", "program enrollment does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueProgramEnrollmentSubject(ProgramEnrollmentSubjectEntity entity) {
        if (programEnrollmentSubjectRepository.takenProgramEnrollmentIdSubjectId1(entity)) {
            throw new DuplicateResourceException(
                    "A program enrollment subject with the same program enrollment id and subject id already exists.");
        }
    }
}
