package com.mysms.comms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.business.NoticeService;
import com.mysms.comms.api.NoticeApi;
import com.mysms.comms.api.model.CloseNoticeRepliesRequest;
import com.mysms.comms.api.model.DeactivateNoticeRequest;
import com.mysms.comms.api.model.Notice;
import com.mysms.comms.api.model.NoticeAudience;
import com.mysms.comms.api.model.NoticeAudienceUpdate;
import com.mysms.comms.api.model.NoticeUpdate;
import com.mysms.comms.api.model.NoticeVersion;
import com.mysms.comms.api.model.PublishNoticeRequest;
import com.mysms.comms.api.model.ReactivateNoticeRequest;
import com.mysms.comms.api.model.UnpublishNoticeRequest;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Notice operations; authentication and the permission check already ran in the auth-client filter.
 */
@RestController
public class NoticeController implements NoticeApi {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @Override
    public ResponseEntity<Notice> closeNoticeReplies(UUID noticeId,
            CloseNoticeRepliesRequest closeNoticeRepliesRequest, UUID idempotencyKey) {
        Notice changed = noticeService.closeNoticeReplies(noticeId, closeNoticeRepliesRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Notice> createNotice(Notice notice, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Notice created = noticeService.createNotice(notice, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/comms/notices/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<NoticeAudience> createNoticeAudience(UUID noticeId,
            NoticeAudience noticeAudience, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        NoticeAudience created = noticeService.createNoticeAudience(noticeId, noticeAudience, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/comms/notices/" + noticeId + "/audiences/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Notice> deactivateNotice(UUID noticeId,
            DeactivateNoticeRequest deactivateNoticeRequest, UUID idempotencyKey) {
        Notice changed = noticeService.deactivateNotice(noticeId, deactivateNoticeRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Void> deleteNotice(UUID noticeId, String ifMatch) {
        noticeService.deleteNotice(noticeId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteNoticeAudience(UUID noticeId, UUID noticeAudienceId,
            String ifMatch) {
        noticeService.deleteNoticeAudience(noticeId, noticeAudienceId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Notice> getNotice(UUID noticeId, String fields) {
        Notice found = noticeService.getNotice(noticeId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<NoticeAudience> getNoticeAudience(UUID noticeId, UUID noticeAudienceId,
            String fields) {
        NoticeAudience found = noticeService.getNoticeAudience(noticeId, noticeAudienceId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<NoticeVersion> getNoticeVersion(UUID noticeId, UUID noticeVersionId,
            String fields) {
        NoticeVersion found = noticeService.getNoticeVersion(noticeId, noticeVersionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<NoticeAudience>> listNoticeAudiences(UUID noticeId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<NoticeAudience> list =
                PagingUtil.pagingInfo(noticeService.listNoticeAudiences(noticeId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<NoticeVersion>> listNoticeVersions(UUID noticeId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<NoticeVersion> list =
                PagingUtil.pagingInfo(noticeService.listNoticeVersions(noticeId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Notice>> listNotices(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Notice> list = PagingUtil.pagingInfo(noticeService.listNotices(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Notice> publishNotice(UUID noticeId, PublishNoticeRequest publishNoticeRequest,
            UUID idempotencyKey) {
        Notice changed = noticeService.publishNotice(noticeId, publishNoticeRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Notice> reactivateNotice(UUID noticeId,
            ReactivateNoticeRequest reactivateNoticeRequest, UUID idempotencyKey) {
        Notice changed = noticeService.reactivateNotice(noticeId, reactivateNoticeRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Notice> unpublishNotice(UUID noticeId,
            UnpublishNoticeRequest unpublishNoticeRequest, UUID idempotencyKey) {
        Notice changed = noticeService.unpublishNotice(noticeId, unpublishNoticeRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Notice> updateNotice(UUID noticeId, String ifMatch, NoticeUpdate noticeUpdate,
            Boolean validateOnly) {
        Notice updated = noticeService.updateNotice(noticeId, ifMatch, noticeUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<NoticeAudience> updateNoticeAudience(UUID noticeId, UUID noticeAudienceId,
            String ifMatch, NoticeAudienceUpdate noticeAudienceUpdate, Boolean validateOnly) {
        NoticeAudience updated =
                noticeService.updateNoticeAudience(noticeId, noticeAudienceId, ifMatch, noticeAudienceUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
