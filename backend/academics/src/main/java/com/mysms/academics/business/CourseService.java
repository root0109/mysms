package com.mysms.academics.business;

import com.mysms.academics.api.model.Course;
import com.mysms.academics.api.model.CourseSubject;
import com.mysms.academics.api.model.CourseSubjectUpdate;
import com.mysms.academics.api.model.CourseUpdate;
import com.mysms.academics.config.AcademicsDatabaseConfig;
import com.mysms.academics.config.AcademicsEventsConfig;
import com.mysms.academics.domain.CourseEntity;
import com.mysms.academics.domain.CourseSubjectEntity;
import com.mysms.academics.events.AcademicsEvents;
import com.mysms.academics.mapper.CourseMapper;
import com.mysms.academics.mapper.CourseSubjectMapper;
import com.mysms.academics.repository.CourseRepository;
import com.mysms.academics.repository.CourseSubjectRepository;
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
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of academics.course: audit and domain event in the same transaction as every change; rows outside
 * the caller's institutions are invisible, and tenant-level rows change only through a tenant-wide grant.
 *
 * <p>Also serves the records that exist only as part of the Course aggregate: CourseSubject.
 */
@Service
public class CourseService extends AppTransactionManager {

    public static final String AGGREGATE = CourseEntity.TABLE;
    static final String PERMISSION_LIST = "academics.course.read";
    static final String PERMISSION_GET = "academics.course.read";
    static final String PERMISSION_CREATE = "academics.course.create";
    static final String PERMISSION_UPDATE = "academics.course.update";
    static final String PERMISSION_DELETE = "academics.course.delete";

    public static final String COURSE_SUBJECT_AGGREGATE = CourseSubjectEntity.TABLE;
    static final String COURSE_SUBJECT_PERMISSION_LIST = "academics.course_subject.read";
    static final String COURSE_SUBJECT_PERMISSION_GET = "academics.course_subject.read";
    static final String COURSE_SUBJECT_PERMISSION_CREATE = "academics.course_subject.create";
    static final String COURSE_SUBJECT_PERMISSION_UPDATE = "academics.course_subject.update";
    static final String COURSE_SUBJECT_PERMISSION_DELETE = "academics.course_subject.delete";

    private final CourseRepository repository;
    private final CourseMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final CourseSubjectRepository courseSubjectRepository;
    private final CourseSubjectMapper courseSubjectMapper;

