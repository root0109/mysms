package com.mysms.students.careers.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.authclient.Guest;
import com.mysms.students.careers.api.model.CloseJobOpeningRequest;
import com.mysms.students.careers.api.model.EmailApplicantRequest;
import com.mysms.students.careers.api.model.ReportJobOpeningRequest;
import com.mysms.students.careers.api.model.UpdateApplicationStatusRequest;
import com.mysms.students.careers.config.CareersMailConfig;
import com.mysms.students.careers.integration.IamGrpcClient;
import com.mysms.core.error.ConflictException;
import com.mysms.core.mail.MailDelivery;
import java.util.Map;
import com.mysms.students.careers.api.model.JobApplication;
import com.mysms.students.careers.api.model.JobApplicationUpdate;
import com.mysms.students.careers.api.model.JobOpening;
import com.mysms.students.careers.api.model.JobOpeningUpdate;
import com.mysms.students.careers.api.model.ReopenJobOpeningRequest;
import com.mysms.students.careers.config.CareersDatabaseConfig;
import com.mysms.students.careers.config.CareersEventsConfig;
import com.mysms.students.careers.domain.JobApplicationEntity;
import com.mysms.students.careers.domain.JobOpeningEntity;
import com.mysms.students.careers.events.CareersEvents;
import com.mysms.students.careers.mapper.JobApplicationMapper;
import com.mysms.students.careers.mapper.JobOpeningMapper;
import com.mysms.students.careers.repository.JobApplicationRepository;
import com.mysms.students.careers.repository.JobOpeningRepository;
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
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.Patches;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of careers.job_opening and of the records that exist only as part of a JobOpening (JobApplication):
 * audit and domain event commit in the same transaction as every change. Rows outside the caller's institutions are
 * invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class JobOpeningService extends AppTransactionManager {

    public static final String AGGREGATE = JobOpeningEntity.TABLE;
    static final String PERMISSION_READ = "careers.job_opening.read";
    static final String PERMISSION_CREATE = "careers.job_opening.create";
    static final String PERMISSION_UPDATE = "careers.job_opening.update";
    static final String PERMISSION_DELETE = "careers.job_opening.delete";
    static final String PERMISSION_CLOSE_JOB_OPENING = "careers.job_opening.close_job_opening";
    static final String PERMISSION_REOPEN_JOB_OPENING = "careers.job_opening.reopen_job_opening";
    private static final String OPEN = "open";
    static final String PERMISSION_REPORT_JOB_OPENING = "careers.job_opening.report_job_opening";
    static final String JOB_APPLICATION_PERMISSION_EMAIL = "careers.job_application.email_applicant";
    static final String JOB_APPLICATION_PERMISSION_SET_STATUS = "careers.job_application.update_application_status";
    /**
     * The hiring steps: an application moves forward from submitted through shortlisted to
     * hired, may be rejected or withdrawn before it is decided, and a decided application (rejected, hired,
     * withdrawn) does not move again.
     */
    static final Map<String, Set<String>> JOB_APPLICATION_NEXT = Map.of(
            "submitted", Set.of("shortlisted", "rejected", "withdrawn"),
            "shortlisted", Set.of("hired", "rejected", "withdrawn"));
    /** Mail headers and bodies stay within what any SMTP relay accepts. */
    private static final int MAX_SUBJECT = 200;
    private static final int MAX_MESSAGE = 10_000;

    public static final String JOB_APPLICATION_AGGREGATE = JobApplicationEntity.TABLE;
    static final String JOB_APPLICATION_PERMISSION_LIST = "careers.job_application.read";
    static final String JOB_APPLICATION_PERMISSION_GET = "careers.job_application.read";
    static final String JOB_APPLICATION_PERMISSION_CREATE = "careers.job_application.create";
    static final String JOB_APPLICATION_PERMISSION_UPDATE = "careers.job_application.update";
    static final String JOB_APPLICATION_PERMISSION_DELETE = "careers.job_application.delete";
    /** The public's filter fields: none on what {@link #guestProjection} leaves out. */
    private static final Map<String, FieldSpec> GUEST_FILTER_FIELDS = JobOpeningRepository.FILTER_FIELDS.entrySet()
            .stream().filter(e -> !Set.of("postedByUserId", "createdBy", "updatedBy").contains(e.getKey()))
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

    private final JobOpeningRepository repository;
    private final JobOpeningMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final JobApplicationRepository jobApplicationRepository;
    private final JobApplicationMapper jobApplicationMapper;
    private final IamGrpcClient iam;
    private final MailDelivery mail;

    public JobOpeningService(JobOpeningRepository repository,
            JobOpeningMapper mapper,
            @Qualifier(CareersDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(CareersDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(CareersEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(CareersEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            JobApplicationRepository jobApplicationRepository,
            JobApplicationMapper jobApplicationMapper,
            IamGrpcClient iam,
            @Qualifier(CareersMailConfig.MAIL) MailDelivery mail) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.jobApplicationRepository = jobApplicationRepository;
        this.jobApplicationMapper = jobApplicationMapper;
        this.iam = iam;
        this.mail = mail;
    }

    /**
     * A job opening. A holder of {@code careers.job_opening.read} sees every opening; anyone else sees open ones only,
     * in the guest projection, and a closed one is 404 to them.
     */
    public JobOpening getJobOpening(UUID id) {
        if (mayReadAll()) {
            DataScope scope = DataScope.current(PERMISSION_READ);
            return doInReadTransaction(() -> mapper.toDto(loadVisible(scope, id)));
        }
        requireTenant();
        return doInReadTransaction(() -> repository.findById(id).filter(e -> OPEN.equals(e.getStatus()))
                .map(e -> guestProjection(mapper.toDto(e)))
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
    }

    /** The tenant's job openings, page by page; open ones only for the public. */
    public Page<JobOpening> listJobOpenings(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        boolean all = mayReadAll();
        if (!all) {
            requireTenant();
        }
        SortSpec sort = SortSpec.parse(sortBy, JobOpeningRepository.SORT_FIELDS, JobOpeningRepository.DEFAULT_ORDER,
                JobOpeningRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, all ? JobOpeningRepository.FILTER_FIELDS : GUEST_FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        if (all) {
            return doInReadTransaction(() -> repository.list(page, where).map(mapper::toDto));
        }
        return doInReadTransaction(() -> repository.list(page, where.and(JobOpeningRepository.OPEN_ONLY))
                .map(e -> guestProjection(mapper.toDto(e))));
    }

    /** Whether the caller reads the whole tenant (signed in with the read permission) rather than the public. */
    private static boolean mayReadAll() {
        return AuthContext.current().map(c -> c.hasPermission(PERMISSION_READ)).orElse(false);
    }

    /** A guest must have named its school (404 otherwise); a signed-in caller reads its own tenant. */
    private static void requireTenant() {
        if (Guest.isGuest()) {
            Guest.requireTenant();
        }
    }

    /** The public's view of an opening: who posted it and who changed it are internal. */
    private static JobOpening guestProjection(JobOpening opening) {
        opening.setPostedByUserId(null);
        opening.setCreatedBy(null);
        opening.setUpdatedBy(null);
        return opening;
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
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public JobOpening updateJobOpening(UUID id, String ifMatch, JobOpeningUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            JobOpeningEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = apply(entity, changes);
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
     * careers.CloseJobOpening: Close. Allowed while status is open, on a row the caller may see and write; otherwise
     * 409 and nothing changes.
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
     * careers.ReopenJobOpening: Reopen. Allowed while status is closed, on a row the caller may see and write;
     * otherwise 409 and nothing changes.
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

    /**
     * careers.ReportJobOpening: someone flags an opening as inappropriate. The opening must be one the caller may see
     * (an open one, for a caller who does not read every opening); the report is an audit record keeping the reason and
     * a {@code careers.job_opening.report} event (ids only) for whoever moderates the board. Nothing on the row changes.
     */
    public void reportJobOpening(UUID id, ReportJobOpeningRequest request) {
        validator.validate(request);
        if (request.getReason().isBlank()) {
            throw new ValidationFailedException(List.of(new FieldError("reason", "must not be blank", "NotBlank")));
        }
        boolean all = mayReadAll();
        runInWriteTransaction(() -> {
            JobOpeningEntity entity = repository.findById(id)
                    .filter(e -> all || OPEN.equals(e.getStatus()))
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            audit.recordTransition(AGGREGATE, entity.getId(), "report", List.of(), request.getReason().trim());
            outbox.publish(AGGREGATE + ".report", AGGREGATE, id, CareersEvents.payload(id));
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

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOf(JobOpeningEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(JobOpeningEntity entity, JobOpeningUpdate changes) {
        Patches patch = new Patches();
        patch.apply("postedByUserId", changes.getPostedByUserId(), entity::setPostedByUserId);
        patch.apply("title", changes.getTitle(), entity::setTitle);
        patch.apply("location", changes.getLocation(), entity::setLocation);
        patch.apply("countryId", changes.getCountryId(), entity::setCountryId);
        patch.apply("employmentType", changes.getEmploymentType(),
                v -> entity.setEmploymentType(v == null ? null : v.getValue()));
        patch.apply("workMode", changes.getWorkMode(), v -> entity.setWorkMode(v == null ? null : v.getValue()));
        patch.apply("description", changes.getDescription(), entity::setDescription);
        patch.apply("companyName", changes.getCompanyName(), entity::setCompanyName);
        patch.apply("companyWebsite", changes.getCompanyWebsite(), entity::setCompanyWebsite);
        patch.apply("companyEmail", changes.getCompanyEmail(), entity::setCompanyEmail);
        patch.apply("companyLogoFileId", changes.getCompanyLogoFileId(), entity::setCompanyLogoFileId);
        return patch.applied();
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
        if (!recruiting()) {
            where = where.and(JobApplicationRepository.ofApplicant(AuthContext.require().userId()));
        }
        SqlFragment visible = where;
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + jobOpeningId, clock);
        return doInReadTransaction(() -> {
            Page<JobApplicationEntity> rows = jobApplicationRepository.list(page, visible);
            return rows.map(jobApplicationMapper::toDto);
        });
    }

    /** Creates the caller's application; with validateOnly every check runs and nothing is written. */
    public JobApplication createJobApplication(UUID jobOpeningId, JobApplication request, boolean validateOnly) {
        validator.validate(request);
        JobApplicationEntity entity = jobApplicationMapper.toEntity(request);
        entity.setJobOpeningId(jobOpeningId);
        entity.setApplicantUserId(AuthContext.require().userId());
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
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public JobApplication updateJobApplication(UUID jobOpeningId, UUID id, String ifMatch, JobApplicationUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(JOB_APPLICATION_PERMISSION_UPDATE);
            JobApplicationEntity entity = loadVisibleJobApplication(scope, jobOpeningId, id);
            scope.requireWritable(institutionOfJobApplication(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyJobApplication(entity, changes);
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

    /**
     * careers.UpdateApplicationStatus: moves an application along {@link #JOB_APPLICATION_NEXT} under its body etag.
     * A step the current status does not allow, the same status included, is 409 and changes nothing.
     */
    public JobApplication updateApplicationStatus(UUID jobOpeningId, UUID id, UpdateApplicationStatusRequest request) {
        validator.validate(request);
        String status = request.getStatus().getValue();
        DataScope scope = DataScope.current(JOB_APPLICATION_PERMISSION_SET_STATUS);
        return doInWriteTransaction(() -> {
            JobApplicationEntity entity = loadVisibleJobApplication(scope, jobOpeningId, id);
            scope.requireWritable(institutionOfJobApplication(entity));
            OptionalInt expected = Etags.bodyVersion(request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!JOB_APPLICATION_NEXT.getOrDefault(entity.getStatus(), Set.of()).contains(status)) {
                throw new InvalidStateException("The application cannot move from " + entity.getStatus() + " to "
                        + status + ".");
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (jobApplicationRepository.setStatus(id, entity.getRowVersion(), status, now,
                    Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(JOB_APPLICATION_AGGREGATE, id, "set_status", List.of("status"),
                    entity.getStatus() + " -> " + status);
            outbox.publish(JOB_APPLICATION_AGGREGATE + ".set_status", JOB_APPLICATION_AGGREGATE, id,
                    CareersEvents.payload(id));
            return jobApplicationMapper.toDto(jobApplicationRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(JOB_APPLICATION_AGGREGATE, id)));
        });
    }

    /**
     * careers.EmailApplicant: writes to the applicant at their iam address; iam and SMTP are called outside any
     * transaction, and the message itself is not kept. 503 NOT_CONFIGURED without SMTP, 409 NO_EMAIL when the account
     * has no address.
     */
    public void emailApplicant(UUID id, EmailApplicantRequest request) {
        validator.validate(request);
        List<FieldError> errors = new ArrayList<>();
        if (request.getSubject().isBlank() || request.getSubject().length() > MAX_SUBJECT
                || request.getSubject().contains("\n") || request.getSubject().contains("\r")) {
            errors.add(new FieldError("subject", "must be one line of 1 to " + MAX_SUBJECT + " characters", "Size"));
        }
        if (request.getMessage().isBlank() || request.getMessage().length() > MAX_MESSAGE) {
            errors.add(new FieldError("message", "must be 1 to " + MAX_MESSAGE + " characters", "Size"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        mail.requireConfigured();
        DataScope scope = DataScope.current(JOB_APPLICATION_PERMISSION_EMAIL);
        UUID applicant = doInReadTransaction(() -> jobApplicationRepository.findById(id)
                .filter(e -> scope.canRead(institutionOfJobApplication(e)) && reaches(e))
                .orElseThrow(() -> new ResourceNotFoundException(JOB_APPLICATION_AGGREGATE, id))
                .getApplicantUserId());
        String address = iam.emailOf(applicant).orElseThrow(() -> new ConflictException("NO_EMAIL",
                "The applicant's account has no e-mail address."));
        mail.send(new MailDelivery.Message(address, request.getSubject().trim(), request.getMessage(), null));
        runInWriteTransaction(() -> {
            audit.recordTransition(JOB_APPLICATION_AGGREGATE, id, "email", List.of(), null);
            outbox.publish(JOB_APPLICATION_AGGREGATE + ".email", JOB_APPLICATION_AGGREGATE, id,
                    CareersEvents.payload(id));
        });
    }

    /**
     * The live row, if the caller may see it: another parent's row, a row outside the caller's scope, or another
     * applicant's application for a caller who is not recruiting, is 404.
     */
    private JobApplicationEntity loadVisibleJobApplication(DataScope scope, UUID jobOpeningId, UUID id) {
        JobApplicationEntity entity = jobApplicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(JOB_APPLICATION_AGGREGATE, id));
        if (!scope.canRead(institutionOfJobApplication(entity)) || !jobOpeningId.equals(entity.getJobOpeningId())
                || !reaches(entity)) {
            throw new ResourceNotFoundException(JOB_APPLICATION_AGGREGATE, id);
        }
        return entity;
    }

    /** An applicant reaches their own applications only; recruiting staff reach every one. */
    private static boolean reaches(JobApplicationEntity entity) {
        return recruiting() || AuthContext.require().userId().equals(entity.getApplicantUserId());
    }

    /**
     * Recruiting staff move applications through hiring; applications are tenant-level rows, so only a tenant-wide
     * grant of that step reaches them all.
     */
    private static boolean recruiting() {
        return AuthContext.require().hasTenantWidePermission(JOB_APPLICATION_PERMISSION_SET_STATUS);
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOfJobApplication(JobApplicationEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyJobApplication(JobApplicationEntity entity, JobApplicationUpdate changes) {
        Patches patch = new Patches();
        patch.apply("resumeFileId", changes.getResumeFileId(), entity::setResumeFileId);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidJobApplication(JobApplicationEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getJobOpeningId() == null) {
            errors.add(new FieldError("jobOpeningId", "must not be null", "NotNull"));
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
