package com.mysms.platform.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.platform.api.TenantSubscriptionApi;
import com.mysms.platform.api.model.CancelSubscriptionRequest;
import com.mysms.platform.api.model.StartSubscriptionCheckoutRequest;
import com.mysms.platform.api.model.SubscriptionCheckout;
import com.mysms.platform.api.model.TenantSubscription;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import com.mysms.platform.business.TenantSubscriptionService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the TenantSubscription operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/platform.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
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
        throw new NotImplementedException();
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
        throw new NotImplementedException();
    }
}
