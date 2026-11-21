package com.mysms.privacy.controller;

import com.mysms.core.paging.PagingInfo;
import com.mysms.core.paging.PagingUtil;
import com.mysms.privacy.api.MyConsentApi;
import com.mysms.privacy.api.model.MyConsent;
import com.mysms.privacy.api.model.MyNotice;
import com.mysms.privacy.api.model.RecordMyConsentRequest;
import com.mysms.privacy.api.model.WithdrawMyConsentRequest;
import com.mysms.privacy.business.ConsentService;
import com.mysms.privacy.business.NoticeService;
import com.mysms.privacy.model.RequestOrigin;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the consent centre (the caller's own consents and the notices shown with them); authentication and the
 * permission check already ran in the auth-client filter. It hands the services the client's address and User-Agent,
 * which consent evidence records.
 */
@RestController
public class MyConsentController implements MyConsentApi {

    private final ConsentService consentService;
    private final NoticeService noticeService;
    private final HttpServletRequest request;

    public MyConsentController(ConsentService consentService, NoticeService noticeService,
            HttpServletRequest request) {
        this.consentService = consentService;
        this.noticeService = noticeService;
        this.request = request;
    }

    @Override
    public ResponseEntity<MyNotice> getMyNotice(String noticeCode, String language) {
        return ResponseEntity.ok(noticeService.getMyNotice(noticeCode, language));
    }

    @Override
    public ResponseEntity<PagingInfo<MyConsent>> listMyConsents() {
        return ResponseEntity.ok(PagingUtil.pagingInfo(consentService.listMyConsents()));
    }

    @Override
    public ResponseEntity<MyConsent> recordMyConsent(RecordMyConsentRequest recordMyConsentRequest,
            UUID idempotencyKey) {
        MyConsent recorded = consentService.recordMyConsent(recordMyConsentRequest, origin());
        return ResponseEntity.status(HttpStatus.CREATED).eTag(recorded.getEtag()).body(recorded);
    }

    @Override
    public ResponseEntity<MyConsent> withdrawMyConsent(UUID consentRecordId,
            WithdrawMyConsentRequest withdrawMyConsentRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(consentService.withdrawMyConsent(consentRecordId, withdrawMyConsentRequest,
                origin()));
    }

    private RequestOrigin origin() {
        return RequestOrigin.of(request.getRemoteAddr(), request.getHeader(HttpHeaders.USER_AGENT));
    }
}
