package com.mysms.learning.lms.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ConflictException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.UnauthenticatedException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.id.UuidV7;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.validation.DtoValidator;
import com.mysms.learning.lms.api.model.ActivateExternalToolRequest;
import com.mysms.learning.lms.api.model.ExternalTool;
import com.mysms.learning.lms.api.model.LaunchExternalToolRequest;
import com.mysms.learning.lms.api.model.LtiLaunch;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.config.LmsEventsConfig;
import com.mysms.learning.lms.domain.ExternalToolEntity;
import com.mysms.learning.lms.errors.ProviderUnavailableException;
import com.mysms.learning.lms.events.LmsEvents;
import com.mysms.learning.lms.mapper.ExternalToolMapper;
import com.mysms.learning.lms.repository.ExternalToolRepository;
import com.mysms.learning.lms.repository.GradebookPostingRepository;
import com.mysms.learning.lms.repository.LtiRepository;
import com.mysms.learning.lms.repository.LtiRepository.Link;
import com.mysms.learning.lms.repository.LtiRepository.Placement;
import com.mysms.learning.lms.repository.LtiRepository.Setting;
import com.mysms.learning.lms.repository.LtiRepository.Tool;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * External tools over LTI 1.1: activating a paid tool with its licence key, signing a student's or teacher's launch,
 * and taking a tool's grade back (Basic Outcomes, OAuth 1.0a with a body hash). A tool is configured by its settings
 * {@code launch_url}, {@code consumer_key} and the secret {@code shared_secret}; without them a launch is 503
 * {@code LTI_NOT_CONFIGURED}. Grades come back only when {@code mysms.lms.lti.outcome-url} is set: the launch of a
 * graded placement then carries the outcome URL and a sealed result id naming the tenant, the placement and the
 * student, which a tool cannot forge.
 */
@Service
public class ExternalToolWorkflowService extends AppTransactionManager {

    static final String AGGREGATE = ExternalToolEntity.TABLE;
    static final String PERMISSION_ACTIVATE = "lms.external_tool.activate_external_tool";
    static final String PERMISSION_LAUNCH = "lms.external_tool_link.launch_external_tool";
    /** Who may grade an item launches as an instructor. */
    /** Who configures a resource's links may try a link out without a placement. */
    static final String PERMISSION_CONFIGURE = "lms.external_tool_link.update";
    static final String LAUNCH_URL = "launch_url";
    static final String CONSUMER_KEY = "consumer_key";
    static final String SHARED_SECRET = "shared_secret";
    static final String ACTIVATION_HASH = "activation_key_sha256";
    static final String ACTIVATED_AT = "activated_at";
    static final String SECRET_COLUMN = "lms.external_tool_setting.secret_value_enc";
    static final String SOURCED_ID_COLUMN = "lms.lti.result_sourcedid";
    static final Duration CLOCK_SKEW = Duration.ofMinutes(5);
    /** Who a tool's grade is recorded as changed by: lms itself, acting for the tool. */
    static final UUID TOOL_ACTOR = AuthContext.serviceUserId("lms");

    private final LtiRepository repository;
    private final ExternalToolRepository tools;
    private final ExternalToolMapper toolMapper;
    private final GradebookPostingRepository gradebook;
    private final GradebookPostings postings;
    private final FieldCipher cipher;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final String outcomeUrl;
    private final CohortAccess access;
    private final SecureRandom random = new SecureRandom();

    public ExternalToolWorkflowService(LtiRepository repository, ExternalToolRepository tools,
            ExternalToolMapper toolMapper, GradebookPostingRepository gradebook, GradebookPostings postings,
            FieldCipher cipher, @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator, Clock clock,
            @Value("${mysms.lms.lti.outcome-url}") String outcomeUrl, CohortAccess access) {
        super(database, transactions);
        this.access = access;
        this.repository = repository;
        this.tools = tools;
        this.toolMapper = toolMapper;
        this.gradebook = gradebook;
        this.postings = postings;
        this.cipher = cipher;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.outcomeUrl = outcomeUrl;
    }

