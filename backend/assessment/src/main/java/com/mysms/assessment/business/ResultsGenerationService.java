package com.mysms.assessment.business;

import com.mysms.assessment.api.model.Operation;
import com.mysms.assessment.config.AssessmentDatabaseConfig;
import com.mysms.assessment.config.AssessmentEventsConfig;
import com.mysms.assessment.domain.ExamEntity;
import com.mysms.assessment.domain.ExamResultEntity;
import com.mysms.assessment.domain.GradingBandEntity;
import com.mysms.assessment.domain.GradingSchemeEntity;
import com.mysms.assessment.domain.MarksheetEntity;
import com.mysms.assessment.domain.MarksheetLineEntity;
import com.mysms.assessment.domain.OperationEntity;
import com.mysms.assessment.domain.ResultTemplateBandEntity;
import com.mysms.assessment.domain.ResultTemplateEntity;
import com.mysms.assessment.events.AssessmentEvents;
import com.mysms.assessment.mapper.OperationMapper;
import com.mysms.assessment.repository.ExamRepository;
import com.mysms.assessment.repository.ExamResultRepository;
import com.mysms.assessment.repository.GradingBandRepository;
import com.mysms.assessment.repository.GradingSchemeRepository;
import com.mysms.assessment.repository.MarksheetLineRepository;
import com.mysms.assessment.repository.MarksheetRepository;
import com.mysms.assessment.repository.OperationRepository;
import com.mysms.assessment.repository.ResultTemplateBandRepository;
import com.mysms.assessment.repository.ResultTemplateRepository;
import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.id.UuidV7;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/** Computes a session marksheet from finalized assessment rows and records its pollable operation. */
@Service
public class ResultsGenerationService extends AppTransactionManager {

    private static final String PERMISSION = "assessment.result_template.generate_results";
    private final ResultTemplateRepository templates;
    private final ExamRepository exams;
    private final ExamResultRepository examResults;
    private final GradingSchemeRepository schemes;
    private final GradingBandRepository bands;
    private final ResultTemplateBandRepository templateBands;
    private final MarksheetRepository marksheets;
    private final MarksheetLineRepository lines;
    private final OperationRepository operations;
    private final OperationMapper operationMapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;

    public ResultsGenerationService(ResultTemplateRepository templates, ExamRepository exams,
            ExamResultRepository examResults, GradingSchemeRepository schemes, GradingBandRepository bands,
            ResultTemplateBandRepository templateBands, MarksheetRepository marksheets,
            MarksheetLineRepository lines, OperationRepository operations, OperationMapper operationMapper,
            @Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AssessmentDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AssessmentEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(AssessmentEventsConfig.AUDIT) AuditWriter audit, Clock clock) {
        super(database, transactions);
        this.templates = templates;
        this.exams = exams;
        this.examResults = examResults;
        this.schemes = schemes;
        this.bands = bands;
        this.templateBands = templateBands;
        this.marksheets = marksheets;
        this.lines = lines;
        this.operations = operations;
        this.operationMapper = operationMapper;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
    }

