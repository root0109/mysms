package com.mysms.privacy.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ConflictException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PermissionDeniedException;
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
import com.mysms.privacy.api.model.ConsentRecord;
import com.mysms.privacy.api.model.MyConsent;
import com.mysms.privacy.api.model.RecordConsentOnBehalfRequest;
import com.mysms.privacy.api.model.RecordMyConsentRequest;
import com.mysms.privacy.api.model.VerifyConsentRecordRequest;
import com.mysms.privacy.api.model.WithdrawMyConsentRequest;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.config.PrivacyEventsConfig;
import com.mysms.privacy.domain.ConsentRecordEntity;
import com.mysms.privacy.domain.NoticeTranslationEntity;
import com.mysms.privacy.domain.NoticeVersionEntity;
import com.mysms.privacy.events.PrivacyEvents;
import com.mysms.privacy.mapper.ConsentRecordMapper;
import com.mysms.privacy.model.ConsentScope;
import com.mysms.privacy.model.Majority;
import com.mysms.privacy.model.RequestOrigin;
import com.mysms.privacy.repository.ConsentRecordRepository;
import com.mysms.privacy.repository.ConsentRecordRepository.GivenConsent;
import com.mysms.privacy.repository.ConsentStateRepository;
import com.mysms.privacy.repository.ConsentStateRepository.Allowed;
import com.mysms.privacy.repository.GuardianLinkRepository;
import com.mysms.privacy.repository.NoticeTranslationRepository;
import com.mysms.privacy.repository.NoticeVersionRepository;
import com.mysms.privacy.repository.NoticeVersionRepository.ActiveNotice;
import com.mysms.privacy.repository.PurposeRepository;
import com.mysms.privacy.repository.PurposeView;
import com.mysms.privacy.repository.SubjectRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The consent record aggregate (privacy.consent_record) with what every change of it writes in the same transaction:
 * an entry of the evidence ledger ({@link ConsentEvidenceLedger}), the recomputed effective state
 * ({@link ConsentStateWriter}), the audit record and the record's event.
 *
 * <p>Self-service (the consent centre): the giver is always the caller's own person from the token, never a field.
 * The caller decides for themself only as an adult, and for a subject only as the active legal guardian of a minor
 * (identified by persons, not accounts); anything else is 404 NOT_FOUND (design §6.1's CONSENT_NOT_GUARDIAN case), so
 * the answer never confirms that a child exists. The centre shows the caller's own decision and the combined
 * effect, never another giver's decision. A minor sees their own rows read only: the combined effect and the day
 * they come of age, nothing of who decided what.
 *
 * <p>Staff: a signed paper form is recorded on its giver's behalf as unverified — it counts only once a second staff
 * member verifies it (four eyes). Consent data is tenant-level: reading needs the permission, writing a tenant-wide
 * grant. No consent decision is made while impersonating (403 PERMISSION_DENIED, design §13's
 * CONSENT_IMPERSONATION_FORBIDDEN case).
 *
 * <p>A newer decision of a giver supersedes their live one. Every write locks the subject's row first, then the
 * tenant's evidence chain.
 */
@Service
public class ConsentService extends AppTransactionManager {

    public static final String AGGREGATE = ConsentRecordEntity.TABLE;
    static final String PERMISSION_RECORD_ON_BEHALF = "privacy.consent_record.record_on_behalf";
    static final String PERMISSION_VERIFY = "privacy.consent_record.verify";

    /** No institution owns a row of this aggregate. */
    private static final Optional<UUID> TENANT_LEVEL = Optional.empty();
    private static final String SELF = "self";
    private static final String GRANTED = "granted";
    private static final String ACTIVE = "active";
    private static final String VERIFIED = "verified";
    private static final String UNVERIFIED = "unverified";
    private static final String WEB = "web";
    private static final String PAPER_FORM = "paper_form";
    private static final String EXISTING_RECORD = "existing_record";
    private static final String PAPER_FORM_COUNTERSIGNED = "paper_form_countersigned";
    private static final String RECORDED = "recorded";
    private static final String SUPERSEDED = "superseded";
    private static final String WITHDRAWN = "withdrawn";
    private static final String SYSTEM = "system";
    /** What a 404 names when the giver may not decide for the subject: a person, never confirmed to exist. */
    private static final String SUBJECT = "person";
    private static final String NOTICE_NOT_ACTIVE = "must name a notice whose active version lists this purpose";

    /**
     * A decision to record: who decided, for whom, in which capacity, about what, under which notice, and how it came
     * in (the channel, whether it is verified already, who recorded a paper form and its scan).
     */
    private record Decision(UUID subjectPersonId, UUID giverPersonId, String giverCapacity, String purposeCode,
                            String scope, String decision, UUID noticeVersionId, String noticeLanguage,
                            String channel, String verificationStatus, String verificationMethod,
                            UUID recordedByUserId, UUID evidenceFileId) {
    }

    /** What the checks of a decision found: its purpose, and the hash of the notice wording the giver was shown. */
    private record Checked(PurposeView purpose, String noticeContentSha256) {
    }

    /**
     * What every consent-centre row of one purpose shares: the purpose with the tenant's setting, the notice a decision
     * is recorded under (none while unpublished), and since when the notice asks everyone again.
     */
    private record PurposeRow(PurposeView purpose, Optional<ActiveNotice> notice,
                              Optional<OffsetDateTime> reconsentSince) {
    }

    private final ConsentRecordRepository records;
    private final ConsentRecordMapper mapper;
    private final SubjectRepository subjects;
    private final GuardianLinkRepository links;
    private final PurposeRepository purposes;
    private final NoticeVersionRepository noticeVersions;
    private final NoticeTranslationRepository noticeTranslations;
    private final ConsentStateRepository states;
    private final ConsentStateWriter stateWriter;
    private final ConsentEvidenceLedger ledger;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final ConsentExpiryJob expiryJob;

    public ConsentService(ConsentRecordRepository records,
            ConsentRecordMapper mapper,
            SubjectRepository subjects,
            GuardianLinkRepository links,
            PurposeRepository purposes,
            NoticeVersionRepository noticeVersions,
            NoticeTranslationRepository noticeTranslations,
            ConsentStateRepository states,
            ConsentStateWriter stateWriter,
            ConsentEvidenceLedger ledger,
            ConsentExpiryJob expiryJob,
            @Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PrivacyDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PrivacyEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(PrivacyEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock) {
        super(database, transactions);
        this.records = records;
        this.mapper = mapper;
        this.subjects = subjects;
        this.links = links;
        this.purposes = purposes;
        this.noticeVersions = noticeVersions;
        this.noticeTranslations = noticeTranslations;
        this.states = states;
        this.stateWriter = stateWriter;
        this.ledger = ledger;
        this.expiryJob = expiryJob;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    // ---- the consent centre (self-service) ----

    /**
     * privacy.ListMyConsents: one row per subject the caller sees — themself (deciding as an adult, read only as a
     * minor), then each minor they are the active legal guardian of — and purpose the tenant has switched on (a purpose
     * forbidden for minors is left out for a minor: nobody can consent to it). A person whose age is unknown sees no
     * row of their own: who decides for them cannot be told. Read on the primary: who may decide is a security
     * decision.
     */
    public Page<MyConsent> listMyConsents() {
        UUID caller = callerPerson(AuthContext.require());
        return doInPrimaryReadTransaction(() -> {
            Optional<Majority> majority = subjects.majority(caller);
            Map<UUID, MyConsent.CapacityEnum> subjectIds = new LinkedHashMap<>();
            majority.flatMap(ConsentService::ownCapacity).ifPresent(capacity -> subjectIds.put(caller, capacity));
            for (UUID ward : links.minorWards(caller)) {
                subjectIds.putIfAbsent(ward, MyConsent.CapacityEnum.GUARDIAN);
            }
            Map<String, GivenConsent> mine = records.givenBy(caller).stream()
                    .collect(Collectors.toMap(g -> key(g.subjectPersonId(), g.purposeCode()), g -> g));
            Map<String, ConsentScope> widest = widest(states.allowed(List.copyOf(subjectIds.keySet())));
            List<PurposeRow> enabled = purposeRows(purposes.enabled());
            LocalDate today = now().toLocalDate();
            List<MyConsent> rows = new ArrayList<>();
            subjectIds.forEach((subject, capacity) -> {
                for (PurposeRow purpose : enabled) {
                    if (capacity != MyConsent.CapacityEnum.SELF && purpose.purpose().forbiddenForMinors()) {
                        continue;
                    }
                    String key = key(subject, purpose.purpose().code());
                    MyConsent row = row(subject, capacity, purpose, Optional.ofNullable(widest.get(key)));
                    rows.add(capacity == MyConsent.CapacityEnum.MINOR_SELF
                            ? row.decidesFrom(majority.map(Majority::majorityDate).orElse(null))
                            : decided(row, Optional.ofNullable(mine.get(key)), purpose.reconsentSince(), today));
                }
            });
            return new Page<>(rows, 1, Math.max(rows.size(), 1), "");
        });
    }

    /**
     * privacy.RecordMyConsent: the caller's decision for themself (capacity self) or a minor they are the active legal
     * guardian of (capacity guardian). Recorded verified through the school's existing record of the person and link,
     * over the web channel, with the notice wording's hash, the client's address and a hash of its User-Agent.
     */
    public MyConsent recordMyConsent(RecordMyConsentRequest request, RequestOrigin origin) {
        validator.validate(request);
        AuthContext caller = AuthContext.require();
        refuseImpersonation(caller);
        UUID giver = callerPerson(caller);
        UUID subject = request.getSubjectPersonId();
        String capacity = giver.equals(subject) ? SELF : "guardian";
        Decision decision = new Decision(subject, giver, capacity, request.getPurposeCode(),
                request.getScope().getValue(), request.getDecision().getValue(), request.getNoticeVersionId(),
                request.getNoticeLanguage(), WEB, VERIFIED, EXISTING_RECORD, null, null);
        return doInWriteTransaction(() -> {
            record(decision, origin, caller);
            return myConsentOf(giver, subject, decision.purposeCode());
        });
    }

    /** privacy.WithdrawMyConsent: ends one of the caller's own live decisions; another's is 404. */
    public MyConsent withdrawMyConsent(UUID id, WithdrawMyConsentRequest request, RequestOrigin origin) {
        AuthContext caller = AuthContext.require();
        refuseImpersonation(caller);
        UUID giver = callerPerson(caller);
        OptionalInt expected = Etags.bodyVersion(request.getEtag());
        return doInWriteTransaction(() -> {
            ConsentRecordEntity record = records.findById(id)
                    .filter(r -> giver.equals(r.getGiverPersonId()) && live(r))
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            requireCurrent(record, expected);
            OffsetDateTime now = now();
            subjects.lockStatus(record.getSubjectPersonId(), now);
            UUID actor = Actor.current().orElse(null);
            if (records.withdraw(id, record.getRowVersion(), now, actor) == 0) {
                throw stale();
            }
            record.setWithdrawnAt(now);
            stepped(record, now, actor);
            ledger.append(step(record, WITHDRAWN, record.getGiverCapacity(), WEB, null, origin, caller));
            audit.recordTransition(AGGREGATE, id, "withdraw", List.of("withdrawnAt"), null);
            outbox.publish(AGGREGATE + PrivacyEvents.WITHDRAWN, AGGREGATE, id, PrivacyEvents.consentRecord(record));
            stateWriter.recompute(record.getSubjectPersonId(), record.getPurposeCode());
            return myConsentOf(giver, record.getSubjectPersonId(), record.getPurposeCode());
        });
    }

    /**
     * Withdraws, for a rights request, every live decision about the subject that one of the givers gave (every
     * giver's when {@code givers} is empty: an erasure), in the caller's transaction and under the subject's row lock,
     * which the caller takes first: per decision a {@code withdrawn} evidence entry (channel system, with the staff
     * member who fulfilled the request when there is one), the audit record and privacy.consent_record.withdrawn; then
     * the subject's answer to each purpose concerned is recomputed.
     */
    public void withdrawForRightsRequest(UUID subjectPersonId, Set<UUID> givers, UUID actorUserId) {
        runInWriteTransaction(() -> {
            OffsetDateTime now = now();
            subjects.lockStatus(subjectPersonId, now);
            List<ConsentRecordEntity> withdrawn = records.lockLiveAbout(subjectPersonId).stream()
                    .filter(r -> givers.isEmpty() || givers.contains(r.getGiverPersonId())).toList();
            for (ConsentRecordEntity record : withdrawn) {
                if (records.withdraw(record.getId(), record.getRowVersion(), now, actorUserId) == 0) {
                    throw stale();
                }
                record.setWithdrawnAt(now);
                stepped(record, now, actorUserId);
                ledger.append(new ConsentEvidenceLedger.Step(record.getTenantId(), record.getId(), WITHDRAWN,
                        actorUserId, null, record.getGiverCapacity(), SYSTEM, null, null, null, null));
                audit.recordTransition(AGGREGATE, record.getId(), "withdraw", List.of("withdrawnAt"), null);
                outbox.publish(AGGREGATE + PrivacyEvents.WITHDRAWN, AGGREGATE, record.getId(),
                        PrivacyEvents.consentRecord(record));
            }
            withdrawn.stream().map(ConsentRecordEntity::getPurposeCode).distinct()
                    .forEach(purpose -> stateWriter.recompute(subjectPersonId, purpose));
        });
    }

    // ---- staff: consent records of the tenant ----

    public ConsentRecord getConsentRecord(UUID id) {
        return doInReadTransaction(() -> mapper.toDto(load(id)));
    }

    public Page<ConsentRecord> listConsentRecords(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, ConsentRecordRepository.SORT_FIELDS,
                ConsentRecordRepository.DEFAULT_ORDER, ConsentRecordRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ConsentRecordRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> records.list(page, where).map(mapper::toDto));
    }

    /**
     * privacy.RecordConsentOnBehalf: a signed paper form, recorded by a staff member for the giver it names under the
     * same entitlement and notice checks as self-service; unverified until another staff member verifies it.
     */
    public ConsentRecord recordConsentOnBehalf(RecordConsentOnBehalfRequest request, RequestOrigin origin) {
        requireTenantWide(PERMISSION_RECORD_ON_BEHALF);
        validator.validate(request);
        AuthContext caller = AuthContext.require();
        refuseImpersonation(caller);
        Decision decision = new Decision(request.getSubjectPersonId(), request.getGiverPersonId(),
                request.getGiverCapacity().getValue(), request.getPurposeCode(), request.getScope().getValue(),
                request.getDecision().getValue(), request.getNoticeVersionId(), request.getNoticeLanguage(),
                PAPER_FORM, UNVERIFIED, null, caller.userId(), request.getEvidenceFileId());
        return doInWriteTransaction(() -> mapper.toDto(record(decision, origin, caller)));
    }

    /**
     * privacy.VerifyConsentRecord: a second staff member confirms an unverified live record (409 when it is not one,
     * 409 CONSENT_FOUR_EYES when the verifier recorded it); from then on it counts.
     */
    public ConsentRecord verifyConsentRecord(UUID id, VerifyConsentRecordRequest request, RequestOrigin origin) {
        requireTenantWide(PERMISSION_VERIFY);
        AuthContext caller = AuthContext.require();
        refuseImpersonation(caller);
        OptionalInt expected = Etags.bodyVersion(request.getEtag());
        return doInWriteTransaction(() -> {
            ConsentRecordEntity record = load(id);
            requireCurrent(record, expected);
            if (!UNVERIFIED.equals(record.getVerificationStatus()) || !live(record)) {
                throw new InvalidStateException("Only an unverified consent record that is still live can be verified.");
            }
            if (caller.userId().equals(record.getRecordedByUserId())) {
                throw new ConflictException("CONSENT_FOUR_EYES",
                        "The staff member who recorded the consent cannot also verify it.");
            }
            OffsetDateTime now = now();
            subjects.lockStatus(record.getSubjectPersonId(), now);
            if (records.verify(id, record.getRowVersion(), PAPER_FORM_COUNTERSIGNED, caller.userId(), now) == 0) {
                throw stale();
            }
            record.setVerificationStatus(VERIFIED);
            record.setVerificationMethod(PAPER_FORM_COUNTERSIGNED);
            record.setVerifiedByUserId(caller.userId());
            record.setVerifiedAt(now);
            stepped(record, now, caller.userId());
            ledger.append(step(record, VERIFIED, null, WEB, null, origin, caller));
            audit.recordTransition(AGGREGATE, id, "verify",
                    List.of("verificationStatus", "verificationMethod", "verifiedByUserId", "verifiedAt"), null);
            outbox.publish(AGGREGATE + PrivacyEvents.VERIFIED, AGGREGATE, id, PrivacyEvents.consentRecord(record));
            stateWriter.recompute(record.getSubjectPersonId(), record.getPurposeCode());
            return mapper.toDto(record);
        });
    }

    // ---- recording a decision ----

    /**
     * Records the decision: entitlement (404) before anything about the decision is checked (422), then the giver's
     * live decision is superseded, the new one inserted, both steps put in the ledger, and the state recomputed.
     */
    private ConsentRecordEntity record(Decision d, RequestOrigin origin, AuthContext caller) {
        OffsetDateTime now = now();
        purposes.lockSetting(d.purposeCode()); // before the subject's lock: ConsentStateWriter#recomputePurpose
        boolean adult = Majority.ADULT.equals(subjects.lockStatus(d.subjectPersonId(), now));
        boolean entitled = SELF.equals(d.giverCapacity())
                ? adult && d.giverPersonId().equals(d.subjectPersonId())
                : !adult && links.isLegalGuardian(d.giverPersonId(), d.subjectPersonId());
        if (!entitled) {
            throw new ResourceNotFoundException(SUBJECT, d.subjectPersonId());
        }
        Checked checked = check(d, adult);
        ConsentRecordEntity entity = ConsentRecordEntity.builder()
                .subjectPersonId(d.subjectPersonId())
                .giverPersonId(d.giverPersonId())
                .giverCapacity(d.giverCapacity())
                .purposeCode(d.purposeCode())
                .scope(d.scope())
                .decision(d.decision())
                .noticeVersionId(d.noticeVersionId())
                .noticeLanguage(d.noticeLanguage())
                .channel(d.channel())
                .verificationStatus(d.verificationStatus())
                .verificationMethod(d.verificationMethod())
                .recordedByUserId(d.recordedByUserId())
                .evidenceFileId(d.evidenceFileId())
                .build();
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(tenant());
        OffsetDateTime decidedAt = entity.getCreatedAt();
        entity.setValidFrom(decidedAt);
        entity.setVerifiedAt(VERIFIED.equals(d.verificationStatus()) ? decidedAt : null);
        entity.setValidUntil(Optional.ofNullable(checked.purpose().defaultValidMonths())
                .map(months -> decidedAt.toLocalDate().plusMonths(months)).orElse(null));
        if (entity.getValidUntil() != null) {
            expiryJob.enrol(decidedAt); // before the evidence chain's lock, which a concurrent first enrolment awaits
        }
        Optional<ConsentRecordEntity> live = records.lockLive(d.subjectPersonId(), d.purposeCode(), d.giverPersonId());
        if (live.isPresent()) {
            supersede(live.get(), entity, origin, caller);
        }
        records.insert(entity);
        ledger.append(step(entity, RECORDED, d.giverCapacity(), d.channel(), checked.noticeContentSha256(), origin,
                caller));
        audit.recordCreate(AGGREGATE, entity.getId());
        outbox.publish(AGGREGATE + PrivacyEvents.RECORDED, AGGREGATE, entity.getId(),
                PrivacyEvents.consentRecord(entity));
        stateWriter.recompute(d.subjectPersonId(), d.purposeCode());
        return entity;
    }

    /** The giver's live decision → superseded by the new one (its row is locked). */
    private void supersede(ConsentRecordEntity old, ConsentRecordEntity successor, RequestOrigin origin,
            AuthContext caller) {
        UUID actor = successor.getCreatedBy();
        if (records.supersede(old.getId(), old.getRowVersion(), successor.getId(), successor.getCreatedAt(),
                actor) == 0) {
            throw stale();
        }
        old.setSupersededById(successor.getId());
        stepped(old, successor.getCreatedAt(), actor);
        ledger.append(step(old, SUPERSEDED, successor.getGiverCapacity(), successor.getChannel(), null, origin,
                caller));
        audit.recordTransition(AGGREGATE, old.getId(), "supersede", List.of("supersededById"), null);
        outbox.publish(AGGREGATE + PrivacyEvents.SUPERSEDED, AGGREGATE, old.getId(), PrivacyEvents.consentRecord(old));
    }

    /**
     * The checks of a decision about what it decides (one 422 with every failing field): the purpose is switched on by
     * the tenant and active in the catalogue, not forbidden for a minor, decided at a scope the purpose takes within
     * the tenant's ceiling; the notice version is active and lists the purpose, and has a translation in the language.
     */
    private Checked check(Decision d, boolean adult) {
        List<FieldError> errors = new ArrayList<>();
        Optional<PurposeView> purpose = purposes.findByCode(d.purposeCode())
                .filter(p -> p.enabled() && ACTIVE.equals(p.status()));
        if (purpose.isEmpty()) {
            errors.add(new FieldError("purposeCode", "must name a purpose the school has switched on",
                    "CONSENT_PURPOSE_DISABLED"));
        }
        purpose.filter(p -> p.forbiddenForMinors() && !adult).ifPresent(p -> errors.add(new FieldError("purposeCode",
                "cannot be consented to for a minor, not even by a guardian", "CONSENT_FORBIDDEN_FOR_MINOR")));
        ConsentScope scope = ConsentScope.of(d.scope());
        purpose.filter(p -> !allows(p, scope)).ifPresent(p -> errors.add(new FieldError("scope",
                "must be a width the purpose takes and the school allows", "CONSENT_SCOPE_ABOVE_TENANT_MAX")));
        Optional<NoticeVersionEntity> version = activeVersionListing(d);
        Optional<NoticeTranslationEntity> shown =
                version.flatMap(v -> noticeTranslations.findByLanguage(v.getId(), d.noticeLanguage()));
        if (version.isEmpty()) {
            errors.add(new FieldError("noticeVersionId", NOTICE_NOT_ACTIVE, "CONSENT_NOTICE_NOT_ACTIVE"));
        } else if (shown.isEmpty()) {
            errors.add(new FieldError("noticeLanguage", NOTICE_NOT_ACTIVE, "CONSENT_NOTICE_NOT_ACTIVE"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        return new Checked(purpose.orElseThrow(), shown.orElseThrow().getContentSha256());
    }

    /** The notice version named, when it is active and asks consent for the purpose. */
    private Optional<NoticeVersionEntity> activeVersionListing(Decision d) {
        return noticeVersions.findById(d.noticeVersionId())
                .filter(v -> ACTIVE.equals(v.getStatus()) && v.getPurposeCodes().contains(d.purposeCode()));
    }

    /** The purpose is decided at this scope, and the tenant's ceiling (if any) reaches it. */
    private static boolean allows(PurposeView purpose, ConsentScope scope) {
        boolean takes = ConsentScope.forKind(purpose.scopeKind()).contains(scope);
        return takes && Optional.ofNullable(purpose.maxScope()).map(ConsentScope::of).map(scope::within).orElse(true);
    }

    // ---- the consent centre's rows ----

    /**
     * The caller's row for one subject and purpose, after a change of the caller's decision. The combined effect is
     * shown only to someone who still decides for the subject: a guardian whose link has ended, or a giver whose
     * subject is no longer theirs to decide for, may withdraw their own record but learns nothing of the child's
     * current state.
     */
    private MyConsent myConsentOf(UUID giver, UUID subject, String purposeCode) {
        Optional<GivenConsent> mine = records.givenBy(giver).stream()
                .filter(g -> g.subjectPersonId().equals(subject) && g.purposeCode().equals(purposeCode)).findFirst();
        Optional<ConsentScope> widest = decidesFor(giver, subject)
                ? Optional.ofNullable(widest(states.allowed(List.of(subject))).get(key(subject, purposeCode)))
                : Optional.empty();
        PurposeRow purpose = purposeRows(List.of(purposes.findByCode(purposeCode).orElseThrow())).get(0);
        MyConsent.CapacityEnum capacity = giver.equals(subject)
                ? MyConsent.CapacityEnum.SELF : MyConsent.CapacityEnum.GUARDIAN;
        return decided(row(subject, capacity, purpose, widest), mine, purpose.reconsentSince(), now().toLocalDate());
    }

    /** Whether the giver decides for the subject today: themself as an adult, or a minor as an active legal guardian. */
    private boolean decidesFor(UUID giver, UUID subject) {
        boolean adult = subjects.majority(subject).filter(m -> Majority.ADULT.equals(m.status())).isPresent();
        return giver.equals(subject) ? adult : !adult && links.isLegalGuardian(giver, subject);
    }

    /** How the caller sees their own row: deciding as an adult, read only as a minor; none while their age is unknown. */
    private static Optional<MyConsent.CapacityEnum> ownCapacity(Majority majority) {
        if (Majority.ADULT.equals(majority.status())) {
            return Optional.of(MyConsent.CapacityEnum.SELF);
        }
        boolean minor = Majority.MINOR.equals(majority.status()) && majority.majorityDate() != null;
        return minor ? Optional.of(MyConsent.CapacityEnum.MINOR_SELF) : Optional.empty();
    }

    /** The shared part of each purpose's rows, the active notices read in one statement. */
    private List<PurposeRow> purposeRows(List<PurposeView> enabled) {
        Map<UUID, ActiveNotice> notices = noticeVersions.activeNotices(
                enabled.stream().map(PurposeView::noticeId).filter(Objects::nonNull).toList());
        return enabled.stream().map(p -> new PurposeRow(p, Optional.ofNullable(p.noticeId()).map(notices::get),
                reconsentSince(p))).toList();
    }

    /** A row before the caller's decision: subject, purpose, what may be decided under which notice, the effect. */
    private static MyConsent row(UUID subject, MyConsent.CapacityEnum capacity, PurposeRow purpose,
            Optional<ConsentScope> widest) {
        PurposeView view = purpose.purpose();
        return new MyConsent().subjectPersonId(subject).purposeCode(view.code()).capacity(capacity)
                .noticeCode(purpose.notice().map(ActiveNotice::code).orElse(null))
                .noticeVersionId(purpose.notice().map(ActiveNotice::versionId).orElse(null))
                .allowedScopes(ConsentScope.forKind(view.scopeKind()).stream().filter(s -> allows(view, s))
                        .map(s -> MyConsent.AllowedScopesEnum.fromValue(s.value())).toList())
                .effectiveWidestScope(widest.map(s -> MyConsent.EffectiveWidestScopeEnum.fromValue(s.value()))
                        .orElse(null));
    }

    /** The row with the caller's own decision on it (not_answered without one). */
    private static MyConsent decided(MyConsent row, Optional<GivenConsent> mine,
            Optional<OffsetDateTime> reconsentSince, LocalDate today) {
        if (mine.isEmpty()) {
            return row.myStatus(MyConsent.MyStatusEnum.NOT_ANSWERED);
        }
        GivenConsent given = mine.get();
        return row.id(given.id()).scope(MyConsent.ScopeEnum.fromValue(given.scope()))
                .myStatus(myStatus(given, reconsentSince, today)).decidedAt(given.validFrom())
                .noticeVersion(given.noticeVersion()).channel(given.channel()).etag(Etags.of(given.rowVersion()));
    }

    /** The caller's own decision as the centre shows it: lapsed, to be given again, or granted / refused. */
    private static MyConsent.MyStatusEnum myStatus(GivenConsent given, Optional<OffsetDateTime> reconsentSince,
            LocalDate today) {
        LocalDate validUntil = given.validUntil();
        if (validUntil != null && validUntil.isBefore(today)) {
            return MyConsent.MyStatusEnum.EXPIRED;
        }
        boolean granted = GRANTED.equals(given.decision());
        if (granted && reconsentSince.filter(given.validFrom()::isBefore).isPresent()) {
            return MyConsent.MyStatusEnum.RECONSENT_REQUIRED;
        }
        return granted ? MyConsent.MyStatusEnum.GRANTED : MyConsent.MyStatusEnum.REFUSED;
    }

    /** The widest allowed scope of each subject's purpose, by {@link #key}. */
    private static Map<String, ConsentScope> widest(List<Allowed> allowed) {
        return allowed.stream().collect(Collectors.toMap(a -> key(a.subjectPersonId(), a.purposeCode()),
                Allowed::scope, (a, b) -> a.within(b) ? b : a));
    }

    private Optional<OffsetDateTime> reconsentSince(PurposeView purpose) {
        return Optional.ofNullable(purpose.noticeId()).flatMap(noticeVersions::reconsentSince);
    }

    private static String key(UUID subject, String purposeCode) {
        return subject + "|" + purposeCode;
    }

    // ---- shared ----

    private ConsentRecordEntity load(UUID id) {
        return records.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
    }

    /** Neither withdrawn nor superseded. */
    private static boolean live(ConsentRecordEntity record) {
        return record.getWithdrawnAt() == null && record.getSupersededById() == null;
    }

    /** The audit stamps and version a successful guarded update gave the row. */
    private static void stepped(ConsentRecordEntity record, OffsetDateTime at, UUID actor) {
        record.setUpdatedAt(at);
        record.setUpdatedBy(actor);
        record.setRowVersion(record.getRowVersion() + 1);
    }

    private static ConsentEvidenceLedger.Step step(ConsentRecordEntity record, String event, String capacity,
            String channel, String noticeContentSha256, RequestOrigin origin, AuthContext caller) {
        return new ConsentEvidenceLedger.Step(record.getTenantId(), record.getId(), event, caller.userId(),
                caller.personId().orElse(null), capacity, channel, noticeContentSha256, origin.ipAddress(),
                origin.userAgentHash(), caller.impersonatorId().orElse(null));
    }

    /** The caller's person (the token's pid): a consent decision is always a person's (403 without one). */
    private static UUID callerPerson(AuthContext caller) {
        return caller.personId()
                .orElseThrow(() -> new PermissionDeniedException("The caller's account is linked to no person."));
    }

    /** Support staff acting as a user may look at a consent centre, never decide in it. */
    private static void refuseImpersonation(AuthContext caller) {
        if (caller.impersonatorId().isPresent()) {
            throw new PermissionDeniedException("Consent decisions cannot be made while acting as another user.");
        }
    }

    /** Refuses a change without a tenant-wide grant of the permission (403): consent data is tenant-level. */
    private static void requireTenantWide(String permission) {
        DataScope.current(permission).requireWritable(TENANT_LEVEL);
    }

    /** 412 when the body's etag names another version of the row than the one read. */
    private static void requireCurrent(ConsentRecordEntity record, OptionalInt expected) {
        if (expected.isPresent() && expected.getAsInt() != record.getRowVersion()) {
            throw new PreconditionFailedException("The etag does not match the current version.");
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    private static UUID tenant() {
        return CallContextHolder.require().tenantId().orElseThrow();
    }

    private static PreconditionFailedException stale() {
        return new PreconditionFailedException("The consent record changed since it was read.");
    }
}
