package com.mysms.finance.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.finance.api.model.Refund;
import com.mysms.finance.business.RefundService;
import com.mysms.finance.v1.GetRefundRequest;
import com.mysms.finance.v1.GetRefundResponse;
import com.mysms.finance.v1.ListRefundsRequest;
import com.mysms.finance.v1.ListRefundsResponse;
import com.mysms.finance.v1.RefundServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of finance.RefundService: the refund reads, delegated to the same business service as REST's controller
 * (a refund is written by PaymentService's RefundPayment and the gateway's notifications). Thin: the service call, DTO
 * → proto; authentication, the permission and error mapping happen at the gRPC edge.
 */
@GrpcService
public class RefundResolver extends RefundServiceGrpc.RefundServiceImplBase {

    private final RefundService service;
    private final ProtoBridge bridge;

    public RefundResolver(RefundService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listRefunds(ListRefundsRequest request, StreamObserver<ListRefundsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Refund> rows = service.listRefunds(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListRefundsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getRefund(GetRefundRequest request, StreamObserver<GetRefundResponse> out) {
        Refund found = service.getRefund(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetRefundResponse.newBuilder(), "refund", found).build());
    }
}
