package com.mysms.comms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.business.NoticeBackgroundService;
import com.mysms.comms.api.NoticeBackgroundApi;
import com.mysms.comms.api.model.NoticeBackground;
import com.mysms.comms.api.model.NoticeBackgroundUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the NoticeBackground operations; authentication and the permission check already ran in the auth-client
 * filter.
 */
@RestController
public class NoticeBackgroundController implements NoticeBackgroundApi {

    private final NoticeBackgroundService noticeBackgroundService;

    public NoticeBackgroundController(NoticeBackgroundService noticeBackgroundService) {
        this.noticeBackgroundService = noticeBackgroundService;
    }

    @Override
    public ResponseEntity<NoticeBackground> createNoticeBackground(NoticeBackground noticeBackground,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        NoticeBackground created = noticeBackgroundService.createNoticeBackground(noticeBackground, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/comms/notice-backgrounds/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteNoticeBackground(UUID noticeBackgroundId, String ifMatch) {
        noticeBackgroundService.deleteNoticeBackground(noticeBackgroundId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<NoticeBackground> getNoticeBackground(UUID noticeBackgroundId, String fields) {
        NoticeBackground found = noticeBackgroundService.getNoticeBackground(noticeBackgroundId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<NoticeBackground>> listNoticeBackgrounds(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<NoticeBackground> list =
                PagingUtil.pagingInfo(noticeBackgroundService.listNoticeBackgrounds(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<NoticeBackground> updateNoticeBackground(UUID noticeBackgroundId,
            String ifMatch, NoticeBackgroundUpdate noticeBackgroundUpdate, String updateMask, Boolean validateOnly) {
        NoticeBackground updated =
                noticeBackgroundService.updateNoticeBackground(noticeBackgroundId, ifMatch, noticeBackgroundUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
