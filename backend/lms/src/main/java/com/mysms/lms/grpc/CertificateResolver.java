package com.mysms.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.lms.api.model.Certificate;
import com.mysms.lms.api.model.DownloadLink;
import com.mysms.lms.api.model.CertificateVerification;
import com.mysms.lms.business.CertificateService;
import com.mysms.lms.v1.CertificateServiceGrpc;
import com.mysms.lms.v1.DownloadCertificateRequest;
import com.mysms.lms.v1.DownloadCertificateResponse;
import com.mysms.lms.v1.GetCertificateRequest;
import com.mysms.lms.v1.GetCertificateResponse;
import com.mysms.lms.v1.ListCertificatesRequest;
import com.mysms.lms.v1.ListCertificatesResponse;
import com.mysms.lms.v1.VerifyCertificateRequest;
import com.mysms.lms.v1.VerifyCertificateResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.CertificateService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class CertificateResolver extends CertificateServiceGrpc.CertificateServiceImplBase {

    private final CertificateService service;
    private final ProtoBridge bridge;

    public CertificateResolver(CertificateService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listCertificates(ListCertificatesRequest request, StreamObserver<ListCertificatesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Certificate> rows = service.listCertificates(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCertificatesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCertificate(GetCertificateRequest request, StreamObserver<GetCertificateResponse> out) {
        Certificate found = service.getCertificate(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCertificateResponse.newBuilder(), "certificate", found).build());
    }

    @Override
    public void verifyCertificate(VerifyCertificateRequest request, StreamObserver<VerifyCertificateResponse> out) {
        CertificateVerification verification = service.verifyCertificate(bridge.id(request.getCertificateId(),
                "certificateId"));
        ProtoBridge.send(out, bridge.reply(VerifyCertificateResponse.newBuilder(), "result", verification).build());
    }

    @Override
    public void downloadCertificate(DownloadCertificateRequest request,
            StreamObserver<DownloadCertificateResponse> out) {
        DownloadLink link = service.downloadCertificate(bridge.id(request.getCertificateId(), "certificateId"));
        ProtoBridge.send(out, bridge.reply(DownloadCertificateResponse.newBuilder(), "result", link).build());
    }
}
