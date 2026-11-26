package com.mysms.academics.assessment.business;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.academics.assessment.domain.ExamResultEntity;
import com.mysms.core.error.InvalidStateException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExamServiceTest {

    @Test
    void onlyUnaggregatedResultsCanBeCorrected() {
        assertThatCode(() -> ExamService.requireUnaggregated(ExamResultEntity.builder().build()))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> ExamService.requireUnaggregated(
                ExamResultEntity.builder().marksheetLineId(UUID.randomUUID()).build()))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("added to a marksheet");
    }
}
