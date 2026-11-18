package com.mysms.library.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.library.api.MediaCategoryApi;
import com.mysms.library.api.model.MediaCategory;
import com.mysms.library.api.model.MediaCategoryUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.library.business.MediaCategoryService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the MediaCategory operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class MediaCategoryController implements MediaCategoryApi {

    private final MediaCategoryService mediaCategoryService;

    public MediaCategoryController(MediaCategoryService mediaCategoryService) {
        this.mediaCategoryService = mediaCategoryService;
    }

    @Override
    public ResponseEntity<MediaCategory> createMediaCategory(MediaCategory mediaCategory,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        MediaCategory created = mediaCategoryService.createMediaCategory(mediaCategory, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/library/media-categories/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteMediaCategory(UUID mediaCategoryId, String ifMatch) {
        mediaCategoryService.deleteMediaCategory(mediaCategoryId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<MediaCategory> getMediaCategory(UUID mediaCategoryId, String fields) {
        MediaCategory found = mediaCategoryService.getMediaCategory(mediaCategoryId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<MediaCategory>> listMediaCategories(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<MediaCategory> list =
                PagingUtil.pagingInfo(mediaCategoryService.listMediaCategorys(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<MediaCategory> updateMediaCategory(UUID mediaCategoryId, String ifMatch,
            MediaCategoryUpdate mediaCategoryUpdate, String updateMask, Boolean validateOnly) {
        MediaCategory updated =
                mediaCategoryService.updateMediaCategory(mediaCategoryId, ifMatch, mediaCategoryUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
