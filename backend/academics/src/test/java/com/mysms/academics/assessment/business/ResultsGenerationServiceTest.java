package com.mysms.academics.assessment.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.academics.assessment.domain.ExamResultEntity;
import com.mysms.academics.assessment.domain.GradingBandEntity;
import com.mysms.core.error.InvalidStateException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The aggregation rules of a generated marksheet line ({@link ResultsGenerationService#aggregate}). */
class ResultsGenerationServiceTest {

    private static final List<GradingBandEntity> BANDS = List.of(
            band("A", "90.00", "100.00", "10.00", true),
            band("B", "75.00", "89.99", "8.00", true),
            band("C", "40.00", "74.99", "5.00", true),
            band("F", "0.00", "39.99", "0.00", false));

    private final UUID exam = UUID.randomUUID();

    @Test
    void computesCurrentPaperGradesFromMaxSnapshotsBeforeWeightedGpa() {
        UUID studentId = UUID.randomUUID();
        ExamResultEntity newlyGraded = ExamResultEntity.builder().id(UUID.randomUUID()).examId(exam)
                .studentId(studentId).attendanceStatus("present").marks(new BigDecimal("90.00"))
                .maxMarks(new BigDecimal("100.00")).credits(new BigDecimal("2.00")).resultStatus("pass").build();
        ExamResultEntity staleGrade = ExamResultEntity.builder().id(UUID.randomUUID()).examId(exam)
                .studentId(studentId).attendanceStatus("present").marks(new BigDecimal("40.00"))
                .maxMarks(new BigDecimal("50.00")).credits(new BigDecimal("3.00")).resultStatus("pass")
                .gradeLabel("A").gradePoint(new BigDecimal("4.00")).build();

        ResultsGenerationService.StudentAggregate aggregate = ResultsGenerationService.aggregate(
                Map.of(studentId, List.of(newlyGraded, staleGrade)), BANDS, "percentage_band").get(0);

        assertThat(aggregate.gradePoints()).as("10 × 2 + 8 × 3").isEqualByComparingTo("44.00");
        assertThat(aggregate.sgpa()).as("44 / 5").isEqualByComparingTo("8.800");
        assertThat(aggregate.results()).extracting(result -> result.grade().label()).containsExactly("A", "B");
    }

    @Test
    void aPaperPercentageIsRoundedToTheBandsScaleSoNoneFallsBetweenBands() {
        assertThat(ResultsGenerationService.percent(new BigDecimal("179.99"), new BigDecimal("200")))
                .isEqualByComparingTo("90.00");
        assertThat(ResultsGenerationService.percent(new BigDecimal("179.97"), new BigDecimal("200")))
                .as("89.985 rounds half-up").isEqualByComparingTo("89.99");
        ResultsGenerationService.StudentAggregate aggregate = aggregate("present", "89.995", "100.000", "pass");
        assertThat(aggregate.results().get(0).grade().label()).isEqualTo("A");
    }

    @Test
    void aFailedOrMissedPaperFailsTheStudentWhateverTheAverage() {
        UUID student = UUID.randomUUID();
        ExamResultEntity strong = result(student, "present", "100.00", "pass");
        ExamResultEntity failed = result(student, "present", "60.00", "fail");
        ResultsGenerationService.StudentAggregate aggregate = ResultsGenerationService.aggregate(
                Map.of(student, List.of(strong, failed)), BANDS, "percentage_band").get(0);
        assertThat(aggregate.percentage()).isEqualByComparingTo("80.00");
        assertThat(aggregate.gradeLabel()).as("the band of the average").isEqualTo("B");
        assertThat(aggregate.passed()).as("a failed paper").isFalse();

        ResultsGenerationService.StudentAggregate absent = ResultsGenerationService.aggregate(
                Map.of(student, List.of(strong, result(student, "absent", null, "absent"))), BANDS, "pass_fail").get(0);
        assertThat(absent.passed()).isFalse();
        assertThat(absent.gradeLabel()).as("the label follows the final decision").isEqualTo("Fail");
    }

    @Test
    void aFailingOverallBandFailsAndThePassFailLabelAgrees() {
        ResultsGenerationService.StudentAggregate low = aggregate("present", "30.00", "100.00", "pass");
        assertThat(low.passed()).isFalse();
        ResultsGenerationService.StudentAggregate passFail = ResultsGenerationService.aggregate(
                Map.of(UUID.randomUUID(), List.of(result(UUID.randomUUID(), "present", "30.00", "pass"))), BANDS,
                "pass_fail").get(0);
        assertThat(passFail.passed()).isFalse();
        assertThat(passFail.gradeLabel()).isEqualTo("Fail");
        ResultsGenerationService.StudentAggregate good = ResultsGenerationService.aggregate(
                Map.of(UUID.randomUUID(), List.of(result(UUID.randomUUID(), "present", "55.00", "pass"))), BANDS,
                "pass_fail").get(0);
        assertThat(good.passed()).isTrue();
        assertThat(good.gradeLabel()).isEqualTo("Pass");
    }

    @Test
    void anExemptedPaperCountsForNothingAndAnAllExemptedStudentHasNoLine() {
        UUID student = UUID.randomUUID();
        ExamResultEntity sat = result(student, "present", "50.00", "pass");
        sat.setCredits(new BigDecimal("4"));
        ExamResultEntity exempted = ExamResultEntity.builder().id(UUID.randomUUID()).examId(exam).studentId(student)
                .attendanceStatus("exempted").maxMarks(new BigDecimal("100.00")).credits(new BigDecimal("2")).build();
        ResultsGenerationService.StudentAggregate aggregate = ResultsGenerationService.aggregate(
                Map.of(student, List.of(sat, exempted)), BANDS, "percentage_band").get(0);
        assertThat(aggregate.percentage()).isEqualByComparingTo("50.00");
        assertThat(aggregate.creditsAttempted()).isEqualByComparingTo("4");
        assertThat(aggregate.results()).hasSize(2);
        assertThat(aggregate.results().get(1).grade()).isNull();
        assertThat(aggregate.passed()).isTrue();

        assertThat(ResultsGenerationService.aggregate(Map.of(student, List.of(exempted)), BANDS,
                "percentage_band")).isEmpty();
        assertThat(ResultsGenerationService.isFinal(exempted)).isTrue();
        exempted.setAttendanceStatus("expected");
        assertThat(ResultsGenerationService.isFinal(exempted)).as("an expected attendee is not final").isFalse();
    }

    @Test
    void aResultWithoutAMaximumSnapshotIsAConflictNotAValidationError() {
        ExamResultEntity row = ExamResultEntity.builder().id(UUID.randomUUID()).examId(exam)
                .studentId(UUID.randomUUID()).attendanceStatus("present").marks(BigDecimal.ONE).resultStatus("pass")
                .build();
        assertThatThrownBy(() -> ResultsGenerationService.aggregate(Map.of(row.getStudentId(), List.of(row)),
                BANDS, "percentage_band")).isInstanceOf(InvalidStateException.class);
    }

    @Test
    void aPercentageBetweenTwoBandsHasNoGradeAndThePapersDecideThePass() {
        List<GradingBandEntity> gapped = List.of(band("Pass", "40.00", "74.00", "5.00", true),
                band("Merit", "75.00", "100.00", "8.00", true));
        UUID student = UUID.randomUUID();
        ResultsGenerationService.StudentAggregate line = ResultsGenerationService.aggregate(
                Map.of(student, List.of(result(student, "present", "74.50", "pass"))), gapped, "percentage_band").get(0);
        assertThat(line.percentage()).isEqualByComparingTo("74.50");
        assertThat(line.gradeLabel()).as("74.50 lies between 74.00 and 75.00").isNull();
        assertThat(line.results().get(0).grade()).isNull();
        assertThat(line.passed()).as("no band says otherwise, and the paper was passed").isTrue();
    }

    private ResultsGenerationService.StudentAggregate aggregate(String attendance, String marks, String max,
            String status) {
        UUID student = UUID.randomUUID();
        ExamResultEntity row = result(student, attendance, marks, status);
        row.setMaxMarks(new BigDecimal(max));
        return ResultsGenerationService.aggregate(Map.of(student, List.of(row)), BANDS, "percentage_band")
                .get(0);
    }

    private ExamResultEntity result(UUID student, String attendance, String marks, String status) {
        return ExamResultEntity.builder().id(UUID.randomUUID()).examId(exam).studentId(student)
                .attendanceStatus(attendance).marks(marks == null ? null : new BigDecimal(marks))
                .maxMarks(new BigDecimal("100.00")).resultStatus(status).build();
    }

    private static GradingBandEntity band(String label, String minimum, String maximum, String points, boolean pass) {
        return GradingBandEntity.builder().minPercent(new BigDecimal(minimum)).maxPercent(new BigDecimal(maximum))
                .gradeLabel(label).gradePoint(new BigDecimal(points)).isPass(pass).build();
    }
}
