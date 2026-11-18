package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.PeerReviewApi;
import com.mysms.lms.api.model.PeerReview;
import com.mysms.lms.api.model.PeerReviewUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.PeerReviewService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the PeerReview operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class PeerReviewController implements PeerReviewApi {

    private final PeerReviewService peerReviewService;

    public PeerReviewController(PeerReviewService peerReviewService) {
        this.peerReviewService = peerReviewService;
    }

    @Override
    public ResponseEntity<PeerReview> createPeerReview(PeerReview peerReview, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        PeerReview created = peerReviewService.createPeerReview(peerReview, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/peer-reviews/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deletePeerReview(UUID peerReviewId, String ifMatch) {
        peerReviewService.deletePeerReview(peerReviewId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PeerReview> getPeerReview(UUID peerReviewId, String fields) {
        PeerReview found = peerReviewService.getPeerReview(peerReviewId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<PeerReview>> listPeerReviews(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<PeerReview> list =
                PagingUtil.pagingInfo(peerReviewService.listPeerReviews(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PeerReview> updatePeerReview(UUID peerReviewId, String ifMatch,
            PeerReviewUpdate peerReviewUpdate, String updateMask, Boolean validateOnly) {
        PeerReview updated = peerReviewService.updatePeerReview(peerReviewId, ifMatch, peerReviewUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
