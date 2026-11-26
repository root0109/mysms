package com.mysms.academics.assessment.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.academics.assessment.business.ExamResultService;
import com.mysms.academics.assessment.api.ExamResultApi;
import com.mysms.academics.assessment.api.model.ExamResult;
import com.mysms.academics.assessment.api.model.ExamResultUpdate;
import com.mysms.core.web.ReadMask;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ExamResult operations; authentication and the permission check already ran in the auth-client
 * filter.
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
            ExamResultUpdate examResultUpdate, Boolean validateOnly) {
        ExamResult updated = examResultService.updateExamResult(examResultId, ifMatch, examResultUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
