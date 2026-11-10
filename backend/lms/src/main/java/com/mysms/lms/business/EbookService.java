package com.mysms.lms.business;

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
import com.mysms.core.web.UpdateMask;
import com.mysms.lms.api.model.Ebook;
import com.mysms.lms.api.model.EbookUpdate;
import com.mysms.lms.api.model.RelatedMaterial;
import com.mysms.lms.api.model.RelatedMaterialUpdate;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.EbookEntity;
import com.mysms.lms.domain.RelatedMaterialEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.EbookMapper;
import com.mysms.lms.mapper.RelatedMaterialMapper;
import com.mysms.lms.repository.EbookRepository;
import com.mysms.lms.repository.RelatedMaterialRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.ebook (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a Ebook
 * are served here too — RelatedMaterial.
 */
@Service
public class EbookService extends AppTransactionManager {

    public static final String AGGREGATE = EbookEntity.TABLE;
    static final String PERMISSION_LIST = "lms.ebook.read";
    static final String PERMISSION_GET = "lms.ebook.read";
    static final String PERMISSION_CREATE = "lms.ebook.create";
    static final String PERMISSION_UPDATE = "lms.ebook.update";
    static final String PERMISSION_DELETE = "lms.ebook.delete";
    /** The contract's EbookUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("institutionId", "title", "description", "ebookType", "fileId", "sourceFileId", "conversionJobRef",
            "showRelatedMaterials", "relatedMaterialSeed", "categoryId", "isInRepository");

    public static final String RELATED_MATERIAL_AGGREGATE = RelatedMaterialEntity.TABLE;
    static final String RELATED_MATERIAL_PERMISSION_LIST = "lms.related_material.read";
    static final String RELATED_MATERIAL_PERMISSION_GET = "lms.related_material.read";
    static final String RELATED_MATERIAL_PERMISSION_CREATE = "lms.related_material.create";
    static final String RELATED_MATERIAL_PERMISSION_UPDATE = "lms.related_material.update";
    static final String RELATED_MATERIAL_PERMISSION_DELETE = "lms.related_material.delete";
    /** The contract's RelatedMaterialUpdate fields, in apply order. */
    static final List<String> RELATED_MATERIAL_WRITABLE =
            List.of("title", "url", "summary", "source", "mediaType", "contentLength", "contentLengthBand",
            "videoLengthSeconds", "videoLengthBand", "initialRank", "interactivityLevel", "interactivityBand",
            "conceptualLevel", "languageComplexity", "languageComplexityBand");

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
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public Ebook updateEbook(UUID id, String ifMatch, EbookUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            EbookEntity entity = loadVisible(scope, id);
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

    /** The institution that owns the row (its own institution column). */
    private Optional<UUID> institutionOf(EbookEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected EbookUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(EbookEntity entity, EbookUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "title" -> entity.setTitle(changes.getTitle());
                case "description" -> entity.setDescription(changes.getDescription());
                case "ebookType" -> entity.setEbookType(Optional.ofNullable(changes.getEbookType())
                        .map(v -> v.getValue()).orElse(null));
                case "fileId" -> entity.setFileId(changes.getFileId());
                case "sourceFileId" -> entity.setSourceFileId(changes.getSourceFileId());
                case "conversionJobRef" -> entity.setConversionJobRef(changes.getConversionJobRef());
                case "showRelatedMaterials" -> entity.setShowRelatedMaterials(changes.getShowRelatedMaterials());
                case "relatedMaterialSeed" -> entity.setRelatedMaterialSeed(changes.getRelatedMaterialSeed());
                case "categoryId" -> entity.setCategoryId(changes.getCategoryId());
                case "isInRepository" -> entity.setIsInRepository(changes.getIsInRepository());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
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
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public RelatedMaterial updateRelatedMaterial(UUID ebookId, UUID id, String ifMatch, RelatedMaterialUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, RELATED_MATERIAL_WRITABLE);
        List<FieldError> wire = new ArrayList<>();
        WireValues.checkInt64("contentLength", changes.getContentLength(), wire);
        WireValues.checkInt64("videoLengthSeconds", changes.getVideoLengthSeconds(), wire);
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
            applyRelatedMaterial(entity, changes, fields);
            validator.validate(relatedMaterialMapper.toDto(entity));
            assertValidRelatedMaterial(entity, false);
            scope.requireWritable(institutionOfRelatedMaterial(entity));
            assertUniqueRelatedMaterial(entity);
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

    /** The institution that owns the row (the institution of the lms.ebook it belongs to). */
    private Optional<UUID> institutionOfRelatedMaterial(RelatedMaterialEntity entity) {
        return relatedMaterialRepository.institutionOf(entity.getEbookId());
    }

    /**
     * Applies the selected RelatedMaterialUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyRelatedMaterial(RelatedMaterialEntity entity, RelatedMaterialUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "title" -> entity.setTitle(changes.getTitle());
                case "url" -> entity.setUrl(changes.getUrl());
                case "summary" -> entity.setSummary(changes.getSummary());
                case "source" -> entity.setSource(changes.getSource());
                case "mediaType" -> entity.setMediaType(changes.getMediaType());
                case "contentLength" -> entity.setContentLength(WireValues.int64(changes.getContentLength()));
                case "contentLengthBand" -> entity.setContentLengthBand(changes.getContentLengthBand());
                case "videoLengthSeconds" -> entity.setVideoLengthSeconds(
                        WireValues.int64(changes.getVideoLengthSeconds()));
                case "videoLengthBand" -> entity.setVideoLengthBand(changes.getVideoLengthBand());
                case "initialRank" -> entity.setInitialRank(changes.getInitialRank());
                case "interactivityLevel" -> entity.setInteractivityLevel(changes.getInteractivityLevel());
                case "interactivityBand" -> entity.setInteractivityBand(changes.getInteractivityBand());
                case "conceptualLevel" -> entity.setConceptualLevel(changes.getConceptualLevel());
                case "languageComplexity" -> entity.setLanguageComplexity(
                        WireValues.decimal(changes.getLanguageComplexity()));
                case "languageComplexityBand" -> entity.setLanguageComplexityBand(changes.getLanguageComplexityBand());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
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
