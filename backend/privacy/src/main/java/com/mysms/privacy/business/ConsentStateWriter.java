package com.mysms.privacy.business;

import com.mysms.core.outbox.OutboxWriter;
import com.mysms.privacy.config.PrivacyEventsConfig;
import com.mysms.privacy.domain.ConsentRecordEntity;
import com.mysms.privacy.events.PrivacyEvents;
import com.mysms.privacy.model.ConsentFacts;
import com.mysms.privacy.model.ConsentScope;
import com.mysms.privacy.model.GiverDecision;
import com.mysms.privacy.model.ScopeState;
import com.mysms.privacy.repository.ConsentRecordRepository;
import com.mysms.privacy.repository.ConsentStateRepository;
import com.mysms.privacy.repository.GuardianLinkRepository;
import com.mysms.privacy.repository.NoticeVersionRepository;
import com.mysms.privacy.repository.PurposeRepository;
import com.mysms.privacy.repository.PurposeView;
import com.mysms.privacy.repository.SubjectRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * The recomputer of privacy.consent_state, which keeps it true: gathers the facts about one subject and one purpose, asks the
 * {@link EffectiveConsentRule} for every scope, and — only when an answer changed — writes all of the purpose's
 * answers under the subject's next state version and publishes privacy.consent_state.changed.
 *
 * <p>Runs inside the caller's write transaction of the subject's tenant, never one of its own, so the state changes
 * atomically with what caused it: a consent change, a subject's majority or legal guardians (the projections'
 * events), a tenant's setting of a purpose, a notice version that asks everyone again, and (later) the daily expiry
 * run. It locks the subject's row first (creating it, status unknown, when no event has named the person yet), so
 * recomputes of one subject never interleave and its state version only grows; a caller that also appends consent
 * evidence takes the subject's lock before the evidence chain's.
 */
@Component
public class ConsentStateWriter {

    private static final String GRANTED = "granted";
    private static final String VERIFIED = "verified";

    private final SubjectRepository subjects;
    private final GuardianLinkRepository links;
    private final PurposeRepository purposes;
    private final NoticeVersionRepository noticeVersions;
    private final ConsentRecordRepository records;
    private final ConsentStateRepository states;
    private final EffectiveConsentRule rule;
    private final OutboxWriter outbox;
    private final Clock clock;

    public ConsentStateWriter(SubjectRepository subjects, GuardianLinkRepository links,
            PurposeRepository purposes, NoticeVersionRepository noticeVersions, ConsentRecordRepository records,
            ConsentStateRepository states, EffectiveConsentRule rule,
            @Qualifier(PrivacyEventsConfig.OUTBOX) OutboxWriter outbox, Clock clock) {
        this.subjects = subjects;
        this.links = links;
        this.purposes = purposes;
        this.noticeVersions = noticeVersions;
        this.records = records;
        this.states = states;
        this.rule = rule;
        this.outbox = outbox;
        this.clock = clock;
    }

    /**
     * Recomputes every scope of the subject's purpose; whether the stored state changed (and the event was written).
     *
     * @throws IllegalStateException when the purpose code names no catalogue purpose (callers pass checked codes)
     */
    public boolean recompute(UUID subjectPersonId, String purposeCode) {
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.MICROS);
        String status = subjects.lockStatus(subjectPersonId, now);
        PurposeView purpose = purposes.findByCode(purposeCode)
                .orElseThrow(() -> new IllegalStateException("no catalogue purpose " + purposeCode));
        Optional<OffsetDateTime> reconsentSince = Optional.ofNullable(purpose.noticeId())
                .flatMap(noticeVersions::reconsentSince);
        List<GiverDecision> decisions = records.current(subjectPersonId, purposeCode).stream()
                .map(ConsentStateWriter::decision).toList();
        ConsentScope maxScope = Optional.ofNullable(purpose.maxScope()).map(ConsentScope::of).orElse(null);
        ConsentFacts facts = new ConsentFacts(subjectPersonId, status, purpose.forbiddenForMinors(),
                purpose.scopeKind(), purpose.enabled(), maxScope, decisions, links.legalGuardians(subjectPersonId),
                reconsentSince.orElse(null), now.toLocalDate());
        List<ScopeState> computed = rule.decide(facts);
        if (Set.copyOf(states.find(subjectPersonId, purposeCode)).equals(Set.copyOf(computed))) {
            return false;
        }
        long stateVersion = subjects.nextStateVersion(subjectPersonId);
        states.write(subjectPersonId, purposeCode, computed, stateVersion, now);
        outbox.publish(PrivacyEvents.CONSENT_STATE + PrivacyEvents.CHANGED, PrivacyEvents.CONSENT_STATE,
                subjectPersonId, PrivacyEvents.consentStateChanged(subjectPersonId, purposeCode, computed,
                        stateVersion));
        return true;
    }

    /**
     * Recomputes every purpose the subject has answers for, after a change to the subject's majority or legal
     * guardians. A purpose without answers needs none: the subject has no decision for it, and no answer already
     * counts as denied.
     */
    public void recomputeSubject(UUID subjectPersonId) {
        // the subject's lock first (as recompute takes it, creating an unknown subject's row), so a decision still
        // being written for the subject is seen once it commits
        subjects.lockStatus(subjectPersonId, OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC));
        for (String purposeCode : states.purposesOf(subjectPersonId)) {
            recompute(subjectPersonId, purposeCode);
        }
    }

    /**
     * Recomputes the purpose for every subject with answers for it, after the tenant's setting of the purpose or the
     * notice it names changed. The caller holds that setting's row lock (the update itself, or
     * {@code NoticeRepository#purposesNaming}), and a decision share-locks it before the subject's lock
     * ({@link PurposeRepository#lockSetting}), so no subject's first answer is written unseen in between. Subjects are
     * locked in person order, the order every caller locking two subjects keeps, so two runs never deadlock.
     */
    public void recomputePurpose(String purposeCode) {
        for (UUID subjectPersonId : states.subjectsOf(purposeCode)) {
            recompute(subjectPersonId, purposeCode);
        }
    }

    private static GiverDecision decision(ConsentRecordEntity record) {
        return new GiverDecision(record.getGiverPersonId(), GRANTED.equals(record.getDecision()),
                ConsentScope.of(record.getScope()), record.getValidFrom(), record.getValidUntil(),
                VERIFIED.equals(record.getVerificationStatus()));
    }
}
