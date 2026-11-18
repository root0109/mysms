package com.mysms.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.lms.api.model.ForumSection;
import com.mysms.lms.api.model.ForumSectionUpdate;
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
import com.mysms.core.web.UpdateMask;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.ForumSectionEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.ForumSectionMapper;
import com.mysms.lms.repository.ForumSectionRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.forum_section: audit and domain event commit in the same transaction as every change. Rows
 * outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class ForumSectionService extends AppTransactionManager {

    public static final String AGGREGATE = ForumSectionEntity.TABLE;
    static final String PERMISSION_LIST = "lms.forum_section.read";
    static final String PERMISSION_GET = "lms.forum_section.read";
    static final String PERMISSION_CREATE = "lms.forum_section.create";
    static final String PERMISSION_UPDATE = "lms.forum_section.update";
    static final String PERMISSION_DELETE = "lms.forum_section.delete";
    /** The contract's ForumSectionUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("forumId", "title", "description", "sectionType", "cohortGroupId", "sortOrder");


    private final ForumSectionRepository repository;
    private final ForumSectionMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public ForumSectionService(ForumSectionRepository repository, ForumSectionMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox, @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public ForumSection getForumSection(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<ForumSection> listForumSections(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ForumSectionRepository.SORT_FIELDS, ForumSectionRepository.DEFAULT_ORDER,
                ForumSectionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ForumSectionRepository.FILTER_FIELDS)
                .and(ForumSectionRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<ForumSectionEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ForumSection createForumSection(ForumSection request, boolean validateOnly) {
        validator.validate(request);
        ForumSectionEntity entity = mapper.toEntity(request);
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
            ForumSectionEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public ForumSection updateForumSection(UUID id, String ifMatch, ForumSectionUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            ForumSectionEntity entity = loadVisible(scope, id);
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
    public void deleteForumSection(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            ForumSectionEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByForumTopicForumSectionId(id)) {
                throw new InvalidStateException(
                        "The forum section is still referenced by lms.forum_topic rows; remove those first.");
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
    private ForumSectionEntity loadVisible(DataScope scope, UUID id) {
        ForumSectionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the lms.forum → lms.cohort it belongs to). */
    private Optional<UUID> institutionOf(ForumSectionEntity entity) {
        return repository.institutionOf(entity.getForumId());
    }

    /** Applies the selected ForumSectionUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(ForumSectionEntity entity, ForumSectionUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "forumId" -> entity.setForumId(changes.getForumId());
                case "title" -> entity.setTitle(changes.getTitle());
                case "description" -> entity.setDescription(changes.getDescription());
                case "sectionType" -> entity.setSectionType(Optional.ofNullable(changes.getSectionType())
                        .map(v -> v.getValue()).orElse(null));
                case "cohortGroupId" -> entity.setCohortGroupId(changes.getCohortGroupId());
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(ForumSectionEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getForumId() == null) {
            errors.add(new FieldError("forumId", "must not be null", "NotNull"));
        }
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSectionType() == null) {
            errors.add(new FieldError("sectionType", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getCohortGroupId() != null && !repository.existsCohortGroupId(entity.getCohortGroupId())) {
            errors.add(new FieldError("cohortGroupId", "cohort group does not exist", "NOT_FOUND"));
        }
        if (entity.getForumId() != null && !repository.existsForumId(entity.getForumId())) {
            errors.add(new FieldError("forumId", "forum does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(ForumSectionEntity entity) {

    }
}
