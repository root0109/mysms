package com.mysms.admission.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.admission.business.ApplicationEnrollmentService;
import com.mysms.admission.business.ApplicationService;
import com.mysms.admission.business.ApplicationWorkflowService;
import com.mysms.admission.api.ApplicationApi;
import com.mysms.admission.api.model.Application;
import com.mysms.admission.api.model.ApplicationEmergencyContact;
import com.mysms.admission.api.model.ApplicationEmergencyContactUpdate;
import com.mysms.admission.api.model.ApplicationGuardian;
import com.mysms.admission.api.model.ApplicationGuardianUpdate;
import com.mysms.admission.api.model.ApplicationUpdate;
import com.mysms.admission.api.model.ApproveApplicationRequest;
import com.mysms.admission.api.model.CancelApplicationRequest;
import com.mysms.admission.api.model.ConfirmAdmissionRequest;
import com.mysms.admission.api.model.ConfirmApplicationRequest;
import com.mysms.admission.api.model.EnrollApplicantRequest;
import com.mysms.admission.api.model.EnrollApplicantResult;
import com.mysms.admission.api.model.MarkApplicationPendingRequest;
import com.mysms.admission.api.model.RejectApplicationRequest;
import com.mysms.admission.api.model.ResetApplicationRequest;
import com.mysms.admission.api.model.SubmitApplicationRequest;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Application operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class ApplicationController implements ApplicationApi {

    private final ApplicationService applicationService;
    private final ApplicationWorkflowService applicationWorkflowService;
    private final ApplicationEnrollmentService applicationEnrollmentService;

    public ApplicationController(ApplicationService applicationService,
            ApplicationWorkflowService applicationWorkflowService,
            ApplicationEnrollmentService applicationEnrollmentService) {
        this.applicationService = applicationService;
        this.applicationWorkflowService = applicationWorkflowService;
        this.applicationEnrollmentService = applicationEnrollmentService;
    }

    /** A step's answer: the application as it now stands, with its ETag. */
    private static ResponseEntity<Application> changed(Application application) {
        return ResponseEntity.ok().eTag(application.getEtag()).body(application);
    }


    @Override
    public ResponseEntity<Application> approveApplication(UUID applicationId,
            ApproveApplicationRequest approveApplicationRequest, UUID idempotencyKey) {
        return changed(applicationWorkflowService.approveApplication(applicationId, approveApplicationRequest));
    }

    @Override
    public ResponseEntity<Application> cancelApplication(UUID applicationId,
            CancelApplicationRequest cancelApplicationRequest, UUID idempotencyKey) {
        return changed(applicationWorkflowService.cancelApplication(applicationId, cancelApplicationRequest));
    }

    @Override
    public ResponseEntity<Application> confirmAdmission(UUID applicationId,
            ConfirmAdmissionRequest confirmAdmissionRequest, UUID idempotencyKey) {
        return changed(applicationWorkflowService.confirmAdmission(applicationId, confirmAdmissionRequest));
    }

    @Override
    public ResponseEntity<Application> confirmApplication(UUID applicationId,
            ConfirmApplicationRequest confirmApplicationRequest, UUID idempotencyKey) {
        return changed(applicationWorkflowService.confirmApplication(applicationId, confirmApplicationRequest));
    }

    @Override
    public ResponseEntity<Application> createApplication(Application application, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Application created = applicationService.createApplication(application, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/admission/applications/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<ApplicationEmergencyContact> createApplicationEmergencyContact(
            UUID applicationId, ApplicationEmergencyContact applicationEmergencyContact, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ApplicationEmergencyContact created =
                applicationService.createApplicationEmergencyContact(applicationId,
                        applicationEmergencyContact, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/admission/applications/" + applicationId + "/emergency-contacts/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<ApplicationGuardian> createApplicationGuardian(UUID applicationId,
            ApplicationGuardian applicationGuardian, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ApplicationGuardian created =
                applicationService.createApplicationGuardian(applicationId, applicationGuardian, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/admission/applications/" + applicationId + "/guardians/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteApplication(UUID applicationId, String ifMatch) {
        applicationService.deleteApplication(applicationId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteApplicationEmergencyContact(UUID applicationId,
            UUID applicationEmergencyContactId, String ifMatch) {
        applicationService.deleteApplicationEmergencyContact(applicationId,
                applicationEmergencyContactId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteApplicationGuardian(UUID applicationId, UUID applicationGuardianId,
            String ifMatch) {
        applicationService.deleteApplicationGuardian(applicationId, applicationGuardianId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<EnrollApplicantResult> enrollApplicant(UUID applicationId, UUID idempotencyKey,
            EnrollApplicantRequest enrollApplicantRequest) {
        return ResponseEntity.ok(applicationEnrollmentService.enrollApplicant(applicationId, enrollApplicantRequest));
    }

    @Override
    public ResponseEntity<Application> getApplication(UUID applicationId, String fields) {
        Application found = applicationService.getApplication(applicationId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ApplicationEmergencyContact> getApplicationEmergencyContact(UUID applicationId,
            UUID applicationEmergencyContactId, String fields) {
        ApplicationEmergencyContact found =
                applicationService.getApplicationEmergencyContact(applicationId,
                        applicationEmergencyContactId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ApplicationGuardian> getApplicationGuardian(UUID applicationId,
            UUID applicationGuardianId, String fields) {
        ApplicationGuardian found =
                applicationService.getApplicationGuardian(applicationId, applicationGuardianId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<ApplicationEmergencyContact>> listApplicationEmergencyContacts(UUID applicationId,
            Integer page, Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ApplicationEmergencyContact> list =
                PagingUtil.pagingInfo(applicationService.listApplicationEmergencyContacts(applicationId, page, size,
                        cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<ApplicationGuardian>> listApplicationGuardians(UUID applicationId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ApplicationGuardian> list =
                PagingUtil.pagingInfo(applicationService.listApplicationGuardians(applicationId, page, size, cursor,
                        filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Application>> listApplications(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<Application> list =
                PagingUtil.pagingInfo(applicationService.listApplications(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Application> markApplicationPending(UUID applicationId,
            MarkApplicationPendingRequest markApplicationPendingRequest, UUID idempotencyKey) {
        return changed(applicationWorkflowService.markApplicationPending(applicationId, markApplicationPendingRequest));
    }

    @Override
    public ResponseEntity<Application> rejectApplication(UUID applicationId,
            RejectApplicationRequest rejectApplicationRequest, UUID idempotencyKey) {
        return changed(applicationWorkflowService.rejectApplication(applicationId, rejectApplicationRequest));
    }

    @Override
    public ResponseEntity<Application> resetApplication(UUID applicationId,
            ResetApplicationRequest resetApplicationRequest, UUID idempotencyKey) {
        return changed(applicationWorkflowService.resetApplication(applicationId, resetApplicationRequest));
    }

    @Override
    public ResponseEntity<Application> submitApplication(UUID applicationId,
            SubmitApplicationRequest submitApplicationRequest, UUID idempotencyKey) {
        return changed(applicationWorkflowService.submitApplication(applicationId, submitApplicationRequest));
    }

    @Override
    public ResponseEntity<Application> updateApplication(UUID applicationId, String ifMatch,
            ApplicationUpdate applicationUpdate, String updateMask, Boolean validateOnly) {
        Application updated =
                applicationService.updateApplication(applicationId, ifMatch, applicationUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ApplicationEmergencyContact> updateApplicationEmergencyContact(
            UUID applicationId, UUID applicationEmergencyContactId, String ifMatch,
            ApplicationEmergencyContactUpdate applicationEmergencyContactUpdate, String updateMask,
            Boolean validateOnly) {
        ApplicationEmergencyContact updated =
                applicationService.updateApplicationEmergencyContact(applicationId,
                        applicationEmergencyContactId, ifMatch, applicationEmergencyContactUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ApplicationGuardian> updateApplicationGuardian(UUID applicationId,
            UUID applicationGuardianId, String ifMatch, ApplicationGuardianUpdate applicationGuardianUpdate,
            String updateMask, Boolean validateOnly) {
        ApplicationGuardian updated =
                applicationService.updateApplicationGuardian(applicationId, applicationGuardianId, ifMatch,
                        applicationGuardianUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
