package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.CertificateEvaluation;
import com.mysms.learning.lms.api.model.CertificateRequest;
import com.mysms.learning.lms.business.CertificateRequestService;
import com.mysms.lms.v1.CancelCertificateRequestRequest;
import com.mysms.lms.v1.CancelCertificateRequestResponse;
import com.mysms.lms.v1.CertificateRequestServiceGrpc;
import com.mysms.lms.v1.CertificateRequestUpdate;
import com.mysms.lms.v1.CreateCertificateRequestRequest;
import com.mysms.lms.v1.CreateCertificateRequestResponse;
import com.mysms.lms.v1.DeleteCertificateRequestRequest;
import com.mysms.lms.v1.DeleteCertificateRequestResponse;
import com.mysms.lms.v1.GetCertificateRequestRequest;
import com.mysms.lms.v1.GetCertificateRequestResponse;
import com.mysms.lms.v1.ListCertificateRequestsRequest;
import com.mysms.lms.v1.ListCertificateRequestsResponse;
import com.mysms.lms.v1.StartEvaluationRequest;
import com.mysms.lms.v1.StartEvaluationResponse;
import com.mysms.lms.v1.UpdateCertificateRequestResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.CertificateRequestService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class CertificateRequestResolver extends CertificateRequestServiceGrpc.CertificateRequestServiceImplBase {

    private final CertificateRequestService service;
    private final ProtoBridge bridge;

    public CertificateRequestResolver(CertificateRequestService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listCertificateRequests(ListCertificateRequestsRequest request,
            StreamObserver<ListCertificateRequestsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CertificateRequest> rows = service.listCertificateRequests(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCertificateRequestsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCertificateRequest(GetCertificateRequestRequest request, StreamObserver<GetCertificateRequestResponse> out) {
        CertificateRequest found = service.getCertificateRequest(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCertificateRequestResponse.newBuilder(), "certificate_request", found).build());
    }

    @Override
    public void createCertificateRequest(CreateCertificateRequestRequest request,
            StreamObserver<CreateCertificateRequestResponse> out) {
        CertificateRequest created = service.createCertificateRequest(
                bridge.body(request.getCertificateRequest(), CertificateRequest.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCertificateRequestResponse.newBuilder(), "certificate_request",
                created).build());
    }

    @Override
    public void updateCertificateRequest(CertificateRequestUpdate request,
            StreamObserver<UpdateCertificateRequestResponse> out) {
        CertificateRequest updated = service.updateCertificateRequest(bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                CertificateRequestProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCertificateRequestResponse.newBuilder(), "certificate_request",
                updated).build());
    }

    @Override
    public void deleteCertificateRequest(DeleteCertificateRequestRequest request,
            StreamObserver<DeleteCertificateRequestResponse> out) {
        service.deleteCertificateRequest(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCertificateRequestResponse.getDefaultInstance());
    }

    @Override
    public void cancelCertificateRequest(CancelCertificateRequestRequest request,
            StreamObserver<CancelCertificateRequestResponse> out) {
        CertificateRequest changed = service.cancelCertificateRequest(
                bridge.id(request.getCertificateRequestId(), "certificateRequestId"),
                request.hasEtag() ? request.getEtag() : null, request.hasReason() ? request.getReason() : null);
        ProtoBridge.send(out,
                bridge.reply(CancelCertificateRequestResponse.newBuilder(), "certificate_request", changed).build());
    }

    @Override
    public void startEvaluation(StartEvaluationRequest request, StreamObserver<StartEvaluationResponse> out) {
        CertificateEvaluation started = service.startEvaluation(
                bridge.id(request.getCertificateRequestId(), "certificateRequestId"));
        ProtoBridge.send(out, bridge.reply(StartEvaluationResponse.newBuilder(), "result", started).build());
    }
}
