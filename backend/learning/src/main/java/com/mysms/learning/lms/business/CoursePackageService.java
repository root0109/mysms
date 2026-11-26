package com.mysms.learning.lms.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ConflictException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.id.UuidV7;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.learning.lms.api.model.DownloadLink;
import com.mysms.learning.lms.api.model.ImportCoursePackageRequest;
import com.mysms.learning.lms.api.model.ImportExportJob;
import com.mysms.learning.lms.api.model.Operation;
import com.mysms.learning.lms.api.model.RestoreCohortArchiveRequest;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.config.LmsEventsConfig;
import com.mysms.learning.lms.domain.ImportExportJobEntity;
import com.mysms.learning.lms.events.LmsEvents;
import com.mysms.learning.lms.mapper.ImportExportJobMapper;
import com.mysms.learning.lms.model.CoursePackage;
import com.mysms.learning.lms.repository.CoursePackageRepository;
import com.mysms.learning.lms.repository.CoursePackageRepository.CourseHead;
import com.mysms.learning.lms.repository.CoursePackageRepository.JobHead;
import com.mysms.learning.lms.repository.ImportExportJobRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Course packages and their jobs (lms.import_export_job): ImportCoursePackage and ExportCourse as long-running
 * operations answered 202, GetJobPackage and RestoreCohortArchive.
 *
 * <p>An import reads the caller's platform {@code .zip} (at most {@value #MAX_PACKAGE_MB} MB) and checks it whole
 * first ({@link CoursePackageCodec}): a package with problems imports nothing ({@code PACKAGE_INVALID}, every problem
 * in the job's error log), and a sound one is written in one transaction into the course template, so a failure
 * leaves nothing half imported. An export writes the template as a native package into a new platform file of the
 * caller.
 */
@Service
public class CoursePackageService extends AppTransactionManager {

    private static final Logger LOG = LoggerFactory.getLogger(CoursePackageService.class);
    static final String JOB_AGGREGATE = ImportExportJobEntity.TABLE;
    static final String KIND_IMPORT = "lms.ImportCoursePackage";
    static final String KIND_EXPORT = "lms.ExportCourse";
    static final String PERMISSION_IMPORT = "lms.course.import_course_package";
    static final String PERMISSION_EXPORT = "lms.course.export_course";
    static final String PERMISSION_PACKAGE = "lms.import_export_job.get_job_package";
    static final String PERMISSION_RESTORE = "lms.import_export_job.restore_cohort_archive";
    static final int MAX_PACKAGE_MB = 50;

    private final CoursePackageRepository repository;
    private final ImportExportJobRepository jobs;
    private final ImportExportJobMapper jobMapper;
    private final CoursePackageCodec codec;
    private final QuestionBankWriter questionWriter;
    private final PlatformFiles files;
    private final OperationService operations;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public CoursePackageService(CoursePackageRepository repository, ImportExportJobRepository jobs,
            ImportExportJobMapper jobMapper, CoursePackageCodec codec, QuestionBankWriter questionWriter,
            PlatformFiles files,
            OperationService operations, @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator, Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.jobs = jobs;
        this.jobMapper = jobMapper;
        this.codec = codec;
        this.questionWriter = questionWriter;
        this.files = files;
        this.operations = operations;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    // ---- import

    public Operation importCoursePackage(UUID courseId, ImportCoursePackageRequest request) {
        DataScope scope = DataScope.current(PERMISSION_IMPORT);
        UUID actor = AuthContext.require().userId();
        CourseHead course = doInReadTransaction(() ->
                writableCourse(scope, repository.course(courseId), courseId));
        validator.validate(request);
        UUID target = request.getTargetChapterId();
        boolean toRepository = Boolean.TRUE.equals(request.getAddToRepository());
        doInReadTransaction(() -> {
            requireTemplateChapter(courseId, target);
            return null;
        });
        PlatformFiles.Read file = files.read(request.getFileId(), "fileId", List.of(".zip"),
                MAX_PACKAGE_MB * 1024L * 1024L);
        UUID operationId = operations.start(KIND_IMPORT, "lms.course", courseId);
        UUID jobId = UuidV7.next(clock);
        try {
            CoursePackageCodec.Outcome outcome = codec.read(file.content());
            if (!outcome.valid()) {
                runInWriteTransaction(() -> {
                    OffsetDateTime now = now();
                    repository.insertJob(jobId, course.institutionId(), "import", courseId, target, toRepository,
                            file.id(), now.toLocalDate(), now, actor);
                    repository.finishJob(jobId, "error", String.join("\n", outcome.problems()), null, null, null,
                            null, now, actor);
                    recordJob(jobId);
                });
                operations.fail(operationId, "PACKAGE_INVALID", "The package has " + outcome.problems().size()
                        + " problem(s); nothing was imported. Read them on import job " + jobId + ".");
                return operations.getOperation(operationId);
            }
            Map<String, Object> result = doInWriteTransaction(() -> {
                OffsetDateTime now = now();
                CourseHead locked = writableCourse(scope, repository.lockCourse(courseId), courseId);
                requireTemplateChapter(courseId, target);
                repository.insertJob(jobId, locked.institutionId(), "import", courseId, target, toRepository,
                        file.id(), now.toLocalDate(), now, actor);
                Map<String, Object> counts = write(locked, outcome.pack(), target, toRepository, now, actor);
                CoursePackage.Course from = outcome.pack().course();
                repository.finishJob(jobId, "complete", null, null, from == null ? null : from.code(),
                        from == null ? null : from.title(), from == null ? null : from.version(), now, actor);
                recordJob(jobId);
                audit.recordTransition("lms.course", courseId, "importPackage", List.of("chapters"), null);
                outbox.publish("lms.course.importPackage", "lms.course", courseId, LmsEvents.payload(courseId));
                return counts;
            });
            Map<String, Object> outcomeMap = new LinkedHashMap<>();
            outcomeMap.put("jobId", jobId.toString());
            outcomeMap.put("courseId", courseId.toString());
            outcomeMap.putAll(result);
            return operations.succeed(operationId, outcomeMap);
        } catch (RuntimeException e) {
            LOG.error("course package import stopped ({})", e.getClass().getSimpleName());
            operations.fail(operationId, "INTERNAL", "The import stopped before it finished; nothing was imported.");
            throw e;
        }
    }

    /** Writes the package's content into the course template; answers what was written, by kind. */
    private Map<String, Object> write(CourseHead course, CoursePackage pack, UUID target, boolean toRepository,
            OffsetDateTime now, UUID actor) {
        int chapters = 0;
        int lessons = 0;
        int quizzes = 0;
        int questions = 0;
        int assignments = 0;
        int chapterOrder = repository.nextChapterOrder(course.id());
        for (CoursePackage.Chapter chapter : pack.chapters()) {
            UUID chapterId = target;
            if (chapterId == null) {
                chapterId = UuidV7.next(clock);
                repository.insertChapter(chapterId, course.id(), chapter.title().trim(), chapterOrder++,
                        chapter.description(), now, actor);
                chapters++;
            }
            int lessonOrder = repository.nextLessonOrder(chapterId);
            for (CoursePackage.Lesson lesson : chapter.lessons()) {
                repository.insertLesson(UuidV7.next(clock), chapterId, lesson.title().trim(), lessonOrder++,
                        Boolean.TRUE.equals(lesson.includeInPreview()), lesson.body(), lesson.youtubeVideoId(), now,
                        actor);
                lessons++;
            }
            int itemOrder = repository.nextItemOrder(chapterId);
            for (CoursePackage.Quiz quiz : chapter.quizzes()) {
                UUID quizId = UuidV7.next(clock);
                BigDecimal total = quiz.questions().stream().map(q -> QuizGrader.number(q.marks()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                repository.insertQuiz(quizId, course, quiz.title().trim(), quiz.description(),
                        quiz.passingPercentage() == null ? 0 : quiz.passingPercentage(), total,
                        quiz.durationMinutes(), quiz.maxAttempts(), toRepository, now, actor);
                int questionOrder = 0;
                for (CoursePackage.Question question : quiz.questions()) {
                    UUID questionId = questionWriter.write(question, now, actor);
                    repository.insertQuizQuestion(UuidV7.next(clock), quizId, questionId,
                            QuizGrader.number(question.marks()), ++questionOrder, now, actor);
                    questions++;
                }
                repository.insertItem(UuidV7.next(clock), chapterId, course.id(), "quiz", quizId, null,
                        quiz.title().trim(), itemOrder++, now, actor);
                quizzes++;
            }
            for (CoursePackage.Assignment assignment : chapter.assignments()) {
                UUID assignmentId = UuidV7.next(clock);
                repository.insertAssignment(assignmentId, course, assignment.title().trim(), assignment.question(),
                        assignment.submissionType(), toRepository, now, actor);
                repository.insertItem(UuidV7.next(clock), chapterId, course.id(), "assignment", null, assignmentId,
                        assignment.title().trim(), itemOrder++, now, actor);
                assignments++;
            }
        }
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("chapters", chapters);
        counts.put("lessons", lessons);
        counts.put("quizzes", quizzes);
        counts.put("questions", questions);
        counts.put("assignments", assignments);
        return counts;
    }

    // ---- export

    public Operation exportCourse(UUID courseId) {
        DataScope scope = DataScope.current(PERMISSION_EXPORT);
        UUID actor = AuthContext.require().userId();
        Snapshot snapshot = doInReadTransaction(() -> {
            CourseHead course = repository.course(courseId)
                    .filter(c -> scope.canRead(Optional.ofNullable(c.institutionId())))
                    .orElseThrow(() -> new ResourceNotFoundException("lms.course", courseId));
            return new Snapshot(course, template(course));
        });
        UUID operationId = operations.start(KIND_EXPORT, "lms.course", courseId);
        try {
            byte[] zip = codec.write(snapshot.pack());
            String name = fileName(snapshot.course());
            UUID fileId = files.write(name, "application/zip", zip);
            UUID jobId = UuidV7.next(clock);
            runInWriteTransaction(() -> {
                OffsetDateTime now = now();
                repository.insertJob(jobId, snapshot.course().institutionId(), "export", courseId, null, false,
                        fileId, now.toLocalDate(), now, actor);
                CoursePackage.Course from = snapshot.pack().course();
                repository.finishJob(jobId, "complete", null, fileId, from.code(), from.title(), from.version(),
                        now, actor);
                recordJob(jobId);
            });
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("jobId", jobId.toString());
            result.put("fileId", fileId.toString());
            result.put("chapters", snapshot.pack().chapters().size());
            return operations.succeed(operationId, result);
        } catch (RuntimeException e) {
            LOG.error("course export stopped ({})", e.getClass().getSimpleName());
            operations.fail(operationId, "EXPORT_FAILED", "The export stopped before it finished; run it again.");
            throw e;
        }
    }

    /** The course template as a package, in the course's order. */
    private CoursePackage template(CourseHead course) {
        List<CoursePackageRepository.ChapterRow> chapters = repository.chapters(course.id());
        List<UUID> chapterIds = chapters.stream().map(CoursePackageRepository.ChapterRow::id).toList();
        Map<UUID, List<CoursePackageRepository.LessonRow>> lessons = repository.lessons(chapterIds).stream()
                .collect(Collectors.groupingBy(CoursePackageRepository.LessonRow::chapterId, LinkedHashMap::new,
                        Collectors.toList()));
        List<CoursePackageRepository.PlacedRow> placed = repository.placed(chapterIds);
        List<UUID> quizIds = placed.stream().map(CoursePackageRepository.PlacedRow::quizId)
                .filter(java.util.Objects::nonNull).toList();
        List<UUID> assignmentIds = placed.stream().map(CoursePackageRepository.PlacedRow::assignmentId)
                .filter(java.util.Objects::nonNull).toList();
        Map<UUID, CoursePackageRepository.QuizRow> quizzes = repository.quizzes(quizIds).stream()
                .collect(Collectors.toMap(CoursePackageRepository.QuizRow::id, q -> q));
        List<CoursePackageRepository.QuestionRow> questionRows = repository.questions(quizIds);
        List<UUID> questionIds = questionRows.stream().map(CoursePackageRepository.QuestionRow::id).distinct()
                .toList();
        Map<UUID, List<CoursePackageRepository.OptionRow>> options = repository.options(questionIds).stream()
                .collect(Collectors.groupingBy(CoursePackageRepository.OptionRow::questionId));
        Map<UUID, List<CoursePackageRepository.AnswerRow>> accepted = repository.acceptedAnswers(questionIds)
                .stream().collect(Collectors.groupingBy(CoursePackageRepository.AnswerRow::questionId));
        Map<UUID, CoursePackageRepository.AssignmentRow> assignments = repository.assignments(assignmentIds)
                .stream().collect(Collectors.toMap(CoursePackageRepository.AssignmentRow::id, a -> a));
        List<CoursePackage.Chapter> out = new ArrayList<>();
        for (CoursePackageRepository.ChapterRow chapter : chapters) {
            List<CoursePackage.Lesson> chapterLessons = lessons.getOrDefault(chapter.id(), List.of()).stream()
                    .map(l -> new CoursePackage.Lesson(l.title(), l.body(), l.includeInPreview(),
                            l.youtubeVideoId())).toList();
            List<CoursePackage.Quiz> chapterQuizzes = new ArrayList<>();
            List<CoursePackage.Assignment> chapterAssignments = new ArrayList<>();
            for (CoursePackageRepository.PlacedRow item : placed) {
                if (!item.chapterId().equals(chapter.id())) {
                    continue;
                }
                if (item.quizId() != null && quizzes.containsKey(item.quizId())) {
                    CoursePackageRepository.QuizRow quiz = quizzes.get(item.quizId());
                    List<CoursePackage.Question> quizQuestions = questionRows.stream()
                            .filter(q -> q.quizId().equals(quiz.id())).map(q -> new CoursePackage.Question(
                                    q.questionType(), q.title(), q.questionText(),
                                    q.marks() == null ? null : q.marks().toPlainString(), q.allowsMultiple(),
                                    q.caseSensitive(), q.trueFalseAnswer(), q.numericAnswer(), q.numericTolerance(),
                                    q.numericIsRange(), q.generalFeedback(), options.getOrDefault(q.id(), List.of()).stream()
                                            .map(o -> new CoursePackage.Option(o.text(), o.correct(),
                                                    o.explanation())).toList(),
                                    accepted.getOrDefault(q.id(), List.of()).stream()
                                            .map(CoursePackageRepository.AnswerRow::text).toList())).toList();
                    chapterQuizzes.add(new CoursePackage.Quiz(quiz.title(), quiz.description(),
                            quiz.passingPercentage(), quiz.durationMinutes(), quiz.maxAttempts(), quizQuestions));
                } else if (item.assignmentId() != null && assignments.containsKey(item.assignmentId())) {
                    CoursePackageRepository.AssignmentRow assignment = assignments.get(item.assignmentId());
                    chapterAssignments.add(new CoursePackage.Assignment(assignment.title(), assignment.question(),
                            assignment.submissionType()));
                }
            }
            out.add(new CoursePackage.Chapter(chapter.title(), chapter.description(), chapterLessons, chapterQuizzes,
                    chapterAssignments));
        }
        return new CoursePackage(CoursePackage.FORMAT, CoursePackage.VERSION, new CoursePackage.Course(course.code(),
                course.title(), course.versionMajor() + "." + course.versionMinor()), out);
    }

    // ---- jobs

    /** A signed link to the job's package: the exported package, or the package an import read. */
    public DownloadLink getJobPackage(UUID jobId) {
        DataScope scope = DataScope.current(PERMISSION_PACKAGE);
        JobHead job = doInReadTransaction(() -> visibleJob(scope, repository.job(jobId), jobId));
        if (job.packageFileId() == null) {
            throw new ConflictException("PACKAGE_NOT_READY", "The job has no package yet.");
        }
        return files.link(job.packageFileId(), "importExportJobId");
    }

    /**
     * Restores the archived cohort of a finished cohort-archive export: the cohort leaves {@code archived} for
     * {@code completed} and the job's restore is {@code complete} — once (409 afterwards, and for any other job).
     */
    public ImportExportJob restoreCohortArchive(UUID jobId, RestoreCohortArchiveRequest request) {
        DataScope scope = DataScope.current(PERMISSION_RESTORE);
        UUID actor = AuthContext.require().userId();
        return doInWriteTransaction(() -> {
            JobHead job = visibleJob(scope, repository.lockJob(jobId), jobId);
            scope.requireWritable(Optional.ofNullable(job.institutionId()));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != job.rowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!"export".equals(job.direction()) || !"cohort_archive".equals(job.packageFormat())
                    || job.cohortId() == null || !"complete".equals(job.status())) {
                throw new InvalidStateException("Only a finished cohort archive export can be restored.");
            }
            if ("complete".equals(job.unarchiveStatus())) {
                throw new InvalidStateException("The archive has already been restored.");
            }
            String lifecycle = repository.lockCohortLifecycle(job.cohortId())
                    .orElseThrow(() -> new InvalidStateException("The archived cohort no longer exists."));
            if (!"archived".equals(lifecycle)) {
                throw new InvalidStateException("The cohort is not archived.");
            }
            OffsetDateTime now = now();
            repository.restoreCohort(job.cohortId(), now, actor);
            audit.recordTransition("lms.cohort", job.cohortId(), "restoreArchive", List.of("lifecycleStatus"), null);
            outbox.publish("lms.cohort.restoreArchive", "lms.cohort", job.cohortId(),
                    LmsEvents.payload(job.cohortId()));
            if (repository.restored(jobId, job.rowVersion(), now, actor) == 0) {
                throw new PreconditionFailedException("The job changed meanwhile; read it again.");
            }
            audit.recordTransition(JOB_AGGREGATE, jobId, "restore", List.of("unarchiveStatus"), null);
            outbox.publish(JOB_AGGREGATE + ".restore", JOB_AGGREGATE, jobId, LmsEvents.payload(jobId));
            return jobMapper.toDto(jobs.findById(jobId).orElseThrow());
        });
    }

    // ---- helpers

    private static CourseHead writableCourse(DataScope scope, Optional<CourseHead> found, UUID courseId) {
        CourseHead course = found.filter(c -> scope.canRead(Optional.ofNullable(c.institutionId())))
                .orElseThrow(() -> new ResourceNotFoundException("lms.course", courseId));
        scope.requireWritable(Optional.ofNullable(course.institutionId()));
        return course;
    }

    private void requireTemplateChapter(UUID courseId, UUID chapterId) {
        if (chapterId != null && !repository.templateChapter(courseId, chapterId)) {
            throw new ValidationFailedException(List.of(new FieldError("targetChapterId",
                    "is not a chapter of the course's template", "NOT_FOUND")));
        }
    }

    private static JobHead visibleJob(DataScope scope, Optional<JobHead> found, UUID id) {
        return found.filter(j -> scope.canRead(Optional.ofNullable(j.institutionId())))
                .orElseThrow(() -> new ResourceNotFoundException(JOB_AGGREGATE, id));
    }

    private void recordJob(UUID jobId) {
        audit.recordCreate(JOB_AGGREGATE, jobId);
        outbox.publish(JOB_AGGREGATE + LmsEvents.CREATED, JOB_AGGREGATE, jobId, LmsEvents.payload(jobId));
    }

    private static String fileName(CourseHead course) {
        String base = course.code() != null && !course.code().isBlank() ? course.code() : "course";
        return base.replaceAll("[^A-Za-z0-9._-]", "_") + "-" + course.versionMajor() + "." + course.versionMinor()
                + ".zip";
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    /** The course read for an export and its package. */
    private record Snapshot(CourseHead course, CoursePackage pack) {
    }
}
