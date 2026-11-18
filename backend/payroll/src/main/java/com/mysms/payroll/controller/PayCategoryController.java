package com.mysms.payroll.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.payroll.api.PayCategoryApi;
import com.mysms.payroll.api.model.PayCategory;
import com.mysms.payroll.api.model.PayCategoryUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.payroll.business.PayCategoryService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the PayCategory operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class PayCategoryController implements PayCategoryApi {

    private final PayCategoryService payCategoryService;

    public PayCategoryController(PayCategoryService payCategoryService) {
        this.payCategoryService = payCategoryService;
    }

    @Override
    public ResponseEntity<PayCategory> createPayCategory(PayCategory payCategory, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        PayCategory created = payCategoryService.createPayCategory(payCategory, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/payroll/pay-categories/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deletePayCategory(UUID payCategoryId, String ifMatch) {
        payCategoryService.deletePayCategory(payCategoryId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PayCategory> getPayCategory(UUID payCategoryId, String fields) {
        PayCategory found = payCategoryService.getPayCategory(payCategoryId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<PayCategory>> listPayCategories(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<PayCategory> list =
                PagingUtil.pagingInfo(payCategoryService.listPayCategorys(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PayCategory> updatePayCategory(UUID payCategoryId, String ifMatch,
            PayCategoryUpdate payCategoryUpdate, String updateMask, Boolean validateOnly) {
        PayCategory updated =
                payCategoryService.updatePayCategory(payCategoryId, ifMatch, payCategoryUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
