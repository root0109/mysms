package com.mysms.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.lms.api.model.ForumPostVote;
import com.mysms.lms.api.model.ForumPostVoteUpdate;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
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
import com.mysms.core.web.UpdateMask;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.ForumPostVoteEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.ForumPostVoteMapper;
import com.mysms.lms.repository.ForumPostVoteRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.forum_post_vote: audit and domain event commit in the same transaction as every change. Rows
 * outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class ForumPostVoteService extends AppTransactionManager {

    public static final String AGGREGATE = ForumPostVoteEntity.TABLE;
    static final String PERMISSION_LIST = "lms.forum_post_vote.read";
    static final String PERMISSION_CREATE = "lms.forum_post_vote.create";
    static final String PERMISSION_UPDATE = "lms.forum_post_vote.update";
    static final String PERMISSION_DELETE = "lms.forum_post_vote.delete";
    /** The contract's ForumPostVoteUpdate fields, in apply order. */
    static final List<String> WRITABLE = List.of("forumPostId", "isLike");


    private final ForumPostVoteRepository repository;
    private final ForumPostVoteMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public ForumPostVoteService(ForumPostVoteRepository repository, ForumPostVoteMapper mapper,
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

    public Page<ForumPostVote> listForumPostVotes(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, ForumPostVoteRepository.SORT_FIELDS, ForumPostVoteRepository.DEFAULT_ORDER,
                ForumPostVoteRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ForumPostVoteRepository.FILTER_FIELDS)
                .and(ForumPostVoteRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<ForumPostVoteEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public ForumPostVote createForumPostVote(ForumPostVote request, boolean validateOnly) {
        validator.validate(request);
        ForumPostVoteEntity entity = mapper.toEntity(request);
        entity.setUserId(OwnRows.caller());
        entity.setVotedAt(null);
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
            ForumPostVoteEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public ForumPostVote updateForumPostVote(UUID id, String ifMatch, ForumPostVoteUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            ForumPostVoteEntity entity = loadVisible(scope, id);
            OwnRows.require(entity.getUserId(), AGGREGATE, id);
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
    public void deleteForumPostVote(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            ForumPostVoteEntity entity = loadVisible(scope, id);
            OwnRows.require(entity.getUserId(), AGGREGATE, id);
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
    private ForumPostVoteEntity loadVisible(DataScope scope, UUID id) {
        ForumPostVoteEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /**
     * The institution that owns the row (the institution of the lms.forum_post → lms.forum_topic →
     * lms.discussion_prompt it belongs to).
     */
    private Optional<UUID> institutionOf(ForumPostVoteEntity entity) {
        return repository.institutionOf(entity.getForumPostId());
    }

    /** Applies the selected ForumPostVoteUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(ForumPostVoteEntity entity, ForumPostVoteUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "forumPostId" -> entity.setForumPostId(changes.getForumPostId());
                case "isLike" -> entity.setIsLike(changes.getIsLike());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(ForumPostVoteEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getForumPostId() == null) {
            errors.add(new FieldError("forumPostId", "must not be null", "NotNull"));
        }
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (entity.getIsLike() == null) {
            errors.add(new FieldError("isLike", "must not be null", "NotNull"));
        }
        if (!creating && entity.getVotedAt() == null) {
            errors.add(new FieldError("votedAt", "must not be null", "NotNull"));
        }
        if (entity.getForumPostId() != null && !repository.existsForumPostId(entity.getForumPostId())) {
            errors.add(new FieldError("forumPostId", "forum post does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(ForumPostVoteEntity entity) {
        if (repository.takenForumPostIdUserId1(entity)) {
            throw new DuplicateResourceException(
                    "A forum post vote with the same forum post id and user id already exists.");
        }
    }
}
