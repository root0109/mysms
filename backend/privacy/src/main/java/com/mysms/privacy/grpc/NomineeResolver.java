package com.mysms.privacy.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.privacy.api.model.MyNominee;
import com.mysms.privacy.business.NomineeService;
import com.mysms.privacy.v1.GetMyNomineeRequest;
import com.mysms.privacy.v1.GetMyNomineeResponse;
import com.mysms.privacy.v1.NomineeServiceGrpc;
import com.mysms.privacy.v1.RevokeMyNomineeRequest;
import com.mysms.privacy.v1.RevokeMyNomineeResponse;
import com.mysms.privacy.v1.SetMyNomineeRequest;
import com.mysms.privacy.v1.SetMyNomineeResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of privacy.NomineeService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class NomineeResolver extends NomineeServiceGrpc.NomineeServiceImplBase {

    private static final String MY_NOMINEE = "my_nominee";
    private static final String REQUEST_ID = "request_id";

    private final NomineeService service;
    private final ProtoBridge bridge;

    public NomineeResolver(NomineeService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void getMyNominee(GetMyNomineeRequest request, StreamObserver<GetMyNomineeResponse> out) {
        MyNominee found = service.getMyNominee();
        ProtoBridge.send(out, bridge.reply(GetMyNomineeResponse.newBuilder(), MY_NOMINEE, found).build());
    }

    @Override
    public void setMyNominee(SetMyNomineeRequest request, StreamObserver<SetMyNomineeResponse> out) {
        MyNominee named = service.setMyNominee(bridge.body(request,
                com.mysms.privacy.api.model.SetMyNomineeRequest.class, REQUEST_ID));
        ProtoBridge.send(out, bridge.reply(SetMyNomineeResponse.newBuilder(), MY_NOMINEE, named).build());
    }

    @Override
    public void revokeMyNominee(RevokeMyNomineeRequest request, StreamObserver<RevokeMyNomineeResponse> out) {
        service.revokeMyNominee(bridge.body(request, com.mysms.privacy.api.model.RevokeMyNomineeRequest.class,
                REQUEST_ID));
        ProtoBridge.send(out, RevokeMyNomineeResponse.getDefaultInstance());
    }
}
