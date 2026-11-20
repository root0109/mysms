package com.mysms.lms.business;

import com.mysms.authclient.AuthContext;
import com.mysms.core.context.Actor;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.SortSpec;
import com.mysms.lms.api.model.CohortEnrollment;
import com.mysms.lms.api.model.CohortEnrollmentList;
import com.mysms.lms.api.model.CohortPerformance;
import com.mysms.lms.api.model.CohortStudentProgress;
import com.mysms.lms.api.model.CourseProgress;
import com.mysms.lms.api.model.ExportGradesRequest;
import com.mysms.lms.api.model.Gradebook;
import com.mysms.lms.api.model.GradebookCell;
import com.mysms.lms.api.model.GradebookColumn;
import com.mysms.lms.api.model.GradebookRow;
import com.mysms.lms.api.model.GradingCategory;
import com.mysms.lms.api.model.Operation;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.CohortEntity;
import com.mysms.lms.domain.OperationEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.CohortEnrollmentMapper;
import com.mysms.lms.mapper.GradingCategoryMapper;
import com.mysms.lms.mapper.OperationMapper;
import com.mysms.lms.model.CourseProgressRow;
import com.mysms.lms.model.GradeCell;
import com.mysms.lms.model.GradedItem;
import com.mysms.lms.model.RosterRow;
import com.mysms.lms.repository.CohortEnrollmentRepository;
import com.mysms.lms.repository.CohortGradebookRepository;
import com.mysms.lms.repository.CohortMembershipRepository;
import com.mysms.lms.repository.CohortRepository;
import com.mysms.lms.repository.GradingCategoryRepository;
import com.mysms.lms.repository.OperationRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The cohort's grades as its people read them, and the step that turns them into final grades: the gradebook grid, the
 * per-item performance report, each student's progress, and publishing final grades. The arithmetic is {@link
 * GradeCalculator}'s.
 *
 * <p>Who sees what: the cohort's teaching staff see every student; a student sees only their own row and only the
 * entries published to them; anybody else who sees the cohort is refused (403). A student's progress follows the same
 * rule.
 */
@Service
public class CohortGradebookService extends AppTransactionManager {

    static final String PERMISSION_GRADEBOOK = "lms.cohort.get_gradebook";
    static final String PERMISSION_PERFORMANCE = "lms.cohort.get_cohort_performance";
    static final String PERMISSION_PROGRESS = "lms.cohort.get_cohort_progress";
    static final String PERMISSION_PUBLISH_FINAL = "lms.cohort.publish_final_grades";
    static final String PERMISSION_EXPORT = "lms.cohort.export_grades";
    static final String OPERATION_EXPORT = "lms.ExportGrades";
    private static final String ENROLLMENT = "lms.cohort_enrollment";
    private static final int CATEGORY_PAGE = 500;

    private final CohortRepository cohorts;
    private final CohortGradebookRepository grades;
    private final CohortMembershipRepository members;
    private final GradingCategoryRepository categories;
    private final GradingCategoryMapper categoryMapper;
    private final CohortEnrollmentRepository enrollments;
    private final CohortEnrollmentMapper enrollmentMapper;
    private final CohortAccess access;
    private final MemberDirectory directory;
    private final GradingScale scale;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;
    private final OperationRepository operations;
    private final OperationMapper operationMapper;
    private final PlatformFiles files;

    public CohortGradebookService(CohortRepository cohorts, CohortGradebookRepository grades,
            CohortMembershipRepository members, GradingCategoryRepository categories,
            GradingCategoryMapper categoryMapper, CohortEnrollmentRepository enrollments,
            CohortEnrollmentMapper enrollmentMapper, CohortAccess access, MemberDirectory directory, GradingScale scale,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox, @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            Clock clock, OperationRepository operations, OperationMapper operationMapper, PlatformFiles files) {
        super(database, transactions);
        this.operations = operations;
        this.operationMapper = operationMapper;
        this.files = files;
        this.cohorts = cohorts;
        this.grades = grades;
        this.members = members;
        this.categories = categories;
        this.categoryMapper = categoryMapper;
        this.enrollments = enrollments;
        this.enrollmentMapper = enrollmentMapper;
        this.access = access;
        this.directory = directory;
        this.scale = scale;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
    }

    /** The gradebook with the students of one page, and that page (for the navigation links). */
    public record GradebookPage(Gradebook gradebook, Page<GradebookRow> rows) {
    }

