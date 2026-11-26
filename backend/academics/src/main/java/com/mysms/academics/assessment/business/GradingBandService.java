package com.mysms.academics.assessment.business;

import com.mysms.academics.assessment.config.AssessmentDatabaseConfig;
import com.mysms.academics.assessment.config.AssessmentEventsConfig;
import com.mysms.academics.assessment.domain.GradingBandEntity;
import com.mysms.academics.assessment.events.AssessmentEvents;
import com.mysms.academics.assessment.mapper.GradingBandMapper;
import com.mysms.academics.assessment.repository.GradingBandRepository;
import com.mysms.academics.assessment.repository.GradingSchemeRepository;
import com.mysms.authclient.DataScope;
import com.mysms.academics.assessment.api.model.GradingBand;
import com.mysms.academics.assessment.api.model.GradingBandUpdate;
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
 * Business service of assessment.grading_band: audit and domain event commit in the same transaction as every change;
 * rows outside the caller's institutions are invisible, tenant-level rows change only through a tenant-wide grant.
 * <p>The bands are what generated results were graded with: every change takes the scheme row FOR UPDATE (a generation
 * holds it FOR SHARE while reading, so the two never interleave) and is refused (409) while a non-draft result template
 * grades with the scheme.
 */
@Service
public class GradingBandService extends AppTransactionManager {

    public static final String AGGREGATE = GradingBandEntity.TABLE;
    static final String PERMISSION_LIST = "assessment.grading_band.read";
    static final String PERMISSION_GET = "assessment.grading_band.read";
    static final String PERMISSION_CREATE = "assessment.grading_band.create";
    static final String PERMISSION_UPDATE = "assessment.grading_band.update";
    static final String PERMISSION_DELETE = "assessment.grading_band.delete";


    private final GradingBandRepository repository;
    private final GradingBandMapper mapper;
    private final GradingSchemeRepository schemes;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public GradingBandService(GradingBandRepository repository, GradingBandMapper mapper,
            GradingSchemeRepository schemes,
            @Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AssessmentDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AssessmentEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(AssessmentEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.schemes = schemes;
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
            requireSchemeChangeable(entity.getGradingSchemeId());
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
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public GradingBand updateGradingBand(UUID id, String ifMatch, GradingBandUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            GradingBandEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            requireSchemeChangeable(entity.getGradingSchemeId());
            List<String> fields = apply(entity, changes);
            validator.validate(mapper.toDto(entity));
            assertValid(entity, false);
            scope.requireWritable(institutionOf(entity));
            requireSchemeChangeable(entity.getGradingSchemeId());
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
            requireSchemeChangeable(entity.getGradingSchemeId());
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

    /** Locks the scheme against generation and refuses (409) a change of the bands of a scheme results were graded with. */
    private void requireSchemeChangeable(UUID schemeId) {
        if (schemeId != null && schemes.lockForUpdate(schemeId) && schemes.usedByGeneratedTemplate(schemeId)) {
            throw new InvalidStateException(
                    "The grading scheme has generated results; cancel their marksheets before changing its bands.");
        }
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

    /** The institution of the assessment.grading_scheme the row belongs to. */
    private Optional<UUID> institutionOf(GradingBandEntity entity) {
        return repository.institutionOf(entity.getGradingSchemeId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(GradingBandEntity entity, GradingBandUpdate changes) {
        Patches patch = new Patches();
        patch.apply("gradingSchemeId", changes.getGradingSchemeId(), entity::setGradingSchemeId);
        patch.apply("minPercent", changes.getMinPercent(), v -> entity.setMinPercent(WireValues.decimal(v)));
        patch.apply("maxPercent", changes.getMaxPercent(), v -> entity.setMaxPercent(WireValues.decimal(v)));
        patch.apply("gradeLabel", changes.getGradeLabel(), entity::setGradeLabel);
        patch.apply("gradePoint", changes.getGradePoint(), v -> entity.setGradePoint(WireValues.decimal(v)));
        patch.apply("isPass", changes.getIsPass(), entity::setIsPass);
        patch.apply("description", changes.getDescription(), entity::setDescription);
        return patch.applied();
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
