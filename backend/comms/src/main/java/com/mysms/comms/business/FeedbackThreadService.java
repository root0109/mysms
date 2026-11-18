package com.mysms.comms.business;

import com.mysms.authclient.DataScope;
import com.mysms.comms.api.model.CloseFeedbackThreadRequest;
import com.mysms.comms.api.model.FeedbackMessage;
import com.mysms.comms.api.model.FeedbackMessageUpdate;
import com.mysms.comms.api.model.FeedbackThread;
import com.mysms.comms.api.model.FeedbackThreadUpdate;
import com.mysms.comms.api.model.ReopenFeedbackThreadRequest;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.config.CommsEventsConfig;
import com.mysms.comms.domain.FeedbackMessageEntity;
import com.mysms.comms.domain.FeedbackThreadEntity;
import com.mysms.comms.events.CommsEvents;
import com.mysms.comms.mapper.FeedbackMessageMapper;
import com.mysms.comms.mapper.FeedbackThreadMapper;
import com.mysms.comms.repository.FeedbackMessageRepository;
import com.mysms.comms.repository.FeedbackThreadRepository;
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
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of comms.feedback_thread and of the records that exist only as part of a FeedbackThread
 * (FeedbackMessage): audit and domain event commit in the same transaction as every change. Rows outside the caller's
 * institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class FeedbackThreadService extends AppTransactionManager {

    public static final String AGGREGATE = FeedbackThreadEntity.TABLE;
    static final String PERMISSION_LIST = "comms.feedback_thread.read";
    static final String PERMISSION_GET = "comms.feedback_thread.read";
    static final String PERMISSION_CREATE = "comms.feedback_thread.create";
    static final String PERMISSION_UPDATE = "comms.feedback_thread.update";
    static final String PERMISSION_DELETE = "comms.feedback_thread.delete";
    static final String PERMISSION_CLOSE_FEEDBACK_THREAD = "comms.feedback_thread.close_feedback_thread";
    static final String PERMISSION_REOPEN_FEEDBACK_THREAD = "comms.feedback_thread.reopen_feedback_thread";
    /** The contract's FeedbackThreadUpdate fields, in apply order. */
    static final List<String> WRITABLE = List.of("institutionId", "guardianId", "studentId", "assignedToUserId");

    public static final String FEEDBACK_MESSAGE_AGGREGATE = FeedbackMessageEntity.TABLE;
    static final String FEEDBACK_MESSAGE_PERMISSION_LIST = "comms.feedback_message.read";
    static final String FEEDBACK_MESSAGE_PERMISSION_GET = "comms.feedback_message.read";
    static final String FEEDBACK_MESSAGE_PERMISSION_CREATE = "comms.feedback_message.create";
    static final String FEEDBACK_MESSAGE_PERMISSION_UPDATE = "comms.feedback_message.update";
    static final String FEEDBACK_MESSAGE_PERMISSION_DELETE = "comms.feedback_message.delete";
    /** The contract's FeedbackMessageUpdate fields, in apply order. */
    static final List<String> FEEDBACK_MESSAGE_WRITABLE =
            List.of("senderUserId", "category", "message", "attachmentFileId", "reviewStatus", "deletedBySender",
            "deletedByReceiver");

    private final FeedbackThreadRepository repository;
    private final FeedbackThreadMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final FeedbackMessageRepository feedbackMessageRepository;
    private final FeedbackMessageMapper feedbackMessageMapper;

    public FeedbackThreadService(FeedbackThreadRepository repository,
            FeedbackThreadMapper mapper,
            @Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(CommsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(CommsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(CommsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            FeedbackMessageRepository feedbackMessageRepository,
            FeedbackMessageMapper feedbackMessageMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.feedbackMessageRepository = feedbackMessageRepository;
        this.feedbackMessageMapper = feedbackMessageMapper;
    }

    public FeedbackThread getFeedbackThread(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<FeedbackThread> listFeedbackThreads(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, FeedbackThreadRepository.SORT_FIELDS, FeedbackThreadRepository.DEFAULT_ORDER,
                FeedbackThreadRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, FeedbackThreadRepository.FILTER_FIELDS)
                .and(FeedbackThreadRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<FeedbackThreadEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public FeedbackThread createFeedbackThread(FeedbackThread request, boolean validateOnly) {
        validator.validate(request);
        FeedbackThreadEntity entity = mapper.toEntity(request);
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
            FeedbackThreadEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + CommsEvents.CREATED, AGGREGATE, stored.getId(),
                    CommsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public FeedbackThread updateFeedbackThread(UUID id, String ifMatch, FeedbackThreadUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            FeedbackThreadEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + CommsEvents.UPDATED, AGGREGATE, id, CommsEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteFeedbackThread(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            FeedbackThreadEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByFeedbackMessageThreadId(id)) {
                throw new InvalidStateException(
                        "The feedback thread is still referenced by comms.feedback_message rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + CommsEvents.DELETED, AGGREGATE, id, CommsEvents.payload(id));
        });
    }

    /**
     * comms.CloseFeedbackThread: Close. Allowed while status is open, on a row the caller may see and write; otherwise
     * 409 and nothing changes.
     */
    public FeedbackThread closeFeedbackThread(UUID id, CloseFeedbackThreadRequest request) {
        DataScope scope = DataScope.current(PERMISSION_CLOSE_FEEDBACK_THREAD);
        return doInWriteTransaction(() -> {
            FeedbackThreadEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("open").contains(entity.getStatus()))) {
                throw new InvalidStateException("The feedback thread cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.closeFeedbackThread(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "close", List.of("status"), null);
            outbox.publish(AGGREGATE + ".close", AGGREGATE, id, CommsEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * comms.ReopenFeedbackThread: Reopen. Allowed while status is closed, on a row the caller may see and write;
     * otherwise 409 and nothing changes.
     */
    public FeedbackThread reopenFeedbackThread(UUID id, ReopenFeedbackThreadRequest request) {
        DataScope scope = DataScope.current(PERMISSION_REOPEN_FEEDBACK_THREAD);
        return doInWriteTransaction(() -> {
            FeedbackThreadEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("closed").contains(entity.getStatus()))) {
                throw new InvalidStateException("The feedback thread cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.reopenFeedbackThread(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "reopen", List.of("status"), null);
            outbox.publish(AGGREGATE + ".reopen", AGGREGATE, id, CommsEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private FeedbackThreadEntity loadVisible(DataScope scope, UUID id) {
        FeedbackThreadEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(FeedbackThreadEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected FeedbackThreadUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(FeedbackThreadEntity entity, FeedbackThreadUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "guardianId" -> entity.setGuardianId(changes.getGuardianId());
                case "studentId" -> entity.setStudentId(changes.getStudentId());
                case "assignedToUserId" -> entity.setAssignedToUserId(changes.getAssignedToUserId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(FeedbackThreadEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getGuardianId() == null) {
            errors.add(new FieldError("guardianId", "must not be null", "NotNull"));
        }
        if (entity.getStudentId() == null) {
            errors.add(new FieldError("studentId", "must not be null", "NotNull"));
        }
        if (entity.getAssignedToUserId() == null) {
            errors.add(new FieldError("assignedToUserId", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(FeedbackThreadEntity entity) {

    }

    // ---- FeedbackMessage ----

    public FeedbackMessage getFeedbackMessage(UUID threadId, UUID id) {
        return doInReadTransaction(() -> {
            return feedbackMessageMapper.toDto(loadVisibleFeedbackMessage(DataScope.current(FEEDBACK_MESSAGE_PERMISSION_GET),
                    threadId, id));
        });
    }

    public Page<FeedbackMessage> listFeedbackMessages(UUID threadId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, FeedbackMessageRepository.SORT_FIELDS, FeedbackMessageRepository.DEFAULT_ORDER,
                FeedbackMessageRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, FeedbackMessageRepository.FILTER_FIELDS)
                .and(FeedbackMessageRepository.ofParent(threadId))
                        .and(FeedbackMessageRepository.scope(DataScope.current(FEEDBACK_MESSAGE_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + threadId, clock);
        return doInReadTransaction(() -> {
            Page<FeedbackMessageEntity> rows = feedbackMessageRepository.list(page, where);
            return rows.map(feedbackMessageMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public FeedbackMessage createFeedbackMessage(UUID threadId, FeedbackMessage request, boolean validateOnly) {
        validator.validate(request);
        FeedbackMessageEntity entity = feedbackMessageMapper.toEntity(request);
        entity.setThreadId(threadId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(FEEDBACK_MESSAGE_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!feedbackMessageRepository.existsThreadId(entity.getThreadId())) {
                throw new ResourceNotFoundException("comms.feedback_thread", entity.getThreadId());
            }
            if (!scope.canRead(institutionOfFeedbackMessage(entity))) {
                throw new ResourceNotFoundException("comms.feedback_thread", entity.getThreadId());
            }
            assertValidFeedbackMessage(entity, true);
            scope.requireWritable(institutionOfFeedbackMessage(entity));
            assertUniqueFeedbackMessage(entity);
            if (validateOnly) {
                return feedbackMessageMapper.toDto(entity);
            }
            FeedbackMessageEntity stored = feedbackMessageRepository.insert(entity);
            audit.recordCreate(FEEDBACK_MESSAGE_AGGREGATE, stored.getId());
            outbox.publish(FEEDBACK_MESSAGE_AGGREGATE + CommsEvents.CREATED, FEEDBACK_MESSAGE_AGGREGATE, stored.getId(),
                    CommsEvents.payload(stored.getId()));
            return feedbackMessageMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public FeedbackMessage updateFeedbackMessage(UUID threadId, UUID id, String ifMatch, FeedbackMessageUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, FEEDBACK_MESSAGE_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(FEEDBACK_MESSAGE_PERMISSION_UPDATE);
            FeedbackMessageEntity entity = loadVisibleFeedbackMessage(scope, threadId, id);
            scope.requireWritable(institutionOfFeedbackMessage(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyFeedbackMessage(entity, changes, fields);
            validator.validate(feedbackMessageMapper.toDto(entity));
            assertValidFeedbackMessage(entity, false);
            scope.requireWritable(institutionOfFeedbackMessage(entity));
            assertUniqueFeedbackMessage(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return feedbackMessageMapper.toDto(entity);
            }
            if (feedbackMessageRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(FEEDBACK_MESSAGE_AGGREGATE, id, fields);
            outbox.publish(FEEDBACK_MESSAGE_AGGREGATE + CommsEvents.UPDATED, FEEDBACK_MESSAGE_AGGREGATE, id,
                    CommsEvents.payload(id));
            return feedbackMessageMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteFeedbackMessage(UUID threadId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(FEEDBACK_MESSAGE_PERMISSION_DELETE);
            FeedbackMessageEntity entity = loadVisibleFeedbackMessage(scope, threadId, id);
            scope.requireWritable(institutionOfFeedbackMessage(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (feedbackMessageRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(FEEDBACK_MESSAGE_AGGREGATE, id);
            outbox.publish(FEEDBACK_MESSAGE_AGGREGATE + CommsEvents.DELETED, FEEDBACK_MESSAGE_AGGREGATE, id,
                    CommsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private FeedbackMessageEntity loadVisibleFeedbackMessage(DataScope scope, UUID threadId, UUID id) {
        FeedbackMessageEntity entity = feedbackMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(FEEDBACK_MESSAGE_AGGREGATE, id));
        if (!scope.canRead(institutionOfFeedbackMessage(entity)) || !threadId.equals(entity.getThreadId())) {
            throw new ResourceNotFoundException(FEEDBACK_MESSAGE_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the comms.feedback_thread the row belongs to. */
    private Optional<UUID> institutionOfFeedbackMessage(FeedbackMessageEntity entity) {
        return feedbackMessageRepository.institutionOf(entity.getThreadId());
    }

    /** Applies the selected FeedbackMessageUpdate fields; explicit per field so a mask can clear values. */
    private static void applyFeedbackMessage(FeedbackMessageEntity entity, FeedbackMessageUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "senderUserId" -> entity.setSenderUserId(changes.getSenderUserId());
                case "category" -> entity.setCategory(Optional.ofNullable(changes.getCategory()).map(v -> v.getValue())
                        .orElse(null));
                case "message" -> entity.setMessage(changes.getMessage());
                case "attachmentFileId" -> entity.setAttachmentFileId(changes.getAttachmentFileId());
                case "reviewStatus" -> entity.setReviewStatus(Optional.ofNullable(changes.getReviewStatus())
                        .map(v -> v.getValue()).orElse(null));
                case "deletedBySender" -> entity.setDeletedBySender(changes.getDeletedBySender());
                case "deletedByReceiver" -> entity.setDeletedByReceiver(changes.getDeletedByReceiver());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidFeedbackMessage(FeedbackMessageEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getThreadId() == null) {
            errors.add(new FieldError("threadId", "must not be null", "NotNull"));
        }
        if (entity.getSenderUserId() == null) {
            errors.add(new FieldError("senderUserId", "must not be null", "NotNull"));
        }
        if (entity.getCategory() == null) {
            errors.add(new FieldError("category", "must not be null", "NotNull"));
        }
        if (entity.getMessage() == null) {
            errors.add(new FieldError("message", "must not be null", "NotNull"));
        }
        if (!creating && entity.getReviewStatus() == null) {
            errors.add(new FieldError("reviewStatus", "must not be null", "NotNull"));
        }
        if (!creating && entity.getDeletedBySender() == null) {
            errors.add(new FieldError("deletedBySender", "must not be null", "NotNull"));
        }
        if (!creating && entity.getDeletedByReceiver() == null) {
            errors.add(new FieldError("deletedByReceiver", "must not be null", "NotNull"));
        }
        if (entity.getThreadId() != null && !feedbackMessageRepository.existsThreadId(entity.getThreadId())) {
            errors.add(new FieldError("threadId", "feedback thread does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueFeedbackMessage(FeedbackMessageEntity entity) {

    }
}
