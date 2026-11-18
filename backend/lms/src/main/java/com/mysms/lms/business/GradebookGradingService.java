package com.mysms.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ApiException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.id.UuidV7;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.lms.api.model.BatchGradeEntriesRequest;
import com.mysms.lms.api.model.GradeEntryInput;
import com.mysms.lms.api.model.GradeEntryRequest;
import com.mysms.lms.api.model.GradeEntryResult;
import com.mysms.lms.api.model.GradeEntryResultList;
import com.mysms.lms.api.model.GradeInput;
import com.mysms.lms.api.model.GradebookEntry;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.mapper.GradebookEntryMapper;
import com.mysms.lms.repository.GradebookEntryRepository;
import com.mysms.lms.repository.GradebookPostingRepository;
import com.mysms.lms.repository.GradebookPostingRepository.Entry;
import com.mysms.lms.repository.GradebookPostingRepository.Grade;
import com.mysms.lms.repository.GradebookPostingRepository.GradedItem;
import com.mysms.lms.repository.GradebookPostingRepository.RubricCell;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * A teacher's grading in the gradebook (GradeEntry, BatchGradeEntries): the grade of a student or a group for a graded
 * item placed in a cohort, with its history and rubric choices ({@link GradebookPostings} writes them).
 *
 * <p>Rules: the item exists, is placed in a cohort the caller may see (404) and write (403) and is graded (422); exactly
 * one of a student enrolled in the cohort or a group of the item is named (422); points are from 0 to the item's points
 * with at most 2 decimals (422); rubric choices are cells of the item's rubric, one per criterion (422) — without
 * points, the rubric's weighted levels give them. A sent etag must match the entry (412). The manual points win over
 * automatic ones; {@code publish} true publishes the grade, false withdraws it, absent leaves it. A batch writes each
 * row in its own transaction and reports a row's refusal by its code, the others are kept.
 */
@Service
public class GradebookGradingService extends AppTransactionManager {

    static final String PERMISSION_GRADE = "lms.gradebook_entry.grade_entry";
    static final String PERMISSION_BATCH = "lms.gradebook_entry.batch_grade_entries";
    static final int MAX_BATCH = 500;
    private static final int MAX_TEXT = 10000;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    /** Statuses a teacher decided that a grade without a status does not overwrite. */
    private static final Set<String> KEPT_STATUSES = Set.of("excused", "reassigned");

    private final GradebookPostingRepository repository;
    private final GradebookEntryRepository entries;
    private final GradebookEntryMapper mapper;
    private final GradebookPostings postings;
    private final DtoValidator validator;
    private final Clock clock;
    private final CohortAccess access;

    public GradebookGradingService(GradebookPostingRepository repository, GradebookEntryRepository entries,
            GradebookEntryMapper mapper, GradebookPostings postings,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            DtoValidator validator, Clock clock, CohortAccess access) {
        super(database, transactions);
        this.access = access;
        this.repository = repository;
        this.entries = entries;
        this.mapper = mapper;
        this.postings = postings;
        this.validator = validator;
        this.clock = clock;
    }

    public GradebookEntry gradeEntry(GradeEntryRequest request) {
        validator.validate(request);
        DataScope scope = DataScope.current(PERMISSION_GRADE);
        return doInWriteTransaction(() -> grade(scope, PERMISSION_GRADE, request.getContentItemId(), request.getUserId(),
                request.getGroupId(), request.getEtag(), request.getGrade(), ""));
    }

    public GradeEntryResultList batchGradeEntries(BatchGradeEntriesRequest request) {
        validator.validate(request);
        if (request.getEntries().isEmpty() || request.getEntries().size() > MAX_BATCH) {
            throw new ValidationFailedException(List.of(new FieldError("entries",
                    "must hold 1 to " + MAX_BATCH + " entries", "Size")));
        }
        DataScope scope = DataScope.current(PERMISSION_BATCH);
        GradeEntryResultList results = new GradeEntryResultList();
        List<GradeEntryResult> items = new ArrayList<>();
        int index = 0;
        for (GradeEntryInput input : request.getEntries()) {
            String prefix = "entries[" + index++ + "].";
            GradeEntryResult result = new GradeEntryResult().contentItemId(input.getContentItemId());
            result.setUserId(input.getUserId());
            result.setGroupId(input.getGroupId());
            try {
                result.setEntry(doInWriteTransaction(() -> grade(scope, PERMISSION_BATCH, input.getContentItemId(),
                        input.getUserId(),
                        input.getGroupId(), input.getEtag(), input.getGrade(), prefix)));
            } catch (ApiException refused) {
                result.setError(refused.code());
            }
            items.add(result);
        }
        results.setItems(items);
        return results;
    }

