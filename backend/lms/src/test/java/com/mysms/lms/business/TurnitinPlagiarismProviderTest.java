package com.mysms.lms.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.lms.business.PlagiarismProvider.Event;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** The Turnitin adapter's callback mapping: which events lms acts on and what they say. */
class TurnitinPlagiarismProviderTest {

    private final TurnitinPlagiarismProvider provider =
            new TurnitinPlagiarismProvider(JsonMapper.builder().build(), "MySMS", "1");

    @Test
    void mapsSubmissionCompleteToAnUploadedEventWithItsMetadata() {
        Event event = provider.event(Map.of("event_type", "SUBMISSION_COMPLETE", "id", "sub-1",
                "metadata", Map.of("custom", Map.of("mysms_tenant", "t1", "mysms_report", "r1")))).orElseThrow();
        assertThat(event.submissionRef()).isEqualTo("sub-1");
        assertThat(event.uploaded()).isTrue();
        assertThat(event.reportReady()).isFalse();
        assertThat(event.metadata()).containsEntry("mysms_tenant", "t1").containsEntry("mysms_report", "r1");
    }

    @Test
    void mapsSimilarityCompleteToAReadyReportWithItsScore() {
        Event event = provider.event(Map.of("event_type", "SIMILARITY_COMPLETE", "submission_id", "sub-2",
                "status", "COMPLETE", "overall_match_percentage", 37)).orElseThrow();
        assertThat(event.submissionRef()).isEqualTo("sub-2");
        assertThat(event.reportReady()).isTrue();
        assertThat(event.similarity()).isEqualTo("37");
    }

    @Test
    void aFailedSubmissionCarriesItsErrorCode() {
        Event event = provider.event(Map.of("event_type", "SUBMISSION_COMPLETE", "id", "sub-3",
                "error_code", "UNSUPPORTED_FILETYPE")).orElseThrow();
        assertThat(event.uploaded()).isFalse();
        assertThat(event.errorCode()).isEqualTo("UNSUPPORTED_FILETYPE");
    }

    @Test
    void otherEventsAndPayloadsWithoutASubmissionAreNone() {
        assertThat(provider.event(Map.of("event_type", "PDF_STATUS", "id", "sub-4"))).isEmpty();
        assertThat(provider.event(Map.of("event_type", "SUBMISSION_COMPLETE"))).isEmpty();
        assertThat(provider.event(Map.of("id", "sub-5"))).isEqualTo(Optional.empty());
    }

    @Test
    void theProviderIsNamedTurnitin() {
        assertThat(provider.name()).isEqualTo("turnitin");
    }
}
