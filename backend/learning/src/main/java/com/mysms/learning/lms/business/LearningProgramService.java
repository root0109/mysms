package com.mysms.learning.lms.business;

import java.math.BigDecimal;
import com.mysms.authclient.AuthContext;
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
import com.mysms.learning.lms.api.model.LearningProgram;
import com.mysms.learning.lms.api.model.LearningProgramCourse;
import com.mysms.learning.lms.api.model.LearningProgramCourseUpdate;
import com.mysms.learning.lms.api.model.LearningProgramMember;
import com.mysms.learning.lms.api.model.LearningProgramMemberUpdate;
import com.mysms.learning.lms.api.model.LearningProgramUpdate;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.config.LmsEventsConfig;
import com.mysms.learning.lms.domain.LearningProgramCourseEntity;
import com.mysms.learning.lms.domain.LearningProgramEntity;
import com.mysms.learning.lms.domain.LearningProgramMemberEntity;
import com.mysms.learning.lms.events.LmsEvents;
import com.mysms.learning.lms.mapper.LearningProgramCourseMapper;
import com.mysms.learning.lms.mapper.LearningProgramMapper;
import com.mysms.learning.lms.mapper.LearningProgramMemberMapper;
import com.mysms.learning.lms.repository.LearningProgramCourseRepository;
import com.mysms.learning.lms.repository.LearningProgramMemberRepository;
import com.mysms.learning.lms.repository.LearningProgramRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.learning_program and of the records that exist only as part of a LearningProgram
 * (LearningProgramCourse, LearningProgramMember): audit and domain event commit in the same transaction as every
 * change. Rows outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide
 * grant.
 */
@Service
public class LearningProgramService extends AppTransactionManager {

    public static final String AGGREGATE = LearningProgramEntity.TABLE;
    static final String PERMISSION_READ = "lms.learning_program.read";
    static final String PERMISSION_ENROLL = "lms.learning_program.enroll_in_learning_program";
    static final String PERMISSION_CREATE = "lms.learning_program.create";
    static final String PERMISSION_UPDATE = "lms.learning_program.update";
    static final String PERMISSION_DELETE = "lms.learning_program.delete";

    public static final String LEARNING_PROGRAM_COURSE_AGGREGATE = LearningProgramCourseEntity.TABLE;
    static final String LEARNING_PROGRAM_COURSE_PERMISSION_LIST = "lms.learning_program_course.read";
    static final String LEARNING_PROGRAM_COURSE_PERMISSION_GET = "lms.learning_program_course.read";
    static final String LEARNING_PROGRAM_COURSE_PERMISSION_CREATE = "lms.learning_program_course.create";
    static final String LEARNING_PROGRAM_COURSE_PERMISSION_UPDATE = "lms.learning_program_course.update";
    static final String LEARNING_PROGRAM_COURSE_PERMISSION_DELETE = "lms.learning_program_course.delete";

