package com.mysms.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.lms.api.model.Category;
import com.mysms.lms.api.model.CategoryUpdate;
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
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.UpdateMask;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.CategoryEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.CategoryMapper;
import com.mysms.lms.repository.CategoryRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.category: audit and domain event commit in the same transaction as every change. Rows outside
 * the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class CategoryService extends AppTransactionManager {

    public static final String AGGREGATE = CategoryEntity.TABLE;
    static final String PERMISSION_READ = "lms.category.read";
    static final String PERMISSION_CREATE = "lms.category.create";
    static final String PERMISSION_UPDATE = "lms.category.update";
    static final String PERMISSION_DELETE = "lms.category.delete";
    /** The contract's CategoryUpdate fields, in apply order. */
    static final List<String> WRITABLE = List.of("code", "sortOrder", "isActive", "name");


    private final CategoryRepository repository;
    private final CategoryMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final PublicReads publicReads;

    public CategoryService(CategoryRepository repository, CategoryMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox, @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock, PublicReads publicReads) {

        super(database, transactions);
        this.publicReads = publicReads;
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Category createCategory(Category request, boolean validateOnly) {
        validator.validate(request);
        CategoryEntity entity = mapper.toEntity(request);
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
            CategoryEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public Category updateCategory(UUID id, String ifMatch, CategoryUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            CategoryEntity entity = loadVisible(scope, id);
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

    /** A category: every category to a member holding lms.category.read, active ones only to the public. */
    public Category getCategory(UUID id) {
        boolean member = publicReads.asMember(PERMISSION_READ);
        return doInReadTransaction(() -> repository.findById(id)
                .filter(e -> member || Boolean.TRUE.equals(e.getIsActive()))
                .map(e -> member ? mapper.toDto(e) : guestProjection(mapper.toDto(e)))
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
    }

    /** The tenant's categories, page by page; active ones only to the public. */
    public Page<Category> listCategories(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        boolean member = publicReads.asMember(PERMISSION_READ);
        SortSpec sort = SortSpec.parse(sortBy, CategoryRepository.SORT_FIELDS, CategoryRepository.DEFAULT_ORDER,
                CategoryRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CategoryRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        if (member) {
            return doInReadTransaction(() -> repository.list(page, where).map(mapper::toDto));
        }
        return doInReadTransaction(() -> repository.list(page, where.and(CategoryRepository.ACTIVE))
                .map(e -> guestProjection(mapper.toDto(e))));
    }

    /** The public's view of a category: who created and changed it is internal. */
    static Category guestProjection(Category category) {
        category.setCreatedBy(null);
        category.setUpdatedBy(null);
        return category;
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCategory(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            CategoryEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByCohortCategoryId(id)) {
                throw new InvalidStateException(
                        "The category is still referenced by lms.cohort rows; remove those first.");
            }
            if (repository.referencedByCourseCategoryId(id)) {
                throw new InvalidStateException(
                        "The category is still referenced by lms.course rows; remove those first.");
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
    private CategoryEntity loadVisible(DataScope scope, UUID id) {
        CategoryEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOf(CategoryEntity entity) {
        return Optional.empty();
    }

    /** Applies the selected CategoryUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(CategoryEntity entity, CategoryUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "code" -> entity.setCode(changes.getCode());
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                case "isActive" -> entity.setIsActive(changes.getIsActive());
                case "name" -> entity.setName(changes.getName());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(CategoryEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCode() == null) {
            errors.add(new FieldError("code", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        if (entity.getName() == null) {
            errors.add(new FieldError("name", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(CategoryEntity entity) {
        if (repository.takenName1(entity)) {
            throw new DuplicateResourceException("A category with the same name already exists.");
        }
        if (repository.takenCodeCode(entity)) {
            throw new DuplicateResourceException("A category with the same code already exists.");
        }
    }
}
