package com.mysms.sis.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.sis.api.StudentHealthRecordApi;
import com.mysms.sis.api.model.StudentHealthRecord;
import com.mysms.sis.api.model.StudentHealthRecordUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.sis.business.StudentHealthRecordService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the StudentHealthRecord operations; authentication and the permission check already happened in the
 * auth-client filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class StudentHealthRecordController implements StudentHealthRecordApi {

    private final StudentHealthRecordService studentHealthRecordService;

    public StudentHealthRecordController(StudentHealthRecordService studentHealthRecordService) {
        this.studentHealthRecordService = studentHealthRecordService;
    }

    @Override
    public ResponseEntity<StudentHealthRecord> createStudentHealthRecord(
            StudentHealthRecord studentHealthRecord, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        StudentHealthRecord created = studentHealthRecordService.createStudentHealthRecord(studentHealthRecord, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/sis/student-health-records/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteStudentHealthRecord(UUID studentHealthRecordId, String ifMatch) {
        studentHealthRecordService.deleteStudentHealthRecord(studentHealthRecordId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<StudentHealthRecord> getStudentHealthRecord(UUID studentHealthRecordId,
            String fields) {
        StudentHealthRecord found = studentHealthRecordService.getStudentHealthRecord(studentHealthRecordId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<StudentHealthRecord>> listStudentHealthRecords(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<StudentHealthRecord> list =
                PagingUtil.pagingInfo(studentHealthRecordService.listStudentHealthRecords(page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<StudentHealthRecord> updateStudentHealthRecord(UUID studentHealthRecordId,
            String ifMatch, StudentHealthRecordUpdate studentHealthRecordUpdate, String updateMask,
            Boolean validateOnly) {
        StudentHealthRecord updated =
                studentHealthRecordService.updateStudentHealthRecord(studentHealthRecordId, ifMatch,
                        studentHealthRecordUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
