package com.mysms.attendance.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.attendance.business.AttendanceRegisterService;
import com.mysms.attendance.api.AttendanceRegisterApi;
import com.mysms.attendance.api.model.AttendanceRegister;
import com.mysms.attendance.api.model.AttendanceRegisterUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the AttendanceRegister operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/attendance.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class AttendanceRegisterController implements AttendanceRegisterApi {

    private final AttendanceRegisterService attendanceRegisterService;

    public AttendanceRegisterController(AttendanceRegisterService attendanceRegisterService) {
        this.attendanceRegisterService = attendanceRegisterService;
    }

    @Override
    public ResponseEntity<AttendanceRegister> createAttendanceRegister(
            AttendanceRegister attendanceRegister, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        AttendanceRegister created = attendanceRegisterService.createAttendanceRegister(attendanceRegister, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/attendance/attendance-registers/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteAttendanceRegister(UUID attendanceRegisterId, String ifMatch) {
        attendanceRegisterService.deleteAttendanceRegister(attendanceRegisterId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<AttendanceRegister> getAttendanceRegister(UUID attendanceRegisterId,
            String fields) {
        AttendanceRegister found = attendanceRegisterService.getAttendanceRegister(attendanceRegisterId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<AttendanceRegister>> listAttendanceRegisters(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<AttendanceRegister> list =
                PagingUtil.pagingInfo(attendanceRegisterService.listAttendanceRegisters(page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<AttendanceRegister> updateAttendanceRegister(UUID attendanceRegisterId,
            String ifMatch, AttendanceRegisterUpdate attendanceRegisterUpdate, String updateMask,
            Boolean validateOnly) {
        AttendanceRegister updated =
                attendanceRegisterService.updateAttendanceRegister(attendanceRegisterId, ifMatch,
                        attendanceRegisterUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
