package com.mysms.sis.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.sis.api.StudentActivityApi;
import com.mysms.sis.api.model.StudentActivity;
import com.mysms.sis.api.model.StudentActivityUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.sis.business.StudentActivityService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the StudentActivity operations; authentication and the permission check already happened in the
 * auth-client filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class StudentActivityController implements StudentActivityApi {

    private final StudentActivityService studentActivityService;

    public StudentActivityController(StudentActivityService studentActivityService) {
        this.studentActivityService = studentActivityService;
    }

    @Override
    public ResponseEntity<StudentActivity> createStudentActivity(StudentActivity studentActivity,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        StudentActivity created = studentActivityService.createStudentActivity(studentActivity, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/sis/student-activities/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteStudentActivity(UUID studentActivityId, String ifMatch) {
        studentActivityService.deleteStudentActivity(studentActivityId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<StudentActivity> getStudentActivity(UUID studentActivityId, String fields) {
        StudentActivity found = studentActivityService.getStudentActivity(studentActivityId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<StudentActivity>> listStudentActivities(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<StudentActivity> list =
                PagingUtil.pagingInfo(studentActivityService.listStudentActivitys(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<StudentActivity> updateStudentActivity(UUID studentActivityId, String ifMatch,
            StudentActivityUpdate studentActivityUpdate, String updateMask, Boolean validateOnly) {
        StudentActivity updated =
                studentActivityService.updateStudentActivity(studentActivityId, ifMatch, studentActivityUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
