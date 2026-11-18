package com.mysms.sis.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.sis.api.ProgramEnrollmentApi;
import com.mysms.sis.api.model.ProgramEnrollment;
import com.mysms.sis.api.model.ProgramEnrollmentSubject;
import com.mysms.sis.api.model.ProgramEnrollmentSubjectUpdate;
import com.mysms.sis.api.model.ProgramEnrollmentUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.sis.business.ProgramEnrollmentService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ProgramEnrollment operations; authentication and the permission check already happened in the
 * auth-client filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class ProgramEnrollmentController implements ProgramEnrollmentApi {

    private final ProgramEnrollmentService programEnrollmentService;

    public ProgramEnrollmentController(ProgramEnrollmentService programEnrollmentService) {
        this.programEnrollmentService = programEnrollmentService;
    }

    @Override
    public ResponseEntity<ProgramEnrollment> createProgramEnrollment(ProgramEnrollment programEnrollment,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ProgramEnrollment created = programEnrollmentService.createProgramEnrollment(programEnrollment, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/sis/program-enrollments/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<ProgramEnrollmentSubject> createProgramEnrollmentSubject(
            UUID programEnrollmentId, ProgramEnrollmentSubject programEnrollmentSubject, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ProgramEnrollmentSubject created =
                programEnrollmentService.createProgramEnrollmentSubject(programEnrollmentId,
                        programEnrollmentSubject, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/sis/program-enrollments/" + programEnrollmentId + "/subjects/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteProgramEnrollment(UUID programEnrollmentId, String ifMatch) {
        programEnrollmentService.deleteProgramEnrollment(programEnrollmentId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteProgramEnrollmentSubject(UUID programEnrollmentId,
            UUID programEnrollmentSubjectId, String ifMatch) {
        programEnrollmentService.deleteProgramEnrollmentSubject(programEnrollmentId, programEnrollmentSubjectId,
                ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ProgramEnrollment> getProgramEnrollment(UUID programEnrollmentId,
            String fields) {
        ProgramEnrollment found = programEnrollmentService.getProgramEnrollment(programEnrollmentId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ProgramEnrollmentSubject> getProgramEnrollmentSubject(UUID programEnrollmentId,
            UUID programEnrollmentSubjectId, String fields) {
        ProgramEnrollmentSubject found =
                programEnrollmentService.getProgramEnrollmentSubject(programEnrollmentId,
                        programEnrollmentSubjectId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<ProgramEnrollmentSubject>> listProgramEnrollmentSubjects(UUID programEnrollmentId,
            Integer page, Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ProgramEnrollmentSubject> list =
                PagingUtil.pagingInfo(programEnrollmentService.listProgramEnrollmentSubjects(programEnrollmentId, page,
                        size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<ProgramEnrollment>> listProgramEnrollments(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ProgramEnrollment> list =
                PagingUtil.pagingInfo(programEnrollmentService.listProgramEnrollments(page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ProgramEnrollment> updateProgramEnrollment(UUID programEnrollmentId,
            String ifMatch, ProgramEnrollmentUpdate programEnrollmentUpdate, String updateMask, Boolean validateOnly) {
        ProgramEnrollment updated =
                programEnrollmentService.updateProgramEnrollment(programEnrollmentId, ifMatch, programEnrollmentUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ProgramEnrollmentSubject> updateProgramEnrollmentSubject(
            UUID programEnrollmentId, UUID programEnrollmentSubjectId, String ifMatch,
            ProgramEnrollmentSubjectUpdate programEnrollmentSubjectUpdate, String updateMask, Boolean validateOnly) {
        ProgramEnrollmentSubject updated =
                programEnrollmentService.updateProgramEnrollmentSubject(programEnrollmentId,
                        programEnrollmentSubjectId, ifMatch, programEnrollmentSubjectUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
