package com.mysms.org.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.org.api.AcademicYearApi;
import com.mysms.org.api.model.AcademicTerm;
import com.mysms.org.api.model.AcademicTermList;
import com.mysms.org.api.model.AcademicTermUpdate;
import com.mysms.org.api.model.AcademicYear;
import com.mysms.org.api.model.AcademicYearUpdate;
import com.mysms.org.api.model.ActivateAcademicYearRequest;
import com.mysms.org.api.model.CloseAcademicYearRequest;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import com.mysms.org.business.AcademicYearService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the AcademicYear operations; authentication and the permission check already happened in the auth-client
 * filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class AcademicYearController implements AcademicYearApi {

    private final AcademicYearService academicYearService;

    public AcademicYearController(AcademicYearService academicYearService) {
        this.academicYearService = academicYearService;
    }

    @Override
    public ResponseEntity<AcademicYear> activateAcademicYear(UUID academicYearId,
            ActivateAcademicYearRequest activateAcademicYearRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<AcademicYear> closeAcademicYear(UUID academicYearId,
            CloseAcademicYearRequest closeAcademicYearRequest, UUID idempotencyKey) {
        AcademicYear changed = academicYearService.closeAcademicYear(academicYearId, closeAcademicYearRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<AcademicTerm> createAcademicTerm(UUID academicYearId, AcademicTerm academicTerm,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        AcademicTerm created = academicYearService.createAcademicTerm(academicYearId, academicTerm, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/org/academic-years/" + academicYearId + "/terms/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<AcademicYear> createAcademicYear(AcademicYear academicYear, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        AcademicYear created = academicYearService.createAcademicYear(academicYear, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/org/academic-years/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteAcademicTerm(UUID academicYearId, UUID academicTermId,
            String ifMatch) {
        academicYearService.deleteAcademicTerm(academicYearId, academicTermId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteAcademicYear(UUID academicYearId, String ifMatch) {
        academicYearService.deleteAcademicYear(academicYearId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<AcademicTermList> generateTerms(UUID academicYearId, Object body,
            UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<AcademicTerm> getAcademicTerm(UUID academicYearId, UUID academicTermId,
            String fields) {
        AcademicTerm found = academicYearService.getAcademicTerm(academicYearId, academicTermId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<AcademicYear> getAcademicYear(UUID academicYearId, String fields) {
        AcademicYear found = academicYearService.getAcademicYear(academicYearId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<AcademicTerm>> listAcademicTerms(UUID academicYearId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<AcademicTerm> list =
                PagingUtil.pagingInfo(academicYearService.listAcademicTerms(academicYearId, page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<AcademicYear>> listAcademicYears(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<AcademicYear> list =
                PagingUtil.pagingInfo(academicYearService.listAcademicYears(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<AcademicTerm> updateAcademicTerm(UUID academicYearId, UUID academicTermId,
            String ifMatch, AcademicTermUpdate academicTermUpdate, String updateMask, Boolean validateOnly) {
        AcademicTerm updated =
                academicYearService.updateAcademicTerm(academicYearId, academicTermId, ifMatch, academicTermUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<AcademicYear> updateAcademicYear(UUID academicYearId, String ifMatch,
            AcademicYearUpdate academicYearUpdate, String updateMask, Boolean validateOnly) {
        AcademicYear updated =
                academicYearService.updateAcademicYear(academicYearId, ifMatch, academicYearUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
