package com.mysms.comms.grpc;

import com.mysms.comms.api.model.Task;
import com.mysms.comms.api.model.TaskAssignee;
import com.mysms.comms.api.model.TaskAssigneeUpdate;
import com.mysms.comms.api.model.TaskUpdate;
import com.mysms.comms.business.TaskService;
import com.mysms.comms.v1.ClaimTaskRequest;
import com.mysms.comms.v1.ClaimTaskResponse;
import com.mysms.comms.v1.CompleteTaskRequest;
import com.mysms.comms.v1.CompleteTaskResponse;
import com.mysms.comms.v1.CreateTaskAssigneeRequest;
import com.mysms.comms.v1.CreateTaskAssigneeResponse;
import com.mysms.comms.v1.CreateTaskRequest;
import com.mysms.comms.v1.CreateTaskResponse;
import com.mysms.comms.v1.DeleteTaskAssigneeRequest;
import com.mysms.comms.v1.DeleteTaskAssigneeResponse;
import com.mysms.comms.v1.DeleteTaskRequest;
import com.mysms.comms.v1.DeleteTaskResponse;
import com.mysms.comms.v1.GetTaskAssigneeRequest;
import com.mysms.comms.v1.GetTaskAssigneeResponse;
import com.mysms.comms.v1.GetTaskRequest;
import com.mysms.comms.v1.GetTaskResponse;
import com.mysms.comms.v1.ListTaskAssigneesRequest;
import com.mysms.comms.v1.ListTaskAssigneesResponse;
import com.mysms.comms.v1.ListTasksRequest;
import com.mysms.comms.v1.ListTasksResponse;
import com.mysms.comms.v1.ReopenTaskRequest;
import com.mysms.comms.v1.ReopenTaskResponse;
import com.mysms.comms.v1.SnoozeTaskRequest;
import com.mysms.comms.v1.SnoozeTaskResponse;
import com.mysms.comms.v1.TaskServiceGrpc;
import com.mysms.comms.v1.UpdateTaskAssigneeRequest;
import com.mysms.comms.v1.UpdateTaskAssigneeResponse;
import com.mysms.comms.v1.UpdateTaskRequest;
import com.mysms.comms.v1.UpdateTaskResponse;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of comms.TaskService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class TaskResolver extends TaskServiceGrpc.TaskServiceImplBase {

    private final TaskService service;
    private final ProtoBridge bridge;

    public TaskResolver(TaskService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listTasks(ListTasksRequest request, StreamObserver<ListTasksResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Task> rows = service.listTasks(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListTasksResponse.newBuilder(), rows).build());
    }

    @Override
    public void getTask(GetTaskRequest request, StreamObserver<GetTaskResponse> out) {
        Task found = service.getTask(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetTaskResponse.newBuilder(), "task", found).build());
    }

    @Override
    public void createTask(CreateTaskRequest request, StreamObserver<CreateTaskResponse> out) {
        Task created = service.createTask(bridge.body(request.getTask(), Task.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateTaskResponse.newBuilder(), "task", created).build());
    }

    @Override
    public void updateTask(UpdateTaskRequest request, StreamObserver<UpdateTaskResponse> out) {
        Task updated = service.updateTask(
                bridge.id(request.getTask().getId(), "id"), bridge.requiredIfMatch(request.getTask().getEtag()),
                bridge.changes(request.getTask(), request.getUpdateMask(), TaskUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateTaskResponse.newBuilder(), "task", updated).build());
    }

    @Override
    public void deleteTask(DeleteTaskRequest request, StreamObserver<DeleteTaskResponse> out) {
        service.deleteTask(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteTaskResponse.getDefaultInstance());
    }

    @Override
    public void listTaskAssignees(ListTaskAssigneesRequest request, StreamObserver<ListTaskAssigneesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<TaskAssignee> rows = service.listTaskAssignees(
                bridge.id(request.getTaskId(), "taskId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListTaskAssigneesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getTaskAssignee(GetTaskAssigneeRequest request, StreamObserver<GetTaskAssigneeResponse> out) {
        TaskAssignee found = service.getTaskAssignee(bridge.id(request.getTaskId(), "taskId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetTaskAssigneeResponse.newBuilder(), "task_assignee", found).build());
    }

    @Override
    public void createTaskAssignee(CreateTaskAssigneeRequest request, StreamObserver<CreateTaskAssigneeResponse> out) {
        TaskAssignee created = service.createTaskAssignee(
                bridge.id(request.getTaskId(), "taskId"), bridge.body(request.getTaskAssignee(), TaskAssignee.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateTaskAssigneeResponse.newBuilder(), "task_assignee", created).build());
    }

    @Override
    public void updateTaskAssignee(UpdateTaskAssigneeRequest request, StreamObserver<UpdateTaskAssigneeResponse> out) {
        TaskAssignee updated = service.updateTaskAssignee(
                bridge.id(request.getTaskId(), "taskId"), bridge.id(request.getTaskAssignee().getId(), "id"),
                bridge.requiredIfMatch(request.getTaskAssignee().getEtag()),
                bridge.changes(request.getTaskAssignee(), request.getUpdateMask(), TaskAssigneeUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateTaskAssigneeResponse.newBuilder(), "task_assignee", updated).build());
    }

    @Override
    public void deleteTaskAssignee(DeleteTaskAssigneeRequest request, StreamObserver<DeleteTaskAssigneeResponse> out) {
        service.deleteTaskAssignee(
                bridge.id(request.getTaskId(), "taskId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteTaskAssigneeResponse.getDefaultInstance());
    }

    /** The REST body (the workflow etag, its other fields) is the request without its path ids and idempotency key. */
    @Override
    public void completeTask(CompleteTaskRequest request, StreamObserver<CompleteTaskResponse> out) {
        Task changed = service.completeTask(
                bridge.id(request.getTaskId(), "taskId"),
                bridge.body(request, com.mysms.comms.api.model.CompleteTaskRequest.class, "task_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(CompleteTaskResponse.newBuilder(), "task", changed).build());
    }

    /** The REST body (the workflow etag, its other fields) is the request without its path ids and idempotency key. */
    @Override
    public void reopenTask(ReopenTaskRequest request, StreamObserver<ReopenTaskResponse> out) {
        Task changed = service.reopenTask(
                bridge.id(request.getTaskId(), "taskId"),
                bridge.body(request, com.mysms.comms.api.model.ReopenTaskRequest.class, "task_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(ReopenTaskResponse.newBuilder(), "task", changed).build());
    }

    /** The REST body (the workflow etag, its other fields) is the request without its path ids and idempotency key. */
    @Override
    public void snoozeTask(SnoozeTaskRequest request, StreamObserver<SnoozeTaskResponse> out) {
        Task changed = service.snoozeTask(
                bridge.id(request.getTaskId(), "taskId"),
                bridge.body(request, com.mysms.comms.api.model.SnoozeTaskRequest.class, "task_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(SnoozeTaskResponse.newBuilder(), "task", changed).build());
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void claimTask(ClaimTaskRequest request, StreamObserver<ClaimTaskResponse> out) {
        throw new NotImplementedException();
    }
}
