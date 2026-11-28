package com.mysms.learning.lms.business;

import com.mysms.authclient.DataScope;
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
import com.mysms.learning.lms.api.model.Ebook;
import com.mysms.learning.lms.api.model.EbookUpdate;
import com.mysms.learning.lms.api.model.RelatedMaterial;
import com.mysms.learning.lms.api.model.RelatedMaterialUpdate;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.config.LmsEventsConfig;
import com.mysms.learning.lms.domain.EbookEntity;
import com.mysms.learning.lms.domain.RelatedMaterialEntity;
import com.mysms.learning.lms.events.LmsEvents;
import com.mysms.learning.lms.mapper.EbookMapper;
import com.mysms.learning.lms.mapper.RelatedMaterialMapper;
import com.mysms.learning.lms.repository.EbookRepository;
import com.mysms.learning.lms.repository.RelatedMaterialRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.ebook and of the records that exist only as part of a Ebook (RelatedMaterial): audit and
 * domain event commit in the same transaction as every change. Rows outside the caller's institutions are invisible;
 * tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class EbookService extends AppTransactionManager {

    public static final String AGGREGATE = EbookEntity.TABLE;
    static final String PERMISSION_LIST = "lms.ebook.read";
    static final String PERMISSION_GET = "lms.ebook.read";
    static final String PERMISSION_CREATE = "lms.ebook.create";
    static final String PERMISSION_UPDATE = "lms.ebook.update";
    static final String PERMISSION_DELETE = "lms.ebook.delete";

    public static final String RELATED_MATERIAL_AGGREGATE = RelatedMaterialEntity.TABLE;
    static final String RELATED_MATERIAL_PERMISSION_LIST = "lms.related_material.read";
    static final String RELATED_MATERIAL_PERMISSION_GET = "lms.related_material.read";
    static final String RELATED_MATERIAL_PERMISSION_CREATE = "lms.related_material.create";
    static final String RELATED_MATERIAL_PERMISSION_UPDATE = "lms.related_material.update";
    static final String RELATED_MATERIAL_PERMISSION_DELETE = "lms.related_material.delete";

    private final EbookRepository repository;
    private final EbookMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final RelatedMaterialRepository relatedMaterialRepository;
    private final RelatedMaterialMapper relatedMaterialMapper;

    public EbookService(EbookRepository repository,
            EbookMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            RelatedMaterialRepository relatedMaterialRepository,
            RelatedMaterialMapper relatedMaterialMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.relatedMaterialRepository = relatedMaterialRepository;
        this.relatedMaterialMapper = relatedMaterialMapper;
    }

    public Ebook getEbook(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Ebook> listEbooks(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, EbookRepository.SORT_FIELDS, EbookRepository.DEFAULT_ORDER,
                EbookRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, EbookRepository.FILTER_FIELDS)
                .and(EbookRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<EbookEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Ebook createEbook(Ebook request, boolean validateOnly) {
        validator.validate(request);
        EbookEntity entity = mapper.toEntity(request);
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
            EbookEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public Ebook updateEbook(UUID id, String ifMatch, EbookUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            EbookEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = apply(entity, changes);
            validator.validate(mapper.toDto(entity));
            assertValid(entity, false);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (fields.isEmpty()) {
                return mapper.toDto(entity);
            }
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            if (repository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(AGGREGATE, id, fields);
            outbox.publish(AGGREGATE + LmsEvents.UPDATED, AGGREGATE, id, LmsEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteEbook(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            EbookEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByAnnotationEbookId(id)) {
                throw new InvalidStateException(
                        "The ebook is still referenced by lms.annotation rows; remove those first.");
            }
            if (repository.referencedByContentItemEbookId(id)) {
                throw new InvalidStateException(
                        "The ebook is still referenced by lms.content_item rows; remove those first.");
            }
            if (repository.referencedByRelatedMaterialEbookId(id)) {
                throw new InvalidStateException(
                        "The ebook is still referenced by lms.related_material rows; remove those first.");
            }
            if (repository.referencedByRelatedMaterialRankEbookId(id)) {
                throw new InvalidStateException(
                        "The ebook is still referenced by lms.related_material_rank rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + LmsEvents.DELETED, AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private EbookEntity loadVisible(DataScope scope, UUID id) {
        EbookEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(EbookEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(EbookEntity entity, EbookUpdate changes) {
        Patches patch = new Patches();
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("title", changes.getTitle(), entity::setTitle);
        patch.apply("description", changes.getDescription(), entity::setDescription);
        patch.apply("ebookType", changes.getEbookType(), v -> entity.setEbookType(v == null ? null : v.getValue()));
        patch.apply("fileId", changes.getFileId(), entity::setFileId);
        patch.apply("sourceFileId", changes.getSourceFileId(), entity::setSourceFileId);
        patch.apply("conversionJobRef", changes.getConversionJobRef(), entity::setConversionJobRef);
        patch.apply("showRelatedMaterials", changes.getShowRelatedMaterials(), entity::setShowRelatedMaterials);
        patch.apply("relatedMaterialSeed", changes.getRelatedMaterialSeed(), entity::setRelatedMaterialSeed);
        patch.apply("categoryId", changes.getCategoryId(), entity::setCategoryId);
        patch.apply("isInRepository", changes.getIsInRepository(), entity::setIsInRepository);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(EbookEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        if (!creating && entity.getEbookType() == null) {
            errors.add(new FieldError("ebookType", "must not be null", "NotNull"));
        }
        if (!creating && entity.getShowRelatedMaterials() == null) {
            errors.add(new FieldError("showRelatedMaterials", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsInRepository() == null) {
            errors.add(new FieldError("isInRepository", "must not be null", "NotNull"));
        }
        if (entity.getCategoryId() != null && !repository.existsCategoryId(entity.getCategoryId())) {
            errors.add(new FieldError("categoryId", "resource category does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(EbookEntity entity) {

    }

    // ---- RelatedMaterial: a record of the Ebook aggregate ----

    public RelatedMaterial getRelatedMaterial(UUID ebookId, UUID id) {
        return doInReadTransaction(() -> {
            return relatedMaterialMapper.toDto(loadVisibleRelatedMaterial(DataScope.current(RELATED_MATERIAL_PERMISSION_GET),
                    ebookId, id));
        });
    }

    public Page<RelatedMaterial> listRelatedMaterials(UUID ebookId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, RelatedMaterialRepository.SORT_FIELDS, RelatedMaterialRepository.DEFAULT_ORDER,
                RelatedMaterialRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, RelatedMaterialRepository.FILTER_FIELDS)
                .and(RelatedMaterialRepository.ofParent(ebookId))
                        .and(RelatedMaterialRepository.scope(DataScope.current(RELATED_MATERIAL_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + ebookId, clock);
        return doInReadTransaction(() -> {
            Page<RelatedMaterialEntity> rows = relatedMaterialRepository.list(page, where);
            return rows.map(relatedMaterialMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public RelatedMaterial createRelatedMaterial(UUID ebookId, RelatedMaterial request, boolean validateOnly) {
        validator.validate(request);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkInt64("contentLength", request.getContentLength(), wire);
        WireValues.checkInt64("videoLengthSeconds", request.getVideoLengthSeconds(), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        RelatedMaterialEntity entity = relatedMaterialMapper.toEntity(request);
        entity.setEbookId(ebookId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(RELATED_MATERIAL_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!relatedMaterialRepository.existsEbookId(entity.getEbookId())) {
                throw new ResourceNotFoundException("lms.ebook", entity.getEbookId());
            }
            if (!scope.canRead(institutionOfRelatedMaterial(entity))) {
                throw new ResourceNotFoundException("lms.ebook", entity.getEbookId());
            }
            assertValidRelatedMaterial(entity, true);
            scope.requireWritable(institutionOfRelatedMaterial(entity));
            assertUniqueRelatedMaterial(entity);
            if (validateOnly) {
                return relatedMaterialMapper.toDto(entity);
            }
            RelatedMaterialEntity stored = relatedMaterialRepository.insert(entity);
            audit.recordCreate(RELATED_MATERIAL_AGGREGATE, stored.getId());
            outbox.publish(RELATED_MATERIAL_AGGREGATE + LmsEvents.CREATED, RELATED_MATERIAL_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return relatedMaterialMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public RelatedMaterial updateRelatedMaterial(UUID ebookId, UUID id, String ifMatch, RelatedMaterialUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkInt64("contentLength", Patches.value(changes.getContentLength()), wire);
        WireValues.checkInt64("videoLengthSeconds", Patches.value(changes.getVideoLengthSeconds()), wire);
        if (!wire.isEmpty()) {
            throw new ValidationFailedException(wire);
        }
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(RELATED_MATERIAL_PERMISSION_UPDATE);
            RelatedMaterialEntity entity = loadVisibleRelatedMaterial(scope, ebookId, id);
            scope.requireWritable(institutionOfRelatedMaterial(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyRelatedMaterial(entity, changes);
            validator.validate(relatedMaterialMapper.toDto(entity));
            assertValidRelatedMaterial(entity, false);
            scope.requireWritable(institutionOfRelatedMaterial(entity));
            assertUniqueRelatedMaterial(entity);
            if (fields.isEmpty()) {
                return relatedMaterialMapper.toDto(entity);
            }
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return relatedMaterialMapper.toDto(entity);
            }
            if (relatedMaterialRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(RELATED_MATERIAL_AGGREGATE, id, fields);
            outbox.publish(RELATED_MATERIAL_AGGREGATE + LmsEvents.UPDATED, RELATED_MATERIAL_AGGREGATE, id, LmsEvents.payload(id));
            return relatedMaterialMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteRelatedMaterial(UUID ebookId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(RELATED_MATERIAL_PERMISSION_DELETE);
            RelatedMaterialEntity entity = loadVisibleRelatedMaterial(scope, ebookId, id);
            scope.requireWritable(institutionOfRelatedMaterial(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (relatedMaterialRepository.referencedByRelatedMaterialRankItemRelatedMaterialId(id)) {
                throw new InvalidStateException(
                        "The related material is still referenced by lms.related_material_rank_item rows; remove "
                                + "those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (relatedMaterialRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(RELATED_MATERIAL_AGGREGATE, id);
            outbox.publish(RELATED_MATERIAL_AGGREGATE + LmsEvents.DELETED, RELATED_MATERIAL_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private RelatedMaterialEntity loadVisibleRelatedMaterial(DataScope scope, UUID ebookId, UUID id) {
        RelatedMaterialEntity entity = relatedMaterialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RELATED_MATERIAL_AGGREGATE, id));
        if (!scope.canRead(institutionOfRelatedMaterial(entity)) || !ebookId.equals(entity.getEbookId())) {
            throw new ResourceNotFoundException(RELATED_MATERIAL_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.ebook the row belongs to. */
    private Optional<UUID> institutionOfRelatedMaterial(RelatedMaterialEntity entity) {
        return relatedMaterialRepository.institutionOf(entity.getEbookId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyRelatedMaterial(RelatedMaterialEntity entity, RelatedMaterialUpdate changes) {
        Patches patch = new Patches();
        patch.apply("title", changes.getTitle(), entity::setTitle);
        patch.apply("url", changes.getUrl(), entity::setUrl);
        patch.apply("summary", changes.getSummary(), entity::setSummary);
        patch.apply("source", changes.getSource(), entity::setSource);
        patch.apply("mediaType", changes.getMediaType(), entity::setMediaType);
        patch.apply("contentLength", changes.getContentLength(), v -> entity.setContentLength(WireValues.int64(v)));
        patch.apply("contentLengthBand", changes.getContentLengthBand(), entity::setContentLengthBand);
        patch.apply("videoLengthSeconds", changes.getVideoLengthSeconds(),
                                                v -> entity.setVideoLengthSeconds(WireValues.int64(v)));
        patch.apply("videoLengthBand", changes.getVideoLengthBand(), entity::setVideoLengthBand);
        patch.apply("initialRank", changes.getInitialRank(), entity::setInitialRank);
        patch.apply("interactivityLevel", changes.getInteractivityLevel(), entity::setInteractivityLevel);
        patch.apply("interactivityBand", changes.getInteractivityBand(), entity::setInteractivityBand);
        patch.apply("conceptualLevel", changes.getConceptualLevel(), entity::setConceptualLevel);
        patch.apply("languageComplexity", changes.getLanguageComplexity(),
                                                v -> entity.setLanguageComplexity(WireValues.decimal(v)));
        patch.apply("languageComplexityBand", changes.getLanguageComplexityBand(), entity::setLanguageComplexityBand);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidRelatedMaterial(RelatedMaterialEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getEbookId() == null) {
            errors.add(new FieldError("ebookId", "must not be null", "NotNull"));
        }
        if (entity.getUrl() == null) {
            errors.add(new FieldError("url", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("languageComplexity", WireValues.decimal(entity.getLanguageComplexity()), 6, 2, errors);
        if (entity.getEbookId() != null && !relatedMaterialRepository.existsEbookId(entity.getEbookId())) {
            errors.add(new FieldError("ebookId", "ebook does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueRelatedMaterial(RelatedMaterialEntity entity) {

    }
}
