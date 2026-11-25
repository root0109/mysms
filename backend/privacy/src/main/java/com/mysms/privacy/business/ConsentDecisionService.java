package com.mysms.privacy.business;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidArgumentException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.privacy.api.model.BatchCheckConsentRequest;
import com.mysms.privacy.api.model.BatchCheckConsentResponse;
import com.mysms.privacy.api.model.ConsentDecision;
import com.mysms.privacy.api.model.ConsentedSubject;
import com.mysms.privacy.api.model.PurposeStatus;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.model.ConsentFacts;
import com.mysms.privacy.model.ConsentReason;
import com.mysms.privacy.model.ConsentScope;
import com.mysms.privacy.model.ScopeState;
import com.mysms.privacy.repository.ConsentDecisionRepository;
import com.mysms.privacy.repository.ConsentDecisionRepository.Activity;
import com.mysms.privacy.repository.ConsentDecisionRepository.Answer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Consent decisions for the other modules (design §6.2, internal: service callers only). A caller names a processing
 * activity, never a purpose, so privacy resolves the purpose and legal basis from the register: an unknown activity is
 * denied (UNKNOWN_ACTIVITY, fail closed), one on a non-consent basis is allowed (NON_CONSENT_BASIS) and one on consent
 * answers from privacy.consent_state. A subject without a stored answer has decided nothing that counts, so the
 * {@link EffectiveConsentRule} decides on the purpose's own rules alone (forbidden for minors, switched off, above the
 * ceiling, otherwise not answered) without writing anything.
 */
@Service
public class ConsentDecisionService extends AppTransactionManager {

    static final String RESOURCE = "privacy.processing_activity";
    private static final String CONSENT = "consent";
    private static final String ALLOWED = "allowed";

    private final ConsentDecisionRepository repository;
    private final EffectiveConsentRule rule;
    private final Clock clock;

    public ConsentDecisionService(ConsentDecisionRepository repository, EffectiveConsentRule rule,
            @Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PrivacyDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions, Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.rule = rule;
        this.clock = clock;
    }

    /**
     * Every decision is about one tenant's subjects: a service call that names no tenant (X-Tenant-Id) is refused
     * with 400, before any transaction would fail to bind one.
     */
    private static void requireTenant() {
        if (CallContextHolder.current().flatMap(CallContext::tenantId).isEmpty()) {
            throw new InvalidArgumentException("A consent decision needs the tenant: send X-Tenant-Id.");
        }
    }

    /** privacy.CheckConsent: one subject's decision. */
    public ConsentDecision checkConsent(String activityCode, UUID subjectPersonId, String scope) {
        return decide(activityCode, List.of(subjectPersonId), scope).get(0);
    }

    /** privacy.BatchCheckConsent: one decision per subject, in the order asked. */
    public BatchCheckConsentResponse batchCheckConsent(BatchCheckConsentRequest request) {
        return new BatchCheckConsentResponse().decisions(decide(request.getActivityCode(),
                request.getSubjectPersonIds(), request.getScope().getValue()));
    }

    /** privacy.GetPurposeStatus: the activity's basis and the tenant's setting of its purpose (404 when unknown). */
    public PurposeStatus getPurposeStatus(String activityCode) {
        requireTenant();
        return doInReadTransaction(() -> {
            Activity activity = known(activityCode);
            return new PurposeStatus().activityCode(activity.code()).purposeCode(activity.purposeCode())
                    .legalBasis(activity.legalBasis()).enabled(activity.enabled()).maxScope(activity.maxScope())
                    .activeNoticeVersionId(Optional.ofNullable(activity.noticeId())
                            .flatMap(repository::activeNoticeVersion).orElse(null));
        });
    }

