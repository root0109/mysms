package com.mysms.privacy.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.id.UuidV7;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.Patches;
import com.mysms.privacy.api.model.Breach;
import com.mysms.privacy.api.model.BreachStepRequest;
import com.mysms.privacy.api.model.BreachUpdate;
import com.mysms.privacy.api.model.MarkBreachNotABreachRequest;
import com.mysms.privacy.api.model.RecordBreachNotificationRequest;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.config.PrivacyEventsConfig;
import com.mysms.privacy.domain.BreachEntity;
import com.mysms.privacy.domain.BreachNotificationEntity;
import com.mysms.privacy.events.PrivacyEvents;
import com.mysms.privacy.mapper.BreachMapper;
import com.mysms.privacy.repository.BreachNotificationRepository;
import com.mysms.privacy.repository.BreachRepository;
import com.mysms.privacy.repository.NumberSequenceRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The breach register (privacy.breach with its notices, design §11): the data protection officer reports a breach,
 * keeps what is known of it up to date while it is open and records each step and each notice sent, against the
 * Board's 72-hour clock from the moment the school became aware. Every change writes its audit record and its event
 * (ids, number and status only) in the same transaction.
 *
 * <p>Breaches are tenant-level data: reading needs the permission, every change a tenant-wide grant, and nothing is
 * reported or changed while impersonating (403). An edit compares If-Match, a step its body etag (412); a step checks
 * the state it leaves (409). Notices are recorded, not sent: delivery through comms is not built, so the officer sends
 * them outside MySMS and records each here, which moves the status where the notice is a step of it — the Board's
 * initial intimation (a contained breach), its detailed report, the principals' notice. Overdue and the hours left
 * are derived at read time.
 */
@Service
public class BreachService extends AppTransactionManager {

    public static final String AGGREGATE = BreachEntity.TABLE;
    static final String PERMISSION_REPORT = "privacy.breach.report";
    static final String PERMISSION_UPDATE = "privacy.breach.update";
    static final String PERMISSION_TRIAGE = "privacy.breach.triage";
    static final String PERMISSION_CONTAIN = "privacy.breach.contain";
    static final String PERMISSION_RECORD_NOTIFICATION = "privacy.breach.record_notification";
    static final String PERMISSION_CLOSE = "privacy.breach.close";
    static final String PERMISSION_MARK_NOT_A_BREACH = "privacy.breach.mark_not_a_breach";
    /** The number series of breaches in privacy.number_sequence (per tenant and calendar year). */
    static final String NUMBER_SEQUENCE = "breach";

    /** No institution owns a row of this aggregate. */
    private static final Optional<UUID> TENANT_LEVEL = Optional.empty();
    /** The Board's detailed report is due this long after the school became aware. */
    private static final Duration BOARD_REPORT_WITHIN = Duration.ofHours(72);
    private static final long SECONDS_PER_HOUR = 3600;
    private static final Pattern MODULE = Pattern.compile("^[a-z][a-z_]{1,29}$");
    private static final String REPORTED = "reported";
    private static final String TRIAGED = "triaged";
    private static final String CONTAINED = "contained";
    private static final String BOARD_INITIAL_SENT = "board_initial_sent";
    private static final String BOARD_REPORT_SENT = "board_report_sent";
    private static final String PRINCIPALS_NOTIFIED = "principals_notified";
    private static final String CLOSED = "closed";
    private static final String NOT_A_BREACH = "not_a_breach";
    /** The states in which the Board has had its initial intimation and the breach is still open. */
    private static final Set<String> BOARD_INTIMATED = Set.of(BOARD_INITIAL_SENT, BOARD_REPORT_SENT,
            PRINCIPALS_NOTIFIED);
    private static final String BOARD = "board";
    private static final String PRINCIPALS = "principals";
    private static final String INITIAL = "initial";
    private static final String DETAILED = "detailed";
    private static final String IN_FUTURE = "must not be in the future";
    private static final String TIME_IN_FUTURE = "BREACH_TIME_IN_FUTURE";

