package com.mysms.timetable.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.timetable.api.PeriodApi;
import com.mysms.timetable.api.model.Period;
import com.mysms.timetable.api.model.PeriodUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.timetable.business.PeriodService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Period operations; authentication and the permission check already ran in the auth-client filter.
 * Operations not served yet answer 501.
 */
@RestController
public class PeriodController implements PeriodApi {

    private final PeriodService periodService;

    public PeriodController(PeriodService periodService) {
        this.periodService = periodService;
    }

    @Override
    public ResponseEntity<Period> createPeriod(Period period, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Period created = periodService.createPeriod(period, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/timetable/periods/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deletePeriod(UUID periodId, String ifMatch) {
        periodService.deletePeriod(periodId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Period> getPeriod(UUID periodId, String fields) {
        Period found = periodService.getPeriod(periodId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Period>> listPeriods(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Period> list = PagingUtil.pagingInfo(periodService.listPeriods(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Period> updatePeriod(UUID periodId, String ifMatch, PeriodUpdate periodUpdate,
            String updateMask, Boolean validateOnly) {
        Period updated = periodService.updatePeriod(periodId, ifMatch, periodUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
