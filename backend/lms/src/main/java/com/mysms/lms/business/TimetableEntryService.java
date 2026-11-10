package com.mysms.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.lms.api.model.TimetableEntry;
import com.mysms.lms.api.model.TimetableEntryUpdate;
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
import com.mysms.core.web.UpdateMask;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.TimetableEntryEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.TimetableEntryMapper;
import com.mysms.lms.repository.TimetableEntryRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.timetable_entry (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 */
@Service
public class TimetableEntryService extends AppTransactionManager {

    public static final String AGGREGATE = TimetableEntryEntity.TABLE;
    static final String PERMISSION_LIST = "lms.timetable_entry.read";
    static final String PERMISSION_GET = "lms.timetable_entry.read";
    static final String PERMISSION_CREATE = "lms.timetable_entry.create";
    static final String PERMISSION_UPDATE = "lms.timetable_entry.update";
    static final String PERMISSION_DELETE = "lms.timetable_entry.delete";
    /** The contract's TimetableEntryUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("cohortId", "timetableTemplateId", "lessonId", "quizId", "assignmentId", "exerciseId",
                    "liveClassId",
            "entryDate", "dayNumber", "startTime", "endTime", "durationMinutes", "isMilestone");


    private final TimetableEntryRepository repository;
    private final TimetableEntryMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public TimetableEntryService(TimetableEntryRepository repository, TimetableEntryMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox, @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
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
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public TimetableEntry updateTimetableEntry(UUID id, String ifMatch, TimetableEntryUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkTime("startTime", changes.getStartTime(), wire);
        WireValues.checkTime("endTime", changes.getEndTime(), wire);
        WireValues.checkDuration("durationMinutes", changes.getDurationMinutes(), 60, wire);
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
            apply(entity, changes, fields);
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

    /** The institution that owns the row (the institution of the lms.cohort it belongs to). */
    private Optional<UUID> institutionOf(TimetableEntryEntity entity) {
        return repository.institutionOf(entity.getCohortId());
    }

    /**
     * Applies the selected TimetableEntryUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void apply(TimetableEntryEntity entity, TimetableEntryUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "cohortId" -> entity.setCohortId(changes.getCohortId());
                case "timetableTemplateId" -> entity.setTimetableTemplateId(changes.getTimetableTemplateId());
                case "lessonId" -> entity.setLessonId(changes.getLessonId());
                case "quizId" -> entity.setQuizId(changes.getQuizId());
                case "assignmentId" -> entity.setAssignmentId(changes.getAssignmentId());
                case "exerciseId" -> entity.setExerciseId(changes.getExerciseId());
                case "liveClassId" -> entity.setLiveClassId(changes.getLiveClassId());
                case "entryDate" -> entity.setEntryDate(changes.getEntryDate());
                case "dayNumber" -> entity.setDayNumber(changes.getDayNumber());
                case "startTime" -> entity.setStartTime(WireValues.time(changes.getStartTime()));
                case "endTime" -> entity.setEndTime(WireValues.time(changes.getEndTime()));
                case "durationMinutes" -> entity.setDurationMinutes(
                        WireValues.duration(changes.getDurationMinutes(), 60));
                case "isMilestone" -> entity.setIsMilestone(changes.getIsMilestone());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
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
}
