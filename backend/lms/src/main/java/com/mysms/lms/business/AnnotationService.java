package com.mysms.lms.business;

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
import com.mysms.lms.api.model.Annotation;
import com.mysms.lms.api.model.AnnotationShare;
import com.mysms.lms.api.model.AnnotationShareUpdate;
import com.mysms.lms.api.model.AnnotationUpdate;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.AnnotationEntity;
import com.mysms.lms.domain.AnnotationShareEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.AnnotationMapper;
import com.mysms.lms.mapper.AnnotationShareMapper;
import com.mysms.lms.repository.AnnotationRepository;
import com.mysms.lms.repository.AnnotationShareRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.annotation and of the records that exist only as part of a Annotation (AnnotationShare):
 * audit and domain event commit in the same transaction as every change. Rows outside the caller's institutions are
 * invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class AnnotationService extends AppTransactionManager {

    public static final String AGGREGATE = AnnotationEntity.TABLE;
    static final String PERMISSION_LIST = "lms.annotation.read";
    static final String PERMISSION_GET = "lms.annotation.read";
    static final String PERMISSION_CREATE = "lms.annotation.create";
    static final String PERMISSION_UPDATE = "lms.annotation.update";
    static final String PERMISSION_DELETE = "lms.annotation.delete";
    /** The contract's AnnotationUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("contentItemId", "cohortId", "ebookId", "fileId", "annotationType", "annotationSubtype",
            "pageNumber", "startCharPos", "charCount", "location", "annotatedText", "title", "description", "embedCode",
            "url", "mediaFileId", "visibility", "forumTopicId");

    public static final String ANNOTATION_SHARE_AGGREGATE = AnnotationShareEntity.TABLE;
    static final String ANNOTATION_SHARE_PERMISSION_LIST = "lms.annotation_share.read";
    static final String ANNOTATION_SHARE_PERMISSION_GET = "lms.annotation_share.read";
    static final String ANNOTATION_SHARE_PERMISSION_CREATE = "lms.annotation_share.create";
    static final String ANNOTATION_SHARE_PERMISSION_UPDATE = "lms.annotation_share.update";
    static final String ANNOTATION_SHARE_PERMISSION_DELETE = "lms.annotation_share.delete";
    /** The contract's AnnotationShareUpdate fields, in apply order. */
    static final List<String> ANNOTATION_SHARE_WRITABLE = List.of("sharedWithUserId", "sharedAt");

    private final AnnotationRepository repository;
    private final AnnotationMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final AnnotationShareRepository annotationShareRepository;
    private final AnnotationShareMapper annotationShareMapper;

    public AnnotationService(AnnotationRepository repository,
            AnnotationMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            AnnotationShareRepository annotationShareRepository,
            AnnotationShareMapper annotationShareMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.annotationShareRepository = annotationShareRepository;
        this.annotationShareMapper = annotationShareMapper;
    }

    public Annotation getAnnotation(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Annotation> listAnnotations(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, AnnotationRepository.SORT_FIELDS, AnnotationRepository.DEFAULT_ORDER,
                AnnotationRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, AnnotationRepository.FILTER_FIELDS)
                .and(AnnotationRepository.scope(DataScope.current(PERMISSION_LIST)))
                .and(AnnotationRepository.ownedBy(OwnRows.caller()));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<AnnotationEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Annotation createAnnotation(Annotation request, boolean validateOnly) {
        validator.validate(request);
        AnnotationEntity entity = mapper.toEntity(request);
        entity.setUserId(OwnRows.caller());
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
            AnnotationEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public Annotation updateAnnotation(UUID id, String ifMatch, AnnotationUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            AnnotationEntity entity = loadVisible(scope, id);
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
    public void deleteAnnotation(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            AnnotationEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByAnnotationShareAnnotationId(id)) {
                throw new InvalidStateException(
                        "The annotation is still referenced by lms.annotation_share rows; remove those first.");
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
    private AnnotationEntity loadVisible(DataScope scope, UUID id) {
        AnnotationEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        OwnRows.require(entity.getUserId(), AGGREGATE, id);
        return entity;
    }

    /** The institution of the lms.cohort the row belongs to. */
    private Optional<UUID> institutionOf(AnnotationEntity entity) {
        return repository.institutionOf(entity.getCohortId());
    }

    /** Applies the selected AnnotationUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(AnnotationEntity entity, AnnotationUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "contentItemId" -> entity.setContentItemId(changes.getContentItemId());
                case "cohortId" -> entity.setCohortId(changes.getCohortId());
                case "ebookId" -> entity.setEbookId(changes.getEbookId());
                case "fileId" -> entity.setFileId(changes.getFileId());
                case "annotationType" -> entity.setAnnotationType(Optional.ofNullable(changes.getAnnotationType())
                        .map(v -> v.getValue()).orElse(null));
                case "annotationSubtype" -> entity.setAnnotationSubtype(changes.getAnnotationSubtype());
                case "pageNumber" -> entity.setPageNumber(changes.getPageNumber());
                case "startCharPos" -> entity.setStartCharPos(changes.getStartCharPos());
                case "charCount" -> entity.setCharCount(changes.getCharCount());
                case "location" -> entity.setLocation(changes.getLocation());
                case "annotatedText" -> entity.setAnnotatedText(changes.getAnnotatedText());
                case "title" -> entity.setTitle(changes.getTitle());
                case "description" -> entity.setDescription(changes.getDescription());
                case "embedCode" -> entity.setEmbedCode(changes.getEmbedCode());
                case "url" -> entity.setUrl(changes.getUrl());
                case "mediaFileId" -> entity.setMediaFileId(changes.getMediaFileId());
                case "visibility" -> entity.setVisibility(Optional.ofNullable(changes.getVisibility())
                        .map(v -> v.getValue()).orElse(null));
                case "forumTopicId" -> entity.setForumTopicId(changes.getForumTopicId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(AnnotationEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (entity.getAnnotationType() == null) {
            errors.add(new FieldError("annotationType", "must not be null", "NotNull"));
        }
        if (!creating && entity.getVisibility() == null) {
            errors.add(new FieldError("visibility", "must not be null", "NotNull"));
        }
        if (entity.getCohortId() != null && !repository.existsCohortId(entity.getCohortId())) {
            errors.add(new FieldError("cohortId", "cohort does not exist", "NOT_FOUND"));
        }
        if (entity.getContentItemId() != null && !repository.existsContentItemId(entity.getContentItemId())) {
            errors.add(new FieldError("contentItemId", "content item does not exist", "NOT_FOUND"));
        }
        if (entity.getEbookId() != null && !repository.existsEbookId(entity.getEbookId())) {
            errors.add(new FieldError("ebookId", "ebook does not exist", "NOT_FOUND"));
        }
        if (entity.getForumTopicId() != null && !repository.existsForumTopicId(entity.getForumTopicId())) {
            errors.add(new FieldError("forumTopicId", "forum topic does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(AnnotationEntity entity) {

    }

    // ---- AnnotationShare: a record of the Annotation aggregate ----

    public AnnotationShare getAnnotationShare(UUID annotationId, UUID id) {
        return doInReadTransaction(() -> {
            return annotationShareMapper.toDto(loadVisibleAnnotationShare(DataScope.current(ANNOTATION_SHARE_PERMISSION_GET),
                    annotationId, id));
        });
    }

    public Page<AnnotationShare> listAnnotationShares(UUID annotationId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, AnnotationShareRepository.SORT_FIELDS, AnnotationShareRepository.DEFAULT_ORDER,
                AnnotationShareRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, AnnotationShareRepository.FILTER_FIELDS)
                .and(AnnotationShareRepository.ofParent(annotationId))
                        .and(AnnotationShareRepository.scope(DataScope.current(ANNOTATION_SHARE_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + annotationId, clock);
        return doInReadTransaction(() -> {
            Page<AnnotationShareEntity> rows = annotationShareRepository.list(page, where);
            return rows.map(annotationShareMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public AnnotationShare createAnnotationShare(UUID annotationId, AnnotationShare request, boolean validateOnly) {
        validator.validate(request);
        AnnotationShareEntity entity = annotationShareMapper.toEntity(request);
        entity.setAnnotationId(annotationId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(ANNOTATION_SHARE_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!annotationShareRepository.existsAnnotationId(entity.getAnnotationId())) {
                throw new ResourceNotFoundException("lms.annotation", entity.getAnnotationId());
            }
            if (!scope.canRead(institutionOfAnnotationShare(entity))) {
                throw new ResourceNotFoundException("lms.annotation", entity.getAnnotationId());
            }
            assertValidAnnotationShare(entity, true);
            scope.requireWritable(institutionOfAnnotationShare(entity));
            assertUniqueAnnotationShare(entity);
            if (validateOnly) {
                return annotationShareMapper.toDto(entity);
            }
            AnnotationShareEntity stored = annotationShareRepository.insert(entity);
            audit.recordCreate(ANNOTATION_SHARE_AGGREGATE, stored.getId());
            outbox.publish(ANNOTATION_SHARE_AGGREGATE + LmsEvents.CREATED, ANNOTATION_SHARE_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return annotationShareMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public AnnotationShare updateAnnotationShare(UUID annotationId, UUID id, String ifMatch,
            AnnotationShareUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, ANNOTATION_SHARE_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(ANNOTATION_SHARE_PERMISSION_UPDATE);
            AnnotationShareEntity entity = loadVisibleAnnotationShare(scope, annotationId, id);
            scope.requireWritable(institutionOfAnnotationShare(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyAnnotationShare(entity, changes, fields);
            validator.validate(annotationShareMapper.toDto(entity));
            assertValidAnnotationShare(entity, false);
            scope.requireWritable(institutionOfAnnotationShare(entity));
            assertUniqueAnnotationShare(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return annotationShareMapper.toDto(entity);
            }
            if (annotationShareRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(ANNOTATION_SHARE_AGGREGATE, id, fields);
            outbox.publish(ANNOTATION_SHARE_AGGREGATE + LmsEvents.UPDATED, ANNOTATION_SHARE_AGGREGATE, id, LmsEvents.payload(id));
            return annotationShareMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteAnnotationShare(UUID annotationId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(ANNOTATION_SHARE_PERMISSION_DELETE);
            AnnotationShareEntity entity = loadVisibleAnnotationShare(scope, annotationId, id);
            scope.requireWritable(institutionOfAnnotationShare(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (annotationShareRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(ANNOTATION_SHARE_AGGREGATE, id);
            outbox.publish(ANNOTATION_SHARE_AGGREGATE + LmsEvents.DELETED, ANNOTATION_SHARE_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private AnnotationShareEntity loadVisibleAnnotationShare(DataScope scope, UUID annotationId, UUID id) {
        AnnotationShareEntity entity = annotationShareRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ANNOTATION_SHARE_AGGREGATE, id));
        if (!scope.canRead(institutionOfAnnotationShare(entity)) || !annotationId.equals(entity.getAnnotationId())) {
            throw new ResourceNotFoundException(ANNOTATION_SHARE_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the lms.annotation → lms.cohort it belongs to). */
    private Optional<UUID> institutionOfAnnotationShare(AnnotationShareEntity entity) {
        return annotationShareRepository.institutionOf(entity.getAnnotationId());
    }

    /** Applies the selected AnnotationShareUpdate fields; explicit per field so a mask can clear values. */
    private static void applyAnnotationShare(AnnotationShareEntity entity, AnnotationShareUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "sharedWithUserId" -> entity.setSharedWithUserId(changes.getSharedWithUserId());
                case "sharedAt" -> entity.setSharedAt(changes.getSharedAt());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidAnnotationShare(AnnotationShareEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getAnnotationId() == null) {
            errors.add(new FieldError("annotationId", "must not be null", "NotNull"));
        }
        if (entity.getSharedWithUserId() == null) {
            errors.add(new FieldError("sharedWithUserId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSharedAt() == null) {
            errors.add(new FieldError("sharedAt", "must not be null", "NotNull"));
        }
        if (entity.getAnnotationId() != null && !annotationShareRepository.existsAnnotationId(entity.getAnnotationId())) {
            errors.add(new FieldError("annotationId", "annotation does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueAnnotationShare(AnnotationShareEntity entity) {
        if (annotationShareRepository.takenAnnotationIdSharedWithUserId1(entity)) {
            throw new DuplicateResourceException(
                    "A annotation share with the same annotation id and shared with user id already exists.");
        }
    }
}
