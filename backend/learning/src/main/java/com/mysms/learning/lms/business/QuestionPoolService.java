package com.mysms.learning.lms.business;

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
import com.mysms.core.web.Patches;
import com.mysms.learning.lms.api.model.QuestionPool;
import com.mysms.learning.lms.api.model.QuestionPoolItem;
import com.mysms.learning.lms.api.model.QuestionPoolItemUpdate;
import com.mysms.learning.lms.api.model.QuestionPoolUpdate;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.config.LmsEventsConfig;
import com.mysms.learning.lms.domain.QuestionPoolEntity;
import com.mysms.learning.lms.domain.QuestionPoolItemEntity;
import com.mysms.learning.lms.events.LmsEvents;
import com.mysms.learning.lms.mapper.QuestionPoolItemMapper;
import com.mysms.learning.lms.mapper.QuestionPoolMapper;
import com.mysms.learning.lms.repository.QuestionPoolItemRepository;
import com.mysms.learning.lms.repository.QuestionPoolRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.question_pool and of the records that exist only as part of a QuestionPool
 * (QuestionPoolItem): audit and domain event commit in the same transaction as every change. Rows outside the caller's
 * institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class QuestionPoolService extends AppTransactionManager {

    public static final String AGGREGATE = QuestionPoolEntity.TABLE;
    static final String PERMISSION_LIST = "lms.question_pool.read";
    static final String PERMISSION_GET = "lms.question_pool.read";
    static final String PERMISSION_CREATE = "lms.question_pool.create";
    static final String PERMISSION_UPDATE = "lms.question_pool.update";
    static final String PERMISSION_DELETE = "lms.question_pool.delete";

    public static final String QUESTION_POOL_ITEM_AGGREGATE = QuestionPoolItemEntity.TABLE;
    static final String QUESTION_POOL_ITEM_PERMISSION_LIST = "lms.question_pool_item.read";
    static final String QUESTION_POOL_ITEM_PERMISSION_GET = "lms.question_pool_item.read";
    static final String QUESTION_POOL_ITEM_PERMISSION_CREATE = "lms.question_pool_item.create";
    static final String QUESTION_POOL_ITEM_PERMISSION_UPDATE = "lms.question_pool_item.update";
    static final String QUESTION_POOL_ITEM_PERMISSION_DELETE = "lms.question_pool_item.delete";

    private final QuestionPoolRepository repository;
    private final QuestionPoolMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final QuestionPoolItemRepository questionPoolItemRepository;
    private final QuestionPoolItemMapper questionPoolItemMapper;

    public QuestionPoolService(QuestionPoolRepository repository,
            QuestionPoolMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            QuestionPoolItemRepository questionPoolItemRepository,
            QuestionPoolItemMapper questionPoolItemMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.questionPoolItemRepository = questionPoolItemRepository;
        this.questionPoolItemMapper = questionPoolItemMapper;
    }

    public QuestionPool getQuestionPool(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<QuestionPool> listQuestionPools(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, QuestionPoolRepository.SORT_FIELDS, QuestionPoolRepository.DEFAULT_ORDER,
                QuestionPoolRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, QuestionPoolRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<QuestionPoolEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public QuestionPool createQuestionPool(QuestionPool request, boolean validateOnly) {
        validator.validate(request);
        QuestionPoolEntity entity = mapper.toEntity(request);
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
            QuestionPoolEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public QuestionPool updateQuestionPool(UUID id, String ifMatch, QuestionPoolUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            QuestionPoolEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = apply(entity, changes);
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
    public void deleteQuestionPool(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            QuestionPoolEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByQuestionPoolItemQuestionPoolId(id)) {
                throw new InvalidStateException(
                        "The question pool is still referenced by lms.question_pool_item rows; remove those first.");
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
    private QuestionPoolEntity loadVisible(DataScope scope, UUID id) {
        QuestionPoolEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOf(QuestionPoolEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(QuestionPoolEntity entity, QuestionPoolUpdate changes) {
        Patches patch = new Patches();
        patch.apply("title", changes.getTitle(), entity::setTitle);
        patch.apply("description", changes.getDescription(), entity::setDescription);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(QuestionPoolEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(QuestionPoolEntity entity) {

    }

    // ---- QuestionPoolItem: a record of the QuestionPool aggregate ----

    public QuestionPoolItem getQuestionPoolItem(UUID questionPoolId, UUID id) {
        return doInReadTransaction(() -> {
            return questionPoolItemMapper.toDto(loadVisibleQuestionPoolItem(DataScope.current(
                    QUESTION_POOL_ITEM_PERMISSION_GET), questionPoolId, id));
        });
    }

    public Page<QuestionPoolItem> listQuestionPoolItems(UUID questionPoolId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, QuestionPoolItemRepository.SORT_FIELDS,
                        QuestionPoolItemRepository.DEFAULT_ORDER,
                QuestionPoolItemRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, QuestionPoolItemRepository.FILTER_FIELDS)
                .and(QuestionPoolItemRepository.ofParent(questionPoolId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + questionPoolId, clock);
        return doInReadTransaction(() -> {
            Page<QuestionPoolItemEntity> rows = questionPoolItemRepository.list(page, where);
            return rows.map(questionPoolItemMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public QuestionPoolItem createQuestionPoolItem(UUID questionPoolId, QuestionPoolItem request,
            boolean validateOnly) {
        validator.validate(request);
        QuestionPoolItemEntity entity = questionPoolItemMapper.toEntity(request);
        entity.setQuestionPoolId(questionPoolId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(QUESTION_POOL_ITEM_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!questionPoolItemRepository.existsQuestionPoolId(entity.getQuestionPoolId())) {
                throw new ResourceNotFoundException("lms.question_pool", entity.getQuestionPoolId());
            }
            assertValidQuestionPoolItem(entity, true);
            scope.requireWritable(institutionOfQuestionPoolItem(entity));
            assertUniqueQuestionPoolItem(entity);
            if (validateOnly) {
                return questionPoolItemMapper.toDto(entity);
            }
            QuestionPoolItemEntity stored = questionPoolItemRepository.insert(entity);
            audit.recordCreate(QUESTION_POOL_ITEM_AGGREGATE, stored.getId());
            outbox.publish(QUESTION_POOL_ITEM_AGGREGATE + LmsEvents.CREATED, QUESTION_POOL_ITEM_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return questionPoolItemMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public QuestionPoolItem updateQuestionPoolItem(UUID questionPoolId, UUID id, String ifMatch,
            QuestionPoolItemUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(QUESTION_POOL_ITEM_PERMISSION_UPDATE);
            QuestionPoolItemEntity entity = loadVisibleQuestionPoolItem(scope, questionPoolId, id);
            scope.requireWritable(institutionOfQuestionPoolItem(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyQuestionPoolItem(entity, changes);
            validator.validate(questionPoolItemMapper.toDto(entity));
            assertValidQuestionPoolItem(entity, false);
            scope.requireWritable(institutionOfQuestionPoolItem(entity));
            assertUniqueQuestionPoolItem(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return questionPoolItemMapper.toDto(entity);
            }
            if (questionPoolItemRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(QUESTION_POOL_ITEM_AGGREGATE, id, fields);
            outbox.publish(QUESTION_POOL_ITEM_AGGREGATE + LmsEvents.UPDATED, QUESTION_POOL_ITEM_AGGREGATE, id,
                    LmsEvents.payload(id));
            return questionPoolItemMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteQuestionPoolItem(UUID questionPoolId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(QUESTION_POOL_ITEM_PERMISSION_DELETE);
            QuestionPoolItemEntity entity = loadVisibleQuestionPoolItem(scope, questionPoolId, id);
            scope.requireWritable(institutionOfQuestionPoolItem(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (questionPoolItemRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(QUESTION_POOL_ITEM_AGGREGATE, id);
            outbox.publish(QUESTION_POOL_ITEM_AGGREGATE + LmsEvents.DELETED, QUESTION_POOL_ITEM_AGGREGATE, id,
                    LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private QuestionPoolItemEntity loadVisibleQuestionPoolItem(DataScope scope, UUID questionPoolId, UUID id) {
        QuestionPoolItemEntity entity = questionPoolItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(QUESTION_POOL_ITEM_AGGREGATE, id));
        if (!scope.canRead(institutionOfQuestionPoolItem(entity)) || !questionPoolId.equals(entity.getQuestionPoolId())) {
            throw new ResourceNotFoundException(QUESTION_POOL_ITEM_AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOfQuestionPoolItem(QuestionPoolItemEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyQuestionPoolItem(QuestionPoolItemEntity entity, QuestionPoolItemUpdate changes) {
        Patches patch = new Patches();
        patch.apply("questionId", changes.getQuestionId(), entity::setQuestionId);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidQuestionPoolItem(QuestionPoolItemEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getQuestionPoolId() == null) {
            errors.add(new FieldError("questionPoolId", "must not be null", "NotNull"));
        }
        if (entity.getQuestionId() == null) {
            errors.add(new FieldError("questionId", "must not be null", "NotNull"));
        }
        if (entity.getQuestionId() != null && !questionPoolItemRepository.existsQuestionId(entity.getQuestionId())) {
            errors.add(new FieldError("questionId", "question does not exist", "NOT_FOUND"));
        }
        if (entity.getQuestionPoolId() != null && !questionPoolItemRepository.existsQuestionPoolId(entity.getQuestionPoolId())) {
            errors.add(new FieldError("questionPoolId", "question pool does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueQuestionPoolItem(QuestionPoolItemEntity entity) {
        if (questionPoolItemRepository.takenQuestionPoolIdQuestionId1(entity)) {
            throw new DuplicateResourceException(
                    "A question pool item with the same question pool id and question id already exists.");
        }
    }
}
