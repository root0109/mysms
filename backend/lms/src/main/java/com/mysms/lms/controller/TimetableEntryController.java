package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.TimetableEntryApi;
import com.mysms.lms.api.model.TimetableEntry;
import com.mysms.lms.api.model.TimetableEntryUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.TimetableEntryService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the TimetableEntry operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class TimetableEntryController implements TimetableEntryApi {

    private final TimetableEntryService timetableEntryService;

    public TimetableEntryController(TimetableEntryService timetableEntryService) {
        this.timetableEntryService = timetableEntryService;
    }

    @Override
    public ResponseEntity<TimetableEntry> createTimetableEntry(TimetableEntry timetableEntry,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        TimetableEntry created = timetableEntryService.createTimetableEntry(timetableEntry, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/timetable-entries/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteTimetableEntry(UUID timetableEntryId, String ifMatch) {
        timetableEntryService.deleteTimetableEntry(timetableEntryId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<TimetableEntry> getTimetableEntry(UUID timetableEntryId, String fields) {
        TimetableEntry found = timetableEntryService.getTimetableEntry(timetableEntryId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<TimetableEntry>> listTimetableEntries(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<TimetableEntry> list =
                PagingUtil.pagingInfo(timetableEntryService.listTimetableEntrys(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<TimetableEntry> updateTimetableEntry(UUID timetableEntryId, String ifMatch,
            TimetableEntryUpdate timetableEntryUpdate, String updateMask, Boolean validateOnly) {
        TimetableEntry updated =
                timetableEntryService.updateTimetableEntry(timetableEntryId, ifMatch, timetableEntryUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