    public Operation generateResults(UUID templateId) {
        DataScope scope = DataScope.current(PERMISSION);
        return doInWriteTransaction(() -> {
            ResultTemplateEntity template = templates.findById(templateId)
                    .orElseThrow(() -> new ResourceNotFoundException(ResultTemplateService.AGGREGATE, templateId));
            Optional<UUID> institution = templates.institutionOf(template.getExamSessionId());
            if (!scope.canRead(institution)) {
                throw new ResourceNotFoundException(ResultTemplateService.AGGREGATE, templateId);
            }
            scope.requireWritable(institution);
            if (!"draft".equals(template.getStatus())) {
                throw new InvalidStateException("Results can only be generated once from a draft template.");
            }
            List<ExamEntity> sessionExams = exams.listForSession(template.getExamSessionId());
            if (sessionExams.isEmpty()) {
                throw new InvalidStateException("The exam session has no papers.");
            }
            Map<UUID, List<ExamResultEntity>> byStudent = new LinkedHashMap<>();
            for (ExamEntity exam : sessionExams) {
                if (!Set.of("result_updated", "done").contains(exam.getStatus())) {
                    throw new InvalidStateException("Every paper must have finalized marks before results are generated.");
                }
                List<ExamResultEntity> rows = examResults.listForExam(exam.getId());
                if (rows.isEmpty()) {
                    throw new InvalidStateException("Every paper must have registered attendees before results are generated.");
                }
                for (ExamResultEntity row : rows) {
                    if (row.getResultStatus() == null || Set.of("pending", "withheld").contains(row.getResultStatus())
                            || "expected".equals(row.getAttendanceStatus())
                            || ("present".equals(row.getAttendanceStatus()) && row.getMarks() == null)) {
                        throw new InvalidStateException("Every attendee must have final attendance and marks before generation.");
                    }
                    byStudent.computeIfAbsent(row.getStudentId(), ignored -> new ArrayList<>()).add(row);
                }
            }
            List<GradingBandEntity> applicableBands = applicableBands(template);
            List<StudentAggregate> aggregates = aggregate(sessionExams, byStudent, applicableBands,
                    template.getGradingSchemeId() == null ? null : schemes.findById(template.getGradingSchemeId())
                            .orElseThrow(() -> new ResourceNotFoundException("assessment.grading_scheme",
                                    template.getGradingSchemeId())).getGradingMethod());
            long passed = aggregates.stream().filter(StudentAggregate::passed).count();
            int totalPassed = Math.toIntExact(passed);
            int totalFailed = aggregates.size() - totalPassed;
            UUID actor = Actor.current().orElseThrow(() -> new IllegalStateException("Authenticated actor is missing."));
            OffsetDateTime now = now();
            UUID tenantId = CallContextHolder.require().tenantId().orElseThrow();

            MarksheetEntity sheet = MarksheetEntity.builder().tenantId(tenantId)
                    .examSessionId(template.getExamSessionId()).resultTemplateId(templateId)
                    .name(template.getName()).generatedOn(template.getResultDate()).generatedByUserId(actor)
                    .visibleToGuardians(false).totalPassed(totalPassed).totalFailed(totalFailed).build();
            sheet.onCreatingData(Optional.of(actor), clock);
            MarksheetEntity storedSheet = marksheets.insert(sheet);
            audit.recordCreate(MarksheetService.AGGREGATE, storedSheet.getId());
            outbox.publish(MarksheetService.AGGREGATE + AssessmentEvents.CREATED, MarksheetService.AGGREGATE,
                    storedSheet.getId(), AssessmentEvents.payload(storedSheet.getId()));

            for (StudentAggregate aggregate : aggregates) {
                MarksheetLineEntity line = MarksheetLineEntity.builder().tenantId(tenantId)
                        .marksheetId(storedSheet.getId()).studentId(aggregate.studentId())
                        .generatedOn(template.getResultDate()).totalMarks(aggregate.totalMarks())
                        .percentage(aggregate.percentage()).gradeLabel(aggregate.gradeLabel())
                        .resultStatus(aggregate.passed() ? "pass" : "fail")
                        .creditsAttempted(aggregate.creditsAttempted()).creditsEarned(aggregate.creditsEarned())
                        .gradePoints(aggregate.gradePoints()).sgpa(aggregate.sgpa())
                        .cgpa(cumulativeGpa(aggregate, template.getExamSessionId(), template.getResultDate()))
                        .build();
                line.onCreatingData(Optional.of(actor), clock);
                MarksheetLineEntity storedLine = lines.insertGenerated(line);
                audit.recordCreate(MarksheetLineService.AGGREGATE, storedLine.getId());
                outbox.publish(MarksheetLineService.AGGREGATE + AssessmentEvents.CREATED,
                        MarksheetLineService.AGGREGATE, storedLine.getId(), AssessmentEvents.payload(storedLine.getId()));
                for (GeneratedResult generated : aggregate.results()) {
                    ExamResultEntity result = generated.result();
                    BandGrade grade = generated.grade();
                    if (examResults.setGeneratedValues(result, result.getRowVersion(),
                            grade == null ? null : grade.label(), grade == null ? null : grade.points(),
                            result.getCredits(), storedLine.getId(), actor, now) == 0) {
                        throw new PreconditionFailedException("An exam result changed during result generation.");
                    }
                    audit.recordTransition(ExamResultService.AGGREGATE, result.getId(), "generateResult",
                            List.of("gradeLabel", "gradePoint", "marksheetLineId"), null);
                    outbox.publish(ExamResultService.AGGREGATE + ".resultGenerated", ExamResultService.AGGREGATE,
                            result.getId(), AssessmentEvents.payload(result.getId()));
                }
            }
            if (templates.markGenerated(templateId, template.getRowVersion(), now, actor) == 0) {
                throw new PreconditionFailedException("The result template changed during generation.");
            }
            audit.recordTransition(ResultTemplateService.AGGREGATE, templateId, "generateResults",
                    List.of("status"), null);
            outbox.publish(ResultTemplateService.AGGREGATE + ".resultsGenerated", ResultTemplateService.AGGREGATE,
                    templateId, AssessmentEvents.payload(templateId));

            OperationEntity operation = OperationEntity.builder().tenantId(tenantId)
                    .operationName("assessment.GenerateResults").resourceType(ResultTemplateService.AGGREGATE)
                    .resourceId(templateId).status("succeeded").progressPercent(100)
                    .resultEntityType(MarksheetService.AGGREGATE).resultEntityId(storedSheet.getId())
                    .startedByUserId(actor).startedAt(now).finishedAt(now).build();
            operation.onCreatingData(Optional.of(actor), clock);
            OperationEntity storedOperation = operations.insertCompleted(operation);
            audit.recordCreate(OperationService.AGGREGATE, storedOperation.getId());
            outbox.publish(OperationService.AGGREGATE + AssessmentEvents.CREATED, OperationService.AGGREGATE,
                    storedOperation.getId(), AssessmentEvents.payload(storedOperation.getId()));
            Operation response = operationMapper.toDto(storedOperation);
            response.setResult(Map.of("marksheetId", storedSheet.getId(), "totalPassed", totalPassed,
                    "totalFailed", totalFailed, "studentCount", aggregates.size()));
            return response;
        });
    }

