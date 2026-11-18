package com.mysms.assessment.business;

import com.mysms.assessment.api.model.Homework;
import com.mysms.assessment.api.model.HomeworkApproval;
import com.mysms.assessment.api.model.HomeworkUpdate;
import com.mysms.assessment.config.AssessmentDatabaseConfig;
import com.mysms.assessment.config.AssessmentEventsConfig;
import com.mysms.assessment.domain.HomeworkApprovalEntity;
import com.mysms.assessment.domain.HomeworkEntity;
import com.mysms.assessment.events.AssessmentEvents;
import com.mysms.assessment.mapper.HomeworkApprovalMapper;
import com.mysms.assessment.mapper.HomeworkMapper;
import com.mysms.assessment.repository.HomeworkApprovalRepository;
import com.mysms.assessment.repository.HomeworkRepository;
import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of assessment.homework and of the records that exist only as part of a Homework (HomeworkApproval,
 * which is read-only here: approvals are written by the homework workflow ({@link HomeworkWorkflowService}) alone, so
 * the contract has no create, update or delete for them): audit and domain event commit in the same transaction as
 * every change. Rows outside the caller's institutions are invisible; tenant-level rows change only through a
 * tenant-wide grant.
 */
@Service
public class HomeworkService extends AppTransactionManager {