    /** One grade, by the teaching staff of the item's cohort only (404 outside the scope, 403 for other callers). */
    private GradebookEntry grade(DataScope scope, String permission, UUID itemId, UUID userId, UUID groupId,
            String etag, GradeInput input, String prefix) {
        GradedItem item = repository.item(itemId).filter(i -> scope.canRead(Optional.ofNullable(i.institutionId())))
                .orElseThrow(() -> new ResourceNotFoundException("lms.content_item", itemId));
        scope.requireWritable(Optional.ofNullable(item.institutionId()));
        if (item.cohortId() != null) {
            access.taught(item.cohortId(), permission);
        }
        UUID actor = Actor.current().orElseThrow(() -> new PermissionDeniedException("Grading needs a signed-in user."));
        List<FieldError> errors = new ArrayList<>();
        if (item.cohortId() == null) {
            errors.add(new FieldError(prefix + "contentItemId", "is not placed in a cohort", "INVALID"));
        } else if (!item.graded()) {
            errors.add(new FieldError(prefix + "contentItemId", "is not a graded item", "INVALID"));
        }
        if ((userId == null) == (groupId == null)) {
            errors.add(new FieldError(prefix + "userId", "name exactly one of userId and groupId", "INVALID"));
        } else if (item.cohortId() != null && userId != null && !repository.enrolled(item.cohortId(), userId)) {
            errors.add(new FieldError(prefix + "userId", "is not enrolled in the item's cohort", "NOT_FOUND"));
        } else if (groupId != null && !repository.groupOfItem(groupId, itemId)) {
            errors.add(new FieldError(prefix + "groupId", "is not a group of this item", "NOT_FOUND"));
        }
        BigDecimal points = WireValues.decimal(input.getPoints());
        if (points != null && (points.signum() < 0 || points.scale() > 2
                || (item.points() != null && points.compareTo(item.points()) > 0))) {
            errors.add(new FieldError(prefix + "grade.points",
                    "must be from 0 to the item's points with at most 2 decimals", "Range"));
        }
        text(input.getFeedback(), prefix + "grade.feedback", errors);
        text(input.getTeacherComment(), prefix + "grade.teacherComment", errors);
        List<RubricCell> cells = rubric(item, input.getRubricCellIds(), prefix, errors);
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        BigDecimal manual = points != null || cells.isEmpty() ? points : rubricPoints(item, cells);
        boolean graded = manual != null;
        OffsetDateTime now = now();
        OptionalInt expected = Etags.bodyVersion(etag);
        UUID entryId = postings.post(item, userId, groupId, expected,
                current -> manualGrade(item, current, input, graded ? manual
                        : current.map(Entry::manualPoints).orElse(null), graded, now), now, actor);
        if (!input.getRubricCellIds().isEmpty()) {
            repository.clearRubric(itemId, userId, groupId, now, actor);
            Integer version = repository.rubricVersion(item.rubricId());
            for (RubricCell cell : cells) {
                repository.insertRubric(UuidV7.next(clock), itemId, userId, groupId, cell.id(), version,
                        cellPoints(item, cell), now, actor);
            }
        }
        return mapper.toDto(entries.findById(entryId).orElseThrow());
    }

    /**
     * The entry after a teacher's grade: what the input omits stays as it was — omitted points and rubric keep the
     * manual grade, an omitted status keeps the status (excused or reassigned stay so even when a grade is given; an
     * entry without a decision becomes graded once points are given).
     */
    private static Grade manualGrade(GradedItem item, Optional<Entry> current, GradeInput input, BigDecimal manual,
            boolean graded, OffsetDateTime now) {
        BigDecimal auto = current.map(Entry::autoPoints).orElse(null);
        var wantedStatus = input.getStatus();
        String kept = current.map(Entry::status).orElse("graded");
        String status = wantedStatus != null ? wantedStatus.getValue()
                : graded && !KEPT_STATUSES.contains(kept) ? "graded" : kept;
        Boolean wanted = input.getPublish();
        boolean published = wanted != null ? wanted : current.map(Entry::published).orElse(false);
        OffsetDateTime publishedAt = !published ? null
                : current.filter(Entry::published).map(Entry::publishedAt).orElse(now);
        return new Grade(manual != null ? manual : auto, auto, manual,
                input.getFeedback() != null ? input.getFeedback() : current.map(Entry::feedback).orElse(null),
                input.getTeacherComment() != null ? input.getTeacherComment()
                        : current.map(Entry::teacherComment).orElse(null),
                status, published, publishedAt, item.points(), null);
    }

    private List<RubricCell> rubric(GradedItem item, List<UUID> ids, String prefix, List<FieldError> errors) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Set<UUID> wanted = new LinkedHashSet<>(ids);
        if (item.rubricId() == null) {
            errors.add(new FieldError(prefix + "grade.rubricCellIds", "the item has no rubric", "INVALID"));
            return List.of();
        }
        List<RubricCell> cells = repository.rubricCells(item.rubricId(), wanted);
        if (cells.size() != wanted.size()) {
            errors.add(new FieldError(prefix + "grade.rubricCellIds", "are not all cells of the item's rubric",
                    "NOT_FOUND"));
        }
        Set<UUID> criteria = new HashSet<>();
        if (cells.stream().anyMatch(c -> !criteria.add(c.criterionId()))) {
            errors.add(new FieldError(prefix + "grade.rubricCellIds", "choose one level per criterion", "DUPLICATE"));
        }
        return cells;
    }

    /** The rubric's points: each chosen level's score share of its criterion's weight of the item's points. */
    private static BigDecimal rubricPoints(GradedItem item, List<RubricCell> cells) {
        return cells.stream().map(c -> cellPoints(item, c)).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal cellPoints(GradedItem item, RubricCell cell) {
        if (item.points() == null || cell.weightPercent() == null || cell.scorePercent() == null) {
            return null;
        }
        return item.points().multiply(cell.weightPercent()).multiply(cell.scorePercent())
                .divide(HUNDRED.multiply(HUNDRED), 2, RoundingMode.HALF_UP);
    }

    private static void text(String value, String field, List<FieldError> errors) {
        if (value != null && value.length() > MAX_TEXT) {
            errors.add(new FieldError(field, "must be at most " + MAX_TEXT + " characters", "Size"));
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
