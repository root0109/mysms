package com.mysms.lms.business;

import java.util.Set;
import java.util.Objects;
import java.util.HashSet;
import com.mysms.core.context.CallContextHolder;
import java.time.ZoneOffset;
import java.time.LocalDate;
import java.math.BigDecimal;
import com.mysms.lms.repository.LmsCourseRepository;
import com.mysms.lms.repository.LiveClassRepository;
import com.mysms.lms.repository.LearningActivityRepository;
import com.mysms.lms.repository.CohortEnrollmentRepository;
import com.mysms.lms.mapper.LiveClassMapper;
import com.mysms.lms.mapper.CohortEnrollmentMapper;
import com.mysms.lms.domain.LmsCourseEntity;
import com.mysms.lms.api.model.StudentDashboard;
import com.mysms.lms.api.model.LmsStatistics;
import com.mysms.lms.api.model.DailyCount;
import com.mysms.core.error.ConflictException;
import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.lms.api.model.Enrollment;
import com.mysms.lms.api.model.EnrollmentUpdate;
import com.mysms.core.context.Actor;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
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
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.EnrollmentEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.EnrollmentMapper;
import com.mysms.lms.repository.EnrollmentRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.enrollment: audit and domain event commit in the same transaction as every change. Rows
 * outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class EnrollmentService extends AppTransactionManager {

    public static final String AGGREGATE = EnrollmentEntity.TABLE;
    static final String PERMISSION_LIST = "lms.enrollment.read";
    static final String PERMISSION_GET = "lms.enrollment.read";
    static final String PERMISSION_UPDATE = "lms.enrollment.update";
    static final String PERMISSION_DELETE = "lms.enrollment.delete";
    /** How much a dashboard lists of each kind, and how far back its activity reaches. */
    static final int DASHBOARD_ROWS = 50;
    static final int DASHBOARD_LIVE_CLASSES = 10;
    static final int HEATMAP_DAYS = 365;
    /** The contract's EnrollmentUpdate fields, in apply order. */
    static final List<String> WRITABLE = List.of("memberRole");


    private final EnrollmentRepository repository;
    private final EnrollmentMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final LmsCourseRepository courses;
    private final CohortEnrollmentRepository cohortEnrollments;
    private final CohortEnrollmentMapper cohortEnrollmentMapper;
    private final LiveClassRepository liveClasses;
    private final LiveClassMapper liveClassMapper;
    private final LearningActivityRepository activity;
    private final PublicReads publicReads;

    public EnrollmentService(EnrollmentRepository repository, EnrollmentMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox, @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock,
            LmsCourseRepository courses,
            CohortEnrollmentRepository cohortEnrollments,
            CohortEnrollmentMapper cohortEnrollmentMapper,
            LiveClassRepository liveClasses,
            LiveClassMapper liveClassMapper,
            LearningActivityRepository activity, PublicReads publicReads) {

        super(database, transactions);
        this.publicReads = publicReads;
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.courses = courses;
        this.cohortEnrollments = cohortEnrollments;
        this.cohortEnrollmentMapper = cohortEnrollmentMapper;
        this.liveClasses = liveClasses;
        this.liveClassMapper = liveClassMapper;
        this.activity = activity;
    }

    public Enrollment getEnrollment(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Enrollment> listEnrollments(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, EnrollmentRepository.SORT_FIELDS, EnrollmentRepository.DEFAULT_ORDER,
                EnrollmentRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, EnrollmentRepository.FILTER_FIELDS)
                .and(EnrollmentRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<EnrollmentEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public Enrollment updateEnrollment(UUID id, String ifMatch, EnrollmentUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            EnrollmentEntity entity = loadVisible(scope, id);
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
    public void deleteEnrollment(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            EnrollmentEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
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
    private EnrollmentEntity loadVisible(DataScope scope, UUID id) {
        EnrollmentEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.course the row belongs to. */
    private Optional<UUID> institutionOf(EnrollmentEntity entity) {
        return repository.institutionOf(entity.getCourseId());
    }

    /** Applies the selected EnrollmentUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(EnrollmentEntity entity, EnrollmentUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "memberRole" -> entity.setMemberRole(Optional.ofNullable(changes.getMemberRole())
                        .map(v -> v.getValue()).orElse(null));
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(EnrollmentEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCourseId() == null) {
            errors.add(new FieldError("courseId", "must not be null", "NotNull"));
        }
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getMemberRole() == null) {
            errors.add(new FieldError("memberRole", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("progressPercent", WireValues.decimal(entity.getProgressPercent()), 5, 2, errors);
        if (entity.getCertificateId() != null && !repository.existsCertificateId(entity.getCertificateId())) {
            errors.add(new FieldError("certificateId", "certificate does not exist", "NOT_FOUND"));
        }
        if (entity.getCourseId() != null && !repository.existsCourseId(entity.getCourseId())) {
            errors.add(new FieldError("courseId", "course does not exist", "NOT_FOUND"));
        }
        if (entity.getCurrentLessonId() != null && !repository.existsCurrentLessonId(entity.getCurrentLessonId())) {
            errors.add(new FieldError("currentLessonId", "lesson does not exist", "NOT_FOUND"));
        }
        if (entity.getSourceCohortId() != null && !repository.existsSourceCohortId(entity.getSourceCohortId())) {
            errors.add(new FieldError("sourceCohortId", "cohort does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(EnrollmentEntity entity) {
        if (repository.takenCourseIdUserId1(entity)) {
            throw new DuplicateResourceException("A enrollment with the same course id and user id already exists.");
        }
    }

    // ---- the learner's own: enrolling, the dashboard; the catalogue's statistics ----

    /**
     * lms.EnrollInCourse: self-enrolment in a published, active, self-learning course. A paid course is 409
     * PAYMENT_REQUIRED because payment cannot be checked yet; a repeat is 409 DUPLICATE, decided by the unique index
     * when two race.
     */
    public Enrollment enrollInCourse(UUID courseId) {
        UUID user = AuthContext.require().userId();
        EnrollmentEntity entity = new EnrollmentEntity();
        entity.setCourseId(courseId);
        entity.setUserId(user);
        entity.setMemberType("student");
        entity.setMemberRole("member");
        entity.setProgressPercent(BigDecimal.ZERO);
        entity.setPurchasedCertificate(false);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        return doInWriteTransaction(() -> {
            LmsCourseEntity course = courses.findById(courseId)
                    .filter(c -> Boolean.TRUE.equals(c.getIsPublished()) && "published".equals(c.getLifecycleStatus()))
                    .orElseThrow(() -> new ResourceNotFoundException(LmsCourseEntity.TABLE, courseId));
            if (Boolean.TRUE.equals(course.getDisableSelfLearning())) {
                throw new ConflictException("SELF_LEARNING_DISABLED", "The course is taught in cohorts only.");
            }
            if (Boolean.TRUE.equals(course.getIsPaid())) {
                throw new ConflictException("PAYMENT_REQUIRED", "The course is paid; enrolment follows its payment.");
            }
            if (repository.takenCourseIdUserId1(entity)) {
                throw new DuplicateResourceException("The caller is already enrolled in this course.");
            }
            EnrollmentEntity stored = repository.insert(entity);
            courses.countEnrollment(courseId);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * lms.GetStudentDashboard: the caller's own course and cohort enrolments, where to continue (the lesson they were
     * last at in their most recently active course), their cohorts' coming live classes, and their activity — the
     * days of the last year they learned on, the current streak (ending today or yesterday) and the longest one.
     */
    public StudentDashboard getStudentDashboard() {
        UUID user = AuthContext.require().userId();
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        return doInReadTransaction(() -> {
            List<EnrollmentEntity> own = repository.listOwn(user, DASHBOARD_ROWS);
            List<LearningActivityRepository.Day> days = activity.activeDays(user, today.minusDays(HEATMAP_DAYS - 1));
            StudentDashboard dashboard = new StudentDashboard(currentStreak(days, today), longestStreak(days));
            dashboard.setEnrollments(own.stream().map(mapper::toDto).toList());
            dashboard.setContinueLessonId(own.stream().map(EnrollmentEntity::getCurrentLessonId)
                    .filter(Objects::nonNull).findFirst().orElse(null));
            dashboard.setCohortEnrollments(cohortEnrollments.ownActive(user, DASHBOARD_ROWS).stream()
                    .map(cohortEnrollmentMapper::toDto).toList());
            dashboard.setUpcomingLiveClasses(liveClasses.upcomingForStudent(user, today, DASHBOARD_LIVE_CLASSES)
                    .stream().map(liveClassMapper::toDto).toList());
            dashboard.setActivityHeatmap(days.stream().map(d -> new DailyCount(d.date(), d.count())).toList());
            return dashboard;
        });
    }

    /** Consecutive active days ending today — or yesterday, when today has no activity yet. */
    static int currentStreak(List<LearningActivityRepository.Day> days, LocalDate today) {
        Set<LocalDate> active = new HashSet<>();
        days.forEach(d -> active.add(d.date()));
        LocalDate at = active.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (active.contains(at)) {
            streak++;
            at = at.minusDays(1);
        }
        return streak;
    }

    /** The longest run of consecutive active days (the days come in date order). */
    static int longestStreak(List<LearningActivityRepository.Day> days) {
        int longest = 0;
        int run = 0;
        LocalDate previous = null;
        for (LearningActivityRepository.Day day : days) {
            run = previous != null && previous.plusDays(1).equals(day.date()) ? run + 1 : 1;
            longest = Math.max(longest, run);
            previous = day.date();
        }
        return longest;
    }

    /**
     * lms.GetLmsStatistics: the public figures of the tenant's catalogue — published courses, students, enrolments,
     * certificates issued and the share of enrolments completed. Public: a guest names the school (404 otherwise).
     */
    public LmsStatistics getLmsStatistics() {
        publicReads.requireTenant();
        return doInReadTransaction(() -> {
            LearningActivityRepository.Statistics figures = activity.statistics();
            return new LmsStatistics(figures.publishedCourses(), figures.students(), figures.enrollments(),
                    figures.certificatesIssued(), WireValues.decimal(figures.completionPercent()));
        });
    }
}
