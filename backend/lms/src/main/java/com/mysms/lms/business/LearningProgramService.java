package com.mysms.lms.business;

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
import com.mysms.core.web.UpdateMask;
import com.mysms.lms.api.model.LearningProgram;
import com.mysms.lms.api.model.LearningProgramCourse;
import com.mysms.lms.api.model.LearningProgramCourseUpdate;
import com.mysms.lms.api.model.LearningProgramMember;
import com.mysms.lms.api.model.LearningProgramMemberUpdate;
import com.mysms.lms.api.model.LearningProgramUpdate;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.LearningProgramCourseEntity;
import com.mysms.lms.domain.LearningProgramEntity;
import com.mysms.lms.domain.LearningProgramMemberEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.LearningProgramCourseMapper;
import com.mysms.lms.mapper.LearningProgramMapper;
import com.mysms.lms.mapper.LearningProgramMemberMapper;
import com.mysms.lms.repository.LearningProgramCourseRepository;
import com.mysms.lms.repository.LearningProgramMemberRepository;
import com.mysms.lms.repository.LearningProgramRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.learning_program (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a LearningProgram
 * are served here too — LearningProgramCourse, LearningProgramMember.
 */
@Service
public class LearningProgramService extends AppTransactionManager {

    public static final String AGGREGATE = LearningProgramEntity.TABLE;
    static final String PERMISSION_CREATE = "lms.learning_program.create";
    static final String PERMISSION_UPDATE = "lms.learning_program.update";
    static final String PERMISSION_DELETE = "lms.learning_program.delete";
    /** The contract's LearningProgramUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("title", "isPublished", "enforceCourseOrder", "courseCount", "memberCount");

    public static final String LEARNING_PROGRAM_COURSE_AGGREGATE = LearningProgramCourseEntity.TABLE;
    static final String LEARNING_PROGRAM_COURSE_PERMISSION_LIST = "lms.learning_program_course.read";
    static final String LEARNING_PROGRAM_COURSE_PERMISSION_GET = "lms.learning_program_course.read";
    static final String LEARNING_PROGRAM_COURSE_PERMISSION_CREATE = "lms.learning_program_course.create";
    static final String LEARNING_PROGRAM_COURSE_PERMISSION_UPDATE = "lms.learning_program_course.update";
    static final String LEARNING_PROGRAM_COURSE_PERMISSION_DELETE = "lms.learning_program_course.delete";
    /** The contract's LearningProgramCourseUpdate fields, in apply order. */
    static final List<String> LEARNING_PROGRAM_COURSE_WRITABLE = List.of("courseId", "sortOrder");

    public static final String LEARNING_PROGRAM_MEMBER_AGGREGATE = LearningProgramMemberEntity.TABLE;
    static final String LEARNING_PROGRAM_MEMBER_PERMISSION_LIST = "lms.learning_program_member.read";
    static final String LEARNING_PROGRAM_MEMBER_PERMISSION_GET = "lms.learning_program_member.read";
    static final String LEARNING_PROGRAM_MEMBER_PERMISSION_CREATE = "lms.learning_program_member.create";
    static final String LEARNING_PROGRAM_MEMBER_PERMISSION_UPDATE = "lms.learning_program_member.update";
    static final String LEARNING_PROGRAM_MEMBER_PERMISSION_DELETE = "lms.learning_program_member.delete";
    /** The contract's LearningProgramMemberUpdate fields, in apply order. */
    static final List<String> LEARNING_PROGRAM_MEMBER_WRITABLE = List.of("userId", "progressPercent");

    private final LearningProgramRepository repository;
    private final LearningProgramMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final LearningProgramCourseRepository learningProgramCourseRepository;
    private final LearningProgramCourseMapper learningProgramCourseMapper;
    private final LearningProgramMemberRepository learningProgramMemberRepository;
    private final LearningProgramMemberMapper learningProgramMemberMapper;

    public LearningProgramService(LearningProgramRepository repository,
            LearningProgramMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            LearningProgramCourseRepository learningProgramCourseRepository,
            LearningProgramCourseMapper learningProgramCourseMapper,
            LearningProgramMemberRepository learningProgramMemberRepository,
            LearningProgramMemberMapper learningProgramMemberMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.learningProgramCourseRepository = learningProgramCourseRepository;
        this.learningProgramCourseMapper = learningProgramCourseMapper;
        this.learningProgramMemberRepository = learningProgramMemberRepository;
        this.learningProgramMemberMapper = learningProgramMemberMapper;
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public LearningProgram createLearningProgram(LearningProgram request, boolean validateOnly) {
        validator.validate(request);
        LearningProgramEntity entity = mapper.toEntity(request);
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
            LearningProgramEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public LearningProgram updateLearningProgram(UUID id, String ifMatch, LearningProgramUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            LearningProgramEntity entity = loadVisible(scope, id);
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
    public void deleteLearningProgram(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            LearningProgramEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByContentAuthorLearningProgramId(id)) {
                throw new InvalidStateException(
                        "The learning program is still referenced by lms.content_author rows; remove those first.");
            }
            if (repository.referencedByLearningProgramCourseLearningProgramId(id)) {
                throw new InvalidStateException(
                        "The learning program is still referenced by lms.learning_program_course rows; remove those "
                                + "first.");
            }
            if (repository.referencedByLearningProgramMemberLearningProgramId(id)) {
                throw new InvalidStateException(
                        "The learning program is still referenced by lms.learning_program_member rows; remove those "
                                + "first.");
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
    private LearningProgramEntity loadVisible(DataScope scope, UUID id) {
        LearningProgramEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOf(LearningProgramEntity entity) {
        return Optional.empty();
    }

    /**
     * Applies the selected LearningProgramUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void apply(LearningProgramEntity entity, LearningProgramUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "title" -> entity.setTitle(changes.getTitle());
                case "isPublished" -> entity.setIsPublished(changes.getIsPublished());
                case "enforceCourseOrder" -> entity.setEnforceCourseOrder(changes.getEnforceCourseOrder());
                case "courseCount" -> entity.setCourseCount(changes.getCourseCount());
                case "memberCount" -> entity.setMemberCount(changes.getMemberCount());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(LearningProgramEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsPublished() == null) {
            errors.add(new FieldError("isPublished", "must not be null", "NotNull"));
        }
        if (!creating && entity.getEnforceCourseOrder() == null) {
            errors.add(new FieldError("enforceCourseOrder", "must not be null", "NotNull"));
        }
        if (!creating && entity.getCourseCount() == null) {
            errors.add(new FieldError("courseCount", "must not be null", "NotNull"));
        }
        if (!creating && entity.getMemberCount() == null) {
            errors.add(new FieldError("memberCount", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(LearningProgramEntity entity) {
        if (repository.takenTitle1(entity)) {
            throw new DuplicateResourceException("A learning program with the same title already exists.");
        }
    }

    // ---- LearningProgramCourse: a record of the LearningProgram aggregate ----

    public LearningProgramCourse getLearningProgramCourse(UUID learningProgramId, UUID id) {
        return doInReadTransaction(() -> {
            return learningProgramCourseMapper.toDto(loadVisibleLearningProgramCourse(DataScope.current(
                    LEARNING_PROGRAM_COURSE_PERMISSION_GET), learningProgramId, id));
        });
    }

    public Page<LearningProgramCourse> listLearningProgramCourses(UUID learningProgramId, Integer pageNumber,
            Integer pageSize, String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, LearningProgramCourseRepository.SORT_FIELDS,
                        LearningProgramCourseRepository.DEFAULT_ORDER,
                LearningProgramCourseRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, LearningProgramCourseRepository.FILTER_FIELDS)
                .and(LearningProgramCourseRepository.ofParent(learningProgramId))
                        .and(LearningProgramCourseRepository.scope(DataScope.current(LEARNING_PROGRAM_COURSE_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + learningProgramId, clock);
        return doInReadTransaction(() -> {
            Page<LearningProgramCourseEntity> rows = learningProgramCourseRepository.list(page, where);
            return rows.map(learningProgramCourseMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public LearningProgramCourse createLearningProgramCourse(UUID learningProgramId, LearningProgramCourse request,
            boolean validateOnly) {
        validator.validate(request);
        LearningProgramCourseEntity entity = learningProgramCourseMapper.toEntity(request);
        entity.setLearningProgramId(learningProgramId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(LEARNING_PROGRAM_COURSE_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!learningProgramCourseRepository.existsLearningProgramId(entity.getLearningProgramId())) {
                throw new ResourceNotFoundException("lms.learning_program", entity.getLearningProgramId());
            }
            assertValidLearningProgramCourse(entity, true);
            scope.requireWritable(institutionOfLearningProgramCourse(entity));
            assertUniqueLearningProgramCourse(entity);
            if (validateOnly) {
                return learningProgramCourseMapper.toDto(entity);
            }
            LearningProgramCourseEntity stored = learningProgramCourseRepository.insert(entity);
            audit.recordCreate(LEARNING_PROGRAM_COURSE_AGGREGATE, stored.getId());
            outbox.publish(LEARNING_PROGRAM_COURSE_AGGREGATE + LmsEvents.CREATED, LEARNING_PROGRAM_COURSE_AGGREGATE,
                    stored.getId(), LmsEvents.payload(stored.getId()));
            return learningProgramCourseMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public LearningProgramCourse updateLearningProgramCourse(UUID learningProgramId, UUID id, String ifMatch,
            LearningProgramCourseUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, LEARNING_PROGRAM_COURSE_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(LEARNING_PROGRAM_COURSE_PERMISSION_UPDATE);
            LearningProgramCourseEntity entity = loadVisibleLearningProgramCourse(scope, learningProgramId, id);
            scope.requireWritable(institutionOfLearningProgramCourse(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyLearningProgramCourse(entity, changes, fields);
            validator.validate(learningProgramCourseMapper.toDto(entity));
            assertValidLearningProgramCourse(entity, false);
            scope.requireWritable(institutionOfLearningProgramCourse(entity));
            assertUniqueLearningProgramCourse(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return learningProgramCourseMapper.toDto(entity);
            }
            if (learningProgramCourseRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(LEARNING_PROGRAM_COURSE_AGGREGATE, id, fields);
            outbox.publish(LEARNING_PROGRAM_COURSE_AGGREGATE + LmsEvents.UPDATED, LEARNING_PROGRAM_COURSE_AGGREGATE, id,
                    LmsEvents.payload(id));
            return learningProgramCourseMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteLearningProgramCourse(UUID learningProgramId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(LEARNING_PROGRAM_COURSE_PERMISSION_DELETE);
            LearningProgramCourseEntity entity = loadVisibleLearningProgramCourse(scope, learningProgramId, id);
            scope.requireWritable(institutionOfLearningProgramCourse(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (learningProgramCourseRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(LEARNING_PROGRAM_COURSE_AGGREGATE, id);
            outbox.publish(LEARNING_PROGRAM_COURSE_AGGREGATE + LmsEvents.DELETED, LEARNING_PROGRAM_COURSE_AGGREGATE, id,
                    LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private LearningProgramCourseEntity loadVisibleLearningProgramCourse(DataScope scope, UUID learningProgramId, UUID id) {
        LearningProgramCourseEntity entity = learningProgramCourseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(LEARNING_PROGRAM_COURSE_AGGREGATE, id));
        if (!scope.canRead(institutionOfLearningProgramCourse(entity))
                || !learningProgramId.equals(entity.getLearningProgramId())) {
            throw new ResourceNotFoundException(LEARNING_PROGRAM_COURSE_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the lms.course it belongs to). */
    private Optional<UUID> institutionOfLearningProgramCourse(LearningProgramCourseEntity entity) {
        return learningProgramCourseRepository.institutionOf(entity.getCourseId());
    }

    /**
     * Applies the selected LearningProgramCourseUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void applyLearningProgramCourse(LearningProgramCourseEntity entity, LearningProgramCourseUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "courseId" -> entity.setCourseId(changes.getCourseId());
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidLearningProgramCourse(LearningProgramCourseEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getLearningProgramId() == null) {
            errors.add(new FieldError("learningProgramId", "must not be null", "NotNull"));
        }
        if (entity.getCourseId() == null) {
            errors.add(new FieldError("courseId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getCourseId() != null && !learningProgramCourseRepository.existsCourseId(entity.getCourseId())) {
            errors.add(new FieldError("courseId", "course does not exist", "NOT_FOUND"));
        }
        if (entity.getLearningProgramId() != null
                && !learningProgramCourseRepository.existsLearningProgramId(entity.getLearningProgramId())) {
            errors.add(new FieldError("learningProgramId", "learning program does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueLearningProgramCourse(LearningProgramCourseEntity entity) {
        if (learningProgramCourseRepository.takenLearningProgramIdCourseId1(entity)) {
            throw new DuplicateResourceException(
                    "A learning program course with the same learning program id and course id already exists.");
        }
    }

    // ---- LearningProgramMember: a record of the LearningProgram aggregate ----

    public LearningProgramMember getLearningProgramMember(UUID learningProgramId, UUID id) {
        return doInReadTransaction(() -> {
            return learningProgramMemberMapper.toDto(loadVisibleLearningProgramMember(DataScope.current(
                    LEARNING_PROGRAM_MEMBER_PERMISSION_GET), learningProgramId, id));
        });
    }

    public Page<LearningProgramMember> listLearningProgramMembers(UUID learningProgramId, Integer pageNumber,
            Integer pageSize, String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, LearningProgramMemberRepository.SORT_FIELDS,
                        LearningProgramMemberRepository.DEFAULT_ORDER,
                LearningProgramMemberRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, LearningProgramMemberRepository.FILTER_FIELDS)
                .and(LearningProgramMemberRepository.ofParent(learningProgramId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + learningProgramId, clock);
        return doInReadTransaction(() -> {
            Page<LearningProgramMemberEntity> rows = learningProgramMemberRepository.list(page, where);
            return rows.map(learningProgramMemberMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public LearningProgramMember createLearningProgramMember(UUID learningProgramId, LearningProgramMember request,
            boolean validateOnly) {
        validator.validate(request);
        LearningProgramMemberEntity entity = learningProgramMemberMapper.toEntity(request);
        entity.setLearningProgramId(learningProgramId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(LEARNING_PROGRAM_MEMBER_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!learningProgramMemberRepository.existsLearningProgramId(entity.getLearningProgramId())) {
                throw new ResourceNotFoundException("lms.learning_program", entity.getLearningProgramId());
            }
            assertValidLearningProgramMember(entity, true);
            scope.requireWritable(institutionOfLearningProgramMember(entity));
            assertUniqueLearningProgramMember(entity);
            if (validateOnly) {
                return learningProgramMemberMapper.toDto(entity);
            }
            LearningProgramMemberEntity stored = learningProgramMemberRepository.insert(entity);
            audit.recordCreate(LEARNING_PROGRAM_MEMBER_AGGREGATE, stored.getId());
            outbox.publish(LEARNING_PROGRAM_MEMBER_AGGREGATE + LmsEvents.CREATED, LEARNING_PROGRAM_MEMBER_AGGREGATE,
                    stored.getId(), LmsEvents.payload(stored.getId()));
            return learningProgramMemberMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public LearningProgramMember updateLearningProgramMember(UUID learningProgramId, UUID id, String ifMatch,
            LearningProgramMemberUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, LEARNING_PROGRAM_MEMBER_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(LEARNING_PROGRAM_MEMBER_PERMISSION_UPDATE);
            LearningProgramMemberEntity entity = loadVisibleLearningProgramMember(scope, learningProgramId, id);
            scope.requireWritable(institutionOfLearningProgramMember(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyLearningProgramMember(entity, changes, fields);
            validator.validate(learningProgramMemberMapper.toDto(entity));
            assertValidLearningProgramMember(entity, false);
            scope.requireWritable(institutionOfLearningProgramMember(entity));
            assertUniqueLearningProgramMember(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return learningProgramMemberMapper.toDto(entity);
            }
            if (learningProgramMemberRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(LEARNING_PROGRAM_MEMBER_AGGREGATE, id, fields);
            outbox.publish(LEARNING_PROGRAM_MEMBER_AGGREGATE + LmsEvents.UPDATED, LEARNING_PROGRAM_MEMBER_AGGREGATE, id,
                    LmsEvents.payload(id));
            return learningProgramMemberMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteLearningProgramMember(UUID learningProgramId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(LEARNING_PROGRAM_MEMBER_PERMISSION_DELETE);
            LearningProgramMemberEntity entity = loadVisibleLearningProgramMember(scope, learningProgramId, id);
            scope.requireWritable(institutionOfLearningProgramMember(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (learningProgramMemberRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(LEARNING_PROGRAM_MEMBER_AGGREGATE, id);
            outbox.publish(LEARNING_PROGRAM_MEMBER_AGGREGATE + LmsEvents.DELETED, LEARNING_PROGRAM_MEMBER_AGGREGATE, id,
                    LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private LearningProgramMemberEntity loadVisibleLearningProgramMember(DataScope scope, UUID learningProgramId, UUID id) {
        LearningProgramMemberEntity entity = learningProgramMemberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(LEARNING_PROGRAM_MEMBER_AGGREGATE, id));
        if (!scope.canRead(institutionOfLearningProgramMember(entity))
                || !learningProgramId.equals(entity.getLearningProgramId())) {
            throw new ResourceNotFoundException(LEARNING_PROGRAM_MEMBER_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOfLearningProgramMember(LearningProgramMemberEntity entity) {
        return Optional.empty();
    }

    /**
     * Applies the selected LearningProgramMemberUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void applyLearningProgramMember(LearningProgramMemberEntity entity, LearningProgramMemberUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "userId" -> entity.setUserId(changes.getUserId());
                case "progressPercent" -> entity.setProgressPercent(WireValues.decimal(changes.getProgressPercent()));
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidLearningProgramMember(LearningProgramMemberEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getLearningProgramId() == null) {
            errors.add(new FieldError("learningProgramId", "must not be null", "NotNull"));
        }
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getProgressPercent() == null) {
            errors.add(new FieldError("progressPercent", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("progressPercent", WireValues.decimal(entity.getProgressPercent()), 5, 2, errors);
        if (entity.getLearningProgramId() != null
                && !learningProgramMemberRepository.existsLearningProgramId(entity.getLearningProgramId())) {
            errors.add(new FieldError("learningProgramId", "learning program does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueLearningProgramMember(LearningProgramMemberEntity entity) {
        if (learningProgramMemberRepository.takenLearningProgramIdUserId1(entity)) {
            throw new DuplicateResourceException(
                    "A learning program member with the same learning program id and user id already exists.");
        }
    }
}