    /** What one read of the grades needs from its transaction. */
    private record Grades(CohortEntity cohort, boolean staff, List<GradedItem> items, List<GradingCategory> categories,
            Page<UUID> students, List<GradeCell> cells, Map<UUID, String> finalGrades) {
    }

    /**
     * lms.GetGradebook: graded items × the students of a page, with each student's category percents, weighted total
     * and published final grade. A student sees their own row only, with only their published entries counted.
     */
    public GradebookPage getGradebook(UUID cohortId, UUID userId, UUID categoryId, Integer pageNumber,
            Integer pageSize, String cursor) {
        AuthContext caller = CohortAccess.member();
        PageRequest page = studentPage(pageNumber, pageSize, cursor, cohortId + "|gradebook|" + userId + "|"
                + categoryId);
        Grades read = doInReadTransaction(() -> read(cohortId, PERMISSION_GRADEBOOK, userId, categoryId, page,
                caller));
        Map<UUID, MemberDirectory.Member> names = names(read, caller, "Reading the gradebook");
        Map<UUID, GradeCalculator.Category> weights = weights(read.categories());
        List<GradebookRow> rows = new ArrayList<>();
        for (UUID student : read.students().items()) {
            List<GradeCell> own = read.cells().stream().filter(cell -> cell.userId().equals(student)).toList();
            GradeCalculator.StudentGrade grade = GradeCalculator.studentGrade(read.items(), weights, own);
            GradebookRow row = new GradebookRow(student, names.getOrDefault(student,
                    MemberDirectory.Member.unknown(student)).displayName());
            row.setCells(read.items().stream().map(item -> cell(item, own)).toList());
            Map<String, String> percents = new LinkedHashMap<>();
            grade.categoryPercents().forEach((id, value) -> percents.put(id.toString(), value.toPlainString()));
            row.setCategoryPercents(percents);
            row.setTotalPercent(grade.totalPercent().map(BigDecimal::toPlainString).orElse(null));
            row.setFinalGrade(read.finalGrades().get(student));
            rows.add(row);
        }
        Gradebook gradebook = new Gradebook(cohortId, read.items().stream().map(CohortGradebookService::column).toList(),
                rows, read.cohort().getGradeDisplay());
        gradebook.setCategories(read.categories());
        Page<GradebookRow> rowPage = new Page<>(rows, read.students().page(), read.students().size(),
                read.students().nextCursor());
        return new GradebookPage(gradebook, rowPage);
    }

