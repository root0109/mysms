package com.mysms.hrms.payroll.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.hrms.payroll.api.PaymentTypeApi;
import com.mysms.hrms.payroll.api.model.PaymentType;
import com.mysms.hrms.payroll.api.model.PaymentTypeUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.hrms.payroll.business.PaymentTypeService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the PaymentType operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class PaymentTypeController implements PaymentTypeApi {

    private final PaymentTypeService paymentTypeService;

    public PaymentTypeController(PaymentTypeService paymentTypeService) {
        this.paymentTypeService = paymentTypeService;
    }

    @Override
    public ResponseEntity<PaymentType> createPaymentType(PaymentType paymentType, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        PaymentType created = paymentTypeService.createPaymentType(paymentType, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/payroll/payment-types/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deletePaymentType(UUID paymentTypeId, String ifMatch) {
        paymentTypeService.deletePaymentType(paymentTypeId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PaymentType> getPaymentType(UUID paymentTypeId, String fields) {
        PaymentType found = paymentTypeService.getPaymentType(paymentTypeId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<PaymentType>> listPaymentTypes(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<PaymentType> list =
                PagingUtil.pagingInfo(paymentTypeService.listPaymentTypes(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PaymentType> updatePaymentType(UUID paymentTypeId, String ifMatch,
            PaymentTypeUpdate paymentTypeUpdate, Boolean validateOnly) {
        PaymentType updated =
                paymentTypeService.updatePaymentType(paymentTypeId, ifMatch, paymentTypeUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
