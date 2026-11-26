package com.mysms.learning.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.CertificateApi;
import com.mysms.learning.lms.api.model.Certificate;
import com.mysms.learning.lms.api.model.CertificateVerification;
import com.mysms.learning.lms.api.model.DownloadLink;
import com.mysms.core.web.ReadMask;
import com.mysms.learning.lms.business.CertificateService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Certificate operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class CertificateController implements CertificateApi {

    private final CertificateService certificateService;

    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @Override
    public ResponseEntity<DownloadLink> downloadCertificate(UUID certificateId) {
        return ResponseEntity.ok(certificateService.downloadCertificate(certificateId));
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
        return ResponseEntity.ok(certificateService.verifyCertificate(certificateId));
    }
}
