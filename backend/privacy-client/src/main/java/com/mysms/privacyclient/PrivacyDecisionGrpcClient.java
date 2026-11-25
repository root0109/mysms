package com.mysms.privacyclient;

import com.mysms.authclient.ServiceTokenProvider;
import com.mysms.core.error.UnavailableException;
import com.mysms.privacy.v1.BatchCheckConsentRequest;
import com.mysms.privacy.v1.BatchCheckConsentResponse;
import com.mysms.privacy.v1.ConsentDecisionServiceGrpc;
import io.grpc.Channel;
import io.grpc.StatusRuntimeException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * privacy's {@code ConsentDecisionService.BatchCheckConsent} over gRPC ({@code grpc.client.privacy-service}, a
 * verbatim copy of privacy.proto), called as the asking module's service principal — the activity names the module —
 * in the tenant of the current call, within a short deadline. A failed call, and an answer that does not match the
 * question (another count or subject, an unknown reason, an allow its reason does not support), is no answer:
 * {@link UnavailableException}, and the checker fails closed.
 */
public final class PrivacyDecisionGrpcClient implements ConsentDecisionSource {

    private static final String REASON_PREFIX = "REASON_";
    /** The only reasons that allow; every other reason refuses. */
    private static final Set<ConsentReason> ALLOWING = Set.of(ConsentReason.CONSENT, ConsentReason.NON_CONSENT_BASIS);

    private final ConsentDecisionServiceGrpc.ConsentDecisionServiceBlockingStub stub;
    private final ServiceTokenProvider serviceTokens;
    private final Duration deadline;

    public PrivacyDecisionGrpcClient(Channel channel, ServiceTokenProvider serviceTokens, Duration deadline) {
        if (deadline.isNegative() || deadline.isZero()) {
            throw new IllegalArgumentException("mysms.privacy-client.deadline must be positive");
        }
        this.stub = ConsentDecisionServiceGrpc.newBlockingStub(channel);
        this.serviceTokens = serviceTokens;
        this.deadline = deadline;
    }

    @Override
    public Map<UUID, ConsentDecision> decide(ActivityCode activity, List<UUID> subjectPersonIds, ConsentScope scope) {
        if (subjectPersonIds.isEmpty() || subjectPersonIds.size() > MAX_BATCH) {
            throw new IllegalArgumentException("a batch is 1 to " + MAX_BATCH + " subjects");
        }
        BatchCheckConsentRequest.Builder request = BatchCheckConsentRequest.newBuilder()
                .setActivityCode(activity.value()).setScope(BatchCheckConsentRequest.Scope.valueOf("SCOPE_" + scope));
        subjectPersonIds.forEach(subject -> request.addSubjectPersonIds(subject.toString()));
        BatchCheckConsentResponse answer;
        try {
            answer = stub.withCallCredentials(serviceTokens.callCredentials(activity.module()))
                    .withDeadlineAfter(deadline.toMillis(), TimeUnit.MILLISECONDS)
                    .batchCheckConsent(request.build());
        } catch (StatusRuntimeException e) {
            throw new UnavailableException("privacy did not decide (" + e.getStatus().getCode() + ").");
        }
        if (answer.getDecisionsCount() != subjectPersonIds.size()) {
            throw new UnavailableException("privacy answered " + answer.getDecisionsCount() + " decisions for "
                    + subjectPersonIds.size() + " subjects.");
        }
        Map<UUID, ConsentDecision> decisions = new HashMap<>();
        for (int i = 0; i < subjectPersonIds.size(); i++) {
            UUID subject = subjectPersonIds.get(i);
            com.mysms.privacy.v1.ConsentDecision decision = answer.getDecisions(i);
            if (!subject.toString().equals(decision.getSubjectPersonId())) {
                throw new UnavailableException("privacy answered the decisions out of order.");
            }
            ConsentReason reason = reason(decision.getReason());
            if (decision.getAllowed() != ALLOWING.contains(reason)) {
                throw new UnavailableException("privacy answered " + (decision.getAllowed() ? "an allow" : "a refusal")
                        + " for the reason " + reason + ".");
            }
            decisions.put(subject, new ConsentDecision(decision.getAllowed(), reason,
                    decision.hasLegalBasis() ? decision.getLegalBasis() : null, decision.getStateVersion()));
        }
        return decisions;
    }

    /**
     * The reason's name without its proto prefix; UNSPECIFIED, UNRECOGNIZED or a reason this client does not know is
     * no answer.
     */
    private static ConsentReason reason(com.mysms.privacy.v1.ConsentDecision.Reason reason) {
        String name = reason.name();
        if (reason == com.mysms.privacy.v1.ConsentDecision.Reason.REASON_UNSPECIFIED || !name.startsWith(REASON_PREFIX)) {
            throw new UnavailableException("privacy answered no reason.");
        }
        try {
            return ConsentReason.valueOf(name.substring(REASON_PREFIX.length()));
        } catch (IllegalArgumentException e) {
            throw new UnavailableException("privacy answered a reason this client does not know.");
        }
    }
}
