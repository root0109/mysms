package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.PlagiarismReportApi;
import com.mysms.lms.api.model.ExternalLink;
import com.mysms.lms.api.model.PlagiarismReport;
import com.mysms.lms.api.model.SubmitForPlagiarismCheckRequest;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.PlagiarismReportService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the PlagiarismReport operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/lms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class PlagiarismReportController implements PlagiarismReportApi {

    private final PlagiarismReportService plagiarismReportService;

    public PlagiarismReportController(PlagiarismReportService plagiarismReportService) {
        this.plagiarismReportService = plagiarismReportService;
    }

    @Override
    public ResponseEntity<PlagiarismReport> getPlagiarismReport(UUID plagiarismReportId, String fields) {
        PlagiarismReport found = plagiarismReportService.getPlagiarismReport(plagiarismReportId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ExternalLink> getPlagiarismReportLink(UUID plagiarismReportId, String view) {
        throw new NotImplementedException();
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
            String xTimestamp, Object body) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<PlagiarismReport> submitForPlagiarismCheck(
            SubmitForPlagiarismCheckRequest submitForPlagiarismCheckRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }
}
