package com.mysms.learning.lms.business;

import com.mysms.authclient.AuthContext;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.id.UuidV7;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.validation.DtoValidator;
import com.mysms.learning.lms.api.model.ExternalLink;
import com.mysms.learning.lms.api.model.PlagiarismReport;
import com.mysms.learning.lms.api.model.SubmitForPlagiarismCheckRequest;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.config.LmsEventsConfig;
import com.mysms.learning.lms.domain.PlagiarismReportEntity;
import com.mysms.learning.lms.errors.ProviderUnavailableException;
import com.mysms.learning.lms.events.LmsEvents;
import com.mysms.learning.lms.mapper.PlagiarismReportMapper;
import com.mysms.learning.lms.repository.AssignmentSubmissionWorkflowRepository;
import com.mysms.learning.lms.repository.ForumPostRepository;
import com.mysms.learning.lms.repository.LmsAssignmentSubmissionRepository;
import com.mysms.learning.lms.repository.PlagiarismReportRepository;
import com.mysms.learning.lms.repository.PlagiarismWorkflowRepository;
import com.mysms.learning.lms.repository.PlagiarismWorkflowRepository.AccountRow;
import com.mysms.learning.lms.repository.PlagiarismWorkflowRepository.ReportRow;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Plagiarism checks of submitted files through the tenant's plagiarism account and its provider: sending a file
 * (SubmitForPlagiarismCheck), the provider's callbacks (ReceivePlagiarismWebhook) and the report viewer link. Without
 * an account, or with an account of a provider lms has no adapter for, the operations answer 503 {@code
 * PLAGIARISM_NOT_CONFIGURED}; a provider that refuses or cannot be reached is 503 {@code PLAGIARISM_PROVIDER_FAILED}.
 */
@Service
public class PlagiarismWorkflowService extends AppTransactionManager {

    static final String AGGREGATE = PlagiarismReportEntity.TABLE;
    static final String SECRET_COLUMN = "lms.plagiarism_account.shared_secret_enc";
    static final String TENANT_KEY = "mysms_tenant";
    static final String REPORT_KEY = "mysms_report";
    static final long MAX_FILE_BYTES = 40L * 1024 * 1024;
    private static final List<String> CHECKABLE = List.of(".pdf", ".doc", ".docx", ".txt", ".rtf", ".odt", ".html",
            ".htm", ".ppt", ".pptx");
    private static final String ASSIGNMENT_SUBMISSION = "lms.assignment_submission";
    private static final String FORUM_POST = "lms.forum_post";

    private final PlagiarismWorkflowRepository repository;
    private final PlagiarismReportRepository reports;
    private final PlagiarismReportMapper mapper;
    private final Map<String, PlagiarismProvider> providers;
    private final PlatformFiles files;
    private final FieldCipher cipher;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final JsonMapper json;
    private final Clock clock;
    private final LmsAssignmentSubmissionRepository submissions;
    private final AssignmentSubmissionWorkflowRepository submissionWorkflows;
    private final ForumPostRepository posts;
    private final CohortAccess access;

    public PlagiarismWorkflowService(PlagiarismWorkflowRepository repository, PlagiarismReportRepository reports,
            PlagiarismReportMapper mapper, List<PlagiarismProvider> providers, PlatformFiles files,
            FieldCipher cipher, @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator, JsonMapper json,
            Clock clock, LmsAssignmentSubmissionRepository submissions,
            AssignmentSubmissionWorkflowRepository submissionWorkflows, ForumPostRepository posts,
            CohortAccess access) {
        super(database, transactions);
        this.submissions = submissions;
        this.submissionWorkflows = submissionWorkflows;
        this.posts = posts;
        this.access = access;
        this.repository = repository;
        this.reports = reports;
        this.mapper = mapper;
        this.providers = providers.stream().collect(Collectors.toMap(PlagiarismProvider::name, Function.identity()));
        this.files = files;
        this.cipher = cipher;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.json = json;
        this.clock = clock;
    }

