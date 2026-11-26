package com.mysms.platform.org.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.platform.org.api.AttributeDefinitionApi;
import com.mysms.platform.org.api.model.AttributeDefinition;
import com.mysms.platform.org.api.model.AttributeDefinitionUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.platform.org.business.AttributeDefinitionService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the AttributeDefinition operations; authentication and the permission check already happened in the
 * auth-client filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class AttributeDefinitionController implements AttributeDefinitionApi {

    private final AttributeDefinitionService attributeDefinitionService;

    public AttributeDefinitionController(AttributeDefinitionService attributeDefinitionService) {
        this.attributeDefinitionService = attributeDefinitionService;
    }

    @Override
    public ResponseEntity<AttributeDefinition> createAttributeDefinition(
            AttributeDefinition attributeDefinition, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        AttributeDefinition created = attributeDefinitionService.createAttributeDefinition(attributeDefinition, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/org/attribute-definitions/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteAttributeDefinition(UUID attributeDefinitionId, String ifMatch) {
        attributeDefinitionService.deleteAttributeDefinition(attributeDefinitionId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<AttributeDefinition> getAttributeDefinition(UUID attributeDefinitionId,
            String fields) {
        AttributeDefinition found = attributeDefinitionService.getAttributeDefinition(attributeDefinitionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<AttributeDefinition>> listAttributeDefinitions(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<AttributeDefinition> list =
                PagingUtil.pagingInfo(attributeDefinitionService.listAttributeDefinitions(page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<AttributeDefinition> updateAttributeDefinition(UUID attributeDefinitionId,
            String ifMatch, AttributeDefinitionUpdate attributeDefinitionUpdate, Boolean validateOnly) {
        AttributeDefinition updated =
                attributeDefinitionService.updateAttributeDefinition(attributeDefinitionId, ifMatch,
                        attributeDefinitionUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
