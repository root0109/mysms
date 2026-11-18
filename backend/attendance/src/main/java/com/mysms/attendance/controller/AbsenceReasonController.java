package com.mysms.attendance.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.attendance.business.AbsenceReasonService;
import com.mysms.attendance.api.AbsenceReasonApi;
import com.mysms.attendance.api.model.AbsenceReason;
import com.mysms.attendance.api.model.AbsenceReasonUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the AbsenceReason operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class AbsenceReasonController implements AbsenceReasonApi {

    private final AbsenceReasonService absenceReasonService;

    public AbsenceReasonController(AbsenceReasonService absenceReasonService) {
        this.absenceReasonService = absenceReasonService;
    }

    @Override
    public ResponseEntity<AbsenceReason> createAbsenceReason(AbsenceReason absenceReason,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        AbsenceReason created = absenceReasonService.createAbsenceReason(absenceReason, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/attendance/absence-reasons/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteAbsenceReason(UUID absenceReasonId, String ifMatch) {
        absenceReasonService.deleteAbsenceReason(absenceReasonId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<AbsenceReason> getAbsenceReason(UUID absenceReasonId, String fields) {
        AbsenceReason found = absenceReasonService.getAbsenceReason(absenceReasonId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<AbsenceReason>> listAbsenceReasons(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<AbsenceReason> list =
                PagingUtil.pagingInfo(absenceReasonService.listAbsenceReasons(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<AbsenceReason> updateAbsenceReason(UUID absenceReasonId, String ifMatch,
            AbsenceReasonUpdate absenceReasonUpdate, String updateMask, Boolean validateOnly) {
        AbsenceReason updated =
                absenceReasonService.updateAbsenceReason(absenceReasonId, ifMatch, absenceReasonUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
