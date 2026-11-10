package com.mysms.careers.business;

import com.mysms.authclient.DataScope;
import com.mysms.careers.api.model.CloseJobOpeningRequest;
import com.mysms.careers.api.model.JobApplication;
import com.mysms.careers.api.model.JobApplicationUpdate;
import com.mysms.careers.api.model.JobOpening;
import com.mysms.careers.api.model.JobOpeningUpdate;
import com.mysms.careers.api.model.ReopenJobOpeningRequest;
import com.mysms.careers.config.CareersDatabaseConfig;
import com.mysms.careers.config.CareersEventsConfig;
import com.mysms.careers.domain.JobApplicationEntity;
import com.mysms.careers.domain.JobOpeningEntity;
import com.mysms.careers.events.CareersEvents;
import com.mysms.careers.mapper.JobApplicationMapper;
import com.mysms.careers.mapper.JobOpeningMapper;
import com.mysms.careers.repository.JobApplicationRepository;
import com.mysms.careers.repository.JobOpeningRepository;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.UpdateMask;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of careers.job_opening (JAVA-CODING-RULES §4): one plain class, explicit methods, transactions driven
 * on the module's PlatformTransactionManager, audit + domain event in the same transaction as every change, and the data-level
 * rules of api/README "Authorization" (rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant).
 *
 * <p>One service per aggregate (owner decision of 2026-11-10): the records that exist only as part of a JobOpening
 * are served here too — JobApplication.
 */
@Service
public class JobOpeningService extends AppTransactionManager {

    public static final String AGGREGATE = JobOpeningEntity.TABLE;
    static final String PERMISSION_CREATE = "careers.job_opening.create";
    static final String PERMISSION_UPDATE = "careers.job_opening.update";
    static final String PERMISSION_DELETE = "careers.job_opening.delete";
    static final String PERMISSION_CLOSE_JOB_OPENING = "careers.job_opening.close_job_opening";
    static final String PERMISSION_REOPEN_JOB_OPENING = "careers.job_opening.reopen_job_opening";
    /** The contract's JobOpeningUpdate fields, in apply order. */
    static final List<String> WRITABLE =
            List.of("postedByUserId", "title", "location", "countryId", "employmentType", "workMode", "description",
            "companyName", "companyWebsite", "companyEmail", "companyLogoFileId");

    public static final String JOB_APPLICATION_AGGREGATE = JobApplicationEntity.TABLE;
    static final String JOB_APPLICATION_PERMISSION_LIST = "careers.job_application.read";
    static final String JOB_APPLICATION_PERMISSION_GET = "careers.job_application.read";
    static final String JOB_APPLICATION_PERMISSION_CREATE = "careers.job_application.create";
    static final String JOB_APPLICATION_PERMISSION_UPDATE = "careers.job_application.update";
    static final String JOB_APPLICATION_PERMISSION_DELETE = "careers.job_application.delete";
    /** The contract's JobApplicationUpdate fields, in apply order. */
    static final List<String> JOB_APPLICATION_WRITABLE = List.of("applicantUserId", "resumeFileId");

    private final JobOpeningRepository repository;
    private final JobOpeningMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final JobApplicationRepository jobApplicationRepository;
    private final JobApplicationMapper jobApplicationMapper;

