package com.mysms.core.db;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.error.ApiException;
import com.mysms.core.error.FieldError;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

/**
 * Applies the {@code request_id} rules of a sibling area's operation to a call made in process instead of over its
 * gRPC server (JAVA-CODING-RULES §2.2), through the same {@link IdempotencyLedger} and the callee's own store: a retry
 * gets the kept answer back, a key reused for another request is 422 {@code IDEMPOTENCY_KEY_REUSED}, a retry while the
 * first runs is 409 {@code REQUEST_IN_PROGRESS}, a refusal (4xx) is kept like an answer and a server failure frees the
 * key. Without a tenant (a public or platform-staff call), or for an area without a store, the work simply runs.
 */
public final class InProcessIdempotency {

    private static final String ANSWER = "answer";
    private static final String STATUS = "status";
    private static final String CODE = "code";
    private static final String DETAIL = "detail";
    private static final String ERRORS = "errors";

    private final IdempotencyLedger ledger;
    private final JsonMapper json;

    public InProcessIdempotency(IdempotencyLedger ledger, JsonMapper json) {
        this.ledger = ledger;
        this.json = json;
    }

    /** A refusal kept for a key, or the ledger's own refusal of a key: thrown as the callee's answer. */
    public static final class KeptRefusal extends ApiException {

        private static final long serialVersionUID = 1L;

        KeptRefusal(HttpStatus status, String code, String detail, List<FieldError> errors) {
            super(status, code, detail, errors);
        }
    }

    /**
     * Runs the callee's operation once per key.
     *
     * @param operation the callee's operation id ({@code <area>.<Name>}), which names the store and the key's scope
     * @param key       the {@code request_id} the caller would have sent
     * @param request   what identifies the request (the body the callee receives): a reused key must carry the same
     * @param answer    the type of the callee's answer, to read a kept one back
     */
    public <T> T call(String operation, UUID key, Object request, Class<T> answer, Supplier<T> work) {
        Optional<CallContext> context = CallContextHolder.current();
        Optional<IdempotencyStore> store = ledger.storeFor(operation);
        if (context.isEmpty() || context.get().tenantId().isEmpty() || store.isEmpty()) {
            return work.get();
        }
        UUID user = context.get().userId().orElse(null);
        IdempotencyLedger.Ticket ticket = ledger.claim(store.get(), operation, key, context.get().tenantId().get(),
                user, IdempotencyLedger.hash("CALL\n" + operation + "\n" + user + "\n", json.writeValueAsBytes(request)));
        return switch (ticket.outcome()) {
            case MISMATCH -> throw reused();
            case IN_PROGRESS -> throw inProgress();
            case REPLAY -> replay(ticket.claim().status(), ledger.kept(ticket), answer);
            case RUN -> run(ticket, work);
        };
    }

    private <T> T run(IdempotencyLedger.Ticket ticket, Supplier<T> work) {
        T result;
        try {
            result = work.get();
        } catch (ApiException e) {
            if (e.status().is4xxClientError()) {
                ledger.complete(ticket, e.status().value(), refusal(e));
            } else {
                ledger.release(ticket);
            }
            throw e;
        } catch (RuntimeException e) {
            ledger.release(ticket);
            throw e;
        }
        Map<String, Object> kept = new LinkedHashMap<>();
        kept.put(ANSWER, json.valueToTree(result));
        ledger.complete(ticket, HttpStatus.OK.value(), kept);
        return result;
    }

    private static Map<String, Object> refusal(ApiException e) {
        List<Map<String, String>> errors = new ArrayList<>();
        for (FieldError error : e.errors()) {
            Map<String, String> kept = new LinkedHashMap<>();
            kept.put("field", error.field());
            kept.put("message", error.message());
            kept.put(CODE, error.code());
            errors.add(kept);
        }
        Map<String, Object> kept = new LinkedHashMap<>();
        kept.put(STATUS, e.status().value());
        kept.put(CODE, e.code());
        kept.put(DETAIL, e.getMessage());
        kept.put(ERRORS, errors);
        return kept;
    }

    private static KeptRefusal reused() {
        return new KeptRefusal(HttpStatus.UNPROCESSABLE_CONTENT, "IDEMPOTENCY_KEY_REUSED",
                "This request_id was used for a different request.", List.of());
    }

    private static KeptRefusal inProgress() {
        return new KeptRefusal(HttpStatus.CONFLICT, "REQUEST_IN_PROGRESS",
                "A request with this request_id is still running; retry shortly.", List.of());
    }

    /**
     * The kept answer or refusal. A kept row in neither form (kept by another caller of the key, such as the gRPC
     * server before the area was called in process) cannot be read back: with no status recorded it counts as still
     * running (409), otherwise as a key used for another request (422).
     */
    @SuppressWarnings("unchecked")
    private <T> T replay(Integer storedStatus, Map<String, Object> kept, Class<T> answer) {
        if (kept.get(ANSWER) != null) {
            return json.convertValue(kept.get(ANSWER), answer);
        }
        if (!(kept.get(STATUS) instanceof Number status)) {
            throw storedStatus == null ? inProgress() : reused();
        }
        List<FieldError> errors = new ArrayList<>();
        for (Map<String, String> error : (List<Map<String, String>>) kept.getOrDefault(ERRORS, List.of())) {
            errors.add(new FieldError(error.get("field"), error.get("message"), error.get(CODE)));
        }
        throw new KeptRefusal(HttpStatus.valueOf(status.intValue()), (String) kept.get(CODE),
                (String) kept.get(DETAIL), errors);
    }
}
