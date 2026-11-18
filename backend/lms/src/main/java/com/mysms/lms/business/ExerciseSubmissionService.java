package com.mysms.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
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
import com.mysms.lms.api.model.ExerciseSubmission;
import com.mysms.lms.api.model.ExerciseTestResult;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.ExerciseSubmissionEntity;
import com.mysms.lms.domain.ExerciseTestResultEntity;
import com.mysms.lms.mapper.ExerciseSubmissionMapper;
import com.mysms.lms.mapper.ExerciseTestResultMapper;
import com.mysms.lms.repository.ExerciseSubmissionRepository;
import com.mysms.lms.repository.ExerciseTestResultRepository;
import java.time.Clock;
import com.mysms.lms.events.LmsEvents;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.time.temporal.ChronoUnit;
import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import com.mysms.lms.repository.ExerciseRunRepository;
import com.mysms.lms.api.model.TestOutput;
import com.mysms.lms.api.model.SubmitExerciseRequest;
import com.mysms.core.id.UuidV7;
import com.mysms.authclient.AuthContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.exercise_submission and of the records that exist only as part of a ExerciseSubmission
 * (ExerciseTestResult): audit and domain event commit in the same transaction as every change. Rows outside the
 * caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class ExerciseSubmissionService extends AppTransactionManager {

    public static final String AGGREGATE = ExerciseSubmissionEntity.TABLE;
    static final String PERMISSION_LIST = "lms.exercise_submission.read";
    static final String PERMISSION_GET = "lms.exercise_submission.read";

    public static final String EXERCISE_TEST_RESULT_AGGREGATE = ExerciseTestResultEntity.TABLE;
    static final String EXERCISE_TEST_RESULT_PERMISSION_LIST = "lms.exercise_test_result.read";
    static final String EXERCISE_TEST_RESULT_PERMISSION_GET = "lms.exercise_test_result.read";

    private final ExerciseSubmissionRepository repository;
    private final ExerciseSubmissionMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final ExerciseTestResultRepository exerciseTestResultRepository;
    private final ExerciseTestResultMapper exerciseTestResultMapper;
    private final ExerciseRunRepository runs;

    public ExerciseSubmissionService(ExerciseSubmissionRepository repository,
            ExerciseSubmissionMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            ExerciseTestResultRepository exerciseTestResultRepository,
            ExerciseTestResultMapper exerciseTestResultMapper,
            ExerciseRunRepository runs) {
        super(database, transactions);
        this.runs = runs;
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.exerciseTestResultRepository = exerciseTestResultRepository;
        this.exerciseTestResultMapper = exerciseTestResultMapper;
    }

    public ExerciseSubmission getExerciseSubmission(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<ExerciseSubmission> listExerciseSubmissions(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ExerciseSubmissionRepository.SORT_FIELDS,
                        ExerciseSubmissionRepository.DEFAULT_ORDER,
                ExerciseSubmissionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ExerciseSubmissionRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<ExerciseSubmissionEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ExerciseSubmissionEntity loadVisible(DataScope scope, UUID id) {
        ExerciseSubmissionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOf(ExerciseSubmissionEntity entity) {
        return Optional.empty();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(ExerciseSubmissionEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getExerciseId() == null) {
            errors.add(new FieldError("exerciseId", "must not be null", "NotNull"));
        }
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (entity.getCode() == null) {
            errors.add(new FieldError("code", "must not be null", "NotNull"));
        }
        if (entity.getExerciseId() != null && !repository.existsExerciseId(entity.getExerciseId())) {
            errors.add(new FieldError("exerciseId", "programming exercise does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(ExerciseSubmissionEntity entity) {

    }

    // ---- ExerciseTestResult: a record of the ExerciseSubmission aggregate ----

    public ExerciseTestResult getExerciseTestResult(UUID exerciseSubmissionId, UUID id) {
        return doInReadTransaction(() -> {
            return exerciseTestResultMapper.toDto(loadVisibleExerciseTestResult(DataScope.current(
                    EXERCISE_TEST_RESULT_PERMISSION_GET), exerciseSubmissionId, id));
        });
    }

    public Page<ExerciseTestResult> listExerciseTestResults(UUID exerciseSubmissionId, Integer pageNumber,
            Integer pageSize, String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ExerciseTestResultRepository.SORT_FIELDS,
                        ExerciseTestResultRepository.DEFAULT_ORDER,
                ExerciseTestResultRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ExerciseTestResultRepository.FILTER_FIELDS)
                .and(ExerciseTestResultRepository.ofParent(exerciseSubmissionId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + exerciseSubmissionId, clock);
        return doInReadTransaction(() -> {
            Page<ExerciseTestResultEntity> rows = exerciseTestResultRepository.list(page, where);
            return rows.map(exerciseTestResultMapper::toDto);
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ExerciseTestResultEntity loadVisibleExerciseTestResult(DataScope scope, UUID exerciseSubmissionId, UUID id) {
        ExerciseTestResultEntity entity = exerciseTestResultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(EXERCISE_TEST_RESULT_AGGREGATE, id));
        if (!scope.canRead(institutionOfExerciseTestResult(entity))
                || !exerciseSubmissionId.equals(entity.getExerciseSubmissionId())) {
            throw new ResourceNotFoundException(EXERCISE_TEST_RESULT_AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOfExerciseTestResult(ExerciseTestResultEntity entity) {
        return Optional.empty();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidExerciseTestResult(ExerciseTestResultEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getExerciseSubmissionId() == null) {
            errors.add(new FieldError("exerciseSubmissionId", "must not be null", "NotNull"));
        }
        if (entity.getStatus() == null) {
            errors.add(new FieldError("status", "must not be null", "NotNull"));
        }
        if (entity.getExerciseSubmissionId() != null
                && !exerciseTestResultRepository.existsExerciseSubmissionId(entity.getExerciseSubmissionId())) {
            errors.add(new FieldError("exerciseSubmissionId", "exercise submission does not exist", "NOT_FOUND"));
        }
        if (entity.getTestCaseId() != null && !exerciseTestResultRepository.existsTestCaseId(entity.getTestCaseId())) {
            errors.add(new FieldError("testCaseId", "exercise test case does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueExerciseTestResult(ExerciseTestResultEntity entity) {

    }

    // ---- SubmitExercise ----

    /** Longest code a submission keeps, and longest output a test result keeps. */
    static final int MAX_CODE = 100_000;
    static final int MAX_OUTPUT = 20_000;
    static final String PERMISSION_SUBMIT = "lms.programming_exercise.submit_exercise";

    /**
     * Stores the caller's code with the outputs their client's runner produced, and judges each output against its
     * test case: equal once line endings and trailing white space are set aside. The submission passes when every test
     * case of the exercise passed; a test case without an output fails. A hidden test case's input and expected output
     * are never copied onto the result the student reads.
     */
    public ExerciseSubmission submitExercise(UUID exerciseId, SubmitExerciseRequest request) {
        validator.validate(request);
        List<FieldError> errors = new ArrayList<>();
        if (request.getCode().length() > MAX_CODE) {
            errors.add(new FieldError("code", "must be at most " + MAX_CODE + " characters", "Size"));
        }
        Map<UUID, String> outputs = new HashMap<>();
        for (TestOutput output : request.getTestOutputs()) {
            if (outputs.put(output.getTestCaseId(), output.getActualOutput()) != null) {
                errors.add(new FieldError("testOutputs", "test case " + output.getTestCaseId()
                        + " appears more than once", "DUPLICATE"));
            }
            if (output.getActualOutput().length() > MAX_OUTPUT) {
                errors.add(new FieldError("testOutputs", "an output is at most " + MAX_OUTPUT + " characters",
                        "Size"));
            }
        }
        UUID student = AuthContext.require().userId();
        return doInWriteTransaction(() -> {
            if (!runs.exists(exerciseId)) {
                throw new ResourceNotFoundException("lms.programming_exercise", exerciseId);
            }
            List<ExerciseRunRepository.TestCase> cases = runs.testCases(exerciseId);
            Set<UUID> known = new HashSet<>();
            cases.forEach(c -> known.add(c.id()));
            outputs.keySet().stream().filter(id -> !known.contains(id)).forEach(id -> errors.add(
                    new FieldError("testOutputs", "test case " + id + " is not part of this exercise", "NOT_FOUND")));
            if (!errors.isEmpty()) {
                throw new ValidationFailedException(errors);
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            boolean allPassed = !cases.isEmpty();
            List<String> statuses = new ArrayList<>();
            for (ExerciseRunRepository.TestCase test : cases) {
                String actual = outputs.get(test.id());
                boolean passed = actual != null && sameOutput(actual, test.expectedOutput());
                allPassed &= passed;
                statuses.add(passed ? "passed" : "failed");
            }
            UUID id = UuidV7.next(clock);
            runs.insertSubmission(id, exerciseId, student, request.getCode(),
                    Boolean.TRUE.equals(request.getIsFullCode()), allPassed ? "passed" : "failed", now);
            for (int i = 0; i < cases.size(); i++) {
                UUID resultId = UuidV7.next(clock);
                runs.insertResult(resultId, id, cases.get(i), outputs.get(cases.get(i).id()), statuses.get(i), now,
                        student);
                audit.recordCreate(EXERCISE_TEST_RESULT_AGGREGATE, resultId);
            }
            audit.recordCreate(AGGREGATE, id);
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, id, LmsEvents.payload(id));
            return mapper.toDto(repository.findById(id).orElseThrow());
        });
    }

    /** Equal outputs: compared line by line without trailing white space, ignoring trailing empty lines. */
    static boolean sameOutput(String actual, String expected) {
        return normalised(actual).equals(normalised(expected));
    }

    private static String normalised(String output) {
        String[] lines = output.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        StringBuilder text = new StringBuilder();
        for (String line : lines) {
            text.append(line.stripTrailing()).append('\n');
        }
        return text.toString().stripTrailing();
    }
}
