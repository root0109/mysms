package com.mysms.hrms.hr.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.hrms.hr.api.DesignationApi;
import com.mysms.hrms.hr.api.model.Designation;
import com.mysms.hrms.hr.api.model.DesignationUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.hrms.hr.business.DesignationService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Designation operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class DesignationController implements DesignationApi {

    private final DesignationService designationService;

    public DesignationController(DesignationService designationService) {
        this.designationService = designationService;
    }

    @Override
    public ResponseEntity<Designation> createDesignation(Designation designation, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Designation created = designationService.createDesignation(designation, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/hr/designations/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteDesignation(UUID designationId, String ifMatch) {
        designationService.deleteDesignation(designationId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Designation> getDesignation(UUID designationId, String fields) {
        Designation found = designationService.getDesignation(designationId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Designation>> listDesignations(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<Designation> list =
                PagingUtil.pagingInfo(designationService.listDesignations(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Designation> updateDesignation(UUID designationId, String ifMatch,
            DesignationUpdate designationUpdate, Boolean validateOnly) {
        Designation updated =
                designationService.updateDesignation(designationId, ifMatch, designationUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
