package com.mysms.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.lms.api.model.PeerReview;
import com.mysms.lms.api.model.PeerReviewUpdate;
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
import com.mysms.core.mapping.WireValues;
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
import com.mysms.lms.domain.PeerReviewEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.PeerReviewMapper;
import com.mysms.lms.repository.PeerReviewRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.peer_review (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 */
@Service
public class PeerReviewService extends AppTransactionManager {

    public static final String AGGREGATE = PeerReviewEntity.TABLE;
    static final String PERMISSION_LIST = "lms.peer_review.read";
    static final String PERMISSION_GET = "lms.peer_review.read";
    static final String PERMISSION_CREATE = "lms.peer_review.create";
    static final String PERMISSION_UPDATE = "lms.peer_review.update";
    static final String PERMISSION_DELETE = "lms.peer_review.delete";
    /** The contract's PeerReviewUpdate fields, in apply order. */
    static final List<String> WRITABLE = List.of("assignmentGroupId", "reviewerUserId", "revieweeUserId", "points");


    private final PeerReviewRepository repository;
    private final PeerReviewMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public PeerReviewService(PeerReviewRepository repository, PeerReviewMapper mapper,
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

    public PeerReview getPeerReview(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<PeerReview> listPeerReviews(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, PeerReviewRepository.SORT_FIELDS, PeerReviewRepository.DEFAULT_ORDER,
                PeerReviewRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, PeerReviewRepository.FILTER_FIELDS)
                .and(PeerReviewRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<PeerReviewEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public PeerReview createPeerReview(PeerReview request, boolean validateOnly) {
        validator.validate(request);
        PeerReviewEntity entity = mapper.toEntity(request);
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
            PeerReviewEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public PeerReview updatePeerReview(UUID id, String ifMatch, PeerReviewUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            PeerReviewEntity entity = loadVisible(scope, id);
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
    public void deletePeerReview(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            PeerReviewEntity entity = loadVisible(scope, id);
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
    private PeerReviewEntity loadVisible(DataScope scope, UUID id) {
        PeerReviewEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (the institution of the lms.assignment_group → lms.cohort it belongs to). */
    private Optional<UUID> institutionOf(PeerReviewEntity entity) {
        return repository.institutionOf(entity.getAssignmentGroupId());
    }

    /** Applies the selected PeerReviewUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(PeerReviewEntity entity, PeerReviewUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "assignmentGroupId" -> entity.setAssignmentGroupId(changes.getAssignmentGroupId());
                case "reviewerUserId" -> entity.setReviewerUserId(changes.getReviewerUserId());
                case "revieweeUserId" -> entity.setRevieweeUserId(changes.getRevieweeUserId());
                case "points" -> entity.setPoints(WireValues.decimal(changes.getPoints()));
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(PeerReviewEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getAssignmentGroupId() == null) {
            errors.add(new FieldError("assignmentGroupId", "must not be null", "NotNull"));
        }
        if (entity.getReviewerUserId() == null) {
            errors.add(new FieldError("reviewerUserId", "must not be null", "NotNull"));
        }
        if (entity.getRevieweeUserId() == null) {
            errors.add(new FieldError("revieweeUserId", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("points", WireValues.decimal(entity.getPoints()), 8, 2, errors);
        if (entity.getAssignmentGroupId() != null
                && !repository.existsAssignmentGroupId(entity.getAssignmentGroupId())) {
            errors.add(new FieldError("assignmentGroupId", "assignment group does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(PeerReviewEntity entity) {
        if (repository.takenAssignmentGroupIdReviewerUserIdRevieweeUserId1(entity)) {
            throw new DuplicateResourceException(
                    "A peer review with the same assignment group id and reviewer user id and reviewee user id "
                            + "already exists.");
        }
    }
}
