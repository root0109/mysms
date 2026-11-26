package com.mysms.learning.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.learning.lms.api.model.LmsAssignment;
import com.mysms.learning.lms.api.model.LmsAssignmentUpdate;
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
import com.mysms.core.mapping.WireValues;
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
import com.mysms.learning.lms.domain.LmsAssignmentEntity;
import com.mysms.learning.lms.events.LmsEvents;
import com.mysms.learning.lms.mapper.LmsAssignmentMapper;
import com.mysms.learning.lms.repository.LmsAssignmentRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.assignment: audit and domain event commit in the same transaction as every change. Rows
 * outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class LmsAssignmentService extends AppTransactionManager {

    public static final String AGGREGATE = LmsAssignmentEntity.TABLE;
    static final String PERMISSION_LIST = "lms.assignment.read";
    static final String PERMISSION_GET = "lms.assignment.read";
    static final String PERMISSION_CREATE = "lms.assignment.create";
    static final String PERMISSION_UPDATE = "lms.assignment.update";
    static final String PERMISSION_DELETE = "lms.assignment.delete";


    private final LmsAssignmentRepository repository;
    private final LmsAssignmentMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public LmsAssignmentService(LmsAssignmentRepository repository, LmsAssignmentMapper mapper,
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

    public LmsAssignment getLmsAssignment(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<LmsAssignment> listLmsAssignments(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, LmsAssignmentRepository.SORT_FIELDS, LmsAssignmentRepository.DEFAULT_ORDER,
                LmsAssignmentRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, LmsAssignmentRepository.FILTER_FIELDS)
                .and(LmsAssignmentRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<LmsAssignmentEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public LmsAssignment createLmsAssignment(LmsAssignment request, boolean validateOnly) {
        validator.validate(request);
        LmsAssignmentEntity entity = mapper.toEntity(request);
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
            LmsAssignmentEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public LmsAssignment updateLmsAssignment(UUID id, String ifMatch, LmsAssignmentUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            LmsAssignmentEntity entity = loadVisible(scope, id);
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

    /**
     * Soft delete under If-Match; refused while live rows of this module still reference the assignment (its
     * submissions, placements, versions), and for a published version (409): published content is immutable.
     */
    public void deleteLmsAssignment(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            LmsAssignmentEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (entity.getPublishedAt() != null) {
                throw new InvalidStateException("A published assignment is kept; make a new version instead.");
            }
            if (repository.referencedByAssignmentSupersededById(id)) {
                throw new InvalidStateException(
                        "The assignment is still referenced by lms.assignment (a newer version) rows; remove those first.");
            }
            if (repository.referencedByAssignmentSubmissionAssignmentId(id)) {
                throw new InvalidStateException(
                        "The assignment is still referenced by lms.assignment_submission rows; remove those first.");
            }
            if (repository.referencedByContentAuthorAssignmentId(id)) {
                throw new InvalidStateException(
                        "The assignment is still referenced by lms.content_author rows; remove those first.");
            }
            if (repository.referencedByContentItemAssignmentId(id)) {
                throw new InvalidStateException(
                        "The assignment is still referenced by lms.content_item rows; remove those first.");
            }
            if (repository.referencedByLessonActivityAssignmentId(id)) {
                throw new InvalidStateException(
                        "The assignment is still referenced by lms.lesson_activity rows; remove those first.");
            }
            if (repository.referencedByTimetableEntryAssignmentId(id)) {
                throw new InvalidStateException(
                        "The assignment is still referenced by lms.timetable_entry rows; remove those first.");
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
    private LmsAssignmentEntity loadVisible(DataScope scope, UUID id) {
        LmsAssignmentEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(LmsAssignmentEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(LmsAssignmentEntity entity, LmsAssignmentUpdate changes) {
        Patches patch = new Patches();
        patch.apply("title", changes.getTitle(), entity::setTitle);
        patch.apply("question", changes.getQuestion(), entity::setQuestion);
        patch.apply("submissionType", changes.getSubmissionType(),
                                                v -> entity.setSubmissionType(v == null ? null : v.getValue()));
        patch.apply("courseId", changes.getCourseId(), entity::setCourseId);
        patch.apply("isGraded", changes.getIsGraded(), entity::setIsGraded);
        patch.apply("showModelAnswer", changes.getShowModelAnswer(), entity::setShowModelAnswer);
        patch.apply("modelAnswer", changes.getModelAnswer(), entity::setModelAnswer);
        patch.apply("schedulingEnabled", changes.getSchedulingEnabled(), entity::setSchedulingEnabled);
        patch.apply("scheduleStart", changes.getScheduleStart(), entity::setScheduleStart);
        patch.apply("scheduleEnd", changes.getScheduleEnd(), entity::setScheduleEnd);
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("submissionMode", changes.getSubmissionMode(),
                                                v -> entity.setSubmissionMode(v == null ? null : v.getValue()));
        patch.apply("minGroupSize", changes.getMinGroupSize(), entity::setMinGroupSize);
        patch.apply("maxGroupSize", changes.getMaxGroupSize(), entity::setMaxGroupSize);
        patch.apply("peerReviewPoints", changes.getPeerReviewPoints(), v -> entity.setPeerReviewPoints(WireValues.decimal(v)));
        patch.apply("usesInlineAuthoring", changes.getUsesInlineAuthoring(), entity::setUsesInlineAuthoring);
        patch.apply("isInRepository", changes.getIsInRepository(), entity::setIsInRepository);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(LmsAssignmentEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        if (entity.getQuestion() == null) {
            errors.add(new FieldError("question", "must not be null", "NotNull"));
        }
        if (entity.getSubmissionType() == null) {
            errors.add(new FieldError("submissionType", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsGraded() == null) {
            errors.add(new FieldError("isGraded", "must not be null", "NotNull"));
        }
        if (!creating && entity.getShowModelAnswer() == null) {
            errors.add(new FieldError("showModelAnswer", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSchedulingEnabled() == null) {
            errors.add(new FieldError("schedulingEnabled", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSubmissionMode() == null) {
            errors.add(new FieldError("submissionMode", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("peerReviewPoints", WireValues.decimal(entity.getPeerReviewPoints()), 8, 2, errors);
        if (!creating && entity.getUsesInlineAuthoring() == null) {
            errors.add(new FieldError("usesInlineAuthoring", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsInRepository() == null) {
            errors.add(new FieldError("isInRepository", "must not be null", "NotNull"));
        }
        if (entity.getCourseId() != null && !repository.existsCourseId(entity.getCourseId())) {
            errors.add(new FieldError("courseId", "course does not exist", "NOT_FOUND"));
        }
        if (entity.getSupersededById() != null && !repository.existsSupersededById(entity.getSupersededById())) {
            errors.add(new FieldError("supersededById", "assignment does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(LmsAssignmentEntity entity) {

    }
}
