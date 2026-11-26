package com.mysms.comms.business;

import com.mysms.core.error.PermissionDeniedException;
import com.mysms.comms.api.model.TaskAssigneeList;
import com.mysms.authclient.DataScope;
import com.mysms.comms.api.model.CompleteTaskRequest;
import com.mysms.comms.api.model.ReopenTaskRequest;
import com.mysms.comms.api.model.SnoozeTaskRequest;
import com.mysms.comms.api.model.Task;
import com.mysms.comms.api.model.TaskAssignee;
import com.mysms.comms.api.model.TaskAssigneeUpdate;
import com.mysms.comms.api.model.TaskUpdate;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.config.CommsEventsConfig;
import com.mysms.comms.domain.TaskAssigneeEntity;
import com.mysms.comms.domain.TaskEntity;
import com.mysms.comms.events.CommsEvents;
import com.mysms.comms.mapper.TaskAssigneeMapper;
import com.mysms.comms.mapper.TaskMapper;
import com.mysms.comms.repository.TaskAssigneeRepository;
import com.mysms.comms.repository.TaskRepository;
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
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of comms.task and of the records that exist only as part of a Task (TaskAssignee): audit and domain
 * event commit in the same transaction as every change. Rows outside the caller's institutions are invisible;
 * tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class TaskService extends AppTransactionManager {

    public static final String AGGREGATE = TaskEntity.TABLE;
    static final String PERMISSION_LIST = "comms.task.read";
    static final String PERMISSION_GET = "comms.task.read";
    static final String PERMISSION_CREATE = "comms.task.create";
    static final String PERMISSION_UPDATE = "comms.task.update";
    static final String PERMISSION_DELETE = "comms.task.delete";
    static final String PERMISSION_COMPLETE_TASK = "comms.task.complete_task";
    static final String PERMISSION_REOPEN_TASK = "comms.task.reopen_task";
    static final String PERMISSION_SNOOZE_TASK = "comms.task.snooze_task";
    static final String PERMISSION_CLAIM_TASK = "comms.task.claim_task";

    public static final String TASK_ASSIGNEE_AGGREGATE = TaskAssigneeEntity.TABLE;
    static final String TASK_ASSIGNEE_PERMISSION_LIST = "comms.task_assignee.read";
    static final String TASK_ASSIGNEE_PERMISSION_GET = "comms.task_assignee.read";
    static final String TASK_ASSIGNEE_PERMISSION_CREATE = "comms.task_assignee.create";
    static final String TASK_ASSIGNEE_PERMISSION_UPDATE = "comms.task_assignee.update";
    static final String TASK_ASSIGNEE_PERMISSION_DELETE = "comms.task_assignee.delete";

    private final TaskRepository repository;
    private final TaskMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final TaskAssigneeRepository taskAssigneeRepository;
    private final TaskAssigneeMapper taskAssigneeMapper;

    public TaskService(TaskRepository repository,
            TaskMapper mapper,
            @Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(CommsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(CommsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(CommsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            TaskAssigneeRepository taskAssigneeRepository,
            TaskAssigneeMapper taskAssigneeMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.taskAssigneeRepository = taskAssigneeRepository;
        this.taskAssigneeMapper = taskAssigneeMapper;
    }

    public Task getTask(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Task> listTasks(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, TaskRepository.SORT_FIELDS, TaskRepository.DEFAULT_ORDER,
                TaskRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, TaskRepository.FILTER_FIELDS)
                .and(TaskRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<TaskEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Task createTask(Task request, boolean validateOnly) {
        validator.validate(request);
        TaskEntity entity = mapper.toEntity(request);
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
            TaskEntity stored = repository.insert(entity);
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
    public Task updateTask(UUID id, String ifMatch, TaskUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            TaskEntity entity = loadVisible(scope, id);
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
            outbox.publish(AGGREGATE + CommsEvents.UPDATED, AGGREGATE, id, CommsEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteTask(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            TaskEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByTaskAssigneeTaskId(id)) {
                throw new InvalidStateException(
                        "The task is still referenced by comms.task_assignee rows; remove those first.");
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
     * comms.CompleteTask: Complete. Allowed while is_completed is false, on a row the caller may see and write;
     * otherwise 409 and nothing changes.
     */
    public Task completeTask(UUID id, CompleteTaskRequest request) {
        DataScope scope = DataScope.current(PERMISSION_COMPLETE_TASK);
        return doInWriteTransaction(() -> {
            TaskEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(!Boolean.TRUE.equals(entity.getIsCompleted()))) {
                throw new InvalidStateException("The task cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.completeTask(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "complete", List.of("isCompleted"), null);
            outbox.publish(AGGREGATE + ".complete", AGGREGATE, id, CommsEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * comms.ReopenTask: Reopen. Allowed while is_completed is true, on a row the caller may see and write; otherwise
     * 409 and nothing changes.
     */
    public Task reopenTask(UUID id, ReopenTaskRequest request) {
        DataScope scope = DataScope.current(PERMISSION_REOPEN_TASK);
        return doInWriteTransaction(() -> {
            TaskEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Boolean.TRUE.equals(entity.getIsCompleted()))) {
                throw new InvalidStateException("The task cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.reopenTask(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "reopen", List.of("isCompleted"), null);
            outbox.publish(AGGREGATE + ".reopen", AGGREGATE, id, CommsEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * comms.SnoozeTask: Snooze reminder. Allowed while is_completed is false, on a row the caller may see and write;
     * otherwise 409 and nothing changes.
     */
    public Task snoozeTask(UUID id, SnoozeTaskRequest request) {
        DataScope scope = DataScope.current(PERMISSION_SNOOZE_TASK);
        return doInWriteTransaction(() -> {
            TaskEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(!Boolean.TRUE.equals(entity.getIsCompleted()))) {
                throw new InvalidStateException("The task cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.snoozeTask(id, entity.getRowVersion(), now, Actor.current().orElse(null), request == null
                    ? null : request.getUntil()) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "snooze", List.of("isSnoozed", "reminderAt"), null);
            outbox.publish(AGGREGATE + ".snooze", AGGREGATE, id, CommsEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * comms.ClaimTask: the caller takes an open task (assignment mode open, not completed) that is assigned to them,
     * directly or through one of their groups. Their pending, unclaimed assignments of the task become claimed by
     * them; answers those assignments. A task not assigned to the caller is 403; one already claimed by someone else
     * is 409. Assignments to a class are not claimable until comms knows who is in which class.
     */
    public TaskAssigneeList claimTask(UUID id) {
        DataScope scope = DataScope.current(PERMISSION_CLAIM_TASK);
        UUID caller = OwnRows.caller();
        return doInWriteTransaction(() -> {
            TaskEntity task = loadVisible(scope, id);
            if (Boolean.TRUE.equals(task.getIsCompleted()) || !"open".equals(task.getAssignmentMode())) {
                throw new InvalidStateException("Only an open task that is not completed can be claimed.");
            }
            List<TaskAssigneeEntity> covering = taskAssigneeRepository.covering(id, caller);
            if (covering.isEmpty()) {
                throw new PermissionDeniedException("The task is not assigned to the caller.");
            }
            List<TaskAssigneeEntity> open = covering.stream()
                    .filter(row -> row.getClaimedByUserId() == null && "pending".equals(row.getStatus())).toList();
            if (open.isEmpty() && covering.stream().noneMatch(row -> caller.equals(row.getClaimedByUserId()))) {
                throw new InvalidStateException("The task has been claimed already.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            for (TaskAssigneeEntity row : open) {
                if (taskAssigneeRepository.claim(row.getId(), row.getRowVersion(), caller, now,
                        Actor.current().orElse(null)) == 0) {
                    throw new PreconditionFailedException("The assignment changed since it was read.");
                }
                audit.recordTransition(TASK_ASSIGNEE_AGGREGATE, row.getId(), "claim", List.of("claimedByUserId"), null);
                outbox.publish(TASK_ASSIGNEE_AGGREGATE + ".claim", TASK_ASSIGNEE_AGGREGATE, row.getId(),
                        CommsEvents.payload(row.getId()));
            }
            return new TaskAssigneeList(taskAssigneeRepository.covering(id, caller).stream()
                    .filter(row -> caller.equals(row.getClaimedByUserId())).map(taskAssigneeMapper::toDto).toList());
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private TaskEntity loadVisible(DataScope scope, UUID id) {
        TaskEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(TaskEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(TaskEntity entity, TaskUpdate changes) {
        Patches patch = new Patches();
        patch.apply("institutionId", changes.getInstitutionId(), entity::setInstitutionId);
        patch.apply("academicYearId", changes.getAcademicYearId(), entity::setAcademicYearId);
        patch.apply("ownerUserId", changes.getOwnerUserId(), entity::setOwnerUserId);
        patch.apply("title", changes.getTitle(), entity::setTitle);
        patch.apply("taskCategory", changes.getTaskCategory(), v -> entity.setTaskCategory(v == null ? null : v.getValue()));
        patch.apply("dueAt", changes.getDueAt(), entity::setDueAt);
        patch.apply("reminderOption", changes.getReminderOption(),
                v -> entity.setReminderOption(v == null ? null : v.getValue()));
        patch.apply("reminderAt", changes.getReminderAt(), entity::setReminderAt);
        patch.apply("checklist", changes.getChecklist(), entity::setChecklist);
        patch.apply("priority", changes.getPriority(), v -> entity.setPriority(v == null ? null : v.getValue()));
        patch.apply("assignmentMode", changes.getAssignmentMode(),
                v -> entity.setAssignmentMode(v == null ? null : v.getValue()));
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(TaskEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getOwnerUserId() == null) {
            errors.add(new FieldError("ownerUserId", "must not be null", "NotNull"));
        }
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        if (entity.getTaskCategory() == null) {
            errors.add(new FieldError("taskCategory", "must not be null", "NotNull"));
        }
        if (entity.getDueAt() == null) {
            errors.add(new FieldError("dueAt", "must not be null", "NotNull"));
        }
        if (entity.getReminderOption() == null) {
            errors.add(new FieldError("reminderOption", "must not be null", "NotNull"));
        }
        if (entity.getChecklist() == null) {
            errors.add(new FieldError("checklist", "must not be null", "NotNull"));
        }
        if (!creating && entity.getPriority() == null) {
            errors.add(new FieldError("priority", "must not be null", "NotNull"));
        }
        if (!creating && entity.getAssignmentMode() == null) {
            errors.add(new FieldError("assignmentMode", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(TaskEntity entity) {

    }

    // ---- TaskAssignee ----

    public TaskAssignee getTaskAssignee(UUID taskId, UUID id) {
        return doInReadTransaction(() -> {
            return taskAssigneeMapper.toDto(loadVisibleTaskAssignee(DataScope.current(TASK_ASSIGNEE_PERMISSION_GET), taskId, id));
        });
    }

    public Page<TaskAssignee> listTaskAssignees(UUID taskId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, TaskAssigneeRepository.SORT_FIELDS, TaskAssigneeRepository.DEFAULT_ORDER,
                TaskAssigneeRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, TaskAssigneeRepository.FILTER_FIELDS)
                .and(TaskAssigneeRepository.ofParent(taskId))
                        .and(TaskAssigneeRepository.scope(DataScope.current(TASK_ASSIGNEE_PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + taskId, clock);
        return doInReadTransaction(() -> {
            Page<TaskAssigneeEntity> rows = taskAssigneeRepository.list(page, where);
            return rows.map(taskAssigneeMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public TaskAssignee createTaskAssignee(UUID taskId, TaskAssignee request, boolean validateOnly) {
        validator.validate(request);
        TaskAssigneeEntity entity = taskAssigneeMapper.toEntity(request);
        entity.setTaskId(taskId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(TASK_ASSIGNEE_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!taskAssigneeRepository.existsTaskId(entity.getTaskId())) {
                throw new ResourceNotFoundException("comms.task", entity.getTaskId());
            }
            if (!scope.canRead(institutionOfTaskAssignee(entity))) {
                throw new ResourceNotFoundException("comms.task", entity.getTaskId());
            }
            assertValidTaskAssignee(entity, true);
            scope.requireWritable(institutionOfTaskAssignee(entity));
            assertUniqueTaskAssignee(entity);
            if (validateOnly) {
                return taskAssigneeMapper.toDto(entity);
            }
            TaskAssigneeEntity stored = taskAssigneeRepository.insert(entity);
            audit.recordCreate(TASK_ASSIGNEE_AGGREGATE, stored.getId());
            outbox.publish(TASK_ASSIGNEE_AGGREGATE + CommsEvents.CREATED, TASK_ASSIGNEE_AGGREGATE, stored.getId(),
                    CommsEvents.payload(stored.getId()));
            return taskAssigneeMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public TaskAssignee updateTaskAssignee(UUID taskId, UUID id, String ifMatch, TaskAssigneeUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(TASK_ASSIGNEE_PERMISSION_UPDATE);
            TaskAssigneeEntity entity = loadVisibleTaskAssignee(scope, taskId, id);
            scope.requireWritable(institutionOfTaskAssignee(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyTaskAssignee(entity, changes);
            validator.validate(taskAssigneeMapper.toDto(entity));
            assertValidTaskAssignee(entity, false);
            scope.requireWritable(institutionOfTaskAssignee(entity));
            assertUniqueTaskAssignee(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return taskAssigneeMapper.toDto(entity);
            }
            if (taskAssigneeRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(TASK_ASSIGNEE_AGGREGATE, id, fields);
            outbox.publish(TASK_ASSIGNEE_AGGREGATE + CommsEvents.UPDATED, TASK_ASSIGNEE_AGGREGATE, id, CommsEvents.payload(id));
            return taskAssigneeMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteTaskAssignee(UUID taskId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(TASK_ASSIGNEE_PERMISSION_DELETE);
            TaskAssigneeEntity entity = loadVisibleTaskAssignee(scope, taskId, id);
            scope.requireWritable(institutionOfTaskAssignee(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (taskAssigneeRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(TASK_ASSIGNEE_AGGREGATE, id);
            outbox.publish(TASK_ASSIGNEE_AGGREGATE + CommsEvents.DELETED, TASK_ASSIGNEE_AGGREGATE, id, CommsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private TaskAssigneeEntity loadVisibleTaskAssignee(DataScope scope, UUID taskId, UUID id) {
        TaskAssigneeEntity entity = taskAssigneeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(TASK_ASSIGNEE_AGGREGATE, id));
        if (!scope.canRead(institutionOfTaskAssignee(entity)) || !taskId.equals(entity.getTaskId())) {
            throw new ResourceNotFoundException(TASK_ASSIGNEE_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the comms.task the row belongs to. */
    private Optional<UUID> institutionOfTaskAssignee(TaskAssigneeEntity entity) {
        return taskAssigneeRepository.institutionOf(entity.getTaskId());
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyTaskAssignee(TaskAssigneeEntity entity, TaskAssigneeUpdate changes) {
        Patches patch = new Patches();
        patch.apply("assigneeType", changes.getAssigneeType(), v -> entity.setAssigneeType(v == null ? null : v.getValue()));
        patch.apply("userId", changes.getUserId(), entity::setUserId);
        patch.apply("classSectionId", changes.getClassSectionId(), entity::setClassSectionId);
        patch.apply("groupId", changes.getGroupId(), entity::setGroupId);
        patch.apply("claimedByUserId", changes.getClaimedByUserId(), entity::setClaimedByUserId);
        patch.apply("status", changes.getStatus(), v -> entity.setStatus(v == null ? null : v.getValue()));
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidTaskAssignee(TaskAssigneeEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getTaskId() == null) {
            errors.add(new FieldError("taskId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getAssigneeType() == null) {
            errors.add(new FieldError("assigneeType", "must not be null", "NotNull"));
        }
        if (!creating && entity.getStatus() == null) {
            errors.add(new FieldError("status", "must not be null", "NotNull"));
        }
        if (entity.getGroupId() != null && !taskAssigneeRepository.existsGroupId(entity.getGroupId())) {
            errors.add(new FieldError("groupId", "audience group does not exist", "NOT_FOUND"));
        }
        if (entity.getTaskId() != null && !taskAssigneeRepository.existsTaskId(entity.getTaskId())) {
            errors.add(new FieldError("taskId", "task does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueTaskAssignee(TaskAssigneeEntity entity) {

    }
}
