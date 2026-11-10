package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.LiveClassApi;
import com.mysms.lms.api.model.ExternalLink;
import com.mysms.lms.api.model.LiveClass;
import com.mysms.lms.api.model.LiveClassParticipant;
import com.mysms.lms.api.model.LiveClassParticipantList;
import com.mysms.lms.api.model.LiveClassUpdate;
import com.mysms.lms.api.model.ScheduleLiveClassRequest;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.LiveClassService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the LiveClass operations: implements the generated contract interface, so paths, parameters and payloads
 * cannot drift from api/openapi/lms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class LiveClassController implements LiveClassApi {

    private final LiveClassService liveClassService;

    public LiveClassController(LiveClassService liveClassService) {
        this.liveClassService = liveClassService;
    }

    @Override
    public ResponseEntity<LiveClass> createLiveClass(LiveClass liveClass, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        LiveClass created = liveClassService.createLiveClass(liveClass, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/live-classes/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteLiveClass(UUID liveClassId, String ifMatch) {
        liveClassService.deleteLiveClass(liveClassId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<LiveClass> endLiveClass(UUID liveClassId, Object body, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<LiveClass> getLiveClass(UUID liveClassId, String fields) {
        LiveClass found = liveClassService.getLiveClass(liveClassId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<LiveClassParticipant> getLiveClassParticipant(UUID liveClassId,
            UUID liveClassParticipantId, String fields) {
        LiveClassParticipant found =
                liveClassService.getLiveClassParticipant(liveClassId, liveClassParticipantId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ExternalLink> getLiveClassStartUrl(UUID liveClassId) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<ExternalLink> joinLiveClass(UUID liveClassId, Object body,
            UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<PagingInfo<LiveClassParticipant>> listLiveClassParticipants(UUID liveClassId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<LiveClassParticipant> list =
                PagingUtil.pagingInfo(liveClassService.listLiveClassParticipants(liveClassId, page, size, cursor,
                        filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<LiveClass>> listLiveClasses(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<LiveClass> list =
                PagingUtil.pagingInfo(liveClassService.listLiveClasss(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<LiveClass> retryLiveClassInvites(UUID liveClassId, Object body,
            UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<LiveClass> scheduleLiveClass(ScheduleLiveClassRequest scheduleLiveClassRequest,
            UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<LiveClassParticipantList> syncLiveClassAttendance(UUID liveClassId, Object body,
            UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<LiveClass> updateLiveClass(UUID liveClassId, String ifMatch,
            LiveClassUpdate liveClassUpdate, String updateMask, Boolean validateOnly) {
        LiveClass updated = liveClassService.updateLiveClass(liveClassId, ifMatch, liveClassUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
