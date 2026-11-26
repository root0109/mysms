package com.mysms.learning.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.CertificateRequestApi;
import com.mysms.learning.lms.api.model.CancelCertificateRequestRequest;
import com.mysms.learning.lms.api.model.CertificateEvaluation;
import com.mysms.learning.lms.api.model.CertificateRequest;
import com.mysms.learning.lms.api.model.CertificateRequestUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.learning.lms.business.CertificateRequestService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the CertificateRequest operations; authentication and the permission check already ran in the
 * auth-client filter. Operations not served yet answer 501.
 */
@RestController
public class CertificateRequestController implements CertificateRequestApi {

    private final CertificateRequestService certificateRequestService;

    public CertificateRequestController(CertificateRequestService certificateRequestService) {
        this.certificateRequestService = certificateRequestService;
    }

    @Override
    public ResponseEntity<CertificateRequest> cancelCertificateRequest(UUID certificateRequestId,
            CancelCertificateRequestRequest cancelCertificateRequestRequest, UUID idempotencyKey) {
        CertificateRequest changed = certificateRequestService.cancelCertificateRequest(certificateRequestId,
                cancelCertificateRequestRequest == null ? null : cancelCertificateRequestRequest.getEtag(),
                cancelCertificateRequestRequest == null ? null : cancelCertificateRequestRequest.getReason());
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<CertificateRequest> createCertificateRequest(
            CertificateRequest certificateRequest, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        CertificateRequest created = certificateRequestService.createCertificateRequest(certificateRequest, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/certificate-requests/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteCertificateRequest(UUID certificateRequestId, String ifMatch) {
        certificateRequestService.deleteCertificateRequest(certificateRequestId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<CertificateRequest> getCertificateRequest(UUID certificateRequestId,
            String fields) {
        CertificateRequest found = certificateRequestService.getCertificateRequest(certificateRequestId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<CertificateRequest>> listCertificateRequests(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<CertificateRequest> list =
                PagingUtil.pagingInfo(certificateRequestService.listCertificateRequests(page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<CertificateEvaluation> startEvaluation(UUID certificateRequestId, Object body,
            UUID idempotencyKey) {
        CertificateEvaluation started = certificateRequestService.startEvaluation(certificateRequestId);
        return ResponseEntity.ok().eTag(started.getEtag()).body(started);
    }

    @Override
    public ResponseEntity<CertificateRequest> updateCertificateRequest(UUID certificateRequestId,
            String ifMatch, CertificateRequestUpdate certificateRequestUpdate, Boolean validateOnly) {
        CertificateRequest updated =
                certificateRequestService.updateCertificateRequest(certificateRequestId, ifMatch,
                        certificateRequestUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
