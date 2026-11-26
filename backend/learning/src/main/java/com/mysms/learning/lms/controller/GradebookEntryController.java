package com.mysms.learning.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.GradebookEntryApi;
import com.mysms.learning.lms.api.model.BatchGradeEntriesRequest;
import com.mysms.learning.lms.api.model.GradeEntryRequest;
import com.mysms.learning.lms.api.model.GradeEntryResultList;
import com.mysms.learning.lms.api.model.GradebookEntry;
import com.mysms.core.web.ReadMask;
import com.mysms.learning.lms.business.GradebookEntryService;
import com.mysms.learning.lms.business.GradebookGradingService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the GradebookEntry operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class GradebookEntryController implements GradebookEntryApi {

    private final GradebookEntryService gradebookEntryService;
    private final GradebookGradingService gradebookGradingService;

    public GradebookEntryController(GradebookEntryService gradebookEntryService,
            GradebookGradingService gradebookGradingService) {
        this.gradebookEntryService = gradebookEntryService;
        this.gradebookGradingService = gradebookGradingService;
    }

    @Override
    public ResponseEntity<GradeEntryResultList> batchGradeEntries(
            BatchGradeEntriesRequest batchGradeEntriesRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(gradebookGradingService.batchGradeEntries(batchGradeEntriesRequest));
    }

    @Override
    public ResponseEntity<GradebookEntry> getGradebookEntry(UUID gradebookEntryId, String fields) {
        GradebookEntry found = gradebookEntryService.getGradebookEntry(gradebookEntryId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<GradebookEntry> gradeEntry(GradeEntryRequest gradeEntryRequest,
            UUID idempotencyKey) {
        GradebookEntry graded = gradebookGradingService.gradeEntry(gradeEntryRequest);
        return ResponseEntity.ok().eTag(graded.getEtag()).body(graded);
    }

    @Override
    public ResponseEntity<PagingInfo<GradebookEntry>> listGradebookEntries(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<GradebookEntry> list =
                PagingUtil.pagingInfo(gradebookEntryService.listGradebookEntrys(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }
}
