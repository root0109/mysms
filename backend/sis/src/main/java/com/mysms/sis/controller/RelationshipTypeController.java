package com.mysms.sis.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.sis.api.RelationshipTypeApi;
import com.mysms.sis.api.model.RelationshipType;
import com.mysms.sis.api.model.RelationshipTypeUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.sis.business.RelationshipTypeService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the RelationshipType operations; authentication and the permission check already happened in the
 * auth-client filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class RelationshipTypeController implements RelationshipTypeApi {

    private final RelationshipTypeService relationshipTypeService;

    public RelationshipTypeController(RelationshipTypeService relationshipTypeService) {
        this.relationshipTypeService = relationshipTypeService;
    }

    @Override
    public ResponseEntity<RelationshipType> createRelationshipType(RelationshipType relationshipType,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        RelationshipType created = relationshipTypeService.createRelationshipType(relationshipType, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/sis/relationship-types/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteRelationshipType(UUID relationshipTypeId, String ifMatch) {
        relationshipTypeService.deleteRelationshipType(relationshipTypeId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<RelationshipType> getRelationshipType(UUID relationshipTypeId, String fields) {
        RelationshipType found = relationshipTypeService.getRelationshipType(relationshipTypeId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<RelationshipType>> listRelationshipTypes(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<RelationshipType> list =
                PagingUtil.pagingInfo(relationshipTypeService.listRelationshipTypes(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<RelationshipType> updateRelationshipType(UUID relationshipTypeId,
            String ifMatch, RelationshipTypeUpdate relationshipTypeUpdate, String updateMask, Boolean validateOnly) {
        RelationshipType updated =
                relationshipTypeService.updateRelationshipType(relationshipTypeId, ifMatch, relationshipTypeUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
