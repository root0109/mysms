package com.mysms.org.controller;

import com.mysms.org.api.model.InstitutionCard;
import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.org.api.InstitutionApi;
import com.mysms.org.api.model.Institution;
import com.mysms.org.api.model.InstitutionAttribute;
import com.mysms.org.api.model.InstitutionAttributeUpdate;
import com.mysms.org.api.model.InstitutionUpdate;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import com.mysms.org.business.InstitutionService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Institution operations; authentication and the permission check already happened in the auth-client
 * filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class InstitutionController implements InstitutionApi {

    private final InstitutionService institutionService;

    public InstitutionController(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    @Override
    public ResponseEntity<Institution> createInstitution(Institution institution, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Institution created = institutionService.createInstitution(institution, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/org/institutions/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<InstitutionAttribute> createInstitutionAttribute(UUID institutionId,
            InstitutionAttribute institutionAttribute, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        InstitutionAttribute created =
                institutionService.createInstitutionAttribute(institutionId, institutionAttribute, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/org/institutions/" + institutionId + "/attributes/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteInstitution(UUID institutionId, String ifMatch) {
        institutionService.deleteInstitution(institutionId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteInstitutionAttribute(UUID institutionId,
            UUID institutionAttributeId, String ifMatch) {
        institutionService.deleteInstitutionAttribute(institutionId, institutionAttributeId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PagingInfo<InstitutionCard>> findInstitutions(String query, String countryCode, Integer page,
            Integer size, String cursor) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Institution> getInstitution(UUID institutionId, String fields) {
        Institution found = institutionService.getInstitution(institutionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<InstitutionAttribute> getInstitutionAttribute(UUID institutionId,
            UUID institutionAttributeId, String fields) {
        InstitutionAttribute found =
                institutionService.getInstitutionAttribute(institutionId, institutionAttributeId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<InstitutionAttribute>> listInstitutionAttributes(UUID institutionId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<InstitutionAttribute> list =
                PagingUtil.pagingInfo(institutionService.listInstitutionAttributes(institutionId, page, size, cursor,
                        filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Institution>> listInstitutions(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<Institution> list =
                PagingUtil.pagingInfo(institutionService.listInstitutions(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Institution>> listActiveInstitutions(Integer page, Integer size, String cursor,
            String filter) {
        return ResponseEntity.ok(PagingUtil.pagingInfo(
                institutionService.listActiveInstitutions(page, size, cursor, filter)));
    }

    @Override
    public ResponseEntity<Institution> updateInstitution(UUID institutionId, String ifMatch,
            InstitutionUpdate institutionUpdate, String updateMask, Boolean validateOnly) {
        Institution updated =
                institutionService.updateInstitution(institutionId, ifMatch, institutionUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<InstitutionAttribute> updateInstitutionAttribute(UUID institutionId,
            UUID institutionAttributeId, String ifMatch, InstitutionAttributeUpdate institutionAttributeUpdate,
            String updateMask, Boolean validateOnly) {
        InstitutionAttribute updated =
                institutionService.updateInstitutionAttribute(institutionId, institutionAttributeId, ifMatch,
                        institutionAttributeUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
