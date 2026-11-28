package com.mysms.learning.lms.grpc;

import com.mysms.learning.lms.business.EbookConversionService;
import com.mysms.learning.lms.business.EbookWorkflowService;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.Ebook;
import com.mysms.learning.lms.api.model.RelatedMaterial;
import com.mysms.learning.lms.business.EbookService;
import com.mysms.lms.v1.ConvertEbookRequest;
import com.mysms.lms.v1.ConvertEbookResponse;
import com.mysms.lms.v1.CreateEbookRequest;
import com.mysms.lms.v1.CreateEbookResponse;
import com.mysms.lms.v1.CreateRelatedMaterialRequest;
import com.mysms.lms.v1.CreateRelatedMaterialResponse;
import com.mysms.lms.v1.DeleteEbookRequest;
import com.mysms.lms.v1.DeleteEbookResponse;
import com.mysms.lms.v1.DeleteRelatedMaterialRequest;
import com.mysms.lms.v1.DeleteRelatedMaterialResponse;
import com.mysms.lms.v1.EbookServiceGrpc;
import com.mysms.lms.v1.EbookUpdate;
import com.mysms.lms.v1.GetEbookRequest;
import com.mysms.lms.v1.GetEbookResponse;
import com.mysms.lms.v1.GetMyRelatedMaterialsRequest;
import com.mysms.lms.v1.GetMyRelatedMaterialsResponse;
import com.mysms.lms.v1.GetRelatedMaterialRankRequest;
import com.mysms.lms.v1.GetRelatedMaterialRankResponse;
import com.mysms.lms.v1.GetRelatedMaterialRequest;
import com.mysms.lms.v1.GetRelatedMaterialResponse;
import com.mysms.lms.v1.ListEbooksRequest;
import com.mysms.lms.v1.ListEbooksResponse;
import com.mysms.lms.v1.ListRelatedMaterialRanksRequest;
import com.mysms.lms.v1.ListRelatedMaterialRanksResponse;
import com.mysms.lms.v1.ListRelatedMaterialsRequest;
import com.mysms.lms.v1.ListRelatedMaterialsResponse;
import com.mysms.lms.v1.PublishEbookCurationRequest;
import com.mysms.lms.v1.PublishEbookCurationResponse;
import com.mysms.lms.v1.ReceiveEbookConversionRequest;
import com.mysms.lms.v1.ReceiveEbookConversionResponse;
import com.mysms.lms.v1.RelatedMaterialUpdate;
import com.mysms.lms.v1.UpdateEbookResponse;
import com.mysms.lms.v1.UpdateRelatedMaterialResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.EbookService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class EbookResolver extends EbookServiceGrpc.EbookServiceImplBase {

    private final EbookService service;
    private final ProtoBridge bridge;
    private final EbookWorkflowService workflows;
    private final EbookConversionService conversions;

    public EbookResolver(EbookService service, ProtoBridge bridge, EbookWorkflowService workflows,
            EbookConversionService conversions) {
        this.workflows = workflows;
        this.service = service;
        this.bridge = bridge;
        this.conversions = conversions;
    }

    @Override
    public void listEbooks(ListEbooksRequest request, StreamObserver<ListEbooksResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Ebook> rows = service.listEbooks(page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()),
                page.sort());
        ProtoBridge.send(out, bridge.page(ListEbooksResponse.newBuilder(), rows).build());
    }

    @Override
    public void getEbook(GetEbookRequest request, StreamObserver<GetEbookResponse> out) {
        Ebook found = service.getEbook(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetEbookResponse.newBuilder(), "ebook", found).build());
    }

    @Override
    public void createEbook(CreateEbookRequest request, StreamObserver<CreateEbookResponse> out) {
        Ebook created = service.createEbook(bridge.body(request.getEbook(), Ebook.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateEbookResponse.newBuilder(), "ebook", created).build());
    }

    @Override
    public void updateEbook(EbookUpdate request, StreamObserver<UpdateEbookResponse> out) {
        Ebook updated = service.updateEbook(bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                EbookProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateEbookResponse.newBuilder(), "ebook", updated).build());
    }

    @Override
    public void deleteEbook(DeleteEbookRequest request, StreamObserver<DeleteEbookResponse> out) {
        service.deleteEbook(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteEbookResponse.getDefaultInstance());
    }

    @Override
    public void listRelatedMaterials(ListRelatedMaterialsRequest request, StreamObserver<ListRelatedMaterialsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<RelatedMaterial> rows = service.listRelatedMaterials(bridge.id(request.getEbookId(), "ebookId"), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListRelatedMaterialsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getRelatedMaterial(GetRelatedMaterialRequest request, StreamObserver<GetRelatedMaterialResponse> out) {
        RelatedMaterial found = service.getRelatedMaterial(bridge.id(request.getEbookId(), "ebookId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetRelatedMaterialResponse.newBuilder(), "related_material", found).build());
    }

    @Override
    public void createRelatedMaterial(CreateRelatedMaterialRequest request, StreamObserver<CreateRelatedMaterialResponse> out) {
        RelatedMaterial created = service.createRelatedMaterial(bridge.id(request.getEbookId(), "ebookId"),
                bridge.body(request.getRelatedMaterial(), RelatedMaterial.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateRelatedMaterialResponse.newBuilder(), "related_material", created).build());
    }

    @Override
    public void updateRelatedMaterial(RelatedMaterialUpdate request, StreamObserver<UpdateRelatedMaterialResponse> out) {
        RelatedMaterial updated = service.updateRelatedMaterial(bridge.id(request.getEbookId(), "ebookId"),
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                RelatedMaterialProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateRelatedMaterialResponse.newBuilder(), "related_material", updated).build());
    }

    @Override
    public void deleteRelatedMaterial(DeleteRelatedMaterialRequest request, StreamObserver<DeleteRelatedMaterialResponse> out) {
        service.deleteRelatedMaterial(bridge.id(request.getEbookId(), "ebookId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteRelatedMaterialResponse.getDefaultInstance());
    }

    @Override
    public void listRelatedMaterialRanks(ListRelatedMaterialRanksRequest request,
            StreamObserver<ListRelatedMaterialRanksResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        ProtoBridge.send(out, bridge.page(ListRelatedMaterialRanksResponse.newBuilder(),
                workflows.listRelatedMaterialRanks(bridge.id(request.getEbookId(), "ebookId"), page.page(),
                        page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort())).build());
    }

    @Override
    public void getRelatedMaterialRank(GetRelatedMaterialRankRequest request,
            StreamObserver<GetRelatedMaterialRankResponse> out) {
        ProtoBridge.send(out, bridge.reply(GetRelatedMaterialRankResponse.newBuilder(), "related_material_rank",
                workflows.getRelatedMaterialRank(bridge.id(request.getEbookId(), "ebookId"),
                        bridge.id(request.getId(), "id"))).build());
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void convertEbook(ConvertEbookRequest request, StreamObserver<ConvertEbookResponse> out) {
        Ebook converted = conversions.convertEbook(bridge.id(request.getEbookId(), "ebookId"));
        ProtoBridge.send(out, bridge.reply(ConvertEbookResponse.newBuilder(), "ebook", converted).build());
    }

    @Override
    public void receiveEbookConversion(ReceiveEbookConversionRequest request,
            StreamObserver<ReceiveEbookConversionResponse> out) {
        conversions.receiveEbookConversion(request.getProvider(),
                request.getHeadersOrDefault("x-signature", null), request.getHeadersOrDefault("x-timestamp", null),
                request.getRawBody().toByteArray());
        ProtoBridge.send(out, ReceiveEbookConversionResponse.getDefaultInstance());
    }

    @Override
    public void publishEbookCuration(PublishEbookCurationRequest request, StreamObserver<PublishEbookCurationResponse> out) {
        ProtoBridge.send(out, bridge.reply(PublishEbookCurationResponse.newBuilder(), "items",
                workflows.publishEbookCuration(bridge.id(request.getEbookId(), "ebookId"),
                        bridge.body(request, com.mysms.learning.lms.api.model.PublishEbookCurationRequest.class, "ebook_id",
                                "request_id")).getItems()).build());
    }

    @Override
    public void getMyRelatedMaterials(GetMyRelatedMaterialsRequest request, StreamObserver<GetMyRelatedMaterialsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        ProtoBridge.send(out, bridge.page(GetMyRelatedMaterialsResponse.newBuilder(),
                workflows.getMyRelatedMaterials(bridge.id(request.getEbookId(), "ebookId"),
                        request.hasCourseId() ? bridge.id(request.getCourseId(), "courseId") : null,
                        request.hasFileId() ? bridge.id(request.getFileId(), "fileId") : null, page.page(),
                        page.size(), page.cursor())).build());
    }
}
