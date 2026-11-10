package com.mysms.lms.business;

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
import com.mysms.core.filter.FilterParser;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.UpdateMask;
import com.mysms.lms.api.model.ExerciseTestCase;
import com.mysms.lms.api.model.ExerciseTestCaseUpdate;
import com.mysms.lms.api.model.ProgrammingExercise;
import com.mysms.lms.api.model.ProgrammingExerciseUpdate;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.ExerciseTestCaseEntity;
import com.mysms.lms.domain.ProgrammingExerciseEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.ExerciseTestCaseMapper;
import com.mysms.lms.mapper.ProgrammingExerciseMapper;
import com.mysms.lms.repository.ExerciseTestCaseRepository;
import com.mysms.lms.repository.ProgrammingExerciseRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.programming_exercise (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a ProgrammingExercise
 * are served here too — ExerciseTestCase.
 */
@Service
public class ProgrammingExerciseService extends AppTransactionManager {

    public static final String AGGREGATE = ProgrammingExerciseEntity.TABLE;
    static final String PERMISSION_LIST = "lms.programming_exercise.read";
    static final String PERMISSION_GET = "lms.programming_exercise.read";
    static final String PERMISSION_CREATE = "lms.programming_exercise.create";
    static final String PERMISSION_UPDATE = "lms.programming_exercise.update";
    static final String PERMISSION_DELETE = "lms.programming_exercise.delete";
    /** The contract's ProgrammingExerciseUpdate fields, in apply order. */
    static final List<String> WRITABLE = List.of("title", "problemStatement", "language", "starterCode");

    public static final String EXERCISE_TEST_CASE_AGGREGATE = ExerciseTestCaseEntity.TABLE;
    static final String EXERCISE_TEST_CASE_PERMISSION_LIST = "lms.exercise_test_case.read";
    static final String EXERCISE_TEST_CASE_PERMISSION_GET = "lms.exercise_test_case.read";
    static final String EXERCISE_TEST_CASE_PERMISSION_CREATE = "lms.exercise_test_case.create";
    static final String EXERCISE_TEST_CASE_PERMISSION_UPDATE = "lms.exercise_test_case.update";
    static final String EXERCISE_TEST_CASE_PERMISSION_DELETE = "lms.exercise_test_case.delete";
    /** The contract's ExerciseTestCaseUpdate fields, in apply order. */
    static final List<String> EXERCISE_TEST_CASE_WRITABLE = List.of("sortOrder", "input", "expectedOutput", "isHidden");

    private final ProgrammingExerciseRepository repository;
    private final ProgrammingExerciseMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final ExerciseTestCaseRepository exerciseTestCaseRepository;
    private final ExerciseTestCaseMapper exerciseTestCaseMapper;

    public ProgrammingExerciseService(ProgrammingExerciseRepository repository,
            ProgrammingExerciseMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            ExerciseTestCaseRepository exerciseTestCaseRepository,
            ExerciseTestCaseMapper exerciseTestCaseMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.exerciseTestCaseRepository = exerciseTestCaseRepository;
        this.exerciseTestCaseMapper = exerciseTestCaseMapper;
    }

    public ProgrammingExercise getProgrammingExercise(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<ProgrammingExercise> listProgrammingExercises(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ProgrammingExerciseRepository.SORT_FIELDS,
                        ProgrammingExerciseRepository.DEFAULT_ORDER,
                ProgrammingExerciseRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ProgrammingExerciseRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<ProgrammingExerciseEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ProgrammingExercise createProgrammingExercise(ProgrammingExercise request, boolean validateOnly) {
        validator.validate(request);
        ProgrammingExerciseEntity entity = mapper.toEntity(request);
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
            ProgrammingExerciseEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public ProgrammingExercise updateProgrammingExercise(UUID id, String ifMatch, ProgrammingExerciseUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            ProgrammingExerciseEntity entity = loadVisible(scope, id);
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
    public void deleteProgrammingExercise(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            ProgrammingExerciseEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByContentAuthorExerciseId(id)) {
                throw new InvalidStateException(
                        "The programming exercise is still referenced by lms.content_author rows; remove those first.");
            }
            if (repository.referencedByContentItemExerciseId(id)) {
                throw new InvalidStateException(
                        "The programming exercise is still referenced by lms.content_item rows; remove those first.");
            }
            if (repository.referencedByExerciseSubmissionExerciseId(id)) {
                throw new InvalidStateException(
                        "The programming exercise is still referenced by lms.exercise_submission rows; remove those "
                                + "first.");
            }
            if (repository.referencedByExerciseTestCaseExerciseId(id)) {
                throw new InvalidStateException(
                        "The programming exercise is still referenced by lms.exercise_test_case rows; remove those "
                                + "first.");
            }
            if (repository.referencedByLessonActivityExerciseId(id)) {
                throw new InvalidStateException(
                        "The programming exercise is still referenced by lms.lesson_activity rows; remove those "
                                + "first.");
            }
            if (repository.referencedByTimetableEntryExerciseId(id)) {
                throw new InvalidStateException(
                        "The programming exercise is still referenced by lms.timetable_entry rows; remove those "
                                + "first.");
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
    private ProgrammingExerciseEntity loadVisible(DataScope scope, UUID id) {
        ProgrammingExerciseEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOf(ProgrammingExerciseEntity entity) {
        return Optional.empty();
    }

    /**
     * Applies the selected ProgrammingExerciseUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void apply(ProgrammingExerciseEntity entity, ProgrammingExerciseUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "title" -> entity.setTitle(changes.getTitle());
                case "problemStatement" -> entity.setProblemStatement(changes.getProblemStatement());
                case "language" -> entity.setLanguage(Optional.ofNullable(changes.getLanguage()).map(v -> v.getValue())
                        .orElse(null));
                case "starterCode" -> entity.setStarterCode(changes.getStarterCode());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(ProgrammingExerciseEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        if (entity.getProblemStatement() == null) {
            errors.add(new FieldError("problemStatement", "must not be null", "NotNull"));
        }
        if (entity.getLanguage() == null) {
            errors.add(new FieldError("language", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(ProgrammingExerciseEntity entity) {

    }

    // ---- ExerciseTestCase: a record of the ProgrammingExercise aggregate ----

    public ExerciseTestCase getExerciseTestCase(UUID exerciseId, UUID id) {
        return doInReadTransaction(() -> {
            return exerciseTestCaseMapper.toDto(loadVisibleExerciseTestCase(DataScope.current(
                    EXERCISE_TEST_CASE_PERMISSION_GET), exerciseId, id));
        });
    }

    public Page<ExerciseTestCase> listExerciseTestCases(UUID exerciseId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ExerciseTestCaseRepository.SORT_FIELDS,
                        ExerciseTestCaseRepository.DEFAULT_ORDER,
                ExerciseTestCaseRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ExerciseTestCaseRepository.FILTER_FIELDS)
                .and(ExerciseTestCaseRepository.ofParent(exerciseId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + exerciseId, clock);
        return doInReadTransaction(() -> {
            Page<ExerciseTestCaseEntity> rows = exerciseTestCaseRepository.list(page, where);
            return rows.map(exerciseTestCaseMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ExerciseTestCase createExerciseTestCase(UUID exerciseId, ExerciseTestCase request, boolean validateOnly) {
        validator.validate(request);
        ExerciseTestCaseEntity entity = exerciseTestCaseMapper.toEntity(request);
        entity.setExerciseId(exerciseId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(EXERCISE_TEST_CASE_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!exerciseTestCaseRepository.existsExerciseId(entity.getExerciseId())) {
                throw new ResourceNotFoundException("lms.programming_exercise", entity.getExerciseId());
            }
            assertValidExerciseTestCase(entity, true);
            scope.requireWritable(institutionOfExerciseTestCase(entity));
            assertUniqueExerciseTestCase(entity);
            if (validateOnly) {
                return exerciseTestCaseMapper.toDto(entity);
            }
            ExerciseTestCaseEntity stored = exerciseTestCaseRepository.insert(entity);
            audit.recordCreate(EXERCISE_TEST_CASE_AGGREGATE, stored.getId());
            outbox.publish(EXERCISE_TEST_CASE_AGGREGATE + LmsEvents.CREATED, EXERCISE_TEST_CASE_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return exerciseTestCaseMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public ExerciseTestCase updateExerciseTestCase(UUID exerciseId, UUID id, String ifMatch,
            ExerciseTestCaseUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, EXERCISE_TEST_CASE_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(EXERCISE_TEST_CASE_PERMISSION_UPDATE);
            ExerciseTestCaseEntity entity = loadVisibleExerciseTestCase(scope, exerciseId, id);
            scope.requireWritable(institutionOfExerciseTestCase(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyExerciseTestCase(entity, changes, fields);
            validator.validate(exerciseTestCaseMapper.toDto(entity));
            assertValidExerciseTestCase(entity, false);
            scope.requireWritable(institutionOfExerciseTestCase(entity));
            assertUniqueExerciseTestCase(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return exerciseTestCaseMapper.toDto(entity);
            }
            if (exerciseTestCaseRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(EXERCISE_TEST_CASE_AGGREGATE, id, fields);
            outbox.publish(EXERCISE_TEST_CASE_AGGREGATE + LmsEvents.UPDATED, EXERCISE_TEST_CASE_AGGREGATE, id,
                    LmsEvents.payload(id));
            return exerciseTestCaseMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteExerciseTestCase(UUID exerciseId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(EXERCISE_TEST_CASE_PERMISSION_DELETE);
            ExerciseTestCaseEntity entity = loadVisibleExerciseTestCase(scope, exerciseId, id);
            scope.requireWritable(institutionOfExerciseTestCase(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (exerciseTestCaseRepository.referencedByExerciseTestResultTestCaseId(id)) {
                throw new InvalidStateException(
                        "The exercise test case is still referenced by lms.exercise_test_result rows; remove those "
                                + "first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (exerciseTestCaseRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(EXERCISE_TEST_CASE_AGGREGATE, id);
            outbox.publish(EXERCISE_TEST_CASE_AGGREGATE + LmsEvents.DELETED, EXERCISE_TEST_CASE_AGGREGATE, id,
                    LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ExerciseTestCaseEntity loadVisibleExerciseTestCase(DataScope scope, UUID exerciseId, UUID id) {
        ExerciseTestCaseEntity entity = exerciseTestCaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(EXERCISE_TEST_CASE_AGGREGATE, id));
        if (!scope.canRead(institutionOfExerciseTestCase(entity)) || !exerciseId.equals(entity.getExerciseId())) {
            throw new ResourceNotFoundException(EXERCISE_TEST_CASE_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOfExerciseTestCase(ExerciseTestCaseEntity entity) {
        return Optional.empty();
    }

    /**
     * Applies the selected ExerciseTestCaseUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void applyExerciseTestCase(ExerciseTestCaseEntity entity, ExerciseTestCaseUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                case "input" -> entity.setInput(changes.getInput());
                case "expectedOutput" -> entity.setExpectedOutput(changes.getExpectedOutput());
                case "isHidden" -> entity.setIsHidden(changes.getIsHidden());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidExerciseTestCase(ExerciseTestCaseEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getExerciseId() == null) {
            errors.add(new FieldError("exerciseId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getExpectedOutput() == null) {
            errors.add(new FieldError("expectedOutput", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsHidden() == null) {
            errors.add(new FieldError("isHidden", "must not be null", "NotNull"));
        }
        if (entity.getExerciseId() != null && !exerciseTestCaseRepository.existsExerciseId(entity.getExerciseId())) {
            errors.add(new FieldError("exerciseId", "programming exercise does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueExerciseTestCase(ExerciseTestCaseEntity entity) {

    }
}
