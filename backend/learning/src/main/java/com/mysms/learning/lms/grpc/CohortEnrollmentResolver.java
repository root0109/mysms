package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.CohortEnrollment;
import com.mysms.learning.lms.api.model.CohortEnrollmentUpdate;
import com.mysms.learning.lms.business.CohortEnrollmentService;
import com.mysms.lms.v1.CohortEnrollmentServiceGrpc;
import com.mysms.lms.v1.DeleteCohortEnrollmentRequest;
import com.mysms.lms.v1.DeleteCohortEnrollmentResponse;
import com.mysms.lms.v1.GetCohortEnrollmentRequest;
import com.mysms.lms.v1.GetCohortEnrollmentResponse;
import com.mysms.lms.v1.ListCohortEnrollmentsRequest;
import com.mysms.lms.v1.ListCohortEnrollmentsResponse;
import com.mysms.lms.v1.UpdateCohortEnrollmentRequest;
import com.mysms.lms.v1.UpdateCohortEnrollmentResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.CohortEnrollmentService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge; operations not served yet answer
 * UNIMPLEMENTED.
 */
@GrpcService
public class CohortEnrollmentResolver extends CohortEnrollmentServiceGrpc.CohortEnrollmentServiceImplBase {

    private final CohortEnrollmentService service;
    private final ProtoBridge bridge;

    public CohortEnrollmentResolver(CohortEnrollmentService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listCohortEnrollments(ListCohortEnrollmentsRequest request, StreamObserver<ListCohortEnrollmentsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CohortEnrollment> rows = service.listCohortEnrollments(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCohortEnrollmentsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCohortEnrollment(GetCohortEnrollmentRequest request, StreamObserver<GetCohortEnrollmentResponse> out) {
        CohortEnrollment found = service.getCohortEnrollment(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCohortEnrollmentResponse.newBuilder(), "cohort_enrollment", found).build());
    }

    @Override
    public void updateCohortEnrollment(UpdateCohortEnrollmentRequest request,
            StreamObserver<UpdateCohortEnrollmentResponse> out) {
        CohortEnrollment updated = service.updateCohortEnrollment(bridge.id(request.getCohortEnrollment().getId(), "id"),
                bridge.requiredIfMatch(request.getCohortEnrollment().getEtag()),
                bridge.changes(request.getCohortEnrollment(), request.getUpdateMask(), CohortEnrollmentUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCohortEnrollmentResponse.newBuilder(), "cohort_enrollment", updated).build());
    }

    @Override
    public void deleteCohortEnrollment(DeleteCohortEnrollmentRequest request,
            StreamObserver<DeleteCohortEnrollmentResponse> out) {
        service.deleteCohortEnrollment(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCohortEnrollmentResponse.getDefaultInstance());
    }
}