    /**
     * privacy.ListSubjectsWithConsent: the subjects whose answer allows the activity at the width, optionally of some
     * institutions, in person order. Refused (422) for an activity on a non-consent basis, which needs no list.
     */
    public Page<ConsentedSubject> listSubjectsWithConsent(String activityCode, String scope,
            List<UUID> institutionIds, Integer pageNumber, Integer pageSize, String cursor) {
        requireTenant();
        ConsentScope width = scope(scope);
        List<UUID> institutions = institutionIds == null ? List.of()
                : institutionIds.stream().distinct().sorted().toList(); // one cursor binding per set
        SortSpec sort = SortSpec.parse(List.of(), ConsentDecisionRepository.SORT_FIELDS,
                ConsentDecisionRepository.DEFAULT_ORDER, ConsentDecisionRepository.ID_COLUMN);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort,
                activityCode + "|" + scope + "|" + institutions, clock);
        return doInReadTransaction(() -> {
            Activity activity = known(activityCode);
            if (!CONSENT.equals(activity.legalBasis())) {
                throw new ValidationFailedException(List.of(new FieldError("activityCode",
                        "does not rest on consent: every subject may be processed", "ACTIVITY_NOT_CONSENT_BASED")));
            }
            requireScopeOf(activity, width);
            return repository.allowed(activity.purposeCode(), width.value(), institutions, page)
                    .map(id -> new ConsentedSubject().subjectPersonId(id));
        });
    }

    private List<ConsentDecision> decide(String activityCode, List<UUID> subjects, String scope) {
        requireTenant();
        ConsentScope width = scope(scope);
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        return doInReadTransaction(() -> {
            Optional<Activity> found = repository.activity(activityCode);
            if (found.isEmpty()) {
                return subjects.stream().map(subject -> decision(subject, false,
                        ConsentDecision.ReasonEnum.UNKNOWN_ACTIVITY, null, 0)).toList();
            }
            Activity activity = found.get();
            requireScopeOf(activity, width);
            if (!CONSENT.equals(activity.legalBasis())) {
                return subjects.stream().map(subject -> decision(subject, true,
                        ConsentDecision.ReasonEnum.NON_CONSENT_BASIS, activity.legalBasis(), 0)).toList();
            }
            return repository.answers(subjects, activity.purposeCode(), width.value()).stream()
                    .map(answer -> decision(activity, width, answer, today)).toList();
        });
    }

    /** The stored answer, or, without one, the rule's answer for a subject who has decided nothing that counts. */
    private ConsentDecision decision(Activity activity, ConsentScope width, Answer answer, LocalDate today) {
        if (answer.result() != null) {
            boolean allowed = ALLOWED.equals(answer.result());
            if (allowed && !withinSetting(activity, width)) {
                // defence in depth: the tenant's setting is read live, so a stored grant never outlives a switch-off
                return decision(answer.subjectPersonId(), false, ConsentDecision.ReasonEnum.PURPOSE_DISABLED,
                        activity.legalBasis(), answer.stateVersion());
            }
            return decision(answer.subjectPersonId(), allowed, reason(ConsentReason.valueOf(answer.reason())),
                    activity.legalBasis(), answer.stateVersion());
        }
        ConsentFacts silence = new ConsentFacts(answer.subjectPersonId(), answer.status(),
                activity.forbiddenForMinors(), activity.scopeKind(), activity.enabled(),
                Optional.ofNullable(activity.maxScope()).map(ConsentScope::of).orElse(null), List.of(), Set.of(),
                null, today);
        ScopeState state = rule.decide(silence).stream().filter(s -> s.scope() == width).findFirst()
                .orElseThrow(() -> new IllegalStateException("the rule answered no " + width.value()));
        return decision(answer.subjectPersonId(), state.allowed(), reason(state.reason()), activity.legalBasis(),
                answer.stateVersion());
    }

    /** Whether the tenant has the purpose switched on and allows the width (under its ceiling, if it set one). */
    private static boolean withinSetting(Activity activity, ConsentScope width) {
        return activity.enabled() && (activity.maxScope() == null || width.within(ConsentScope.of(activity.maxScope())));
    }

    /** The contract's name of the rule's reason (the same word, lowercase). */
    private static ConsentDecision.ReasonEnum reason(ConsentReason reason) {
        return ConsentDecision.ReasonEnum.fromValue(reason.name().toLowerCase(Locale.ROOT));
    }

    private static ConsentDecision decision(UUID subject, boolean allowed, ConsentDecision.ReasonEnum reason,
            String legalBasis, long stateVersion) {
        return new ConsentDecision().subjectPersonId(subject).allowed(allowed).reason(reason).legalBasis(legalBasis)
                .stateVersion(Long.toString(stateVersion));
    }

    private Activity known(String activityCode) {
        return repository.activity(activityCode).orElseThrow(() -> new ResourceNotFoundException(RESOURCE,
                activityCode));
    }

    /** A width the activity's purpose takes: one of class..public for a width purpose, tenant otherwise (422). */
    private static void requireScopeOf(Activity activity, ConsentScope width) {
        if (!ConsentScope.forKind(activity.scopeKind()).contains(width)) {
            throw new ValidationFailedException(List.of(new FieldError("scope",
                    "is not a width the activity's purpose takes", "SCOPE_NOT_APPLICABLE")));
        }
    }

    private static ConsentScope scope(String value) {
        try {
            return ConsentScope.of(value);
        } catch (IllegalArgumentException e) {
            throw new ValidationFailedException(List.of(new FieldError("scope",
                    "must be one of class, institution, inter_school, public, tenant", "INVALID_ENUM")));
        }
    }
}
