package com.mysms.campus.transport.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.campus.transport.api.StudentTransportApi;
import com.mysms.campus.transport.api.model.StudentTransport;
import com.mysms.campus.transport.api.model.StudentTransportUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.campus.transport.business.StudentTransportService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the StudentTransport operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class StudentTransportController implements StudentTransportApi {

    private final StudentTransportService studentTransportService;

    public StudentTransportController(StudentTransportService studentTransportService) {
        this.studentTransportService = studentTransportService;
    }

    @Override
    public ResponseEntity<StudentTransport> createStudentTransport(StudentTransport studentTransport,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        StudentTransport created = studentTransportService.createStudentTransport(studentTransport, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/transport/student-transports/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteStudentTransport(UUID studentTransportId, String ifMatch) {
        studentTransportService.deleteStudentTransport(studentTransportId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<StudentTransport> getStudentTransport(UUID studentTransportId, String fields) {
        StudentTransport found = studentTransportService.getStudentTransport(studentTransportId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<StudentTransport>> listStudentTransports(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<StudentTransport> list =
                PagingUtil.pagingInfo(studentTransportService.listStudentTransports(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<StudentTransport> updateStudentTransport(UUID studentTransportId,
            String ifMatch, StudentTransportUpdate studentTransportUpdate, Boolean validateOnly) {
        StudentTransport updated =
                studentTransportService.updateStudentTransport(studentTransportId, ifMatch, studentTransportUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
