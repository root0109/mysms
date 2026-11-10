package com.mysms.assessment.business;

import com.mysms.assessment.config.AssessmentDatabaseConfig;
import com.mysms.assessment.config.AssessmentEventsConfig;
import com.mysms.assessment.domain.MarksheetLineEntity;
import com.mysms.assessment.mapper.MarksheetLineMapper;
import com.mysms.assessment.repository.MarksheetLineRepository;
import com.mysms.authclient.DataScope;
import com.mysms.assessment.api.model.MarksheetLine;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
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
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of assessment.marksheet_line (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 */
@Service
public class MarksheetLineService extends AppTransactionManager {

    public static final String AGGREGATE = MarksheetLineEntity.TABLE;
    static final String PERMISSION_LIST = "assessment.marksheet_line.read";
    static final String PERMISSION_GET = "assessment.marksheet_line.read";


    private final MarksheetLineRepository repository;
    private final MarksheetLineMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public MarksheetLineService(MarksheetLineRepository repository, MarksheetLineMapper mapper,
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

    public MarksheetLine getMarksheetLine(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<MarksheetLine> listMarksheetLines(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, MarksheetLineRepository.SORT_FIELDS, MarksheetLineRepository.DEFAULT_ORDER,
                MarksheetLineRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, MarksheetLineRepository.FILTER_FIELDS)
                .and(MarksheetLineRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<MarksheetLineEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private MarksheetLineEntity loadVisible(DataScope scope, UUID id) {
        MarksheetLineEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /**
     * The institution that owns the row (the institution of the assessment.marksheet → assessment.exam_session it
     * belongs to).
     */
    private Optional<UUID> institutionOf(MarksheetLineEntity entity) {
        return repository.institutionOf(entity.getMarksheetId());
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(MarksheetLineEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getMarksheetId() == null) {
            errors.add(new FieldError("marksheetId", "must not be null", "NotNull"));
        }
        if (entity.getStudentId() == null) {
            errors.add(new FieldError("studentId", "must not be null", "NotNull"));
        }
        if (entity.getGeneratedOn() == null) {
            errors.add(new FieldError("generatedOn", "must not be null", "NotNull"));
        }
        if (entity.getMarksheetId() != null && !repository.existsMarksheetId(entity.getMarksheetId())) {
            errors.add(new FieldError("marksheetId", "marksheet does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(MarksheetLineEntity entity) {
        if (repository.takenMarksheetIdStudentId1(entity)) {
            throw new DuplicateResourceException(
                    "A marksheet line with the same marksheet id and student id already exists.");
        }
    }
}