    public CourseService(CourseRepository repository,
            CourseMapper mapper,
            @Qualifier(AcademicsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AcademicsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AcademicsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(AcademicsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            CourseSubjectRepository courseSubjectRepository,
            CourseSubjectMapper courseSubjectMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.courseSubjectRepository = courseSubjectRepository;
        this.courseSubjectMapper = courseSubjectMapper;
    }

    public Course getCourse(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Course> listCourses(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, CourseRepository.SORT_FIELDS, CourseRepository.DEFAULT_ORDER,
                CourseRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CourseRepository.FILTER_FIELDS)
                .and(CourseRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<CourseEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Course createCourse(Course request, boolean validateOnly) {
        validator.validate(request);
        CourseEntity entity = mapper.toEntity(request);
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
            CourseEntity stored = repository.insert(entity);
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
    public Course updateCourse(UUID id, String ifMatch, CourseUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            CourseEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + AcademicsEvents.UPDATED, AGGREGATE, id, AcademicsEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCourse(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            CourseEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByBatchCourseId(id)) {
                throw new InvalidStateException(
                        "The course is still referenced by academics.batch rows; remove those first.");
            }
            if (repository.referencedByCourseParentCourseId(id)) {
                throw new InvalidStateException(
                        "The course is still referenced by academics.course rows; remove those first.");
            }
            if (repository.referencedByCourseSubjectCourseId(id)) {
                throw new InvalidStateException(
                        "The course is still referenced by academics.course_subject rows; remove those first.");
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
    private CourseEntity loadVisible(DataScope scope, UUID id) {
        CourseEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(CourseEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(CourseEntity entity, CourseUpdate changes) {
        Patches patch = new Patches();
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("programId", changes.getProgramId(), entity::setProgramId);
        patch.apply("departmentId", changes.getDepartmentId(), entity::setDepartmentId);
        patch.apply("parentCourseId", changes.getParentCourseId(), entity::setParentCourseId);
        patch.apply("code", changes.getCode(), entity::setCode);
        patch.apply("name", changes.getName(), entity::setName);
        patch.apply("evaluationType", changes.getEvaluationType(),
                v -> entity.setEvaluationType(v == null ? null : v.getValue()));
        patch.apply("minUnitLoad", changes.getMinUnitLoad(), v -> entity.setMinUnitLoad(WireValues.decimal(v)));
        patch.apply("maxUnitLoad", changes.getMaxUnitLoad(), v -> entity.setMaxUnitLoad(WireValues.decimal(v)));
        patch.apply("isActive", changes.getIsActive(), entity::setIsActive);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(CourseEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getCode() == null) {
            errors.add(new FieldError("code", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (!creating && entity.getEvaluationType() == null) {
            errors.add(new FieldError("evaluationType", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("minUnitLoad", WireValues.decimal(entity.getMinUnitLoad()), 6, 2, errors);
        WireValues.checkDecimal("maxUnitLoad", WireValues.decimal(entity.getMaxUnitLoad()), 6, 2, errors);
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (entity.getParentCourseId() != null && !repository.existsParentCourseId(entity.getParentCourseId())) {
            errors.add(new FieldError("parentCourseId", "course does not exist", "NOT_FOUND"));
        }
        if (entity.getProgramId() != null && !repository.existsProgramId(entity.getProgramId())) {
            errors.add(new FieldError("programId", "program does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(CourseEntity entity) {
        if (repository.takenInstitutionIdCode1(entity)) {
            throw new DuplicateResourceException("A course with the same institution id and code already exists.");
        }
    }

    // ---- CourseSubject: a record of the Course aggregate ----

    public CourseSubject getCourseSubject(UUID courseId, UUID id) {
        return doInReadTransaction(() -> {
            return courseSubjectMapper.toDto(loadVisibleCourseSubject(DataScope.current(COURSE_SUBJECT_PERMISSION_GET),
                    courseId, id));
        });
    }

    public Page<CourseSubject> listCourseSubjects(UUID courseId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, CourseSubjectRepository.SORT_FIELDS, CourseSubjectRepository.DEFAULT_ORDER,
                CourseSubjectRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CourseSubjectRepository.FILTER_FIELDS)
                .and(CourseSubjectRepository.ofParent(courseId))
                        .and(CourseSubjectRepository.scope(DataScope.current(COURSE_SUBJECT_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + courseId, clock);
        return doInReadTransaction(() -> {
            Page<CourseSubjectEntity> rows = courseSubjectRepository.list(page, where);
            return rows.map(courseSubjectMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public CourseSubject createCourseSubject(UUID courseId, CourseSubject request, boolean validateOnly) {
        validator.validate(request);
        CourseSubjectEntity entity = courseSubjectMapper.toEntity(request);
        entity.setCourseId(courseId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(COURSE_SUBJECT_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!courseSubjectRepository.existsCourseId(entity.getCourseId())) {
                throw new ResourceNotFoundException("academics.course", entity.getCourseId());
            }
            if (!scope.canRead(institutionOfCourseSubject(entity))) {
                throw new ResourceNotFoundException("academics.course", entity.getCourseId());
            }
            assertValidCourseSubject(entity, true);
            scope.requireWritable(institutionOfCourseSubject(entity));
            assertUniqueCourseSubject(entity);
            if (validateOnly) {
                return courseSubjectMapper.toDto(entity);
            }
            CourseSubjectEntity stored = courseSubjectRepository.insert(entity);
            audit.recordCreate(COURSE_SUBJECT_AGGREGATE, stored.getId());
            outbox.publish(COURSE_SUBJECT_AGGREGATE + AcademicsEvents.CREATED, COURSE_SUBJECT_AGGREGATE, stored.getId(),
                    AcademicsEvents.payload(stored.getId()));
            return courseSubjectMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. A field the request leaves out is kept, a null one is cleared and a
     * value is set (§7.2). The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public CourseSubject updateCourseSubject(UUID courseId, UUID id, String ifMatch, CourseSubjectUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COURSE_SUBJECT_PERMISSION_UPDATE);
            CourseSubjectEntity entity = loadVisibleCourseSubject(scope, courseId, id);
            scope.requireWritable(institutionOfCourseSubject(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyCourseSubject(entity, changes);
            validator.validate(courseSubjectMapper.toDto(entity));
            assertValidCourseSubject(entity, false);
            scope.requireWritable(institutionOfCourseSubject(entity));
            assertUniqueCourseSubject(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return courseSubjectMapper.toDto(entity);
            }
            if (courseSubjectRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(COURSE_SUBJECT_AGGREGATE, id, fields);
            outbox.publish(COURSE_SUBJECT_AGGREGATE + AcademicsEvents.UPDATED, COURSE_SUBJECT_AGGREGATE, id,
                    AcademicsEvents.payload(id));
            return courseSubjectMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCourseSubject(UUID courseId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(COURSE_SUBJECT_PERMISSION_DELETE);
            CourseSubjectEntity entity = loadVisibleCourseSubject(scope, courseId, id);
            scope.requireWritable(institutionOfCourseSubject(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (courseSubjectRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(COURSE_SUBJECT_AGGREGATE, id);
            outbox.publish(COURSE_SUBJECT_AGGREGATE + AcademicsEvents.DELETED, COURSE_SUBJECT_AGGREGATE, id,
                    AcademicsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private CourseSubjectEntity loadVisibleCourseSubject(DataScope scope, UUID courseId, UUID id) {
        CourseSubjectEntity entity = courseSubjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(COURSE_SUBJECT_AGGREGATE, id));
        if (!scope.canRead(institutionOfCourseSubject(entity)) || !courseId.equals(entity.getCourseId())) {
            throw new ResourceNotFoundException(COURSE_SUBJECT_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the academics.course it belongs to). */
    private Optional<UUID> institutionOfCourseSubject(CourseSubjectEntity entity) {
        return courseSubjectRepository.institutionOf(entity.getCourseId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyCourseSubject(CourseSubjectEntity entity, CourseSubjectUpdate changes) {
        Patches patch = new Patches();
        patch.apply("termNumber", changes.getTermNumber(), entity::setTermNumber);
        patch.apply("credits", changes.getCredits(), v -> entity.setCredits(WireValues.decimal(v)));
        patch.apply("subjectCategory", changes.getSubjectCategory(), entity::setSubjectCategory);
        patch.apply("maxMarks", changes.getMaxMarks(), v -> entity.setMaxMarks(WireValues.decimal(v)));
        patch.apply("subjectId", changes.getSubjectId(), entity::setSubjectId);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidCourseSubject(CourseSubjectEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        WireValues.checkDecimal("credits", WireValues.decimal(entity.getCredits()), 5, 2, errors);
        WireValues.checkDecimal("maxMarks", WireValues.decimal(entity.getMaxMarks()), 7, 2, errors);
        if (entity.getCourseId() == null) {
            errors.add(new FieldError("courseId", "must not be null", "NotNull"));
        }
        if (entity.getSubjectId() == null) {
            errors.add(new FieldError("subjectId", "must not be null", "NotNull"));
        }
        if (entity.getCourseId() != null && !courseSubjectRepository.existsCourseId(entity.getCourseId())) {
            errors.add(new FieldError("courseId", "course does not exist", "NOT_FOUND"));
        }
        if (entity.getSubjectId() != null && !courseSubjectRepository.existsSubjectId(entity.getSubjectId())) {
            errors.add(new FieldError("subjectId", "subject does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueCourseSubject(CourseSubjectEntity entity) {

    }
}
