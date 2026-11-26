package com.mysms.academics.grpc;

import com.mysms.academics.api.model.ProgramLevel;
import com.mysms.academics.api.model.ProgramLevelUpdate;
import com.mysms.academics.business.ProgramLevelService;
import com.mysms.academics.v1.CreateProgramLevelRequest;
import com.mysms.academics.v1.CreateProgramLevelResponse;
import com.mysms.academics.v1.DeleteProgramLevelRequest;
import com.mysms.academics.v1.DeleteProgramLevelResponse;
import com.mysms.academics.v1.GetProgramLevelRequest;
import com.mysms.academics.v1.GetProgramLevelResponse;
import com.mysms.academics.v1.ListProgramLevelsRequest;
import com.mysms.academics.v1.ListProgramLevelsResponse;
import com.mysms.academics.v1.ProgramLevelServiceGrpc;
import com.mysms.academics.v1.UpdateProgramLevelRequest;
import com.mysms.academics.v1.UpdateProgramLevelResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of academics.ProgramLevelService: delegates every RPC to the business service REST uses, so the rules
 * exist once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ProgramLevelResolver extends ProgramLevelServiceGrpc.ProgramLevelServiceImplBase {

    private final ProgramLevelService service;
    private final ProtoBridge bridge;

    public ProgramLevelResolver(ProgramLevelService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listProgramLevels(ListProgramLevelsRequest request, StreamObserver<ListProgramLevelsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ProgramLevel> rows = service.listProgramLevels(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListProgramLevelsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getProgramLevel(GetProgramLevelRequest request, StreamObserver<GetProgramLevelResponse> out) {
        ProgramLevel found = service.getProgramLevel(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetProgramLevelResponse.newBuilder(), "program_level", found).build());
    }

    @Override
    public void createProgramLevel(CreateProgramLevelRequest request, StreamObserver<CreateProgramLevelResponse> out) {
        ProgramLevel created = service.createProgramLevel(bridge.body(request.getProgramLevel(), ProgramLevel.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateProgramLevelResponse.newBuilder(), "program_level", created).build());
    }

    @Override
    public void updateProgramLevel(UpdateProgramLevelRequest request, StreamObserver<UpdateProgramLevelResponse> out) {
        ProgramLevel updated = service.updateProgramLevel(bridge.id(request.getProgramLevel().getId(), "id"),
                bridge.requiredIfMatch(request.getProgramLevel().getEtag()),
                bridge.changes(request.getProgramLevel(), request.getUpdateMask(), ProgramLevelUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateProgramLevelResponse.newBuilder(), "program_level", updated).build());
    }

    @Override
    public void deleteProgramLevel(DeleteProgramLevelRequest request, StreamObserver<DeleteProgramLevelResponse> out) {
        service.deleteProgramLevel(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteProgramLevelResponse.getDefaultInstance());
    }
}
