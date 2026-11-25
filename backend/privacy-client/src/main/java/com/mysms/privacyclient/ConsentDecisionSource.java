package com.mysms.privacyclient;

import com.mysms.core.error.UnavailableException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Where decisions come from on a cache miss (Strategy): privacy's ConsentDecisionService over gRPC in every process
 * ({@link PrivacyDecisionGrpcClient}). When privacy gives no usable answer it throws {@link UnavailableException},
 * which the checker turns into a fail-closed answer; any other exception is a programming error (a check made inside
 * a database transaction, say) and reaches the caller.
 */
public interface ConsentDecisionSource {

    /** Most subjects asked at once (privacy's BatchCheckConsent limit). */
    int MAX_BATCH = 1000;

    /**
     * One decision per subject of at most {@link #MAX_BATCH}, in the current call's tenant.
     *
     * @throws UnavailableException when privacy cannot answer, or answers something that does not fit the question
     */
    Map<UUID, ConsentDecision> decide(ActivityCode activity, List<UUID> subjectPersonIds, ConsentScope scope);
}
