package com.mysms.attendance.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.attendance.business.AttendanceTypeService;
import com.mysms.attendance.api.AttendanceTypeApi;
import com.mysms.attendance.api.model.AttendanceType;
import com.mysms.attendance.api.model.AttendanceTypeUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the AttendanceType operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/attendance.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class AttendanceTypeController implements AttendanceTypeApi {

    private final AttendanceTypeService attendanceTypeService;

    public AttendanceTypeController(AttendanceTypeService attendanceTypeService) {
        this.attendanceTypeService = attendanceTypeService;
    }

    @Override
    public ResponseEntity<AttendanceType> createAttendanceType(AttendanceType attendanceType,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        AttendanceType created = attendanceTypeService.createAttendanceType(attendanceType, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/attendance/attendance-types/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteAttendanceType(UUID attendanceTypeId, String ifMatch) {
        attendanceTypeService.deleteAttendanceType(attendanceTypeId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<AttendanceType> getAttendanceType(UUID attendanceTypeId, String fields) {
        AttendanceType found = attendanceTypeService.getAttendanceType(attendanceTypeId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<AttendanceType>> listAttendanceTypes(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<AttendanceType> list =
                PagingUtil.pagingInfo(attendanceTypeService.listAttendanceTypes(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<AttendanceType> updateAttendanceType(UUID attendanceTypeId, String ifMatch,
            AttendanceTypeUpdate attendanceTypeUpdate, String updateMask, Boolean validateOnly) {
        AttendanceType updated =
                attendanceTypeService.updateAttendanceType(attendanceTypeId, ifMatch, attendanceTypeUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
