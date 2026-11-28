package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.Evaluator;
import com.mysms.learning.lms.api.model.ExternalLink;
import com.mysms.learning.lms.api.model.EvaluatorAvailability;
import com.mysms.learning.lms.business.EvaluatorService;
import com.mysms.lms.v1.ConnectEvaluatorCalendarRequest;
import com.mysms.lms.v1.ConnectEvaluatorCalendarResponse;
import com.mysms.lms.v1.CreateEvaluatorAvailabilityRequest;
import com.mysms.lms.v1.CreateEvaluatorAvailabilityResponse;
import com.mysms.lms.v1.CreateEvaluatorRequest;
import com.mysms.lms.v1.CreateEvaluatorResponse;
import com.mysms.lms.v1.DeleteEvaluatorAvailabilityRequest;
import com.mysms.lms.v1.DeleteEvaluatorAvailabilityResponse;
import com.mysms.lms.v1.DeleteEvaluatorRequest;
import com.mysms.lms.v1.DeleteEvaluatorResponse;
import com.mysms.lms.v1.DisconnectEvaluatorCalendarRequest;
import com.mysms.lms.v1.DisconnectEvaluatorCalendarResponse;
import com.mysms.lms.v1.EvaluatorAvailabilityUpdate;
import com.mysms.lms.v1.EvaluatorServiceGrpc;
import com.mysms.lms.v1.EvaluatorUpdate;
import com.mysms.lms.v1.GetEvaluatorAvailabilityRequest;
import com.mysms.lms.v1.GetEvaluatorAvailabilityResponse;
import com.mysms.lms.v1.GetEvaluatorRequest;
import com.mysms.lms.v1.GetEvaluatorResponse;
import com.mysms.lms.v1.ListEvaluatorAvailabilitiesRequest;
import com.mysms.lms.v1.ListEvaluatorAvailabilitiesResponse;
import com.mysms.lms.v1.ListEvaluatorsRequest;
import com.mysms.lms.v1.ListEvaluatorsResponse;
import com.mysms.lms.v1.UpdateEvaluatorAvailabilityResponse;
import com.mysms.lms.v1.UpdateEvaluatorResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.EvaluatorService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge; operations not served yet answer UNIMPLEMENTED.
 */
@GrpcService
public class EvaluatorResolver extends EvaluatorServiceGrpc.EvaluatorServiceImplBase {

    private final EvaluatorService service;
    private final ProtoBridge bridge;

    public EvaluatorResolver(EvaluatorService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listEvaluators(ListEvaluatorsRequest request, StreamObserver<ListEvaluatorsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Evaluator> rows = service.listEvaluators(page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()),
                page.sort());
        ProtoBridge.send(out, bridge.page(ListEvaluatorsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getEvaluator(GetEvaluatorRequest request, StreamObserver<GetEvaluatorResponse> out) {
        Evaluator found = service.getEvaluator(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetEvaluatorResponse.newBuilder(), "evaluator", found).build());
    }

    @Override
    public void createEvaluator(CreateEvaluatorRequest request, StreamObserver<CreateEvaluatorResponse> out) {
        Evaluator created = service.createEvaluator(bridge.body(request.getEvaluator(), Evaluator.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateEvaluatorResponse.newBuilder(), "evaluator", created).build());
    }

    @Override
    public void updateEvaluator(EvaluatorUpdate request, StreamObserver<UpdateEvaluatorResponse> out) {
        Evaluator updated = service.updateEvaluator(bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                EvaluatorProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateEvaluatorResponse.newBuilder(), "evaluator", updated).build());
    }

    @Override
    public void deleteEvaluator(DeleteEvaluatorRequest request, StreamObserver<DeleteEvaluatorResponse> out) {
        service.deleteEvaluator(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteEvaluatorResponse.getDefaultInstance());
    }

    @Override
    public void listEvaluatorAvailabilities(ListEvaluatorAvailabilitiesRequest request,
            StreamObserver<ListEvaluatorAvailabilitiesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<EvaluatorAvailability> rows = service.listEvaluatorAvailabilitys(bridge.id(request.getEvaluatorId(), "evaluatorId"),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListEvaluatorAvailabilitiesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getEvaluatorAvailability(GetEvaluatorAvailabilityRequest request,
            StreamObserver<GetEvaluatorAvailabilityResponse> out) {
        EvaluatorAvailability found = service.getEvaluatorAvailability(bridge.id(request.getEvaluatorId(), "evaluatorId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out,
                bridge.reply(GetEvaluatorAvailabilityResponse.newBuilder(), "evaluator_availability", found).build());
    }

    @Override
    public void createEvaluatorAvailability(CreateEvaluatorAvailabilityRequest request,
            StreamObserver<CreateEvaluatorAvailabilityResponse> out) {
        EvaluatorAvailability created = service.createEvaluatorAvailability(bridge.id(request.getEvaluatorId(), "evaluatorId"),
                bridge.body(request.getEvaluatorAvailability(), EvaluatorAvailability.class), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(CreateEvaluatorAvailabilityResponse.newBuilder(), "evaluator_availability", created).build());
    }

    @Override
    public void updateEvaluatorAvailability(EvaluatorAvailabilityUpdate request,
            StreamObserver<UpdateEvaluatorAvailabilityResponse> out) {
        EvaluatorAvailability updated = service.updateEvaluatorAvailability(bridge.id(request.getEvaluatorId(), "evaluatorId"),
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                EvaluatorAvailabilityProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(UpdateEvaluatorAvailabilityResponse.newBuilder(), "evaluator_availability", updated).build());
    }

    @Override
    public void deleteEvaluatorAvailability(DeleteEvaluatorAvailabilityRequest request,
            StreamObserver<DeleteEvaluatorAvailabilityResponse> out) {
        service.deleteEvaluatorAvailability(bridge.id(request.getEvaluatorId(), "evaluatorId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteEvaluatorAvailabilityResponse.getDefaultInstance());
    }

    @Override
    public void connectEvaluatorCalendar(ConnectEvaluatorCalendarRequest request,
            StreamObserver<ConnectEvaluatorCalendarResponse> out) {
        ExternalLink link = service.connectEvaluatorCalendar(bridge.id(request.getEvaluatorId(), "evaluatorId"),
                bridge.body(request, com.mysms.learning.lms.api.model.ConnectEvaluatorCalendarRequest.class, "evaluator_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(ConnectEvaluatorCalendarResponse.newBuilder(), "result", link).build());
    }

    @Override
    public void disconnectEvaluatorCalendar(DisconnectEvaluatorCalendarRequest request,
            StreamObserver<DisconnectEvaluatorCalendarResponse> out) {
        Evaluator unlinked = service.disconnectEvaluatorCalendar(bridge.id(request.getEvaluatorId(), "evaluatorId"));
        ProtoBridge.send(out, bridge.reply(DisconnectEvaluatorCalendarResponse.newBuilder(), "evaluator", unlinked)
                .build());
    }
}
