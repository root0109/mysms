package com.mysms.privacy.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.privacy.api.model.AccessSummary;
import com.mysms.privacy.api.model.MyRightsRequest;
import com.mysms.privacy.api.model.RightsRequest;
import com.mysms.privacy.business.RightsRequestService;
import com.mysms.privacy.v1.AssignRightsRequestRequest;
import com.mysms.privacy.v1.AssignRightsRequestResponse;
import com.mysms.privacy.v1.CloseRightsRequestRequest;
import com.mysms.privacy.v1.CloseRightsRequestResponse;
import com.mysms.privacy.v1.ExtendRightsRequestRequest;
import com.mysms.privacy.v1.ExtendRightsRequestResponse;
import com.mysms.privacy.v1.FulfilAccessRequestRequest;
import com.mysms.privacy.v1.FulfilAccessRequestResponse;
import com.mysms.privacy.v1.FulfilWithdrawAllRequestRequest;
import com.mysms.privacy.v1.FulfilWithdrawAllRequestResponse;
import com.mysms.privacy.v1.GetAccessSummaryRequest;
import com.mysms.privacy.v1.GetAccessSummaryResponse;
import com.mysms.privacy.v1.GetMyAccessSummaryRequest;
import com.mysms.privacy.v1.GetMyAccessSummaryResponse;
import com.mysms.privacy.v1.GetMyRequestRequest;
import com.mysms.privacy.v1.GetMyRequestResponse;
import com.mysms.privacy.v1.GetRightsRequestRequest;
import com.mysms.privacy.v1.GetRightsRequestResponse;
import com.mysms.privacy.v1.ListMyRequestsRequest;
import com.mysms.privacy.v1.ListMyRequestsResponse;
import com.mysms.privacy.v1.ListRightsRequestsRequest;
import com.mysms.privacy.v1.ListRightsRequestsResponse;
import com.mysms.privacy.v1.RejectRightsRequestRequest;
import com.mysms.privacy.v1.RejectRightsRequestResponse;
import com.mysms.privacy.v1.RetryRightsRequestTaskRequest;
import com.mysms.privacy.v1.RetryRightsRequestTaskResponse;
import com.mysms.privacy.v1.RightsRequestServiceGrpc;
import com.mysms.privacy.v1.StartRightsRequestFulfilmentRequest;
import com.mysms.privacy.v1.StartRightsRequestFulfilmentResponse;
import com.mysms.privacy.v1.SubmitMyRequestRequest;
import com.mysms.privacy.v1.SubmitMyRequestResponse;
import com.mysms.privacy.v1.VerifyRightsRequestIdentityRequest;
import com.mysms.privacy.v1.VerifyRightsRequestIdentityResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of privacy.RightsRequestService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge. A step's REST body is its
 * request without the path id and the idempotency key.
 */
@GrpcService
public class RightsRequestResolver extends RightsRequestServiceGrpc.RightsRequestServiceImplBase {

    private static final String MY_RIGHTS_REQUEST = "my_rights_request";
    private static final String RIGHTS_REQUEST = "rights_request";
    private static final String ACCESS_SUMMARY = "access_summary";
    private static final String PATH_ID = "rights_request_id";
    private static final String REQUEST_ID = "request_id";
    private static final String TASK_ID = "task_id";

    private final RightsRequestService service;
    private final ProtoBridge bridge;

