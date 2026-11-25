package com.mysms.privacy.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.privacy.api.model.Breach;
import com.mysms.privacy.api.model.BreachStepRequest;
import com.mysms.privacy.api.model.BreachUpdate;
import com.mysms.privacy.business.BreachService;
import com.mysms.privacy.v1.BreachServiceGrpc;
import com.mysms.privacy.v1.CloseBreachRequest;
import com.mysms.privacy.v1.CloseBreachResponse;
import com.mysms.privacy.v1.ContainBreachRequest;
import com.mysms.privacy.v1.ContainBreachResponse;
import com.mysms.privacy.v1.GetBreachRequest;
import com.mysms.privacy.v1.GetBreachResponse;
import com.mysms.privacy.v1.ListBreachesRequest;
import com.mysms.privacy.v1.ListBreachesResponse;
import com.mysms.privacy.v1.MarkBreachNotABreachRequest;
import com.mysms.privacy.v1.MarkBreachNotABreachResponse;
import com.mysms.privacy.v1.RecordBreachNotificationRequest;
import com.mysms.privacy.v1.RecordBreachNotificationResponse;
import com.mysms.privacy.v1.ReportBreachRequest;
import com.mysms.privacy.v1.ReportBreachResponse;
import com.mysms.privacy.v1.TriageBreachRequest;
import com.mysms.privacy.v1.TriageBreachResponse;
import com.mysms.privacy.v1.UpdateBreachRequest;
import com.mysms.privacy.v1.UpdateBreachResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of privacy.BreachService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge. A step's REST body is its request without the
 * path id and the idempotency key.
 */
@GrpcService
public class BreachResolver extends BreachServiceGrpc.BreachServiceImplBase {

    private static final String BREACH = "breach";
    private static final String PATH_ID = "breach_id";
    private static final String REQUEST_ID = "request_id";

    private final BreachService service;
    private final ProtoBridge bridge;

    public BreachResolver(BreachService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listBreaches(ListBreachesRequest request, StreamObserver<ListBreachesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Breach> rows = service.listBreaches(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListBreachesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getBreach(GetBreachRequest request, StreamObserver<GetBreachResponse> out) {
        Breach found = service.getBreach(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetBreachResponse.newBuilder(), BREACH, found).build());
    }

    @Override
    public void reportBreach(ReportBreachRequest request, StreamObserver<ReportBreachResponse> out) {
        Breach reported = service.reportBreach(bridge.body(request.getBreach(), Breach.class));
        ProtoBridge.send(out, bridge.reply(ReportBreachResponse.newBuilder(), BREACH, reported).build());
    }

    @Override
    public void updateBreach(UpdateBreachRequest request, StreamObserver<UpdateBreachResponse> out) {
        Breach updated = service.updateBreach(bridge.id(request.getBreach().getId(), "id"),
                bridge.requiredIfMatch(request.getBreach().getEtag()),
                bridge.changes(request.getBreach(), request.getUpdateMask(), BreachUpdate.class),
                bridge.updateMask(request.getUpdateMask()));
        ProtoBridge.send(out, bridge.reply(UpdateBreachResponse.newBuilder(), BREACH, updated).build());
    }

    @Override
    public void triageBreach(TriageBreachRequest request, StreamObserver<TriageBreachResponse> out) {
        Breach triaged = service.triageBreach(bridge.id(request.getBreachId(), "breachId"),
                bridge.body(request, BreachStepRequest.class, PATH_ID, REQUEST_ID));
        ProtoBridge.send(out, bridge.reply(TriageBreachResponse.newBuilder(), BREACH, triaged).build());
    }

    @Override
    public void containBreach(ContainBreachRequest request, StreamObserver<ContainBreachResponse> out) {
        Breach contained = service.containBreach(bridge.id(request.getBreachId(), "breachId"),
                bridge.body(request, BreachStepRequest.class, PATH_ID, REQUEST_ID));
        ProtoBridge.send(out, bridge.reply(ContainBreachResponse.newBuilder(), BREACH, contained).build());
    }

    @Override
    public void recordBreachNotification(RecordBreachNotificationRequest request,
            StreamObserver<RecordBreachNotificationResponse> out) {
        Breach recorded = service.recordBreachNotification(bridge.id(request.getBreachId(), "breachId"),
                bridge.body(request, com.mysms.privacy.api.model.RecordBreachNotificationRequest.class, PATH_ID,
                        REQUEST_ID));
        ProtoBridge.send(out, bridge.reply(RecordBreachNotificationResponse.newBuilder(), BREACH, recorded).build());
    }

    @Override
    public void closeBreach(CloseBreachRequest request, StreamObserver<CloseBreachResponse> out) {
        Breach closed = service.closeBreach(bridge.id(request.getBreachId(), "breachId"),
                bridge.body(request, BreachStepRequest.class, PATH_ID, REQUEST_ID));
        ProtoBridge.send(out, bridge.reply(CloseBreachResponse.newBuilder(), BREACH, closed).build());
    }

    @Override
    public void markBreachNotABreach(MarkBreachNotABreachRequest request,
            StreamObserver<MarkBreachNotABreachResponse> out) {
        Breach marked = service.markBreachNotABreach(bridge.id(request.getBreachId(), "breachId"),
                bridge.body(request, com.mysms.privacy.api.model.MarkBreachNotABreachRequest.class, PATH_ID,
                        REQUEST_ID));
        ProtoBridge.send(out, bridge.reply(MarkBreachNotABreachResponse.newBuilder(), BREACH, marked).build());
    }
}
