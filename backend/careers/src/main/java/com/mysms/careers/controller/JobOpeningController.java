package com.mysms.careers.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.careers.business.JobOpeningService;
import com.mysms.careers.api.JobOpeningApi;
import com.mysms.careers.api.model.CloseJobOpeningRequest;
import com.mysms.careers.api.model.EmailApplicantRequest;
import com.mysms.careers.api.model.JobApplication;
import com.mysms.careers.api.model.JobApplicationUpdate;
import com.mysms.careers.api.model.JobOpening;
import com.mysms.careers.api.model.JobOpeningUpdate;
import com.mysms.careers.api.model.ReopenJobOpeningRequest;
import com.mysms.careers.api.model.ReportJobOpeningRequest;
import com.mysms.careers.api.model.UpdateApplicationStatusRequest;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the JobOpening operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class JobOpeningController implements JobOpeningApi {

    private final JobOpeningService jobOpeningService;

    public JobOpeningController(JobOpeningService jobOpeningService) {
        this.jobOpeningService = jobOpeningService;
    }

    @Override
    public ResponseEntity<JobOpening> closeJobOpening(UUID jobOpeningId,
            CloseJobOpeningRequest closeJobOpeningRequest, UUID idempotencyKey) {
        JobOpening changed = jobOpeningService.closeJobOpening(jobOpeningId, closeJobOpeningRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<JobApplication> createJobApplication(UUID jobOpeningId,
            JobApplication jobApplication, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        JobApplication created = jobOpeningService.createJobApplication(jobOpeningId, jobApplication, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/careers/job-openings/" + jobOpeningId + "/applications/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<JobOpening> createJobOpening(JobOpening jobOpening, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        JobOpening created = jobOpeningService.createJobOpening(jobOpening, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/careers/job-openings/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteJobApplication(UUID jobOpeningId, UUID jobApplicationId,
            String ifMatch) {
        jobOpeningService.deleteJobApplication(jobOpeningId, jobApplicationId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteJobOpening(UUID jobOpeningId, String ifMatch) {
        jobOpeningService.deleteJobOpening(jobOpeningId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> emailApplicant(UUID applicationId,
            EmailApplicantRequest emailApplicantRequest, UUID idempotencyKey) {
        jobOpeningService.emailApplicant(applicationId, emailApplicantRequest);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<JobApplication> getJobApplication(UUID jobOpeningId, UUID jobApplicationId,
            String fields) {
        JobApplication found = jobOpeningService.getJobApplication(jobOpeningId, jobApplicationId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<JobOpening> getJobOpening(UUID jobOpeningId, String fields) {
        JobOpening found = jobOpeningService.getJobOpening(jobOpeningId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<JobApplication>> listJobApplications(UUID jobOpeningId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<JobApplication> list =
                PagingUtil.pagingInfo(jobOpeningService.listJobApplications(jobOpeningId, page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<JobOpening>> listJobOpenings(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<JobOpening> list =
                PagingUtil.pagingInfo(jobOpeningService.listJobOpenings(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<JobOpening> reopenJobOpening(UUID jobOpeningId,
            ReopenJobOpeningRequest reopenJobOpeningRequest, UUID idempotencyKey) {
        JobOpening changed = jobOpeningService.reopenJobOpening(jobOpeningId, reopenJobOpeningRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Void> reportJobOpening(UUID jobOpeningId,
            ReportJobOpeningRequest reportJobOpeningRequest, UUID idempotencyKey) {
        jobOpeningService.reportJobOpening(jobOpeningId, reportJobOpeningRequest);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<JobApplication> updateApplicationStatus(UUID jobOpeningId,
            UUID jobApplicationId, UpdateApplicationStatusRequest updateApplicationStatusRequest, UUID idempotencyKey) {
        JobApplication changed = jobOpeningService.updateApplicationStatus(jobOpeningId, jobApplicationId,
                updateApplicationStatusRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<JobApplication> updateJobApplication(UUID jobOpeningId, UUID jobApplicationId,
            String ifMatch, JobApplicationUpdate jobApplicationUpdate, String updateMask, Boolean validateOnly) {
        JobApplication updated =
                jobOpeningService.updateJobApplication(jobOpeningId, jobApplicationId, ifMatch,
                        jobApplicationUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<JobOpening> updateJobOpening(UUID jobOpeningId, String ifMatch,
            JobOpeningUpdate jobOpeningUpdate, String updateMask, Boolean validateOnly) {
        JobOpening updated = jobOpeningService.updateJobOpening(jobOpeningId, ifMatch, jobOpeningUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
