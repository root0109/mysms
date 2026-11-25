package com.mysms.privacyclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** What a module's handler reads from privacy (tasks, holds) and what it answers, without a database. */
class PrivacyTaskModelTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final UUID SUBJECT = UUID.randomUUID();
    private static final UUID ROW = UUID.randomUUID();

    @Test
    void aSubjectHoldKeepsEveryRowACategoryHoldItsCategoryARecordHoldItsRows() {
        ActiveHolds subject = new ActiveHolds(List.of(hold(ActiveHold.SUBJECT, null, null, Set.of())));
        ActiveHolds category = new ActiveHolds(List.of(hold(ActiveHold.CATEGORY, "health", null, Set.of())));
        ActiveHolds records = new ActiveHolds(List.of(hold(ActiveHold.RECORDS, null, "sis.student", Set.of(ROW))));

        assertThat(subject.keeps("people.person", "identity", ROW)).isTrue();
        assertThat(category.keeps("sis.student_health_record", "health", ROW)).isTrue();
        assertThat(category.keeps("sis.student", "academic_record", ROW)).isFalse();
        assertThat(records.keeps("sis.student", "academic_record", ROW)).isTrue();
        assertThat(records.keeps("sis.student", "academic_record", UUID.randomUUID())).isFalse();
        assertThat(records.keeps("sis.guardian", "contact", ROW)).isFalse();
        assertThat(ActiveHolds.none().keeps("people.person", "identity", ROW)).isFalse();
    }

    @Test
    void anErasureIsPartialWhenRowsWereKeptAndNotApplicableWhenNothingWasHeld() {
        Erasure kept = new Erasure(Map.of("people.person", 1), 2, Map.of());
        Erasure done = new Erasure(Map.of("people.person", 1), 0, Map.of());
        Erasure nothing = new Erasure(Map.of(), 0, Map.of());

        assertThat(TaskResult.erased(kept, List.of()).status()).isEqualTo(TaskResult.Status.PARTIAL);
        assertThat(TaskResult.erased(done, List.of()).status()).isEqualTo(TaskResult.Status.DONE);
        assertThat(TaskResult.erased(nothing, List.of()).status()).isEqualTo(TaskResult.Status.NOT_APPLICABLE);
        assertThat(TaskResult.exported(Map.of("people.person", 0), null).status())
                .isEqualTo(TaskResult.Status.NOT_APPLICABLE);
    }

    @Test
    void aTaskIsReadStrictlyAndAnsweredWithIdsCountsAndCodes() {
        UUID taskId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        PrivacyTask task = PrivacyTask.from(JSON.readTree("{\"requestId\":\"" + requestId + "\",\"taskId\":\"" + taskId
                + "\",\"type\":\"erasure\",\"action\":\"erase\",\"subjectPersonId\":\"" + SUBJECT + "\","
                + "\"module\":\"people\",\"wave\":2,\"deadline\":\"2027-01-31T00:00:00Z\"}"));
        assertThat(task.action()).isEqualTo(PrivacyTaskAction.ERASE);
        assertThat(task.wave()).isEqualTo(2);

        Map<String, Object> answer = TaskResult.erased(new Erasure(Map.of("people.person", 1), 0, Map.of()),
                List.of()).answer(task);
        assertThat(answer).containsEntry("taskId", taskId.toString()).containsEntry("requestId", requestId.toString())
                .containsEntry("status", "done").containsEntry("heldItems", 0);
        assertThat(TaskResult.failed("LEGAL_HOLDS_UNAVAILABLE").answer(task)).containsOnlyKeys("taskId", "requestId",
                "status", "errorCode");

        assertThatThrownBy(() -> PrivacyTask.from(JSON.readTree("{\"taskId\":\"" + taskId + "\",\"action\":\"shred\"}")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ActiveHold hold(String scope, String category, String entityType, Set<UUID> ids) {
        return new ActiveHold(UUID.randomUUID(), scope, ActiveHold.SUBJECT.equals(scope) ? SUBJECT : null, category,
                entityType == null ? null : "sis", entityType, ids);
    }
}
