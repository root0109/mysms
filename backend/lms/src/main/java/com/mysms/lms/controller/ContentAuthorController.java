package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.ContentAuthorApi;
import com.mysms.lms.api.model.ContentAuthor;
import com.mysms.lms.api.model.ContentAuthorUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.ContentAuthorService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ContentAuthor operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/lms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
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
            ContentAuthorUpdate contentAuthorUpdate, String updateMask, Boolean validateOnly) {
        ContentAuthor updated =
                contentAuthorService.updateContentAuthor(contentAuthorId, ifMatch, contentAuthorUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
