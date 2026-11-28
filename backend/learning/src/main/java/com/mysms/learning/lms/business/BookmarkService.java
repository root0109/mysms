package com.mysms.learning.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.learning.lms.api.model.Bookmark;
import com.mysms.learning.lms.api.model.BookmarkUpdate;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
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
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.config.LmsEventsConfig;
import com.mysms.learning.lms.domain.BookmarkEntity;
import com.mysms.learning.lms.events.LmsEvents;
import com.mysms.learning.lms.mapper.BookmarkMapper;
import com.mysms.learning.lms.repository.BookmarkRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.bookmark: audit and domain event commit in the same transaction as every change. Rows outside
 * the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class BookmarkService extends AppTransactionManager {

    public static final String AGGREGATE = BookmarkEntity.TABLE;
    static final String PERMISSION_LIST = "lms.bookmark.read";
    static final String PERMISSION_GET = "lms.bookmark.read";
    static final String PERMISSION_CREATE = "lms.bookmark.create";
    static final String PERMISSION_UPDATE = "lms.bookmark.update";
    static final String PERMISSION_DELETE = "lms.bookmark.delete";


    private final BookmarkRepository repository;
    private final BookmarkMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public BookmarkService(BookmarkRepository repository, BookmarkMapper mapper,
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

    public Bookmark getBookmark(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Bookmark> listBookmarks(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, BookmarkRepository.SORT_FIELDS, BookmarkRepository.DEFAULT_ORDER,
                BookmarkRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, BookmarkRepository.FILTER_FIELDS)
                .and(BookmarkRepository.scope(DataScope.current(PERMISSION_LIST)))
                .and(BookmarkRepository.ownedBy(OwnRows.caller()));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<BookmarkEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Bookmark createBookmark(Bookmark request, boolean validateOnly) {
        validator.validate(request);
        BookmarkEntity entity = mapper.toEntity(request);
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
            BookmarkEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public Bookmark updateBookmark(UUID id, String ifMatch, BookmarkUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            BookmarkEntity entity = loadVisible(scope, id);
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
    public void deleteBookmark(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            BookmarkEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
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
    private BookmarkEntity loadVisible(DataScope scope, UUID id) {
        BookmarkEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        OwnRows.require(entity.getUserId(), AGGREGATE, id);
        return entity;
    }

    /** The institution of the lms.cohort the row belongs to. */
    private Optional<UUID> institutionOf(BookmarkEntity entity) {
        return repository.institutionOf(entity.getCohortId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(BookmarkEntity entity, BookmarkUpdate changes) {
        Patches patch = new Patches();
        patch.apply("title", changes.getTitle(), entity::setTitle);
        patch.apply("url", changes.getUrl(), entity::setUrl);
        patch.apply("moduleCode", changes.getModuleCode(), entity::setModuleCode);
        patch.apply("cohortId", changes.getCohortId(), entity::setCohortId);
        patch.apply("savedAt", changes.getSavedAt(), entity::setSavedAt);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(BookmarkEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        if (entity.getUrl() == null) {
            errors.add(new FieldError("url", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSavedAt() == null) {
            errors.add(new FieldError("savedAt", "must not be null", "NotNull"));
        }
        if (entity.getCohortId() != null && !repository.existsCohortId(entity.getCohortId())) {
            errors.add(new FieldError("cohortId", "cohort does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(BookmarkEntity entity) {

    }
}
