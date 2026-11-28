package com.mysms.platform.org.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.platform.org.api.model.Department;
import com.mysms.platform.org.business.DepartmentService;
import com.mysms.org.v1.CreateDepartmentRequest;
import com.mysms.org.v1.CreateDepartmentResponse;
import com.mysms.org.v1.DeleteDepartmentRequest;
import com.mysms.org.v1.DeleteDepartmentResponse;
import com.mysms.org.v1.DepartmentServiceGrpc;
import com.mysms.org.v1.DepartmentUpdate;
import com.mysms.org.v1.GetDepartmentRequest;
import com.mysms.org.v1.GetDepartmentResponse;
import com.mysms.org.v1.ListDepartmentsRequest;
import com.mysms.org.v1.ListDepartmentsResponse;
import com.mysms.org.v1.UpdateDepartmentResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of org.DepartmentService: delegates every RPC to the business service REST uses, so the rules exist
 * once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class DepartmentResolver extends DepartmentServiceGrpc.DepartmentServiceImplBase {

    private final DepartmentService service;
    private final ProtoBridge bridge;

    public DepartmentResolver(DepartmentService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }


    @Override
    public void listDepartments(ListDepartmentsRequest request,
            StreamObserver<ListDepartmentsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Department> rows = service.listDepartments(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListDepartmentsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getDepartment(GetDepartmentRequest request, StreamObserver<GetDepartmentResponse> out) {
        Department found = service.getDepartment(
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetDepartmentResponse.newBuilder(), "department", found).build());
    }

    @Override
    public void createDepartment(CreateDepartmentRequest request, StreamObserver<CreateDepartmentResponse> out) {
        Department created = service.createDepartment(
                bridge.body(request.getDepartment(), Department.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateDepartmentResponse.newBuilder(), "department", created).build());
    }

    @Override
    public void updateDepartment(DepartmentUpdate request, StreamObserver<UpdateDepartmentResponse> out) {
        Department updated = service.updateDepartment(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                DepartmentProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateDepartmentResponse.newBuilder(), "department", updated).build());
    }

    @Override
    public void deleteDepartment(DeleteDepartmentRequest request, StreamObserver<DeleteDepartmentResponse> out) {
        service.deleteDepartment(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteDepartmentResponse.getDefaultInstance());
    }
}
