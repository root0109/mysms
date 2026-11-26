package com.mysms.finance.controller;

import com.mysms.core.paging.PagingInfo;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.web.ReadMask;
import com.mysms.finance.api.StudentFeeDueApi;
import com.mysms.finance.api.model.GenerateFeeDuesRequest;
import com.mysms.finance.api.model.Operation;
import com.mysms.finance.api.model.StudentFeeDue;
import com.mysms.finance.api.model.StudentFeeDueUpdate;
import com.mysms.finance.business.StudentFeeDueService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** REST edge of the StudentFeeDue operations; the Idempotency-Key of a fee run is handled by core's filter. */
@RestController
public class StudentFeeDueController implements StudentFeeDueApi {

    private final StudentFeeDueService studentFeeDueService;

    public StudentFeeDueController(StudentFeeDueService studentFeeDueService) {
        this.studentFeeDueService = studentFeeDueService;
    }

    @Override
    public ResponseEntity<Operation> generateFeeDues(UUID idempotencyKey, GenerateFeeDuesRequest request) {
        return ResponseEntity.accepted().body(studentFeeDueService.generateFeeDues(request));
    }

    @Override
    public ResponseEntity<StudentFeeDue> getStudentFeeDue(UUID studentFeeDueId, String fields) {
        StudentFeeDue found = studentFeeDueService.getStudentFeeDue(studentFeeDueId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<StudentFeeDue>> listStudentFeeDues(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<StudentFeeDue> list =
                PagingUtil.pagingInfo(studentFeeDueService.listStudentFeeDues(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<StudentFeeDue> updateStudentFeeDue(UUID studentFeeDueId, String ifMatch,
            StudentFeeDueUpdate studentFeeDueUpdate, Boolean validateOnly) {
        StudentFeeDue updated = studentFeeDueService.updateStudentFeeDue(studentFeeDueId, ifMatch,
                studentFeeDueUpdate, Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
