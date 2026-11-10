package com.mysms.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.lms.api.model.AcceptGroupRequestRequest;
import com.mysms.lms.api.model.AssignmentGroupMember;
import com.mysms.lms.api.model.AssignmentGroupMemberUpdate;
import com.mysms.lms.api.model.RejectGroupRequestRequest;
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
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.AssignmentGroupMemberEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.AssignmentGroupMemberMapper;
import com.mysms.lms.repository.AssignmentGroupMemberRepository;
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
 * Business service of lms.assignment_group_member (JAVA-CODING-RULES §4): one plain class, explicit methods,
 * transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 */
@Service
public class AssignmentGroupMemberService extends AppTransactionManager {

    public static final String AGGREGATE = AssignmentGroupMemberEntity.TABLE;
    static final String PERMISSION_LIST = "lms.assignment_group_member.read";
    static final String PERMISSION_GET = "lms.assignment_group_member.read";
    static final String PERMISSION_CREATE = "lms.assignment_group_member.create";
    static final String PERMISSION_UPDATE = "lms.assignment_group_member.update";
    static final String PERMISSION_DELETE = "lms.assignment_group_member.delete";
    static final String PERMISSION_ACCEPT_GROUP_REQUEST = "lms.assignment_group_member.accept_group_request";
    static final String PERMISSION_REJECT_GROUP_REQUEST = "lms.assignment_group_member.reject_group_request";
    /** The contract's AssignmentGroupMemberUpdate fields, in apply order. */
    static final List<String> WRITABLE = List.of("assignmentGroupId", "userId", "contentItemId");


    private final AssignmentGroupMemberRepository repository;
    private final AssignmentGroupMemberMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public AssignmentGroupMemberService(AssignmentGroupMemberRepository repository, AssignmentGroupMemberMapper mapper,
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

    public AssignmentGroupMember getAssignmentGroupMember(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<AssignmentGroupMember> listAssignmentGroupMembers(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, AssignmentGroupMemberRepository.SORT_FIELDS,
                        AssignmentGroupMemberRepository.DEFAULT_ORDER,
                AssignmentGroupMemberRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, AssignmentGroupMemberRepository.FILTER_FIELDS)
                .and(AssignmentGroupMemberRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<AssignmentGroupMemberEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public AssignmentGroupMember createAssignmentGroupMember(AssignmentGroupMember request, boolean validateOnly) {
        validator.validate(request);
        AssignmentGroupMemberEntity entity = mapper.toEntity(request);
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
            AssignmentGroupMemberEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public AssignmentGroupMember updateAssignmentGroupMember(UUID id, String ifMatch,
            AssignmentGroupMemberUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            AssignmentGroupMemberEntity entity = loadVisible(scope, id);
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
    public void deleteAssignmentGroupMember(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            AssignmentGroupMemberEntity entity = loadVisible(scope, id);
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

    /**
     * lms.AcceptGroupRequest: Accept a join request. The row must be visible and writable for the caller, and the step
     * is allowed while status is requested; in any other state
     * the answer is 409 and nothing changes.
     */
    public AssignmentGroupMember acceptGroupRequest(UUID id, AcceptGroupRequestRequest request) {
        DataScope scope = DataScope.current(PERMISSION_ACCEPT_GROUP_REQUEST);
        return doInWriteTransaction(() -> {
            AssignmentGroupMemberEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("requested").contains(entity.getStatus()))) {
                throw new InvalidStateException(
                        "The assignment group member cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.acceptGroupRequest(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "acceptGroupRequest", List.of("status"), null);
            outbox.publish(AGGREGATE + ".acceptGroupRequest", AGGREGATE, id, LmsEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * lms.RejectGroupRequest: Reject a join request. The row must be visible and writable for the caller, and the step
     * is allowed while status is requested; in any other state
     * the answer is 409 and nothing changes.
     */
    public AssignmentGroupMember rejectGroupRequest(UUID id, RejectGroupRequestRequest request) {
        DataScope scope = DataScope.current(PERMISSION_REJECT_GROUP_REQUEST);
        return doInWriteTransaction(() -> {
            AssignmentGroupMemberEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("requested").contains(entity.getStatus()))) {
                throw new InvalidStateException(
                        "The assignment group member cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.rejectGroupRequest(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "rejectGroupRequest", List.of("status"), request == null ? null
                    : request.getReason());
            outbox.publish(AGGREGATE + ".rejectGroupRequest", AGGREGATE, id, LmsEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private AssignmentGroupMemberEntity loadVisible(DataScope scope, UUID id) {
        AssignmentGroupMemberEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the lms.assignment_group → lms.cohort it belongs to). */
    private Optional<UUID> institutionOf(AssignmentGroupMemberEntity entity) {
        return repository.institutionOf(entity.getAssignmentGroupId());
    }

    /**
     * Applies the selected AssignmentGroupMemberUpdate fields; explicit per field so a mask can clear values (no
     * reflection).
     */
    private static void apply(AssignmentGroupMemberEntity entity, AssignmentGroupMemberUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "assignmentGroupId" -> entity.setAssignmentGroupId(changes.getAssignmentGroupId());
                case "userId" -> entity.setUserId(changes.getUserId());
                case "contentItemId" -> entity.setContentItemId(changes.getContentItemId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(AssignmentGroupMemberEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getAssignmentGroupId() == null) {
            errors.add(new FieldError("assignmentGroupId", "must not be null", "NotNull"));
        }
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (entity.getContentItemId() == null) {
            errors.add(new FieldError("contentItemId", "must not be null", "NotNull"));
        }
        if (entity.getAssignmentGroupId() != null
                && !repository.existsAssignmentGroupId(entity.getAssignmentGroupId())) {
            errors.add(new FieldError("assignmentGroupId", "assignment group does not exist", "NOT_FOUND"));
        }
        if (entity.getContentItemId() != null && !repository.existsContentItemId(entity.getContentItemId())) {
            errors.add(new FieldError("contentItemId", "content item does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(AssignmentGroupMemberEntity entity) {
        if (repository.takenAssignmentGroupIdUserId2(entity)) {
            throw new DuplicateResourceException(
                    "A assignment group member with the same assignment group id and user id already exists.");
        }
    }
}
