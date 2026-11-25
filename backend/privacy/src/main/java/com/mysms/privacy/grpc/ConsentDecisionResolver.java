package com.mysms.privacy.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.privacy.api.model.ConsentDecision;
import com.mysms.privacy.api.model.ConsentedSubject;
import com.mysms.privacy.api.model.PurposeStatus;
import com.mysms.privacy.business.ConsentDecisionService;
import com.mysms.privacy.v1.BatchCheckConsentRequest;
import com.mysms.privacy.v1.BatchCheckConsentResponse;
import com.mysms.privacy.v1.CheckConsentRequest;
import com.mysms.privacy.v1.CheckConsentResponse;
import com.mysms.privacy.v1.ConsentDecisionServiceGrpc;
import com.mysms.privacy.v1.GetPurposeStatusRequest;
import com.mysms.privacy.v1.GetPurposeStatusResponse;
import com.mysms.privacy.v1.ListSubjectsWithConsentRequest;
import com.mysms.privacy.v1.ListSubjectsWithConsentResponse;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.UUID;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of privacy.ConsentDecisionService, the service callers' path (privacy-client): delegates to the same
 * business service as REST. Authentication, permission and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ConsentDecisionResolver extends ConsentDecisionServiceGrpc.ConsentDecisionServiceImplBase {

    private final ConsentDecisionService service;
    private final ProtoBridge bridge;

    public ConsentDecisionResolver(ConsentDecisionService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void checkConsent(CheckConsentRequest request, StreamObserver<CheckConsentResponse> out) {
        ConsentDecision decision = service.checkConsent(request.getActivityCode(),
                bridge.id(request.getSubjectPersonId(), "subjectPersonId"), request.getScope());
        ProtoBridge.send(out, bridge.reply(CheckConsentResponse.newBuilder(), "decision", decision).build());
    }

    @Override
    public void batchCheckConsent(BatchCheckConsentRequest request, StreamObserver<BatchCheckConsentResponse> out) {
        com.mysms.privacy.api.model.BatchCheckConsentResponse answered = service.batchCheckConsent(bridge.body(request,
                com.mysms.privacy.api.model.BatchCheckConsentRequest.class));
        ProtoBridge.send(out, bridge.reply(BatchCheckConsentResponse.newBuilder(), "decisions",
                answered.getDecisions()).build());
    }

    @Override
    public void getPurposeStatus(GetPurposeStatusRequest request, StreamObserver<GetPurposeStatusResponse> out) {
        PurposeStatus status = service.getPurposeStatus(request.getActivityCode());
        ProtoBridge.send(out, bridge.reply(GetPurposeStatusResponse.newBuilder(), "purpose_status", status).build());
    }

    @Override
    public void listSubjectsWithConsent(ListSubjectsWithConsentRequest request,
            StreamObserver<ListSubjectsWithConsentResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        List<UUID> institutions = request.getInstitutionIdsList().stream()
                .map(id -> bridge.id(id, "institutionIds")).toList();
        Page<ConsentedSubject> rows = service.listSubjectsWithConsent(request.getActivityCode(), request.getScope(),
                institutions, page.page(), page.size(), page.cursor());
        ProtoBridge.send(out, bridge.page(ListSubjectsWithConsentResponse.newBuilder(), rows).build());
    }
}
