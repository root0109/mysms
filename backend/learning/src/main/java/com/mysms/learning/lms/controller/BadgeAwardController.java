package com.mysms.learning.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.BadgeAwardApi;
import com.mysms.learning.lms.api.model.BadgeAward;
import com.mysms.core.web.ReadMask;
import com.mysms.learning.lms.business.BadgeAwardService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the BadgeAward operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class BadgeAwardController implements BadgeAwardApi {

    private final BadgeAwardService badgeAwardService;

    public BadgeAwardController(BadgeAwardService badgeAwardService) {
        this.badgeAwardService = badgeAwardService;
    }

    @Override
    public ResponseEntity<BadgeAward> getBadgeAward(UUID badgeAwardId, String fields) {
        BadgeAward found = badgeAwardService.getBadgeAward(badgeAwardId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<BadgeAward>> listBadgeAwards(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<BadgeAward> list =
                PagingUtil.pagingInfo(badgeAwardService.listBadgeAwards(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }
}
