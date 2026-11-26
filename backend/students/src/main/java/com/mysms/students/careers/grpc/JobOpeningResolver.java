package com.mysms.students.careers.grpc;

import com.mysms.students.careers.api.model.JobApplication;
import com.mysms.students.careers.api.model.JobApplicationUpdate;
import com.mysms.students.careers.api.model.JobOpening;
import com.mysms.students.careers.api.model.JobOpeningUpdate;
import com.mysms.students.careers.business.JobOpeningService;
import com.mysms.careers.v1.CloseJobOpeningRequest;
import com.mysms.careers.v1.CloseJobOpeningResponse;
import com.mysms.careers.v1.CreateJobApplicationRequest;
import com.mysms.careers.v1.CreateJobApplicationResponse;
import com.mysms.careers.v1.CreateJobOpeningRequest;
import com.mysms.careers.v1.CreateJobOpeningResponse;
import com.mysms.careers.v1.DeleteJobApplicationRequest;
import com.mysms.careers.v1.DeleteJobApplicationResponse;
import com.mysms.careers.v1.DeleteJobOpeningRequest;
import com.mysms.careers.v1.DeleteJobOpeningResponse;
import com.mysms.careers.v1.EmailApplicantRequest;
import com.mysms.careers.v1.EmailApplicantResponse;
import com.mysms.careers.v1.GetJobApplicationRequest;
import com.mysms.careers.v1.GetJobApplicationResponse;
import com.mysms.careers.v1.GetJobOpeningRequest;
import com.mysms.careers.v1.GetJobOpeningResponse;
import com.mysms.careers.v1.JobOpeningServiceGrpc;
import com.mysms.careers.v1.ListJobApplicationsRequest;
import com.mysms.careers.v1.ListJobApplicationsResponse;
import com.mysms.careers.v1.ListJobOpeningsRequest;
import com.mysms.careers.v1.ListJobOpeningsResponse;
import com.mysms.careers.v1.ReopenJobOpeningRequest;
import com.mysms.careers.v1.ReopenJobOpeningResponse;
import com.mysms.careers.v1.ReportJobOpeningRequest;
import com.mysms.careers.v1.ReportJobOpeningResponse;
import com.mysms.careers.v1.UpdateApplicationStatusRequest;
import com.mysms.careers.v1.UpdateApplicationStatusResponse;
import com.mysms.careers.v1.UpdateJobApplicationRequest;
import com.mysms.careers.v1.UpdateJobApplicationResponse;
import com.mysms.careers.v1.UpdateJobOpeningRequest;
import com.mysms.careers.v1.UpdateJobOpeningResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of careers.JobOpeningService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class JobOpeningResolver extends JobOpeningServiceGrpc.JobOpeningServiceImplBase {

    private final JobOpeningService service;
    private final ProtoBridge bridge;

    public JobOpeningResolver(JobOpeningService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listJobOpenings(ListJobOpeningsRequest request, StreamObserver<ListJobOpeningsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<JobOpening> rows = service.listJobOpenings(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListJobOpeningsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getJobOpening(GetJobOpeningRequest request, StreamObserver<GetJobOpeningResponse> out) {
        JobOpening found = service.getJobOpening(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetJobOpeningResponse.newBuilder(), "job_opening", found).build());
    }

    @Override
    public void createJobOpening(CreateJobOpeningRequest request, StreamObserver<CreateJobOpeningResponse> out) {
        JobOpening created = service.createJobOpening(
                bridge.body(request.getJobOpening(), JobOpening.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateJobOpeningResponse.newBuilder(), "job_opening", created).build());
    }

    @Override
    public void updateJobOpening(UpdateJobOpeningRequest request, StreamObserver<UpdateJobOpeningResponse> out) {
        JobOpening updated = service.updateJobOpening(
                bridge.id(request.getJobOpening().getId(), "id"),
                bridge.requiredIfMatch(request.getJobOpening().getEtag()),
                bridge.changes(request.getJobOpening(), request.getUpdateMask(), JobOpeningUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateJobOpeningResponse.newBuilder(), "job_opening", updated).build());
    }

    @Override
    public void deleteJobOpening(DeleteJobOpeningRequest request, StreamObserver<DeleteJobOpeningResponse> out) {
        service.deleteJobOpening(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteJobOpeningResponse.getDefaultInstance());
    }

    @Override
    public void listJobApplications(ListJobApplicationsRequest request, StreamObserver<ListJobApplicationsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<JobApplication> rows = service.listJobApplications(
                bridge.id(request.getJobOpeningId(), "jobOpeningId"),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListJobApplicationsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getJobApplication(GetJobApplicationRequest request, StreamObserver<GetJobApplicationResponse> out) {
        JobApplication found = service.getJobApplication(
                bridge.id(request.getJobOpeningId(), "jobOpeningId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetJobApplicationResponse.newBuilder(), "job_application", found).build());
    }

    @Override
    public void createJobApplication(CreateJobApplicationRequest request, StreamObserver<CreateJobApplicationResponse> out) {
        JobApplication created = service.createJobApplication(
                bridge.id(request.getJobOpeningId(), "jobOpeningId"),
                bridge.body(request.getJobApplication(), JobApplication.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateJobApplicationResponse.newBuilder(), "job_application", created).build());
    }

    @Override
    public void updateJobApplication(UpdateJobApplicationRequest request, StreamObserver<UpdateJobApplicationResponse> out) {
        JobApplication updated = service.updateJobApplication(
                bridge.id(request.getJobOpeningId(), "jobOpeningId"),
                bridge.id(request.getJobApplication().getId(), "id"),
                bridge.requiredIfMatch(request.getJobApplication().getEtag()),
                bridge.changes(request.getJobApplication(), request.getUpdateMask(), JobApplicationUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateJobApplicationResponse.newBuilder(), "job_application", updated).build());
    }

    @Override
    public void deleteJobApplication(DeleteJobApplicationRequest request, StreamObserver<DeleteJobApplicationResponse> out) {
        service.deleteJobApplication(
                bridge.id(request.getJobOpeningId(), "jobOpeningId"),
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteJobApplicationResponse.getDefaultInstance());
    }

    /** The request's etag (optional) is the REST body's; the path id and the idempotency key travel apart. */
    @Override
    public void closeJobOpening(CloseJobOpeningRequest request, StreamObserver<CloseJobOpeningResponse> out) {
        JobOpening changed = service.closeJobOpening(
                bridge.id(request.getJobOpeningId(), "jobOpeningId"),
                bridge.body(request, com.mysms.students.careers.api.model.CloseJobOpeningRequest.class, "job_opening_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(CloseJobOpeningResponse.newBuilder(), "job_opening", changed).build());
    }

    /** The request's etag (optional) is the REST body's; the path id and the idempotency key travel apart. */
    @Override
    public void reopenJobOpening(ReopenJobOpeningRequest request, StreamObserver<ReopenJobOpeningResponse> out) {
        JobOpening changed = service.reopenJobOpening(
                bridge.id(request.getJobOpeningId(), "jobOpeningId"),
                bridge.body(request, com.mysms.students.careers.api.model.ReopenJobOpeningRequest.class,
                        "job_opening_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(ReopenJobOpeningResponse.newBuilder(), "job_opening", changed).build());
    }

    @Override
    public void reportJobOpening(ReportJobOpeningRequest request, StreamObserver<ReportJobOpeningResponse> out) {
        service.reportJobOpening(bridge.id(request.getJobOpeningId(), "jobOpeningId"),
                bridge.body(request, com.mysms.students.careers.api.model.ReportJobOpeningRequest.class, "job_opening_id",
                        "request_id"));
        ProtoBridge.send(out, ReportJobOpeningResponse.getDefaultInstance());
    }

    @Override
    public void emailApplicant(EmailApplicantRequest request, StreamObserver<EmailApplicantResponse> out) {
        service.emailApplicant(bridge.id(request.getApplicationId(), "applicationId"),
                bridge.body(request, com.mysms.students.careers.api.model.EmailApplicantRequest.class, "application_id",
                        "request_id"));
        ProtoBridge.send(out, EmailApplicantResponse.getDefaultInstance());
    }

    @Override
    public void updateApplicationStatus(UpdateApplicationStatusRequest request,
            StreamObserver<UpdateApplicationStatusResponse> out) {
        JobApplication changed = service.updateApplicationStatus(bridge.id(request.getJobOpeningId(), "jobOpeningId"),
                bridge.id(request.getJobApplicationId(), "jobApplicationId"),
                bridge.body(request, com.mysms.students.careers.api.model.UpdateApplicationStatusRequest.class,
                        "job_opening_id", "job_application_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(UpdateApplicationStatusResponse.newBuilder(), "job_application", changed)
                .build());
    }
}