    public static final String LEARNING_PROGRAM_MEMBER_AGGREGATE = LearningProgramMemberEntity.TABLE;
    static final String LEARNING_PROGRAM_MEMBER_PERMISSION_LIST = "lms.learning_program_member.read";
    static final String LEARNING_PROGRAM_MEMBER_PERMISSION_GET = "lms.learning_program_member.read";
    static final String LEARNING_PROGRAM_MEMBER_PERMISSION_CREATE = "lms.learning_program_member.create";
    static final String LEARNING_PROGRAM_MEMBER_PERMISSION_UPDATE = "lms.learning_program_member.update";
    static final String LEARNING_PROGRAM_MEMBER_PERMISSION_DELETE = "lms.learning_program_member.delete";

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
    private final PublicReads publicReads;

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
            LearningProgramMemberMapper learningProgramMemberMapper, PublicReads publicReads) {
        super(database, transactions);
        this.publicReads = publicReads;
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
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public LearningProgram updateLearningProgram(UUID id, String ifMatch, LearningProgramUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            LearningProgramEntity entity = loadVisible(scope, id);
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

    /** A learning program: every program to a member holding lms.learning_program.read, published ones to the public. */
    public LearningProgram getLearningProgram(UUID id) {
        boolean member = publicReads.asMember(PERMISSION_READ);
        return doInReadTransaction(() -> repository.findById(id)
                .filter(e -> member || Boolean.TRUE.equals(e.getIsPublished()))
                .map(e -> member ? mapper.toDto(e) : guestProjection(mapper.toDto(e)))
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
    }

    /** The tenant's learning programs, page by page; published ones only to the public. */
    public Page<LearningProgram> listLearningPrograms(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        boolean member = publicReads.asMember(PERMISSION_READ);
        SortSpec sort = SortSpec.parse(sortBy, LearningProgramRepository.SORT_FIELDS,
                LearningProgramRepository.DEFAULT_ORDER, LearningProgramRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, LearningProgramRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        if (member) {
            return doInReadTransaction(() -> repository.list(page, where).map(mapper::toDto));
        }
        return doInReadTransaction(() -> repository.list(page, where.and(LearningProgramRepository.PUBLISHED))
                .map(e -> guestProjection(mapper.toDto(e))));
    }

    /** The public's view of a program: who created and changed it is internal. */
    private static LearningProgram guestProjection(LearningProgram program) {
        program.setCreatedBy(null);
        program.setUpdatedBy(null);
        return program;
    }

    /**
     * lms.EnrollInLearningProgram: the caller joins a published program; a repeat is 409 DUPLICATE, decided by the
     * unique index when two race. Joining does not enrol in the program's courses.
     */
    public LearningProgramMember enrollInLearningProgram(UUID id) {
        UUID user = AuthContext.require().userId();
        LearningProgramMemberEntity entity = new LearningProgramMemberEntity();
        entity.setLearningProgramId(id);
        entity.setUserId(user);
        entity.setProgressPercent(BigDecimal.ZERO);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        return doInWriteTransaction(() -> {
            LearningProgramEntity program = repository.findById(id)
                    .filter(e -> Boolean.TRUE.equals(e.getIsPublished()))
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (learningProgramMemberRepository.takenLearningProgramIdUserId1(entity)) {
                throw new DuplicateResourceException("The caller is already a member of this learning program.");
            }
            LearningProgramMemberEntity stored = learningProgramMemberRepository.insert(entity);
            repository.countMember(program.getId());
            audit.recordCreate(LEARNING_PROGRAM_MEMBER_AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + ".enroll", AGGREGATE, id, LmsEvents.payload(id));
            return learningProgramMemberMapper.toDto(stored);
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

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOf(LearningProgramEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(LearningProgramEntity entity, LearningProgramUpdate changes) {
        Patches patch = new Patches();
        patch.apply("title", changes.getTitle(), entity::setTitle);
        patch.apply("isPublished", changes.getIsPublished(), entity::setIsPublished);
        patch.apply("enforceCourseOrder", changes.getEnforceCourseOrder(), entity::setEnforceCourseOrder);
        patch.apply("courseCount", changes.getCourseCount(), entity::setCourseCount);
        patch.apply("memberCount", changes.getMemberCount(), entity::setMemberCount);
        return patch.applied();
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
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public LearningProgramCourse updateLearningProgramCourse(UUID learningProgramId, UUID id, String ifMatch,
            LearningProgramCourseUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(LEARNING_PROGRAM_COURSE_PERMISSION_UPDATE);
            LearningProgramCourseEntity entity = loadVisibleLearningProgramCourse(scope, learningProgramId, id);
            scope.requireWritable(institutionOfLearningProgramCourse(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyLearningProgramCourse(entity, changes);
            validator.validate(learningProgramCourseMapper.toDto(entity));
            assertValidLearningProgramCourse(entity, false);
            scope.requireWritable(institutionOfLearningProgramCourse(entity));
            assertUniqueLearningProgramCourse(entity);
            if (fields.isEmpty()) {
                return learningProgramCourseMapper.toDto(entity);
            }
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

    /** The institution of the lms.course the row belongs to. */
    private Optional<UUID> institutionOfLearningProgramCourse(LearningProgramCourseEntity entity) {
        return learningProgramCourseRepository.institutionOf(entity.getCourseId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyLearningProgramCourse(LearningProgramCourseEntity entity,
            LearningProgramCourseUpdate changes) {
        Patches patch = new Patches();
        patch.apply("courseId", changes.getCourseId(), entity::setCourseId);
        patch.apply("sortOrder", changes.getSortOrder(), entity::setSortOrder);
        return patch.applied();
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
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public LearningProgramMember updateLearningProgramMember(UUID learningProgramId, UUID id, String ifMatch,
            LearningProgramMemberUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(LEARNING_PROGRAM_MEMBER_PERMISSION_UPDATE);
            LearningProgramMemberEntity entity = loadVisibleLearningProgramMember(scope, learningProgramId, id);
            scope.requireWritable(institutionOfLearningProgramMember(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyLearningProgramMember(entity, changes);
            validator.validate(learningProgramMemberMapper.toDto(entity));
            assertValidLearningProgramMember(entity, false);
            scope.requireWritable(institutionOfLearningProgramMember(entity));
            assertUniqueLearningProgramMember(entity);
            if (fields.isEmpty()) {
                return learningProgramMemberMapper.toDto(entity);
            }
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

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOfLearningProgramMember(LearningProgramMemberEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyLearningProgramMember(LearningProgramMemberEntity entity,
            LearningProgramMemberUpdate changes) {
        Patches patch = new Patches();
        patch.apply("userId", changes.getUserId(), entity::setUserId);
        patch.apply("progressPercent", changes.getProgressPercent(), v -> entity.setProgressPercent(WireValues.decimal(v)));
        return patch.applied();
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
