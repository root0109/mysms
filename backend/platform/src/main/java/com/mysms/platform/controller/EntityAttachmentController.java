package com.mysms.platform.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.platform.api.EntityAttachmentApi;
import com.mysms.platform.api.model.EntityAttachment;
import com.mysms.core.web.ReadMask;
import com.mysms.platform.business.EntityAttachmentService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the EntityAttachment operations; authentication and the permission check already happened in the
 * auth-client filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class EntityAttachmentController implements EntityAttachmentApi {

    private final EntityAttachmentService entityAttachmentService;

    public EntityAttachmentController(EntityAttachmentService entityAttachmentService) {
        this.entityAttachmentService = entityAttachmentService;
    }

    @Override
    public ResponseEntity<EntityAttachment> createEntityAttachment(EntityAttachment entityAttachment,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        EntityAttachment created = entityAttachmentService.createEntityAttachment(entityAttachment, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteEntityAttachment(UUID entityAttachmentId, String ifMatch) {
        entityAttachmentService.deleteEntityAttachment(entityAttachmentId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PagingInfo<EntityAttachment>> listEntityAttachments(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<EntityAttachment> list =
                PagingUtil.pagingInfo(entityAttachmentService.listEntityAttachments(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }
}
