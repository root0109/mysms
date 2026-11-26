package com.mysms.platform.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.platform.api.EntityNoteApi;
import com.mysms.platform.api.model.EntityNote;
import com.mysms.platform.api.model.EntityNoteUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.platform.business.EntityNoteService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the EntityNote operations; authentication and the permission check already happened in the auth-client
 * filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class EntityNoteController implements EntityNoteApi {

    private final EntityNoteService entityNoteService;

    public EntityNoteController(EntityNoteService entityNoteService) {
        this.entityNoteService = entityNoteService;
    }

    @Override
    public ResponseEntity<EntityNote> createEntityNote(EntityNote entityNote, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        EntityNote created = entityNoteService.createEntityNote(entityNote, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/platform/entity-notes/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteEntityNote(UUID entityNoteId, String ifMatch) {
        entityNoteService.deleteEntityNote(entityNoteId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<EntityNote> getEntityNote(UUID entityNoteId, String fields) {
        EntityNote found = entityNoteService.getEntityNote(entityNoteId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<EntityNote>> listEntityNotes(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<EntityNote> list =
                PagingUtil.pagingInfo(entityNoteService.listEntityNotes(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<EntityNote> updateEntityNote(UUID entityNoteId, String ifMatch,
            EntityNoteUpdate entityNoteUpdate, Boolean validateOnly) {
        EntityNote updated = entityNoteService.updateEntityNote(entityNoteId, ifMatch, entityNoteUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
