package com.mysms.library.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.library.api.model.PurchaseRequest;
import com.mysms.library.api.model.PurchaseRequestCourse;
import com.mysms.library.api.model.PurchaseRequestCourseUpdate;
import com.mysms.library.api.model.PurchaseRequestSubject;
import com.mysms.library.api.model.PurchaseRequestSubjectUpdate;
import com.mysms.library.api.model.PurchaseRequestUpdate;
import com.mysms.library.business.PurchaseRequestService;
import com.mysms.library.v1.AcceptPurchaseRequestRequest;
import com.mysms.library.v1.AcceptPurchaseRequestResponse;
import com.mysms.library.v1.CreatePurchaseRequestCourseRequest;
import com.mysms.library.v1.CreatePurchaseRequestCourseResponse;
import com.mysms.library.v1.CreatePurchaseRequestRequest;
import com.mysms.library.v1.CreatePurchaseRequestResponse;
import com.mysms.library.v1.CreatePurchaseRequestSubjectRequest;
import com.mysms.library.v1.CreatePurchaseRequestSubjectResponse;
import com.mysms.library.v1.DeletePurchaseRequestCourseRequest;
import com.mysms.library.v1.DeletePurchaseRequestCourseResponse;
import com.mysms.library.v1.DeletePurchaseRequestRequest;
import com.mysms.library.v1.DeletePurchaseRequestResponse;
import com.mysms.library.v1.DeletePurchaseRequestSubjectRequest;
import com.mysms.library.v1.DeletePurchaseRequestSubjectResponse;
import com.mysms.library.v1.GetPurchaseRequestCourseRequest;
import com.mysms.library.v1.GetPurchaseRequestCourseResponse;
import com.mysms.library.v1.GetPurchaseRequestRequest;
import com.mysms.library.v1.GetPurchaseRequestResponse;
import com.mysms.library.v1.GetPurchaseRequestSubjectRequest;
import com.mysms.library.v1.GetPurchaseRequestSubjectResponse;
import com.mysms.library.v1.ListPurchaseRequestCoursesRequest;
import com.mysms.library.v1.ListPurchaseRequestCoursesResponse;
import com.mysms.library.v1.ListPurchaseRequestSubjectsRequest;
import com.mysms.library.v1.ListPurchaseRequestSubjectsResponse;
import com.mysms.library.v1.ListPurchaseRequestsRequest;
import com.mysms.library.v1.ListPurchaseRequestsResponse;
import com.mysms.library.v1.PurchaseRequestServiceGrpc;
import com.mysms.library.v1.RejectPurchaseRequestRequest;
import com.mysms.library.v1.RejectPurchaseRequestResponse;
import com.mysms.library.v1.SubmitPurchaseRequestRequest;
import com.mysms.library.v1.SubmitPurchaseRequestResponse;
import com.mysms.library.v1.UpdatePurchaseRequestCourseRequest;
import com.mysms.library.v1.UpdatePurchaseRequestCourseResponse;
import com.mysms.library.v1.UpdatePurchaseRequestRequest;
import com.mysms.library.v1.UpdatePurchaseRequestResponse;
import com.mysms.library.v1.UpdatePurchaseRequestSubjectRequest;
import com.mysms.library.v1.UpdatePurchaseRequestSubjectResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of library.PurchaseRequestService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class PurchaseRequestResolver extends PurchaseRequestServiceGrpc.PurchaseRequestServiceImplBase {

    private final PurchaseRequestService service;
    private final ProtoBridge bridge;

    public PurchaseRequestResolver(PurchaseRequestService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listPurchaseRequests(ListPurchaseRequestsRequest request, StreamObserver<ListPurchaseRequestsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<PurchaseRequest> rows = service.listPurchaseRequests(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPurchaseRequestsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPurchaseRequest(GetPurchaseRequestRequest request, StreamObserver<GetPurchaseRequestResponse> out) {
        PurchaseRequest found = service.getPurchaseRequest(
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetPurchaseRequestResponse.newBuilder(), "purchase_request", found).build());
    }

    @Override
    public void createPurchaseRequest(CreatePurchaseRequestRequest request, StreamObserver<CreatePurchaseRequestResponse> out) {
        PurchaseRequest created = service.createPurchaseRequest(
                bridge.body(request.getPurchaseRequest(), PurchaseRequest.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreatePurchaseRequestResponse.newBuilder(), "purchase_request", created).build());
    }

    @Override
    public void updatePurchaseRequest(UpdatePurchaseRequestRequest request, StreamObserver<UpdatePurchaseRequestResponse> out) {
        PurchaseRequest updated = service.updatePurchaseRequest(
                bridge.id(request.getPurchaseRequest().getId(), "id"),
                bridge.requiredIfMatch(request.getPurchaseRequest().getEtag()),
                bridge.changes(request.getPurchaseRequest(), request.getUpdateMask(), PurchaseRequestUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdatePurchaseRequestResponse.newBuilder(), "purchase_request", updated).build());
    }

    @Override
    public void deletePurchaseRequest(DeletePurchaseRequestRequest request, StreamObserver<DeletePurchaseRequestResponse> out) {
        service.deletePurchaseRequest(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeletePurchaseRequestResponse.getDefaultInstance());
    }

    @Override
    public void listPurchaseRequestCourses(ListPurchaseRequestCoursesRequest request,
            StreamObserver<ListPurchaseRequestCoursesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<PurchaseRequestCourse> rows = service.listPurchaseRequestCourses(
                bridge.id(request.getPurchaseRequestId(), "purchaseRequestId"),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPurchaseRequestCoursesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPurchaseRequestCourse(GetPurchaseRequestCourseRequest request,
            StreamObserver<GetPurchaseRequestCourseResponse> out) {
        PurchaseRequestCourse found = service.getPurchaseRequestCourse(
                bridge.id(request.getPurchaseRequestId(), "purchaseRequestId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetPurchaseRequestCourseResponse.newBuilder(), "purchase_request_course",
                found).build());
    }

    @Override
    public void createPurchaseRequestCourse(CreatePurchaseRequestCourseRequest request,
            StreamObserver<CreatePurchaseRequestCourseResponse> out) {
        PurchaseRequestCourse created = service.createPurchaseRequestCourse(
                bridge.id(request.getPurchaseRequestId(), "purchaseRequestId"),
                bridge.body(request.getPurchaseRequestCourse(), PurchaseRequestCourse.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreatePurchaseRequestCourseResponse.newBuilder(), "purchase_request_course",
                created).build());
    }

    @Override
    public void updatePurchaseRequestCourse(UpdatePurchaseRequestCourseRequest request,
            StreamObserver<UpdatePurchaseRequestCourseResponse> out) {
        PurchaseRequestCourse updated = service.updatePurchaseRequestCourse(
                bridge.id(request.getPurchaseRequestId(), "purchaseRequestId"),
                bridge.id(request.getPurchaseRequestCourse().getId(), "id"),
                bridge.requiredIfMatch(request.getPurchaseRequestCourse().getEtag()),
                bridge.changes(request.getPurchaseRequestCourse(), request.getUpdateMask(), PurchaseRequestCourseUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdatePurchaseRequestCourseResponse.newBuilder(), "purchase_request_course",
                updated).build());
    }

    @Override
    public void deletePurchaseRequestCourse(DeletePurchaseRequestCourseRequest request,
            StreamObserver<DeletePurchaseRequestCourseResponse> out) {
        service.deletePurchaseRequestCourse(
                bridge.id(request.getPurchaseRequestId(), "purchaseRequestId"),
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeletePurchaseRequestCourseResponse.getDefaultInstance());
    }

    @Override
    public void listPurchaseRequestSubjects(ListPurchaseRequestSubjectsRequest request,
            StreamObserver<ListPurchaseRequestSubjectsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<PurchaseRequestSubject> rows = service.listPurchaseRequestSubjects(
                bridge.id(request.getPurchaseRequestId(), "purchaseRequestId"),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPurchaseRequestSubjectsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPurchaseRequestSubject(GetPurchaseRequestSubjectRequest request,
            StreamObserver<GetPurchaseRequestSubjectResponse> out) {
        PurchaseRequestSubject found = service.getPurchaseRequestSubject(
                bridge.id(request.getPurchaseRequestId(), "purchaseRequestId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetPurchaseRequestSubjectResponse.newBuilder(), "purchase_request_subject",
                found).build());
    }

    @Override
    public void createPurchaseRequestSubject(CreatePurchaseRequestSubjectRequest request,
            StreamObserver<CreatePurchaseRequestSubjectResponse> out) {
        PurchaseRequestSubject created = service.createPurchaseRequestSubject(
                bridge.id(request.getPurchaseRequestId(), "purchaseRequestId"),
                bridge.body(request.getPurchaseRequestSubject(), PurchaseRequestSubject.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreatePurchaseRequestSubjectResponse.newBuilder(), "purchase_request_subject",
                created).build());
    }

    @Override
    public void updatePurchaseRequestSubject(UpdatePurchaseRequestSubjectRequest request,
            StreamObserver<UpdatePurchaseRequestSubjectResponse> out) {
        PurchaseRequestSubject updated = service.updatePurchaseRequestSubject(
                bridge.id(request.getPurchaseRequestId(), "purchaseRequestId"),
                bridge.id(request.getPurchaseRequestSubject().getId(), "id"),
                bridge.requiredIfMatch(request.getPurchaseRequestSubject().getEtag()),
                bridge.changes(request.getPurchaseRequestSubject(), request.getUpdateMask(),
                        PurchaseRequestSubjectUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdatePurchaseRequestSubjectResponse.newBuilder(), "purchase_request_subject",
                updated).build());
    }

    @Override
    public void deletePurchaseRequestSubject(DeletePurchaseRequestSubjectRequest request,
            StreamObserver<DeletePurchaseRequestSubjectResponse> out) {
        service.deletePurchaseRequestSubject(
                bridge.id(request.getPurchaseRequestId(), "purchaseRequestId"),
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeletePurchaseRequestSubjectResponse.getDefaultInstance());
    }

    /** The request's etag (optional) is the REST body's; the path id and the idempotency key travel apart. */
    @Override
    public void submitPurchaseRequest(SubmitPurchaseRequestRequest request, StreamObserver<SubmitPurchaseRequestResponse> out) {
        PurchaseRequest changed = service.submitPurchaseRequest(
                bridge.id(request.getPurchaseRequestId(), "purchaseRequestId"),
                bridge.body(request, com.mysms.library.api.model.SubmitPurchaseRequestRequest.class,
                        "purchase_request_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(SubmitPurchaseRequestResponse.newBuilder(), "purchase_request", changed).build());
    }

    /** The request's etag (optional) is the REST body's; the path id and the idempotency key travel apart. */
    @Override
    public void acceptPurchaseRequest(AcceptPurchaseRequestRequest request, StreamObserver<AcceptPurchaseRequestResponse> out) {
        PurchaseRequest changed = service.acceptPurchaseRequest(
                bridge.id(request.getPurchaseRequestId(), "purchaseRequestId"),
                bridge.body(request, com.mysms.library.api.model.AcceptPurchaseRequestRequest.class,
                        "purchase_request_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(AcceptPurchaseRequestResponse.newBuilder(), "purchase_request", changed).build());
    }

    /** The request's etag (optional) is the REST body's; the path id and the idempotency key travel apart. */
    @Override
    public void rejectPurchaseRequest(RejectPurchaseRequestRequest request, StreamObserver<RejectPurchaseRequestResponse> out) {
        PurchaseRequest changed = service.rejectPurchaseRequest(
                bridge.id(request.getPurchaseRequestId(), "purchaseRequestId"),
                bridge.body(request, com.mysms.library.api.model.RejectPurchaseRequestRequest.class,
                        "purchase_request_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(RejectPurchaseRequestResponse.newBuilder(), "purchase_request", changed).build());
    }
}
