package com.mysms.learning.lms.controller;

import com.mysms.learning.lms.business.PlagiarismWorkflowService;
import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.PlagiarismReportApi;
import com.mysms.learning.lms.api.model.ExternalLink;
import com.mysms.learning.lms.api.model.PlagiarismReport;
import com.mysms.learning.lms.api.model.SubmitForPlagiarismCheckRequest;
import com.mysms.core.web.ReadMask;
import com.mysms.learning.lms.business.PlagiarismReportService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the PlagiarismReport operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class PlagiarismReportController implements PlagiarismReportApi {

    private final PlagiarismReportService plagiarismReportService;
    private final PlagiarismWorkflowService plagiarismWorkflowService;

    public PlagiarismReportController(PlagiarismReportService plagiarismReportService,
            PlagiarismWorkflowService plagiarismWorkflowService) {
        this.plagiarismWorkflowService = plagiarismWorkflowService;
        this.plagiarismReportService = plagiarismReportService;
    }

    @Override
    public ResponseEntity<PlagiarismReport> getPlagiarismReport(UUID plagiarismReportId, String fields) {
        PlagiarismReport found = plagiarismReportService.getPlagiarismReport(plagiarismReportId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ExternalLink> getPlagiarismReportLink(UUID plagiarismReportId, String view) {
        return ResponseEntity.ok(plagiarismWorkflowService.getPlagiarismReportLink(plagiarismReportId, view));
    }

    @Override
    public ResponseEntity<PagingInfo<PlagiarismReport>> listPlagiarismReports(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<PlagiarismReport> list =
                PagingUtil.pagingInfo(plagiarismReportService.listPlagiarismReports(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Void> receivePlagiarismWebhook(String provider, String xSignature,
            String xTimestamp, byte[] body) {
        plagiarismWorkflowService.receivePlagiarismWebhook(provider, xSignature, xTimestamp, body);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PlagiarismReport> submitForPlagiarismCheck(
            SubmitForPlagiarismCheckRequest submitForPlagiarismCheckRequest, UUID idempotencyKey) {
        PlagiarismReport created = plagiarismWorkflowService.submitForPlagiarismCheck(
                submitForPlagiarismCheckRequest);
        return ResponseEntity.ok().eTag(created.getEtag()).body(created);
    }
}