    private static final Step TRIAGE = new Step("triage", PrivacyEvents.TRIAGED, in(REPORTED),
            "Only a reported breach can be triaged.", List.of("status"));
    private static final Step CONTAIN = new Step("contain", PrivacyEvents.CONTAINED, in(TRIAGED),
            "Only a triaged breach can be marked contained.", List.of("status"));
    private static final Step CLOSE = new Step("close", PrivacyEvents.CLOSED,
            in(PRINCIPALS_NOTIFIED).and(b -> b.getBoardReportSentAt() != null),
            "A breach is closed once the Board's detailed report and the principals' notice are recorded.",
            List.of("status", "closedAt"));
    private static final Step MARK_NOT_A_BREACH = new Step("mark_not_a_breach", PrivacyEvents.NOT_A_BREACH,
            in(TRIAGED), "Only a triaged breach can be found not to be a breach.",
            List.of("status", "notABreachReason", "closedAt"));

    /** A step: its audit name, its event, the states it leaves (refused with the sentence otherwise), what it changes. */
    private record Step(String name, String event, Predicate<BreachEntity> allowed, String refusal,
                        List<String> changedFields) {
    }

    /** The step's change of the loaded breach, at the step's time. */
    @FunctionalInterface
    private interface StepChange {
        void apply(BreachEntity breach, OffsetDateTime at);
    }

    private final BreachRepository repository;
    private final BreachNotificationRepository notifications;
    private final NumberSequenceRepository numbers;
    private final BreachMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public BreachService(BreachRepository repository,
            BreachNotificationRepository notifications,
            NumberSequenceRepository numbers,
            BreachMapper mapper,
            @Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PrivacyDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PrivacyEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(PrivacyEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.notifications = notifications;
        this.numbers = numbers;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public Breach getBreach(UUID id) {
        return doInReadTransaction(() -> {
            BreachEntity breach = load(id);
            return dto(breach, notifications.ofBreaches(List.of(id)), now());
        });
    }

    /** The register page by page, nearest Board deadline first; each breach with its notices. */
    public Page<Breach> listBreaches(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, BreachRepository.SORT_FIELDS, BreachRepository.DEFAULT_ORDER,
                BreachRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, BreachRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            OffsetDateTime now = now();
            Page<BreachEntity> rows = repository.list(page, where);
            Map<UUID, List<BreachNotificationEntity>> notices = notifications
                    .ofBreaches(rows.items().stream().map(BreachEntity::getId).toList()).stream()
                    .collect(Collectors.groupingBy(BreachNotificationEntity::getBreachId));
            return rows.map(b -> dto(b, notices.getOrDefault(b.getId(), List.of()), now));
        });
    }

    /**
     * privacy.ReportBreach: a new breach in the reported state, numbered PRB-year-number from the tenant's series of
     * the year (UTC), handled by the reporter, its Board report due 72 hours after the school became aware. Neither
     * time may lie in the future, nor awareness before detection (422).
     */
    public Breach reportBreach(Breach request) {
        requireTenantWide(PERMISSION_REPORT);
        validator.validate(request);
        AuthContext caller = AuthContext.require();
        refuseImpersonation(caller);
        OffsetDateTime detectedAt = utc(request.getDetectedAt());
        OffsetDateTime awareAt = utc(request.getBecameAwareAt());
        BreachEntity entity = BreachEntity.builder()
                .status(REPORTED)
                .detectedAt(detectedAt)
                .becameAwareAt(awareAt)
                .boardReportDueAt(awareAt.plus(BOARD_REPORT_WITHIN))
                .description(request.getDescription())
                .categories(orEmpty(request.getCategories()))
                .modules(orEmpty(request.getModules()))
                .estimatedPrincipals(request.getEstimatedPrincipals())
                .likelyConsequences(request.getLikelyConsequences())
                .mitigation(request.getMitigation())
                .ownerUserId(caller.userId())
                .build();
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(tenant());
        OffsetDateTime reportedAt = entity.getCreatedAt();
        List<FieldError> errors = new ArrayList<>();
        if (detectedAt.isAfter(reportedAt)) {
            errors.add(new FieldError("detectedAt", IN_FUTURE, TIME_IN_FUTURE));
        }
        if (awareAt.isAfter(reportedAt)) {
            errors.add(new FieldError("becameAwareAt", IN_FUTURE, TIME_IN_FUTURE));
        } else if (awareAt.isBefore(detectedAt)) {
            errors.add(new FieldError("becameAwareAt", "must not be earlier than when the breach was detected",
                    "BREACH_AWARE_BEFORE_DETECTED"));
        }
        return doInWriteTransaction(() -> {
            requireKnown(entity, errors);
            String year = String.valueOf(reportedAt.getYear());
            entity.setNumber(numbers.next(UuidV7.next(clock), NUMBER_SEQUENCE, year, "PRB-" + year + "-",
                    reportedAt, entity.getCreatedBy()));
            repository.insert(entity);
            audit.recordCreate(AGGREGATE, entity.getId());
            outbox.publish(AGGREGATE + PrivacyEvents.REPORTED, AGGREGATE, entity.getId(), PrivacyEvents.breach(entity));
            return dto(entity, List.of(), reportedAt);
        });
    }

