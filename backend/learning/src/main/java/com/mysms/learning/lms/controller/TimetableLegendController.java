package com.mysms.learning.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.TimetableLegendApi;
import com.mysms.learning.lms.api.model.TimetableLegend;
import com.mysms.learning.lms.api.model.TimetableLegendUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.learning.lms.business.TimetableLegendService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the TimetableLegend operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class TimetableLegendController implements TimetableLegendApi {

    private final TimetableLegendService timetableLegendService;

    public TimetableLegendController(TimetableLegendService timetableLegendService) {
        this.timetableLegendService = timetableLegendService;
    }

    @Override
    public ResponseEntity<TimetableLegend> createTimetableLegend(TimetableLegend timetableLegend,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        TimetableLegend created = timetableLegendService.createTimetableLegend(timetableLegend, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/timetable-legends/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteTimetableLegend(UUID timetableLegendId, String ifMatch) {
        timetableLegendService.deleteTimetableLegend(timetableLegendId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<TimetableLegend> getTimetableLegend(UUID timetableLegendId, String fields) {
        TimetableLegend found = timetableLegendService.getTimetableLegend(timetableLegendId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<TimetableLegend>> listTimetableLegends(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<TimetableLegend> list =
                PagingUtil.pagingInfo(timetableLegendService.listTimetableLegends(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<TimetableLegend> updateTimetableLegend(UUID timetableLegendId, String ifMatch,
            TimetableLegendUpdate timetableLegendUpdate, Boolean validateOnly) {
        TimetableLegend updated =
                timetableLegendService.updateTimetableLegend(timetableLegendId, ifMatch, timetableLegendUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
