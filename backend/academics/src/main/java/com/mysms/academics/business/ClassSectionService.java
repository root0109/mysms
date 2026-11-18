package com.mysms.academics.business;

import com.mysms.academics.api.model.ClassSection;
import com.mysms.academics.api.model.ClassSectionUpdate;
import com.mysms.academics.api.model.ClassSubject;
import com.mysms.academics.api.model.ClassSubjectUpdate;
import com.mysms.academics.config.AcademicsDatabaseConfig;
import com.mysms.academics.config.AcademicsEventsConfig;
import com.mysms.academics.domain.ClassSectionEntity;
import com.mysms.academics.domain.ClassSubjectEntity;
import com.mysms.academics.events.AcademicsEvents;
import com.mysms.academics.mapper.ClassSectionMapper;
import com.mysms.academics.mapper.ClassSubjectMapper;
import com.mysms.academics.repository.ClassSectionRepository;
import com.mysms.academics.repository.ClassSubjectRepository;
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
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of academics.class_section: audit and domain event in the same transaction as every change; rows
 * outside the caller's institutions are invisible, and tenant-level rows change only through a tenant-wide grant.
 *
 * <p>Also serves the records that exist only as part of the ClassSection aggregate: ClassSubject.
 */
@Service
public class ClassSectionService extends AppTransactionManager {

    public static final String AGGREGATE = ClassSectionEntity.TABLE;
    static final String PERMISSION_LIST = "academics.class_section.read";
    static final String PERMISSION_GET = "academics.class_section.read";
    static final String PERMISSION_CREATE = "academics.class_section.create";
    static final String PERMISSION_UPDATE = "academics.class_section.update";
    static final String PERMISSION_DELETE = "academics.class_section.delete";
    /** The contract's ClassSectionUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("institutionId", "academicYearId", "gradeLevelId", "sectionId", "classTeacherStaffId", "capacity",
                    "stream",
            "isActive");

    public static final String CLASS_SUBJECT_AGGREGATE = ClassSubjectEntity.TABLE;
    static final String CLASS_SUBJECT_PERMISSION_LIST = "academics.class_subject.read";
    static final String CLASS_SUBJECT_PERMISSION_GET = "academics.class_subject.read";
    static final String CLASS_SUBJECT_PERMISSION_CREATE = "academics.class_subject.create";
    static final String CLASS_SUBJECT_PERMISSION_UPDATE = "academics.class_subject.update";
    static final String CLASS_SUBJECT_PERMISSION_DELETE = "academics.class_subject.delete";
    /** The contract's ClassSubjectUpdate fields, in apply order. */
    static final List<String> CLASS_SUBJECT_WRITABLE = List.of("subjectId", "offeringType", "isActive");

    private final ClassSectionRepository repository;
    private final ClassSectionMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final ClassSubjectRepository classSubjectRepository;
    private final ClassSubjectMapper classSubjectMapper;

