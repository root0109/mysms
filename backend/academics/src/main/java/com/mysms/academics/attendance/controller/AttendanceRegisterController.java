package com.mysms.academics.attendance.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.academics.attendance.business.AttendanceRegisterService;
import com.mysms.academics.attendance.api.AttendanceRegisterApi;
import com.mysms.academics.attendance.api.model.AttendanceRegister;
import com.mysms.academics.attendance.api.model.AttendanceRegisterUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the AttendanceRegister operations; authentication and the permission check already ran in the
 * auth-client filter. Operations not served yet answer 501.
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
            String ifMatch, AttendanceRegisterUpdate attendanceRegisterUpdate, Boolean validateOnly) {
        AttendanceRegister updated =
                attendanceRegisterService.updateAttendanceRegister(attendanceRegisterId, ifMatch,
                        attendanceRegisterUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
