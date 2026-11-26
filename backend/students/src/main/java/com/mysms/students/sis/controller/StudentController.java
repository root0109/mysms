package com.mysms.students.sis.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.students.sis.api.StudentApi;
import com.mysms.students.sis.api.model.ChangeStudentStatusRequest;
import com.mysms.students.sis.api.model.CreateStudentPortalAccountRequest;
import com.mysms.students.sis.api.model.IssueBonafideCertificateRequest;
import com.mysms.students.sis.api.model.Operation;
import com.mysms.students.sis.api.model.Student;
import com.mysms.students.sis.api.model.StudentGuardian;
import com.mysms.students.sis.api.model.StudentGuardianUpdate;
import com.mysms.students.sis.api.model.StudentSibling;
import com.mysms.students.sis.api.model.StudentSiblingUpdate;
import com.mysms.students.sis.api.model.StudentSummary;
import com.mysms.students.sis.api.model.StudentUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.students.sis.business.PortalAccountService;
import com.mysms.students.sis.business.StudentService;
import com.mysms.students.sis.business.StudentSummaryService;
import com.mysms.students.sis.business.StudentWorkflowService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Student operations; authentication and the permission check already happened in the auth-client
 * filter. The steps with further effects, the portal account and the summary are served by their own services.
 */
@RestController
public class StudentController implements StudentApi {

    private final StudentService studentService;
    private final StudentWorkflowService workflowService;
    private final PortalAccountService portalAccountService;
    private final StudentSummaryService summaryService;

    public StudentController(StudentService studentService, StudentWorkflowService workflowService,
            PortalAccountService portalAccountService, StudentSummaryService summaryService) {
        this.studentService = studentService;
        this.workflowService = workflowService;
        this.portalAccountService = portalAccountService;
        this.summaryService = summaryService;
    }

    @Override
    public ResponseEntity<Student> changeStudentStatus(UUID studentId,
            ChangeStudentStatusRequest changeStudentStatusRequest, UUID idempotencyKey) {
        Student changed = workflowService.changeStudentStatus(studentId, changeStudentStatusRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Student> createStudent(Student student, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Student created = studentService.createStudent(student, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/sis/students/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<StudentGuardian> createStudentGuardian(UUID studentId,
            StudentGuardian studentGuardian, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        StudentGuardian created = studentService.createStudentGuardian(studentId, studentGuardian, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/sis/students/" + studentId + "/guardians/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Student> createStudentPortalAccount(UUID studentId,
            CreateStudentPortalAccountRequest createStudentPortalAccountRequest, UUID idempotencyKey) {
        Student linked = portalAccountService.createStudentPortalAccount(studentId,
                createStudentPortalAccountRequest);
        return ResponseEntity.ok().eTag(linked.getEtag()).body(linked);
    }

    @Override
    public ResponseEntity<StudentSibling> createStudentSibling(UUID studentId,
            StudentSibling studentSibling, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        StudentSibling created = studentService.createStudentSibling(studentId, studentSibling, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/sis/students/" + studentId + "/siblings/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteStudent(UUID studentId, String ifMatch) {
        studentService.deleteStudent(studentId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteStudentGuardian(UUID studentId, UUID studentGuardianId,
            String ifMatch) {
        studentService.deleteStudentGuardian(studentId, studentGuardianId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteStudentSibling(UUID studentId, UUID studentSiblingId,
            String ifMatch) {
        studentService.deleteStudentSibling(studentId, studentSiblingId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Student> getStudent(UUID studentId, String fields) {
        Student found = studentService.getStudent(studentId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<StudentGuardian> getStudentGuardian(UUID studentId, UUID studentGuardianId,
            String fields) {
        StudentGuardian found = studentService.getStudentGuardian(studentId, studentGuardianId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<StudentSibling> getStudentSibling(UUID studentId, UUID studentSiblingId,
            String fields) {
        StudentSibling found = studentService.getStudentSibling(studentId, studentSiblingId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<StudentSummary> getStudentSummary(UUID studentId, UUID academicYearId) {
        return ResponseEntity.ok(summaryService.getStudentSummary(studentId, academicYearId));
    }

    @Override
    public ResponseEntity<Operation> issueBonafideCertificate(UUID studentId,
            IssueBonafideCertificateRequest issueBonafideCertificateRequest, UUID idempotencyKey) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(workflowService.issueBonafideCertificate(studentId, issueBonafideCertificateRequest));
    }

    @Override
    public ResponseEntity<PagingInfo<StudentGuardian>> listStudentGuardians(UUID studentId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<StudentGuardian> list =
                PagingUtil.pagingInfo(studentService.listStudentGuardians(studentId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<StudentSibling>> listStudentSiblings(UUID studentId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<StudentSibling> list =
                PagingUtil.pagingInfo(studentService.listStudentSiblings(studentId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Student>> listStudents(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Student> list = PagingUtil.pagingInfo(studentService.listStudents(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Student> updateStudent(UUID studentId, String ifMatch,
            StudentUpdate studentUpdate, Boolean validateOnly) {
        Student updated = studentService.updateStudent(studentId, ifMatch, studentUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<StudentGuardian> updateStudentGuardian(UUID studentId, UUID studentGuardianId,
            String ifMatch, StudentGuardianUpdate studentGuardianUpdate, Boolean validateOnly) {
        StudentGuardian updated =
                studentService.updateStudentGuardian(studentId, studentGuardianId, ifMatch,
                        studentGuardianUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<StudentSibling> updateStudentSibling(UUID studentId, UUID studentSiblingId,
            String ifMatch, StudentSiblingUpdate studentSiblingUpdate, Boolean validateOnly) {
        StudentSibling updated =
                studentService.updateStudentSibling(studentId, studentSiblingId, ifMatch, studentSiblingUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
