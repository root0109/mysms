package com.mysms.comms.business;

import com.mysms.core.error.PermissionDeniedException;
import com.mysms.comms.api.model.MarkFeedbackMessageRequest;
import com.mysms.authclient.AuthContext;
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
import com.mysms.comms.repository.MemberDirectory;
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
import com.mysms.core.web.Patches;
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
 * institutions are invisible; tenant-level rows change only through a tenant-wide grant. A thread is reached — read,
 * written to, changed — only by its guardian (the caller's account or person is the thread's guardian record), the
 * staff member handling it, and the school side of its institution (those who mark feedback there); to anyone else it
 * and its messages are 404. Only the school side opens a thread for someone else; a guardian opens it as themself.
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

    public static final String FEEDBACK_MESSAGE_AGGREGATE = FeedbackMessageEntity.TABLE;
    static final String FEEDBACK_MESSAGE_PERMISSION_LIST = "comms.feedback_message.read";
    static final String FEEDBACK_MESSAGE_PERMISSION_GET = "comms.feedback_message.read";
    static final String FEEDBACK_MESSAGE_PERMISSION_CREATE = "comms.feedback_message.create";
    static final String FEEDBACK_MESSAGE_PERMISSION_UPDATE = "comms.feedback_message.update";
    static final String FEEDBACK_MESSAGE_PERMISSION_DELETE = "comms.feedback_message.delete";
    static final String PERMISSION_MARK_FEEDBACK_MESSAGE = "comms.feedback_message.mark_feedback_message";
    /** The permission only the school side holds: whoever marks feedback in an institution reaches its threads. */
    static final String PERMISSION_SCHOOL_SIDE = PERMISSION_MARK_FEEDBACK_MESSAGE;
    /** The handling states in order: a message's state only moves forward. */
    static final List<String> REVIEW_ORDER = List.of("new", "seen", "action_taken");
    /** The fields only the school side (anyone but the sender) changes; the rest are the sender's own. */
    static final Set<String> FEEDBACK_MESSAGE_RECEIVER_FIELDS = Set.of("reviewStatus", "deletedByReceiver");

    private final FeedbackThreadRepository repository;
    private final FeedbackThreadMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final FeedbackMessageRepository feedbackMessageRepository;
    private final FeedbackMessageMapper feedbackMessageMapper;
    private final MemberDirectory members;

    public FeedbackThreadService(FeedbackThreadRepository repository,
            FeedbackThreadMapper mapper,
            @Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(CommsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(CommsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(CommsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            FeedbackMessageRepository feedbackMessageRepository,
            FeedbackMessageMapper feedbackMessageMapper,
            MemberDirectory members) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.feedbackMessageRepository = feedbackMessageRepository;
        this.feedbackMessageMapper = feedbackMessageMapper;
        this.members = members;
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
        AuthContext caller = AuthContext.require();
        SqlFragment where = FilterParser.parse(filter, FeedbackThreadRepository.FILTER_FIELDS)
                .and(FeedbackThreadRepository.scope(DataScope.current(PERMISSION_LIST)))
                .and(FeedbackThreadRepository.reachableBy(caller.userId(), caller.personId(), schoolSide()));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + caller.userId(), clock);
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
            requireOpenedAsGuardianUnlessSchoolSide(entity);
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
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public FeedbackThread updateFeedbackThread(UUID id, String ifMatch, FeedbackThreadUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            FeedbackThreadEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = apply(entity, changes);
            validator.validate(mapper.toDto(entity));
            assertValid(entity, false);
            scope.requireWritable(institutionOf(entity));
            requireOpenedAsGuardianUnlessSchoolSide(entity);
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
            if (!scope.canRead(institutionOf(entity)) || !reaches(entity)) {
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
            if (!scope.canRead(institutionOf(entity)) || !reaches(entity)) {
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

    /** The live thread, if the caller may see it and reaches it; 404 otherwise. */
    private FeedbackThreadEntity loadVisible(DataScope scope, UUID id) {
        FeedbackThreadEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity)) || !reaches(entity)) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** Whether the caller is the thread's guardian, handles it, or works on the school side of its institution. */
    private boolean reaches(FeedbackThreadEntity thread) {
        AuthContext caller = AuthContext.require();
        return caller.userId().equals(thread.getAssignedToUserId())
                || thread.getInstitutionId() != null && schoolSide().canRead(institutionOf(thread))
                || thread.getGuardianId() != null && members.isGuardian(thread.getGuardianId(), caller.userId(),
                        caller.personId().orElse(null));
    }

    /** The institutions where the caller works on the school side; none without the permission. */
    private static DataScope schoolSide() {
        AuthContext caller = AuthContext.require();
        return caller.hasPermission(PERMISSION_SCHOOL_SIDE) ? DataScope.of(caller, PERMISSION_SCHOOL_SIDE)
                : new DataScope(false, Set.of(), false);
    }

    /** Anyone but the school side opens or keeps a thread only as the named student's guardian (403 otherwise). */
    private void requireOpenedAsGuardianUnlessSchoolSide(FeedbackThreadEntity thread) {
        if (thread.getInstitutionId() != null && schoolSide().canRead(institutionOf(thread))) {
            return;
        }
        AuthContext caller = AuthContext.require();
        if (!members.isGuardianOf(thread.getGuardianId(), thread.getStudentId(), caller.userId(),
                caller.personId().orElse(null))) {
            throw new PermissionDeniedException("A feedback thread is opened by the student's own guardian or by the "
                    + "school.");
        }
    }

    /** The thread the message belongs to, when the caller reaches it; 404 (as the thread) otherwise. */
    private FeedbackThreadEntity reachableThread(UUID threadId) {
        return repository.findById(threadId).filter(this::reaches)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, threadId));
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(FeedbackThreadEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(FeedbackThreadEntity entity, FeedbackThreadUpdate changes) {
        Patches patch = new Patches();
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("guardianId", changes.getGuardianId(), entity::setGuardianId);
        patch.apply("studentId", changes.getStudentId(), entity::setStudentId);
        patch.apply("assignedToUserId", changes.getAssignedToUserId(), entity::setAssignedToUserId);
        return patch.applied();
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
            reachableThread(threadId);
            Page<FeedbackMessageEntity> rows = feedbackMessageRepository.list(page, where);
            return rows.map(feedbackMessageMapper::toDto);
        });
    }

    /** Creates the caller's message in the thread; with validateOnly every check runs and nothing is written. */
    public FeedbackMessage createFeedbackMessage(UUID threadId, FeedbackMessage request, boolean validateOnly) {
        validator.validate(request);
        FeedbackMessageEntity entity = feedbackMessageMapper.toEntity(request);
        entity.setThreadId(threadId);
        entity.setSenderUserId(OwnRows.caller());
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
            reachableThread(entity.getThreadId());
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
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope. The message's own fields
     * change only by its sender (404 to anyone else); the handling state and the receiver's hiding only by the school
     * side (403 for the sender), the state never moving back (409).
     */
    public FeedbackMessage updateFeedbackMessage(UUID threadId, UUID id, String ifMatch, FeedbackMessageUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(FEEDBACK_MESSAGE_PERMISSION_UPDATE);
            FeedbackMessageEntity entity = loadVisibleFeedbackMessage(scope, threadId, id);
            String reviewed = entity.getReviewStatus();
            List<String> fields = applyFeedbackMessage(entity, changes); // in memory: who may change what comes first
            requireFieldsOwnedFeedbackMessage(entity, fields);
            scope.requireWritable(institutionOfFeedbackMessage(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            validator.validate(feedbackMessageMapper.toDto(entity));
            assertValidFeedbackMessage(entity, false);
            if (REVIEW_ORDER.indexOf(entity.getReviewStatus()) < REVIEW_ORDER.indexOf(reviewed)) {
                throw new InvalidStateException("The message's handling state only moves forward.");
            }
            scope.requireWritable(institutionOfFeedbackMessage(entity));
            assertUniqueFeedbackMessage(entity);
            if (fields.isEmpty()) {
                return feedbackMessageMapper.toDto(entity);
            }
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

    /**
     * Soft delete of the caller's own message under If-Match (anyone else's is 404; the school side hides it with
     * deletedByReceiver).
     */
    public void deleteFeedbackMessage(UUID threadId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(FEEDBACK_MESSAGE_PERMISSION_DELETE);
            FeedbackMessageEntity entity = loadVisibleFeedbackMessage(scope, threadId, id);
            OwnRows.require(entity.getSenderUserId(), FEEDBACK_MESSAGE_AGGREGATE, id);
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

    /**
     * comms.MarkFeedbackMessage: the school side records that a message was seen or acted on. The state only moves
     * forward (new → seen → action taken; 409 otherwise), and nobody marks a message they sent themself (403).
     */
    public FeedbackMessage markFeedbackMessage(UUID id, MarkFeedbackMessageRequest request) {
        DataScope scope = DataScope.current(PERMISSION_MARK_FEEDBACK_MESSAGE);
        String status = request.getReviewStatus().getValue();
        UUID caller = OwnRows.caller();
        return doInWriteTransaction(() -> {
            FeedbackMessageEntity entity = feedbackMessageRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(FEEDBACK_MESSAGE_AGGREGATE, id));
            if (!scope.canRead(institutionOfFeedbackMessage(entity))
                    || repository.findById(entity.getThreadId()).filter(this::reaches).isEmpty()) {
                throw new ResourceNotFoundException(FEEDBACK_MESSAGE_AGGREGATE, id);
            }
            scope.requireWritable(institutionOfFeedbackMessage(entity));
            if (caller.equals(entity.getSenderUserId())) {
                throw new PermissionDeniedException("The sender of a message does not mark it.");
            }
            if (REVIEW_ORDER.indexOf(status) <= REVIEW_ORDER.indexOf(entity.getReviewStatus())) {
                throw new InvalidStateException("The message's handling state only moves forward.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (feedbackMessageRepository.markReviewStatus(id, entity.getRowVersion(), status, now,
                    Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(FEEDBACK_MESSAGE_AGGREGATE, id, "mark_" + status, List.of("reviewStatus"), null);
            outbox.publish(FEEDBACK_MESSAGE_AGGREGATE + ".mark_" + status, FEEDBACK_MESSAGE_AGGREGATE, id,
                    CommsEvents.payload(id));
            return feedbackMessageMapper.toDto(feedbackMessageRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(FEEDBACK_MESSAGE_AGGREGATE, id)));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private FeedbackMessageEntity loadVisibleFeedbackMessage(DataScope scope, UUID threadId, UUID id) {
        FeedbackMessageEntity entity = feedbackMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(FEEDBACK_MESSAGE_AGGREGATE, id));
        if (!scope.canRead(institutionOfFeedbackMessage(entity)) || !threadId.equals(entity.getThreadId())
                || repository.findById(threadId).filter(this::reaches).isEmpty()) {
            throw new ResourceNotFoundException(FEEDBACK_MESSAGE_AGGREGATE, id);
        }
        return entity;
    }

    /**
     * The sender changes the message's own fields and never the school side's (403); anyone else changes only the
     * school side's, and the sender's message is not theirs to edit (404).
     */
    private static void requireFieldsOwnedFeedbackMessage(FeedbackMessageEntity entity, List<String> fields) {
        boolean sender = OwnRows.caller().equals(entity.getSenderUserId());
        for (String field : fields) {
            boolean receiverField = FEEDBACK_MESSAGE_RECEIVER_FIELDS.contains(field);
            if (sender && receiverField) {
                throw new PermissionDeniedException("The sender of a message does not mark or hide it for the school.");
            }
            if (!sender && !receiverField) {
                throw new ResourceNotFoundException(FEEDBACK_MESSAGE_AGGREGATE, entity.getId());
            }
        }
    }

    /** The institution of the comms.feedback_thread the row belongs to. */
    private Optional<UUID> institutionOfFeedbackMessage(FeedbackMessageEntity entity) {
        return feedbackMessageRepository.institutionOf(entity.getThreadId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyFeedbackMessage(FeedbackMessageEntity entity, FeedbackMessageUpdate changes) {
        Patches patch = new Patches();
        patch.apply("category", changes.getCategory(), v -> entity.setCategory(v == null ? null : v.getValue()));
        patch.apply("message", changes.getMessage(), entity::setMessage);
        patch.apply("attachmentFileId", changes.getAttachmentFileId(), entity::setAttachmentFileId);
        patch.apply("reviewStatus", changes.getReviewStatus(), v -> entity.setReviewStatus(v == null ? null : v.getValue()));
        patch.apply("deletedBySender", changes.getDeletedBySender(), entity::setDeletedBySender);
        patch.apply("deletedByReceiver", changes.getDeletedByReceiver(), entity::setDeletedByReceiver);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidFeedbackMessage(FeedbackMessageEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getThreadId() == null) {
            errors.add(new FieldError("threadId", "must not be null", "NotNull"));
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
