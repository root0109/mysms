package com.mysms.academics.assessment.business;

import com.mysms.academics.assessment.config.AssessmentDatabaseConfig;
import com.mysms.academics.assessment.config.AssessmentEventsConfig;
import com.mysms.academics.assessment.domain.MarksheetEntity;
import com.mysms.academics.assessment.events.AssessmentEvents;
import com.mysms.academics.assessment.mapper.MarksheetMapper;
import com.mysms.academics.assessment.repository.ExamResultRepository;
import com.mysms.academics.assessment.repository.MarksheetRepository;
import com.mysms.academics.assessment.repository.ResultTemplateRepository;
import com.mysms.authclient.DataScope;
import com.mysms.academics.assessment.api.model.CancelMarksheetRequest;
import com.mysms.academics.assessment.api.model.Marksheet;
import com.mysms.academics.assessment.api.model.MarksheetUpdate;
import com.mysms.academics.assessment.api.model.ResetMarksheetRequest;
import com.mysms.academics.assessment.api.model.ValidateMarksheetRequest;
import com.mysms.academics.assessment.api.model.PublishMarksheetRequest;
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
import com.mysms.core.web.Patches;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of assessment.marksheet: audit and domain event commit in the same transaction as every change; rows
 * outside the caller's institutions are invisible, tenant-level rows change only through a tenant-wide grant.
 * <p>Cancelling a marksheet releases what generation fixed: its exam results leave it and can be corrected again, and
 * its result template returns to draft once none of its marksheets is live, so results can be generated anew.
 */
@Service
public class MarksheetService extends AppTransactionManager {

    public static final String AGGREGATE = MarksheetEntity.TABLE;
    static final String PERMISSION_LIST = "assessment.marksheet.read";
    static final String PERMISSION_GET = "assessment.marksheet.read";
    static final String PERMISSION_CREATE = "assessment.marksheet.create";
    static final String PERMISSION_UPDATE = "assessment.marksheet.update";
    static final String PERMISSION_DELETE = "assessment.marksheet.delete";
    static final String PERMISSION_CANCEL_MARKSHEET = "assessment.marksheet.cancel_marksheet";
    static final String PERMISSION_RESET_MARKSHEET = "assessment.marksheet.reset_marksheet";
    static final String PERMISSION_VALIDATE_MARKSHEET = "assessment.marksheet.validate_marksheet";
    static final String PERMISSION_PUBLISH_MARKSHEET = "assessment.marksheet.publish";


