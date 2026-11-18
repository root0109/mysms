package com.mysms.assessment.business;

import com.mysms.assessment.api.model.ResultTemplate;
import com.mysms.assessment.api.model.ResultTemplateBand;
import com.mysms.assessment.api.model.ResultTemplateBandUpdate;
import com.mysms.assessment.api.model.ResultTemplateUpdate;
import com.mysms.assessment.config.AssessmentDatabaseConfig;
import com.mysms.assessment.config.AssessmentEventsConfig;
import com.mysms.assessment.domain.ResultTemplateBandEntity;
import com.mysms.assessment.domain.ResultTemplateEntity;
import com.mysms.assessment.events.AssessmentEvents;
import com.mysms.assessment.mapper.ResultTemplateBandMapper;
import com.mysms.assessment.mapper.ResultTemplateMapper;
import com.mysms.assessment.repository.ResultTemplateBandRepository;
import com.mysms.assessment.repository.ResultTemplateRepository;
import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
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
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of assessment.result_template and of the records that exist only as part of a ResultTemplate
 * (ResultTemplateBand): audit and domain event commit in the same transaction as every change. Rows outside the
 * caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class ResultTemplateService extends AppTransactionManager {

    public static final String AGGREGATE = ResultTemplateEntity.TABLE;
    static final String PERMISSION_LIST = "assessment.result_template.read";
    static final String PERMISSION_GET = "assessment.result_template.read";
    static final String PERMISSION_CREATE = "assessment.result_template.create";
    static final String PERMISSION_UPDATE = "assessment.result_template.update";
    static final String PERMISSION_DELETE = "assessment.result_template.delete";
    /** The contract's ResultTemplateUpdate fields, in apply order. */
    static final List<String> WRITABLE = List.of("examSessionId", "gradingSchemeId", "name", "resultDate", "status");

    public static final String RESULT_TEMPLATE_BAND_AGGREGATE = ResultTemplateBandEntity.TABLE;
    static final String RESULT_TEMPLATE_BAND_PERMISSION_LIST = "assessment.result_template_band.read";
    static final String RESULT_TEMPLATE_BAND_PERMISSION_GET = "assessment.result_template_band.read";
    static final String RESULT_TEMPLATE_BAND_PERMISSION_CREATE = "assessment.result_template_band.create";
    static final String RESULT_TEMPLATE_BAND_PERMISSION_UPDATE = "assessment.result_template_band.update";
    static final String RESULT_TEMPLATE_BAND_PERMISSION_DELETE = "assessment.result_template_band.delete";
    /** The contract's ResultTemplateBandUpdate fields, in apply order. */
    static final List<String> RESULT_TEMPLATE_BAND_WRITABLE = List.of("gradingBandId");

    private final ResultTemplateRepository repository;
    private final ResultTemplateMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final ResultTemplateBandRepository resultTemplateBandRepository;
    private final ResultTemplateBandMapper resultTemplateBandMapper;

    public ResultTemplateService(ResultTemplateRepository repository,
            ResultTemplateMapper mapper,
            @Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AssessmentDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AssessmentEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(AssessmentEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            ResultTemplateBandRepository resultTemplateBandRepository,
            ResultTemplateBandMapper resultTemplateBandMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.resultTemplateBandRepository = resultTemplateBandRepository;
        this.resultTemplateBandMapper = resultTemplateBandMapper;
    }

    public ResultTemplate getResultTemplate(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<ResultTemplate> listResultTemplates(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ResultTemplateRepository.SORT_FIELDS, ResultTemplateRepository.DEFAULT_ORDER,
                ResultTemplateRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ResultTemplateRepository.FILTER_FIELDS)
                .and(ResultTemplateRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<ResultTemplateEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ResultTemplate createResultTemplate(ResultTemplate request, boolean validateOnly) {
        validator.validate(request);
        ResultTemplateEntity entity = mapper.toEntity(request);
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
            ResultTemplateEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + AssessmentEvents.CREATED, AGGREGATE, stored.getId(),
                    AssessmentEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public ResultTemplate updateResultTemplate(UUID id, String ifMatch, ResultTemplateUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            ResultTemplateEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + AssessmentEvents.UPDATED, AGGREGATE, id, AssessmentEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteResultTemplate(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            ResultTemplateEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByMarksheetResultTemplateId(id)) {
                throw new InvalidStateException(
                        "The result template is still referenced by assessment.marksheet rows; remove those first.");
            }
            if (repository.referencedByResultTemplateBandResultTemplateId(id)) {
                throw new InvalidStateException(
                        "The result template is still referenced by assessment.result_template_band rows; remove "
                                + "those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + AssessmentEvents.DELETED, AGGREGATE, id, AssessmentEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ResultTemplateEntity loadVisible(DataScope scope, UUID id) {
        ResultTemplateEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the assessment.exam_session the row belongs to. */
    private Optional<UUID> institutionOf(ResultTemplateEntity entity) {
        return repository.institutionOf(entity.getExamSessionId());
    }

    /** Applies the selected ResultTemplateUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(ResultTemplateEntity entity, ResultTemplateUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "examSessionId" -> entity.setExamSessionId(changes.getExamSessionId());
                case "gradingSchemeId" -> entity.setGradingSchemeId(changes.getGradingSchemeId());
                case "name" -> entity.setName(changes.getName());
                case "resultDate" -> entity.setResultDate(changes.getResultDate());
                case "status" -> entity.setStatus(Optional.ofNullable(changes.getStatus()).map(v -> v.getValue())
                        .orElse(null));
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(ResultTemplateEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getExamSessionId() == null) {
            errors.add(new FieldError("examSessionId", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (entity.getResultDate() == null) {
            errors.add(new FieldError("resultDate", "must not be null", "NotNull"));
        }
        if (!creating && entity.getStatus() == null) {
            errors.add(new FieldError("status", "must not be null", "NotNull"));
        }
        if (entity.getExamSessionId() != null && !repository.existsExamSessionId(entity.getExamSessionId())) {
            errors.add(new FieldError("examSessionId", "exam session does not exist", "NOT_FOUND"));
        }
        if (entity.getGradingSchemeId() != null && !repository.existsGradingSchemeId(entity.getGradingSchemeId())) {
            errors.add(new FieldError("gradingSchemeId", "grading scheme does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(ResultTemplateEntity entity) {

    }

    // ---- ResultTemplateBand ----

    public ResultTemplateBand getResultTemplateBand(UUID resultTemplateId, UUID id) {
        return doInReadTransaction(() -> {
            return resultTemplateBandMapper.toDto(loadVisibleResultTemplateBand(DataScope.current(
                    RESULT_TEMPLATE_BAND_PERMISSION_GET), resultTemplateId, id));
        });
    }

    public Page<ResultTemplateBand> listResultTemplateBands(UUID resultTemplateId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ResultTemplateBandRepository.SORT_FIELDS,
                        ResultTemplateBandRepository.DEFAULT_ORDER,
                ResultTemplateBandRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ResultTemplateBandRepository.FILTER_FIELDS)
                .and(ResultTemplateBandRepository.ofParent(resultTemplateId))
                        .and(ResultTemplateBandRepository.scope(DataScope.current(RESULT_TEMPLATE_BAND_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + resultTemplateId, clock);
        return doInReadTransaction(() -> {
            Page<ResultTemplateBandEntity> rows = resultTemplateBandRepository.list(page, where);
            return rows.map(resultTemplateBandMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ResultTemplateBand createResultTemplateBand(UUID resultTemplateId, ResultTemplateBand request,
            boolean validateOnly) {
        validator.validate(request);
        ResultTemplateBandEntity entity = resultTemplateBandMapper.toEntity(request);
        entity.setResultTemplateId(resultTemplateId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(RESULT_TEMPLATE_BAND_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!resultTemplateBandRepository.existsResultTemplateId(entity.getResultTemplateId())) {
                throw new ResourceNotFoundException("assessment.result_template", entity.getResultTemplateId());
            }
            if (!scope.canRead(institutionOfResultTemplateBand(entity))) {
                throw new ResourceNotFoundException("assessment.result_template", entity.getResultTemplateId());
            }
            assertValidResultTemplateBand(entity, true);
            scope.requireWritable(institutionOfResultTemplateBand(entity));
            assertUniqueResultTemplateBand(entity);
            if (validateOnly) {
                return resultTemplateBandMapper.toDto(entity);
            }
            ResultTemplateBandEntity stored = resultTemplateBandRepository.insert(entity);
            audit.recordCreate(RESULT_TEMPLATE_BAND_AGGREGATE, stored.getId());
            outbox.publish(RESULT_TEMPLATE_BAND_AGGREGATE + AssessmentEvents.CREATED, RESULT_TEMPLATE_BAND_AGGREGATE,
                    stored.getId(),
                    AssessmentEvents.payload(stored.getId()));
            return resultTemplateBandMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public ResultTemplateBand updateResultTemplateBand(UUID resultTemplateId, UUID id, String ifMatch,
            ResultTemplateBandUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, RESULT_TEMPLATE_BAND_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(RESULT_TEMPLATE_BAND_PERMISSION_UPDATE);
            ResultTemplateBandEntity entity = loadVisibleResultTemplateBand(scope, resultTemplateId, id);
            scope.requireWritable(institutionOfResultTemplateBand(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyResultTemplateBand(entity, changes, fields);
            validator.validate(resultTemplateBandMapper.toDto(entity));
            assertValidResultTemplateBand(entity, false);
            scope.requireWritable(institutionOfResultTemplateBand(entity));
            assertUniqueResultTemplateBand(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return resultTemplateBandMapper.toDto(entity);
            }
            if (resultTemplateBandRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(RESULT_TEMPLATE_BAND_AGGREGATE, id, fields);
            outbox.publish(RESULT_TEMPLATE_BAND_AGGREGATE + AssessmentEvents.UPDATED, RESULT_TEMPLATE_BAND_AGGREGATE, id,
                    AssessmentEvents.payload(id));
            return resultTemplateBandMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteResultTemplateBand(UUID resultTemplateId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(RESULT_TEMPLATE_BAND_PERMISSION_DELETE);
            ResultTemplateBandEntity entity = loadVisibleResultTemplateBand(scope, resultTemplateId, id);
            scope.requireWritable(institutionOfResultTemplateBand(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (resultTemplateBandRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(RESULT_TEMPLATE_BAND_AGGREGATE, id);
            outbox.publish(RESULT_TEMPLATE_BAND_AGGREGATE + AssessmentEvents.DELETED, RESULT_TEMPLATE_BAND_AGGREGATE, id,
                    AssessmentEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private ResultTemplateBandEntity loadVisibleResultTemplateBand(DataScope scope, UUID resultTemplateId, UUID id) {
        ResultTemplateBandEntity entity = resultTemplateBandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESULT_TEMPLATE_BAND_AGGREGATE, id));
        if (!scope.canRead(institutionOfResultTemplateBand(entity)) || !resultTemplateId.equals(entity.getResultTemplateId())) {
            throw new ResourceNotFoundException(RESULT_TEMPLATE_BAND_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the assessment.result_template → assessment.exam_session the row belongs to. */
    private Optional<UUID> institutionOfResultTemplateBand(ResultTemplateBandEntity entity) {
        return resultTemplateBandRepository.institutionOf(entity.getResultTemplateId());
    }

    /** Applies the selected ResultTemplateBandUpdate fields; explicit per field so a mask can clear values. */
    private static void applyResultTemplateBand(ResultTemplateBandEntity entity, ResultTemplateBandUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "gradingBandId" -> entity.setGradingBandId(changes.getGradingBandId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidResultTemplateBand(ResultTemplateBandEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getResultTemplateId() == null) {
            errors.add(new FieldError("resultTemplateId", "must not be null", "NotNull"));
        }
        if (entity.getGradingBandId() == null) {
            errors.add(new FieldError("gradingBandId", "must not be null", "NotNull"));
        }
        if (entity.getGradingBandId() != null && !resultTemplateBandRepository.existsGradingBandId(entity.getGradingBandId())) {
            errors.add(new FieldError("gradingBandId", "grading band does not exist", "NOT_FOUND"));
        }
        if (entity.getResultTemplateId() != null
                && !resultTemplateBandRepository.existsResultTemplateId(entity.getResultTemplateId())) {
            errors.add(new FieldError("resultTemplateId", "result template does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueResultTemplateBand(ResultTemplateBandEntity entity) {
        if (resultTemplateBandRepository.takenResultTemplateIdGradingBandId1(entity)) {
            throw new DuplicateResourceException(
                    "A result template band with the same result template id and grading band id already exists.");
        }
    }
}
