package com.mysms.platform.business;

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
import com.mysms.platform.api.model.Tag;
import com.mysms.platform.api.model.TagUpdate;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.config.PlatformEventsConfig;
import com.mysms.platform.domain.TagEntity;
import com.mysms.platform.events.PlatformEvents;
import com.mysms.platform.mapper.TagMapper;
import com.mysms.platform.repository.TagRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of platform.tag: the tenant's labels for posts, library media, courses. Tags are tenant-level rows:
 * every holder of the read permission sees them, and only a tenant-wide grant changes them. A slug (and a code, when
 * given) is unique within its tag type among live tags (409); a tag still applied by live assignments cannot be deleted
 * (409).
 */
@Service
public class TagService extends AppTransactionManager {

    public static final String AGGREGATE = TagEntity.TABLE;
    static final String PERMISSION_READ = "platform.tag.read";
    static final String PERMISSION_CREATE = "platform.tag.create";
    static final String PERMISSION_UPDATE = "platform.tag.update";
    static final String PERMISSION_DELETE = "platform.tag.delete";
    /** The contract's TagUpdate fields that have a column, in apply order ({@code translations} has none). */
    static final List<String> WRITABLE = List.of("tagType", "name", "slug", "sortOrder", "code");
    /** A URL-safe key: lower-case letters and digits in words joined by single hyphens. */
    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    private final TagRepository repository;
    private final TagMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public TagService(TagRepository repository, TagMapper mapper,
            @Qualifier(PlatformDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PlatformDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PlatformEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(PlatformEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public Tag getTag(UUID id) {
        return doInReadTransaction(() -> mapper.toDto(loadVisible(DataScope.current(PERMISSION_READ), id)));
    }

    public Page<Tag> listTags(Integer pageNumber, Integer pageSize, String cursor, String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, TagRepository.SORT_FIELDS, TagRepository.DEFAULT_ORDER,
                TagRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, TagRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> repository.list(page, where).map(mapper::toDto));
    }

    /** Creates the tag; with validateOnly every check runs and nothing is written. */
    public Tag createTag(Tag request, boolean validateOnly) {
        validator.validate(request);
        refuseTranslations(request.getTranslations());
        TagEntity entity = mapper.toEntity(request);
        entity.setTagType(Optional.ofNullable(entity.getTagType()).orElse("general"));
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            assertValid(entity);
            scope.requireWritable(Optional.empty());
            assertUnique(entity);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            TagEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + PlatformEvents.CREATED, AGGREGATE, stored.getId(),
                    PlatformEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole.
     */
    public Tag updateTag(UUID id, String ifMatch, TagUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        refuseTranslations(changes.getTranslations());
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            TagEntity entity = loadVisible(scope, id);
            scope.requireWritable(Optional.empty());
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            apply(entity, changes, fields);
            validator.validate(mapper.toDto(entity));
            assertValid(entity);
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
            outbox.publish(AGGREGATE + PlatformEvents.UPDATED, AGGREGATE, id, PlatformEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live assignments still apply the tag. */
    public void deleteTag(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            TagEntity entity = loadVisible(scope, id);
            scope.requireWritable(Optional.empty());
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.assigned(id)) {
                throw new InvalidStateException(
                        "The tag is still applied by platform.tag_assignment rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + PlatformEvents.DELETED, AGGREGATE, id, PlatformEvents.payload(id));
        });
    }

    /** The live tag of the caller's tenant (tags are tenant-level, so the permission alone decides), else 404. */
    private TagEntity loadVisible(DataScope scope, UUID id) {
        TagEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(Optional.empty())) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** Applies the selected TagUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(TagEntity entity, TagUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "tagType" -> entity.setTagType(changes.getTagType());
                case "name" -> entity.setName(changes.getName());
                case "slug" -> entity.setSlug(changes.getSlug());
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                case "code" -> entity.setCode(changes.getCode());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Localised names have no storage yet (platform.tag has no column for them): a value is refused, never lost. */
    private static void refuseTranslations(Object translations) {
        if (translations != null) {
            throw new ValidationFailedException(List.of(new FieldError("translations",
                    "translations are not stored; send the name in the tenant's language", "UNSUPPORTED")));
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private static void assertValid(TagEntity entity) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getTagType() == null) {
            errors.add(new FieldError("tagType", "must not be null", "NotNull"));
        }
        if (entity.getSlug() != null && !SLUG.matcher(entity.getSlug()).matches()) {
            errors.add(new FieldError("slug", "lower-case letters and digits joined by single hyphens", "Pattern"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    /** uq_tag_1 and uq_tag_code among live rows: 409 before the database is asked. */
    private void assertUnique(TagEntity entity) {
        if (repository.slugTaken(entity)) {
            throw new DuplicateResourceException("A tag of this type already uses this slug.");
        }
        if (entity.getCode() != null && repository.codeTaken(entity)) {
            throw new DuplicateResourceException("A tag of this type already uses this code.");
        }
    }
}
