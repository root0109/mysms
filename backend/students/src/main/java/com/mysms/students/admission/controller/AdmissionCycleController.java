package com.mysms.students.admission.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.students.admission.business.AdmissionCycleService;
import com.mysms.students.admission.api.AdmissionCycleApi;
import com.mysms.students.admission.api.model.AdmissionCycle;
import com.mysms.students.admission.api.model.AdmissionCycleFee;
import com.mysms.students.admission.api.model.AdmissionCycleFeeUpdate;
import com.mysms.students.admission.api.model.AdmissionCycleUpdate;
import com.mysms.students.admission.api.model.CancelAdmissionCycleRequest;
import com.mysms.students.admission.api.model.CloseAdmissionCycleRequest;
import com.mysms.students.admission.api.model.ConfirmAdmissionCycleRequest;
import com.mysms.students.admission.api.model.OpenApplicationsRequest;
import com.mysms.students.admission.api.model.ResetAdmissionCycleRequest;
import com.mysms.students.admission.api.model.StartAdmissionRequest;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the AdmissionCycle operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class AdmissionCycleController implements AdmissionCycleApi {

    private final AdmissionCycleService admissionCycleService;

    public AdmissionCycleController(AdmissionCycleService admissionCycleService) {
        this.admissionCycleService = admissionCycleService;
    }

    @Override
    public ResponseEntity<AdmissionCycle> cancelAdmissionCycle(UUID admissionCycleId,
            CancelAdmissionCycleRequest cancelAdmissionCycleRequest, UUID idempotencyKey) {
        AdmissionCycle changed =
                admissionCycleService.cancelAdmissionCycle(admissionCycleId, cancelAdmissionCycleRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<AdmissionCycle> closeAdmissionCycle(UUID admissionCycleId,
            CloseAdmissionCycleRequest closeAdmissionCycleRequest, UUID idempotencyKey) {
        AdmissionCycle changed =
                admissionCycleService.closeAdmissionCycle(admissionCycleId, closeAdmissionCycleRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<AdmissionCycle> confirmAdmissionCycle(UUID admissionCycleId,
            ConfirmAdmissionCycleRequest confirmAdmissionCycleRequest, UUID idempotencyKey) {
        AdmissionCycle changed =
                admissionCycleService.confirmAdmissionCycle(admissionCycleId, confirmAdmissionCycleRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<AdmissionCycle> createAdmissionCycle(AdmissionCycle admissionCycle,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        AdmissionCycle created = admissionCycleService.createAdmissionCycle(admissionCycle, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/admission/admission-cycles/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<AdmissionCycleFee> createAdmissionCycleFee(UUID admissionCycleId,
            AdmissionCycleFee admissionCycleFee, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        AdmissionCycleFee created =
                admissionCycleService.createAdmissionCycleFee(admissionCycleId, admissionCycleFee, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/admission/admission-cycles/" + admissionCycleId + "/fees/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteAdmissionCycle(UUID admissionCycleId, String ifMatch) {
        admissionCycleService.deleteAdmissionCycle(admissionCycleId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteAdmissionCycleFee(UUID admissionCycleId, UUID admissionCycleFeeId,
            String ifMatch) {
        admissionCycleService.deleteAdmissionCycleFee(admissionCycleId, admissionCycleFeeId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<AdmissionCycle> getAdmissionCycle(UUID admissionCycleId, String fields) {
        AdmissionCycle found = admissionCycleService.getAdmissionCycle(admissionCycleId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<AdmissionCycleFee> getAdmissionCycleFee(UUID admissionCycleId,
            UUID admissionCycleFeeId, String fields) {
        AdmissionCycleFee found = admissionCycleService.getAdmissionCycleFee(admissionCycleId, admissionCycleFeeId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<AdmissionCycleFee>> listAdmissionCycleFees(UUID admissionCycleId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<AdmissionCycleFee> list =
                PagingUtil.pagingInfo(admissionCycleService.listAdmissionCycleFees(admissionCycleId, page, size, cursor,
                        filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<AdmissionCycle>> listAdmissionCycles(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<AdmissionCycle> list =
                PagingUtil.pagingInfo(admissionCycleService.listAdmissionCycles(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<AdmissionCycle> openApplications(UUID admissionCycleId,
            OpenApplicationsRequest openApplicationsRequest, UUID idempotencyKey) {
        AdmissionCycle changed = admissionCycleService.openApplications(admissionCycleId, openApplicationsRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<AdmissionCycle> resetAdmissionCycle(UUID admissionCycleId,
            ResetAdmissionCycleRequest resetAdmissionCycleRequest, UUID idempotencyKey) {
        AdmissionCycle changed =
                admissionCycleService.resetAdmissionCycle(admissionCycleId, resetAdmissionCycleRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<AdmissionCycle> startAdmission(UUID admissionCycleId,
            StartAdmissionRequest startAdmissionRequest, UUID idempotencyKey) {
        AdmissionCycle changed = admissionCycleService.startAdmission(admissionCycleId, startAdmissionRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<AdmissionCycle> updateAdmissionCycle(UUID admissionCycleId, String ifMatch,
            AdmissionCycleUpdate admissionCycleUpdate, Boolean validateOnly) {
        AdmissionCycle updated =
                admissionCycleService.updateAdmissionCycle(admissionCycleId, ifMatch, admissionCycleUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<AdmissionCycleFee> updateAdmissionCycleFee(UUID admissionCycleId,
            UUID admissionCycleFeeId, String ifMatch, AdmissionCycleFeeUpdate admissionCycleFeeUpdate,
            Boolean validateOnly) {
        AdmissionCycleFee updated =
                admissionCycleService.updateAdmissionCycleFee(admissionCycleId, admissionCycleFeeId, ifMatch,
                        admissionCycleFeeUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
