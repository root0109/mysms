package com.mysms.students.careers.grpc;

import com.mysms.students.careers.api.model.CandidatePreference;
import com.mysms.students.careers.api.model.CandidatePreferredFunction;
import com.mysms.students.careers.api.model.CandidatePreferredIndustry;
import com.mysms.students.careers.business.CandidatePreferenceService;
import com.mysms.careers.v1.CandidatePreferenceServiceGrpc;
import com.mysms.careers.v1.CandidatePreferenceUpdate;
import com.mysms.careers.v1.CandidatePreferredFunctionUpdate;
import com.mysms.careers.v1.CandidatePreferredIndustryUpdate;
import com.mysms.careers.v1.CreateCandidatePreferenceRequest;
import com.mysms.careers.v1.CreateCandidatePreferenceResponse;
import com.mysms.careers.v1.CreateCandidatePreferredFunctionRequest;
import com.mysms.careers.v1.CreateCandidatePreferredFunctionResponse;
import com.mysms.careers.v1.CreateCandidatePreferredIndustryRequest;
import com.mysms.careers.v1.CreateCandidatePreferredIndustryResponse;
import com.mysms.careers.v1.DeleteCandidatePreferenceRequest;
import com.mysms.careers.v1.DeleteCandidatePreferenceResponse;
import com.mysms.careers.v1.DeleteCandidatePreferredFunctionRequest;
import com.mysms.careers.v1.DeleteCandidatePreferredFunctionResponse;
import com.mysms.careers.v1.DeleteCandidatePreferredIndustryRequest;
import com.mysms.careers.v1.DeleteCandidatePreferredIndustryResponse;
import com.mysms.careers.v1.GetCandidatePreferenceRequest;
import com.mysms.careers.v1.GetCandidatePreferenceResponse;
import com.mysms.careers.v1.GetCandidatePreferredFunctionRequest;
import com.mysms.careers.v1.GetCandidatePreferredFunctionResponse;
import com.mysms.careers.v1.GetCandidatePreferredIndustryRequest;
import com.mysms.careers.v1.GetCandidatePreferredIndustryResponse;
import com.mysms.careers.v1.ListCandidatePreferencesRequest;
import com.mysms.careers.v1.ListCandidatePreferencesResponse;
import com.mysms.careers.v1.ListCandidatePreferredFunctionsRequest;
import com.mysms.careers.v1.ListCandidatePreferredFunctionsResponse;
import com.mysms.careers.v1.ListCandidatePreferredIndustriesRequest;
import com.mysms.careers.v1.ListCandidatePreferredIndustriesResponse;
import com.mysms.careers.v1.UpdateCandidatePreferenceResponse;
import com.mysms.careers.v1.UpdateCandidatePreferredFunctionResponse;
import com.mysms.careers.v1.UpdateCandidatePreferredIndustryResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of careers.CandidatePreferenceService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class CandidatePreferenceResolver extends CandidatePreferenceServiceGrpc.CandidatePreferenceServiceImplBase {

    private final CandidatePreferenceService service;
    private final ProtoBridge bridge;

    public CandidatePreferenceResolver(CandidatePreferenceService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listCandidatePreferences(ListCandidatePreferencesRequest request,
            StreamObserver<ListCandidatePreferencesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CandidatePreference> rows = service.listCandidatePreferences(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCandidatePreferencesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCandidatePreference(GetCandidatePreferenceRequest request,
            StreamObserver<GetCandidatePreferenceResponse> out) {
        CandidatePreference found = service.getCandidatePreference(
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCandidatePreferenceResponse.newBuilder(), "candidate_preference", found).build());
    }

    @Override
    public void createCandidatePreference(CreateCandidatePreferenceRequest request,
            StreamObserver<CreateCandidatePreferenceResponse> out) {
        CandidatePreference created = service.createCandidatePreference(
                bridge.body(request.getCandidatePreference(), CandidatePreference.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCandidatePreferenceResponse.newBuilder(), "candidate_preference",
                created).build());
    }

    @Override
    public void updateCandidatePreference(CandidatePreferenceUpdate request,
            StreamObserver<UpdateCandidatePreferenceResponse> out) {
        CandidatePreference updated = service.updateCandidatePreference(
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                CandidatePreferenceProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCandidatePreferenceResponse.newBuilder(), "candidate_preference",
                updated).build());
    }

    @Override
    public void deleteCandidatePreference(DeleteCandidatePreferenceRequest request,
            StreamObserver<DeleteCandidatePreferenceResponse> out) {
        service.deleteCandidatePreference(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCandidatePreferenceResponse.getDefaultInstance());
    }

    @Override
    public void listCandidatePreferredFunctions(ListCandidatePreferredFunctionsRequest request,
            StreamObserver<ListCandidatePreferredFunctionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CandidatePreferredFunction> rows = service.listCandidatePreferredFunctions(
                bridge.id(request.getCandidatePreferenceId(), "candidatePreferenceId"),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCandidatePreferredFunctionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCandidatePreferredFunction(GetCandidatePreferredFunctionRequest request,
            StreamObserver<GetCandidatePreferredFunctionResponse> out) {
        CandidatePreferredFunction found = service.getCandidatePreferredFunction(
                bridge.id(request.getCandidatePreferenceId(), "candidatePreferenceId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCandidatePreferredFunctionResponse.newBuilder(), "candidate_preferred_function",
                found).build());
    }

    @Override
    public void createCandidatePreferredFunction(CreateCandidatePreferredFunctionRequest request,
            StreamObserver<CreateCandidatePreferredFunctionResponse> out) {
        CandidatePreferredFunction created = service.createCandidatePreferredFunction(
                bridge.id(request.getCandidatePreferenceId(), "candidatePreferenceId"),
                bridge.body(request.getCandidatePreferredFunction(), CandidatePreferredFunction.class),
                        request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCandidatePreferredFunctionResponse.newBuilder(), "candidate_preferred_function",
                created).build());
    }

    @Override
    public void updateCandidatePreferredFunction(CandidatePreferredFunctionUpdate request,
            StreamObserver<UpdateCandidatePreferredFunctionResponse> out) {
        CandidatePreferredFunction updated = service.updateCandidatePreferredFunction(
                bridge.id(request.getCandidatePreferenceId(), "candidatePreferenceId"),
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                CandidatePreferredFunctionProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCandidatePreferredFunctionResponse.newBuilder(), "candidate_preferred_function",
                updated).build());
    }

    @Override
    public void deleteCandidatePreferredFunction(DeleteCandidatePreferredFunctionRequest request,
            StreamObserver<DeleteCandidatePreferredFunctionResponse> out) {
        service.deleteCandidatePreferredFunction(
                bridge.id(request.getCandidatePreferenceId(), "candidatePreferenceId"),
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCandidatePreferredFunctionResponse.getDefaultInstance());
    }

    @Override
    public void listCandidatePreferredIndustries(ListCandidatePreferredIndustriesRequest request,
            StreamObserver<ListCandidatePreferredIndustriesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CandidatePreferredIndustry> rows = service.listCandidatePreferredIndustrys(
                bridge.id(request.getCandidatePreferenceId(), "candidatePreferenceId"),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCandidatePreferredIndustriesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCandidatePreferredIndustry(GetCandidatePreferredIndustryRequest request,
            StreamObserver<GetCandidatePreferredIndustryResponse> out) {
        CandidatePreferredIndustry found = service.getCandidatePreferredIndustry(
                bridge.id(request.getCandidatePreferenceId(), "candidatePreferenceId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCandidatePreferredIndustryResponse.newBuilder(), "candidate_preferred_industry",
                found).build());
    }

    @Override
    public void createCandidatePreferredIndustry(CreateCandidatePreferredIndustryRequest request,
            StreamObserver<CreateCandidatePreferredIndustryResponse> out) {
        CandidatePreferredIndustry created = service.createCandidatePreferredIndustry(
                bridge.id(request.getCandidatePreferenceId(), "candidatePreferenceId"),
                bridge.body(request.getCandidatePreferredIndustry(), CandidatePreferredIndustry.class),
                        request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCandidatePreferredIndustryResponse.newBuilder(), "candidate_preferred_industry",
                created).build());
    }

    @Override
    public void updateCandidatePreferredIndustry(CandidatePreferredIndustryUpdate request,
            StreamObserver<UpdateCandidatePreferredIndustryResponse> out) {
        CandidatePreferredIndustry updated = service.updateCandidatePreferredIndustry(
                bridge.id(request.getCandidatePreferenceId(), "candidatePreferenceId"),
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                CandidatePreferredIndustryProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCandidatePreferredIndustryResponse.newBuilder(), "candidate_preferred_industry",
                updated).build());
    }

    @Override
    public void deleteCandidatePreferredIndustry(DeleteCandidatePreferredIndustryRequest request,
            StreamObserver<DeleteCandidatePreferredIndustryResponse> out) {
        service.deleteCandidatePreferredIndustry(
                bridge.id(request.getCandidatePreferenceId(), "candidatePreferenceId"),
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCandidatePreferredIndustryResponse.getDefaultInstance());
    }
}
