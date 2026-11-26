package com.mysms.comms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.business.ClassPageService;
import com.mysms.comms.api.ClassPageApi;
import com.mysms.comms.api.model.ClassPage;
import com.mysms.comms.api.model.ClassPageAttachment;
import com.mysms.comms.api.model.ClassPageAttachmentUpdate;
import com.mysms.comms.api.model.ClassPageMember;
import com.mysms.comms.api.model.ClassPageMemberUpdate;
import com.mysms.comms.api.model.ClassPageUpdate;
import com.mysms.comms.api.model.FollowClassPageRequest;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ClassPage operations; authentication and the permission check already ran in the auth-client filter.
 */
@RestController
public class ClassPageController implements ClassPageApi {

    private final ClassPageService classPageService;

    public ClassPageController(ClassPageService classPageService) {
        this.classPageService = classPageService;
    }

    @Override
    public ResponseEntity<ClassPage> createClassPage(ClassPage classPage, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ClassPage created = classPageService.createClassPage(classPage, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/comms/class-pages/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<ClassPageAttachment> createClassPageAttachment(UUID classPageId,
            ClassPageAttachment classPageAttachment, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ClassPageAttachment created =
                classPageService.createClassPageAttachment(classPageId, classPageAttachment, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/comms/class-pages/" + classPageId + "/attachments/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<ClassPageMember> createClassPageMember(UUID classPageId,
            ClassPageMember classPageMember, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ClassPageMember created = classPageService.createClassPageMember(classPageId, classPageMember, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/comms/class-pages/" + classPageId + "/members/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteClassPage(UUID classPageId, String ifMatch) {
        classPageService.deleteClassPage(classPageId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteClassPageAttachment(UUID classPageId, UUID classPageAttachmentId,
            String ifMatch) {
        classPageService.deleteClassPageAttachment(classPageId, classPageAttachmentId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteClassPageMember(UUID classPageId, UUID classPageMemberId,
            String ifMatch) {
        classPageService.deleteClassPageMember(classPageId, classPageMemberId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ClassPageMember> followClassPage(UUID classPageId,
            FollowClassPageRequest followClassPageRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(classPageService.followClassPage(classPageId, followClassPageRequest));
    }

    @Override
    public ResponseEntity<ClassPage> getClassPage(UUID classPageId, String fields) {
        ClassPage found = classPageService.getClassPage(classPageId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ClassPageAttachment> getClassPageAttachment(UUID classPageId,
            UUID classPageAttachmentId, String fields) {
        ClassPageAttachment found =
                classPageService.getClassPageAttachment(classPageId, classPageAttachmentId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ClassPageMember> getClassPageMember(UUID classPageId, UUID classPageMemberId,
            String fields) {
        ClassPageMember found = classPageService.getClassPageMember(classPageId, classPageMemberId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<ClassPageAttachment>> listClassPageAttachments(UUID classPageId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ClassPageAttachment> list =
                PagingUtil.pagingInfo(classPageService.listClassPageAttachments(classPageId, page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<ClassPageMember>> listClassPageMembers(UUID classPageId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ClassPageMember> list =
                PagingUtil.pagingInfo(classPageService.listClassPageMembers(classPageId, page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<ClassPage>> listClassPages(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<ClassPage> list =
                PagingUtil.pagingInfo(classPageService.listClassPages(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ClassPage> updateClassPage(UUID classPageId, String ifMatch,
            ClassPageUpdate classPageUpdate, Boolean validateOnly) {
        ClassPage updated = classPageService.updateClassPage(classPageId, ifMatch, classPageUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ClassPageAttachment> updateClassPageAttachment(UUID classPageId,
            UUID classPageAttachmentId, String ifMatch, ClassPageAttachmentUpdate classPageAttachmentUpdate,
            Boolean validateOnly) {
        ClassPageAttachment updated =
                classPageService.updateClassPageAttachment(classPageId, classPageAttachmentId, ifMatch,
                        classPageAttachmentUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ClassPageMember> updateClassPageMember(UUID classPageId, UUID classPageMemberId,
            String ifMatch, ClassPageMemberUpdate classPageMemberUpdate, Boolean validateOnly) {
        ClassPageMember updated =
                classPageService.updateClassPageMember(classPageId, classPageMemberId, ifMatch,
                        classPageMemberUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
