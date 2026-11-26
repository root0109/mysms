package com.mysms.academics.grpc;

import com.mysms.academics.api.model.Section;
import com.mysms.academics.api.model.SectionUpdate;
import com.mysms.academics.business.SectionService;
import com.mysms.academics.v1.CreateSectionRequest;
import com.mysms.academics.v1.CreateSectionResponse;
import com.mysms.academics.v1.DeleteSectionRequest;
import com.mysms.academics.v1.DeleteSectionResponse;
import com.mysms.academics.v1.GetSectionRequest;
import com.mysms.academics.v1.GetSectionResponse;
import com.mysms.academics.v1.ListSectionsRequest;
import com.mysms.academics.v1.ListSectionsResponse;
import com.mysms.academics.v1.SectionServiceGrpc;
import com.mysms.academics.v1.UpdateSectionRequest;
import com.mysms.academics.v1.UpdateSectionResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of academics.SectionService: delegates every RPC to the business service REST uses, so the rules exist
 * once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class SectionResolver extends SectionServiceGrpc.SectionServiceImplBase {

    private final SectionService service;
    private final ProtoBridge bridge;

    public SectionResolver(SectionService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listSections(ListSectionsRequest request, StreamObserver<ListSectionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Section> rows = service.listSections(page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()),
                page.sort());
        ProtoBridge.send(out, bridge.page(ListSectionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getSection(GetSectionRequest request, StreamObserver<GetSectionResponse> out) {
        Section found = service.getSection(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetSectionResponse.newBuilder(), "section", found).build());
    }

    @Override
    public void createSection(CreateSectionRequest request, StreamObserver<CreateSectionResponse> out) {
        Section created = service.createSection(bridge.body(request.getSection(), Section.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateSectionResponse.newBuilder(), "section", created).build());
    }

    @Override
    public void updateSection(UpdateSectionRequest request, StreamObserver<UpdateSectionResponse> out) {
        Section updated = service.updateSection(bridge.id(request.getSection().getId(), "id"),
                bridge.requiredIfMatch(request.getSection().getEtag()),
                bridge.changes(request.getSection(), request.getUpdateMask(), SectionUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateSectionResponse.newBuilder(), "section", updated).build());
    }

    @Override
    public void deleteSection(DeleteSectionRequest request, StreamObserver<DeleteSectionResponse> out) {
        service.deleteSection(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteSectionResponse.getDefaultInstance());
    }
}
