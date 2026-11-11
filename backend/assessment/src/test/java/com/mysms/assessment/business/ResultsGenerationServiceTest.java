package com.mysms.assessment.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.assessment.domain.ExamEntity;
import com.mysms.assessment.domain.ExamResultEntity;
import com.mysms.assessment.domain.GradingBandEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResultsGenerationServiceTest {

    @Test
    void computesCurrentPaperGradesFromMaxSnapshotsBeforeWeightedGpa() {
        UUID studentId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        ExamEntity paper = ExamEntity.builder().id(examId).totalMarks(new BigDecimal("100.00")).build();
        ExamResultEntity newlyGraded = ExamResultEntity.builder().id(UUID.randomUUID()).examId(examId)
                .studentId(studentId).marks(new BigDecimal("90.00")).maxMarks(new BigDecimal("100.00"))
                .credits(new BigDecimal("2.00")).resultStatus("pass").build();
        ExamResultEntity staleGrade = ExamResultEntity.builder().id(UUID.randomUUID()).examId(examId)
                .studentId(studentId).marks(new BigDecimal("25.00")).maxMarks(new BigDecimal("50.00"))
                .credits(new BigDecimal("3.00")).resultStatus("pass").gradeLabel("A")
                .gradePoint(new BigDecimal("4.00")).build();
        List<GradingBandEntity> bands = List.of(
                band("A", "80.00", "100.00", "4.00"),
                band("B", "50.00", "79.99", "2.00"));

        ResultsGenerationService.StudentAggregate aggregate = ResultsGenerationService.aggregate(List.of(paper),
                Map.of(studentId, List.of(newlyGraded, staleGrade)), bands, "percentage_band").get(0);

        assertThat(aggregate.gradePoints()).isEqualByComparingTo("14.00");
        assertThat(aggregate.sgpa()).isEqualByComparingTo("2.800");
        assertThat(aggregate.results()).extracting(result -> result.grade().label()).containsExactly("A", "B");
        assertThat(aggregate.results()).extracting(result -> result.grade().points())
                .containsExactly(new BigDecimal("4.00"), new BigDecimal("2.00"));
    }

    private static GradingBandEntity band(String label, String minimum, String maximum, String points) {
        return GradingBandEntity.builder().minPercent(new BigDecimal(minimum)).maxPercent(new BigDecimal(maximum))
                .gradeLabel(label).gradePoint(new BigDecimal(points)).isPass(true).build();
    }
}