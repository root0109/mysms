package com.mysms.sis.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.sis.api.StudentLeaveRequestApi;
import com.mysms.sis.api.model.StudentLeaveDecisionRequest;
import com.mysms.sis.api.model.StudentLeaveRequest;
import com.mysms.sis.api.model.StudentLeaveRequestUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.sis.business.StudentLeaveRequestService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the StudentLeaveRequest operations: implements the generated contract interface, so paths, parameters
 * and payloads
 * cannot drift from api/openapi/sis.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class StudentLeaveRequestController implements StudentLeaveRequestApi {

    private final StudentLeaveRequestService studentLeaveRequestService;

    public StudentLeaveRequestController(StudentLeaveRequestService studentLeaveRequestService) {
        this.studentLeaveRequestService = studentLeaveRequestService;
    }

    @Override
    public ResponseEntity<StudentLeaveRequest> approveStudentLeaveRequest(UUID studentLeaveRequestId,
            String ifMatch, UUID idempotencyKey, StudentLeaveDecisionRequest studentLeaveDecisionRequest) {
        StudentLeaveRequest changed =
                studentLeaveRequestService.approveStudentLeaveRequest(studentLeaveRequestId, ifMatch,
                        studentLeaveDecisionRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<StudentLeaveRequest> createStudentLeaveRequest(
            StudentLeaveRequest studentLeaveRequest, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        StudentLeaveRequest created = studentLeaveRequestService.createStudentLeaveRequest(studentLeaveRequest, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/sis/student-leave-requests/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteStudentLeaveRequest(UUID studentLeaveRequestId, String ifMatch) {
        studentLeaveRequestService.deleteStudentLeaveRequest(studentLeaveRequestId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<StudentLeaveRequest> getStudentLeaveRequest(UUID studentLeaveRequestId,
            String fields) {
        StudentLeaveRequest found = studentLeaveRequestService.getStudentLeaveRequest(studentLeaveRequestId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<StudentLeaveRequest>> listStudentLeaveRequests(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<StudentLeaveRequest> list =
                PagingUtil.pagingInfo(studentLeaveRequestService.listStudentLeaveRequests(page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<StudentLeaveRequest> rejectStudentLeaveRequest(UUID studentLeaveRequestId,
            String ifMatch, UUID idempotencyKey, StudentLeaveDecisionRequest studentLeaveDecisionRequest) {
        StudentLeaveRequest changed =
                studentLeaveRequestService.rejectStudentLeaveRequest(studentLeaveRequestId, ifMatch,
                        studentLeaveDecisionRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<StudentLeaveRequest> updateStudentLeaveRequest(UUID studentLeaveRequestId,
            String ifMatch, StudentLeaveRequestUpdate studentLeaveRequestUpdate, String updateMask,
            Boolean validateOnly) {
        StudentLeaveRequest updated =
                studentLeaveRequestService.updateStudentLeaveRequest(studentLeaveRequestId, ifMatch,
                        studentLeaveRequestUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
