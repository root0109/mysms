package com.mysms.privacy.controller;

import com.mysms.core.paging.PagingInfo;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.web.ReadMask;
import com.mysms.privacy.api.ConsentRecordApi;
import com.mysms.privacy.api.model.ConsentRecord;
import com.mysms.privacy.api.model.RecordConsentOnBehalfRequest;
import com.mysms.privacy.api.model.VerifyConsentRecordRequest;
import com.mysms.privacy.business.ConsentService;
import com.mysms.privacy.model.RequestOrigin;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the staff's consent records (paper forms recorded on a giver's behalf, their verification, lists);
 * authentication and the permission check already ran in the auth-client filter. It hands the service the client's
 * address and User-Agent, which consent evidence records.
 */
@RestController
public class ConsentRecordController implements ConsentRecordApi {

    private static final String CONSENT_RECORDS = "/v1/privacy/consent-records/";

    private final ConsentService consentService;
    private final HttpServletRequest request;

    public ConsentRecordController(ConsentService consentService, HttpServletRequest request) {
        this.consentService = consentService;
        this.request = request;
    }

    @Override
    public ResponseEntity<ConsentRecord> getConsentRecord(UUID consentRecordId, String fields) {
        ConsentRecord found = consentService.getConsentRecord(consentRecordId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<ConsentRecord>> listConsentRecords(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<ConsentRecord> list =
                PagingUtil.pagingInfo(consentService.listConsentRecords(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ConsentRecord> recordConsentOnBehalf(RecordConsentOnBehalfRequest recordConsentOnBehalfRequest,
            UUID idempotencyKey) {
        ConsentRecord recorded = consentService.recordConsentOnBehalf(recordConsentOnBehalfRequest, origin());
        return ResponseEntity.created(URI.create(CONSENT_RECORDS + recorded.getId())).eTag(recorded.getEtag())
                .body(recorded);
    }

    @Override
    public ResponseEntity<ConsentRecord> verifyConsentRecord(UUID consentRecordId,
            VerifyConsentRecordRequest verifyConsentRecordRequest, UUID idempotencyKey) {
        ConsentRecord verified = consentService.verifyConsentRecord(consentRecordId, verifyConsentRecordRequest,
                origin());
        return ResponseEntity.ok().eTag(verified.getEtag()).body(verified);
    }

    private RequestOrigin origin() {
        return RequestOrigin.of(request.getRemoteAddr(), request.getHeader(HttpHeaders.USER_AGENT));
    }
}
