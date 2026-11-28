package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.Enrollment;
import com.mysms.learning.lms.api.model.MemberProfile;
import com.mysms.learning.lms.business.EnrollmentService;
import com.mysms.learning.lms.business.MemberProfileService;
import com.mysms.lms.v1.DeleteEnrollmentRequest;
import com.mysms.lms.v1.DeleteEnrollmentResponse;
import com.mysms.lms.v1.EnrollmentServiceGrpc;
import com.mysms.lms.v1.EnrollmentUpdate;
import com.mysms.lms.v1.GetEnrollmentRequest;
import com.mysms.lms.v1.GetEnrollmentResponse;
import com.mysms.lms.v1.GetLmsStatisticsRequest;
import com.mysms.lms.v1.GetLmsStatisticsResponse;
import com.mysms.lms.v1.GetMemberProfileRequest;
import com.mysms.lms.v1.GetMemberProfileResponse;
import com.mysms.lms.v1.GetStudentDashboardRequest;
import com.mysms.lms.v1.GetStudentDashboardResponse;
import com.mysms.lms.v1.ListEnrollmentsRequest;
import com.mysms.lms.v1.ListEnrollmentsResponse;
import com.mysms.lms.v1.UpdateEnrollmentResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.EnrollmentService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge; operations not served yet answer UNIMPLEMENTED.
 */
@GrpcService
public class EnrollmentResolver extends EnrollmentServiceGrpc.EnrollmentServiceImplBase {

    private final EnrollmentService service;
    private final MemberProfileService profiles;
    private final ProtoBridge bridge;

    public EnrollmentResolver(EnrollmentService service, MemberProfileService profiles, ProtoBridge bridge) {
        this.service = service;
        this.profiles = profiles;
        this.bridge = bridge;
    }

    @Override
    public void listEnrollments(ListEnrollmentsRequest request, StreamObserver<ListEnrollmentsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Enrollment> rows = service.listEnrollments(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListEnrollmentsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getEnrollment(GetEnrollmentRequest request, StreamObserver<GetEnrollmentResponse> out) {
        Enrollment found = service.getEnrollment(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetEnrollmentResponse.newBuilder(), "enrollment", found).build());
    }

    @Override
    public void updateEnrollment(EnrollmentUpdate request, StreamObserver<UpdateEnrollmentResponse> out) {
        Enrollment updated = service.updateEnrollment(bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                EnrollmentProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateEnrollmentResponse.newBuilder(), "enrollment", updated).build());
    }

    @Override
    public void deleteEnrollment(DeleteEnrollmentRequest request, StreamObserver<DeleteEnrollmentResponse> out) {
        service.deleteEnrollment(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteEnrollmentResponse.getDefaultInstance());
    }

    @Override
    public void getStudentDashboard(GetStudentDashboardRequest request, StreamObserver<GetStudentDashboardResponse> out) {
        ProtoBridge.send(out, bridge.reply(GetStudentDashboardResponse.newBuilder(), "result", service.getStudentDashboard())
                .build());
    }

    @Override
    public void getLmsStatistics(GetLmsStatisticsRequest request, StreamObserver<GetLmsStatisticsResponse> out) {
        ProtoBridge.send(out, bridge.reply(GetLmsStatisticsResponse.newBuilder(), "result", service.getLmsStatistics())
                .build());
    }

    @Override
    public void getMemberProfile(GetMemberProfileRequest request, StreamObserver<GetMemberProfileResponse> out) {
        MemberProfile profile = profiles.getMemberProfile(bridge.id(request.getUserId(), "userId"));
        ProtoBridge.send(out, bridge.reply(GetMemberProfileResponse.newBuilder(), "result", profile).build());
    }
}
