package com.mysms.payroll.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.payroll.api.PayslipApi;
import com.mysms.payroll.api.model.CancelPayslipRequest;
import com.mysms.payroll.api.model.GeneratePayslipsRequest;
import com.mysms.payroll.api.model.MarkPayslipPaidRequest;
import com.mysms.payroll.api.model.Operation;
import com.mysms.payroll.api.model.Payslip;
import com.mysms.payroll.api.model.PayslipLine;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import com.mysms.payroll.business.PayslipService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Payslip operations: implements the generated contract interface, so paths, parameters and payloads
 * cannot drift from api/openapi/payroll.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class PayslipController implements PayslipApi {

    private final PayslipService payslipService;

    public PayslipController(PayslipService payslipService) {
        this.payslipService = payslipService;
    }

    @Override
    public ResponseEntity<Payslip> cancelPayslip(UUID payslipId,
            CancelPayslipRequest cancelPayslipRequest, UUID idempotencyKey) {
        Payslip changed = payslipService.cancelPayslip(payslipId, cancelPayslipRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Operation> generatePayslips(UUID idempotencyKey,
            GeneratePayslipsRequest generatePayslipsRequest) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Payslip> getPayslip(UUID payslipId, String fields) {
        Payslip found = payslipService.getPayslip(payslipId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PayslipLine> getPayslipLine(UUID payslipId, UUID payslipLineId, String fields) {
        PayslipLine found = payslipService.getPayslipLine(payslipId, payslipLineId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<PayslipLine>> listPayslipLines(UUID payslipId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<PayslipLine> list =
                PagingUtil.pagingInfo(payslipService.listPayslipLines(payslipId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Payslip>> listPayslips(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Payslip> list = PagingUtil.pagingInfo(payslipService.listPayslips(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Payslip> markPayslipPaid(UUID payslipId,
            MarkPayslipPaidRequest markPayslipPaidRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }
}
