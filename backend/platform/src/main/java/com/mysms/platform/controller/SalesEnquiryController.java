package com.mysms.platform.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.platform.api.SalesEnquiryApi;
import com.mysms.platform.api.model.MarkEnquiryContactedRequest;
import com.mysms.platform.api.model.SalesEnquiry;
import com.mysms.platform.api.model.SalesEnquiryUpdate;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import com.mysms.platform.business.SalesEnquiryService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the SalesEnquiry operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/platform.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class SalesEnquiryController implements SalesEnquiryApi {

    private final SalesEnquiryService salesEnquiryService;

    public SalesEnquiryController(SalesEnquiryService salesEnquiryService) {
        this.salesEnquiryService = salesEnquiryService;
    }

    @Override
    public ResponseEntity<SalesEnquiry> createSalesEnquiry(SalesEnquiry salesEnquiry, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        SalesEnquiry created = salesEnquiryService.createSalesEnquiry(salesEnquiry, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/platform/sales-enquiries/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteSalesEnquiry(UUID salesEnquiryId, String ifMatch) {
        salesEnquiryService.deleteSalesEnquiry(salesEnquiryId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<SalesEnquiry> getSalesEnquiry(UUID salesEnquiryId, String fields) {
        SalesEnquiry found = salesEnquiryService.getSalesEnquiry(salesEnquiryId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<SalesEnquiry>> listSalesEnquiries(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<SalesEnquiry> list =
                PagingUtil.pagingInfo(salesEnquiryService.listSalesEnquirys(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<SalesEnquiry> markEnquiryContacted(UUID salesEnquiryId,
            MarkEnquiryContactedRequest markEnquiryContactedRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<SalesEnquiry> updateSalesEnquiry(UUID salesEnquiryId, String ifMatch,
            SalesEnquiryUpdate salesEnquiryUpdate, String updateMask, Boolean validateOnly) {
        SalesEnquiry updated =
                salesEnquiryService.updateSalesEnquiry(salesEnquiryId, ifMatch, salesEnquiryUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
