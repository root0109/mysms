package com.mysms.comms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.business.ContentViewService;
import com.mysms.comms.api.ContentViewApi;
import com.mysms.comms.api.model.ContentView;
import com.mysms.comms.api.model.ContentViewUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ContentView operations; authentication and the permission check already ran in the auth-client
 * filter.
 */
@RestController
public class ContentViewController implements ContentViewApi {

    private final ContentViewService contentViewService;

    public ContentViewController(ContentViewService contentViewService) {
        this.contentViewService = contentViewService;
    }

    @Override
    public ResponseEntity<ContentView> createContentView(ContentView contentView, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ContentView created = contentViewService.createContentView(contentView, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/comms/content-views/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteContentView(UUID contentViewId, String ifMatch) {
        contentViewService.deleteContentView(contentViewId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ContentView> getContentView(UUID contentViewId, String fields) {
        ContentView found = contentViewService.getContentView(contentViewId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<ContentView>> listContentViews(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<ContentView> list =
                PagingUtil.pagingInfo(contentViewService.listContentViews(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ContentView> updateContentView(UUID contentViewId, String ifMatch,
            ContentViewUpdate contentViewUpdate, Boolean validateOnly) {
        ContentView updated =
                contentViewService.updateContentView(contentViewId, ifMatch, contentViewUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
