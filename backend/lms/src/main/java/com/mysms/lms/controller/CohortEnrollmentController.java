package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.CohortEnrollmentApi;
import com.mysms.lms.api.model.CohortEnrollment;
import com.mysms.lms.api.model.CohortEnrollmentUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.CohortEnrollmentService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the CohortEnrollment operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/lms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class CohortEnrollmentController implements CohortEnrollmentApi {

    private final CohortEnrollmentService cohortEnrollmentService;

    public CohortEnrollmentController(CohortEnrollmentService cohortEnrollmentService) {
        this.cohortEnrollmentService = cohortEnrollmentService;
    }

    @Override
    public ResponseEntity<Void> deleteCohortEnrollment(UUID cohortEnrollmentId, String ifMatch) {
        cohortEnrollmentService.deleteCohortEnrollment(cohortEnrollmentId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<CohortEnrollment> getCohortEnrollment(UUID cohortEnrollmentId, String fields) {
        CohortEnrollment found = cohortEnrollmentService.getCohortEnrollment(cohortEnrollmentId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<CohortEnrollment>> listCohortEnrollments(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<CohortEnrollment> list =
                PagingUtil.pagingInfo(cohortEnrollmentService.listCohortEnrollments(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<CohortEnrollment> updateCohortEnrollment(UUID cohortEnrollmentId,
            String ifMatch, CohortEnrollmentUpdate cohortEnrollmentUpdate, String updateMask, Boolean validateOnly) {
        CohortEnrollment updated =
                cohortEnrollmentService.updateCohortEnrollment(cohortEnrollmentId, ifMatch, cohortEnrollmentUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
