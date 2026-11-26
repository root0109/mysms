package com.mysms.academics.assessment.business;

import com.mysms.academics.assessment.api.model.GradingScheme;
import com.mysms.academics.assessment.api.model.GradingSchemeUpdate;
import com.mysms.academics.assessment.config.AssessmentDatabaseConfig;
import com.mysms.academics.assessment.config.AssessmentEventsConfig;
import com.mysms.academics.assessment.domain.GradingSchemeEntity;
import com.mysms.academics.assessment.events.AssessmentEvents;
import com.mysms.academics.assessment.mapper.GradingCriteriaJson;
import com.mysms.academics.assessment.mapper.GradingSchemeMapper;
import com.mysms.academics.assessment.repository.GradingSchemeRepository;
import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of the grading-scheme aggregate, with institution scope and auditable optimistic writes. A scheme
 * results were generated with (a result template that is no longer a draft grades with it) is not changed (409); the
 * update takes the scheme row FOR UPDATE first, so a generation holding it FOR SHARE finishes before the check.
 */
@Service
public class GradingSchemeService extends AppTransactionManager {

    public static final String AGGREGATE = GradingSchemeEntity.TABLE;
    static final String PERMISSION_LIST = "assessment.grading_scheme.read";
    static final String PERMISSION_GET = "assessment.grading_scheme.read";
    static final String PERMISSION_CREATE = "assessment.grading_scheme.create";
    static final String PERMISSION_UPDATE = "assessment.grading_scheme.update";
    static final String PERMISSION_DELETE = "assessment.grading_scheme.delete";

    private final GradingSchemeRepository repository;
    private final GradingSchemeMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public GradingSchemeService(GradingSchemeRepository repository, GradingSchemeMapper mapper,
            @Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AssessmentDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AssessmentEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(AssessmentEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator, Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public GradingScheme getGradingScheme(UUID id) {
        return doInReadTransaction(() -> mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id)));
    }

    public Page<GradingScheme> listGradingSchemes(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, GradingSchemeRepository.SORT_FIELDS,
                GradingSchemeRepository.DEFAULT_ORDER, GradingSchemeRepository.ID_COLUMN);
        DataScope scope = DataScope.current(PERMISSION_LIST);
        SqlFragment where = FilterParser.parse(filter, GradingSchemeRepository.FILTER_FIELDS)
                .and(GradingSchemeRepository.scope(scope));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> repository.list(page, where).map(mapper::toDto));
    }

    public GradingScheme createGradingScheme(GradingScheme request, boolean validateOnly) {
        validator.validate(request);
        GradingSchemeEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            GradingSchemeEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + AssessmentEvents.CREATED, AGGREGATE, stored.getId(),
                    AssessmentEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    public GradingScheme updateGradingScheme(UUID id, String ifMatch, GradingSchemeUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            repository.lockForUpdate(id);
            GradingSchemeEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.usedByGeneratedTemplate(id)) {
                throw new InvalidStateException(
                        "The grading scheme has generated results; cancel their marksheets before changing it.");
            }
            List<String> fields = apply(entity, changes);
            validator.validate(mapper.toDto(entity));
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

    public void deleteGradingScheme(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            GradingSchemeEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByGradingBand(id) || repository.referencedByResultTemplate(id)) {
                throw new InvalidStateException("The grading scheme is still referenced by live assessment records.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + AssessmentEvents.DELETED, AGGREGATE, id, AssessmentEvents.payload(id));
        });
    }

    private GradingSchemeEntity loadVisible(DataScope scope, UUID id) {
        GradingSchemeEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    private static Optional<UUID> institutionOf(GradingSchemeEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(GradingSchemeEntity entity, GradingSchemeUpdate changes) {
        Patches patch = new Patches();
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("academicYearId", changes.getAcademicYearId(), entity::setAcademicYearId);
        patch.apply("name", changes.getName(), entity::setName);
        patch.apply("schemeType", changes.getSchemeType(), v -> entity.setSchemeType(v == null ? null : v.getValue()));
        patch.apply("gradingMethod", changes.getGradingMethod(),
                                v -> entity.setGradingMethod(v == null ? null : v.getValue()));
        patch.apply("criteria", changes.getCriteria(), v -> entity.setCriteria(GradingCriteriaJson.write(v)));
        patch.apply("isActive", changes.getIsActive(), entity::setIsActive);
        patch.apply("isTemplate", changes.getIsTemplate(), entity::setIsTemplate);
        return patch.applied();
    }

    private void assertUnique(GradingSchemeEntity entity) {
        if (repository.takenName(entity)) {
            throw new DuplicateResourceException("A grading scheme with the same name already exists in this year.");
        }
    }
}
