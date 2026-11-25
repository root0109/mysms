package com.mysms.privacyclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.error.UnavailableException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * The checker's cache-aside rules against a stand-in decision source: what is asked and when, per tenant, in batches,
 * learned non-consent bases, and every way it fails closed.
 */
class CachingConsentCheckerTest {

    private static final ActivityCode MARKETING = new ActivityCode("comms.marketing_broadcast");
    private static final ActivityCode NOTICES = new ActivityCode("comms.operational_notice");
    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final Duration TTL = Duration.ofMinutes(1);

    /** Answers from a script and records every batch it is asked; can be told to fail. */
    private static final class Source implements ConsentDecisionSource {

        private final List<List<UUID>> asked = new ArrayList<>();
        private boolean failing;
        private RuntimeException bug;
        private ConsentDecision answer = new ConsentDecision(true, ConsentReason.CONSENT, "consent", 3);
        private UUID skipped;

        @Override
        public Map<UUID, ConsentDecision> decide(ActivityCode activity, List<UUID> subjects, ConsentScope scope) {
            asked.add(List.copyOf(subjects));
            if (bug != null) {
                throw bug;
            }
            if (failing) {
                throw new UnavailableException("privacy is down");
            }
            Map<UUID, ConsentDecision> answers = new HashMap<>();
            subjects.stream().filter(s -> !s.equals(skipped)).forEach(s -> answers.put(s, answer));
            return answers;
        }
    }

    private static <T> T in(UUID tenant, Supplier<T> work) {
        return CallContextHolder.callAs(CallContext.anonymous("trace").withIdentity(tenant, null), work);
    }

    @Test
    void asksOnlyWhatTheTenantsCacheLacks() {
        Source source = new Source();
        CachingConsentChecker checker = new CachingConsentChecker(source, TTL);
        UUID asha = UUID.randomUUID();
        UUID kabir = UUID.randomUUID();
        assertThat(in(TENANT, () -> checker.check(MARKETING, asha, ConsentScope.TENANT)).allowed()).isTrue();
        Map<UUID, ConsentDecision> both = in(TENANT, () -> checker.checkAll(MARKETING, List.of(asha, kabir, kabir),
                ConsentScope.TENANT));
        assertThat(both).containsOnlyKeys(asha, kabir);
        assertThat(source.asked).containsExactly(List.of(asha), List.of(kabir));
        // another tenant, another scope: never another tenant's answer
        in(OTHER_TENANT, () -> checker.check(MARKETING, asha, ConsentScope.TENANT));
        in(TENANT, () -> checker.check(MARKETING, asha, ConsentScope.CLASS));
        assertThat(source.asked).hasSize(4);
    }

    @Test
    void asksAtMostOneThousandSubjectsAtOnce() {
        Source source = new Source();
        CachingConsentChecker checker = new CachingConsentChecker(source, TTL);
        List<UUID> school = IntStream.range(0, 2500).mapToObj(i -> UUID.randomUUID()).toList();
        Map<UUID, ConsentDecision> decided = in(TENANT, () -> checker.checkAll(MARKETING, school, ConsentScope.TENANT));
        assertThat(decided).hasSize(2500);
        assertThat(source.asked).extracting(List::size).containsExactly(1000, 1000, 500);
        assertThat(in(TENANT, () -> checker.allowed(MARKETING, school, ConsentScope.TENANT))).hasSize(2500);
    }

    @Test
    void failsClosedAndAsksAgainOncePrivacyAnswers() {
        Source source = new Source();
        CachingConsentChecker checker = new CachingConsentChecker(source, TTL);
        UUID asha = UUID.randomUUID();
        source.failing = true;
        assertThat(in(TENANT, () -> checker.check(MARKETING, asha, ConsentScope.TENANT)))
                .isEqualTo(ConsentDecision.unavailable());
        source.failing = false;
        assertThat(in(TENANT, () -> checker.check(MARKETING, asha, ConsentScope.TENANT)).allowed()).isTrue();
        assertThat(source.asked).hasSize(2);
    }

    @Test
    void aSubjectTheSourceSkippedIsNotAllowed() {
        Source source = new Source();
        CachingConsentChecker checker = new CachingConsentChecker(source, TTL);
        UUID asha = UUID.randomUUID();
        UUID kabir = UUID.randomUUID();
        source.skipped = kabir;
        Map<UUID, ConsentDecision> decided = in(TENANT, () -> checker.checkAll(MARKETING, List.of(asha, kabir),
                ConsentScope.TENANT));
        assertThat(decided.get(asha).allowed()).isTrue();
        assertThat(decided.get(kabir)).isEqualTo(ConsentDecision.unavailable());
    }