    public static final String AGGREGATE = HomeworkEntity.TABLE;
    static final String PERMISSION_LIST = "assessment.homework.read";
    static final String PERMISSION_GET = "assessment.homework.read";
    static final String PERMISSION_CREATE = "assessment.homework.create";
    static final String PERMISSION_UPDATE = "assessment.homework.update";
    static final String PERMISSION_DELETE = "assessment.homework.delete";
    /** The contract's HomeworkUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("institutionId", "academicYearId", "classSectionId", "subjectId", "assignedByStaffId",
                    "description",
            "attachmentFileId", "assignedOn", "dueAt");

    public static final String HOMEWORK_APPROVAL_AGGREGATE = HomeworkApprovalEntity.TABLE;
    static final String HOMEWORK_APPROVAL_PERMISSION_LIST = "assessment.homework_approval.read";
    static final String HOMEWORK_APPROVAL_PERMISSION_GET = "assessment.homework_approval.read";

    private final HomeworkRepository repository;
    private final HomeworkMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final HomeworkApprovalRepository homeworkApprovalRepository;
    private final HomeworkApprovalMapper homeworkApprovalMapper;

    public HomeworkService(HomeworkRepository repository,
            HomeworkMapper mapper,
            @Qualifier(AssessmentDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AssessmentDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AssessmentEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(AssessmentEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            HomeworkApprovalRepository homeworkApprovalRepository,
            HomeworkApprovalMapper homeworkApprovalMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.homeworkApprovalRepository = homeworkApprovalRepository;
        this.homeworkApprovalMapper = homeworkApprovalMapper;
    }

    public Homework getHomework(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Homework> listHomeworks(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, HomeworkRepository.SORT_FIELDS, HomeworkRepository.DEFAULT_ORDER,
                HomeworkRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, HomeworkRepository.FILTER_FIELDS)
                .and(HomeworkRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<HomeworkEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Homework createHomework(Homework request, boolean validateOnly) {
        validator.validate(request);
        HomeworkEntity entity = mapper.toEntity(request);
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
            HomeworkEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + AssessmentEvents.CREATED, AGGREGATE, stored.getId(),
                    AssessmentEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public Homework updateHomework(UUID id, String ifMatch, HomeworkUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            HomeworkEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            requireEditable(entity);
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
            outbox.publish(AGGREGATE + AssessmentEvents.UPDATED, AGGREGATE, id, AssessmentEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /**
     * A draft is its teacher's to change. Homework waiting for review does not change, or the reviewer would decide
     * on something else than what they read. Published homework changes only in the hands of someone who may approve
     * homework in its institution — the same rule that lets them publish without review — so an approved text
     * cannot be replaced by an unreviewed one.
     */
    private void requireEditable(HomeworkEntity entity) {
        String status = entity.getStatus();
        if ("draft".equals(status)) {
            return;
        }
        if ("pending".equals(status)) {
            throw new InvalidStateException(
                    "The homework is waiting for review; it can be changed once it is approved or rejected.");
        }
        boolean reviewer = AuthContext.require().hasPermission(HomeworkWorkflowService.PERMISSION_APPROVE)
                && DataScope.current(HomeworkWorkflowService.PERMISSION_APPROVE).canRead(institutionOf(entity));
        if (!reviewer) {
            throw new InvalidStateException(
                    "Published homework is changed only by someone who may approve homework.");
        }
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteHomework(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            HomeworkEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByHomeworkApprovalHomeworkId(id)) {
                throw new InvalidStateException(
                        "The homework is still referenced by assessment.homework_approval rows; remove those first.");
            }
            if (repository.referencedByHomeworkSubmissionHomeworkId(id)) {
                throw new InvalidStateException(
                        "The homework is still referenced by assessment.homework_submission rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + AssessmentEvents.DELETED, AGGREGATE, id, AssessmentEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private HomeworkEntity loadVisible(DataScope scope, UUID id) {
        HomeworkEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(HomeworkEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the selected HomeworkUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(HomeworkEntity entity, HomeworkUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "academicYearId" -> entity.setAcademicYearId(changes.getAcademicYearId());
                case "classSectionId" -> entity.setClassSectionId(changes.getClassSectionId());
                case "subjectId" -> entity.setSubjectId(changes.getSubjectId());
                case "assignedByStaffId" -> entity.setAssignedByStaffId(changes.getAssignedByStaffId());
                case "description" -> entity.setDescription(changes.getDescription());
                case "attachmentFileId" -> entity.setAttachmentFileId(changes.getAttachmentFileId());
                case "assignedOn" -> entity.setAssignedOn(changes.getAssignedOn());
                case "dueAt" -> entity.setDueAt(changes.getDueAt());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(HomeworkEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getAcademicYearId() == null) {
            errors.add(new FieldError("academicYearId", "must not be null", "NotNull"));
        }
        if (entity.getClassSectionId() == null) {
            errors.add(new FieldError("classSectionId", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(HomeworkEntity entity) {

    }

    // ---- HomeworkApproval ----

    public HomeworkApproval getHomeworkApproval(UUID homeworkId, UUID id) {
        return doInReadTransaction(() -> {
            return homeworkApprovalMapper.toDto(loadVisibleHomeworkApproval(DataScope.current(
                    HOMEWORK_APPROVAL_PERMISSION_GET), homeworkId, id));
        });
    }

    public Page<HomeworkApproval> listHomeworkApprovals(UUID homeworkId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, HomeworkApprovalRepository.SORT_FIELDS,
                        HomeworkApprovalRepository.DEFAULT_ORDER,
                HomeworkApprovalRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, HomeworkApprovalRepository.FILTER_FIELDS)
                .and(HomeworkApprovalRepository.ofParent(homeworkId))
                        .and(HomeworkApprovalRepository.scope(DataScope.current(HOMEWORK_APPROVAL_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + homeworkId, clock);
        return doInReadTransaction(() -> {
            Page<HomeworkApprovalEntity> rows = homeworkApprovalRepository.list(page, where);
            return rows.map(homeworkApprovalMapper::toDto);
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private HomeworkApprovalEntity loadVisibleHomeworkApproval(DataScope scope, UUID homeworkId, UUID id) {
        HomeworkApprovalEntity entity = homeworkApprovalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(HOMEWORK_APPROVAL_AGGREGATE, id));
        if (!scope.canRead(institutionOfHomeworkApproval(entity)) || !homeworkId.equals(entity.getHomeworkId())) {
            throw new ResourceNotFoundException(HOMEWORK_APPROVAL_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the assessment.homework the row belongs to. */
    private Optional<UUID> institutionOfHomeworkApproval(HomeworkApprovalEntity entity) {
        return homeworkApprovalRepository.institutionOf(entity.getHomeworkId());
    }
}
