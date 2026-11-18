package com.mysms.finance.grpc;

import com.mysms.core.error.NotImplementedException;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.finance.api.model.CheckoutSession;
import com.mysms.finance.api.model.OrderQuote;
import com.mysms.finance.api.model.Payment;
import com.mysms.finance.api.model.Refund;
import com.mysms.finance.business.PaymentService;
import com.mysms.finance.v1.GetPaymentRequest;
import com.mysms.finance.v1.GetPaymentResponse;
import com.mysms.finance.v1.ListPaymentsRequest;
import com.mysms.finance.v1.ListPaymentsResponse;
import com.mysms.finance.v1.PaymentServiceGrpc;
import com.mysms.finance.v1.QuoteCheckoutRequest;
import com.mysms.finance.v1.QuoteCheckoutResponse;
import com.mysms.finance.v1.ReceiveGatewayWebhookRequest;
import com.mysms.finance.v1.ReceiveGatewayWebhookResponse;
import com.mysms.finance.v1.RecordPaymentRequest;
import com.mysms.finance.v1.RecordPaymentResponse;
import com.mysms.finance.v1.RefundPaymentRequest;
import com.mysms.finance.v1.RefundPaymentResponse;
import com.mysms.finance.v1.StartCheckoutRequest;
import com.mysms.finance.v1.StartCheckoutResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of finance.PaymentService: delegates every RPC to the business service REST uses, so the rules exist
 * once. Authentication, permission, idempotency (the required {@code request_id} of a checkout, a counter payment and a
 * refund) and error mapping happen at the gRPC edge.
 */
@GrpcService
public class PaymentResolver extends PaymentServiceGrpc.PaymentServiceImplBase {

    /** Why the gateway's notifications are REST's alone. */
    private static final String WEBHOOK_REST_ONLY = "Payment gateway notifications are received over REST only: "
            + "their signature covers the raw HTTP body the gateway sent.";

    private final PaymentService service;
    private final ProtoBridge bridge;

    public PaymentResolver(PaymentService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void recordPayment(RecordPaymentRequest request, StreamObserver<RecordPaymentResponse> out) {
        Payment recorded = service.recordPayment(
                bridge.body(request, com.mysms.finance.api.model.RecordPaymentRequest.class, "request_id"));
        ProtoBridge.send(out, bridge.reply(RecordPaymentResponse.newBuilder(), "payment", recorded).build());
    }

    @Override
    public void listPayments(ListPaymentsRequest request, StreamObserver<ListPaymentsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Payment> rows = service.listPayments(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListPaymentsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getPayment(GetPaymentRequest request, StreamObserver<GetPaymentResponse> out) {
        Payment found = service.getPayment(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetPaymentResponse.newBuilder(), "payment", found).build());
    }

    @Override
    public void quoteCheckout(QuoteCheckoutRequest request, StreamObserver<QuoteCheckoutResponse> out) {
        OrderQuote quote = service.quoteCheckout(
                bridge.body(request, com.mysms.finance.api.model.QuoteCheckoutRequest.class, "request_id"));
        ProtoBridge.send(out, bridge.reply(QuoteCheckoutResponse.newBuilder(), "result", quote).build());
    }

    @Override
    public void startCheckout(StartCheckoutRequest request, StreamObserver<StartCheckoutResponse> out) {
        CheckoutSession session = service.startCheckout(
                bridge.body(request, com.mysms.finance.api.model.StartCheckoutRequest.class, "request_id"));
        ProtoBridge.send(out, bridge.reply(StartCheckoutResponse.newBuilder(), "result", session).build());
    }

    /**
     * Not served over gRPC (UNIMPLEMENTED): the gateway calls REST, and the service authenticates a notification by
     * the signature of its exact raw body, which only the HTTP edge receives.
     */
    @Override
    public void receiveGatewayWebhook(ReceiveGatewayWebhookRequest request,
            StreamObserver<ReceiveGatewayWebhookResponse> out) {
        throw new NotImplementedException(WEBHOOK_REST_ONLY);
    }

    /**
     * finance.RefundPayment: the request_id is also the refund's own key (REST's Idempotency-Key), so a retry after
     * the gateway did not answer finds the refund it opened instead of asking the gateway again.
     */
    @Override
    public void refundPayment(RefundPaymentRequest request, StreamObserver<RefundPaymentResponse> out) {
        Refund refund = service.refundPayment(bridge.id(request.getPaymentId(), "paymentId"),
                bridge.id(request.getRequestId(), "request_id"),
                bridge.body(request, com.mysms.finance.api.model.RefundPaymentRequest.class, "payment_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(RefundPaymentResponse.newBuilder(), "refund", refund).build());
    }
}
