package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.CertificateRequestApi;
import com.mysms.lms.api.model.CancelCertificateRequestRequest;
import com.mysms.lms.api.model.CertificateEvaluation;
import com.mysms.lms.api.model.CertificateRequest;
import com.mysms.lms.api.model.CertificateRequestUpdate;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.CertificateRequestService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the CertificateRequest operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/lms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
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
        throw new NotImplementedException();
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
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<CertificateRequest> updateCertificateRequest(UUID certificateRequestId,
            String ifMatch, CertificateRequestUpdate certificateRequestUpdate, String updateMask,
            Boolean validateOnly) {
        CertificateRequest updated =
                certificateRequestService.updateCertificateRequest(certificateRequestId, ifMatch,
                        certificateRequestUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