    private List<GradingBandEntity> applicableBands(ResultTemplateEntity template) {
        if (template.getGradingSchemeId() == null) {
            return List.of();
        }
        List<ResultTemplateBandEntity> selected = templateBands.findBandsForTemplate(template.getId());
        if (selected.isEmpty()) {
            return bands.listForScheme(template.getGradingSchemeId());
        }
        Set<UUID> ids = selected.stream().map(ResultTemplateBandEntity::getGradingBandId).collect(java.util.stream.Collectors.toSet());
        return bands.listForScheme(template.getGradingSchemeId()).stream().filter(band -> ids.contains(band.getId())).toList();
    }

    static List<StudentAggregate> aggregate(List<ExamEntity> exams,
            Map<UUID, List<ExamResultEntity>> byStudent, List<GradingBandEntity> bands, String gradingMethod) {
        List<StudentAggregate> output = new ArrayList<>();
        for (Map.Entry<UUID, List<ExamResultEntity>> entry : byStudent.entrySet()) {
            List<ExamResultEntity> rows = entry.getValue();
            List<GeneratedResult> gradedResults = new ArrayList<>(rows.size());
            BigDecimal total = BigDecimal.ZERO;
            BigDecimal maximum = BigDecimal.ZERO;
            boolean passed = true;
            BigDecimal creditsAttempted = BigDecimal.ZERO;
            BigDecimal creditsEarned = BigDecimal.ZERO;
            BigDecimal gradePoints = BigDecimal.ZERO;
            boolean hasCredits = false;
            boolean hasPoints = true;
            for (ExamResultEntity result : rows) {
                if ("fail".equals(result.getResultStatus()) || "absent".equals(result.getResultStatus())) {
                    passed = false;
                }
                BigDecimal max = result.getMaxMarks();
                if (max == null) {
                    max = exams.stream().filter(exam -> exam.getId().equals(result.getExamId()))
                            .map(ExamEntity::getTotalMarks).findFirst().orElse(null);
                }
                if (max == null || max.signum() <= 0) {
                    throw new ValidationFailedException(List.of(new FieldError("examResults.maxMarks",
                            "every result needs a positive maximum-mark snapshot", "NOT_NULL")));
                }
                maximum = maximum.add(max);
                BandGrade grade = result.getMarks() == null ? null
                        : bandFor(result.getMarks().multiply(BigDecimal.valueOf(100))
                                .divide(max, 4, RoundingMode.HALF_UP), bands);
                gradedResults.add(new GeneratedResult(result, grade));
                if (result.getMarks() != null) {
                    total = total.add(result.getMarks());
                }
                if (result.getCredits() != null) {
                    hasCredits = true;
                    creditsAttempted = creditsAttempted.add(result.getCredits());
                    if ("pass".equals(result.getResultStatus())) {
                        creditsEarned = creditsEarned.add(result.getCredits());
                    }
                    if (grade == null || grade.points() == null) {
                        hasPoints = false;
                    } else {
                        gradePoints = gradePoints.add(grade.points().multiply(result.getCredits()));
                    }
                }
            }
            BigDecimal percentage = total.multiply(BigDecimal.valueOf(100)).divide(maximum, 2, RoundingMode.HALF_UP);
            BandGrade overallBand = bandFor(percentage, bands);
            String gradeLabel = overallBand == null ? null : overallBand.label();
            if ("pass_fail".equals(gradingMethod)) {
                gradeLabel = passed ? "Pass" : "Fail";
            }
            if (overallBand != null && overallBand.isPass() != null) {
                passed = overallBand.isPass();
            }
            BigDecimal attempted = hasCredits ? creditsAttempted : null;
            BigDecimal earned = hasCredits ? creditsEarned : null;
            BigDecimal points = hasCredits && hasPoints ? gradePoints : null;
            BigDecimal sgpa = points != null && creditsAttempted.signum() > 0
                    ? points.divide(creditsAttempted, 3, RoundingMode.HALF_UP) : null;
            output.add(new StudentAggregate(entry.getKey(), gradedResults, total, percentage, gradeLabel, passed,
                    attempted, earned, points, sgpa));
        }
        return output;
    }

