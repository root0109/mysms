package com.mysms.learning.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.ForumPostReportApi;
import com.mysms.learning.lms.api.model.ForumPostReport;
import com.mysms.learning.lms.api.model.ForumPostReportUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.learning.lms.business.ForumPostReportService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ForumPostReport operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class ForumPostReportController implements ForumPostReportApi {

    private final ForumPostReportService forumPostReportService;

    public ForumPostReportController(ForumPostReportService forumPostReportService) {
        this.forumPostReportService = forumPostReportService;
    }

    @Override
    public ResponseEntity<ForumPostReport> createForumPostReport(ForumPostReport forumPostReport,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ForumPostReport created = forumPostReportService.createForumPostReport(forumPostReport, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/forum-post-reports/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteForumPostReport(UUID forumPostReportId, String ifMatch) {
        forumPostReportService.deleteForumPostReport(forumPostReportId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ForumPostReport> getForumPostReport(UUID forumPostReportId, String fields) {
        ForumPostReport found = forumPostReportService.getForumPostReport(forumPostReportId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<ForumPostReport>> listForumPostReports(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<ForumPostReport> list =
                PagingUtil.pagingInfo(forumPostReportService.listForumPostReports(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ForumPostReport> updateForumPostReport(UUID forumPostReportId, String ifMatch,
            ForumPostReportUpdate forumPostReportUpdate, Boolean validateOnly) {
        ForumPostReport updated =
                forumPostReportService.updateForumPostReport(forumPostReportId, ifMatch, forumPostReportUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
