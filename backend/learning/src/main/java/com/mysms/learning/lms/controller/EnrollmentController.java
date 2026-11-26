package com.mysms.learning.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.EnrollmentApi;
import com.mysms.learning.lms.api.model.Enrollment;
import com.mysms.learning.lms.api.model.EnrollmentUpdate;
import com.mysms.learning.lms.api.model.LmsStatistics;
import com.mysms.learning.lms.api.model.MemberProfile;
import com.mysms.learning.lms.api.model.StudentDashboard;
import com.mysms.core.web.ReadMask;
import com.mysms.learning.lms.business.EnrollmentService;
import com.mysms.learning.lms.business.MemberProfileService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Enrollment operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class EnrollmentController implements EnrollmentApi {

    private final EnrollmentService enrollmentService;
    private final MemberProfileService memberProfileService;

    public EnrollmentController(EnrollmentService enrollmentService, MemberProfileService memberProfileService) {
        this.memberProfileService = memberProfileService;
        this.enrollmentService = enrollmentService;
    }

    @Override
    public ResponseEntity<Void> deleteEnrollment(UUID enrollmentId, String ifMatch) {
        enrollmentService.deleteEnrollment(enrollmentId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Enrollment> getEnrollment(UUID enrollmentId, String fields) {
        Enrollment found = enrollmentService.getEnrollment(enrollmentId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<LmsStatistics> getLmsStatistics() {
        return ResponseEntity.ok(enrollmentService.getLmsStatistics());
    }

    @Override
    public ResponseEntity<MemberProfile> getMemberProfile(UUID userId) {
        return ResponseEntity.ok(memberProfileService.getMemberProfile(userId));
    }

    @Override
    public ResponseEntity<StudentDashboard> getStudentDashboard() {
        return ResponseEntity.ok(enrollmentService.getStudentDashboard());
    }

    @Override
    public ResponseEntity<PagingInfo<Enrollment>> listEnrollments(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<Enrollment> list =
                PagingUtil.pagingInfo(enrollmentService.listEnrollments(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Enrollment> updateEnrollment(UUID enrollmentId, String ifMatch,
            EnrollmentUpdate enrollmentUpdate, Boolean validateOnly) {
        Enrollment updated = enrollmentService.updateEnrollment(enrollmentId, ifMatch, enrollmentUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
