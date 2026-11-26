package com.mysms.academics.assessment.controller;

import com.mysms.academics.assessment.api.model.ResultCard;
import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.academics.assessment.business.MarksheetLineService;
import com.mysms.academics.assessment.api.MarksheetLineApi;
import com.mysms.academics.assessment.api.model.MarksheetLine;
import com.mysms.core.web.ReadMask;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the MarksheetLine operations; authentication and the permission check already ran in the auth-client
 * filter.
 */
@RestController
public class MarksheetLineController implements MarksheetLineApi {

    private final MarksheetLineService marksheetLineService;

    public MarksheetLineController(MarksheetLineService marksheetLineService) {
        this.marksheetLineService = marksheetLineService;
    }

    @Override
    public ResponseEntity<MarksheetLine> getMarksheetLine(UUID marksheetLineId, String fields) {
        MarksheetLine found = marksheetLineService.getMarksheetLine(marksheetLineId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<ResultCard>> getStudentResults(UUID studentId, UUID academicYearId,
            UUID examSessionId, Integer page, Integer size, String cursor) {
        return ResponseEntity.ok(PagingUtil.pagingInfo(marksheetLineService.getStudentResults(studentId,
                academicYearId, examSessionId, page, size, cursor)));
    }

    @Override
    public ResponseEntity<PagingInfo<MarksheetLine>> listMarksheetLines(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<MarksheetLine> list =
                PagingUtil.pagingInfo(marksheetLineService.listMarksheetLines(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }
}
