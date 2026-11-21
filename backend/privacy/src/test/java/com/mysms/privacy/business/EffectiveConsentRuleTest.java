package com.mysms.privacy.business;

import static com.mysms.privacy.model.ConsentReason.CONSENT;
import static com.mysms.privacy.model.ConsentReason.EXPIRED;
import static com.mysms.privacy.model.ConsentReason.FORBIDDEN_FOR_MINOR;
import static com.mysms.privacy.model.ConsentReason.NARROWER_GRANT;
import static com.mysms.privacy.model.ConsentReason.NOT_ANSWERED;
import static com.mysms.privacy.model.ConsentReason.PURPOSE_DISABLED;
import static com.mysms.privacy.model.ConsentReason.RECONSENT_REQUIRED;
import static com.mysms.privacy.model.ConsentReason.REFUSED;
import static com.mysms.privacy.model.ConsentScope.CLASS;
import static com.mysms.privacy.model.ConsentScope.INSTITUTION;
import static com.mysms.privacy.model.ConsentScope.INTER_SCHOOL;
import static com.mysms.privacy.model.ConsentScope.PUBLIC;
import static com.mysms.privacy.model.ConsentScope.TENANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.privacy.model.ConsentFacts;
import com.mysms.privacy.model.ConsentReason;
import com.mysms.privacy.model.ConsentScope;
import com.mysms.privacy.model.GiverDecision;
import com.mysms.privacy.model.ScopeState;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * The effective consent rule (design §4.2) as a truth table: adult, minor and unknown subjects × a second legal
 * guardian who granted, refused, granted narrower or stayed silent, then each modifier on its own — expiry,
 * re-consent, forbidden for minors, the tenant's switch and ceiling, unverified and unentitled givers — for width and
 * tenant-scoped purposes.
 */
class EffectiveConsentRuleTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 21);
    private static final OffsetDateTime DECIDED = OffsetDateTime.of(2026, 9, 1, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final UUID SUBJECT = UUID.randomUUID();
    private static final UUID MOTHER = UUID.randomUUID();
    private static final UUID FATHER = UUID.randomUUID();
    private static final UUID NEIGHBOUR = UUID.randomUUID();
    private static final String ADULT = "adult";
    private static final String MINOR = "minor";
    private static final String UNKNOWN = "unknown";
    private static final String WIDTH = "width";

    private final EffectiveConsentRule rule = new DefaultEffectiveConsentRule();

    /** Builds the facts of one case; defaults: a width purpose, switched on, no ceiling, no re-consent. */
    private static final class Case {
        private String status = MINOR;
        private boolean forbiddenForMinors;
        private String scopeKind = WIDTH;
        private boolean enabled = true;
        private ConsentScope maxScope;
        private OffsetDateTime reconsentSince;
        private final List<GiverDecision> decisions = new ArrayList<>();

        Case status(String value) {
            status = value;
            return this;
        }

        Case forbiddenForMinors() {
            forbiddenForMinors = true;
            return this;
        }

        Case tenantScoped() {
            scopeKind = "tenant";
            return this;
        }

        Case disabled() {
            enabled = false;
            return this;
        }

        Case ceiling(ConsentScope value) {
            maxScope = value;
            return this;
        }

        Case reconsentSince(OffsetDateTime value) {
            reconsentSince = value;
            return this;
        }

        Case grant(UUID giver, ConsentScope scope) {
            return decide(new GiverDecision(giver, true, scope, DECIDED, null, true));
        }

        Case refuse(UUID giver, ConsentScope scope) {
            return decide(new GiverDecision(giver, false, scope, DECIDED, null, true));
        }

        Case decide(GiverDecision decision) {
            decisions.add(decision);
            return this;
        }

        ConsentFacts facts() {
            return new ConsentFacts(SUBJECT, status, forbiddenForMinors, scopeKind, enabled, maxScope, decisions,
                    Set.of(MOTHER, FATHER), reconsentSince, TODAY);
        }
    }

    private Map<ConsentScope, ConsentReason> decide(Case c) {
        List<ScopeState> states = rule.decide(c.facts());
        for (ScopeState state : states) {
            assertThat(state.allowed()).as("%s allowed iff CONSENT", state.scope()).isEqualTo(state.reason() == CONSENT);
        }
        return states.stream().collect(Collectors.toMap(ScopeState::scope, ScopeState::reason));
    }

    private static Map<ConsentScope, ConsentReason> widths(ConsentReason classReason, ConsentReason institution,
            ConsentReason interSchool, ConsentReason publicReason) {
        return Map.of(CLASS, classReason, INSTITUTION, institution, INTER_SCHOOL, interSchool, PUBLIC, publicReason);
    }

    private static Map<ConsentScope, ConsentReason> all(ConsentReason reason) {
        return widths(reason, reason, reason, reason);
    }

    // ---- the grid: subject status × the second guardian's answer (the first granted public) ----

    @Test
    void aMinorOrUnknownSubjectFollowsTheMostRestrictiveGuardian() {
        for (String status : List.of(MINOR, UNKNOWN)) {
            assertThat(decide(new Case().status(status).grant(MOTHER, PUBLIC).grant(FATHER, PUBLIC)))
                    .as(status + ", both granted").isEqualTo(all(CONSENT));
            assertThat(decide(new Case().status(status).grant(MOTHER, PUBLIC).refuse(FATHER, INSTITUTION)))
                    .as(status + ", one refused from institution on")
                    .isEqualTo(widths(CONSENT, REFUSED, REFUSED, REFUSED));
            assertThat(decide(new Case().status(status).grant(MOTHER, PUBLIC).grant(FATHER, CLASS)))
                    .as(status + ", one granted narrower")
                    .isEqualTo(widths(CONSENT, NARROWER_GRANT, NARROWER_GRANT, NARROWER_GRANT));
            assertThat(decide(new Case().status(status).grant(MOTHER, PUBLIC)))
                    .as(status + ", the other silent: silence does not block").isEqualTo(all(CONSENT));
        }
    }

    @Test
    void anAdultSubjectDecidesAloneAndGuardiansNoLongerCount() {
        assertThat(decide(new Case().status(ADULT).grant(MOTHER, PUBLIC).grant(FATHER, PUBLIC)))
                .isEqualTo(all(NOT_ANSWERED));
        assertThat(decide(new Case().status(ADULT).grant(MOTHER, PUBLIC).refuse(FATHER, CLASS)))
                .isEqualTo(all(NOT_ANSWERED));
        assertThat(decide(new Case().status(ADULT).grant(SUBJECT, INSTITUTION).refuse(FATHER, CLASS)))
                .as("a narrower own grant leaves wider widths unanswered, never NARROWER_GRANT")
                .isEqualTo(widths(CONSENT, CONSENT, NOT_ANSWERED, NOT_ANSWERED));
        assertThat(decide(new Case().status(ADULT).refuse(SUBJECT, INTER_SCHOOL)))
                .isEqualTo(widths(NOT_ANSWERED, NOT_ANSWERED, REFUSED, REFUSED));
    }

    @Test
    void aMinorsOwnDecisionDoesNotCount() {
        assertThat(decide(new Case().status(MINOR).grant(SUBJECT, PUBLIC))).isEqualTo(all(NOT_ANSWERED));
        assertThat(decide(new Case().status(UNKNOWN).grant(SUBJECT, PUBLIC))).isEqualTo(all(NOT_ANSWERED));
    }

    @Test
    void silenceNeverAllows() {
        assertThat(decide(new Case())).isEqualTo(all(NOT_ANSWERED));
        assertThat(decide(new Case().status(ADULT))).isEqualTo(all(NOT_ANSWERED));
    }

    @Test
    void someoneWhoIsNoLegalGuardianDoesNotCount() {
        assertThat(decide(new Case().grant(NEIGHBOUR, PUBLIC))).isEqualTo(all(NOT_ANSWERED));
        assertThat(decide(new Case().grant(MOTHER, PUBLIC).refuse(NEIGHBOUR, CLASS))).isEqualTo(all(CONSENT));
    }

    // ---- modifiers ----

    @Test
    void anUnverifiedDecisionDoesNotCount() {
        GiverDecision unverified = new GiverDecision(MOTHER, true, PUBLIC, DECIDED, null, false);
        assertThat(decide(new Case().decide(unverified))).isEqualTo(all(NOT_ANSWERED));
        GiverDecision unverifiedRefusal = new GiverDecision(FATHER, false, CLASS, DECIDED, null, false);
        assertThat(decide(new Case().grant(MOTHER, PUBLIC).decide(unverifiedRefusal))).isEqualTo(all(CONSENT));
    }

    @Test
    void aDecisionCountsUpToItsLastDayAndThenLapses() {
        GiverDecision lastDayToday = new GiverDecision(MOTHER, true, PUBLIC, DECIDED, TODAY, true);
        assertThat(decide(new Case().decide(lastDayToday))).isEqualTo(all(CONSENT));
        GiverDecision lapsed = new GiverDecision(MOTHER, true, PUBLIC, DECIDED, TODAY.minusDays(1), true);
        assertThat(decide(new Case().decide(lapsed))).isEqualTo(all(EXPIRED));
        assertThat(decide(new Case().decide(lapsed).grant(FATHER, INSTITUTION)))
                .as("another guardian's live grant still counts")
                .isEqualTo(widths(CONSENT, CONSENT, NARROWER_GRANT, NARROWER_GRANT));
        GiverDecision lapsedRefusal = new GiverDecision(FATHER, false, CLASS, DECIDED, TODAY.minusDays(1), true);
        assertThat(decide(new Case().grant(MOTHER, PUBLIC).decide(lapsedRefusal))).isEqualTo(all(CONSENT));
    }

    @Test
    void aGrantBeforeANoticeAskingAgainNoLongerAllows() {
        assertThat(decide(new Case().grant(MOTHER, PUBLIC).reconsentSince(DECIDED.plusDays(1))))
                .isEqualTo(all(RECONSENT_REQUIRED));
        assertThat(decide(new Case().grant(MOTHER, PUBLIC).reconsentSince(DECIDED.minusDays(1))))
                .as("a grant given after the notice asked again counts").isEqualTo(all(CONSENT));
        assertThat(decide(new Case().status(ADULT).grant(SUBJECT, INSTITUTION).reconsentSince(DECIDED.plusDays(1))))
                .isEqualTo(widths(RECONSENT_REQUIRED, RECONSENT_REQUIRED, NOT_ANSWERED, NOT_ANSWERED));
    }

    @Test
    void refusalsAndNarrowerGrantsStillRestrictAfterANoticeAskingAgain() {
        OffsetDateTime asked = DECIDED.plusDays(1);
        GiverDecision freshGrant = new GiverDecision(MOTHER, true, PUBLIC, asked.plusDays(1), null, true);
        assertThat(decide(new Case().decide(freshGrant).refuse(FATHER, INTER_SCHOOL).reconsentSince(asked)))
                .isEqualTo(widths(CONSENT, CONSENT, REFUSED, REFUSED));
        assertThat(decide(new Case().decide(freshGrant).grant(FATHER, CLASS).reconsentSince(asked)))
                .isEqualTo(widths(CONSENT, NARROWER_GRANT, NARROWER_GRANT, NARROWER_GRANT));
    }

    @Test
    void aPurposeForbiddenForMinorsIsDeniedForAnyoneNotKnownToBeAnAdult() {
        for (String status : List.of(MINOR, UNKNOWN)) {
            assertThat(decide(new Case().status(status).forbiddenForMinors().grant(MOTHER, PUBLIC)
                    .grant(FATHER, PUBLIC))).as(status).isEqualTo(all(FORBIDDEN_FOR_MINOR));
        }
        assertThat(decide(new Case().status(ADULT).forbiddenForMinors().grant(SUBJECT, PUBLIC)))
                .isEqualTo(all(CONSENT));
        assertThat(decide(new Case().forbiddenForMinors().disabled()))
                .as("forbidden for minors is decided before the tenant's switch").isEqualTo(all(FORBIDDEN_FOR_MINOR));
    }

    @Test
    void aPurposeTheTenantSwitchedOffIsDenied() {
        assertThat(decide(new Case().disabled().grant(MOTHER, PUBLIC))).isEqualTo(all(PURPOSE_DISABLED));
        assertThat(decide(new Case().status(ADULT).disabled().grant(SUBJECT, PUBLIC))).isEqualTo(all(PURPOSE_DISABLED));
    }

    @Test
    void widthsAboveTheTenantsCeilingAreDenied() {
        assertThat(decide(new Case().ceiling(INSTITUTION).grant(MOTHER, PUBLIC)))
                .isEqualTo(widths(CONSENT, CONSENT, PURPOSE_DISABLED, PURPOSE_DISABLED));
        assertThat(decide(new Case().ceiling(INSTITUTION).refuse(MOTHER, CLASS)))
                .isEqualTo(widths(REFUSED, REFUSED, PURPOSE_DISABLED, PURPOSE_DISABLED));
    }

    // ---- a tenant-scoped purpose has the single scope tenant ----

    @Test
    void aTenantScopedPurposeIsDecidedAtTheTenantScopeOnly() {
        assertThat(decide(new Case().tenantScoped())).isEqualTo(Map.of(TENANT, NOT_ANSWERED));
        assertThat(decide(new Case().tenantScoped().grant(MOTHER, TENANT))).isEqualTo(Map.of(TENANT, CONSENT));
        assertThat(decide(new Case().tenantScoped().grant(MOTHER, TENANT).refuse(FATHER, TENANT)))
                .isEqualTo(Map.of(TENANT, REFUSED));
        assertThat(decide(new Case().tenantScoped().grant(MOTHER, PUBLIC)))
                .as("a width decision does not answer a tenant-scoped purpose").isEqualTo(Map.of(TENANT, NOT_ANSWERED));
        assertThat(decide(new Case().tenantScoped().status(ADULT).grant(SUBJECT, TENANT)))
                .isEqualTo(Map.of(TENANT, CONSENT));
        assertThat(decide(new Case().tenantScoped().disabled().grant(MOTHER, TENANT)))
                .isEqualTo(Map.of(TENANT, PURPOSE_DISABLED));
    }

    @Test
    void theAnswersComeNarrowestFirst() {
        assertThat(rule.decide(new Case().facts()).stream().map(ScopeState::scope))
                .containsExactly(CLASS, INSTITUTION, INTER_SCHOOL, PUBLIC);
    }
}
