package com.mysms.payroll.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.payroll.api.PayrollPaymentApi;
import com.mysms.payroll.api.model.PayrollPayment;
import com.mysms.core.web.ReadMask;
import com.mysms.payroll.business.PayrollPaymentService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the PayrollPayment operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/payroll.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class PayrollPaymentController implements PayrollPaymentApi {

    private final PayrollPaymentService payrollPaymentService;

    public PayrollPaymentController(PayrollPaymentService payrollPaymentService) {
        this.payrollPaymentService = payrollPaymentService;
    }

    @Override
    public ResponseEntity<PayrollPayment> createPayrollPayment(PayrollPayment payrollPayment,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        PayrollPayment created = payrollPaymentService.createPayrollPayment(payrollPayment, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/payroll/payroll-payments/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<PayrollPayment> getPayrollPayment(UUID payrollPaymentId, String fields) {
        PayrollPayment found = payrollPaymentService.getPayrollPayment(payrollPaymentId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<PayrollPayment>> listPayrollPayments(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<PayrollPayment> list =
                PagingUtil.pagingInfo(payrollPaymentService.listPayrollPayments(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }
}
