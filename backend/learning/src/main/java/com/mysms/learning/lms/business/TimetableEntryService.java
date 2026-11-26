package com.mysms.learning.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.learning.lms.api.model.TimetableEntry;
import com.mysms.learning.lms.api.model.TimetableEntryUpdate;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.Patches;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.config.LmsEventsConfig;
import com.mysms.learning.lms.domain.TimetableEntryEntity;
import com.mysms.learning.lms.events.LmsEvents;
import com.mysms.learning.lms.mapper.TimetableEntryMapper;
import com.mysms.learning.lms.repository.TimetableEntryRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import com.mysms.core.error.InvalidStateException;
import com.mysms.learning.lms.api.model.TimetableEntryList;
import com.mysms.learning.lms.domain.CohortEntity;
import com.mysms.learning.lms.domain.TimetableLegendEntity;
import com.mysms.learning.lms.repository.CohortRepository;
import com.mysms.learning.lms.repository.CohortTimetableRepository;
import com.mysms.learning.lms.repository.TimetableLegendRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * Business service of lms.timetable_entry: audit and domain event commit in the same transaction as every change. Rows
 * outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class TimetableEntryService extends AppTransactionManager {

    public static final String AGGREGATE = TimetableEntryEntity.TABLE;
    static final String PERMISSION_LIST = "lms.timetable_entry.read";
    static final String PERMISSION_GET = "lms.timetable_entry.read";
    static final String PERMISSION_CREATE = "lms.timetable_entry.create";
    static final String PERMISSION_UPDATE = "lms.timetable_entry.update";
    static final String PERMISSION_DELETE = "lms.timetable_entry.delete";


    private final TimetableEntryRepository repository;
    private final TimetableEntryMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final CohortRepository cohorts;
    private final CohortTimetableRepository timetables;
    private final TimetableLegendRepository legends;
    private final CohortAccess access;

    public TimetableEntryService(TimetableEntryRepository repository, TimetableEntryMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox, @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock, CohortRepository cohorts, CohortTimetableRepository timetables,
            TimetableLegendRepository legends, CohortAccess access) {

        super(database, transactions);
        this.cohorts = cohorts;
        this.timetables = timetables;
        this.legends = legends;
        this.access = access;
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public TimetableEntry getTimetableEntry(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<TimetableEntry> listTimetableEntrys(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, TimetableEntryRepository.SORT_FIELDS, TimetableEntryRepository.DEFAULT_ORDER,
                TimetableEntryRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, TimetableEntryRepository.FILTER_FIELDS)
                .and(TimetableEntryRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<TimetableEntryEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public TimetableEntry createTimetableEntry(TimetableEntry request, boolean validateOnly) {
        validator.validate(request);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("startTime", request.getStartTime(), wire);
        WireValues.checkTime("endTime", request.getEndTime(), wire);
        WireValues.checkDuration("durationMinutes", request.getDurationMinutes(), 60, wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        TimetableEntryEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            assertValid(entity, true);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            TimetableEntryEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public TimetableEntry updateTimetableEntry(UUID id, String ifMatch, TimetableEntryUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("startTime", Patches.value(changes.getStartTime()), wire);
        WireValues.checkTime("endTime", Patches.value(changes.getEndTime()), wire);
        WireValues.checkDuration("durationMinutes", Patches.value(changes.getDurationMinutes()), 60, wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            TimetableEntryEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = apply(entity, changes);
            validator.validate(mapper.toDto(entity));
            assertValid(entity, false);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            if (repository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(AGGREGATE, id, fields);
            outbox.publish(AGGREGATE + LmsEvents.UPDATED, AGGREGATE, id, LmsEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteTimetableEntry(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            TimetableEntryEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + LmsEvents.DELETED, AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private TimetableEntryEntity loadVisible(DataScope scope, UUID id) {
        TimetableEntryEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.cohort the row belongs to. */
    private Optional<UUID> institutionOf(TimetableEntryEntity entity) {
        return repository.institutionOf(entity.getCohortId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(TimetableEntryEntity entity, TimetableEntryUpdate changes) {
        Patches patch = new Patches();
        patch.apply("cohortId", changes.getCohortId(), entity::setCohortId);
        patch.apply("timetableTemplateId", changes.getTimetableTemplateId(), entity::setTimetableTemplateId);
        patch.apply("lessonId", changes.getLessonId(), entity::setLessonId);
        patch.apply("quizId", changes.getQuizId(), entity::setQuizId);
        patch.apply("assignmentId", changes.getAssignmentId(), entity::setAssignmentId);
        patch.apply("exerciseId", changes.getExerciseId(), entity::setExerciseId);
        patch.apply("liveClassId", changes.getLiveClassId(), entity::setLiveClassId);
        patch.apply("entryDate", changes.getEntryDate(), entity::setEntryDate);
        patch.apply("dayNumber", changes.getDayNumber(), entity::setDayNumber);
        patch.apply("startTime", changes.getStartTime(), v -> entity.setStartTime(WireValues.time(v)));
        patch.apply("endTime", changes.getEndTime(), v -> entity.setEndTime(WireValues.time(v)));
        patch.apply("durationMinutes", changes.getDurationMinutes(), v -> entity.setDurationMinutes(WireValues.duration(v, 60)));
        patch.apply("isMilestone", changes.getIsMilestone(), entity::setIsMilestone);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(TimetableEntryEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (!creating && entity.getIsMilestone() == null) {
            errors.add(new FieldError("isMilestone", "must not be null", "NotNull"));
        }
        if (entity.getAssignmentId() != null && !repository.existsAssignmentId(entity.getAssignmentId())) {
            errors.add(new FieldError("assignmentId", "assignment does not exist", "NOT_FOUND"));
        }
        if (entity.getCohortId() != null && !repository.existsCohortId(entity.getCohortId())) {
            errors.add(new FieldError("cohortId", "cohort does not exist", "NOT_FOUND"));
        }
        if (entity.getExerciseId() != null && !repository.existsExerciseId(entity.getExerciseId())) {
            errors.add(new FieldError("exerciseId", "programming exercise does not exist", "NOT_FOUND"));
        }
        if (entity.getLessonId() != null && !repository.existsLessonId(entity.getLessonId())) {
            errors.add(new FieldError("lessonId", "lesson does not exist", "NOT_FOUND"));
        }
        if (entity.getLiveClassId() != null && !repository.existsLiveClassId(entity.getLiveClassId())) {
            errors.add(new FieldError("liveClassId", "live class does not exist", "NOT_FOUND"));
        }
        if (entity.getQuizId() != null && !repository.existsQuizId(entity.getQuizId())) {
            errors.add(new FieldError("quizId", "quiz does not exist", "NOT_FOUND"));
        }
        if (entity.getTimetableTemplateId() != null
                && !repository.existsTimetableTemplateId(entity.getTimetableTemplateId())) {
            errors.add(new FieldError("timetableTemplateId", "timetable template does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(TimetableEntryEntity entity) {

    }

    // ---- a cohort's timetable: reading it, and replacing it from a template ----

    static final String PERMISSION_COHORT_TIMETABLE = "lms.cohort.get_cohort_timetable";
    static final String PERMISSION_APPLY_TEMPLATE = "lms.cohort.apply_timetable_template";
    private static final String LEGEND_AGGREGATE = "lms.timetable_legend";
    private static final int TEMPLATE_PAGE = 500;

    /** lms.GetCohortTimetable: the cohort's timetable entries, for anybody who sees the cohort. */
    public Page<TimetableEntry> getCohortTimetable(UUID cohortId, Integer pageNumber, Integer pageSize,
            String cursor) {
        CohortAccess.member();
        SortSpec sort = SortSpec.parse(null, TimetableEntryRepository.SORT_FIELDS,
                TimetableEntryRepository.DEFAULT_ORDER, TimetableEntryRepository.ID_COLUMN);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, "cohort|" + cohortId, clock);
        return doInReadTransaction(() -> {
            access.visible(cohortId, PERMISSION_COHORT_TIMETABLE);
            return repository.list(page, CohortTimetableRepository.ofCohort(cohortId)).map(mapper::toDto);
        });
    }

    /**
     * lms.ApplyTimetableTemplate: replaces the cohort's timetable with the template's. Each template entry lands on
     * the cohort's start date plus its day number less one (day 1 is the start date); entries without a day number
     * keep no date. The template's legend is copied too, and the cohort records the template. The cohort must be in
     * the caller's scope (404), writable (403) and not archived (409); an unknown template is 422.
     */
    public TimetableEntryList applyTimetableTemplate(UUID cohortId, UUID templateId) {
        DataScope scope = DataScope.current(PERMISSION_APPLY_TEMPLATE);
        return doInWriteTransaction(() -> {
            CohortEntity cohort = cohorts.findForUpdate(cohortId)
                    .orElseThrow(() -> new ResourceNotFoundException(CohortAccess.COHORT, cohortId));
            if (!scope.canRead(CohortAccess.institutionOf(cohort))) {
                throw new ResourceNotFoundException(CohortAccess.COHORT, cohortId);
            }
            scope.requireWritable(CohortAccess.institutionOf(cohort));
            if ("archived".equals(cohort.getLifecycleStatus())) {
                throw new InvalidStateException("An archived cohort is read-only.");
            }
            if (templateId == null) {
                throw new ValidationFailedException(List.of(new FieldError("timetableTemplateId",
                        "must not be null", "NotNull")));
            }
            if (!timetables.templateExists(templateId)) {
                throw new ValidationFailedException(List.of(new FieldError("timetableTemplateId",
                        "timetable template does not exist", "NOT_FOUND")));
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            UUID actor = Actor.current().orElse(null);
            timetables.clear(cohortId, now, actor);
            List<TimetableEntry> applied = new ArrayList<>();
            for (TimetableEntryEntity source : templateRows(templateId)) {
                TimetableEntryEntity copy = TimetableEntryEntity.builder().cohortId(cohortId)
                        .lessonId(source.getLessonId()).quizId(source.getQuizId())
                        .assignmentId(source.getAssignmentId()).exerciseId(source.getExerciseId())
                        .liveClassId(source.getLiveClassId()).dayNumber(source.getDayNumber())
                        .entryDate(source.getDayNumber() == null ? null
                                : cohort.getStartDate().plusDays(source.getDayNumber() - 1L))
                        .startTime(source.getStartTime()).endTime(source.getEndTime())
                        .durationMinutes(source.getDurationMinutes()).isMilestone(source.getIsMilestone()).build();
                copy.onCreatingData(Actor.current(), clock);
                copy.setTenantId(cohort.getTenantId());
                TimetableEntryEntity stored = repository.insert(copy);
                audit.recordCreate(AGGREGATE, stored.getId());
                outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(),
                        LmsEvents.payload(stored.getId()));
                applied.add(mapper.toDto(stored));
            }
            for (TimetableLegendEntity source : templateLegends(templateId)) {
                TimetableLegendEntity copy = TimetableLegendEntity.builder().cohortId(cohortId)
                        .itemKind(source.getItemKind()).color(source.getColor()).label(source.getLabel()).build();
                copy.onCreatingData(Actor.current(), clock);
                copy.setTenantId(cohort.getTenantId());
                UUID legend = legends.insert(copy).getId();
                audit.recordCreate(LEGEND_AGGREGATE, legend);
            }
            if (timetables.setTemplate(cohortId, cohort.getRowVersion(), templateId, now, actor) == 0) {
                throw new PreconditionFailedException("The cohort changed meanwhile; try again.");
            }
            audit.recordTransition(CohortAccess.COHORT, cohortId, "apply_timetable_template",
                    List.of("timetableTemplateId"), null);
            outbox.publish(CohortAccess.COHORT + ".apply_timetable_template", CohortAccess.COHORT, cohortId,
                    LmsEvents.payload(cohortId));
            return new TimetableEntryList(applied);
        });
    }

    private List<TimetableEntryEntity> templateRows(UUID templateId) {
        SortSpec sort = SortSpec.parse(null, TimetableEntryRepository.SORT_FIELDS,
                TimetableEntryRepository.DEFAULT_ORDER, TimetableEntryRepository.ID_COLUMN);
        List<TimetableEntryEntity> rows = new ArrayList<>();
        String cursor = null;
        int number = 1;
        do {
            Page<TimetableEntryEntity> page = repository.list(PageRequest.of(number, TEMPLATE_PAGE, cursor, sort,
                    "template|" + templateId, clock), CohortTimetableRepository.ofTemplate(templateId));
            rows.addAll(page.items());
            cursor = page.hasNext() ? page.nextCursor() : null;
            number++;
        } while (cursor != null);
        return rows;
    }

    private List<TimetableLegendEntity> templateLegends(UUID templateId) {
        SortSpec sort = SortSpec.parse(null, TimetableLegendRepository.SORT_FIELDS,
                TimetableLegendRepository.DEFAULT_ORDER, TimetableLegendRepository.ID_COLUMN);
        return legends.list(PageRequest.of(1, TEMPLATE_PAGE, null, sort, "template|" + templateId, clock),
                CohortTimetableRepository.ofTemplate(templateId)).items();
    }
}