    @Test
    void cachesRefusalsToo() {
        Source source = new Source();
        source.answer = new ConsentDecision(false, ConsentReason.REFUSED, "consent", 4);
        CachingConsentChecker checker = new CachingConsentChecker(source, TTL);
        UUID asha = UUID.randomUUID();
        in(TENANT, () -> checker.check(MARKETING, asha, ConsentScope.TENANT));
        assertThat(in(TENANT, () -> checker.check(MARKETING, asha, ConsentScope.TENANT)).reason())
                .isEqualTo(ConsentReason.REFUSED);
        assertThat(source.asked).hasSize(1);
    }

    @Test
    void answersALegitimateUseActivityWithoutAskingOnceItsBasisIsKnown() {
        Source source = new Source();
        source.answer = new ConsentDecision(true, ConsentReason.NON_CONSENT_BASIS, "legitimate_use", 0);
        CachingConsentChecker checker = new CachingConsentChecker(source, TTL);
        in(TENANT, () -> checker.check(NOTICES, UUID.randomUUID(), ConsentScope.TENANT));
        source.failing = true;
        ConsentDecision decided = in(TENANT, () -> checker.check(NOTICES, UUID.randomUUID(), ConsentScope.TENANT));
        assertThat(decided.allowed()).isTrue();
        assertThat(decided.reason()).isEqualTo(ConsentReason.NON_CONSENT_BASIS);
        assertThat(source.asked).hasSize(1);
    }

    @Test
    void learnsANonConsentBasisOnlyFromAnAllowThatRestsOnIt() {
        Source source = new Source();
        CachingConsentChecker checker = new CachingConsentChecker(source, TTL);
        // a consent answer that names another basis, and a non-consent allow with a blank basis, teach nothing
        source.answer = new ConsentDecision(true, ConsentReason.CONSENT, "legitimate_use", 1);
        in(TENANT, () -> checker.check(MARKETING, UUID.randomUUID(), ConsentScope.TENANT));
        source.answer = new ConsentDecision(true, ConsentReason.NON_CONSENT_BASIS, " ", 0);
        in(TENANT, () -> checker.check(MARKETING, UUID.randomUUID(), ConsentScope.TENANT));
        source.failing = true;
        assertThat(in(TENANT, () -> checker.check(MARKETING, UUID.randomUUID(), ConsentScope.TENANT)))
                .isEqualTo(ConsentDecision.unavailable());
        assertThat(source.asked).hasSize(3);
    }

    @Test
    void oneActivitysBasisNeverAnswersForAnother() {
        Source source = new Source();
        CachingConsentChecker checker = new CachingConsentChecker(source, TTL);
        source.answer = new ConsentDecision(true, ConsentReason.NON_CONSENT_BASIS, "legitimate_use", 0);
        in(TENANT, () -> checker.check(NOTICES, UUID.randomUUID(), ConsentScope.TENANT));
        source.answer = new ConsentDecision(false, ConsentReason.NOT_ANSWERED, "consent", 1);
        assertThat(in(OTHER_TENANT, () -> checker.check(MARKETING, UUID.randomUUID(), ConsentScope.TENANT)).allowed())
                .isFalse();
        // notices, in any tenant, rest on legitimate use: answered without asking
        assertThat(in(OTHER_TENANT, () -> checker.check(NOTICES, UUID.randomUUID(), ConsentScope.TENANT)).reason())
                .isEqualTo(ConsentReason.NON_CONSENT_BASIS);
        assertThat(source.asked).hasSize(2);
    }

    @Test
    void aProgrammingErrorIsNotTakenForAnOutage() {
        Source source = new Source();
        source.bug = new IllegalStateException("a call inside a database transaction");
        CachingConsentChecker checker = new CachingConsentChecker(source, TTL);
        assertThatThrownBy(() -> in(TENANT, () -> checker.check(MARKETING, UUID.randomUUID(), ConsentScope.TENANT)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refusesACheckOutsideATenantAndANonPositiveTtl() {
        CachingConsentChecker checker = new CachingConsentChecker(new Source(), TTL);
        assertThatThrownBy(() -> checker.check(MARKETING, UUID.randomUUID(), ConsentScope.TENANT))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new CachingConsentChecker(new Source(), Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anActivityCodeNamesItsModule() {
        assertThat(MARKETING.module()).isEqualTo("comms");
        assertThat(MARKETING).hasToString("comms.marketing_broadcast");
        assertThatThrownBy(() -> new ActivityCode("Marketing")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ActivityCode(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
