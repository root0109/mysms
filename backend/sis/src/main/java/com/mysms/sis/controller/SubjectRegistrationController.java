package com.mysms.sis.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.sis.api.SubjectRegistrationApi;
import com.mysms.sis.api.model.ApproveSubjectRegistrationRequest;
import com.mysms.sis.api.model.RejectSubjectRegistrationRequest;
import com.mysms.sis.api.model.ResetSubjectRegistrationRequest;
import com.mysms.sis.api.model.SubjectRegistration;
import com.mysms.sis.api.model.SubjectRegistrationLine;
import com.mysms.sis.api.model.SubjectRegistrationLineUpdate;
import com.mysms.sis.api.model.SubjectRegistrationUpdate;
import com.mysms.sis.api.model.SubmitSubjectRegistrationRequest;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import com.mysms.sis.business.SubjectRegistrationService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the SubjectRegistration operations: implements the generated contract interface, so paths, parameters
 * and payloads
 * cannot drift from api/openapi/sis.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class SubjectRegistrationController implements SubjectRegistrationApi {

    private final SubjectRegistrationService subjectRegistrationService;

    public SubjectRegistrationController(SubjectRegistrationService subjectRegistrationService) {
        this.subjectRegistrationService = subjectRegistrationService;
    }

    @Override
    public ResponseEntity<SubjectRegistration> approveSubjectRegistration(UUID subjectRegistrationId,
            ApproveSubjectRegistrationRequest approveSubjectRegistrationRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<SubjectRegistration> createSubjectRegistration(
            SubjectRegistration subjectRegistration, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        SubjectRegistration created = subjectRegistrationService.createSubjectRegistration(subjectRegistration, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/sis/subject-registrations/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<SubjectRegistrationLine> createSubjectRegistrationLine(
            UUID subjectRegistrationId, SubjectRegistrationLine subjectRegistrationLine, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        SubjectRegistrationLine created =
                subjectRegistrationService.createSubjectRegistrationLine(subjectRegistrationId,
                        subjectRegistrationLine, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/sis/subject-registrations/" + subjectRegistrationId + "/lines/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteSubjectRegistration(UUID subjectRegistrationId, String ifMatch) {
        subjectRegistrationService.deleteSubjectRegistration(subjectRegistrationId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteSubjectRegistrationLine(UUID subjectRegistrationId,
            UUID subjectRegistrationLineId, String ifMatch) {
        subjectRegistrationService.deleteSubjectRegistrationLine(subjectRegistrationId, subjectRegistrationLineId,
                ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<SubjectRegistration> getSubjectRegistration(UUID subjectRegistrationId,
            String fields) {
        SubjectRegistration found = subjectRegistrationService.getSubjectRegistration(subjectRegistrationId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<SubjectRegistrationLine> getSubjectRegistrationLine(UUID subjectRegistrationId,
            UUID subjectRegistrationLineId, String fields) {
        SubjectRegistrationLine found =
                subjectRegistrationService.getSubjectRegistrationLine(subjectRegistrationId,
                        subjectRegistrationLineId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<SubjectRegistrationLine>> listSubjectRegistrationLines(UUID subjectRegistrationId,
            Integer page, Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<SubjectRegistrationLine> list =
                PagingUtil.pagingInfo(subjectRegistrationService.listSubjectRegistrationLines(subjectRegistrationId,
                        page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<SubjectRegistration>> listSubjectRegistrations(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<SubjectRegistration> list =
                PagingUtil.pagingInfo(subjectRegistrationService.listSubjectRegistrations(page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<SubjectRegistration> loadCourseSubjects(UUID subjectRegistrationId, Object body,
            UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<SubjectRegistration> rejectSubjectRegistration(UUID subjectRegistrationId,
            RejectSubjectRegistrationRequest rejectSubjectRegistrationRequest, UUID idempotencyKey) {
        SubjectRegistration changed =
                subjectRegistrationService.rejectSubjectRegistration(subjectRegistrationId,
                        rejectSubjectRegistrationRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<SubjectRegistration> resetSubjectRegistration(UUID subjectRegistrationId,
            ResetSubjectRegistrationRequest resetSubjectRegistrationRequest, UUID idempotencyKey) {
        SubjectRegistration changed =
                subjectRegistrationService.resetSubjectRegistration(subjectRegistrationId,
                        resetSubjectRegistrationRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<SubjectRegistration> submitSubjectRegistration(UUID subjectRegistrationId,
            SubmitSubjectRegistrationRequest submitSubjectRegistrationRequest, UUID idempotencyKey) {
        SubjectRegistration changed =
                subjectRegistrationService.submitSubjectRegistration(subjectRegistrationId,
                        submitSubjectRegistrationRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<SubjectRegistration> updateSubjectRegistration(UUID subjectRegistrationId,
            String ifMatch, SubjectRegistrationUpdate subjectRegistrationUpdate, String updateMask,
            Boolean validateOnly) {
        SubjectRegistration updated =
                subjectRegistrationService.updateSubjectRegistration(subjectRegistrationId, ifMatch,
                        subjectRegistrationUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<SubjectRegistrationLine> updateSubjectRegistrationLine(
            UUID subjectRegistrationId, UUID subjectRegistrationLineId, String ifMatch,
            SubjectRegistrationLineUpdate subjectRegistrationLineUpdate, String updateMask, Boolean validateOnly) {
        SubjectRegistrationLine updated =
                subjectRegistrationService.updateSubjectRegistrationLine(subjectRegistrationId,
                        subjectRegistrationLineId, ifMatch, subjectRegistrationLineUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
