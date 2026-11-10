package com.mysms.comms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.business.TaskService;
import com.mysms.comms.api.TaskApi;
import com.mysms.comms.api.model.CompleteTaskRequest;
import com.mysms.comms.api.model.ReopenTaskRequest;
import com.mysms.comms.api.model.SnoozeTaskRequest;
import com.mysms.comms.api.model.Task;
import com.mysms.comms.api.model.TaskAssignee;
import com.mysms.comms.api.model.TaskAssigneeList;
import com.mysms.comms.api.model.TaskAssigneeUpdate;
import com.mysms.comms.api.model.TaskUpdate;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Task operations: implements the generated contract interface, so paths, parameters and payloads
 * cannot drift from api/openapi/comms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class TaskController implements TaskApi {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    public ResponseEntity<TaskAssigneeList> claimTask(UUID taskId, Object body, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Task> completeTask(UUID taskId, CompleteTaskRequest completeTaskRequest,
            UUID idempotencyKey) {
        Task changed = taskService.completeTask(taskId, completeTaskRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Task> createTask(Task task, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Task created = taskService.createTask(task, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/comms/tasks/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<TaskAssignee> createTaskAssignee(UUID taskId, TaskAssignee taskAssignee,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        TaskAssignee created = taskService.createTaskAssignee(taskId, taskAssignee, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/comms/tasks/" + taskId + "/assignees/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteTask(UUID taskId, String ifMatch) {
        taskService.deleteTask(taskId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteTaskAssignee(UUID taskId, UUID taskAssigneeId, String ifMatch) {
        taskService.deleteTaskAssignee(taskId, taskAssigneeId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Task> getTask(UUID taskId, String fields) {
        Task found = taskService.getTask(taskId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<TaskAssignee> getTaskAssignee(UUID taskId, UUID taskAssigneeId, String fields) {
        TaskAssignee found = taskService.getTaskAssignee(taskId, taskAssigneeId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<TaskAssignee>> listTaskAssignees(UUID taskId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<TaskAssignee> list =
                PagingUtil.pagingInfo(taskService.listTaskAssignees(taskId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Task>> listTasks(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Task> list = PagingUtil.pagingInfo(taskService.listTasks(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Task> reopenTask(UUID taskId, ReopenTaskRequest reopenTaskRequest,
            UUID idempotencyKey) {
        Task changed = taskService.reopenTask(taskId, reopenTaskRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Task> snoozeTask(UUID taskId, SnoozeTaskRequest snoozeTaskRequest,
            UUID idempotencyKey) {
        Task changed = taskService.snoozeTask(taskId, snoozeTaskRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Task> updateTask(UUID taskId, String ifMatch, TaskUpdate taskUpdate,
            String updateMask, Boolean validateOnly) {
        Task updated = taskService.updateTask(taskId, ifMatch, taskUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<TaskAssignee> updateTaskAssignee(UUID taskId, UUID taskAssigneeId,
            String ifMatch, TaskAssigneeUpdate taskAssigneeUpdate, String updateMask, Boolean validateOnly) {
        TaskAssignee updated =
                taskService.updateTaskAssignee(taskId, taskAssigneeId, ifMatch, taskAssigneeUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
