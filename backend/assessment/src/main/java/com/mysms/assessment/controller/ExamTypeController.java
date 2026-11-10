package com.mysms.assessment.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.assessment.business.ExamTypeService;
import com.mysms.assessment.api.ExamTypeApi;
import com.mysms.assessment.api.model.ExamType;
import com.mysms.assessment.api.model.ExamTypeUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ExamType operations: implements the generated contract interface, so paths, parameters and payloads
 * cannot drift from api/openapi/assessment.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class ExamTypeController implements ExamTypeApi {

    private final ExamTypeService examTypeService;

    public ExamTypeController(ExamTypeService examTypeService) {
        this.examTypeService = examTypeService;
    }

    @Override
    public ResponseEntity<ExamType> createExamType(ExamType examType, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ExamType created = examTypeService.createExamType(examType, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/assessment/exam-types/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteExamType(UUID examTypeId, String ifMatch) {
        examTypeService.deleteExamType(examTypeId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ExamType> getExamType(UUID examTypeId, String fields) {
        ExamType found = examTypeService.getExamType(examTypeId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<ExamType>> listExamTypes(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<ExamType> list =
                PagingUtil.pagingInfo(examTypeService.listExamTypes(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ExamType> updateExamType(UUID examTypeId, String ifMatch,
            ExamTypeUpdate examTypeUpdate, String updateMask, Boolean validateOnly) {
        ExamType updated = examTypeService.updateExamType(examTypeId, ifMatch, examTypeUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
