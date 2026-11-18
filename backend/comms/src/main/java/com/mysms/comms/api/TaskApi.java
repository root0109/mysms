// Owned API contract of comms.
package com.mysms.comms.api;

import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.api.model.CompleteTaskRequest;
import org.springframework.lang.Nullable;
import com.mysms.comms.api.model.ReopenTaskRequest;
import com.mysms.comms.api.model.SnoozeTaskRequest;
import com.mysms.comms.api.model.Task;
import com.mysms.comms.api.model.TaskAssignee;
import com.mysms.comms.api.model.TaskAssigneeList;
import com.mysms.comms.api.model.TaskAssigneeUpdate;
import com.mysms.comms.api.model.TaskUpdate;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Validated
public interface TaskApi {

    String PATH_CLAIM_TASK = "/v1/comms/tasks/{taskId}:claim";
    /**
     * POST /v1/comms/tasks/{taskId}:claim : Claim an open task
     * Claim an open task.
     *
     * @param taskId  (required)
     * @param body  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = TaskApi.PATH_CLAIM_TASK,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<TaskAssigneeList> claimTask(
         @PathVariable("taskId") UUID taskId,
         @Valid @RequestBody Object body,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_COMPLETE_TASK = "/v1/comms/tasks/{taskId}:complete";
    /**
     * POST /v1/comms/tasks/{taskId}:complete : Complete
     * Complete.
     *
     * @param taskId  (required)
     * @param completeTaskRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = TaskApi.PATH_COMPLETE_TASK,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Task> completeTask(
         @PathVariable("taskId") UUID taskId,
         @Valid @RequestBody CompleteTaskRequest completeTaskRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_CREATE_TASK = "/v1/comms/tasks";
    /**
     * POST /v1/comms/tasks : Create Task
     * Create a Task. Supports Idempotency-Key and validateOnly.
     *
     * @param task  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return validateOnly — every check ran and nothing was persisted; the would-be resource. (status code 200)
     *         or Created (status code 201)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = TaskApi.PATH_CREATE_TASK,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Task> createTask(
         @Valid @RequestBody Task task,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_CREATE_TASK_ASSIGNEE = "/v1/comms/tasks/{taskId}/assignees";
    /**
     * POST /v1/comms/tasks/{taskId}/assignees : Create TaskAssignee
     * Create a TaskAssignee. Supports Idempotency-Key and validateOnly.
     *
     * @param taskId Task id. (required)
     * @param taskAssignee  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return validateOnly — every check ran and nothing was persisted; the would-be resource. (status code 200)
     *         or Created (status code 201)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = TaskApi.PATH_CREATE_TASK_ASSIGNEE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<TaskAssignee> createTaskAssignee(
         @PathVariable("taskId") UUID taskId,
         @Valid @RequestBody TaskAssignee taskAssignee,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_DELETE_TASK = "/v1/comms/tasks/{taskId}";
    /**
     * DELETE /v1/comms/tasks/{taskId} : Delete Task
     * Soft delete (sets deleted_at).
     *
     * @param taskId Task id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @return Deleted (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = TaskApi.PATH_DELETE_TASK,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteTask(
         @PathVariable("taskId") UUID taskId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_DELETE_TASK_ASSIGNEE = "/v1/comms/tasks/{taskId}/assignees/{taskAssigneeId}";
    /**
     * DELETE /v1/comms/tasks/{taskId}/assignees/{taskAssigneeId} : Delete TaskAssignee
     * Soft delete (sets deleted_at).
     *
     * @param taskId Task id. (required)
     * @param taskAssigneeId TaskAssignee id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @return Deleted (status code 204)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.DELETE,
        value = TaskApi.PATH_DELETE_TASK_ASSIGNEE,
        produces = { "application/problem+json" }
    )
    ResponseEntity<Void> deleteTaskAssignee(
         @PathVariable("taskId") UUID taskId,
         @PathVariable("taskAssigneeId") UUID taskAssigneeId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch
    );


    String PATH_GET_TASK = "/v1/comms/tasks/{taskId}";
    /**
     * GET /v1/comms/tasks/{taskId} : Get Task
     * Get one Task by id. The ETag header carries the row version.
     *
     * @param taskId Task id. (required)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = TaskApi.PATH_GET_TASK,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<Task> getTask(
         @PathVariable("taskId") UUID taskId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_GET_TASK_ASSIGNEE = "/v1/comms/tasks/{taskId}/assignees/{taskAssigneeId}";
    /**
     * GET /v1/comms/tasks/{taskId}/assignees/{taskAssigneeId} : Get TaskAssignee
     * Get one TaskAssignee by id. The ETag header carries the row version.
     *
     * @param taskId Task id. (required)
     * @param taskAssigneeId TaskAssignee id. (required)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = TaskApi.PATH_GET_TASK_ASSIGNEE,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<TaskAssignee> getTaskAssignee(
         @PathVariable("taskId") UUID taskId,
         @PathVariable("taskAssigneeId") UUID taskAssigneeId,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_TASK_ASSIGNEES = "/v1/comms/tasks/{taskId}/assignees";
    /**
     * GET /v1/comms/tasks/{taskId}/assignees : List TaskAssignees
     * List TaskAssignees visible to the caller, page by page, with filtering and sorting.
     *
     * @param taskId Task id. (required)
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields, e.g. &#x60;status &#x3D; \&quot;active\&quot; AND academicYearId &#x3D; \&quot;…\&quot;&#x60;. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = TaskApi.PATH_LIST_TASK_ASSIGNEES,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<TaskAssignee>> listTaskAssignees(
         @PathVariable("taskId") UUID taskId,
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_LIST_TASKS = "/v1/comms/tasks";
    /**
     * GET /v1/comms/tasks : List Tasks
     * List Tasks visible to the caller, page by page, with filtering and sorting.
     *
     * @param page One-based page number (default 1). (optional, default to 1)
     * @param size Page size (1-500, default 50). (optional, default to 50)
     * @param cursor Opaque cursor carried by the next link. (optional)
     * @param filter Filter expression (AIP-160 subset) over the resource fields, e.g. &#x60;status &#x3D; \&quot;active\&quot; AND academicYearId &#x3D; \&quot;…\&quot;&#x60;. (optional)
     * @param sort Sort criteria `field,asc|desc`; repeat for several. (optional)
     * @param fields Comma-separated fields to return (partial response). (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.GET,
        value = TaskApi.PATH_LIST_TASKS,
        produces = { "application/json", "application/problem+json" }
    )
    ResponseEntity<PagingInfo<Task>> listTasks(
        @Min(value = 1)  @Valid @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
        @Min(value = 1) @Max(value = 500)  @Valid @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
         @Valid @RequestParam(value = "cursor", required = false) @Nullable String cursor,
        @Size(max = 2000)  @Valid @RequestParam(value = "filter", required = false) @Nullable String filter,
         @Valid @RequestParam(value = "sort", required = false) @Nullable List<String> sort,
         @Valid @RequestParam(value = "fields", required = false) @Nullable String fields
    );


    String PATH_REOPEN_TASK = "/v1/comms/tasks/{taskId}:reopen";
    /**
     * POST /v1/comms/tasks/{taskId}:reopen : Reopen
     * Reopen.
     *
     * @param taskId  (required)
     * @param reopenTaskRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = TaskApi.PATH_REOPEN_TASK,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Task> reopenTask(
         @PathVariable("taskId") UUID taskId,
         @Valid @RequestBody ReopenTaskRequest reopenTaskRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_SNOOZE_TASK = "/v1/comms/tasks/{taskId}:snooze";
    /**
     * POST /v1/comms/tasks/{taskId}:snooze : Snooze reminder
     * Snooze reminder.
     *
     * @param taskId  (required)
     * @param snoozeTaskRequest  (required)
     * @param idempotencyKey Client-generated UUID; repeating a request with the same key returns the original result for 24 h. (optional)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.POST,
        value = TaskApi.PATH_SNOOZE_TASK,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Task> snoozeTask(
         @PathVariable("taskId") UUID taskId,
         @Valid @RequestBody SnoozeTaskRequest snoozeTaskRequest,
         @RequestHeader(value = "Idempotency-Key", required = false) @Nullable UUID idempotencyKey
    );


    String PATH_UPDATE_TASK = "/v1/comms/tasks/{taskId}";
    /**
     * PATCH /v1/comms/tasks/{taskId} : Update Task
     * Partially update a Task (updateMask). Requires If-Match with the current ETag.
     *
     * @param taskId Task id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param taskUpdate  (required)
     * @param updateMask Comma-separated fields to update. If omitted, every field present in the body is updated. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = TaskApi.PATH_UPDATE_TASK,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<Task> updateTask(
         @PathVariable("taskId") UUID taskId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody TaskUpdate taskUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );


    String PATH_UPDATE_TASK_ASSIGNEE = "/v1/comms/tasks/{taskId}/assignees/{taskAssigneeId}";
    /**
     * PATCH /v1/comms/tasks/{taskId}/assignees/{taskAssigneeId} : Update TaskAssignee
     * Partially update a TaskAssignee (updateMask). Requires If-Match with the current ETag.
     *
     * @param taskId Task id. (required)
     * @param taskAssigneeId TaskAssignee id. (required)
     * @param ifMatch ETag from the last read; required for updates and deletes. (required)
     * @param taskAssigneeUpdate  (required)
     * @param updateMask Comma-separated fields to update. If omitted, every field present in the body is updated. (optional)
     * @param validateOnly Validate the request without persisting. (optional, default to false)
     * @return OK (status code 200)
     *         or Malformed request. (status code 400)
     *         or Missing or invalid credentials. (status code 401)
     *         or Authenticated but not permitted. (status code 403)
     *         or Resource not found (or not visible to the caller). (status code 404)
     *         or Conflict with the current state (duplicate, invalid state transition). (status code 409)
     *         or ETag precondition failed (stale row_version). (status code 412)
     *         or Validation failed; see errors[]. (status code 422)
     *         or Precondition required (If-Match missing or &#x60;*&#x60;; send the ETag of the last read). (status code 428)
     *         or Rate limit exceeded. (status code 429)
     *         or Unexpected server error. (status code 500)
     */
    @RequestMapping(
        method = RequestMethod.PATCH,
        value = TaskApi.PATH_UPDATE_TASK_ASSIGNEE,
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )
    ResponseEntity<TaskAssignee> updateTaskAssignee(
         @PathVariable("taskId") UUID taskId,
         @PathVariable("taskAssigneeId") UUID taskAssigneeId,
        @NotNull  @RequestHeader(value = "If-Match", required = true) String ifMatch,
         @Valid @RequestBody TaskAssigneeUpdate taskAssigneeUpdate,
         @Valid @RequestParam(value = "updateMask", required = false) @Nullable String updateMask,
         @Valid @RequestParam(value = "validateOnly", required = false, defaultValue = "false") Boolean validateOnly
    );

}
