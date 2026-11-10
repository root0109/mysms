package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.CertificateEvaluationApi;
import com.mysms.lms.api.model.Certificate;
import com.mysms.lms.api.model.CertificateEvaluation;
import com.mysms.lms.api.model.CertificateEvaluationUpdate;
import com.mysms.lms.api.model.IssueCertificateFromEvaluationRequest;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.CertificateEvaluationService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the CertificateEvaluation operations: implements the generated contract interface, so paths, parameters
 * and payloads
 * cannot drift from api/openapi/lms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class CertificateEvaluationController implements CertificateEvaluationApi {

    private final CertificateEvaluationService certificateEvaluationService;

    public CertificateEvaluationController(CertificateEvaluationService certificateEvaluationService) {
        this.certificateEvaluationService = certificateEvaluationService;
    }

    @Override
    public ResponseEntity<CertificateEvaluation> createCertificateEvaluation(
            CertificateEvaluation certificateEvaluation, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        CertificateEvaluation created =
                certificateEvaluationService.createCertificateEvaluation(certificateEvaluation, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/certificate-evaluations/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteCertificateEvaluation(UUID certificateEvaluationId,
            String ifMatch) {
        certificateEvaluationService.deleteCertificateEvaluation(certificateEvaluationId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<CertificateEvaluation> getCertificateEvaluation(UUID certificateEvaluationId,
            String fields) {
        CertificateEvaluation found = certificateEvaluationService.getCertificateEvaluation(certificateEvaluationId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<Certificate> issueCertificateFromEvaluation(UUID certificateEvaluationId,
            IssueCertificateFromEvaluationRequest issueCertificateFromEvaluationRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<PagingInfo<CertificateEvaluation>> listCertificateEvaluations(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<CertificateEvaluation> list =
                PagingUtil.pagingInfo(certificateEvaluationService.listCertificateEvaluations(page, size, cursor,
                        filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<CertificateEvaluation> updateCertificateEvaluation(UUID certificateEvaluationId,
            String ifMatch, CertificateEvaluationUpdate certificateEvaluationUpdate, String updateMask,
            Boolean validateOnly) {
        CertificateEvaluation updated =
                certificateEvaluationService.updateCertificateEvaluation(certificateEvaluationId, ifMatch,
                        certificateEvaluationUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
