package com.mysms.lms.business;

import com.mysms.authclient.AuthContext;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.id.UuidV7;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.lms.business.ConferencingEventProvider.Event;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.repository.ConferencingWebhookRepository;
import com.mysms.lms.repository.ConferencingWebhookRepository.Meeting;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * A conferencing provider's callbacks (ReceiveConferencingWebhook): the event names its tenant and meeting, the
 * signature is checked the MySMS way with the meeting's conferencing-account secret (401 otherwise), and the live
 * class takes the effect — ended, host or participant joined, participant left, recording ready. An unknown meeting
 * is answered like a bad signature (401), so a caller without the secret learns nothing; a participant without a
 * MySMS user reference is ignored.
 */
@Service
public class ConferencingWebhookService extends AppTransactionManager {

    static final String AGGREGATE = "lms.live_class";
    static final String SECRET_COLUMN = "lms.conferencing_account.client_secret_enc";
    /** Who a callback's effect is recorded as changed by: lms itself, acting for the provider. */
    static final UUID SERVICE_ACTOR = AuthContext.serviceUserId("lms");

    private final ConferencingWebhookRepository repository;
    private final Map<String, ConferencingEventProvider> providers;
    private final FieldCipher cipher;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final JsonMapper json;
    private final Clock clock;

    public ConferencingWebhookService(ConferencingWebhookRepository repository,
            List<ConferencingEventProvider> providers, FieldCipher cipher,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit, JsonMapper json, Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.providers = providers.stream()
                .collect(Collectors.toMap(ConferencingEventProvider::name, Function.identity()));
        this.cipher = cipher;
        this.outbox = outbox;
        this.audit = audit;
        this.json = json;
        this.clock = clock;
    }

    public void receiveConferencingWebhook(String providerName, String signature, String timestamp, byte[] body) {
        ConferencingEventProvider provider = providers.get(providerName);
        if (provider == null) {
            throw new ResourceNotFoundException("lms.conferencing_provider", providerName);
        }
        Map<String, Object> parsed;
        try {
            parsed = json.readValue(body, Map.class);
        } catch (JacksonException e) {
            throw WebhookSignature.refused();
        }
        Event event = provider.event(parsed).orElse(null);
        if (event == null) {
            return;
        }
        UUID tenant;
        try {
            tenant = UUID.fromString(event.tenant());
        } catch (IllegalArgumentException e) {
            throw WebhookSignature.refused();
        }
        String trace = CallContextHolder.current().map(CallContext::traceId).orElse("");
        CallContextHolder.callAs(CallContext.anonymous(trace).withIdentity(tenant, null), () -> {
            handle(providerName, event, tenant, signature, timestamp, body);
            return null;
        });
    }

    private void handle(String provider, Event event, UUID tenant, String signature, String timestamp, byte[] body) {
        Meeting meeting = doInReadTransaction(() -> repository.meeting(provider, event.meetingRef()))
                .orElseThrow(WebhookSignature::refused);
        if (meeting.accountId() == null) {
            throw WebhookSignature.refused();
        }
        String secret = doInReadTransaction(() -> repository.accountSecret(meeting.accountId()))
                .flatMap(enc -> cipher.decrypt(enc, tenant, SECRET_COLUMN))
                .orElseThrow(WebhookSignature::refused);
        WebhookSignature.verify(secret, timestamp, signature, body, clock.instant());
        OffsetDateTime now = now();
        OffsetDateTime at = Optional.ofNullable(event.occurredAt()).orElse(now);
        runInWriteTransaction(() -> {
            boolean changed = switch (event.kind()) {
                case MEETING_ENDED -> repository.ended(meeting.id(), at, now, SERVICE_ACTOR) > 0;
                case RECORDING_READY -> event.recordingUrl() != null
                        && repository.recording(meeting.id(), event.recordingUrl(), now, SERVICE_ACTOR) > 0;
                case PARTICIPANT_JOINED -> joined(meeting, event.participantRef(), at, now);
                case PARTICIPANT_LEFT -> user(event.participantRef())
                        .map(user -> repository.left(meeting.id(), user, at, now, SERVICE_ACTOR) > 0).orElse(false);
            };
            if (changed) {
                audit.recordTransition(AGGREGATE, meeting.id(), "receiveWebhook", List.of(event.kind().name()), null);
                outbox.publish(AGGREGATE + ".receiveWebhook", AGGREGATE, meeting.id(), LmsEvents.payload(meeting.id()));
            }
        });
    }

    private boolean joined(Meeting meeting, String participantRef, OffsetDateTime at, OffsetDateTime now) {
        UUID user = user(participantRef).orElse(null);
        if (user == null) {
            return false;
        }
        if (repository.joined(meeting.id(), user, at, now, SERVICE_ACTOR) == 0) {
            repository.insertJoined(UuidV7.next(clock), meeting.id(), user, at, now, SERVICE_ACTOR);
        }
        if (user.equals(meeting.hostUserId())) {
            repository.hostJoined(meeting.id(), now, SERVICE_ACTOR);
        }
        return true;
    }

    private static Optional<UUID> user(String participantRef) {
        try {
            return participantRef == null ? Optional.empty() : Optional.of(UUID.fromString(participantRef));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
