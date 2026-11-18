package com.mysms.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.lms.api.model.ReassignmentLog;
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
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.ReassignmentLogEntity;
import com.mysms.lms.mapper.ReassignmentLogMapper;
import com.mysms.lms.repository.ReassignmentLogRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.reassignment_log: audit and domain event commit in the same transaction as every change. Rows
 * outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class ReassignmentLogService extends AppTransactionManager {

    public static final String AGGREGATE = ReassignmentLogEntity.TABLE;
    static final String PERMISSION_LIST = "lms.reassignment_log.read";
    static final String PERMISSION_GET = "lms.reassignment_log.read";


    private final ReassignmentLogRepository repository;
    private final ReassignmentLogMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public ReassignmentLogService(ReassignmentLogRepository repository, ReassignmentLogMapper mapper,
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

    public ReassignmentLog getReassignmentLog(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<ReassignmentLog> listReassignmentLogs(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ReassignmentLogRepository.SORT_FIELDS, ReassignmentLogRepository.DEFAULT_ORDER,
                ReassignmentLogRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ReassignmentLogRepository.FILTER_FIELDS)
                .and(ReassignmentLogRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<ReassignmentLogEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ReassignmentLogEntity loadVisible(DataScope scope, UUID id) {
        ReassignmentLogEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the lms.content_item → lms.course it belongs to). */
    private Optional<UUID> institutionOf(ReassignmentLogEntity entity) {
        return repository.institutionOf(entity.getContentItemId());
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(ReassignmentLogEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getContentItemId() == null) {
            errors.add(new FieldError("contentItemId", "must not be null", "NotNull"));
        }
        if (entity.getReassignedBy() == null) {
            errors.add(new FieldError("reassignedBy", "must not be null", "NotNull"));
        }
        if (entity.getReassignedAt() == null) {
            errors.add(new FieldError("reassignedAt", "must not be null", "NotNull"));
        }
        if (entity.getContentItemId() != null && !repository.existsContentItemId(entity.getContentItemId())) {
            errors.add(new FieldError("contentItemId", "content item does not exist", "NOT_FOUND"));
        }
        if (entity.getGroupId() != null && !repository.existsGroupId(entity.getGroupId())) {
            errors.add(new FieldError("groupId", "assignment group does not exist", "NOT_FOUND"));
        }
        if (entity.getQuizSubmissionId() != null && !repository.existsQuizSubmissionId(entity.getQuizSubmissionId())) {
            errors.add(new FieldError("quizSubmissionId", "quiz submission does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(ReassignmentLogEntity entity) {

    }
}