    /**
     * Sends the attached file to the provider of the named account (else the tenant's default one). The caller must be
     * able to read the attachment and its file (platform decides); the file is a document of at most 40 MB.
     */
    public PlagiarismReport submitForPlagiarismCheck(SubmitForPlagiarismCheckRequest request) {
        validator.validate(request);
        UUID caller = AuthContext.require().userId();
        UUID tenant = CallContextHolder.require().tenantId().orElseThrow();
        AccountRow account = doInReadTransaction(() -> request.getPlagiarismAccountId() != null
                ? repository.account(request.getPlagiarismAccountId()).orElseThrow(() -> new ValidationFailedException(
                        List.of(new FieldError("plagiarismAccountId", "plagiarism account does not exist",
                                "NOT_FOUND"))))
                : repository.defaultAccount().orElseThrow(() -> notConfigured("The tenant has no default "
                        + "plagiarism account.")));
        PlagiarismProvider provider = provider(account);
        PlagiarismProvider.Account credentials = credentials(account, tenant);
        String submitterRef = doInReadTransaction(() -> repository.userRef(account.id(), caller))
                .orElse(caller.toString());
        PlatformFiles.Attachment attachment = files.attachment(request.getEntityAttachmentId(), "entityAttachmentId");
        PlatformFiles.Read file = files.read(attachment.fileId(), "entityAttachmentId", CHECKABLE, MAX_FILE_BYTES);
        UUID reportId = UuidV7.next(clock);
        String ref;
        try {
            ref = provider.submit(credentials, new PlagiarismProvider.Upload(file.fileName(), file.fileName(),
                    file.content(), account.accountRef(), submitterRef, Map.of(TENANT_KEY, tenant.toString(),
                            REPORT_KEY, reportId.toString())));
        } catch (PlagiarismProvider.Failure e) {
            throw failed();
        }
        String itemKind = itemKind(attachment.entityType());
        return doInWriteTransaction(() -> {
            repository.insert(reportId, account.id(), attachment.id(), attachment.fileId(), itemKind,
                    attachment.entityType(), attachment.entityId(), ref, now(), caller);
            audit.recordCreate(AGGREGATE, reportId);
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, reportId, LmsEvents.payload(reportId));
            return mapper.toDto(reports.findById(reportId).orElseThrow());
        });
    }

    /**
     * A short-lived link to the provider's report viewer ({@code view}: similarity, the default, or grademark — the
     * provider opens both in its viewer). The teaching staff of the checked record's cohort open it as teachers; the
     * record's own student (or, for any other record, who sent the file) as a student; anybody else gets 404.
     */
    public ExternalLink getPlagiarismReportLink(UUID id, String view) {
        if (view != null && !List.of("similarity", "grademark").contains(view)) {
            throw new ValidationFailedException(List.of(new FieldError("view", "must be similarity or grademark",
                    "AllowedValues")));
        }
        AuthContext caller = AuthContext.require();
        UUID tenant = CallContextHolder.require().tenantId().orElseThrow();
        record Reached(ReportRow report, boolean teacher) {
        }
        Reached reached = doInReadTransaction(() -> repository.report(id)
                .flatMap(found -> viewAs(found, caller.userId()).map(teacher -> new Reached(found, teacher))))
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        ReportRow report = reached.report();
        if (report.submissionRef() == null || report.accountId() == null) {
            throw new com.mysms.core.error.InvalidStateException("The file has not reached the provider.");
        }
        AccountRow account = doInReadTransaction(() -> repository.account(report.accountId()))
                .orElseThrow(() -> notConfigured("The report's plagiarism account no longer exists."));
        boolean teacher = reached.teacher();
        String viewer = doInReadTransaction(() -> repository.userRef(account.id(), caller.userId()))
                .orElse(caller.userId().toString());
        try {
            String url = provider(account).viewerUrl(credentials(account, tenant), report.submissionRef(), viewer,
                    teacher);
            return new ExternalLink().url(url).expiresAt(OffsetDateTime.now(clock).plusMinutes(15));
        } catch (PlagiarismProvider.Failure e) {
            throw failed();
        }
    }

    /**
     * A provider callback: its signature is checked with the report's account secret (401 otherwise), then the report
     * moves on — an upload processed asks for the similarity report, a finished report keeps its score. The tenant is
     * the one the submission was created in (its metadata, covered by the signature); an unknown submission is 404.
     */
    public void receivePlagiarismWebhook(String providerName, String signature, String timestamp, byte[] body) {
        PlagiarismProvider provider = providers.get(providerName);
        if (provider == null) {
            throw new ResourceNotFoundException("lms.plagiarism_provider", providerName);
        }
        Map<String, Object> parsed;
        try {
            parsed = json.readValue(body, Map.class);
        } catch (JacksonException e) {
            throw WebhookSignature.refused();
        }
        PlagiarismProvider.Event event = provider.event(parsed).orElse(null);
        if (event == null) {
            return;
        }
        UUID tenant;
        try {
            tenant = UUID.fromString(event.metadata().getOrDefault(TENANT_KEY, ""));
        } catch (IllegalArgumentException e) {
            throw WebhookSignature.refused();
        }
        String trace = CallContextHolder.current().map(CallContext::traceId).orElse("");
        CallContextHolder.callAs(CallContext.anonymous(trace).withIdentity(tenant, null), () -> {
            handle(provider, event, tenant, signature, timestamp, body);
            return null;
        });
    }

    private void handle(PlagiarismProvider provider, PlagiarismProvider.Event event, UUID tenant, String signature,
            String timestamp, byte[] body) {
        Optional<ReportRow> found = doInReadTransaction(() -> repository.byRef(event.submissionRef()));
        if (found.isEmpty()) {
            throw WebhookSignature.refused();
        }
        AccountRow account = doInReadTransaction(() -> repository.account(found.get().accountId()))
                .orElseThrow(WebhookSignature::refused);
        PlagiarismProvider.Account credentials = credentials(account, tenant);
        WebhookSignature.verify(credentials.secret(), timestamp, signature, body, clock.instant());
        String status;
        if (event.errorCode() != null) {
            status = "error";
        } else if (event.reportReady()) {
            status = "complete";
        } else {
            try {
                provider.requestSimilarity(credentials, event.submissionRef());
                status = "pending";
            } catch (PlagiarismProvider.Failure e) {
                status = "uploaded";
            }
        }
        String finalStatus = status;
        runInWriteTransaction(() -> {
            ReportRow report = repository.lockByRef(event.submissionRef()).orElseThrow(WebhookSignature::refused);
            repository.recordEvent(report.id(), report.rowVersion(), finalStatus, event.similarity(),
                    event.errorCode(), now());
            audit.recordTransition(AGGREGATE, report.id(), "receiveWebhook", List.of("status", "similarityScore"),
                    null);
            outbox.publish(AGGREGATE + ".receiveWebhook", AGGREGATE, report.id(), LmsEvents.payload(report.id()));
        });
    }

    /** How the caller opens the report: as a teacher (true), as its student (false), or not at all (empty). */
    private Optional<Boolean> viewAs(ReportRow report, UUID caller) {
        if (ASSIGNMENT_SUBMISSION.equals(report.entityType()) && report.entityId() != null) {
            return submissions.findById(report.entityId()).flatMap(submission -> {
                if (submissions.taught(submission.getId(), CohortAccess.reach())) {
                    return Optional.of(true);
                }
                boolean own = caller.equals(submission.getUserId()) || submission.getGroupId() != null
                        && submissionWorkflows.activeMember(submission.getGroupId(), caller);
                return own ? Optional.of(false) : Optional.empty();
            });
        }
        if (FORUM_POST.equals(report.entityType()) && report.entityId() != null) {
            return posts.findById(report.entityId()).flatMap(post -> {
                if (posts.cohortOfTopic(post.getForumTopicId()).map(access::teaches).orElse(false)) {
                    return Optional.of(true);
                }
                return caller.equals(post.getAuthorUserId()) ? Optional.of(false) : Optional.empty();
            });
        }
        return caller.equals(report.createdBy()) ? Optional.of(false) : Optional.empty();
    }

    private PlagiarismProvider provider(AccountRow account) {
        PlagiarismProvider provider = providers.get(account.provider());
        if (provider == null || account.apiUrl() == null || account.apiUrl().isBlank()
                || account.secretEnc() == null) {
            throw notConfigured("The plagiarism account has no supported provider, API URL or key.");
        }
        return provider;
    }

    private PlagiarismProvider.Account credentials(AccountRow account, UUID tenant) {
        String secret = cipher.decrypt(account.secretEnc(), tenant, SECRET_COLUMN)
                .orElseThrow(() -> notConfigured("The plagiarism account's key cannot be read."));
        return new PlagiarismProvider.Account(account.apiUrl(), account.accountRef(), secret);
    }

    static String itemKind(String entityType) {
        if (ASSIGNMENT_SUBMISSION.equals(entityType)) {
            return "assignment";
        }
        return entityType != null && entityType.startsWith("lms.forum") ? "discussion" : "other";
    }

    private static ProviderUnavailableException notConfigured(String detail) {
        return new ProviderUnavailableException(ProviderUnavailableException.PLAGIARISM_NOT_CONFIGURED, detail);
    }

    private static ProviderUnavailableException failed() {
        return new ProviderUnavailableException(ProviderUnavailableException.PLAGIARISM_PROVIDER_FAILED,
                "The plagiarism provider refused the request or could not be reached.");
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
