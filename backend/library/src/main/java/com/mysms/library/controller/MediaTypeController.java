package com.mysms.library.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.library.api.MediaTypeApi;
import com.mysms.library.api.model.MediaType;
import com.mysms.library.api.model.MediaTypeUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.library.business.MediaTypeService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the MediaType operations; authentication and the permission check already ran in the auth-client filter.
 * Operations not served yet answer 501.
 */
@RestController
public class MediaTypeController implements MediaTypeApi {

    private final MediaTypeService mediaTypeService;

    public MediaTypeController(MediaTypeService mediaTypeService) {
        this.mediaTypeService = mediaTypeService;
    }

    @Override
    public ResponseEntity<MediaType> createMediaType(MediaType mediaType, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        MediaType created = mediaTypeService.createMediaType(mediaType, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/library/media-types/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteMediaType(UUID mediaTypeId, String ifMatch) {
        mediaTypeService.deleteMediaType(mediaTypeId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<MediaType> getMediaType(UUID mediaTypeId, String fields) {
        MediaType found = mediaTypeService.getMediaType(mediaTypeId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<MediaType>> listMediaTypes(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<MediaType> list =
                PagingUtil.pagingInfo(mediaTypeService.listMediaTypes(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<MediaType> updateMediaType(UUID mediaTypeId, String ifMatch,
            MediaTypeUpdate mediaTypeUpdate, String updateMask, Boolean validateOnly) {
        MediaType updated = mediaTypeService.updateMediaType(mediaTypeId, ifMatch, mediaTypeUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
