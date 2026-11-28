package com.mysms.academics.assessment.grpc;

import com.mysms.academics.assessment.api.model.Operation;
import com.mysms.academics.assessment.api.model.ResultTemplate;
import com.mysms.academics.assessment.api.model.ResultTemplateBand;
import com.mysms.academics.assessment.business.ResultTemplateService;
import com.mysms.academics.assessment.business.ResultsGenerationService;
import com.mysms.assessment.v1.CreateResultTemplateBandRequest;
import com.mysms.assessment.v1.CreateResultTemplateBandResponse;
import com.mysms.assessment.v1.CreateResultTemplateRequest;
import com.mysms.assessment.v1.CreateResultTemplateResponse;
import com.mysms.assessment.v1.DeleteResultTemplateBandRequest;
import com.mysms.assessment.v1.DeleteResultTemplateBandResponse;
import com.mysms.assessment.v1.DeleteResultTemplateRequest;
import com.mysms.assessment.v1.DeleteResultTemplateResponse;
import com.mysms.assessment.v1.GenerateResultsRequest;
import com.mysms.assessment.v1.GenerateResultsResponse;
import com.mysms.assessment.v1.GetResultTemplateBandRequest;
import com.mysms.assessment.v1.GetResultTemplateBandResponse;
import com.mysms.assessment.v1.GetResultTemplateRequest;
import com.mysms.assessment.v1.GetResultTemplateResponse;
import com.mysms.assessment.v1.ListResultTemplateBandsRequest;
import com.mysms.assessment.v1.ListResultTemplateBandsResponse;
import com.mysms.assessment.v1.ListResultTemplatesRequest;
import com.mysms.assessment.v1.ListResultTemplatesResponse;
import com.mysms.assessment.v1.ResultTemplateBandUpdate;
import com.mysms.assessment.v1.ResultTemplateServiceGrpc;
import com.mysms.assessment.v1.ResultTemplateUpdate;
import com.mysms.assessment.v1.UpdateResultTemplateBandResponse;
import com.mysms.assessment.v1.UpdateResultTemplateResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of assessment.ResultTemplateService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ResultTemplateResolver extends ResultTemplateServiceGrpc.ResultTemplateServiceImplBase {

    private final ResultTemplateService resultTemplateService;
    private final ResultsGenerationService resultsGenerationService;
    private final ProtoBridge bridge;

    public ResultTemplateResolver(ResultTemplateService resultTemplateService,
            ResultsGenerationService resultsGenerationService,
            ProtoBridge bridge) {
        this.resultTemplateService = resultTemplateService;
        this.resultsGenerationService = resultsGenerationService;
        this.bridge = bridge;
    }

    @Override
    public void listResultTemplates(ListResultTemplatesRequest request,
            StreamObserver<ListResultTemplatesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ResultTemplate> rows = resultTemplateService.listResultTemplates(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListResultTemplatesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getResultTemplate(GetResultTemplateRequest request, StreamObserver<GetResultTemplateResponse> out) {
        ResultTemplate found = resultTemplateService.getResultTemplate(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetResultTemplateResponse.newBuilder(), "result_template", found).build());
    }

    @Override
    public void createResultTemplate(CreateResultTemplateRequest request,
            StreamObserver<CreateResultTemplateResponse> out) {
        ResultTemplate created = resultTemplateService.createResultTemplate(
                bridge.body(request.getResultTemplate(), ResultTemplate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateResultTemplateResponse.newBuilder(),
                "result_template", created).build());
    }

    @Override
    public void updateResultTemplate(ResultTemplateUpdate request,
            StreamObserver<UpdateResultTemplateResponse> out) {
        ResultTemplate updated = resultTemplateService.updateResultTemplate(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                ResultTemplateProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateResultTemplateResponse.newBuilder(),
                "result_template", updated).build());
    }

    @Override
    public void deleteResultTemplate(DeleteResultTemplateRequest request,
            StreamObserver<DeleteResultTemplateResponse> out) {
        resultTemplateService.deleteResultTemplate(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteResultTemplateResponse.getDefaultInstance());
    }

    @Override
    public void listResultTemplateBands(ListResultTemplateBandsRequest request,
            StreamObserver<ListResultTemplateBandsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ResultTemplateBand> rows = resultTemplateService.listResultTemplateBands(
                bridge.id(request.getResultTemplateId(), "resultTemplateId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListResultTemplateBandsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getResultTemplateBand(GetResultTemplateBandRequest request,
            StreamObserver<GetResultTemplateBandResponse> out) {
        ResultTemplateBand found = resultTemplateService.getResultTemplateBand(
                bridge.id(request.getResultTemplateId(), "resultTemplateId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetResultTemplateBandResponse.newBuilder(),
                "result_template_band", found).build());
    }

    @Override
    public void createResultTemplateBand(CreateResultTemplateBandRequest request,
            StreamObserver<CreateResultTemplateBandResponse> out) {
        ResultTemplateBand created = resultTemplateService.createResultTemplateBand(
                bridge.id(request.getResultTemplateId(), "resultTemplateId"),
                bridge.body(request.getResultTemplateBand(), ResultTemplateBand.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateResultTemplateBandResponse.newBuilder(),
                "result_template_band", created).build());
    }

    @Override
    public void updateResultTemplateBand(ResultTemplateBandUpdate request,
            StreamObserver<UpdateResultTemplateBandResponse> out) {
        ResultTemplateBand updated = resultTemplateService.updateResultTemplateBand(
                bridge.id(request.getResultTemplateId(), "resultTemplateId"),
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                ResultTemplateBandProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateResultTemplateBandResponse.newBuilder(),
                "result_template_band", updated).build());
    }

    @Override
    public void deleteResultTemplateBand(DeleteResultTemplateBandRequest request,
            StreamObserver<DeleteResultTemplateBandResponse> out) {
        resultTemplateService.deleteResultTemplateBand(
                bridge.id(request.getResultTemplateId(), "resultTemplateId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteResultTemplateBandResponse.getDefaultInstance());
    }

    @Override
    public void generateResults(GenerateResultsRequest request, StreamObserver<GenerateResultsResponse> out) {
        Operation changed = resultsGenerationService.generateResults(
                bridge.id(request.getResultTemplateId(), "resultTemplateId"));
        ProtoBridge.send(out, bridge.reply(GenerateResultsResponse.newBuilder(), "result", changed).build());
    }
}
