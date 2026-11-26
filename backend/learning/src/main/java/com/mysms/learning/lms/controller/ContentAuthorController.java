package com.mysms.learning.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.ContentAuthorApi;
import com.mysms.learning.lms.api.model.ContentAuthor;
import com.mysms.learning.lms.api.model.ContentAuthorUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.learning.lms.business.ContentAuthorService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ContentAuthor operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class ContentAuthorController implements ContentAuthorApi {

    private final ContentAuthorService contentAuthorService;

    public ContentAuthorController(ContentAuthorService contentAuthorService) {
        this.contentAuthorService = contentAuthorService;
    }

    @Override
    public ResponseEntity<ContentAuthor> createContentAuthor(ContentAuthor contentAuthor,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ContentAuthor created = contentAuthorService.createContentAuthor(contentAuthor, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/content-authors/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteContentAuthor(UUID contentAuthorId, String ifMatch) {
        contentAuthorService.deleteContentAuthor(contentAuthorId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ContentAuthor> getContentAuthor(UUID contentAuthorId, String fields) {
        ContentAuthor found = contentAuthorService.getContentAuthor(contentAuthorId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<ContentAuthor>> listContentAuthors(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<ContentAuthor> list =
                PagingUtil.pagingInfo(contentAuthorService.listContentAuthors(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ContentAuthor> updateContentAuthor(UUID contentAuthorId, String ifMatch,
            ContentAuthorUpdate contentAuthorUpdate, Boolean validateOnly) {
        ContentAuthor updated =
                contentAuthorService.updateContentAuthor(contentAuthorId, ifMatch, contentAuthorUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
