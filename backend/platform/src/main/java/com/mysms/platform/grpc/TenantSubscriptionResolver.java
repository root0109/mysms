package com.mysms.platform.grpc;

import com.mysms.core.error.NotImplementedException;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.platform.api.model.SubscriptionCheckout;
import com.mysms.platform.api.model.TenantSubscription;
import com.mysms.platform.business.TenantSubscriptionService;
import com.mysms.platform.v1.CancelSubscriptionRequest;
import com.mysms.platform.v1.CancelSubscriptionResponse;
import com.mysms.platform.v1.ConfirmSubscriptionCheckoutRequest;
import com.mysms.platform.v1.ConfirmSubscriptionCheckoutResponse;
import com.mysms.platform.v1.GetTenantSubscriptionRequest;
import com.mysms.platform.v1.GetTenantSubscriptionResponse;
import com.mysms.platform.v1.ListTenantSubscriptionsRequest;
import com.mysms.platform.v1.ListTenantSubscriptionsResponse;
import com.mysms.platform.v1.ReceiveSubscriptionWebhookRequest;
import com.mysms.platform.v1.ReceiveSubscriptionWebhookResponse;
import com.mysms.platform.v1.StartSubscriptionCheckoutRequest;
import com.mysms.platform.v1.StartSubscriptionCheckoutResponse;
import com.mysms.platform.v1.TenantSubscriptionServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of platform.TenantSubscriptionService, delegating to the business service REST uses; the edge requires
 * an idempotency key for the checkout.
 */
@GrpcService
public class TenantSubscriptionResolver extends TenantSubscriptionServiceGrpc.TenantSubscriptionServiceImplBase {

    private final TenantSubscriptionService service;
    private final ProtoBridge bridge;

    public TenantSubscriptionResolver(TenantSubscriptionService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listTenantSubscriptions(ListTenantSubscriptionsRequest request,
            StreamObserver<ListTenantSubscriptionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<TenantSubscription> rows = service.listTenantSubscriptions(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListTenantSubscriptionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getTenantSubscription(GetTenantSubscriptionRequest request,
            StreamObserver<GetTenantSubscriptionResponse> out) {
        TenantSubscription found = service.getTenantSubscription(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetTenantSubscriptionResponse.newBuilder(), "tenant_subscription",
                found).build());
    }

    @Override
    public void startSubscriptionCheckout(StartSubscriptionCheckoutRequest request,
            StreamObserver<StartSubscriptionCheckoutResponse> out) {
        SubscriptionCheckout checkout = service.startSubscriptionCheckout(bridge.body(request,
                com.mysms.platform.api.model.StartSubscriptionCheckoutRequest.class, "request_id"));
        ProtoBridge.send(out, bridge.reply(StartSubscriptionCheckoutResponse.newBuilder(), "result", checkout).build());
    }

    @Override
    public void cancelSubscription(CancelSubscriptionRequest request, StreamObserver<CancelSubscriptionResponse> out) {
        TenantSubscription cancelled = service.cancelSubscription(
                bridge.id(request.getTenantSubscriptionId(), "tenantSubscriptionId"),
                bridge.body(request, com.mysms.platform.api.model.CancelSubscriptionRequest.class,
                        "tenant_subscription_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(CancelSubscriptionResponse.newBuilder(), "tenant_subscription",
                cancelled).build());
    }

    @Override
    public void confirmSubscriptionCheckout(ConfirmSubscriptionCheckoutRequest request,
            StreamObserver<ConfirmSubscriptionCheckoutResponse> out) {
        TenantSubscription confirmed = service.confirmSubscriptionCheckout(
                bridge.id(request.getTenantSubscriptionId(), "tenantSubscriptionId"),
                bridge.body(request, com.mysms.platform.api.model.ConfirmSubscriptionCheckoutRequest.class,
                        "tenant_subscription_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(ConfirmSubscriptionCheckoutResponse.newBuilder(), "tenant_subscription",
                confirmed).build());
    }

    /**
     * The payment gateway's callback, authenticated by its signature over the raw HTTP body: REST only, so
     * UNIMPLEMENTED over gRPC — a message re-encoded by gRPC no longer carries the bytes the gateway signed.
     */
    @Override
    public void receiveSubscriptionWebhook(ReceiveSubscriptionWebhookRequest request,
            StreamObserver<ReceiveSubscriptionWebhookResponse> out) {
        throw new NotImplementedException();
    }
}
