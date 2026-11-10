package com.mysms.attendance.controller;

import com.mysms.attendance.api.model.AttendanceSummary;
import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.attendance.business.AttendanceSheetService;
import com.mysms.attendance.business.AttendanceSummaryService;
import com.mysms.attendance.business.AttendanceTakingService;
import com.mysms.attendance.api.AttendanceSheetApi;
import com.mysms.attendance.api.model.AttendanceRecord;
import com.mysms.attendance.api.model.AttendanceRecordList;
import com.mysms.attendance.api.model.AttendanceSheet;
import com.mysms.attendance.api.model.AttendanceSheetUpdate;
import com.mysms.attendance.api.model.CancelAttendanceSheetRequest;
import com.mysms.attendance.api.model.CompleteAttendanceSheetRequest;
import com.mysms.attendance.api.model.RecordAttendanceRequest;
import com.mysms.attendance.api.model.ResetAttendanceSheetRequest;
import com.mysms.attendance.api.model.StartAttendanceSheetRequest;
import com.mysms.attendance.api.model.TakeAttendanceForSessionRequest;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the AttendanceSheet operations: implements the module's contract interface (JAVA-CODING-RULES §9).
 * Thin: delegate, then shape the HTTP response. Authentication and the x-permission check already happened in the
 * auth-client filter.
 */
@RestController
public class AttendanceSheetController implements AttendanceSheetApi {

    private final AttendanceSheetService attendanceSheetService;
    private final AttendanceTakingService attendanceTakingService;
    private final AttendanceSummaryService attendanceSummaryService;

    public AttendanceSheetController(AttendanceSheetService attendanceSheetService,
            AttendanceTakingService attendanceTakingService,
            AttendanceSummaryService attendanceSummaryService) {
        this.attendanceSheetService = attendanceSheetService;
        this.attendanceTakingService = attendanceTakingService;
        this.attendanceSummaryService = attendanceSummaryService;
    }

    @Override
    public ResponseEntity<AttendanceSheet> cancelAttendanceSheet(UUID attendanceSheetId,
            CancelAttendanceSheetRequest cancelAttendanceSheetRequest, UUID idempotencyKey) {
        AttendanceSheet changed =
                attendanceSheetService.cancelAttendanceSheet(attendanceSheetId, cancelAttendanceSheetRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<AttendanceSheet> completeAttendanceSheet(UUID attendanceSheetId,
            CompleteAttendanceSheetRequest completeAttendanceSheetRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(attendanceTakingService.completeAttendanceSheet(attendanceSheetId,
                completeAttendanceSheetRequest));
    }

    @Override
    public ResponseEntity<AttendanceSheet> createAttendanceSheet(AttendanceSheet attendanceSheet,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        AttendanceSheet created = attendanceSheetService.createAttendanceSheet(attendanceSheet, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/attendance/attendance-sheets/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteAttendanceSheet(UUID attendanceSheetId, String ifMatch) {
        attendanceSheetService.deleteAttendanceSheet(attendanceSheetId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<AttendanceRecord> getAttendanceRecord(UUID attendanceSheetId,
            UUID attendanceRecordId, String fields) {
        AttendanceRecord found = attendanceSheetService.getAttendanceRecord(attendanceSheetId, attendanceRecordId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<AttendanceSheet> getAttendanceSheet(UUID attendanceSheetId, String fields) {
        AttendanceSheet found = attendanceSheetService.getAttendanceSheet(attendanceSheetId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<AttendanceRecord>> listAttendanceRecords(UUID attendanceSheetId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<AttendanceRecord> list =
                PagingUtil.pagingInfo(attendanceSheetService.listAttendanceRecords(attendanceSheetId, page, size,
                        cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<AttendanceSheet>> listAttendanceSheets(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<AttendanceSheet> list =
                PagingUtil.pagingInfo(attendanceSheetService.listAttendanceSheets(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<AttendanceRecordList> recordAttendance(UUID attendanceSheetId,
            RecordAttendanceRequest recordAttendanceRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(attendanceTakingService.recordAttendance(attendanceSheetId,
                recordAttendanceRequest));
    }

    @Override
    public ResponseEntity<AttendanceSheet> resetAttendanceSheet(UUID attendanceSheetId,
            ResetAttendanceSheetRequest resetAttendanceSheetRequest, UUID idempotencyKey) {
        AttendanceSheet changed =
                attendanceSheetService.resetAttendanceSheet(attendanceSheetId, resetAttendanceSheetRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<AttendanceSheet> startAttendanceSheet(UUID attendanceSheetId,
            StartAttendanceSheetRequest startAttendanceSheetRequest, UUID idempotencyKey) {
        AttendanceSheet changed =
                attendanceSheetService.startAttendanceSheet(attendanceSheetId, startAttendanceSheetRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<PagingInfo<AttendanceSummary>> summarizeAttendance(UUID institutionId, LocalDate fromDate,
            LocalDate toDate, String groupBy, UUID studentId, UUID classSectionId, Integer page, Integer size,
            String cursor) {
        return ResponseEntity.ok(PagingUtil.pagingInfo(attendanceSummaryService.summarizeAttendance(institutionId,
                fromDate, toDate, groupBy, studentId, classSectionId, page, size, cursor)));
    }

    @Override
    public ResponseEntity<AttendanceSheet> takeAttendanceForSession(
            TakeAttendanceForSessionRequest takeAttendanceForSessionRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(attendanceTakingService.takeAttendanceForSession(
                takeAttendanceForSessionRequest));
    }

    @Override
    public ResponseEntity<AttendanceSheet> updateAttendanceSheet(UUID attendanceSheetId, String ifMatch,
            AttendanceSheetUpdate attendanceSheetUpdate, String updateMask, Boolean validateOnly) {
        AttendanceSheet updated =
                attendanceSheetService.updateAttendanceSheet(attendanceSheetId, ifMatch, attendanceSheetUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
