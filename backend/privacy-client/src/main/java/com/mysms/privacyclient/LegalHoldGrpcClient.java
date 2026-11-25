package com.mysms.privacyclient;

import com.mysms.authclient.ServiceTokenProvider;
import com.mysms.core.error.UnavailableException;
import com.mysms.privacy.v1.ActiveLegalHold;
import com.mysms.privacy.v1.LegalHoldServiceGrpc;
import com.mysms.privacy.v1.ListActiveHoldsRequest;
import com.mysms.privacy.v1.ListActiveHoldsResponse;
import io.grpc.Channel;
import io.grpc.StatusRuntimeException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * privacy's {@code LegalHoldService.ListActiveHolds} over gRPC ({@code grpc.client.privacy-service}, the verbatim copy
 * of privacy.proto), called as the asking module's service principal (permission {@code privacy.legal_hold.check}) in
 * the tenant of the current call, within {@code mysms.privacy-client.holds-deadline}. A failed call, and a hold this
 * client cannot read (no scope, a malformed id), is no answer: {@link UnavailableException}, and the caller removes
 * nothing.
 */
public final class LegalHoldGrpcClient implements LegalHolds {

    private static final String SCOPE_PREFIX = "SCOPE_";

    private final LegalHoldServiceGrpc.LegalHoldServiceBlockingStub stub;
    private final ServiceTokenProvider serviceTokens;
    private final Duration deadline;

    public LegalHoldGrpcClient(Channel channel, ServiceTokenProvider serviceTokens, Duration deadline) {
        if (deadline.isNegative() || deadline.isZero()) {
            throw new IllegalArgumentException("mysms.privacy-client.holds-deadline must be positive");
        }
        this.stub = LegalHoldServiceGrpc.newBlockingStub(channel);
        this.serviceTokens = serviceTokens;
        this.deadline = deadline;
    }

    @Override
    public ActiveHolds activeHolds(String module, UUID subjectPersonId) {
        ListActiveHoldsResponse answer;
        try {
            answer = stub.withCallCredentials(serviceTokens.callCredentials(module))
                    .withDeadlineAfter(deadline.toMillis(), TimeUnit.MILLISECONDS)
                    .listActiveHolds(ListActiveHoldsRequest.newBuilder()
                            .setSubjectPersonId(subjectPersonId.toString()).setModule(module).build());
        } catch (StatusRuntimeException e) {
            throw new UnavailableException("privacy did not list the legal holds (" + e.getStatus().getCode() + ").");
        }
        List<ActiveHold> holds = new ArrayList<>();
        for (ActiveLegalHold hold : answer.getHoldsList()) {
            holds.add(hold(hold));
        }
        return new ActiveHolds(holds);
    }

    private static ActiveHold hold(ActiveLegalHold hold) {
        ActiveLegalHold.Scope scope = hold.getScope();
        if (scope == ActiveLegalHold.Scope.SCOPE_UNSPECIFIED || scope == ActiveLegalHold.Scope.UNRECOGNIZED) {
            throw new UnavailableException("privacy answered a legal hold without a scope.");
        }
        try {
            List<UUID> records = hold.getEntityIdsList().stream().map(UUID::fromString).toList();
            return new ActiveHold(UUID.fromString(hold.getId()),
                    scope.name().substring(SCOPE_PREFIX.length()).toLowerCase(Locale.ROOT),
                    hold.hasSubjectPersonId() ? UUID.fromString(hold.getSubjectPersonId()) : null,
                    hold.hasCategoryCode() ? hold.getCategoryCode() : null, hold.hasModule() ? hold.getModule() : null,
                    hold.hasEntityType() ? hold.getEntityType() : null, Set.copyOf(records));
        } catch (IllegalArgumentException e) {
            throw new UnavailableException("privacy answered a legal hold with a malformed id.");
        }
    }
}