    /** privacy.UpdateBreach: what is known of an open breach (409 once closed), under If-Match; the clock is fixed. */
    public Breach updateBreach(UUID id, String ifMatch, BreachUpdate changes) {
        requireTenantWide(PERMISSION_UPDATE);
        refuseImpersonation(AuthContext.require());
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            BreachEntity entity = load(id);
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (entity.getClosedAt() != null) {
                throw new InvalidStateException("A closed breach, or one found not to be a breach, cannot be changed.");
            }
            List<String> fields = apply(entity, changes);
            validator.validate(mapper.toDto(entity));
            requireKnown(entity, new ArrayList<>());
            if (fields.isEmpty()) {
                return dto(entity, notifications.ofBreaches(List.of(id)), now());
            }
            entity.onUpdatingData(Actor.current(), clock);
            if (repository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("The breach changed since it was read.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(AGGREGATE, id, fields);
            outbox.publish(AGGREGATE + PrivacyEvents.UPDATED, AGGREGATE, id, PrivacyEvents.breach(entity));
            return dto(entity, notifications.ofBreaches(List.of(id)), now());
        });
    }

    /** privacy.TriageBreach: reported → triaged. */
    public Breach triageBreach(UUID id, BreachStepRequest request) {
        requireTenantWide(PERMISSION_TRIAGE);
        validator.validate(request);
        return step(id, request.getEtag(), TRIAGE, null, (b, at) -> b.setStatus(TRIAGED));
    }

    /** privacy.ContainBreach: triaged → contained. */
    public Breach containBreach(UUID id, BreachStepRequest request) {
        requireTenantWide(PERMISSION_CONTAIN);
        validator.validate(request);
        return step(id, request.getEtag(), CONTAIN, null, (b, at) -> b.setStatus(CONTAINED));
    }

    /** privacy.CloseBreach: principals_notified → closed, once the Board's detailed report is recorded (409). */
    public Breach closeBreach(UUID id, BreachStepRequest request) {
        requireTenantWide(PERMISSION_CLOSE);
        validator.validate(request);
        return step(id, request.getEtag(), CLOSE, null, (b, at) -> {
            b.setStatus(CLOSED);
            b.setClosedAt(at);
        });
    }

    /** privacy.MarkBreachNotABreach: triaged → not_a_breach with its reason, which ends it. */
    public Breach markBreachNotABreach(UUID id, MarkBreachNotABreachRequest request) {
        requireTenantWide(PERMISSION_MARK_NOT_A_BREACH);
        validator.validate(request);
        String reason = request.getReason();
        return step(id, request.getEtag(), MARK_NOT_A_BREACH, reason, (b, at) -> {
            b.setStatus(NOT_A_BREACH);
            b.setNotABreachReason(reason);
            b.setClosedAt(at);
        });
    }

