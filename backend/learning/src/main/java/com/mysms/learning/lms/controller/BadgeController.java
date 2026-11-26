package com.mysms.learning.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.BadgeApi;
import com.mysms.learning.lms.api.model.AwardBadgeRequest;
import com.mysms.learning.lms.api.model.Badge;
import com.mysms.learning.lms.api.model.BadgeAward;
import com.mysms.learning.lms.api.model.BadgeUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.learning.lms.business.BadgeService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Badge operations; authentication and the permission check already ran in the auth-client filter.
 * Operations not served yet answer 501.
 */
@RestController
public class BadgeController implements BadgeApi {

    private final BadgeService badgeService;

    public BadgeController(BadgeService badgeService) {
        this.badgeService = badgeService;
    }

    @Override
    public ResponseEntity<BadgeAward> awardBadge(UUID badgeId, AwardBadgeRequest awardBadgeRequest,
            UUID idempotencyKey) {
        BadgeAward awarded = badgeService.awardBadge(badgeId, awardBadgeRequest.getUserId());
        return ResponseEntity.ok().eTag(awarded.getEtag()).body(awarded);
    }

    @Override
    public ResponseEntity<Badge> createBadge(Badge badge, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Badge created = badgeService.createBadge(badge, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/badges/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteBadge(UUID badgeId, String ifMatch) {
        badgeService.deleteBadge(badgeId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Badge> getBadge(UUID badgeId, String fields) {
        Badge found = badgeService.getBadge(badgeId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Badge>> listBadges(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Badge> list = PagingUtil.pagingInfo(badgeService.listBadges(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Badge> updateBadge(UUID badgeId, String ifMatch, BadgeUpdate badgeUpdate,
            Boolean validateOnly) {
        Badge updated = badgeService.updateBadge(badgeId, ifMatch, badgeUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
