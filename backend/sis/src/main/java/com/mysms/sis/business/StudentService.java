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
import com.mysms.sis.api.model.Student;
import com.mysms.sis.api.model.StudentGuardian;
import com.mysms.sis.api.model.StudentGuardianUpdate;
import com.mysms.sis.api.model.StudentSibling;
import com.mysms.sis.api.model.StudentSiblingUpdate;
import com.mysms.sis.api.model.StudentUpdate;
import com.mysms.sis.config.SisDatabaseConfig;
import com.mysms.sis.config.SisEventsConfig;
import com.mysms.sis.domain.StudentEntity;
import com.mysms.sis.domain.StudentGuardianEntity;
import com.mysms.sis.domain.StudentSiblingEntity;
import com.mysms.sis.events.SisEvents;
import com.mysms.sis.mapper.StudentGuardianMapper;
import com.mysms.sis.mapper.StudentMapper;
import com.mysms.sis.mapper.StudentSiblingMapper;
import com.mysms.sis.repository.StudentGuardianRepository;
import com.mysms.sis.repository.StudentRepository;
import com.mysms.sis.repository.StudentSiblingRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of sis.student (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a Student
 * are served here too — StudentGuardian, StudentSibling.
 */
@Service
public class StudentService extends AppTransactionManager {

    public static final String AGGREGATE = StudentEntity.TABLE;
    static final String PERMISSION_LIST = "sis.student.read";
    static final String PERMISSION_GET = "sis.student.read";
    static final String PERMISSION_CREATE = "sis.student.create";
    static final String PERMISSION_UPDATE = "sis.student.update";
    static final String PERMISSION_DELETE = "sis.student.delete";
    /** The contract's StudentUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("personId", "userId", "institutionId", "admissionNumber", "admissionDate", "joiningDate",
            "studentCategoryId", "governmentStudentId", "bonafideCertificateNumber", "exitDate", "exitReason");

    public static final String STUDENT_GUARDIAN_AGGREGATE = StudentGuardianEntity.TABLE;
    static final String STUDENT_GUARDIAN_PERMISSION_LIST = "sis.student_guardian.read";
    static final String STUDENT_GUARDIAN_PERMISSION_GET = "sis.student_guardian.read";
    static final String STUDENT_GUARDIAN_PERMISSION_CREATE = "sis.student_guardian.create";
    static final String STUDENT_GUARDIAN_PERMISSION_UPDATE = "sis.student_guardian.update";
    static final String STUDENT_GUARDIAN_PERMISSION_DELETE = "sis.student_guardian.delete";
    /** The contract's StudentGuardianUpdate fields, in apply order. */
    static final List<String> STUDENT_GUARDIAN_WRITABLE =
            List.of("isLegalGuardian", "hasCustody", "guardianId", "relationshipTypeId", "isPrimaryContact",
            "receivesCommunication", "canPickUp", "status");

    public static final String STUDENT_SIBLING_AGGREGATE = StudentSiblingEntity.TABLE;
    static final String STUDENT_SIBLING_PERMISSION_LIST = "sis.student_sibling.read";
    static final String STUDENT_SIBLING_PERMISSION_GET = "sis.student_sibling.read";
    static final String STUDENT_SIBLING_PERMISSION_CREATE = "sis.student_sibling.create";
    static final String STUDENT_SIBLING_PERMISSION_UPDATE = "sis.student_sibling.update";
    static final String STUDENT_SIBLING_PERMISSION_DELETE = "sis.student_sibling.delete";
    /** The contract's StudentSiblingUpdate fields, in apply order. */
    static final List<String> STUDENT_SIBLING_WRITABLE = List.of("siblingStudentId", "siblingName", "details");

    private final StudentRepository repository;
    private final StudentMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final StudentGuardianRepository studentGuardianRepository;
    private final StudentGuardianMapper studentGuardianMapper;
    private final StudentSiblingRepository studentSiblingRepository;
    private final StudentSiblingMapper studentSiblingMapper;

    public StudentService(StudentRepository repository,
            StudentMapper mapper,
            @Qualifier(SisDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(SisDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(SisEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(SisEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            StudentGuardianRepository studentGuardianRepository,
            StudentGuardianMapper studentGuardianMapper,
            StudentSiblingRepository studentSiblingRepository,
            StudentSiblingMapper studentSiblingMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.studentGuardianRepository = studentGuardianRepository;
        this.studentGuardianMapper = studentGuardianMapper;
        this.studentSiblingRepository = studentSiblingRepository;
        this.studentSiblingMapper = studentSiblingMapper;
    }

    public Student getStudent(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Student> listStudents(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, StudentRepository.SORT_FIELDS, StudentRepository.DEFAULT_ORDER,
                StudentRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, StudentRepository.FILTER_FIELDS)
                .and(StudentRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<StudentEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Student createStudent(Student request, boolean validateOnly) {
        validator.validate(request);
        StudentEntity entity = mapper.toEntity(request);
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
            StudentEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + SisEvents.CREATED, AGGREGATE, stored.getId(), memberPayload(stored));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public Student updateStudent(UUID id, String ifMatch, StudentUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            StudentEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + SisEvents.UPDATED, AGGREGATE, id, memberPayload(entity));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteStudent(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            StudentEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByDisciplineIncidentStudentId(id)) {
                throw new InvalidStateException(
                        "The student is still referenced by sis.discipline_incident rows; remove those first.");
            }
            if (repository.referencedByProgramEnrollmentStudentId(id)) {
                throw new InvalidStateException(
                        "The student is still referenced by sis.program_enrollment rows; remove those first.");
            }
            if (repository.referencedByPromotionStudentId(id)) {
                throw new InvalidStateException(
                        "The student is still referenced by sis.promotion rows; remove those first.");
            }
            if (repository.referencedByStudentActivityStudentId(id)) {
                throw new InvalidStateException(
                        "The student is still referenced by sis.student_activity rows; remove those first.");
            }
            if (repository.referencedByStudentEnrollmentStudentId(id)) {
                throw new InvalidStateException(
                        "The student is still referenced by sis.student_enrollment rows; remove those first.");
            }
            if (repository.referencedByStudentGuardianStudentId(id)) {
                throw new InvalidStateException(
                        "The student is still referenced by sis.student_guardian rows; remove those first.");
            }
            if (repository.referencedByStudentHealthRecordStudentId(id)) {
                throw new InvalidStateException(
                        "The student is still referenced by sis.student_health_record rows; remove those first.");
            }
            if (repository.referencedByStudentLeaveRequestStudentId(id)) {
                throw new InvalidStateException(
                        "The student is still referenced by sis.student_leave_request rows; remove those first.");
            }
            if (repository.referencedByStudentSiblingSiblingStudentId(id)) {
                throw new InvalidStateException(
                        "The student is still referenced by sis.student_sibling rows; remove those first.");
            }
            if (repository.referencedByStudentSiblingStudentId(id)) {
                throw new InvalidStateException(
                        "The student is still referenced by sis.student_sibling rows; remove those first.");
            }
            if (repository.referencedBySubjectRegistrationStudentId(id)) {
                throw new InvalidStateException(
                        "The student is still referenced by sis.subject_registration rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + SisEvents.DELETED, AGGREGATE, id, memberPayload(entity));
        });
    }

    /** The event payload of a student record as stored: whose it is, where, and its row version. */
    private static Map<String, Object> memberPayload(StudentEntity student) {
        return SisEvents.member(student.getId(), student.getUserId(), student.getPersonId(),
                student.getInstitutionId(), student.getRowVersion());
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private StudentEntity loadVisible(DataScope scope, UUID id) {
        StudentEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(StudentEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected StudentUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(StudentEntity entity, StudentUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "personId" -> entity.setPersonId(changes.getPersonId());
                case "userId" -> entity.setUserId(changes.getUserId());
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "admissionNumber" -> entity.setAdmissionNumber(changes.getAdmissionNumber());
                case "admissionDate" -> entity.setAdmissionDate(changes.getAdmissionDate());
                case "joiningDate" -> entity.setJoiningDate(changes.getJoiningDate());
                case "studentCategoryId" -> entity.setStudentCategoryId(changes.getStudentCategoryId());
                case "governmentStudentId" -> entity.setGovernmentStudentId(changes.getGovernmentStudentId());
                case "bonafideCertificateNumber" -> entity.setBonafideCertificateNumber(
                        changes.getBonafideCertificateNumber());
                case "exitDate" -> entity.setExitDate(changes.getExitDate());
                case "exitReason" -> entity.setExitReason(changes.getExitReason());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(StudentEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getPersonId() == null) {
            errors.add(new FieldError("personId", "must not be null", "NotNull"));
        }
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getStudentCategoryId() != null
                && !repository.existsStudentCategoryId(entity.getStudentCategoryId())) {
            errors.add(new FieldError("studentCategoryId", "student category does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(StudentEntity entity) {
        if (repository.takenInstitutionIdPersonId1(entity)) {
            throw new DuplicateResourceException(
                    "A student with the same institution id and person id already exists.");
        }
        if (entity.getUserId() != null && repository.takenInstitutionIdUserId2(entity)) {
            throw new DuplicateResourceException("A student with the same institution id and user id already exists.");
        }
    }

    // ---- StudentGuardian: a record of the Student aggregate ----

    public StudentGuardian getStudentGuardian(UUID studentId, UUID id) {
        return doInReadTransaction(() -> {
            return studentGuardianMapper.toDto(loadVisibleStudentGuardian(DataScope.current(STUDENT_GUARDIAN_PERMISSION_GET),
                    studentId, id));
        });
    }

    public Page<StudentGuardian> listStudentGuardians(UUID studentId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, StudentGuardianRepository.SORT_FIELDS, StudentGuardianRepository.DEFAULT_ORDER,
                StudentGuardianRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, StudentGuardianRepository.FILTER_FIELDS)
                .and(StudentGuardianRepository.ofParent(studentId))
                        .and(StudentGuardianRepository.scope(DataScope.current(STUDENT_GUARDIAN_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + studentId, clock);
        return doInReadTransaction(() -> {
            Page<StudentGuardianEntity> rows = studentGuardianRepository.list(page, where);
            return rows.map(studentGuardianMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public StudentGuardian createStudentGuardian(UUID studentId, StudentGuardian request, boolean validateOnly) {
        validator.validate(request);
        StudentGuardianEntity entity = studentGuardianMapper.toEntity(request);
        entity.setStudentId(studentId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(STUDENT_GUARDIAN_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!studentGuardianRepository.existsStudentId(entity.getStudentId())) {
                throw new ResourceNotFoundException("sis.student", entity.getStudentId());
            }
            if (!scope.canRead(institutionOfStudentGuardian(entity))) {
                throw new ResourceNotFoundException("sis.student", entity.getStudentId());
            }
            assertValidStudentGuardian(entity, true);
            scope.requireWritable(institutionOfStudentGuardian(entity));
            assertUniqueStudentGuardian(entity);
            if (validateOnly) {
                return studentGuardianMapper.toDto(entity);
            }
            StudentGuardianEntity stored = studentGuardianRepository.insert(entity);
            audit.recordCreate(STUDENT_GUARDIAN_AGGREGATE, stored.getId());
            outbox.publish(STUDENT_GUARDIAN_AGGREGATE + SisEvents.CREATED, STUDENT_GUARDIAN_AGGREGATE, stored.getId(),
                    SisEvents.payload(stored.getId()));
            return studentGuardianMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public StudentGuardian updateStudentGuardian(UUID studentId, UUID id, String ifMatch, StudentGuardianUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, STUDENT_GUARDIAN_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(STUDENT_GUARDIAN_PERMISSION_UPDATE);
            StudentGuardianEntity entity = loadVisibleStudentGuardian(scope, studentId, id);
            scope.requireWritable(institutionOfStudentGuardian(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyStudentGuardian(entity, changes, fields);
            validator.validate(studentGuardianMapper.toDto(entity));
            assertValidStudentGuardian(entity, false);
            scope.requireWritable(institutionOfStudentGuardian(entity));
            assertUniqueStudentGuardian(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return studentGuardianMapper.toDto(entity);
            }
            if (studentGuardianRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(STUDENT_GUARDIAN_AGGREGATE, id, fields);
            outbox.publish(STUDENT_GUARDIAN_AGGREGATE + SisEvents.UPDATED, STUDENT_GUARDIAN_AGGREGATE, id, SisEvents.payload(id));
            return studentGuardianMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteStudentGuardian(UUID studentId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(STUDENT_GUARDIAN_PERMISSION_DELETE);
            StudentGuardianEntity entity = loadVisibleStudentGuardian(scope, studentId, id);
            scope.requireWritable(institutionOfStudentGuardian(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (studentGuardianRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(STUDENT_GUARDIAN_AGGREGATE, id);
            outbox.publish(STUDENT_GUARDIAN_AGGREGATE + SisEvents.DELETED, STUDENT_GUARDIAN_AGGREGATE, id, SisEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private StudentGuardianEntity loadVisibleStudentGuardian(DataScope scope, UUID studentId, UUID id) {
        StudentGuardianEntity entity = studentGuardianRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_GUARDIAN_AGGREGATE, id));
        if (!scope.canRead(institutionOfStudentGuardian(entity)) || !studentId.equals(entity.getStudentId())) {
            throw new ResourceNotFoundException(STUDENT_GUARDIAN_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the sis.student it belongs to). */
    private Optional<UUID> institutionOfStudentGuardian(StudentGuardianEntity entity) {
        return studentGuardianRepository.institutionOf(entity.getStudentId());
    }

    /**
     * Applies the selected StudentGuardianUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyStudentGuardian(StudentGuardianEntity entity, StudentGuardianUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "isLegalGuardian" -> entity.setIsLegalGuardian(changes.getIsLegalGuardian());
                case "hasCustody" -> entity.setHasCustody(changes.getHasCustody());
                case "guardianId" -> entity.setGuardianId(changes.getGuardianId());
                case "relationshipTypeId" -> entity.setRelationshipTypeId(changes.getRelationshipTypeId());
                case "isPrimaryContact" -> entity.setIsPrimaryContact(changes.getIsPrimaryContact());
                case "receivesCommunication" -> entity.setReceivesCommunication(changes.getReceivesCommunication());
                case "canPickUp" -> entity.setCanPickUp(changes.getCanPickUp());
                case "status" -> entity.setStatus(Optional.ofNullable(changes.getStatus()).map(v -> v.getValue())
                        .orElse(null));
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidStudentGuardian(StudentGuardianEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (!creating && entity.getIsLegalGuardian() == null) {
            errors.add(new FieldError("isLegalGuardian", "must not be null", "NotNull"));
        }
        if (!creating && entity.getHasCustody() == null) {
            errors.add(new FieldError("hasCustody", "must not be null", "NotNull"));
        }
        if (entity.getStudentId() == null) {
            errors.add(new FieldError("studentId", "must not be null", "NotNull"));
        }
        if (entity.getGuardianId() == null) {
            errors.add(new FieldError("guardianId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsPrimaryContact() == null) {
            errors.add(new FieldError("isPrimaryContact", "must not be null", "NotNull"));
        }
        if (!creating && entity.getReceivesCommunication() == null) {
            errors.add(new FieldError("receivesCommunication", "must not be null", "NotNull"));
        }
        if (!creating && entity.getCanPickUp() == null) {
            errors.add(new FieldError("canPickUp", "must not be null", "NotNull"));
        }
        if (!creating && entity.getStatus() == null) {
            errors.add(new FieldError("status", "must not be null", "NotNull"));
        }
        if (entity.getGuardianId() != null && !studentGuardianRepository.existsGuardianId(entity.getGuardianId())) {
            errors.add(new FieldError("guardianId", "guardian does not exist", "NOT_FOUND"));
        }
        if (entity.getRelationshipTypeId() != null
                && !studentGuardianRepository.existsRelationshipTypeId(entity.getRelationshipTypeId())) {
            errors.add(new FieldError("relationshipTypeId", "relationship type does not exist", "NOT_FOUND"));
        }
        if (entity.getStudentId() != null && !studentGuardianRepository.existsStudentId(entity.getStudentId())) {
            errors.add(new FieldError("studentId", "student does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueStudentGuardian(StudentGuardianEntity entity) {
        if (studentGuardianRepository.takenStudentIdGuardianId1(entity)) {
            throw new DuplicateResourceException(
                    "A student guardian with the same student id and guardian id already exists.");
        }
    }

    // ---- StudentSibling: a record of the Student aggregate ----

    public StudentSibling getStudentSibling(UUID studentId, UUID id) {
        return doInReadTransaction(() -> {
            return studentSiblingMapper.toDto(loadVisibleStudentSibling(DataScope.current(STUDENT_SIBLING_PERMISSION_GET),
                    studentId, id));
        });
    }

    public Page<StudentSibling> listStudentSiblings(UUID studentId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, StudentSiblingRepository.SORT_FIELDS, StudentSiblingRepository.DEFAULT_ORDER,
                StudentSiblingRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, StudentSiblingRepository.FILTER_FIELDS)
                .and(StudentSiblingRepository.ofParent(studentId))
                        .and(StudentSiblingRepository.scope(DataScope.current(STUDENT_SIBLING_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + studentId, clock);
        return doInReadTransaction(() -> {
            Page<StudentSiblingEntity> rows = studentSiblingRepository.list(page, where);
            return rows.map(studentSiblingMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public StudentSibling createStudentSibling(UUID studentId, StudentSibling request, boolean validateOnly) {
        validator.validate(request);
        StudentSiblingEntity entity = studentSiblingMapper.toEntity(request);
        entity.setStudentId(studentId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(STUDENT_SIBLING_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!studentSiblingRepository.existsStudentId(entity.getStudentId())) {
                throw new ResourceNotFoundException("sis.student", entity.getStudentId());
            }
            if (!scope.canRead(institutionOfStudentSibling(entity))) {
                throw new ResourceNotFoundException("sis.student", entity.getStudentId());
            }
            assertValidStudentSibling(entity, true);
            scope.requireWritable(institutionOfStudentSibling(entity));
            assertUniqueStudentSibling(entity);
            if (validateOnly) {
                return studentSiblingMapper.toDto(entity);
            }
            StudentSiblingEntity stored = studentSiblingRepository.insert(entity);
            audit.recordCreate(STUDENT_SIBLING_AGGREGATE, stored.getId());
            outbox.publish(STUDENT_SIBLING_AGGREGATE + SisEvents.CREATED, STUDENT_SIBLING_AGGREGATE, stored.getId(),
                    SisEvents.payload(stored.getId()));
            return studentSiblingMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public StudentSibling updateStudentSibling(UUID studentId, UUID id, String ifMatch, StudentSiblingUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, STUDENT_SIBLING_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(STUDENT_SIBLING_PERMISSION_UPDATE);
            StudentSiblingEntity entity = loadVisibleStudentSibling(scope, studentId, id);
            scope.requireWritable(institutionOfStudentSibling(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyStudentSibling(entity, changes, fields);
            validator.validate(studentSiblingMapper.toDto(entity));
            assertValidStudentSibling(entity, false);
            scope.requireWritable(institutionOfStudentSibling(entity));
            assertUniqueStudentSibling(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return studentSiblingMapper.toDto(entity);
            }
            if (studentSiblingRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(STUDENT_SIBLING_AGGREGATE, id, fields);
            outbox.publish(STUDENT_SIBLING_AGGREGATE + SisEvents.UPDATED, STUDENT_SIBLING_AGGREGATE, id, SisEvents.payload(id));
            return studentSiblingMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteStudentSibling(UUID studentId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(STUDENT_SIBLING_PERMISSION_DELETE);
            StudentSiblingEntity entity = loadVisibleStudentSibling(scope, studentId, id);
            scope.requireWritable(institutionOfStudentSibling(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (studentSiblingRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(STUDENT_SIBLING_AGGREGATE, id);
            outbox.publish(STUDENT_SIBLING_AGGREGATE + SisEvents.DELETED, STUDENT_SIBLING_AGGREGATE, id, SisEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private StudentSiblingEntity loadVisibleStudentSibling(DataScope scope, UUID studentId, UUID id) {
        StudentSiblingEntity entity = studentSiblingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(STUDENT_SIBLING_AGGREGATE, id));
        if (!scope.canRead(institutionOfStudentSibling(entity)) || !studentId.equals(entity.getStudentId())) {
            throw new ResourceNotFoundException(STUDENT_SIBLING_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the sis.student it belongs to). */
    private Optional<UUID> institutionOfStudentSibling(StudentSiblingEntity entity) {
        return studentSiblingRepository.institutionOf(entity.getStudentId());
    }

    /**
     * Applies the selected StudentSiblingUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyStudentSibling(StudentSiblingEntity entity, StudentSiblingUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "siblingStudentId" -> entity.setSiblingStudentId(changes.getSiblingStudentId());
                case "siblingName" -> entity.setSiblingName(changes.getSiblingName());
                case "details" -> entity.setDetails(changes.getDetails());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidStudentSibling(StudentSiblingEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getStudentId() == null) {
            errors.add(new FieldError("studentId", "must not be null", "NotNull"));
        }
        if (entity.getSiblingStudentId() != null
                && !studentSiblingRepository.existsSiblingStudentId(entity.getSiblingStudentId())) {
            errors.add(new FieldError("siblingStudentId", "student does not exist", "NOT_FOUND"));
        }
        if (entity.getStudentId() != null && !studentSiblingRepository.existsStudentId(entity.getStudentId())) {
            errors.add(new FieldError("studentId", "student does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueStudentSibling(StudentSiblingEntity entity) {

    }
}
