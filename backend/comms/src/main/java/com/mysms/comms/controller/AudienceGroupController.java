package com.mysms.comms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.business.AudienceGroupService;
import com.mysms.comms.api.AudienceGroupApi;
import com.mysms.comms.api.model.AddGroupMembersRequest;
import com.mysms.comms.api.model.AudienceGroup;
import com.mysms.comms.api.model.AudienceGroupMember;
import com.mysms.comms.api.model.AudienceGroupMemberList;
import com.mysms.comms.api.model.AudienceGroupMemberUpdate;
import com.mysms.comms.api.model.AudienceGroupUpdate;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the AudienceGroup operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class AudienceGroupController implements AudienceGroupApi {

    private final AudienceGroupService audienceGroupService;

    public AudienceGroupController(AudienceGroupService audienceGroupService) {
        this.audienceGroupService = audienceGroupService;
    }

    @Override
    public ResponseEntity<AudienceGroupMemberList> addGroupMembers(UUID audienceGroupId,
            AddGroupMembersRequest addGroupMembersRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<AudienceGroup> createAudienceGroup(AudienceGroup audienceGroup,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        AudienceGroup created = audienceGroupService.createAudienceGroup(audienceGroup, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/comms/audience-groups/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<AudienceGroupMember> createAudienceGroupMember(UUID audienceGroupId,
            AudienceGroupMember audienceGroupMember, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        AudienceGroupMember created =
                audienceGroupService.createAudienceGroupMember(audienceGroupId, audienceGroupMember, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/comms/audience-groups/" + audienceGroupId + "/members/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteAudienceGroup(UUID audienceGroupId, String ifMatch) {
        audienceGroupService.deleteAudienceGroup(audienceGroupId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteAudienceGroupMember(UUID audienceGroupId,
            UUID audienceGroupMemberId, String ifMatch) {
        audienceGroupService.deleteAudienceGroupMember(audienceGroupId, audienceGroupMemberId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<AudienceGroup> getAudienceGroup(UUID audienceGroupId, String fields) {
        AudienceGroup found = audienceGroupService.getAudienceGroup(audienceGroupId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<AudienceGroupMember> getAudienceGroupMember(UUID audienceGroupId,
            UUID audienceGroupMemberId, String fields) {
        AudienceGroupMember found =
                audienceGroupService.getAudienceGroupMember(audienceGroupId, audienceGroupMemberId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<AudienceGroupMember>> listAudienceGroupMembers(UUID audienceGroupId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<AudienceGroupMember> list =
                PagingUtil.pagingInfo(audienceGroupService.listAudienceGroupMembers(audienceGroupId, page, size, cursor,
                        filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<AudienceGroup>> listAudienceGroups(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<AudienceGroup> list =
                PagingUtil.pagingInfo(audienceGroupService.listAudienceGroups(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<AudienceGroup> updateAudienceGroup(UUID audienceGroupId, String ifMatch,
            AudienceGroupUpdate audienceGroupUpdate, String updateMask, Boolean validateOnly) {
        AudienceGroup updated =
                audienceGroupService.updateAudienceGroup(audienceGroupId, ifMatch, audienceGroupUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<AudienceGroupMember> updateAudienceGroupMember(UUID audienceGroupId,
            UUID audienceGroupMemberId, String ifMatch, AudienceGroupMemberUpdate audienceGroupMemberUpdate,
            String updateMask, Boolean validateOnly) {
        AudienceGroupMember updated =
                audienceGroupService.updateAudienceGroupMember(audienceGroupId, audienceGroupMemberId, ifMatch,
                        audienceGroupMemberUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
