package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.CertificateApi;
import com.mysms.lms.api.model.Certificate;
import com.mysms.lms.api.model.CertificateVerification;
import com.mysms.lms.api.model.DownloadLink;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.CertificateService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Certificate operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/lms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class CertificateController implements CertificateApi {

    private final CertificateService certificateService;

    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @Override
    public ResponseEntity<DownloadLink> downloadCertificate(UUID certificateId) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Certificate> getCertificate(UUID certificateId, String fields) {
        Certificate found = certificateService.getCertificate(certificateId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Certificate>> listCertificates(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<Certificate> list =
                PagingUtil.pagingInfo(certificateService.listCertificates(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<CertificateVerification> verifyCertificate(UUID certificateId) {
        throw new NotImplementedException();
    }
}
