package com.mysms.privacy.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.privacy.api.model.ConsentCoverage;
import com.mysms.privacy.business.ConsentCoverageService;
import com.mysms.privacy.v1.ConsentCoverageServiceGrpc;
import com.mysms.privacy.v1.GetConsentCoverageRequest;
import com.mysms.privacy.v1.GetConsentCoverageResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of privacy.ConsentCoverageService: delegates to the same business service as REST. Authentication,
 * permission and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ConsentCoverageResolver extends ConsentCoverageServiceGrpc.ConsentCoverageServiceImplBase {

    private final ConsentCoverageService service;
    private final ProtoBridge bridge;

    public ConsentCoverageResolver(ConsentCoverageService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void getConsentCoverage(GetConsentCoverageRequest request, StreamObserver<GetConsentCoverageResponse> out) {
        ConsentCoverage coverage = service.getConsentCoverage(request.getPurposeCode(),
                request.hasScope() ? request.getScope() : null,
                request.hasInstitutionId() ? bridge.id(request.getInstitutionId(), "institutionId") : null);
        ProtoBridge.send(out, bridge.reply(GetConsentCoverageResponse.newBuilder(), "consent_coverage", coverage)
                .build());
    }
}
