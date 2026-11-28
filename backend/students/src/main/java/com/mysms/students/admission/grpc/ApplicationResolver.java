package com.mysms.students.admission.grpc;

import com.mysms.admission.v1.ApplicationEmergencyContactUpdate;
import com.mysms.admission.v1.ApplicationGuardianUpdate;
import com.mysms.students.admission.api.model.Application;
import com.mysms.students.admission.api.model.ApplicationEmergencyContact;
import com.mysms.students.admission.api.model.ApplicationGuardian;
import com.mysms.students.admission.api.model.EnrollApplicantResult;
import com.mysms.students.admission.business.ApplicationEnrollmentService;
import com.mysms.students.admission.business.ApplicationService;
import com.mysms.students.admission.business.ApplicationWorkflowService;
import com.mysms.admission.v1.ApplicationServiceGrpc;
import com.mysms.admission.v1.ApplicationUpdate;
import com.mysms.admission.v1.ApproveApplicationRequest;
import com.mysms.admission.v1.ApproveApplicationResponse;
import com.mysms.admission.v1.CancelApplicationRequest;
import com.mysms.admission.v1.CancelApplicationResponse;
import com.mysms.admission.v1.ConfirmAdmissionRequest;
import com.mysms.admission.v1.ConfirmAdmissionResponse;
import com.mysms.admission.v1.ConfirmApplicationRequest;
import com.mysms.admission.v1.ConfirmApplicationResponse;
import com.mysms.admission.v1.CreateApplicationEmergencyContactRequest;
import com.mysms.admission.v1.CreateApplicationEmergencyContactResponse;
import com.mysms.admission.v1.CreateApplicationGuardianRequest;
import com.mysms.admission.v1.CreateApplicationGuardianResponse;
import com.mysms.admission.v1.CreateApplicationRequest;
import com.mysms.admission.v1.CreateApplicationResponse;
import com.mysms.admission.v1.DeleteApplicationEmergencyContactRequest;
import com.mysms.admission.v1.DeleteApplicationEmergencyContactResponse;
import com.mysms.admission.v1.DeleteApplicationGuardianRequest;
import com.mysms.admission.v1.DeleteApplicationGuardianResponse;
import com.mysms.admission.v1.DeleteApplicationRequest;
import com.mysms.admission.v1.DeleteApplicationResponse;
import com.mysms.admission.v1.EnrollApplicantRequest;
import com.mysms.admission.v1.EnrollApplicantResponse;
import com.mysms.admission.v1.GetApplicationEmergencyContactRequest;
import com.mysms.admission.v1.GetApplicationEmergencyContactResponse;
import com.mysms.admission.v1.GetApplicationGuardianRequest;
import com.mysms.admission.v1.GetApplicationGuardianResponse;
import com.mysms.admission.v1.GetApplicationRequest;
import com.mysms.admission.v1.GetApplicationResponse;
import com.mysms.admission.v1.ListApplicationEmergencyContactsRequest;
import com.mysms.admission.v1.ListApplicationEmergencyContactsResponse;
import com.mysms.admission.v1.ListApplicationGuardiansRequest;
import com.mysms.admission.v1.ListApplicationGuardiansResponse;
import com.mysms.admission.v1.ListApplicationsRequest;
import com.mysms.admission.v1.ListApplicationsResponse;
import com.mysms.admission.v1.MarkApplicationPendingRequest;
import com.mysms.admission.v1.MarkApplicationPendingResponse;
import com.mysms.admission.v1.RejectApplicationRequest;
import com.mysms.admission.v1.RejectApplicationResponse;
import com.mysms.admission.v1.ResetApplicationRequest;
import com.mysms.admission.v1.ResetApplicationResponse;
import com.mysms.admission.v1.SubmitApplicationRequest;
import com.mysms.admission.v1.SubmitApplicationResponse;
import com.mysms.admission.v1.UpdateApplicationEmergencyContactResponse;
import com.mysms.admission.v1.UpdateApplicationGuardianResponse;
import com.mysms.admission.v1.UpdateApplicationResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of admission.ApplicationService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ApplicationResolver extends ApplicationServiceGrpc.ApplicationServiceImplBase {

    private final ApplicationService applicationService;
    private final ApplicationWorkflowService applicationWorkflowService;
    private final ApplicationEnrollmentService applicationEnrollmentService;
    private final ProtoBridge bridge;

    public ApplicationResolver(
            ApplicationService applicationService, ApplicationWorkflowService applicationWorkflowService,
            ApplicationEnrollmentService applicationEnrollmentService, ProtoBridge bridge) {
        this.applicationService = applicationService;
        this.applicationWorkflowService = applicationWorkflowService;
        this.applicationEnrollmentService = applicationEnrollmentService;
        this.bridge = bridge;
    }

    @Override
    public void listApplications(ListApplicationsRequest request, StreamObserver<ListApplicationsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Application> rows = applicationService.listApplications(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListApplicationsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getApplication(GetApplicationRequest request, StreamObserver<GetApplicationResponse> out) {
        Application found = applicationService.getApplication(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetApplicationResponse.newBuilder(), "application", found).build());
    }

    @Override
    public void createApplication(CreateApplicationRequest request, StreamObserver<CreateApplicationResponse> out) {
        Application created = applicationService.createApplication(
                bridge.body(request.getApplication(), Application.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateApplicationResponse.newBuilder(), "application", created).build());
    }

    @Override
    public void updateApplication(ApplicationUpdate request, StreamObserver<UpdateApplicationResponse> out) {
        Application updated = applicationService.updateApplication(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()),
                ApplicationProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateApplicationResponse.newBuilder(), "application", updated).build());
    }

    @Override
    public void deleteApplication(DeleteApplicationRequest request, StreamObserver<DeleteApplicationResponse> out) {
        applicationService.deleteApplication(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteApplicationResponse.getDefaultInstance());
    }

    @Override
    public void listApplicationGuardians(ListApplicationGuardiansRequest request,
            StreamObserver<ListApplicationGuardiansResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ApplicationGuardian> rows = applicationService.listApplicationGuardians(
                bridge.id(request.getApplicationId(), "applicationId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListApplicationGuardiansResponse.newBuilder(), rows).build());
    }

    @Override
    public void getApplicationGuardian(GetApplicationGuardianRequest request,
            StreamObserver<GetApplicationGuardianResponse> out) {
        ApplicationGuardian found = applicationService.getApplicationGuardian(
                bridge.id(request.getApplicationId(), "applicationId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetApplicationGuardianResponse.newBuilder(), "application_guardian", found).build());
    }

    @Override
    public void createApplicationGuardian(CreateApplicationGuardianRequest request,
            StreamObserver<CreateApplicationGuardianResponse> out) {
        ApplicationGuardian created = applicationService.createApplicationGuardian(
                bridge.id(request.getApplicationId(), "applicationId"),
                bridge.body(request.getApplicationGuardian(), ApplicationGuardian.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreateApplicationGuardianResponse.newBuilder(), "application_guardian", created).build());
    }

    @Override
    public void updateApplicationGuardian(ApplicationGuardianUpdate request,
            StreamObserver<UpdateApplicationGuardianResponse> out) {
        ApplicationGuardian updated = applicationService.updateApplicationGuardian(
                bridge.id(request.getApplicationId(), "applicationId"),
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                ApplicationGuardianProtoBridge.changes(bridge, request),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                UpdateApplicationGuardianResponse.newBuilder(), "application_guardian", updated).build());
    }

    @Override
    public void deleteApplicationGuardian(DeleteApplicationGuardianRequest request,
            StreamObserver<DeleteApplicationGuardianResponse> out) {
        applicationService.deleteApplicationGuardian(
                bridge.id(request.getApplicationId(), "applicationId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteApplicationGuardianResponse.getDefaultInstance());
    }

    @Override
    public void listApplicationEmergencyContacts(ListApplicationEmergencyContactsRequest request,
            StreamObserver<ListApplicationEmergencyContactsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ApplicationEmergencyContact> rows = applicationService.listApplicationEmergencyContacts(
                bridge.id(request.getApplicationId(), "applicationId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListApplicationEmergencyContactsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getApplicationEmergencyContact(GetApplicationEmergencyContactRequest request,
            StreamObserver<GetApplicationEmergencyContactResponse> out) {
        ApplicationEmergencyContact found = applicationService.getApplicationEmergencyContact(
                bridge.id(request.getApplicationId(), "applicationId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(
                GetApplicationEmergencyContactResponse.newBuilder(), "application_emergency_contact", found).build());
    }

    @Override
    public void createApplicationEmergencyContact(CreateApplicationEmergencyContactRequest request,
            StreamObserver<CreateApplicationEmergencyContactResponse> out) {
        ApplicationEmergencyContact created = applicationService.createApplicationEmergencyContact(
                bridge.id(request.getApplicationId(), "applicationId"),
                bridge.body(request.getApplicationEmergencyContact(), ApplicationEmergencyContact.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreateApplicationEmergencyContactResponse.newBuilder(), "application_emergency_contact", created).build());
    }

    @Override
    public void updateApplicationEmergencyContact(ApplicationEmergencyContactUpdate request,
            StreamObserver<UpdateApplicationEmergencyContactResponse> out) {
        ApplicationEmergencyContact updated = applicationService.updateApplicationEmergencyContact(
                bridge.id(request.getApplicationId(), "applicationId"),
                bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()),
                ApplicationEmergencyContactProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                UpdateApplicationEmergencyContactResponse.newBuilder(), "application_emergency_contact", updated).build());
    }

    @Override
    public void deleteApplicationEmergencyContact(DeleteApplicationEmergencyContactRequest request,
            StreamObserver<DeleteApplicationEmergencyContactResponse> out) {
        applicationService.deleteApplicationEmergencyContact(
                bridge.id(request.getApplicationId(), "applicationId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteApplicationEmergencyContactResponse.getDefaultInstance());
    }

    @Override
    public void submitApplication(SubmitApplicationRequest request, StreamObserver<SubmitApplicationResponse> out) {
        Application changed = applicationWorkflowService.submitApplication(
                bridge.id(request.getApplicationId(), "applicationId"),
                bridge.body(request, com.mysms.students.admission.api.model.SubmitApplicationRequest.class, "application_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(SubmitApplicationResponse.newBuilder(), "application", changed).build());
    }

    @Override
    public void confirmApplication(ConfirmApplicationRequest request, StreamObserver<ConfirmApplicationResponse> out) {
        Application changed = applicationWorkflowService.confirmApplication(
                bridge.id(request.getApplicationId(), "applicationId"),
                bridge.body(request, com.mysms.students.admission.api.model.ConfirmApplicationRequest.class, "application_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(ConfirmApplicationResponse.newBuilder(), "application", changed).build());
    }

    @Override
    public void markApplicationPending(MarkApplicationPendingRequest request,
            StreamObserver<MarkApplicationPendingResponse> out) {
        Application changed = applicationWorkflowService.markApplicationPending(
                bridge.id(request.getApplicationId(), "applicationId"),
                bridge.body(request, com.mysms.students.admission.api.model.MarkApplicationPendingRequest.class, "application_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(MarkApplicationPendingResponse.newBuilder(), "application", changed).build());
    }

    @Override
    public void rejectApplication(RejectApplicationRequest request, StreamObserver<RejectApplicationResponse> out) {
        Application changed = applicationWorkflowService.rejectApplication(
                bridge.id(request.getApplicationId(), "applicationId"),
                bridge.body(request, com.mysms.students.admission.api.model.RejectApplicationRequest.class, "application_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(RejectApplicationResponse.newBuilder(), "application", changed).build());
    }

    @Override
    public void cancelApplication(CancelApplicationRequest request, StreamObserver<CancelApplicationResponse> out) {
        Application changed = applicationWorkflowService.cancelApplication(
                bridge.id(request.getApplicationId(), "applicationId"),
                bridge.body(request, com.mysms.students.admission.api.model.CancelApplicationRequest.class, "application_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(CancelApplicationResponse.newBuilder(), "application", changed).build());
    }

    @Override
    public void resetApplication(ResetApplicationRequest request, StreamObserver<ResetApplicationResponse> out) {
        Application changed = applicationWorkflowService.resetApplication(
                bridge.id(request.getApplicationId(), "applicationId"),
                bridge.body(request, com.mysms.students.admission.api.model.ResetApplicationRequest.class, "application_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(ResetApplicationResponse.newBuilder(), "application", changed).build());
    }

    @Override
    public void approveApplication(ApproveApplicationRequest request, StreamObserver<ApproveApplicationResponse> out) {
        Application changed = applicationWorkflowService.approveApplication(
                bridge.id(request.getApplicationId(), "applicationId"),
                bridge.body(request, com.mysms.students.admission.api.model.ApproveApplicationRequest.class, "application_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(ApproveApplicationResponse.newBuilder(), "application", changed).build());
    }

    @Override
    public void confirmAdmission(ConfirmAdmissionRequest request, StreamObserver<ConfirmAdmissionResponse> out) {
        Application changed = applicationWorkflowService.confirmAdmission(
                bridge.id(request.getApplicationId(), "applicationId"),
                bridge.body(request, com.mysms.students.admission.api.model.ConfirmAdmissionRequest.class, "application_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(ConfirmAdmissionResponse.newBuilder(), "application", changed).build());
    }

    /** The result's three parts travel as the response's three fields; a part the result lacks stays unset. */
    @Override
    public void enrollApplicant(EnrollApplicantRequest request, StreamObserver<EnrollApplicantResponse> out) {
        EnrollApplicantResult result = applicationEnrollmentService.enrollApplicant(
                bridge.id(request.getApplicationId(), "applicationId"),
                bridge.body(request, com.mysms.students.admission.api.model.EnrollApplicantRequest.class,
                        "application_id", "request_id"));
        EnrollApplicantResponse.Builder response = EnrollApplicantResponse.newBuilder();
        bridge.reply(response, "application", result.getApplication());
        bridge.reply(response, "student_id", result.getStudentId());
        bridge.reply(response, "operation", result.getOperation());
        ProtoBridge.send(out, response.build());
    }
}