    /**
     * privacy.RecordBreachNotification: a notice the officer sent outside MySMS, kept with the breach. It is sent no
     * later than now and no earlier than the school became aware (422), and moves the status where it is a step
     * ({@link #afterNotice}).
     */
    public Breach recordBreachNotification(UUID id, RecordBreachNotificationRequest request) {
        requireTenantWide(PERMISSION_RECORD_NOTIFICATION);
        validator.validate(request);
        refuseImpersonation(AuthContext.require());
        String audience = request.getAudience().getValue();
        String kind = request.getKind().getValue();
        OffsetDateTime sentAt = utc(request.getSentAt());
        OptionalInt expected = Etags.bodyVersion(request.getEtag());
        return doInWriteTransaction(() -> {
            BreachEntity breach = load(id);
            requireVersion(breach, expected);
            OffsetDateTime now = now();
            if (sentAt.isAfter(now)) {
                throw invalid("sentAt", IN_FUTURE, TIME_IN_FUTURE);
            }
            if (sentAt.isBefore(breach.getBecameAwareAt())) {
                throw invalid("sentAt", "must not be earlier than when the school became aware of the breach",
                        "BREACH_NOTICE_BEFORE_AWARE");
            }
            String from = breach.getStatus();
            breach.setStatus(afterNotice(breach, audience, kind));
            if (BOARD.equals(audience) && DETAILED.equals(kind)) {
                breach.setBoardReportSentAt(sentAt);
            }
            BreachNotificationEntity notice = BreachNotificationEntity.builder()
                    .breachId(id)
                    .audience(audience)
                    .kind(kind)
                    .sentAt(sentAt)
                    .documentFileId(request.getDocumentFileId())
                    .build();
            notice.onCreatingData(Actor.current(), clock);
            notice.setTenantId(breach.getTenantId());
            notifications.insert(notice);
            advance(breach, from, now);
            audit.recordTransition(AGGREGATE, id, "record_notification",
                    List.of("status", "boardReportSentAt", "notifications"), null);
            outbox.publish(AGGREGATE + PrivacyEvents.NOTIFICATION_RECORDED, AGGREGATE, id,
                    PrivacyEvents.breachNotification(breach, notice));
            return dto(breach, notifications.ofBreaches(List.of(id)), now);
        });
    }

    /**
     * The status a notice leaves the breach in (409 when the breach cannot take it): the Board's initial intimation
     * moves a contained breach on; its detailed report, recorded once and only after the intimation, moves it to
     * board_report_sent unless the principals were told first; the principals' notice, after the intimation, moves it
     * to principals_notified. A Board update, a processor's or the tenant's notice leaves the status as it is.
     */
    private static String afterNotice(BreachEntity breach, String audience, String kind) {
        String status = breach.getStatus();
        if (breach.getClosedAt() != null) {
            throw new InvalidStateException("No notice is recorded for a closed breach or one that is not a breach.");
        }
        if (BOARD.equals(audience) && INITIAL.equals(kind)) {
            if (!CONTAINED.equals(status)) {
                throw new InvalidStateException("The Board's initial intimation is recorded once, for a contained"
                        + " breach.");
            }
            return BOARD_INITIAL_SENT;
        }
        boolean intimated = BOARD_INTIMATED.contains(status);
        if (BOARD.equals(audience) && !intimated) {
            throw new InvalidStateException("The Board's detailed report or an update follows its initial"
                    + " intimation.");
        }
        if (BOARD.equals(audience) && DETAILED.equals(kind)) {
            if (breach.getBoardReportSentAt() != null) {
                throw new InvalidStateException("The Board's detailed report is already recorded.");
            }
            return BOARD_INITIAL_SENT.equals(status) ? BOARD_REPORT_SENT : status;
        }
        if (PRINCIPALS.equals(audience)) {
            if (!intimated) {
                throw new InvalidStateException("The principals are told once the Board has been intimated.");
            }
            return PRINCIPALS_NOTIFIED;
        }
        return status;
    }

    /**
     * Runs one step: the breach loaded (404), the body etag compared (412), the state checked (409), then the
     * step's change, its guarded write (0 rows → 412), audit record and event, in one transaction.
     */
    private Breach step(UUID id, String etag, Step step, String reason, StepChange change) {
        refuseImpersonation(AuthContext.require());
        OptionalInt expected = Etags.bodyVersion(etag);
        return doInWriteTransaction(() -> {
            BreachEntity breach = load(id);
            requireVersion(breach, expected);
            if (!step.allowed().test(breach)) {
                throw new InvalidStateException(step.refusal());
            }
            OffsetDateTime now = now();
            String from = breach.getStatus();
            change.apply(breach, now);
            advance(breach, from, now);
            audit.recordTransition(AGGREGATE, id, step.name(), step.changedFields(), reason);
            outbox.publish(AGGREGATE + step.event(), AGGREGATE, id, PrivacyEvents.breach(breach));
            return dto(breach, notifications.ofBreaches(List.of(id)), now);
        });
    }

