package com.mysms.identity.people.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.identity.people.api.QualificationApi;
import com.mysms.identity.people.api.model.Qualification;
import com.mysms.identity.people.api.model.QualificationUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.identity.people.business.QualificationService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Qualification operations; authentication and the permission check already happened in the
 * auth-client filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class QualificationController implements QualificationApi {

    private final QualificationService qualificationService;

    public QualificationController(QualificationService qualificationService) {
        this.qualificationService = qualificationService;
    }

    @Override
    public ResponseEntity<Qualification> createQualification(Qualification qualification,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Qualification created = qualificationService.createQualification(qualification, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/people/qualifications/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteQualification(UUID qualificationId, String ifMatch) {
        qualificationService.deleteQualification(qualificationId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Qualification> getQualification(UUID qualificationId, String fields) {
        Qualification found = qualificationService.getQualification(qualificationId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Qualification>> listQualifications(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<Qualification> list =
                PagingUtil.pagingInfo(qualificationService.listQualifications(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Qualification> updateQualification(UUID qualificationId, String ifMatch,
            QualificationUpdate qualificationUpdate, Boolean validateOnly) {
        Qualification updated =
                qualificationService.updateQualification(qualificationId, ifMatch, qualificationUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
