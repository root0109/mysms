package com.mysms.platform.controller;

import com.mysms.core.paging.PagingInfo;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.web.ReadMask;
import com.mysms.platform.api.TagApi;
import com.mysms.platform.api.model.Tag;
import com.mysms.platform.api.model.TagUpdate;
import com.mysms.platform.business.TagService;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Tag operations; authentication and the permission check already happened in the auth-client filter.
 */
@RestController
public class TagController implements TagApi {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @Override
    public ResponseEntity<Tag> createTag(Tag tag, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Tag created = tagService.createTag(tag, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/platform/tags/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteTag(UUID tagId, String ifMatch) {
        tagService.deleteTag(tagId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Tag> getTag(UUID tagId, String fields) {
        Tag found = tagService.getTag(tagId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Tag>> listTags(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Tag> list = PagingUtil.pagingInfo(tagService.listTags(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Tag> updateTag(UUID tagId, String ifMatch, TagUpdate tagUpdate, Boolean validateOnly) {
        Tag updated = tagService.updateTag(tagId, ifMatch, tagUpdate, Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