    public JobOpeningService(JobOpeningRepository repository,
            JobOpeningMapper mapper,
            @Qualifier(CareersDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(CareersDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(CareersEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(CareersEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            JobApplicationRepository jobApplicationRepository,
            JobApplicationMapper jobApplicationMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.jobApplicationRepository = jobApplicationRepository;
        this.jobApplicationMapper = jobApplicationMapper;
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public JobOpening createJobOpening(JobOpening request, boolean validateOnly) {
        validator.validate(request);
        JobOpeningEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            assertValid(entity, true);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            JobOpeningEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + CareersEvents.CREATED, AGGREGATE, stored.getId(),
                    CareersEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public JobOpening updateJobOpening(UUID id, String ifMatch, JobOpeningUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            JobOpeningEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            apply(entity, changes, fields);
            validator.validate(mapper.toDto(entity));
            assertValid(entity, false);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            if (repository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(AGGREGATE, id, fields);
            outbox.publish(AGGREGATE + CareersEvents.UPDATED, AGGREGATE, id, CareersEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteJobOpening(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            JobOpeningEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByJobApplicationJobOpeningId(id)) {
                throw new InvalidStateException(
                        "The job opening is still referenced by careers.job_application rows; remove those first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + CareersEvents.DELETED, AGGREGATE, id, CareersEvents.payload(id));
        });
    }

    /**
     * careers.CloseJobOpening: Close. The row must be visible and writable for the caller, and the step is allowed
     * while status is open; in any other state
     * the answer is 409 and nothing changes.
     */
    public JobOpening closeJobOpening(UUID id, CloseJobOpeningRequest request) {
        DataScope scope = DataScope.current(PERMISSION_CLOSE_JOB_OPENING);
        return doInWriteTransaction(() -> {
            JobOpeningEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("open").contains(entity.getStatus()))) {
                throw new InvalidStateException("The job opening cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.closeJobOpening(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "close", List.of("status"), null);
            outbox.publish(AGGREGATE + ".close", AGGREGATE, id, CareersEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * careers.ReopenJobOpening: Reopen. The row must be visible and writable for the caller, and the step is allowed
     * while status is closed; in any other state
     * the answer is 409 and nothing changes.
     */
    public JobOpening reopenJobOpening(UUID id, ReopenJobOpeningRequest request) {
        DataScope scope = DataScope.current(PERMISSION_REOPEN_JOB_OPENING);
        return doInWriteTransaction(() -> {
            JobOpeningEntity entity = repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (!scope.canRead(institutionOf(entity))) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            scope.requireWritable(institutionOf(entity));
            OptionalInt expected = Etags.bodyVersion(request == null ? null : request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!(Set.of("closed").contains(entity.getStatus()))) {
                throw new InvalidStateException("The job opening cannot be changed this way in its current state.");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (repository.reopenJobOpening(id, entity.getRowVersion(), now, Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "reopen", List.of("status"), null);
            outbox.publish(AGGREGATE + ".reopen", AGGREGATE, id, CareersEvents.payload(id));
            return mapper.toDto(repository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private JobOpeningEntity loadVisible(DataScope scope, UUID id) {
        JobOpeningEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOf(JobOpeningEntity entity) {
        return Optional.empty();
    }

    /** Applies the selected JobOpeningUpdate fields; explicit per field so a mask can clear values (no reflection). */
    private static void apply(JobOpeningEntity entity, JobOpeningUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "postedByUserId" -> entity.setPostedByUserId(changes.getPostedByUserId());
                case "title" -> entity.setTitle(changes.getTitle());
                case "location" -> entity.setLocation(changes.getLocation());
                case "countryId" -> entity.setCountryId(changes.getCountryId());
                case "employmentType" -> entity.setEmploymentType(Optional.ofNullable(changes.getEmploymentType())
                        .map(v -> v.getValue()).orElse(null));
                case "workMode" -> entity.setWorkMode(Optional.ofNullable(changes.getWorkMode()).map(v -> v.getValue())
                        .orElse(null));
                case "description" -> entity.setDescription(changes.getDescription());
                case "companyName" -> entity.setCompanyName(changes.getCompanyName());
                case "companyWebsite" -> entity.setCompanyWebsite(changes.getCompanyWebsite());
                case "companyEmail" -> entity.setCompanyEmail(changes.getCompanyEmail());
                case "companyLogoFileId" -> entity.setCompanyLogoFileId(changes.getCompanyLogoFileId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(JobOpeningEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getTitle() == null) {
            errors.add(new FieldError("title", "must not be null", "NotNull"));
        }
        if (entity.getLocation() == null) {
            errors.add(new FieldError("location", "must not be null", "NotNull"));
        }
        if (entity.getCountryId() == null) {
            errors.add(new FieldError("countryId", "must not be null", "NotNull"));
        }
        if (entity.getEmploymentType() == null) {
            errors.add(new FieldError("employmentType", "must not be null", "NotNull"));
        }
        if (entity.getDescription() == null) {
            errors.add(new FieldError("description", "must not be null", "NotNull"));
        }
        if (entity.getCompanyName() == null) {
            errors.add(new FieldError("companyName", "must not be null", "NotNull"));
        }
        if (entity.getCompanyWebsite() == null) {
            errors.add(new FieldError("companyWebsite", "must not be null", "NotNull"));
        }
        if (entity.getCompanyEmail() == null) {
            errors.add(new FieldError("companyEmail", "must not be null", "NotNull"));
        }
        if (entity.getCompanyLogoFileId() == null) {
            errors.add(new FieldError("companyLogoFileId", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(JobOpeningEntity entity) {

    }

    // ---- JobApplication: a record of the JobOpening aggregate ----

    public JobApplication getJobApplication(UUID jobOpeningId, UUID id) {
        return doInReadTransaction(() -> {
            return jobApplicationMapper.toDto(loadVisibleJobApplication(DataScope.current(JOB_APPLICATION_PERMISSION_GET),
                    jobOpeningId, id));
        });
    }

    public Page<JobApplication> listJobApplications(UUID jobOpeningId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, JobApplicationRepository.SORT_FIELDS, JobApplicationRepository.DEFAULT_ORDER,
                JobApplicationRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, JobApplicationRepository.FILTER_FIELDS)
                .and(JobApplicationRepository.ofParent(jobOpeningId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + jobOpeningId, clock);
        return doInReadTransaction(() -> {
            Page<JobApplicationEntity> rows = jobApplicationRepository.list(page, where);
            return rows.map(jobApplicationMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public JobApplication createJobApplication(UUID jobOpeningId, JobApplication request, boolean validateOnly) {
        validator.validate(request);
        JobApplicationEntity entity = jobApplicationMapper.toEntity(request);
        entity.setJobOpeningId(jobOpeningId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(JOB_APPLICATION_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!jobApplicationRepository.existsJobOpeningId(entity.getJobOpeningId())) {
                throw new ResourceNotFoundException("careers.job_opening", entity.getJobOpeningId());
            }
            assertValidJobApplication(entity, true);
            scope.requireWritable(institutionOfJobApplication(entity));
            assertUniqueJobApplication(entity);
            if (validateOnly) {
                return jobApplicationMapper.toDto(entity);
            }
            JobApplicationEntity stored = jobApplicationRepository.insert(entity);
            audit.recordCreate(JOB_APPLICATION_AGGREGATE, stored.getId());
            outbox.publish(JOB_APPLICATION_AGGREGATE + CareersEvents.CREATED, JOB_APPLICATION_AGGREGATE, stored.getId(),
                    CareersEvents.payload(stored.getId()));
            return jobApplicationMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match. With an updateMask the listed fields are set (absent ones cleared); without one,
     * the non-null body fields apply. The merged row is validated as a whole, and must stay within the caller's scope.
     */
    public JobApplication updateJobApplication(UUID jobOpeningId, UUID id, String ifMatch, JobApplicationUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, JOB_APPLICATION_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(JOB_APPLICATION_PERMISSION_UPDATE);
            JobApplicationEntity entity = loadVisibleJobApplication(scope, jobOpeningId, id);
            scope.requireWritable(institutionOfJobApplication(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applyJobApplication(entity, changes, fields);
            validator.validate(jobApplicationMapper.toDto(entity));
            assertValidJobApplication(entity, false);
            scope.requireWritable(institutionOfJobApplication(entity));
            assertUniqueJobApplication(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return jobApplicationMapper.toDto(entity);
            }
            if (jobApplicationRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(JOB_APPLICATION_AGGREGATE, id, fields);
            outbox.publish(JOB_APPLICATION_AGGREGATE + CareersEvents.UPDATED, JOB_APPLICATION_AGGREGATE, id,
                    CareersEvents.payload(id));
            return jobApplicationMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteJobApplication(UUID jobOpeningId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(JOB_APPLICATION_PERMISSION_DELETE);
            JobApplicationEntity entity = loadVisibleJobApplication(scope, jobOpeningId, id);
            scope.requireWritable(institutionOfJobApplication(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (jobApplicationRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(JOB_APPLICATION_AGGREGATE, id);
            outbox.publish(JOB_APPLICATION_AGGREGATE + CareersEvents.DELETED, JOB_APPLICATION_AGGREGATE, id,
                    CareersEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private JobApplicationEntity loadVisibleJobApplication(DataScope scope, UUID jobOpeningId, UUID id) {
        JobApplicationEntity entity = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(JOB_APPLICATION_AGGREGATE, id));
        if (!scope.canRead(institutionOfJobApplication(entity)) || !jobOpeningId.equals(entity.getJobOpeningId())) {
            throw new ResourceNotFoundException(JOB_APPLICATION_AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row (none: the rows are tenant-level). */
    private Optional<UUID> institutionOfJobApplication(JobApplicationEntity entity) {
        return Optional.empty();
    }

    /**
     * Applies the selected JobApplicationUpdate fields; explicit per field so a mask can clear values (no reflection).
     */
    private static void applyJobApplication(JobApplicationEntity entity, JobApplicationUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "applicantUserId" -> entity.setApplicantUserId(changes.getApplicantUserId());
                case "resumeFileId" -> entity.setResumeFileId(changes.getResumeFileId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidJobApplication(JobApplicationEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getJobOpeningId() == null) {
            errors.add(new FieldError("jobOpeningId", "must not be null", "NotNull"));
        }
        if (entity.getApplicantUserId() == null) {
            errors.add(new FieldError("applicantUserId", "must not be null", "NotNull"));
        }
        if (entity.getResumeFileId() == null) {
            errors.add(new FieldError("resumeFileId", "must not be null", "NotNull"));
        }
        if (entity.getJobOpeningId() != null && !jobApplicationRepository.existsJobOpeningId(entity.getJobOpeningId())) {
            errors.add(new FieldError("jobOpeningId", "job opening does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueJobApplication(JobApplicationEntity entity) {
        if (jobApplicationRepository.takenJobOpeningIdApplicantUserId1(entity)) {
            throw new DuplicateResourceException(
                    "A job application with the same job opening id and applicant user id already exists.");
        }
    }
}
