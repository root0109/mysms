package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.GradebookEntryApi;
import com.mysms.lms.api.model.BatchGradeEntriesRequest;
import com.mysms.lms.api.model.GradeEntryRequest;
import com.mysms.lms.api.model.GradeEntryResultList;
import com.mysms.lms.api.model.GradebookEntry;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.GradebookEntryService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the GradebookEntry operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/lms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class GradebookEntryController implements GradebookEntryApi {

    private final GradebookEntryService gradebookEntryService;

    public GradebookEntryController(GradebookEntryService gradebookEntryService) {
        this.gradebookEntryService = gradebookEntryService;
    }

    @Override
    public ResponseEntity<GradeEntryResultList> batchGradeEntries(
            BatchGradeEntriesRequest batchGradeEntriesRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<GradebookEntry> getGradebookEntry(UUID gradebookEntryId, String fields) {
        GradebookEntry found = gradebookEntryService.getGradebookEntry(gradebookEntryId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<GradebookEntry> gradeEntry(GradeEntryRequest gradeEntryRequest,
            UUID idempotencyKey) {
        throw new NotImplementedException();
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