    /**
     * Enables a paid tool for the tenant: the key's SHA-256 must equal the tool's {@code activation_key_sha256}
     * setting (422 otherwise). A tool that needs no licence, or is already enabled, is 409. Tenant administrators only.
     */
    public ExternalTool activateExternalTool(UUID id, ActivateExternalToolRequest request) {
        validator.validate(request);
        DataScope scope = DataScope.current(PERMISSION_ACTIVATE);
        UUID actor = AuthContext.require().userId();
        return doInWriteTransaction(() -> {
            Tool tool = repository.lockTool(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            scope.requireWritable(Optional.empty());
            if (!tool.paid()) {
                throw new InvalidStateException("The tool needs no licence.");
            }
            if (tool.included()) {
                throw new InvalidStateException("The tool is already enabled.");
            }
            Setting expected = repository.settings(id).get(ACTIVATION_HASH);
            String sent = sha256(request.getActivationKey().trim());
            if (expected == null || expected.value() == null || !MessageDigest.isEqual(
                    expected.value().trim().toLowerCase().getBytes(StandardCharsets.UTF_8),
                    sent.getBytes(StandardCharsets.UTF_8))) {
                throw new ValidationFailedException(List.of(new FieldError("activationKey",
                        "is not a valid licence key for this tool", "INVALID")));
            }
            OffsetDateTime now = now();
            repository.putSetting(UuidV7.next(clock), id, ACTIVATED_AT, now.toString(), now, actor);
            if (repository.include(id, tool.rowVersion(), now, actor) == 0) {
                throw new PreconditionFailedException("The tool changed meanwhile; read it again.");
            }
            audit.recordTransition(AGGREGATE, id, "activate", List.of("isIncluded"), null);
            outbox.publish(AGGREGATE + ".activate", AGGREGATE, id, LmsEvents.payload(id));
            return toolMapper.toDto(tools.findById(id).orElseThrow());
        });
    }

    /**
     * The signed LTI 1.1 launch of a resource link for the caller: a form the client posts to the tool. The link's
     * resource must be in the caller's institutions (404), its tool enabled for that institution (403) and, when
     * paid, activated (409 {@code TOOL_NOT_ACTIVATED}). A student launches from a placement of a cohort they study in,
     * as Learner; the cohort's teaching staff as Instructor (anyone else 403); the cohort is the LTI context, and a
     * graded placement lets the tool send the student's grade back. Without a placement (or from one of no cohort)
     * only who configures the resource's links may launch, to try the link out, as Learner without a context.
     */
    public LtiLaunch launchExternalTool(UUID resourceId, UUID linkId, LaunchExternalToolRequest request) {
        AuthContext caller = AuthContext.require();
        DataScope scope = DataScope.current(PERMISSION_LAUNCH);
        UUID tenant = CallContextHolder.require().tenantId().orElseThrow();
        return doInReadTransaction(() -> {
            Link link = repository.link(resourceId, linkId)
                    .filter(l -> scope.canRead(Optional.ofNullable(l.institutionId())))
                    .orElseThrow(() -> new ResourceNotFoundException("lms.external_tool_link", linkId));
            if (link.toolId() == null) {
                throw new InvalidStateException("The link names no tool.");
            }
            Tool tool = repository.tool(link.toolId())
                    .orElseThrow(() -> new InvalidStateException("The link's tool no longer exists."));
            if (tool.paid() && !tool.included()) {
                throw new ConflictException("TOOL_NOT_ACTIVATED", "The tool's licence has not been activated.");
            }
            List<UUID> enabledFor = repository.toolInstitutions(tool.id());
            if (!enabledFor.isEmpty() && (link.institutionId() == null || !enabledFor.contains(link.institutionId()))) {
                throw new PermissionDeniedException("The tool is not enabled for this institution.");
            }
            Optional<Placement> placement = Optional.empty();
            if (request != null && request.getContentItemId() != null) {
                placement = repository.placement(resourceId, request.getContentItemId())
                        .filter(p -> scope.canRead(Optional.ofNullable(p.institutionId())));
                if (placement.isEmpty()) {
                    throw new ValidationFailedException(List.of(new FieldError("contentItemId",
                            "is not a placement of this resource", "NOT_FOUND")));
                }
            }
            Optional<UUID> cohort = placement.map(Placement::cohortId);
            boolean instructor = cohort.isPresent() && access.requireMember(cohort.get(), PERMISSION_LAUNCH);
            if (cohort.isEmpty() && !configures(caller, link)) {
                throw new PermissionDeniedException("Launch the tool from a placement of one of your cohorts.");
            }
            Map<String, Setting> settings = repository.settings(tool.id());
            String launchUrl = plain(settings, LAUNCH_URL);
            String consumerKey = plain(settings, CONSUMER_KEY);
            String secret = secret(settings, tenant);
            if (launchUrl == null || !launchUrl.startsWith("https://") || consumerKey == null || secret == null) {
                throw new ProviderUnavailableException(ProviderUnavailableException.LTI_NOT_CONFIGURED,
                        "The tool has no https launch URL, consumer key or shared secret.");
            }
            Map<String, String> form = new LinkedHashMap<>();
            form.put("lti_message_type", "basic-lti-launch-request");
            form.put("lti_version", "LTI-1p0");
            form.put("resource_link_id", link.linkRef());
            if (link.title() != null) {
                form.put("resource_link_title", link.title());
            }
            form.put("user_id", caller.userId().toString());
            form.put("roles", instructor ? "Instructor" : "Learner");
            form.put("tool_consumer_instance_guid", tenant.toString());
            form.put("tool_consumer_info_product_family_code", "mysms");
            form.put("launch_presentation_locale", "en");
            form.put("launch_presentation_document_target", "window");
            placement.ifPresent(p -> {
                if (p.cohortId() != null) {
                    form.put("context_id", p.cohortId().toString());
                }
                if (!instructor && p.cohortId() != null && !outcomeUrl.isBlank() && gradedPlacement(p.id())) {
                    form.put("lis_outcome_service_url", outcomeUrl);
                    form.put("lis_result_sourcedid", sourcedId(tenant, p.id(), caller.userId(), tool.id()));
                }
            });
            OffsetDateTime now = OffsetDateTime.now(clock);
            form.put("oauth_consumer_key", consumerKey);
            form.put("oauth_signature_method", OAuth1Signature.METHOD);
            form.put("oauth_timestamp", Long.toString(now.toEpochSecond()));
            form.put("oauth_nonce", HexFormat.of().formatHex(nonce()));
            form.put("oauth_version", "1.0");
            form.put("oauth_callback", "about:blank");
            form.put("oauth_signature", OAuth1Signature.sign("POST", launchUrl, form, secret));
            LtiLaunch launch = new LtiLaunch().launchUrl(launchUrl).method("POST").formFields(form);
            launch.setExpiresAt(now.plus(CLOCK_SKEW));
            return launch;
        });
    }

    /**
     * A tool's grade (LTI 1.1 replaceResult): the result id names the tenant, placement, student and tool; the OAuth
     * header must be the tool's consumer key, signed with its secret over the outcome URL, with the body's hash and a
     * timestamp within five minutes — else 401. The score (0 to 1) of the item's points is posted to the student's
     * gradebook entry as automatic points.
     */
    public void receiveToolOutcome(String authorization, byte[] body) {
        if (outcomeUrl.isBlank()) {
            throw new ProviderUnavailableException(ProviderUnavailableException.LTI_NOT_CONFIGURED,
                    "Grade return from tools is not configured.");
        }
        Map<String, String> oauth = OAuth1Signature.header(authorization);
        LtiOutcome outcome = LtiOutcome.parse(body).orElseThrow(() -> new ValidationFailedException(List.of(
                new FieldError("body", "is not an LTI Basic Outcomes request", "INVALID"))));
        Sealed sealed = unseal(outcome.sourcedId());
        String trace = CallContextHolder.current().map(CallContext::traceId).orElse("");
        CallContextHolder.callAs(CallContext.anonymous(trace).withIdentity(sealed.tenant(), null), () -> {
            record(sealed, oauth, outcome, body);
            return null;
        });
    }

    private void record(Sealed sealed, Map<String, String> oauth, LtiOutcome outcome, byte[] body) {
        Map<String, Setting> settings = doInReadTransaction(() -> repository.settings(sealed.tool()));
        String secret = secret(settings, sealed.tenant());
        String consumerKey = plain(settings, CONSUMER_KEY);
        long timestamp = number(oauth.get("oauth_timestamp"));
        boolean fresh = Math.abs(clock.instant().getEpochSecond() - timestamp) <= CLOCK_SKEW.getSeconds();
        if (secret == null || consumerKey == null || !consumerKey.equals(oauth.get("oauth_consumer_key"))
                || !OAuth1Signature.METHOD.equals(oauth.get("oauth_signature_method")) || !fresh
                || !OAuth1Signature.bodyHash(body).equals(oauth.get("oauth_body_hash"))
                || !OAuth1Signature.verify("POST", outcomeUrl, oauth, secret)) {
            throw unsigned();
        }
        if (!LtiOutcome.REPLACE.equals(outcome.operation())) {
            throw new ValidationFailedException(List.of(new FieldError("body",
                    "only replaceResultRequest is supported", "AllowedValues")));
        }
        BigDecimal score = outcome.score();
        if (score == null || score.signum() < 0 || score.compareTo(BigDecimal.ONE) > 0) {
            throw new ValidationFailedException(List.of(new FieldError("body",
                    "the score must be a number from 0 to 1", "Range")));
        }
        runInWriteTransaction(() -> {
            GradebookPostingRepository.GradedItem item = gradebook.item(sealed.placement())
                    .filter(i -> i.cohortId() != null && i.graded())
                    .orElseThrow(() -> new InvalidStateException("The placement no longer takes grades."));
            postings.postAutomatic(item, sealed.student(), GradebookPostings.pointsOf(item,
                    score.multiply(BigDecimal.valueOf(100))), null, now(), TOOL_ACTOR);
        });
    }

    private static boolean configures(AuthContext caller, Link link) {
        Optional<UUID> institution = Optional.ofNullable(link.institutionId());
        return caller.hasPermission(PERMISSION_CONFIGURE) && (institution.isPresent()
                ? DataScope.of(caller, PERMISSION_CONFIGURE).canRead(institution)
                : caller.hasTenantWidePermission(PERMISSION_CONFIGURE));
    }

    private boolean gradedPlacement(UUID contentItemId) {
        return gradebook.item(contentItemId).map(GradebookPostingRepository.GradedItem::graded).orElse(false);
    }

    /** The result id handed to the tool: the tenant in clear, the rest sealed for that tenant. */
    private String sourcedId(UUID tenant, UUID placement, UUID student, UUID tool) {
        return tenant + "." + cipher.encrypt(placement + "|" + student + "|" + tool, tenant, SOURCED_ID_COLUMN);
    }

    private Sealed unseal(String sourcedId) {
        if (sourcedId == null || sourcedId.indexOf('.') <= 0) {
            throw unsigned();
        }
        try {
            int dot = sourcedId.indexOf('.');
            UUID tenant = UUID.fromString(sourcedId.substring(0, dot));
            String[] parts = cipher.decrypt(sourcedId.substring(dot + 1), tenant, SOURCED_ID_COLUMN)
                    .orElseThrow(ExternalToolWorkflowService::unsigned).split("\\|");
            return new Sealed(tenant, UUID.fromString(parts[0]), UUID.fromString(parts[1]), UUID.fromString(parts[2]));
        } catch (IllegalArgumentException | IllegalStateException | IndexOutOfBoundsException e) {
            // a forged or foreign id does not open; it is the tool's input, never a stored envelope
            throw unsigned();
        }
    }

    private String secret(Map<String, Setting> settings, UUID tenant) {
        Setting setting = settings.get(SHARED_SECRET);
        if (setting == null || setting.secretEnc() == null) {
            return null;
        }
        return cipher.decrypt(setting.secretEnc(), tenant, SECRET_COLUMN).orElse(null);
    }

    private static String plain(Map<String, Setting> settings, String key) {
        Setting setting = settings.get(key);
        return setting == null || setting.value() == null || setting.value().isBlank() ? null : setting.value().trim();
    }

    private static long number(String value) {
        try {
            return value == null ? 0 : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private byte[] nonce() {
        byte[] nonce = new byte[16];
        random.nextBytes(nonce);
        return nonce;
    }

    static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is part of every Java runtime", e);
        }
    }

    private static UnauthenticatedException unsigned() {
        return new UnauthenticatedException("INVALID_SIGNATURE", "The outcome request's signature does not verify.");
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    /** What a result id names. */
    private record Sealed(UUID tenant, UUID placement, UUID student, UUID tool) {
    }
}
