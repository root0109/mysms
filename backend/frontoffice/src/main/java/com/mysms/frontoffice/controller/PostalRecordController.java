package com.mysms.frontoffice.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.frontoffice.api.PostalRecordApi;
import com.mysms.frontoffice.api.model.PostalRecord;
import com.mysms.frontoffice.api.model.PostalRecordUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.frontoffice.business.PostalRecordService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the PostalRecord operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class PostalRecordController implements PostalRecordApi {

    private final PostalRecordService postalRecordService;

    public PostalRecordController(PostalRecordService postalRecordService) {
        this.postalRecordService = postalRecordService;
    }

    @Override
    public ResponseEntity<PostalRecord> createPostalRecord(PostalRecord postalRecord, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        PostalRecord created = postalRecordService.createPostalRecord(postalRecord, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/frontoffice/postal-records/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deletePostalRecord(UUID postalRecordId, String ifMatch) {
        postalRecordService.deletePostalRecord(postalRecordId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PostalRecord> getPostalRecord(UUID postalRecordId, String fields) {
        PostalRecord found = postalRecordService.getPostalRecord(postalRecordId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<PostalRecord>> listPostalRecords(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<PostalRecord> list =
                PagingUtil.pagingInfo(postalRecordService.listPostalRecords(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PostalRecord> updatePostalRecord(UUID postalRecordId, String ifMatch,
            PostalRecordUpdate postalRecordUpdate, String updateMask, Boolean validateOnly) {
        PostalRecord updated =
                postalRecordService.updatePostalRecord(postalRecordId, ifMatch, postalRecordUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
