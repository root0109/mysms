package com.mysms.academics.assessment.business;

import com.mysms.academics.assessment.config.AssessmentDatabaseConfig;
import com.mysms.academics.assessment.config.AssessmentEventsConfig;
import com.mysms.academics.assessment.domain.ExamResultEntity;
import com.mysms.academics.assessment.events.AssessmentEvents;
import com.mysms.academics.assessment.mapper.ExamResultMapper;
import com.mysms.academics.assessment.repository.ExamResultRepository;
import com.mysms.authclient.DataScope;
import com.mysms.academics.assessment.api.model.ExamResult;
import com.mysms.academics.assessment.api.model.ExamResultUpdate;
import com.mysms.core.context.Actor;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
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
import com.mysms.core.web.Patches;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of assessment.exam_result: audit and domain event commit in the same transaction as every change.
 * Rows outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class ExamResultService extends AppTransactionManager {

    public static final String AGGREGATE = ExamResultEntity.TABLE;
    static final String PERMISSION_LIST = "assessment.exam_result.read";
    static final String PERMISSION_GET = "assessment.exam_result.read";
    static final String PERMISSION_UPDATE = "assessment.exam_result.update";
    /**
     * The contract's ExamResultUpdate fields, in apply order. Attendance is not among them: it decides the result
     * status and whether marks are allowed, so it changes only with the marks, through assessment.RecordExamMarks.
     */


    private final ExamResultRepository repository;
    private final ExamResultMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public ExamResultService(ExamResultRepository repository, ExamResultMapper mapper,
            @Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AssessmentDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AssessmentEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(AssessmentEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public ExamResult getExamResult(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<ExamResult> listExamResults(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, ExamResultRepository.SORT_FIELDS, ExamResultRepository.DEFAULT_ORDER,
                ExamResultRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ExamResultRepository.FILTER_FIELDS)
                .and(ExamResultRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<ExamResultEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope. A result already added to a
     * marksheet is immutable (409) until that marksheet is cancelled.
     */
    public ExamResult updateExamResult(UUID id, String ifMatch, ExamResultUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            ExamResultEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            ExamService.requireUnaggregated(entity);
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
            outbox.publish(AGGREGATE + AssessmentEvents.UPDATED, AGGREGATE, id, AssessmentEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ExamResultEntity loadVisible(DataScope scope, UUID id) {
        ExamResultEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the assessment.exam → assessment.exam_session the row belongs to. */
    private Optional<UUID> institutionOf(ExamResultEntity entity) {
        return repository.institutionOf(entity.getExamId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(ExamResultEntity entity, ExamResultUpdate changes) {
        Patches patch = new Patches();
        patch.apply("roomId", changes.getRoomId(), entity::setRoomId);
        patch.apply("note", changes.getNote(), entity::setNote);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(ExamResultEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getExamId() == null) {
            errors.add(new FieldError("examId", "must not be null", "NotNull"));
        }
        if (entity.getStudentId() == null) {
            errors.add(new FieldError("studentId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getAttendanceStatus() == null) {
            errors.add(new FieldError("attendanceStatus", "must not be null", "NotNull"));
        }
        if (entity.getExamId() != null && !repository.existsExamId(entity.getExamId())) {
            errors.add(new FieldError("examId", "exam does not exist", "NOT_FOUND"));
        }
        if (entity.getMarksheetLineId() != null && !repository.existsMarksheetLineId(entity.getMarksheetLineId())) {
            errors.add(new FieldError("marksheetLineId", "marksheet line does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(ExamResultEntity entity) {
        if (repository.takenExamIdStudentId1(entity)) {
            throw new DuplicateResourceException("A exam result with the same exam id and student id already exists.");
        }
    }
}