    /** The step's guarded write of the breach as changed, leaving {@code from}; 0 rows → 412. */
    private void advance(BreachEntity breach, String from, OffsetDateTime at) {
        breach.setUpdatedAt(at);
        breach.setUpdatedBy(Actor.current().orElse(null));
        if (repository.advance(breach, from, breach.getRowVersion()) == 0) {
            throw new PreconditionFailedException("The breach changed since it was read.");
        }
        breach.setRowVersion(breach.getRowVersion() + 1);
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(BreachEntity entity, BreachUpdate changes) {
        Patches patch = new Patches();
        patch.apply("description", changes.getDescription(), entity::setDescription);
        patch.apply("categories", changes.getCategories(), v -> entity.setCategories(orEmpty(v)));
        patch.apply("modules", changes.getModules(), v -> entity.setModules(orEmpty(v)));
        patch.apply("estimatedPrincipals", changes.getEstimatedPrincipals(), entity::setEstimatedPrincipals);
        patch.apply("likelyConsequences", changes.getLikelyConsequences(), entity::setLikelyConsequences);
        patch.apply("mitigation", changes.getMitigation(), entity::setMitigation);
        patch.apply("ownerUserId", changes.getOwnerUserId(), entity::setOwnerUserId);
        return patch.applied();
    }

    /**
     * Adds the errors of the categories (codes of the catalogue) and modules (module codes) to those found so far,
     * and refuses them all at once (422).
     */
    private void requireKnown(BreachEntity breach, List<FieldError> errors) {
        List<String> categories = breach.getCategories();
        if (!categories.isEmpty() && !repository.knownCategories(categories).containsAll(categories)) {
            errors.add(new FieldError("categories", "must name data categories of the catalogue",
                    "BREACH_CATEGORY_UNKNOWN"));
        }
        if (!breach.getModules().stream().allMatch(m -> MODULE.matcher(m).matches())) {
            errors.add(new FieldError("modules", "must name MySMS modules by their codes", "BREACH_MODULE_INVALID"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private BreachEntity load(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
    }

    /** The breach as the contract shows it at {@code now}: with its notices and the Board clock. */
    private Breach dto(BreachEntity breach, List<BreachNotificationEntity> notices, OffsetDateTime now) {
        Breach dto = mapper.toDto(breach).notifications(notices.stream().map(mapper::toDto).toList());
        if (breach.getBoardReportSentAt() != null || NOT_A_BREACH.equals(breach.getStatus())) {
            return dto.overdue(false);
        }
        long seconds = Duration.between(now, breach.getBoardReportDueAt()).getSeconds();
        return dto.overdue(seconds < 0).hoursRemaining(Math.toIntExact(Math.floorDiv(seconds, SECONDS_PER_HOUR)));
    }

    private static void requireVersion(BreachEntity breach, OptionalInt expected) {
        if (expected.isPresent() && expected.getAsInt() != breach.getRowVersion()) {
            throw new PreconditionFailedException("The etag does not match the current version.");
        }
    }

    private static List<String> orEmpty(List<String> given) {
        return given == null ? List.of() : List.copyOf(given);
    }

    /** A one-field 422 with its code (the sentence comes from the message catalogue in the caller's language). */
    private static ValidationFailedException invalid(String field, String message, String code) {
        return new ValidationFailedException(List.of(new FieldError(field, message, code)));
    }

    private static Predicate<BreachEntity> in(String... states) {
        Set<String> allowed = Set.of(states);
        return breach -> allowed.contains(breach.getStatus());
    }

    /** Support staff acting as a user may read the register, never report or change a breach. */
    private static void refuseImpersonation(AuthContext caller) {
        if (caller.impersonatorId().isPresent()) {
            throw new PermissionDeniedException("Breaches cannot be reported or changed while acting as another user.");
        }
    }

    /** Refuses a change without a tenant-wide grant of the permission (403): breaches are tenant-level. */
    private static void requireTenantWide(String permission) {
        DataScope.current(permission).requireWritable(TENANT_LEVEL);
    }

    private OffsetDateTime now() {
        return utc(OffsetDateTime.now(clock));
    }

    private static OffsetDateTime utc(OffsetDateTime time) {
        return time.withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    private static UUID tenant() {
        return CallContextHolder.require().tenantId().orElseThrow();
    }
}