    private static BandGrade bandFor(BigDecimal percentage, List<GradingBandEntity> bands) {
        return bands.stream().filter(band -> band.getMinPercent() != null && band.getMaxPercent() != null
                        && percentage.compareTo(band.getMinPercent()) >= 0
                        && percentage.compareTo(band.getMaxPercent()) <= 0)
                .findFirst().map(band -> new BandGrade(band.getGradeLabel(), band.getGradePoint(), band.getIsPass()))
                .orElse(null);
    }

    private BigDecimal cumulativeGpa(StudentAggregate aggregate, UUID examSessionId,
            java.time.LocalDate generatedOn) {
        if (aggregate.creditsAttempted() == null || aggregate.gradePoints() == null) {
            return null;
        }
        MarksheetLineRepository.GpaTotals prior = lines.cumulativeGpaTotals(aggregate.studentId(), examSessionId,
                generatedOn);
        BigDecimal credits = prior.creditsAttempted().add(aggregate.creditsAttempted());
        if (credits.signum() <= 0) {
            return null;
        }
        return prior.gradePoints().add(aggregate.gradePoints()).divide(credits, 3, RoundingMode.HALF_UP);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    record BandGrade(String label, BigDecimal points, Boolean isPass) { }
    record GeneratedResult(ExamResultEntity result, BandGrade grade) { }
    record StudentAggregate(UUID studentId, List<GeneratedResult> results, BigDecimal totalMarks,
            BigDecimal percentage, String gradeLabel, boolean passed, BigDecimal creditsAttempted,
            BigDecimal creditsEarned, BigDecimal gradePoints, BigDecimal sgpa) { }
}
