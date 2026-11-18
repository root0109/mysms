package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.MentorshipApi;
import com.mysms.lms.api.model.Mentorship;
import com.mysms.lms.api.model.MentorshipUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.MentorshipService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Mentorship operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class MentorshipController implements MentorshipApi {

    private final MentorshipService mentorshipService;

    public MentorshipController(MentorshipService mentorshipService) {
        this.mentorshipService = mentorshipService;
    }

    @Override
    public ResponseEntity<Mentorship> createMentorship(Mentorship mentorship, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Mentorship created = mentorshipService.createMentorship(mentorship, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/mentorships/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteMentorship(UUID mentorshipId, String ifMatch) {
        mentorshipService.deleteMentorship(mentorshipId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Mentorship> getMentorship(UUID mentorshipId, String fields) {
        Mentorship found = mentorshipService.getMentorship(mentorshipId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Mentorship>> listMentorships(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<Mentorship> list =
                PagingUtil.pagingInfo(mentorshipService.listMentorships(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Mentorship> updateMentorship(UUID mentorshipId, String ifMatch,
            MentorshipUpdate mentorshipUpdate, String updateMask, Boolean validateOnly) {
        Mentorship updated = mentorshipService.updateMentorship(mentorshipId, ifMatch, mentorshipUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
