package com.mysms.learning.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.learning.lms.api.model.Certificate;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
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
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.config.LmsEventsConfig;
import com.mysms.learning.lms.domain.CertificateEntity;
import com.mysms.learning.lms.mapper.CertificateMapper;
import com.mysms.learning.lms.repository.CertificateRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.error.ConflictException;
import com.mysms.learning.lms.api.model.CertificateVerification;
import com.mysms.learning.lms.api.model.DownloadLink;
import com.mysms.learning.lms.api.model.Operation;
import com.mysms.learning.lms.domain.CohortEntity;
import com.mysms.learning.lms.domain.OperationEntity;
import com.mysms.learning.lms.events.LmsEvents;
import com.mysms.learning.lms.mapper.OperationMapper;
import com.mysms.learning.lms.repository.CertificationRepository;
import com.mysms.learning.lms.repository.CohortMembershipRepository;
import com.mysms.learning.lms.repository.CohortRepository;
import com.mysms.learning.lms.repository.OperationRepository;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

/**
 * Business service of lms.certificate: audit and domain event commit in the same transaction as every change. Rows
 * outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class CertificateService extends AppTransactionManager {

    public static final String AGGREGATE = CertificateEntity.TABLE;
    static final String PERMISSION_LIST = "lms.certificate.read";
    static final String PERMISSION_GET = "lms.certificate.read";


    private final CertificateRepository repository;
    private final CertificateMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final CertificationRepository certification;
    private final CohortRepository cohorts;
    private final CohortMembershipRepository members;
    private final OperationRepository operations;
    private final OperationMapper operationMapper;
    private final MemberDirectory directory;
    private final PlatformFiles files;
    private final PublicReads publicReads;

    public CertificateService(CertificateRepository repository, CertificateMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox, @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock, CertificationRepository certification, CohortRepository cohorts,
            CohortMembershipRepository members, OperationRepository operations, OperationMapper operationMapper,
            MemberDirectory directory, PlatformFiles files, PublicReads publicReads) {

        super(database, transactions);
        this.publicReads = publicReads;
        this.directory = directory;
        this.files = files;
        this.certification = certification;
        this.cohorts = cohorts;
        this.members = members;
        this.operations = operations;
        this.operationMapper = operationMapper;
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public Certificate getCertificate(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Certificate> listCertificates(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, CertificateRepository.SORT_FIELDS, CertificateRepository.DEFAULT_ORDER,
                CertificateRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CertificateRepository.FILTER_FIELDS)
                .and(CertificateRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<CertificateEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /**
     * lms.DownloadCertificate: the caller's own certificate, or one staff who issue certificates reach in its
     * institution (anyone else's is 404), rendered as a PDF, stored as the caller's file at platform and answered as a
     * fresh download link (503 while platform cannot).
     */
    public DownloadLink downloadCertificate(UUID id) {
        com.mysms.authclient.AuthContext caller = CohortAccess.member();
        DataScope scope = DataScope.current(PERMISSION_GET);
        record Printed(CertificateEntity certificate, String title) {
        }
        Printed printed = doInReadTransaction(() -> {
            CertificateEntity entity = loadVisible(scope, id);
            boolean staff = caller.hasPermission(PERMISSION_GENERATE)
                    && DataScope.of(caller, PERMISSION_GENERATE).canRead(institutionOf(entity))
                    && (institutionOf(entity).isPresent() || caller.hasTenantWidePermission(PERMISSION_GENERATE));
            if (!caller.userId().equals(entity.getUserId()) && !staff) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            String title = certification.verification(id).map(CertificationRepository.Verification::title)
                    .orElse(null);
            return new Printed(entity, title);
        });
        CertificateEntity certificate = printed.certificate();
        String purpose = "Downloading a certificate";
        MemberDirectory.Member student = caller.userId().equals(certificate.getUserId()) ? directory.self(purpose)
                : Optional.ofNullable(directory.lookup(List.of(certificate.getUserId()), purpose)
                        .get(certificate.getUserId())).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        List<String> lines = new ArrayList<>();
        lines.add("Awarded to " + student.displayName());
        if (printed.title() != null) {
            lines.add("for completing " + printed.title());
        }
        if (certificate.getCertificateNumber() != null) {
            lines.add("Certificate number: " + certificate.getCertificateNumber());
        }
        lines.add("Issued on " + certificate.getIssueDate());
        if (certificate.getExpiryDate() != null) {
            lines.add("Valid until " + certificate.getExpiryDate());
        }
        byte[] pdf = Pdf.render("Certificate", lines);
        UUID fileId = files.write("certificate-" + (certificate.getCertificateNumber() == null ? id
                : certificate.getCertificateNumber()) + ".pdf", "application/pdf", pdf);
        return files.link(fileId, "certificateId");
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private CertificateEntity loadVisible(DataScope scope, UUID id) {
        CertificateEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.course the row belongs to. */
    private Optional<UUID> institutionOf(CertificateEntity entity) {
        return repository.institutionOf(entity.getCourseId());
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(CertificateEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (entity.getIssueDate() == null) {
            errors.add(new FieldError("issueDate", "must not be null", "NotNull"));
        }
        if (entity.getTemplateCode() == null) {
            errors.add(new FieldError("templateCode", "must not be null", "NotNull"));
        }
        if (entity.getCohortId() != null && !repository.existsCohortId(entity.getCohortId())) {
            errors.add(new FieldError("cohortId", "cohort does not exist", "NOT_FOUND"));
        }
        if (entity.getCourseId() != null && !repository.existsCourseId(entity.getCourseId())) {
            errors.add(new FieldError("courseId", "course does not exist", "NOT_FOUND"));
        }
        if (entity.getEvaluatorId() != null && !repository.existsEvaluatorId(entity.getEvaluatorId())) {
            errors.add(new FieldError("evaluatorId", "evaluator does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(CertificateEntity entity) {

    }

    // ---- issuing and verifying certificates ----

    static final String PERMISSION_GENERATE = "lms.cohort.generate_cohort_certificates";
    static final String PERMISSION_VERIFY = "lms.certificate.verify_certificate";
    static final String OPERATION_GENERATE = "lms.GenerateCohortCertificates";
    private static final String CERTIFICATE_NUMBER_PREFIX = "LMS-";

    /** One certificate to issue: to whom, for which course and/or cohort, on which template and dates. */
    public record Issue(UUID userId, UUID courseId, UUID cohortId, UUID evaluatorId, String templateCode,
            LocalDate issueDate, LocalDate expiryDate, boolean publish) {
    }

    /**
     * lms.GenerateCohortCertificates: issues the cohort's certificate to the given active students, or to every
     * active student whose final grade is published. A student who already holds the cohort's certificate is skipped;
     * a named user who is not an active student is 422. The cohort must be in the caller's scope (404), writable (403)
     * and offer certification (409 CERTIFICATION_DISABLED). The job runs at once and is answered as a completed
     * operation (202) whose result counts the certificates issued and skipped.
     */
    public Operation generateCohortCertificates(UUID cohortId, List<UUID> userIds, String templateCode,
            LocalDate issueDate, boolean publish) {
        DataScope scope = DataScope.current(PERMISSION_GENERATE);
        return doInWriteTransaction(() -> {
            CohortEntity cohort = cohorts.findForUpdate(cohortId)
                    .orElseThrow(() -> new ResourceNotFoundException(CohortAccess.COHORT, cohortId));
            if (!scope.canRead(CohortAccess.institutionOf(cohort))) {
                throw new ResourceNotFoundException(CohortAccess.COHORT, cohortId);
            }
            scope.requireWritable(CohortAccess.institutionOf(cohort));
            if (!Boolean.TRUE.equals(cohort.getCertificationEnabled())) {
                throw new ConflictException("CERTIFICATION_DISABLED", "The cohort does not issue certificates.");
            }
            List<UUID> chosen;
            if (userIds == null || userIds.isEmpty()) {
                chosen = certification.graduates(cohortId);
            } else {
                List<UUID> active = members.activeStudentIds(cohortId);
                chosen = userIds.stream().distinct().toList();
                if (!active.containsAll(chosen)) {
                    throw new ValidationFailedException(List.of(new FieldError("userIds",
                            "every user must be an active student of the cohort", "NOT_A_STUDENT")));
                }
            }
            int issued = 0;
            for (UUID student : chosen) {
                if (certification.holdsForCohort(cohortId, student)) {
                    continue;
                }
                issue(new Issue(student, null, cohortId, null, templateCode, issueDate, null, publish));
                issued++;
            }
            OperationEntity operation = OperationEntity.builder().operationName(OPERATION_GENERATE)
                    .resourceType(CohortAccess.COHORT).resourceId(cohortId).resultEntityType(CohortAccess.COHORT)
                    .resultEntityId(cohortId).startedByUserId(Actor.current().orElse(null)).build();
            operation.onCreatingData(Actor.current(), clock);
            operation.setTenantId(cohort.getTenantId());
            operation.setStartedAt(operation.getCreatedAt());
            operation.setFinishedAt(operation.getCreatedAt());
            Operation done = operationMapper.toDto(operations.insertCompleted(operation));
            done.setResult(Map.of("issued", issued, "skipped", chosen.size() - issued));
            audit.recordCreate("lms.operation", done.getOperationId());
            return done;
        });
    }

    /**
     * Writes one certificate (inside the caller's transaction) with a certificate number derived from its id, and its
     * audit record and event. Used by cohort generation and by issuing from a passed evaluation.
     */
    Certificate issue(Issue issue) {
        CertificateEntity entity = CertificateEntity.builder().userId(issue.userId()).courseId(issue.courseId())
                .cohortId(issue.cohortId()).evaluatorId(issue.evaluatorId()).templateCode(issue.templateCode())
                .issueDate(issue.issueDate()).expiryDate(issue.expiryDate()).isPublished(issue.publish()).build();
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        entity.setCertificateNumber(CERTIFICATE_NUMBER_PREFIX
                + entity.getId().toString().replace("-", "").toUpperCase(Locale.ROOT));
        if (issue.expiryDate() != null && issue.expiryDate().isBefore(issue.issueDate())) {
            throw new ValidationFailedException(List.of(new FieldError("expiryDate", "must not be before issueDate",
                    "INVALID_RANGE")));
        }
        assertValid(entity, true);
        CertificateEntity stored = repository.insert(entity);
        audit.recordCreate(AGGREGATE, stored.getId());
        outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
        return mapper.toDto(stored);
    }

    /**
     * lms.VerifyCertificate, for anyone who names the school (a guest by its school code): whether the certificate is
     * genuine and current — published and not expired — with its title and dates, never an id. An unknown or
     * unpublished certificate is answered {@code valid: false} with nothing else, so a verifier learns no more than
     * that. The student's name is not given: lms keeps no names, and a verifier may not read iam's accounts.
     */
    public CertificateVerification verifyCertificate(UUID id) {
        publicReads.requireTenant();
        LocalDate today = LocalDate.now(clock);
        return doInReadTransaction(() -> certification.verification(id).filter(CertificationRepository
                .Verification::isPublished).map(found -> {
                    boolean current = found.expiryDate() == null || !found.expiryDate().isBefore(today);
                    CertificateVerification verification = new CertificateVerification(current);
                    verification.setTitle(found.title());
                    verification.setIssueDate(found.issueDate());
                    verification.setExpiryDate(found.expiryDate());
                    return verification;
                }).orElseGet(() -> new CertificateVerification(false)));
    }
}
