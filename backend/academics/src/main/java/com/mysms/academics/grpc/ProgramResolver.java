package com.mysms.academics.grpc;

import com.mysms.academics.api.model.Program;
import com.mysms.academics.api.model.ProgramUpdate;
import com.mysms.academics.business.ProgramService;
import com.mysms.academics.v1.CreateProgramRequest;
import com.mysms.academics.v1.CreateProgramResponse;
import com.mysms.academics.v1.DeleteProgramRequest;
import com.mysms.academics.v1.DeleteProgramResponse;
import com.mysms.academics.v1.GetProgramRequest;
import com.mysms.academics.v1.GetProgramResponse;
import com.mysms.academics.v1.ListProgramsRequest;
import com.mysms.academics.v1.ListProgramsResponse;
import com.mysms.academics.v1.ProgramServiceGrpc;
import com.mysms.academics.v1.UpdateProgramRequest;
import com.mysms.academics.v1.UpdateProgramResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of academics.ProgramService: delegates every RPC to the business service REST uses, so the rules exist
 * once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ProgramResolver extends ProgramServiceGrpc.ProgramServiceImplBase {

    private final ProgramService service;
    private final ProtoBridge bridge;

    public ProgramResolver(ProgramService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listPrograms(ListProgramsRequest request, StreamObserver<ListProgramsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Program> rows = service.listPrograms(page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()),
                page.sort());
        ProtoBridge.send(out, bridge.page(ListProgramsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getProgram(GetProgramRequest request, StreamObserver<GetProgramResponse> out) {
        Program found = service.getProgram(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetProgramResponse.newBuilder(), "program", found).build());
    }

    @Override
    public void createProgram(CreateProgramRequest request, StreamObserver<CreateProgramResponse> out) {
        Program created = service.createProgram(bridge.body(request.getProgram(), Program.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateProgramResponse.newBuilder(), "program", created).build());
    }

    @Override
    public void updateProgram(UpdateProgramRequest request, StreamObserver<UpdateProgramResponse> out) {
        Program updated = service.updateProgram(bridge.id(request.getProgram().getId(), "id"),
                bridge.requiredIfMatch(request.getProgram().getEtag()),
                bridge.changes(request.getProgram(), request.getUpdateMask(), ProgramUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateProgramResponse.newBuilder(), "program", updated).build());
    }

    @Override
    public void deleteProgram(DeleteProgramRequest request, StreamObserver<DeleteProgramResponse> out) {
        service.deleteProgram(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteProgramResponse.getDefaultInstance());
    }
}
