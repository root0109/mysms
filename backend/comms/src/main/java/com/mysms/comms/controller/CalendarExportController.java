package com.mysms.comms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.business.CalendarExportService;
import com.mysms.comms.api.CalendarExportApi;
import com.mysms.comms.api.model.CalendarExport;
import com.mysms.comms.api.model.ExportCalendarRequest;
import com.mysms.comms.api.model.Operation;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the CalendarExport operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/comms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class CalendarExportController implements CalendarExportApi {

    private final CalendarExportService calendarExportService;

    public CalendarExportController(CalendarExportService calendarExportService) {
        this.calendarExportService = calendarExportService;
    }

    @Override
    public ResponseEntity<Void> deleteCalendarExport(UUID calendarExportId, String ifMatch) {
        calendarExportService.deleteCalendarExport(calendarExportId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Operation> exportCalendar(ExportCalendarRequest exportCalendarRequest,
            UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<CalendarExport> getCalendarExport(UUID calendarExportId, String fields) {
        CalendarExport found = calendarExportService.getCalendarExport(calendarExportId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<CalendarExport>> listCalendarExports(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<CalendarExport> list =
                PagingUtil.pagingInfo(calendarExportService.listCalendarExports(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }
}