    /**
     * lms.GetCohortPerformance: per graded item, the average and median percent of its graded entries and how many
     * students submitted, were graded and are missing — for the cohort's teaching staff.
     */
    public Page<CohortPerformance> getCohortPerformance(UUID cohortId, Integer pageNumber, Integer pageSize,
            String cursor) {
        SortSpec sort = SortSpec.parse(null, CohortGradebookRepository.ITEM_SORT, "createTime", "r.id");
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, cohortId + "|performance", clock);
        return doInReadTransaction(() -> {
            access.taught(cohortId, PERMISSION_PERFORMANCE);
            Page<GradedItem> items = grades.gradedItems(page, cohortId);
            List<UUID> students = members.activeStudentIds(cohortId);
            List<GradeCell> cells = grades.cells(cohortId, students,
                    items.items().stream().map(GradedItem::contentItemId).toList());
            return items.map(item -> {
                GradeCalculator.ItemStats stats = GradeCalculator.itemStats(cells.stream()
                        .filter(cell -> cell.contentItemId().equals(item.contentItemId())).toList(), students.size());
                CohortPerformance performance = new CohortPerformance(item.contentItemId(), stats.submitted(),
                        stats.graded(), stats.missing());
                performance.setAveragePercent(text(stats.averagePercent()));
                performance.setMedianPercent(text(stats.medianPercent()));
                return performance;
            });
        });
    }

    /**
     * lms.GetCohortProgress: each student's progress in the cohort's courses and how many of its graded items they
     * have completed (submitted, graded or excused). Staff read every student (or one with userId); a student reads
     * their own.
     */
    public Page<CohortStudentProgress> getCohortProgress(UUID cohortId, UUID userId, Integer pageNumber,
            Integer pageSize, String cursor) {
        AuthContext caller = CohortAccess.member();
        PageRequest page = studentPage(pageNumber, pageSize, cursor, cohortId + "|progress|" + userId);
        return doInReadTransaction(() -> {
            Grades read = read(cohortId, PERMISSION_PROGRESS, userId, null, page, caller);
            List<UUID> students = read.students().items();
            Map<UUID, List<CourseProgress>> courses = new LinkedHashMap<>();
            for (CourseProgressRow row : grades.courseProgress(cohortId, students)) {
                courses.computeIfAbsent(row.userId(), id -> new ArrayList<>())
                        .add(new CourseProgress(row.courseId(), GradeCalculator.shown(row.progressPercent())
                                .toPlainString()));
            }
            return read.students().map(student -> {
                long completed = read.cells().stream().filter(cell -> cell.userId().equals(student))
                        .filter(cell -> !GradeCell.NOT_SUBMITTED.equals(cell.status())
                                && !"reassigned".equals(cell.status()))
                        .count();
                CohortStudentProgress progress = new CohortStudentProgress(student, (int) completed,
                        read.items().size());
                progress.setCourseProgress(courses.getOrDefault(student, List.of()));
                return progress;
            });
        });
    }

    /**
     * lms.PublishFinalGrades: computes each student's total from the gradebook (as {@link #getGradebook} does, from
     * every graded entry) and publishes it as their final grade — the band of the cohort's grading scheme that holds
     * the total (its label and grade point), or the total itself when the cohort has no scheme. For the given students
     * or every active student; a student without any graded work is skipped. The cohort's teaching staff only; an
     * archived cohort is 409. The scheme is read from assessment between the read and the write transactions.
     */
    public CohortEnrollmentList publishFinalGrades(UUID cohortId, List<UUID> userIds) {
        CohortAccess.member();
        Map<UUID, BigDecimal> totals = new LinkedHashMap<>();
        UUID scheme = doInReadTransaction(() -> {
            CohortEntity cohort = access.taught(cohortId, PERMISSION_PUBLISH_FINAL);
            if ("archived".equals(cohort.getLifecycleStatus())) {
                throw new InvalidStateException("An archived cohort is read-only.");
            }
            List<UUID> active = members.activeStudentIds(cohortId);
            List<UUID> chosen = userIds == null || userIds.isEmpty() ? active
                    : userIds.stream().distinct().filter(active::contains).toList();
            List<GradedItem> items = grades.gradedItems(cohortId, null);
            Map<UUID, GradeCalculator.Category> weights = weights(categoriesOf(cohortId));
            List<GradeCell> cells = grades.cells(cohortId, chosen, items.stream().map(GradedItem::contentItemId)
                    .toList());
            for (UUID student : chosen) {
                GradeCalculator.studentGrade(items, weights, cells.stream()
                        .filter(cell -> cell.userId().equals(student)).toList()).totalPercent()
                        .ifPresent(total -> totals.put(student, total));
            }
            return cohort.getGradingSchemeId();
        });
        List<GradingScale.Band> bands = scheme == null || totals.isEmpty() ? List.of()
                : scale.bands(scheme, "Publishing final grades");
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.MICROS);
        return doInWriteTransaction(() -> {
            CohortEntity cohort = access.taught(cohortId, PERMISSION_PUBLISH_FINAL);
            if ("archived".equals(cohort.getLifecycleStatus())) {
                throw new InvalidStateException("An archived cohort is read-only.");
            }
            List<CohortEnrollment> published = new ArrayList<>();
            for (Map.Entry<UUID, BigDecimal> total : totals.entrySet()) {
                Optional<GradingScale.Band> band = GradingScale.bandOf(bands, total.getValue());
                String grade = band.map(GradingScale.Band::label).orElse(total.getValue().toPlainString());
                BigDecimal gpa = band.map(GradingScale.Band::gradePoint).orElse(null);
                for (UUID id : grades.publishFinalGrade(cohortId, total.getKey(), grade, gpa, now,
                        Actor.current().orElse(null))) {
                    audit.recordTransition(ENROLLMENT, id, "publish_final_grade", List.of("finalGrade", "finalGpa",
                            "gradedBy", "gradedAt", "gradePublished", "gradePublishedAt"), null);
                    outbox.publish(ENROLLMENT + ".publish_final_grade", ENROLLMENT, id, LmsEvents.payload(id));
                    published.add(enrollmentMapper.toDto(enrollments.findById(id).orElseThrow()));
                }
            }
            return new CohortEnrollmentList(published);
        });
    }

    // ---- shared reading ----

    /**
     * lms.ExportGrades: the whole gradebook, or one graded item, as a CSV file of the caller (csv is the one format
     * made; 422 otherwise), one line per student and item. Teaching staff only. Answers a finished operation whose
     * result carries the stored file and its download link.
     */
    public Operation exportGrades(UUID cohortId, ExportGradesRequest request) {
        if (request.getFormat() != ExportGradesRequest.FormatEnum.CSV) {
            throw new ValidationFailedException(List.of(new FieldError("format", "this export is made as csv",
                    "AllowedValues")));
        }
        record Export(UUID tenantId, List<GradedItem> items, List<UUID> students, List<GradeCell> cells) {
        }
        Export export = doInReadTransaction(() -> {
            CohortEntity cohort = access.taught(cohortId, PERMISSION_EXPORT);
            List<GradedItem> items = grades.gradedItems(cohortId, null);
            if (request.getContentItemId() != null) {
                items = items.stream().filter(item -> item.contentItemId().equals(request.getContentItemId()))
                        .toList();
                if (items.isEmpty()) {
                    throw new ValidationFailedException(List.of(new FieldError("contentItemId",
                            "is not a graded item of the cohort", "NOT_FOUND")));
                }
            }
            List<UUID> students = members.activeStudentIds(cohortId);
            return new Export(cohort.getTenantId(), items,
                    students, grades.cells(cohortId, students, items.stream().map(GradedItem::contentItemId)
                            .toList()));
        });
        Map<UUID, MemberDirectory.Member> names = directory.lookup(export.students(), "Exporting grades");
        Map<String, GradeCell> byKey = new LinkedHashMap<>();
        export.cells().forEach(cell -> byKey.put(cell.contentItemId() + "|" + cell.userId(), cell));
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("student", "user_id", "item", "kind", "points_possible", "points", "status", "published"));
        for (UUID student : export.students()) {
            for (GradedItem item : export.items()) {
                GradeCell cell = byKey.get(item.contentItemId() + "|" + student);
                BigDecimal possible = cell == null ? item.points() : cell.pointsPossible();
                rows.add(List.of(names.getOrDefault(student, MemberDirectory.Member.unknown(student)).displayName(),
                        student.toString(),
                        item.title() == null ? "" : item.title(), item.itemKind(),
                        possible == null ? "" : possible.toPlainString(),
                        cell == null || cell.points() == null ? "" : cell.points().toPlainString(),
                        cell == null ? GradeCell.NOT_SUBMITTED : cell.status(),
                        String.valueOf(cell != null && cell.isPublished())));
            }
        }
        UUID fileId = files.write("cohort-grades-" + cohortId + ".csv", "text/csv", Csv.write(rows));
        com.mysms.lms.api.model.DownloadLink link = files.link(fileId, "cohortId");
        return doInWriteTransaction(() -> {
            OperationEntity operation = OperationEntity.builder().operationName(OPERATION_EXPORT)
                    .resourceType(CohortAccess.COHORT).resourceId(cohortId).resultEntityType("platform.file_object")
                    .resultEntityId(fileId).startedByUserId(Actor.current().orElse(null)).build();
            operation.onCreatingData(Actor.current(), clock);
            operation.setTenantId(export.tenantId());
            operation.setStartedAt(operation.getCreatedAt());
            operation.setFinishedAt(operation.getCreatedAt());
            Operation done = operationMapper.toDto(operations.insertCompleted(operation));
            done.setResult(Map.of("fileId", fileId.toString(), "url", link.getUrl(),
                    "expiresAt", link.getExpiresAt().toString(), "rows", rows.size() - 1));
            audit.recordCreate("lms.operation", done.getOperationId());
            return done;
        });
    }

    private PageRequest studentPage(Integer pageNumber, Integer pageSize, String cursor, String view) {
        SortSpec sort = SortSpec.parse(null, CohortMembershipRepository.ROSTER_SORT, "createTime", "r.id");
        return PageRequest.of(pageNumber, pageSize, cursor, sort, view, clock);
    }

    /**
     * The grades one caller may read: staff read every active student (or the one asked for), a student their own
     * row (asking for anybody else is 403), with only the entries published to them.
     */
    private Grades read(UUID cohortId, String permission, UUID userId, UUID categoryId, PageRequest page,
            AuthContext caller) {
        CohortEntity cohort = access.visible(cohortId, permission);
        boolean staff = access.isTeachingStaff(cohort);
        Page<UUID> students;
        if (staff) {
            Page<RosterRow> rows = members.roster(page, cohortId, RosterRow.STUDENT, false);
            students = rows.map(RosterRow::userId);
            if (userId != null) {
                List<UUID> one = members.activeStudentIds(cohortId).contains(userId) ? List.of(userId) : List.of();
                students = new Page<>(one, 1, page.size(), "");
            }
        } else if (access.isStudent(cohort) && (userId == null || userId.equals(caller.userId()))) {
            students = new Page<>(List.of(caller.userId()), 1, page.size(), "");
        } else {
            throw new PermissionDeniedException("Only the cohort's teachers read other students' grades.");
        }
        List<GradedItem> items = grades.gradedItems(cohortId, categoryId);
        List<GradeCell> cells = grades.cells(cohortId, students.items(),
                items.stream().map(GradedItem::contentItemId).toList());
        if (!staff) {
            cells = cells.stream().filter(GradeCell::isPublished).toList();
        }
        Map<UUID, String> gradeOf = new HashMap<>();
        enrollments.ofCohortUsers(cohortId, students.items()).stream()
                .filter(row -> staff || Boolean.TRUE.equals(row.getGradePublished()))
                .forEach(row -> gradeOf.put(row.getUserId(), row.getFinalGrade()));
        Map<UUID, String> finals = new LinkedHashMap<>();
        for (UUID student : students.items()) {
            if (gradeOf.containsKey(student)) {
                finals.put(student, gradeOf.get(student));
            }
        }
        return new Grades(cohort, staff, items, categoriesOf(cohortId), students, cells, finals);
    }

    private List<GradingCategory> categoriesOf(UUID cohortId) {
        SortSpec sort = SortSpec.parse(null, GradingCategoryRepository.SORT_FIELDS,
                GradingCategoryRepository.DEFAULT_ORDER, GradingCategoryRepository.ID_COLUMN);
        return categories.list(PageRequest.of(1, CATEGORY_PAGE, null, sort, "cohort|" + cohortId, clock),
                new SqlFragment("r.cohort_id = :gradebookCohortId",
                        Map.of("gradebookCohortId", cohortId)))
                .items().stream().map(categoryMapper::toDto).toList();
    }

    private static Map<UUID, GradeCalculator.Category> weights(List<GradingCategory> categories) {
        Map<UUID, GradeCalculator.Category> weights = new LinkedHashMap<>();
        for (GradingCategory category : categories) {
            String weight = category.getWeightPercent();
            weights.put(category.getId(), new GradeCalculator.Category(category.getId(),
                    weight == null ? null : new BigDecimal(weight),
                    Boolean.TRUE.equals(category.getEqualItemWeighting())));
        }
        return weights;
    }

    /** The students' names: the caller's own from their person record, everybody else's account through iam. */
    private Map<UUID, MemberDirectory.Member> names(Grades read, AuthContext caller, String purpose) {
        List<UUID> students = read.students().items();
        if (students.isEmpty()) {
            return Map.of();
        }
        if (!read.staff()) {
            return Map.of(caller.userId(), directory.self(purpose));
        }
        return directory.lookup(students, purpose);
    }

    private static GradebookColumn column(GradedItem item) {
        GradebookColumn column = new GradebookColumn(item.contentItemId(), item.title(), item.itemKind());
        column.setGradingCategoryId(item.gradingCategoryId());
        column.setPoints(text(item.points()));
        column.setWeightPercent(text(item.weightPercent()));
        column.setDueAt(item.dueAt());
        return column;
    }

    private static GradebookCell cell(GradedItem item, List<GradeCell> own) {
        Optional<GradeCell> entry = own.stream().filter(cell -> cell.contentItemId().equals(item.contentItemId()))
                .findFirst();
        GradebookCell cell = new GradebookCell(item.contentItemId(),
                entry.map(GradeCell::status).orElse(GradeCell.NOT_SUBMITTED),
                entry.map(GradeCell::isPublished).orElse(false));
        entry.ifPresent(found -> {
            cell.setEntryId(found.entryId());
            cell.setPoints(text(found.points()));
        });
        return cell;
    }

    private static String text(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }
}
