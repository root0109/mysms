package com.mysms.platform.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.platform.api.TenantSubscriptionApi;
import com.mysms.platform.api.model.CancelSubscriptionRequest;
import com.mysms.platform.api.model.ConfirmSubscriptionCheckoutRequest;
import com.mysms.platform.api.model.StartSubscriptionCheckoutRequest;
import com.mysms.platform.api.model.SubscriptionCheckout;
import com.mysms.platform.api.model.TenantSubscription;
import com.mysms.core.web.ReadMask;
import com.mysms.platform.business.TenantSubscriptionService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the TenantSubscription operations; authentication and the permission check already happened in the
 * auth-client filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class TenantSubscriptionController implements TenantSubscriptionApi {

    private final TenantSubscriptionService tenantSubscriptionService;

    public TenantSubscriptionController(TenantSubscriptionService tenantSubscriptionService) {
        this.tenantSubscriptionService = tenantSubscriptionService;
    }

    @Override
    public ResponseEntity<TenantSubscription> cancelSubscription(UUID tenantSubscriptionId,
            CancelSubscriptionRequest cancelSubscriptionRequest, UUID idempotencyKey) {
        TenantSubscription cancelled =
                tenantSubscriptionService.cancelSubscription(tenantSubscriptionId, cancelSubscriptionRequest);
        return ResponseEntity.ok().eTag(cancelled.getEtag()).body(cancelled);
    }

    @Override
    public ResponseEntity<TenantSubscription> confirmSubscriptionCheckout(UUID tenantSubscriptionId,
            ConfirmSubscriptionCheckoutRequest confirmSubscriptionCheckoutRequest, UUID idempotencyKey) {
        TenantSubscription confirmed = tenantSubscriptionService.confirmSubscriptionCheckout(tenantSubscriptionId,
                confirmSubscriptionCheckoutRequest);
        return ResponseEntity.ok().eTag(confirmed.getEtag()).body(confirmed);
    }

    @Override
    public ResponseEntity<TenantSubscription> getTenantSubscription(UUID tenantSubscriptionId,
            String fields) {
        TenantSubscription found = tenantSubscriptionService.getTenantSubscription(tenantSubscriptionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<TenantSubscription>> listTenantSubscriptions(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<TenantSubscription> list =
                PagingUtil.pagingInfo(tenantSubscriptionService.listTenantSubscriptions(page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<SubscriptionCheckout> startSubscriptionCheckout(UUID idempotencyKey,
            StartSubscriptionCheckoutRequest startSubscriptionCheckoutRequest) {
        return ResponseEntity.ok(tenantSubscriptionService.startSubscriptionCheckout(startSubscriptionCheckoutRequest));
    }

    @Override
    public ResponseEntity<Void> receiveSubscriptionWebhook(String signature, byte[] body) {
        tenantSubscriptionService.receiveSubscriptionWebhook(signature, body);
        return ResponseEntity.noContent().build();
    }
}