    public RightsRequestResolver(RightsRequestService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void submitMyRequest(SubmitMyRequestRequest request, StreamObserver<SubmitMyRequestResponse> out) {
        MyRightsRequest submitted = service.submitMyRequest(bridge.body(request,
                com.mysms.privacy.api.model.SubmitMyRequestRequest.class, REQUEST_ID));
        ProtoBridge.send(out, bridge.reply(SubmitMyRequestResponse.newBuilder(), MY_RIGHTS_REQUEST, submitted)
                .build());
    }

    @Override
    public void listMyRequests(ListMyRequestsRequest request, StreamObserver<ListMyRequestsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<MyRightsRequest> rows = service.listMyRequests(page.page(), page.size(), page.cursor());
        ProtoBridge.send(out, bridge.page(ListMyRequestsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getMyRequest(GetMyRequestRequest request, StreamObserver<GetMyRequestResponse> out) {
        MyRightsRequest found = service.getMyRequest(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetMyRequestResponse.newBuilder(), MY_RIGHTS_REQUEST, found).build());
    }

    @Override
    public void getMyAccessSummary(GetMyAccessSummaryRequest request, StreamObserver<GetMyAccessSummaryResponse> out) {
        AccessSummary found = service.getMyAccessSummary(bridge.id(request.getRightsRequestId(), "rightsRequestId"));
        ProtoBridge.send(out, bridge.reply(GetMyAccessSummaryResponse.newBuilder(), ACCESS_SUMMARY, found).build());
    }

    @Override
    public void listRightsRequests(ListRightsRequestsRequest request, StreamObserver<ListRightsRequestsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<RightsRequest> rows = service.listRightsRequests(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListRightsRequestsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getRightsRequest(GetRightsRequestRequest request, StreamObserver<GetRightsRequestResponse> out) {
        RightsRequest found = service.getRightsRequest(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetRightsRequestResponse.newBuilder(), RIGHTS_REQUEST, found).build());
    }

    @Override
    public void getAccessSummary(GetAccessSummaryRequest request, StreamObserver<GetAccessSummaryResponse> out) {
        AccessSummary found = service.getAccessSummary(bridge.id(request.getRightsRequestId(), "rightsRequestId"));
        ProtoBridge.send(out, bridge.reply(GetAccessSummaryResponse.newBuilder(), ACCESS_SUMMARY, found).build());
    }

    @Override
    public void verifyRightsRequestIdentity(VerifyRightsRequestIdentityRequest request,
            StreamObserver<VerifyRightsRequestIdentityResponse> out) {
        RightsRequest verified = service.verifyRightsRequestIdentity(bridge.id(request.getRightsRequestId(),
                "rightsRequestId"), bridge.body(request,
                com.mysms.privacy.api.model.VerifyRightsRequestIdentityRequest.class, PATH_ID, REQUEST_ID));
        ProtoBridge.send(out, bridge.reply(VerifyRightsRequestIdentityResponse.newBuilder(), RIGHTS_REQUEST, verified)
                .build());
    }

    @Override
    public void assignRightsRequest(AssignRightsRequestRequest request,
            StreamObserver<AssignRightsRequestResponse> out) {
        RightsRequest assigned = service.assignRightsRequest(bridge.id(request.getRightsRequestId(),
                "rightsRequestId"), bridge.body(request, com.mysms.privacy.api.model.AssignRightsRequestRequest.class,
                PATH_ID, REQUEST_ID));
        ProtoBridge.send(out, bridge.reply(AssignRightsRequestResponse.newBuilder(), RIGHTS_REQUEST, assigned)
                .build());
    }

    @Override
    public void extendRightsRequest(ExtendRightsRequestRequest request,
            StreamObserver<ExtendRightsRequestResponse> out) {
        RightsRequest extended = service.extendRightsRequest(bridge.id(request.getRightsRequestId(),
                "rightsRequestId"), bridge.body(request, com.mysms.privacy.api.model.ExtendRightsRequestRequest.class,
                PATH_ID, REQUEST_ID));
        ProtoBridge.send(out, bridge.reply(ExtendRightsRequestResponse.newBuilder(), RIGHTS_REQUEST, extended)
                .build());
    }

    @Override
    public void rejectRightsRequest(RejectRightsRequestRequest request,
            StreamObserver<RejectRightsRequestResponse> out) {
        RightsRequest rejected = service.rejectRightsRequest(bridge.id(request.getRightsRequestId(),
                "rightsRequestId"), bridge.body(request, com.mysms.privacy.api.model.RejectRightsRequestRequest.class,
                PATH_ID, REQUEST_ID));
        ProtoBridge.send(out, bridge.reply(RejectRightsRequestResponse.newBuilder(), RIGHTS_REQUEST, rejected)
                .build());
    }

    @Override
    public void closeRightsRequest(CloseRightsRequestRequest request,
            StreamObserver<CloseRightsRequestResponse> out) {
        RightsRequest closed = service.closeRightsRequest(bridge.id(request.getRightsRequestId(),
                "rightsRequestId"), bridge.body(request, com.mysms.privacy.api.model.CloseRightsRequestRequest.class,
                PATH_ID, REQUEST_ID));
        ProtoBridge.send(out, bridge.reply(CloseRightsRequestResponse.newBuilder(), RIGHTS_REQUEST, closed).build());
    }

    @Override
    public void fulfilAccessRequest(FulfilAccessRequestRequest request,
            StreamObserver<FulfilAccessRequestResponse> out) {
        RightsRequest fulfilled = service.fulfilAccessRequest(bridge.id(request.getRightsRequestId(),
                "rightsRequestId"), bridge.body(request, com.mysms.privacy.api.model.FulfilAccessRequestRequest.class,
                PATH_ID, REQUEST_ID));
        ProtoBridge.send(out, bridge.reply(FulfilAccessRequestResponse.newBuilder(), RIGHTS_REQUEST, fulfilled)
                .build());
    }

    @Override
    public void startRightsRequestFulfilment(StartRightsRequestFulfilmentRequest request,
            StreamObserver<StartRightsRequestFulfilmentResponse> out) {
        RightsRequest started = service.startRightsRequestFulfilment(bridge.id(request.getRightsRequestId(),
                "rightsRequestId"), bridge.body(request, com.mysms.privacy.api.model.FulfilRightsRequestRequest.class,
                PATH_ID, REQUEST_ID));
        ProtoBridge.send(out, bridge.reply(StartRightsRequestFulfilmentResponse.newBuilder(), RIGHTS_REQUEST, started)
                .build());
    }

    @Override
    public void fulfilWithdrawAllRequest(FulfilWithdrawAllRequestRequest request,
            StreamObserver<FulfilWithdrawAllRequestResponse> out) {
        RightsRequest fulfilled = service.fulfilWithdrawAllRequest(bridge.id(request.getRightsRequestId(),
                "rightsRequestId"), bridge.body(request, com.mysms.privacy.api.model.FulfilRightsRequestRequest.class,
                PATH_ID, REQUEST_ID));
        ProtoBridge.send(out, bridge.reply(FulfilWithdrawAllRequestResponse.newBuilder(), RIGHTS_REQUEST, fulfilled)
                .build());
    }

    @Override
    public void retryRightsRequestTask(RetryRightsRequestTaskRequest request,
            StreamObserver<RetryRightsRequestTaskResponse> out) {
        RightsRequest retried = service.retryRightsRequestTask(bridge.id(request.getRightsRequestId(),
                "rightsRequestId"), bridge.id(request.getTaskId(), "taskId"), bridge.body(request,
                com.mysms.privacy.api.model.RetryRightsRequestTaskRequest.class, PATH_ID, TASK_ID, REQUEST_ID));
        ProtoBridge.send(out, bridge.reply(RetryRightsRequestTaskResponse.newBuilder(), RIGHTS_REQUEST, retried)
                .build());
    }
}
