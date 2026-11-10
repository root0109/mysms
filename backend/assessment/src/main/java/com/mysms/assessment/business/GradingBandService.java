package com.mysms.assessment.business;

import com.mysms.assessment.config.AssessmentDatabaseConfig;
import com.mysms.assessment.config.AssessmentEventsConfig;
import com.mysms.assessment.domain.GradingBandEntity;
import com.mysms.assessment.events.AssessmentEvents;
import com.mysms.assessment.mapper.GradingBandMapper;
import com.mysms.assessment.repository.GradingBandRepository;
import com.mysms.authclient.DataScope;
import com.mysms.assessment.api.model.GradingBand;
import com.mysms.assessment.api.model.GradingBandUpdate;
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
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of assessment.grading_band (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 */
@Service
public class GradingBandService extends AppTransactionManager {

    public static final String AGGREGATE = GradingBandEntity.TABLE;
    static final String PERMISSION_LIST = "assessment.grading_band.read";
    static final String PERMISSION_GET = "assessment.grading_band.read";
    static final String PERMISSION_CREATE = "assessment.grading_band.create";
    static final String PERMISSION_UPDATE = "assessment.grading_band.update";
    static final String PERMISSION_DELETE = "assessment.grading_band.delete";
    /** The contract's GradingBandUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("gradingSchemeId", "minPercent", "maxPercent", "gradeLabel", "gradePoint", "isPass", "description");


    private final GradingBandRepository repository;
    private final GradingBandMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public GradingBandService(GradingBandRepository repository, GradingBandMapper mapper,
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

    public GradingBand getGradingBand(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<GradingBand> listGradingBands(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, GradingBandRepository.SORT_FIELDS, GradingBandRepository.DEFAULT_ORDER,
                GradingBandRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, GradingBandRepository.FILTER_FIELDS)
                .and(GradingBandRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<GradingBandEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public GradingBand createGradingBand(GradingBand request, boolean validateOnly) {
        validator.validate(request);
        GradingBandEntity entity = mapper.toEntity(request);
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
            GradingBandEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + AssessmentEvents.CREATED, AGGREGATE, stored.getId(),
                    AssessmentEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public GradingBand updateGradingBand(UUID id, String ifMatch, GradingBandUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            GradingBandEntity entity = loadVisible(scope, id);
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
    public void deleteGradingBand(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            GradingBandEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByResultTemplateBandGradingBandId(id)) {
                throw new InvalidStateException(
                        "The grading band is still referenced by assessment.result_template_band rows; remove those "
                                + "first.");
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
    private GradingBandEntity loadVisible(DataScope scope, UUID id) {
        GradingBandEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the assessment.grading_scheme it belongs to). */
    private Optional<UUID> institutionOf(GradingBandEntity entity) {
        return repository.institutionOf(entity.getGradingSchemeId());
    }

    /** Applies the selected GradingBandUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(GradingBandEntity entity, GradingBandUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "gradingSchemeId" -> entity.setGradingSchemeId(changes.getGradingSchemeId());
                case "minPercent" -> entity.setMinPercent(WireValues.decimal(changes.getMinPercent()));
                case "maxPercent" -> entity.setMaxPercent(WireValues.decimal(changes.getMaxPercent()));
                case "gradeLabel" -> entity.setGradeLabel(changes.getGradeLabel());
                case "gradePoint" -> entity.setGradePoint(WireValues.decimal(changes.getGradePoint()));
                case "isPass" -> entity.setIsPass(changes.getIsPass());
                case "description" -> entity.setDescription(changes.getDescription());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(GradingBandEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getGradingSchemeId() == null) {
            errors.add(new FieldError("gradingSchemeId", "must not be null", "NotNull"));
        }
        if (entity.getMinPercent() == null) {
            errors.add(new FieldError("minPercent", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("minPercent", WireValues.decimal(entity.getMinPercent()), 5, 2, errors);
        if (entity.getMaxPercent() == null) {
            errors.add(new FieldError("maxPercent", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("maxPercent", WireValues.decimal(entity.getMaxPercent()), 5, 2, errors);
        if (entity.getGradeLabel() == null) {
            errors.add(new FieldError("gradeLabel", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("gradePoint", WireValues.decimal(entity.getGradePoint()), 4, 2, errors);
        if (!creating && entity.getIsPass() == null) {
            errors.add(new FieldError("isPass", "must not be null", "NotNull"));
        }
        if (entity.getGradingSchemeId() != null && !repository.existsGradingSchemeId(entity.getGradingSchemeId())) {
            errors.add(new FieldError("gradingSchemeId", "grading scheme does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(GradingBandEntity entity) {
        if (repository.takenGradingSchemeIdGradeLabel1(entity)) {
            throw new DuplicateResourceException(
                    "A grading band with the same grading scheme id and grade label already exists.");
        }
    }
}
