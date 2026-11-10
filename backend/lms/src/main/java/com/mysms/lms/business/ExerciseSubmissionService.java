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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.exercise_submission (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a ExerciseSubmission
 * are served here too — ExerciseTestResult.
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

    public ExerciseSubmissionService(ExerciseSubmissionRepository repository,
            ExerciseSubmissionMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            ExerciseTestResultRepository exerciseTestResultRepository,
            ExerciseTestResultMapper exerciseTestResultMapper) {
        super(database, transactions);
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

    /** The institution that owns the row (none: the rows are tenant-level). */
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

    /** The institution that owns the row (none: the rows are tenant-level). */
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
}
