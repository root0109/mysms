package com.mysms.finance.controller;

import com.mysms.core.paging.PagingInfo;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.web.ReadMask;
import com.mysms.finance.api.RefundApi;
import com.mysms.finance.api.model.Refund;
import com.mysms.finance.business.RefundService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** REST edge of the Refund reads. */
@RestController
public class RefundController implements RefundApi {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @Override
    public ResponseEntity<Refund> getRefund(UUID refundId, String fields) {
        Refund found = refundService.getRefund(refundId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Refund>> listRefunds(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Refund> list = PagingUtil.pagingInfo(refundService.listRefunds(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }
}
