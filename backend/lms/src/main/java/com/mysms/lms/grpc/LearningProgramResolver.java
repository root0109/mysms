package com.mysms.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.lms.api.model.LearningProgram;
import com.mysms.lms.api.model.LearningProgramCourse;
import com.mysms.lms.api.model.LearningProgramCourseUpdate;
import com.mysms.lms.api.model.LearningProgramMember;
import com.mysms.lms.api.model.LearningProgramMemberUpdate;
import com.mysms.lms.api.model.LearningProgramUpdate;
import com.mysms.lms.business.LearningProgramService;
import com.mysms.lms.v1.CreateLearningProgramCourseRequest;
import com.mysms.lms.v1.CreateLearningProgramCourseResponse;
import com.mysms.lms.v1.CreateLearningProgramMemberRequest;
import com.mysms.lms.v1.CreateLearningProgramMemberResponse;
import com.mysms.lms.v1.CreateLearningProgramRequest;
import com.mysms.lms.v1.CreateLearningProgramResponse;
import com.mysms.lms.v1.DeleteLearningProgramCourseRequest;
import com.mysms.lms.v1.DeleteLearningProgramCourseResponse;
import com.mysms.lms.v1.DeleteLearningProgramMemberRequest;
import com.mysms.lms.v1.DeleteLearningProgramMemberResponse;
import com.mysms.lms.v1.DeleteLearningProgramRequest;
import com.mysms.lms.v1.DeleteLearningProgramResponse;
import com.mysms.lms.v1.EnrollInLearningProgramRequest;
import com.mysms.lms.v1.EnrollInLearningProgramResponse;
import com.mysms.lms.v1.GetLearningProgramCourseRequest;
import com.mysms.lms.v1.GetLearningProgramCourseResponse;
import com.mysms.lms.v1.GetLearningProgramMemberRequest;
import com.mysms.lms.v1.GetLearningProgramMemberResponse;
import com.mysms.lms.v1.GetLearningProgramRequest;
import com.mysms.lms.v1.GetLearningProgramResponse;
import com.mysms.lms.v1.LearningProgramServiceGrpc;
import com.mysms.lms.v1.ListLearningProgramCoursesRequest;
import com.mysms.lms.v1.ListLearningProgramCoursesResponse;
import com.mysms.lms.v1.ListLearningProgramMembersRequest;
import com.mysms.lms.v1.ListLearningProgramMembersResponse;
import com.mysms.lms.v1.ListLearningProgramsRequest;
import com.mysms.lms.v1.ListLearningProgramsResponse;
import com.mysms.lms.v1.UpdateLearningProgramCourseRequest;
import com.mysms.lms.v1.UpdateLearningProgramCourseResponse;
import com.mysms.lms.v1.UpdateLearningProgramMemberRequest;
import com.mysms.lms.v1.UpdateLearningProgramMemberResponse;
import com.mysms.lms.v1.UpdateLearningProgramRequest;
import com.mysms.lms.v1.UpdateLearningProgramResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.LearningProgramService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge; operations not served yet answer
 * UNIMPLEMENTED.
 */
@GrpcService
public class LearningProgramResolver extends LearningProgramServiceGrpc.LearningProgramServiceImplBase {

    private final LearningProgramService service;
    private final ProtoBridge bridge;

