package com.mysms.learning.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.CohortEnrollmentApi;
import com.mysms.learning.lms.api.model.CohortEnrollment;
import com.mysms.learning.lms.api.model.CohortEnrollmentUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.learning.lms.business.CohortEnrollmentService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the CohortEnrollment operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
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
            String ifMatch, CohortEnrollmentUpdate cohortEnrollmentUpdate, Boolean validateOnly) {
        CohortEnrollment updated =
                cohortEnrollmentService.updateCohortEnrollment(cohortEnrollmentId, ifMatch, cohortEnrollmentUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
