package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.ContentViewSessionApi;
import com.mysms.lms.api.model.ContentViewSession;
import com.mysms.lms.api.model.ContentViewSessionUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.ContentViewSessionService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ContentViewSession operations; authentication and the permission check already ran in the
 * auth-client filter. Operations not served yet answer 501.
 */
@RestController
public class ContentViewSessionController implements ContentViewSessionApi {

    private final ContentViewSessionService contentViewSessionService;

    public ContentViewSessionController(ContentViewSessionService contentViewSessionService) {
        this.contentViewSessionService = contentViewSessionService;
    }

    @Override
    public ResponseEntity<ContentViewSession> createContentViewSession(
            ContentViewSession contentViewSession, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ContentViewSession created = contentViewSessionService.createContentViewSession(contentViewSession, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/content-view-sessions/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<ContentViewSession> getContentViewSession(UUID contentViewSessionId,
            String fields) {
        ContentViewSession found = contentViewSessionService.getContentViewSession(contentViewSessionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<ContentViewSession>> listContentViewSessions(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ContentViewSession> list =
                PagingUtil.pagingInfo(contentViewSessionService.listContentViewSessions(page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ContentViewSession> updateContentViewSession(UUID contentViewSessionId,
            String ifMatch, ContentViewSessionUpdate contentViewSessionUpdate, String updateMask,
            Boolean validateOnly) {
        ContentViewSession updated =
                contentViewSessionService.updateContentViewSession(contentViewSessionId, ifMatch,
                        contentViewSessionUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
