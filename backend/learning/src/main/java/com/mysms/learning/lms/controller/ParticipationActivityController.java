package com.mysms.learning.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.ParticipationActivityApi;
import com.mysms.learning.lms.api.model.ParticipationActivity;
import com.mysms.learning.lms.api.model.ParticipationActivityUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.learning.lms.business.ParticipationActivityService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ParticipationActivity operations; authentication and the permission check already ran in the
 * auth-client filter. Operations not served yet answer 501.
 */
@RestController
public class ParticipationActivityController implements ParticipationActivityApi {

    private final ParticipationActivityService participationActivityService;

    public ParticipationActivityController(ParticipationActivityService participationActivityService) {
        this.participationActivityService = participationActivityService;
    }

    @Override
    public ResponseEntity<ParticipationActivity> createParticipationActivity(
            ParticipationActivity participationActivity, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ParticipationActivity created =
                participationActivityService.createParticipationActivity(participationActivity, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/participation-activities/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteParticipationActivity(UUID participationActivityId,
            String ifMatch) {
        participationActivityService.deleteParticipationActivity(participationActivityId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ParticipationActivity> getParticipationActivity(UUID participationActivityId,
            String fields) {
        ParticipationActivity found = participationActivityService.getParticipationActivity(participationActivityId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<ParticipationActivity>> listParticipationActivities(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ParticipationActivity> list =
                PagingUtil.pagingInfo(participationActivityService.listParticipationActivitys(page, size, cursor,
                        filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ParticipationActivity> updateParticipationActivity(UUID participationActivityId,
            String ifMatch, ParticipationActivityUpdate participationActivityUpdate, Boolean validateOnly) {
        ParticipationActivity updated =
                participationActivityService.updateParticipationActivity(participationActivityId, ifMatch,
                        participationActivityUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
