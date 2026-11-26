package com.mysms.platform.org.events;

import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.platform.org.config.OrgEventsConfig;
import com.mysms.platform.org.repository.ReferenceCleanupRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * org keeps its logical references consistent when their owners are deleted (references are not checked against the
 * owner when written). For each owner's `.deleted` event it clears the referencing column or soft-deletes the
 * referencing row, audits each changed row and announces it on its own outbox, inside the event's tenant (exactly once
 * per event).
 */
@Component
public class OrgReferenceCleanup implements EventSubscriptions {

    /** One action: the statement, the entity it changes, and the cleared field (null = the row is archived). */
    record Step(String statement, String entity, String field) {
    }

    static final Map<String, List<Step>> STEPS = Map.ofEntries(
        Map.entry("platform.file_object.deleted", List.of(
            new Step(ReferenceCleanupRepository.CLEAR_INSTITUTION_LOGO_FILE_ID,
                    "org.institution", "logoFileId"),
            new Step(ReferenceCleanupRepository.CLEAR_INSTITUTION_SIGNATURE_FILE_ID,
                    "org.institution", "signatureFileId"))),
        Map.entry("platform.theme.deleted", List.of(
            new Step(ReferenceCleanupRepository.CLEAR_INSTITUTION_THEME_ID,
                    "org.institution", "themeId"))));

    private final ReferenceCleanupRepository repository;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;

    public OrgReferenceCleanup(ReferenceCleanupRepository repository,
            @Qualifier(OrgEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(OrgEventsConfig.AUDIT) AuditWriter audit, Clock clock) {
        this.repository = repository;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    public String module() {
        return "org";
    }

    @Override
    public Map<String, EventHandler> handlers() {
        return STEPS.keySet().stream().collect(Collectors.toMap(type -> type, type -> (EventHandler) this::handle));
    }

    /** Applies every step of the deleted owner; returns how many rows changed. */
    public int handle(EventEnvelope event) {
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        int changed = 0;
        for (Step step : STEPS.getOrDefault(event.type(), List.of())) {
            for (UUID id : repository.apply(step.statement(), event.aggregateId(), now)) {
                if (step.field() == null) {
                    audit.recordDelete(step.entity(), id);
                    outbox.publish(step.entity() + OrgEvents.DELETED, step.entity(), id, OrgEvents.payload(id));
                } else {
                    audit.recordUpdate(step.entity(), id, List.of(step.field()));
                    outbox.publish(step.entity() + OrgEvents.UPDATED, step.entity(), id, OrgEvents.payload(id));
                }
                changed++;
            }
        }
        return changed;
    }
}