    public LearningProgramResolver(LearningProgramService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listLearningPrograms(ListLearningProgramsRequest request, StreamObserver<ListLearningProgramsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<LearningProgram> rows = service.listLearningPrograms(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListLearningProgramsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getLearningProgram(GetLearningProgramRequest request, StreamObserver<GetLearningProgramResponse> out) {
        LearningProgram found = service.getLearningProgram(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetLearningProgramResponse.newBuilder(), "learning_program", found).build());
    }

    @Override
    public void createLearningProgram(CreateLearningProgramRequest request, StreamObserver<CreateLearningProgramResponse> out) {
        LearningProgram created = service.createLearningProgram(bridge.body(request.getLearningProgram(), LearningProgram.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateLearningProgramResponse.newBuilder(), "learning_program", created).build());
    }

    @Override
    public void updateLearningProgram(UpdateLearningProgramRequest request, StreamObserver<UpdateLearningProgramResponse> out) {
        LearningProgram updated = service.updateLearningProgram(bridge.id(request.getLearningProgram().getId(), "id"),
                bridge.requiredIfMatch(request.getLearningProgram().getEtag()),
                bridge.changes(request.getLearningProgram(), request.getUpdateMask(), LearningProgramUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateLearningProgramResponse.newBuilder(), "learning_program", updated).build());
    }

    @Override
    public void deleteLearningProgram(DeleteLearningProgramRequest request, StreamObserver<DeleteLearningProgramResponse> out) {
        service.deleteLearningProgram(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteLearningProgramResponse.getDefaultInstance());
    }

    @Override
    public void listLearningProgramCourses(ListLearningProgramCoursesRequest request,
            StreamObserver<ListLearningProgramCoursesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<LearningProgramCourse> rows = service.listLearningProgramCourses(
                bridge.id(request.getLearningProgramId(), "learningProgramId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListLearningProgramCoursesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getLearningProgramCourse(GetLearningProgramCourseRequest request,
            StreamObserver<GetLearningProgramCourseResponse> out) {
        LearningProgramCourse found = service.getLearningProgramCourse(
                bridge.id(request.getLearningProgramId(), "learningProgramId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out,
                bridge.reply(GetLearningProgramCourseResponse.newBuilder(), "learning_program_course", found).build());
    }

    @Override
    public void createLearningProgramCourse(CreateLearningProgramCourseRequest request,
            StreamObserver<CreateLearningProgramCourseResponse> out) {
        LearningProgramCourse created = service.createLearningProgramCourse(
                bridge.id(request.getLearningProgramId(), "learningProgramId"),
                bridge.body(request.getLearningProgramCourse(), LearningProgramCourse.class), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(CreateLearningProgramCourseResponse.newBuilder(), "learning_program_course", created).build());
    }

    @Override
    public void updateLearningProgramCourse(UpdateLearningProgramCourseRequest request,
            StreamObserver<UpdateLearningProgramCourseResponse> out) {
        LearningProgramCourse updated = service.updateLearningProgramCourse(
                bridge.id(request.getLearningProgramId(), "learningProgramId"),
                bridge.id(request.getLearningProgramCourse().getId(), "id"),
                bridge.requiredIfMatch(request.getLearningProgramCourse().getEtag()),
                bridge.changes(request.getLearningProgramCourse(), request.getUpdateMask(), LearningProgramCourseUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(UpdateLearningProgramCourseResponse.newBuilder(), "learning_program_course", updated).build());
    }

    @Override
    public void deleteLearningProgramCourse(DeleteLearningProgramCourseRequest request,
            StreamObserver<DeleteLearningProgramCourseResponse> out) {
        service.deleteLearningProgramCourse(bridge.id(request.getLearningProgramId(), "learningProgramId"),
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteLearningProgramCourseResponse.getDefaultInstance());
    }

    @Override
    public void listLearningProgramMembers(ListLearningProgramMembersRequest request,
            StreamObserver<ListLearningProgramMembersResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<LearningProgramMember> rows = service.listLearningProgramMembers(
                bridge.id(request.getLearningProgramId(), "learningProgramId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListLearningProgramMembersResponse.newBuilder(), rows).build());
    }

    @Override
    public void getLearningProgramMember(GetLearningProgramMemberRequest request,
            StreamObserver<GetLearningProgramMemberResponse> out) {
        LearningProgramMember found = service.getLearningProgramMember(
                bridge.id(request.getLearningProgramId(), "learningProgramId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out,
                bridge.reply(GetLearningProgramMemberResponse.newBuilder(), "learning_program_member", found).build());
    }

    @Override
    public void createLearningProgramMember(CreateLearningProgramMemberRequest request,
            StreamObserver<CreateLearningProgramMemberResponse> out) {
        LearningProgramMember created = service.createLearningProgramMember(
                bridge.id(request.getLearningProgramId(), "learningProgramId"),
                bridge.body(request.getLearningProgramMember(), LearningProgramMember.class), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(CreateLearningProgramMemberResponse.newBuilder(), "learning_program_member", created).build());
    }

    @Override
    public void updateLearningProgramMember(UpdateLearningProgramMemberRequest request,
            StreamObserver<UpdateLearningProgramMemberResponse> out) {
        LearningProgramMember updated = service.updateLearningProgramMember(
                bridge.id(request.getLearningProgramId(), "learningProgramId"),
                bridge.id(request.getLearningProgramMember().getId(), "id"),
                bridge.requiredIfMatch(request.getLearningProgramMember().getEtag()),
                bridge.changes(request.getLearningProgramMember(), request.getUpdateMask(), LearningProgramMemberUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(UpdateLearningProgramMemberResponse.newBuilder(), "learning_program_member", updated).build());
    }

    @Override
    public void deleteLearningProgramMember(DeleteLearningProgramMemberRequest request,
            StreamObserver<DeleteLearningProgramMemberResponse> out) {
        service.deleteLearningProgramMember(bridge.id(request.getLearningProgramId(), "learningProgramId"),
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteLearningProgramMemberResponse.getDefaultInstance());
    }

    @Override
    public void enrollInLearningProgram(EnrollInLearningProgramRequest request,
            StreamObserver<EnrollInLearningProgramResponse> out) {
        LearningProgramMember member = service.enrollInLearningProgram(
                bridge.id(request.getLearningProgramId(), "learningProgramId"));
        ProtoBridge.send(out, bridge.reply(EnrollInLearningProgramResponse.newBuilder(), "result", member).build());
    }
}
