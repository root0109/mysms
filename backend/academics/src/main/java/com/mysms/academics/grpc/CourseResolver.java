package com.mysms.academics.grpc;

import com.mysms.academics.api.model.Course;
import com.mysms.academics.api.model.CourseSubject;
import com.mysms.academics.api.model.CourseSubjectUpdate;
import com.mysms.academics.api.model.CourseUpdate;
import com.mysms.academics.business.CourseService;
import com.mysms.academics.v1.CourseServiceGrpc;
import com.mysms.academics.v1.CreateCourseRequest;
import com.mysms.academics.v1.CreateCourseResponse;
import com.mysms.academics.v1.CreateCourseSubjectRequest;
import com.mysms.academics.v1.CreateCourseSubjectResponse;
import com.mysms.academics.v1.DeleteCourseRequest;
import com.mysms.academics.v1.DeleteCourseResponse;
import com.mysms.academics.v1.DeleteCourseSubjectRequest;
import com.mysms.academics.v1.DeleteCourseSubjectResponse;
import com.mysms.academics.v1.GetCourseRequest;
import com.mysms.academics.v1.GetCourseResponse;
import com.mysms.academics.v1.GetCourseSubjectRequest;
import com.mysms.academics.v1.GetCourseSubjectResponse;
import com.mysms.academics.v1.ListCourseSubjectsRequest;
import com.mysms.academics.v1.ListCourseSubjectsResponse;
import com.mysms.academics.v1.ListCoursesRequest;
import com.mysms.academics.v1.ListCoursesResponse;
import com.mysms.academics.v1.UpdateCourseRequest;
import com.mysms.academics.v1.UpdateCourseResponse;
import com.mysms.academics.v1.UpdateCourseSubjectRequest;
import com.mysms.academics.v1.UpdateCourseSubjectResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of academics.CourseService: delegates every RPC to the business service REST uses, so the rules exist
 * once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class CourseResolver extends CourseServiceGrpc.CourseServiceImplBase {

    private final CourseService service;
    private final ProtoBridge bridge;

    public CourseResolver(CourseService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listCourses(ListCoursesRequest request, StreamObserver<ListCoursesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Course> rows = service.listCourses(page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()),
                page.sort());
        ProtoBridge.send(out, bridge.page(ListCoursesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCourse(GetCourseRequest request, StreamObserver<GetCourseResponse> out) {
        Course found = service.getCourse(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCourseResponse.newBuilder(), "course", found).build());
    }

    @Override
    public void createCourse(CreateCourseRequest request, StreamObserver<CreateCourseResponse> out) {
        Course created = service.createCourse(bridge.body(request.getCourse(), Course.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCourseResponse.newBuilder(), "course", created).build());
    }

    @Override
    public void updateCourse(UpdateCourseRequest request, StreamObserver<UpdateCourseResponse> out) {
        Course updated = service.updateCourse(bridge.id(request.getCourse().getId(), "id"),
                bridge.requiredIfMatch(request.getCourse().getEtag()),
                bridge.changes(request.getCourse(), request.getUpdateMask(), CourseUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCourseResponse.newBuilder(), "course", updated).build());
    }

    @Override
    public void deleteCourse(DeleteCourseRequest request, StreamObserver<DeleteCourseResponse> out) {
        service.deleteCourse(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCourseResponse.getDefaultInstance());
    }

    @Override
    public void listCourseSubjects(ListCourseSubjectsRequest request, StreamObserver<ListCourseSubjectsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CourseSubject> rows = service.listCourseSubjects(bridge.id(request.getCourseId(), "courseId"), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCourseSubjectsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCourseSubject(GetCourseSubjectRequest request, StreamObserver<GetCourseSubjectResponse> out) {
        CourseSubject found = service.getCourseSubject(bridge.id(request.getCourseId(), "courseId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCourseSubjectResponse.newBuilder(), "course_subject", found).build());
    }

    @Override
    public void createCourseSubject(CreateCourseSubjectRequest request, StreamObserver<CreateCourseSubjectResponse> out) {
        CourseSubject created = service.createCourseSubject(bridge.id(request.getCourseId(), "courseId"),
                bridge.body(request.getCourseSubject(), CourseSubject.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCourseSubjectResponse.newBuilder(), "course_subject", created).build());
    }

    @Override
    public void updateCourseSubject(UpdateCourseSubjectRequest request, StreamObserver<UpdateCourseSubjectResponse> out) {
        CourseSubject updated = service.updateCourseSubject(bridge.id(request.getCourseId(), "courseId"),
                bridge.id(request.getCourseSubject().getId(), "id"), bridge.requiredIfMatch(request.getCourseSubject().getEtag()),
                bridge.changes(request.getCourseSubject(), request.getUpdateMask(), CourseSubjectUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCourseSubjectResponse.newBuilder(), "course_subject", updated).build());
    }

    @Override
    public void deleteCourseSubject(DeleteCourseSubjectRequest request, StreamObserver<DeleteCourseSubjectResponse> out) {
        service.deleteCourseSubject(bridge.id(request.getCourseId(), "courseId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCourseSubjectResponse.getDefaultInstance());
    }
}
