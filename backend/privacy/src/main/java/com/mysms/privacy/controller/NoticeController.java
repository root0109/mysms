package com.mysms.privacy.controller;

import com.mysms.core.paging.PagingInfo;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.web.ReadMask;
import com.mysms.privacy.api.NoticeApi;
import com.mysms.privacy.api.model.Notice;
import com.mysms.privacy.api.model.NoticeTranslation;
import com.mysms.privacy.api.model.NoticeTranslationUpdate;
import com.mysms.privacy.api.model.NoticeUpdate;
import com.mysms.privacy.api.model.NoticeVersion;
import com.mysms.privacy.api.model.NoticeVersionUpdate;
import com.mysms.privacy.api.model.PublishNoticeVersionRequest;
import com.mysms.privacy.business.NoticeService;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Notice operations (notices, their versions and translations); authentication and the permission
 * check already ran in the auth-client filter.
 */
@RestController
public class NoticeController implements NoticeApi {

    private static final String NOTICES = "/v1/privacy/notices/";

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @Override
    public ResponseEntity<Notice> createNotice(Notice notice, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Notice created = noticeService.createNotice(notice, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create(NOTICES + created.getId())).eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<NoticeTranslation> createNoticeTranslation(UUID noticeId, UUID noticeVersionId,
            NoticeTranslation noticeTranslation, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        NoticeTranslation created =
                noticeService.createNoticeTranslation(noticeId, noticeVersionId, noticeTranslation, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create(NOTICES + noticeId + "/versions/" + noticeVersionId
                + "/translations/" + created.getId())).eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<NoticeVersion> createNoticeVersion(UUID noticeId, NoticeVersion noticeVersion,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        NoticeVersion created = noticeService.createNoticeVersion(noticeId, noticeVersion, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create(NOTICES + noticeId + "/versions/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteNotice(UUID noticeId, String ifMatch) {
        noticeService.deleteNotice(noticeId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteNoticeTranslation(UUID noticeId, UUID noticeVersionId, UUID noticeTranslationId,
            String ifMatch) {
        noticeService.deleteNoticeTranslation(noticeId, noticeVersionId, noticeTranslationId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Notice> getNotice(UUID noticeId, String fields) {
        Notice found = noticeService.getNotice(noticeId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<NoticeTranslation> getNoticeTranslation(UUID noticeId, UUID noticeVersionId,
            UUID noticeTranslationId, String fields) {
        NoticeTranslation found = noticeService.getNoticeTranslation(noticeId, noticeVersionId, noticeTranslationId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<NoticeVersion> getNoticeVersion(UUID noticeId, UUID noticeVersionId, String fields) {
        NoticeVersion found = noticeService.getNoticeVersion(noticeId, noticeVersionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<NoticeTranslation>> listNoticeTranslations(UUID noticeId, UUID noticeVersionId,
            Integer page, Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<NoticeTranslation> list = PagingUtil.pagingInfo(noticeService.listNoticeTranslations(noticeId,
                noticeVersionId, page, size, cursor, filter, sort));
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
    public ResponseEntity<NoticeVersion> publishNoticeVersion(UUID noticeId, UUID noticeVersionId,
            PublishNoticeVersionRequest publishNoticeVersionRequest, UUID idempotencyKey) {
        NoticeVersion changed =
                noticeService.publishNoticeVersion(noticeId, noticeVersionId, publishNoticeVersionRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Notice> updateNotice(UUID noticeId, String ifMatch, NoticeUpdate noticeUpdate,
            String updateMask, Boolean validateOnly) {
        Notice updated = noticeService.updateNotice(noticeId, ifMatch, noticeUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<NoticeTranslation> updateNoticeTranslation(UUID noticeId, UUID noticeVersionId,
            UUID noticeTranslationId, String ifMatch, NoticeTranslationUpdate noticeTranslationUpdate,
            String updateMask, Boolean validateOnly) {
        NoticeTranslation updated = noticeService.updateNoticeTranslation(noticeId, noticeVersionId,
                noticeTranslationId, ifMatch, noticeTranslationUpdate, updateMask, Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<NoticeVersion> updateNoticeVersion(UUID noticeId, UUID noticeVersionId, String ifMatch,
            NoticeVersionUpdate noticeVersionUpdate, String updateMask, Boolean validateOnly) {
        NoticeVersion updated = noticeService.updateNoticeVersion(noticeId, noticeVersionId, ifMatch,
                noticeVersionUpdate, updateMask, Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
