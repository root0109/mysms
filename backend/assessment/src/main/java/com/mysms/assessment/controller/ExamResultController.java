package com.mysms.assessment.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.assessment.business.ExamResultService;
import com.mysms.assessment.api.ExamResultApi;
import com.mysms.assessment.api.model.ExamResult;
import com.mysms.assessment.api.model.ExamResultUpdate;
import com.mysms.core.web.ReadMask;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ExamResult operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/assessment.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class ExamResultController implements ExamResultApi {

    private final ExamResultService examResultService;

    public ExamResultController(ExamResultService examResultService) {
        this.examResultService = examResultService;
    }

    @Override
    public ResponseEntity<ExamResult> getExamResult(UUID examResultId, String fields) {
        ExamResult found = examResultService.getExamResult(examResultId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<ExamResult>> listExamResults(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<ExamResult> list =
                PagingUtil.pagingInfo(examResultService.listExamResults(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ExamResult> updateExamResult(UUID examResultId, String ifMatch,
            ExamResultUpdate examResultUpdate, String updateMask, Boolean validateOnly) {
        ExamResult updated = examResultService.updateExamResult(examResultId, ifMatch, examResultUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
