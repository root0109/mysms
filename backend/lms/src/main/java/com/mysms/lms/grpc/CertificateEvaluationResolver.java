package com.mysms.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.lms.api.model.Certificate;
import com.mysms.lms.api.model.CertificateEvaluation;
import com.mysms.lms.api.model.CertificateEvaluationUpdate;
import com.mysms.lms.business.CertificateEvaluationService;
import com.mysms.lms.v1.CertificateEvaluationServiceGrpc;
import com.mysms.lms.v1.CreateCertificateEvaluationRequest;
import com.mysms.lms.v1.CreateCertificateEvaluationResponse;
import com.mysms.lms.v1.DeleteCertificateEvaluationRequest;
import com.mysms.lms.v1.DeleteCertificateEvaluationResponse;
import com.mysms.lms.v1.GetCertificateEvaluationRequest;
import com.mysms.lms.v1.GetCertificateEvaluationResponse;
import com.mysms.lms.v1.IssueCertificateFromEvaluationRequest;
import com.mysms.lms.v1.IssueCertificateFromEvaluationResponse;
import com.mysms.lms.v1.ListCertificateEvaluationsRequest;
import com.mysms.lms.v1.ListCertificateEvaluationsResponse;
import com.mysms.lms.v1.UpdateCertificateEvaluationRequest;
import com.mysms.lms.v1.UpdateCertificateEvaluationResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.CertificateEvaluationService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class CertificateEvaluationResolver extends CertificateEvaluationServiceGrpc.CertificateEvaluationServiceImplBase {

    private final CertificateEvaluationService service;
    private final ProtoBridge bridge;

    public CertificateEvaluationResolver(CertificateEvaluationService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listCertificateEvaluations(ListCertificateEvaluationsRequest request,
            StreamObserver<ListCertificateEvaluationsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CertificateEvaluation> rows = service.listCertificateEvaluations(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCertificateEvaluationsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCertificateEvaluation(GetCertificateEvaluationRequest request,
            StreamObserver<GetCertificateEvaluationResponse> out) {
        CertificateEvaluation found = service.getCertificateEvaluation(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCertificateEvaluationResponse.newBuilder(), "certificate_evaluation",
                found).build());
    }

    @Override
    public void createCertificateEvaluation(CreateCertificateEvaluationRequest request,
            StreamObserver<CreateCertificateEvaluationResponse> out) {
        CertificateEvaluation created = service.createCertificateEvaluation(
                bridge.body(request.getCertificateEvaluation(), CertificateEvaluation.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCertificateEvaluationResponse.newBuilder(), "certificate_evaluation",
                created).build());
    }

    @Override
    public void updateCertificateEvaluation(UpdateCertificateEvaluationRequest request,
            StreamObserver<UpdateCertificateEvaluationResponse> out) {
        CertificateEvaluation updated = service.updateCertificateEvaluation(
                bridge.id(request.getCertificateEvaluation().getId(), "id"),
                bridge.requiredIfMatch(request.getCertificateEvaluation().getEtag()),
                bridge.changes(request.getCertificateEvaluation(), request.getUpdateMask(), CertificateEvaluationUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCertificateEvaluationResponse.newBuilder(), "certificate_evaluation",
                updated).build());
    }

    @Override
    public void deleteCertificateEvaluation(DeleteCertificateEvaluationRequest request,
            StreamObserver<DeleteCertificateEvaluationResponse> out) {
        service.deleteCertificateEvaluation(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCertificateEvaluationResponse.getDefaultInstance());
    }

    @Override
    public void issueCertificateFromEvaluation(IssueCertificateFromEvaluationRequest request,
            StreamObserver<IssueCertificateFromEvaluationResponse> out) {
        com.mysms.lms.api.model.IssueCertificateFromEvaluationRequest body = bridge.body(request,
                com.mysms.lms.api.model.IssueCertificateFromEvaluationRequest.class, "certificate_evaluation_id",
                "request_id");
        Certificate issued = service.issueCertificateFromEvaluation(
                bridge.id(request.getCertificateEvaluationId(), "certificateEvaluationId"), body.getTemplateCode(),
                body.getIssueDate(), body.getExpiryDate(), Boolean.TRUE.equals(body.getPublish()));
        ProtoBridge.send(out, bridge.reply(IssueCertificateFromEvaluationResponse.newBuilder(), "result", issued).build());
    }
}