    private final MarksheetRepository repository;
    private final MarksheetMapper mapper;
    private final ExamResultRepository examResults;
    private final ResultTemplateRepository templates;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public MarksheetService(MarksheetRepository repository, MarksheetMapper mapper, ExamResultRepository examResults,
            ResultTemplateRepository templates,
            @Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AssessmentDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AssessmentEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(AssessmentEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.examResults = examResults;
        this.templates = templates;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public Marksheet getMarksheet(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Marksheet> listMarksheets(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, MarksheetRepository.SORT_FIELDS, MarksheetRepository.DEFAULT_ORDER,
                MarksheetRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, MarksheetRepository.FILTER_FIELDS)
                .and(MarksheetRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<MarksheetEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Marksheet createMarksheet(Marksheet request, boolean validateOnly) {
        validator.validate(request);
        MarksheetEntity entity = mapper.toEntity(request);
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
            MarksheetEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + AssessmentEvents.CREATED, AGGREGATE, stored.getId(),
                    AssessmentEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public Marksheet updateMarksheet(UUID id, String ifMatch, MarksheetUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            MarksheetEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + AssessmentEvents.UPDATED, AGGREGATE, id, AssessmentEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteMarksheet(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            MarksheetEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByMarksheetLineMarksheetId(id)) {
                throw new InvalidStateException(
                        "The marksheet is still referenced by assessment.marksheet_line rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + AssessmentEvents.DELETED, AGGREGATE, id, AssessmentEvents.payload(id));
        });
    }

    /**
     * assessment.CancelMarksheet: Cancel. The row must be visible and writable for the caller, and the step is allowed
     * while status is draft or validated; in any other state the answer is 409 and nothing changes. The marksheet's
     * exam results are released and its template reopened, in the same transaction.
     */
    public Marksheet cancelMarksheet(UUID id, CancelMarksheetRequest request) {
        DataScope scope = DataScope.current(PERMISSION_CANCEL_MARKSHEET);
        return doInWriteTransaction(() -> {
            MarksheetEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("draft", "validated").contains(entity.getStatus()))) {
                throw new InvalidStateException("The marksheet cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            UUID actor = Actor.current().orElse(null);
            String reason = request == null ? null : request.getReason();
            if (repository.cancelMarksheet(id, entity.getRowVersion(), now, actor) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "cancel",
                    List.of("status", "publishedAt", "publishedByUserId", "visibleToGuardians"), reason);
            outbox.publish(AGGREGATE + ".cancel", AGGREGATE, id, AssessmentEvents.payload(id));
            for (UUID result : examResults.unlinkMarksheet(id, now, actor)) {
                audit.recordTransition(ExamResultService.AGGREGATE, result, "releaseResult",
                        List.of("marksheetLineId", "gradeLabel", "gradePoint"), reason);
                outbox.publish(ExamResultService.AGGREGATE + ".resultReleased", ExamResultService.AGGREGATE, result,
                        AssessmentEvents.payload(result));
            }
            UUID template = entity.getResultTemplateId();
            if (templates.reopen(template, now, actor) > 0) {
                audit.recordTransition(ResultTemplateService.AGGREGATE, template, "reopen", List.of("status"), reason);
                outbox.publish(ResultTemplateService.AGGREGATE + ".reopened", ResultTemplateService.AGGREGATE, template,
                        AssessmentEvents.payload(template));
            }
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * assessment.ResetMarksheet: Back to draft. The row must be visible and writable for the caller, and the step is
     * allowed while status is cancelled and the marksheet has no generated lines (cancelling released their results);
     * otherwise the answer is 409 and nothing changes.
     */
    public Marksheet resetMarksheet(UUID id, ResetMarksheetRequest request) {
        DataScope scope = DataScope.current(PERMISSION_RESET_MARKSHEET);
        return doInWriteTransaction(() -> {
            MarksheetEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("cancelled").contains(entity.getStatus()))) {
                throw new InvalidStateException("The marksheet cannot be changed this way in its current state.");
            }
            if (repository.referencedByMarksheetLineMarksheetId(id)) {
                throw new InvalidStateException(
                        "A cancelled marksheet with generated lines stays cancelled; generate the results again.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.resetMarksheet(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "reset",
                    List.of("status", "publishedAt", "publishedByUserId", "visibleToGuardians"), null);
            outbox.publish(AGGREGATE + ".reset", AGGREGATE, id, AssessmentEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** Validate generated rows only after every official result for the session is linked and finalized. */
    public Marksheet validateMarksheet(UUID id, ValidateMarksheetRequest request) {
        DataScope scope = DataScope.current(PERMISSION_VALIDATE_MARKSHEET);
        return doInWriteTransaction(() -> {
            MarksheetEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!"draft".equals(entity.getStatus())) {
                throw new InvalidStateException("Only a draft marksheet can be validated.");
            }
            if (!repository.hasCompleteResults(id)) {
                throw new InvalidStateException("The marksheet has missing or unfinished exam results.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            UUID actor = Actor.current().orElse(null);
            if (repository.validateMarksheet(id, entity.getRowVersion(), now, actor) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "validate", List.of("status"), null);
            outbox.publish(AGGREGATE + ".validated", AGGREGATE, id, AssessmentEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** Publish a validated marksheet once; students see it and guardians when explicitly enabled. */
    public Marksheet publishMarksheet(UUID id, String ifMatch, PublishMarksheetRequest request) {
        int expected = Etags.expectedVersion(ifMatch);
        DataScope scope = DataScope.current(PERMISSION_PUBLISH_MARKSHEET);
        return doInWriteTransaction(() -> {
            MarksheetEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (!"validated".equals(entity.getStatus()) || entity.getPublishedAt() != null) {
                throw new InvalidStateException("Only an unpublished validated marksheet can be published.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            UUID actor = Actor.current().orElse(null);
            boolean visibleToGuardians = request != null && Boolean.TRUE.equals(request.getVisibleToGuardians());
            if (repository.publishMarksheet(id, expected, now, actor, visibleToGuardians) == 0) {
                throw new PreconditionFailedException("The marksheet changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "publish", List.of("publishedAt", "publishedByUserId",
                    "visibleToGuardians"), null);
            outbox.publish(AGGREGATE + ".published", AGGREGATE, id, AssessmentEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private MarksheetEntity loadVisible(DataScope scope, UUID id) {
        MarksheetEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the assessment.exam_session the row belongs to. */
    private Optional<UUID> institutionOf(MarksheetEntity entity) {
        return repository.institutionOf(entity.getExamSessionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(MarksheetEntity entity, MarksheetUpdate changes) {
        Patches patch = new Patches();
        patch.apply("visibleToGuardians", changes.getVisibleToGuardians(), entity::setVisibleToGuardians);
        patch.apply("examSessionId", changes.getExamSessionId(), entity::setExamSessionId);
        patch.apply("resultTemplateId", changes.getResultTemplateId(), entity::setResultTemplateId);
        patch.apply("name", changes.getName(), entity::setName);
        patch.apply("generatedOn", changes.getGeneratedOn(), entity::setGeneratedOn);
        patch.apply("generatedByUserId", changes.getGeneratedByUserId(), entity::setGeneratedByUserId);
        patch.apply("totalPassed", changes.getTotalPassed(), entity::setTotalPassed);
        patch.apply("totalFailed", changes.getTotalFailed(), entity::setTotalFailed);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(MarksheetEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (!creating && entity.getVisibleToGuardians() == null) {
            errors.add(new FieldError("visibleToGuardians", "must not be null", "NotNull"));
        }
        if (entity.getExamSessionId() == null) {
            errors.add(new FieldError("examSessionId", "must not be null", "NotNull"));
        }
        if (entity.getResultTemplateId() == null) {
            errors.add(new FieldError("resultTemplateId", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (entity.getGeneratedOn() == null) {
            errors.add(new FieldError("generatedOn", "must not be null", "NotNull"));
        }
        if (entity.getGeneratedByUserId() == null) {
            errors.add(new FieldError("generatedByUserId", "must not be null", "NotNull"));
        }
        if (entity.getExamSessionId() != null && !repository.existsExamSessionId(entity.getExamSessionId())) {
            errors.add(new FieldError("examSessionId", "exam session does not exist", "NOT_FOUND"));
        }
        if (entity.getResultTemplateId() != null && !repository.existsResultTemplateId(entity.getResultTemplateId())) {
            errors.add(new FieldError("resultTemplateId", "result template does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(MarksheetEntity entity) {

    }
}
