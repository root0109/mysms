package com.mysms.facility.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.facility.api.FacilityApi;
import com.mysms.facility.api.model.Facility;
import com.mysms.facility.api.model.FacilityUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.facility.business.FacilityService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Facility operations; authentication and the permission check already ran in the auth-client filter.
 * Operations not served yet answer 501.
 */
@RestController
public class FacilityController implements FacilityApi {

    private final FacilityService facilityService;

    public FacilityController(FacilityService facilityService) {
        this.facilityService = facilityService;
    }

    @Override
    public ResponseEntity<Facility> createFacility(Facility facility, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Facility created = facilityService.createFacility(facility, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/facility/facilities/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteFacility(UUID facilityId, String ifMatch) {
        facilityService.deleteFacility(facilityId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Facility> getFacility(UUID facilityId, String fields) {
        Facility found = facilityService.getFacility(facilityId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Facility>> listFacilities(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Facility> list =
                PagingUtil.pagingInfo(facilityService.listFacilitys(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Facility> updateFacility(UUID facilityId, String ifMatch,
            FacilityUpdate facilityUpdate, String updateMask, Boolean validateOnly) {
        Facility updated = facilityService.updateFacility(facilityId, ifMatch, facilityUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