    public ClassSectionService(ClassSectionRepository repository,
            ClassSectionMapper mapper,
            @Qualifier(AcademicsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AcademicsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AcademicsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(AcademicsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            ClassSubjectRepository classSubjectRepository,
            ClassSubjectMapper classSubjectMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.classSubjectRepository = classSubjectRepository;
        this.classSubjectMapper = classSubjectMapper;
    }

    public ClassSection getClassSection(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<ClassSection> listClassSections(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ClassSectionRepository.SORT_FIELDS, ClassSectionRepository.DEFAULT_ORDER,
                ClassSectionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ClassSectionRepository.FILTER_FIELDS)
                .and(ClassSectionRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<ClassSectionEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ClassSection createClassSection(ClassSection request, boolean validateOnly) {
        validator.validate(request);
        ClassSectionEntity entity = mapper.toEntity(request);
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
            ClassSectionEntity stored = repository.insert(entity);
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
    public ClassSection updateClassSection(UUID id, String ifMatch, ClassSectionUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            ClassSectionEntity entity = loadVisible(scope, id);
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
    public void deleteClassSection(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            ClassSectionEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByClassSubjectClassSectionId(id)) {
                throw new InvalidStateException(
                        "The class section is still referenced by academics.class_subject rows; remove those first.");
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
    private ClassSectionEntity loadVisible(DataScope scope, UUID id) {
        ClassSectionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(ClassSectionEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /**
     * Applies the selected ClassSectionUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void apply(ClassSectionEntity entity, ClassSectionUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "academicYearId" -> entity.setAcademicYearId(changes.getAcademicYearId());
                case "gradeLevelId" -> entity.setGradeLevelId(changes.getGradeLevelId());
                case "sectionId" -> entity.setSectionId(changes.getSectionId());
                case "classTeacherStaffId" -> entity.setClassTeacherStaffId(changes.getClassTeacherStaffId());
                case "capacity" -> entity.setCapacity(changes.getCapacity());
                case "stream" -> entity.setStream(changes.getStream());
                case "isActive" -> entity.setIsActive(changes.getIsActive());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(ClassSectionEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getAcademicYearId() == null) {
            errors.add(new FieldError("academicYearId", "must not be null", "NotNull"));
        }
        if (entity.getGradeLevelId() == null) {
            errors.add(new FieldError("gradeLevelId", "must not be null", "NotNull"));
        }
        if (entity.getSectionId() == null) {
            errors.add(new FieldError("sectionId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (entity.getGradeLevelId() != null && !repository.existsGradeLevelId(entity.getGradeLevelId())) {
            errors.add(new FieldError("gradeLevelId", "grade level does not exist", "NOT_FOUND"));
        }
        if (entity.getSectionId() != null && !repository.existsSectionId(entity.getSectionId())) {
            errors.add(new FieldError("sectionId", "section does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(ClassSectionEntity entity) {
        if (repository.takenAcademicYearIdGradeLevelIdSectionId1(entity)) {
            throw new DuplicateResourceException(
                    "A class section with the same academic year id and grade level id and section id already exists.");
        }
    }

    // ---- ClassSubject: a record of the ClassSection aggregate ----

    public ClassSubject getClassSubject(UUID classSectionId, UUID id) {
        return doInReadTransaction(() -> {
            return classSubjectMapper.toDto(loadVisibleClassSubject(DataScope.current(CLASS_SUBJECT_PERMISSION_GET),
                    classSectionId, id));
        });
    }

    public Page<ClassSubject> listClassSubjects(UUID classSectionId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ClassSubjectRepository.SORT_FIELDS, ClassSubjectRepository.DEFAULT_ORDER,
                ClassSubjectRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ClassSubjectRepository.FILTER_FIELDS)
                .and(ClassSubjectRepository.ofParent(classSectionId))
                        .and(ClassSubjectRepository.scope(DataScope.current(CLASS_SUBJECT_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + classSectionId, clock);
        return doInReadTransaction(() -> {
            Page<ClassSubjectEntity> rows = classSubjectRepository.list(page, where);
            return rows.map(classSubjectMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ClassSubject createClassSubject(UUID classSectionId, ClassSubject request, boolean validateOnly) {
        validator.validate(request);
        ClassSubjectEntity entity = classSubjectMapper.toEntity(request);
        entity.setClassSectionId(classSectionId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(CLASS_SUBJECT_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!classSubjectRepository.existsClassSectionId(entity.getClassSectionId())) {
                throw new ResourceNotFoundException("academics.class_section", entity.getClassSectionId());
            }
            if (!scope.canRead(institutionOfClassSubject(entity))) {
                throw new ResourceNotFoundException("academics.class_section", entity.getClassSectionId());
            }
            assertValidClassSubject(entity, true);
            scope.requireWritable(institutionOfClassSubject(entity));
            assertUniqueClassSubject(entity);
            if (validateOnly) {
                return classSubjectMapper.toDto(entity);
            }
            ClassSubjectEntity stored = classSubjectRepository.insert(entity);
            audit.recordCreate(CLASS_SUBJECT_AGGREGATE, stored.getId());
            outbox.publish(CLASS_SUBJECT_AGGREGATE + AcademicsEvents.CREATED, CLASS_SUBJECT_AGGREGATE, stored.getId(),
                    AcademicsEvents.payload(stored.getId()));
            return classSubjectMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public ClassSubject updateClassSubject(UUID classSectionId, UUID id, String ifMatch, ClassSubjectUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, CLASS_SUBJECT_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(CLASS_SUBJECT_PERMISSION_UPDATE);
            ClassSubjectEntity entity = loadVisibleClassSubject(scope, classSectionId, id);
            scope.requireWritable(institutionOfClassSubject(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyClassSubject(entity, changes, fields);
            validator.validate(classSubjectMapper.toDto(entity));
            assertValidClassSubject(entity, false);
            scope.requireWritable(institutionOfClassSubject(entity));
            assertUniqueClassSubject(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return classSubjectMapper.toDto(entity);
            }
            if (classSubjectRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(CLASS_SUBJECT_AGGREGATE, id, fields);
            outbox.publish(CLASS_SUBJECT_AGGREGATE + AcademicsEvents.UPDATED, CLASS_SUBJECT_AGGREGATE, id,
                    AcademicsEvents.payload(id));
            return classSubjectMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteClassSubject(UUID classSectionId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(CLASS_SUBJECT_PERMISSION_DELETE);
            ClassSubjectEntity entity = loadVisibleClassSubject(scope, classSectionId, id);
            scope.requireWritable(institutionOfClassSubject(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (classSubjectRepository.referencedByCurriculumUnitClassSubjectId(id)) {
                throw new InvalidStateException(
                        "The class subject is still referenced by academics.curriculum_unit rows; remove those first.");
            }
            if (classSubjectRepository.referencedByTeachingAssignmentClassSubjectId(id)) {
                throw new InvalidStateException(
                        "The class subject is still referenced by academics.teaching_assignment rows; remove those "
                                + "first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (classSubjectRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(CLASS_SUBJECT_AGGREGATE, id);
            outbox.publish(CLASS_SUBJECT_AGGREGATE + AcademicsEvents.DELETED, CLASS_SUBJECT_AGGREGATE, id,
                    AcademicsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ClassSubjectEntity loadVisibleClassSubject(DataScope scope, UUID classSectionId, UUID id) {
        ClassSubjectEntity entity = classSubjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CLASS_SUBJECT_AGGREGATE, id));
        if (!scope.canRead(institutionOfClassSubject(entity)) || !classSectionId.equals(entity.getClassSectionId())) {
            throw new ResourceNotFoundException(CLASS_SUBJECT_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the academics.class_section it belongs to). */
    private Optional<UUID> institutionOfClassSubject(ClassSubjectEntity entity) {
        return classSubjectRepository.institutionOf(entity.getClassSectionId());
    }

    /**
     * Applies the selected ClassSubjectUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyClassSubject(ClassSubjectEntity entity, ClassSubjectUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "subjectId" -> entity.setSubjectId(changes.getSubjectId());
                case "offeringType" -> entity.setOfferingType(Optional.ofNullable(changes.getOfferingType())
                        .map(v -> v.getValue()).orElse(null));
                case "isActive" -> entity.setIsActive(changes.getIsActive());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidClassSubject(ClassSubjectEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getClassSectionId() == null) {
            errors.add(new FieldError("classSectionId", "must not be null", "NotNull"));
        }
        if (entity.getSubjectId() == null) {
            errors.add(new FieldError("subjectId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getOfferingType() == null) {
            errors.add(new FieldError("offeringType", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (entity.getClassSectionId() != null && !classSubjectRepository.existsClassSectionId(entity.getClassSectionId())) {
            errors.add(new FieldError("classSectionId", "class section does not exist", "NOT_FOUND"));
        }
        if (entity.getSubjectId() != null && !classSubjectRepository.existsSubjectId(entity.getSubjectId())) {
            errors.add(new FieldError("subjectId", "subject does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueClassSubject(ClassSubjectEntity entity) {
        if (classSubjectRepository.takenClassSectionIdSubjectId1(entity)) {
            throw new DuplicateResourceException(
                    "A class subject with the same class section id and subject id already exists.");
        }
    }
}
