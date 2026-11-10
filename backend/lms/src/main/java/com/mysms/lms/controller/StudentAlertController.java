package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.StudentAlertApi;
import com.mysms.lms.api.model.StudentAlert;
import com.mysms.lms.api.model.StudentAlertUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.StudentAlertService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the StudentAlert operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/lms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class StudentAlertController implements StudentAlertApi {

    private final StudentAlertService studentAlertService;

    public StudentAlertController(StudentAlertService studentAlertService) {
        this.studentAlertService = studentAlertService;
    }

    @Override
    public ResponseEntity<StudentAlert> createStudentAlert(StudentAlert studentAlert, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        StudentAlert created = studentAlertService.createStudentAlert(studentAlert, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/student-alerts/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteStudentAlert(UUID studentAlertId, String ifMatch) {
        studentAlertService.deleteStudentAlert(studentAlertId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<StudentAlert> getStudentAlert(UUID studentAlertId, String fields) {
        StudentAlert found = studentAlertService.getStudentAlert(studentAlertId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<StudentAlert>> listStudentAlerts(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<StudentAlert> list =
                PagingUtil.pagingInfo(studentAlertService.listStudentAlerts(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<StudentAlert> updateStudentAlert(UUID studentAlertId, String ifMatch,
            StudentAlertUpdate studentAlertUpdate, String updateMask, Boolean validateOnly) {
        StudentAlert updated =
                studentAlertService.updateStudentAlert(studentAlertId, ifMatch, studentAlertUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
