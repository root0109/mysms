package com.mysms.learning.lms.business;

import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.id.UuidV7;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.learning.lms.config.LmsEventsConfig;
import com.mysms.learning.lms.domain.GradebookEntryEntity;
import com.mysms.learning.lms.events.LmsEvents;
import com.mysms.learning.lms.repository.GradebookPostingRepository;
import com.mysms.learning.lms.repository.GradebookPostingRepository.Entry;
import com.mysms.learning.lms.repository.GradebookPostingRepository.Grade;
import com.mysms.learning.lms.repository.GradebookPostingRepository.GradedItem;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Writes a grade into the gradebook inside the caller's transaction (never one of its own): the student's or group's
 * entry for the item is locked (or created), changed, and the change is kept in the grade history
 * ({@code lms.grade_change_log}), audited and announced ({@code lms.gradebook_entry.graded}). Used by every path that
 * grades: a teacher's GradeEntry / BatchGradeEntries, a marked quiz attempt and a tool's LTI outcome.
 */
@Component
public class GradebookPostings {

    static final String AGGREGATE = GradebookEntryEntity.TABLE;
    static final String GRADED_EVENT = AGGREGATE + ".graded";
    private static final List<String> CHANGED = List.of("points", "autoPoints", "manualPoints", "feedback",
            "teacherComment", "status", "isPublished", "publishedAt", "pointsPossible");

    private final GradebookPostingRepository repository;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;

    public GradebookPostings(GradebookPostingRepository repository,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit, Clock clock) {
        this.repository = repository;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
    }

    /**
     * Applies a grade to the student's ({@code userId}) or group's entry for the item.
     *
     * @param expected the version the caller read the entry at (412 when it moved on; for a missing entry a sent
     *                 version is stale too), or empty
     * @param change   the grade the entry takes, given the entry as it stands (empty: none yet)
     * @return the id of the entry
     */
    public UUID post(GradedItem item, UUID userId, UUID groupId, OptionalInt expected,
            Function<Optional<Entry>, Grade> change, OffsetDateTime now, UUID actor) {
        Optional<Entry> current = repository.lock(item.id(), userId, groupId);
        if (expected.isPresent() && (current.isEmpty() || current.get().rowVersion() != expected.getAsInt())) {
            throw new PreconditionFailedException("The gradebook entry changed since it was read; read it again.");
        }
        Grade grade = change.apply(current);
        UUID entryId;
        if (current.isPresent()) {
            entryId = current.get().id();
            if (repository.update(entryId, current.get().rowVersion(), grade, now, actor) == 0) {
                throw new PreconditionFailedException("The gradebook entry changed meanwhile; read it again.");
            }
            audit.recordTransition(AGGREGATE, entryId, "grade", CHANGED, null);
        } else {
            entryId = UuidV7.next(clock);
            if (!repository.insert(entryId, item.cohortId(), item.id(), userId, groupId, grade, now, actor)) {
                throw new PreconditionFailedException("The gradebook entry was created meanwhile; read it again.");
            }
            audit.recordCreate(AGGREGATE, entryId);
        }
        repository.logChange(UuidV7.next(clock), entryId, grade, userId, now, actor);
        outbox.publish(GRADED_EVENT, AGGREGATE, entryId, LmsEvents.payload(entryId));
        return entryId;
    }

    /**
     * Records automatic points (a marked quiz, a tool's outcome) for a student: a teacher's manual points still win
     * over them, an excused entry stays excused, and publication is left as it is.
     */
    public UUID postAutomatic(GradedItem item, UUID userId, BigDecimal autoPoints, UUID quizSubmissionId,
            OffsetDateTime now, UUID actor) {
        return post(item, userId, null, OptionalInt.empty(), current -> {
            BigDecimal manual = current.map(Entry::manualPoints).orElse(null);
            String status = current.map(Entry::status).filter("excused"::equals).orElse("graded");
            return new Grade(manual != null ? manual : autoPoints, autoPoints, manual,
                    current.map(Entry::feedback).orElse(null), current.map(Entry::teacherComment).orElse(null),
                    status, current.map(Entry::published).orElse(false),
                    current.map(Entry::publishedAt).orElse(null), item.points(), quizSubmissionId);
        }, now, actor);
    }

    /** {@code percentage} of the item's points (2 decimals, half up); the percentage itself when it has none. */
    static BigDecimal pointsOf(GradedItem item, BigDecimal percentage) {
        if (item.points() == null) {
            return percentage.setScale(2, RoundingMode.HALF_UP);
        }
        return item.points().multiply(percentage).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
