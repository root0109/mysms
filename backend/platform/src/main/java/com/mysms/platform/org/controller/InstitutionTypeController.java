package com.mysms.platform.org.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.platform.org.api.InstitutionTypeApi;
import com.mysms.platform.org.api.model.InstitutionType;
import com.mysms.platform.org.api.model.InstitutionTypeAttribute;
import com.mysms.platform.org.api.model.InstitutionTypeAttributeUpdate;
import com.mysms.platform.org.api.model.InstitutionTypeUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.platform.org.business.InstitutionTypeService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the InstitutionType operations; authentication and the permission check already happened in the
 * auth-client filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class InstitutionTypeController implements InstitutionTypeApi {

    private final InstitutionTypeService institutionTypeService;

    public InstitutionTypeController(InstitutionTypeService institutionTypeService) {
        this.institutionTypeService = institutionTypeService;
    }

    @Override
    public ResponseEntity<InstitutionType> createInstitutionType(InstitutionType institutionType,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        InstitutionType created = institutionTypeService.createInstitutionType(institutionType, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/org/institution-types/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<InstitutionTypeAttribute> createInstitutionTypeAttribute(UUID institutionTypeId,
            InstitutionTypeAttribute institutionTypeAttribute, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        InstitutionTypeAttribute created =
                institutionTypeService.createInstitutionTypeAttribute(institutionTypeId,
                        institutionTypeAttribute, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/org/institution-types/" + institutionTypeId + "/attributes/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteInstitutionType(UUID institutionTypeId, String ifMatch) {
        institutionTypeService.deleteInstitutionType(institutionTypeId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteInstitutionTypeAttribute(UUID institutionTypeId,
            UUID institutionTypeAttributeId, String ifMatch) {
        institutionTypeService.deleteInstitutionTypeAttribute(institutionTypeId, institutionTypeAttributeId,
                ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<InstitutionType> getInstitutionType(UUID institutionTypeId, String fields) {
        InstitutionType found = institutionTypeService.getInstitutionType(institutionTypeId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<InstitutionTypeAttribute> getInstitutionTypeAttribute(UUID institutionTypeId,
            UUID institutionTypeAttributeId, String fields) {
        InstitutionTypeAttribute found =
                institutionTypeService.getInstitutionTypeAttribute(institutionTypeId,
                        institutionTypeAttributeId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<InstitutionTypeAttribute>> listInstitutionTypeAttributes(UUID institutionTypeId,
            Integer page, Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<InstitutionTypeAttribute> list =
                PagingUtil.pagingInfo(institutionTypeService.listInstitutionTypeAttributes(institutionTypeId, page,
                        size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<InstitutionType>> listInstitutionTypes(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<InstitutionType> list =
                PagingUtil.pagingInfo(institutionTypeService.listInstitutionTypes(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<InstitutionType> updateInstitutionType(UUID institutionTypeId, String ifMatch,
            InstitutionTypeUpdate institutionTypeUpdate, Boolean validateOnly) {
        InstitutionType updated =
                institutionTypeService.updateInstitutionType(institutionTypeId, ifMatch, institutionTypeUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<InstitutionTypeAttribute> updateInstitutionTypeAttribute(UUID institutionTypeId,
            UUID institutionTypeAttributeId, String ifMatch,
            InstitutionTypeAttributeUpdate institutionTypeAttributeUpdate, Boolean validateOnly) {
        InstitutionTypeAttribute updated =
                institutionTypeService.updateInstitutionTypeAttribute(institutionTypeId,
                        institutionTypeAttributeId, ifMatch, institutionTypeAttributeUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
