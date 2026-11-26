package com.mysms.academics.assessment.business;

import com.mysms.academics.assessment.config.AssessmentDatabaseConfig;
import com.mysms.academics.assessment.config.AssessmentEventsConfig;
import com.mysms.academics.assessment.domain.MarksheetLineEntity;
import com.mysms.academics.assessment.mapper.MarksheetLineMapper;
import com.mysms.academics.assessment.mapper.ExamResultMapper;
import com.mysms.academics.assessment.repository.MarksheetLineRepository;
import com.mysms.academics.assessment.repository.ExamResultRepository;
import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.academics.assessment.api.model.MarksheetLine;
import com.mysms.academics.assessment.api.model.ResultCard;
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
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.validation.DtoValidator;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of assessment.marksheet_line: audit and domain event commit in the same transaction as every change.
 * Rows outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class MarksheetLineService extends AppTransactionManager {

    public static final String AGGREGATE = MarksheetLineEntity.TABLE;
    static final String PERMISSION_LIST = "assessment.marksheet_line.read";
    static final String PERMISSION_GET = "assessment.marksheet_line.read";


    private final MarksheetLineRepository repository;
    private final MarksheetLineMapper mapper;
    private final ExamResultRepository examResultRepository;
    private final ExamResultMapper examResultMapper;
    private final StudentOwnershipLookup ownership;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public MarksheetLineService(MarksheetLineRepository repository, MarksheetLineMapper mapper,
            ExamResultRepository examResultRepository, ExamResultMapper examResultMapper,
            StudentOwnershipLookup ownership,
            @Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AssessmentDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AssessmentEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(AssessmentEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.examResultRepository = examResultRepository;
        this.examResultMapper = examResultMapper;
        this.ownership = ownership;
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

    /**
     * assessment.GetStudentResults: a student's report-card data, one card per validated marksheet line with its
     * per-paper results.
     *
     * <p><b>Staff</b> — callers who read marksheet lines ({@link #PERMISSION_LIST}) — read every validated result of
     * the student in the institutions of their reach, published or not; when the student's results are all outside
     * it the student does not exist for them (404). <b>The student and their guardians</b> read published results
     * only, guardians only those shared with guardians; sis confirms the relation with the caller's token before the
     * read (404 when the student is not theirs, 503 when sis cannot answer), and their cards leave out the
     * teacher-internal details of each result (the note, the room, who evaluated, created and changed it).
     */
    public Page<ResultCard> getStudentResults(UUID studentId, UUID academicYearId, UUID examSessionId,
            Integer pageNumber, Integer pageSize, String cursor) {
        AuthContext caller = AuthContext.require();
        SortSpec sort = SortSpec.parse(null, MarksheetLineRepository.SORT_FIELDS,
                MarksheetLineRepository.DEFAULT_ORDER, MarksheetLineRepository.ID_COLUMN);
        String cursorScope = studentId + "|" + academicYearId + "|" + examSessionId;
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, cursorScope, clock);
        if (caller.hasPermission(PERMISSION_LIST)) {
            DataScope reach = StaffReach.of(caller, PERMISSION_LIST);
            return doInReadTransaction(() -> {
                if (repository.studentResultsOutOfReach(studentId, reach)) {
                    throw new ResourceNotFoundException("sis.student", studentId);
                }
                return cards(repository.listStudentResults(studentId, academicYearId, examSessionId, false, false,
                        page, MarksheetLineRepository.sessionReach(reach)), studentId, false);
            });
        }
        StudentOwnershipLookup.Relation relation = ownership.relationTo(caller, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("sis.student", studentId));
        boolean guardian = relation == StudentOwnershipLookup.Relation.GUARDIAN;
        return doInReadTransaction(() -> cards(repository.listStudentResults(studentId, academicYearId,
                examSessionId, true, guardian, page, SqlFragment.TRUE), studentId, true));
    }

    /** The cards of a page of the student's lines, each with its exam session (read once for the page). */
    private Page<ResultCard> cards(Page<MarksheetLineEntity> lines, UUID studentId, boolean forFamily) {
        Map<UUID, UUID> sessions = repository.examSessionsOfMarksheets(
                lines.items().stream().map(MarksheetLineEntity::getMarksheetId).distinct().toList());
        return lines.map(line -> card(line, sessions.get(line.getMarksheetId()), studentId, forFamily));
    }

    /** One card: the line and the results linked to it; for the student and their family without staff details. */
    private ResultCard card(MarksheetLineEntity line, UUID examSessionId, UUID studentId, boolean forFamily) {
        ResultCard card = new ResultCard(examSessionId);
        card.setMarksheetLine(mapper.toDto(line));
        card.setResults(examResultRepository.listForSessionAndStudent(examSessionId, studentId).stream()
                .filter(result -> line.getId().equals(result.getMarksheetLineId()))
                .map(examResultMapper::toDto)
                .map(result -> forFamily ? result.note(null).roomId(null).evaluatedByUserId(null).createdBy(null)
                        .updatedBy(null) : result)
                .toList());
        return card;
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

    /** The institution of the assessment.marksheet → assessment.exam_session the row belongs to. */
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
