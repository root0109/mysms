package com.mysms.comms.business;

import com.mysms.comms.api.model.CancelOutboundMessageRequest;
import com.mysms.comms.api.model.Recipients;
import com.mysms.comms.api.model.RetryOutboundMessageRequest;
import com.mysms.comms.api.model.SendMessageRequest;
import com.mysms.comms.api.model.SendResult;
import com.mysms.comms.domain.MessageTemplateEntity;
import com.mysms.comms.events.CommsEvents;
import com.mysms.comms.repository.AudienceGroupMemberRepository;
import com.mysms.comms.repository.MessageTemplateRepository;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.web.Etags;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import com.mysms.authclient.DataScope;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.config.CommsEventsConfig;
import com.mysms.comms.domain.OutboundMessageEntity;
import com.mysms.comms.mapper.OutboundMessageMapper;
import com.mysms.comms.repository.OutboundMessageRepository;
import com.mysms.comms.api.model.OutboundMessage;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of comms.outbound_message: audit and domain event commit in the same transaction as every change.
 * Rows outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class OutboundMessageService extends AppTransactionManager {

    public static final String AGGREGATE = OutboundMessageEntity.TABLE;
    static final String PERMISSION_LIST = "comms.outbound_message.read";
    static final String PERMISSION_GET = "comms.outbound_message.read";
    static final String PERMISSION_SEND_MESSAGE = "comms.outbound_message.send_message";
    static final String PERMISSION_CANCEL = "comms.outbound_message.cancel_outbound_message";
    static final String PERMISSION_RETRY = "comms.outbound_message.retry_outbound_message";
    /** The most recipients one send reaches. */
    static final int MAX_RECIPIENTS = 5000;


    private final OutboundMessageRepository repository;
    private final OutboundMessageMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final MessageTemplateRepository templates;
    private final AudienceGroupMemberRepository groups;
    private final DeliveryChannels channels;
    private final RecipientDirectory directory;
    private final MessageDeliveryExecutor delivery;

    public OutboundMessageService(OutboundMessageRepository repository, OutboundMessageMapper mapper,
            @Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(CommsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(CommsEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(CommsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock, MessageTemplateRepository templates,
            AudienceGroupMemberRepository groups, DeliveryChannels channels, RecipientDirectory directory,
            MessageDeliveryExecutor delivery) {

        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.templates = templates;
        this.groups = groups;
        this.channels = channels;
        this.directory = directory;
        this.delivery = delivery;
    }

    /** The text one send delivers to everybody, and who it goes to. */
    private record Prepared(String subject, String body, UUID templateId, List<UUID> users) {
    }

    /**
     * comms.SendMessage: queues one message per recipient with an address on the channel and delivers them once the
     * queue is committed. Recipients are named users and the members of audience groups the caller reaches; the text
     * is the body, or a template of the channel with its {@code {{name}}} placeholders filled from the payload. The
     * institution is the one the caller acts in. A channel without a provider is 503 before anything is written;
     * recipients by class, cohort, guardians or staff only, and sending later, are not offered by this release (422).
     */
    public SendResult sendMessage(SendMessageRequest request) {
        validator.validate(request);
        String channel = request.getChannel().getValue();
        channels.require(channel);
        assertSendable(request);
        UUID institution = ActingInstitution.require(PERMISSION_SEND_MESSAGE);
        DataScope scope = DataScope.current(PERMISSION_SEND_MESSAGE);
        UUID caller = OwnRows.caller();
        Prepared prepared = doInReadTransaction(() -> prepare(request, channel, scope));
        Map<UUID, String> addresses = directory.addresses(prepared.users(), channel);
        List<UUID> queued = doInWriteTransaction(() -> {
            List<UUID> ids = new ArrayList<>();
            addresses.forEach((user, address) -> {
                OutboundMessageEntity message = OutboundMessageEntity.builder().institutionId(institution)
                        .messageKind(request.getMessageKind().getValue()).channel(channel).sender(caller.toString())
                        .recipient(address).recipientUserId(user).subject(prepared.subject()).body(prepared.body())
                        .templateId(prepared.templateId()).tenantId(CallContextHolder.require().tenantId().orElseThrow())
                        .build();
                message.onCreatingData(Actor.current(), clock);
                OutboundMessageEntity stored = repository.insert(message);
                audit.recordCreate(AGGREGATE, stored.getId());
                outbox.publish(AGGREGATE + ".queued", AGGREGATE, stored.getId(), CommsEvents.payload(stored.getId()));
                ids.add(stored.getId());
            });
            return ids;
        });
        delivery.dispatch(queued);
        return new SendResult(queued.size(), prepared.users().size() - queued.size());
    }

    /** comms.CancelOutboundMessage: a queued message is withdrawn before it is sent; any other state is 409. */
    public OutboundMessage cancelOutboundMessage(UUID id, CancelOutboundMessageRequest request) {
        DataScope scope = DataScope.current(PERMISSION_CANCEL);
        return doInWriteTransaction(() -> {
            OutboundMessageEntity entity = moved(scope, id, request == null ? null : request.getEtag(), "queued",
                    "cancelled");
            audit.recordTransition(AGGREGATE, id, "cancel", List.of("status"), null);
            outbox.publish(AGGREGATE + ".cancel", AGGREGATE, id, CommsEvents.payload(id));
            return mapper.toDto(repository.findById(entity.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /**
     * comms.RetryOutboundMessage: a failed message is queued again and delivered at once; any other state is 409, and
     * a channel without a provider is 503 before anything changes. Answers the message after the attempt.
     */
    public OutboundMessage retryOutboundMessage(UUID id, RetryOutboundMessageRequest request) {
        DataScope scope = DataScope.current(PERMISSION_RETRY);
        runInWriteTransaction(() -> {
            OutboundMessageEntity entity = loadVisible(scope, id);
            channels.require(entity.getChannel());
            moved(scope, id, request == null ? null : request.getEtag(), "failed", "queued");
            audit.recordTransition(AGGREGATE, id, "retry", List.of("status", "providerResponse"), null);
            outbox.publish(AGGREGATE + ".retry", AGGREGATE, id, CommsEvents.payload(id));
        });
        delivery.dispatch(List.of(id));
        return doInReadTransaction(() -> mapper.toDto(loadVisible(scope, id)));
    }

    /** One status step under the scope, the optional body etag and the step's guard. */
    private OutboundMessageEntity moved(DataScope scope, UUID id, String etag, String from, String to) {
        OutboundMessageEntity entity = loadVisible(scope, id);
        scope.requireWritable(institutionOf(entity));
        OptionalInt expected = Etags.bodyVersion(etag);
        if (expected.isPresent() && expected.getAsInt() != entity.getRowVersion()) {
            throw new PreconditionFailedException("The etag does not match the current version.");
        }
        if (!from.equals(entity.getStatus())) {
            throw new InvalidStateException("The message cannot be changed this way in its current state.");
        }
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.MICROS);
        if (repository.move(id, entity.getRowVersion(), from, to, null, null, now, Actor.current().orElse(null)) == 0) {
            throw new PreconditionFailedException("The row changed since it was read.");
        }
        return entity;
    }

    /** What this release cannot send yet, refused before anything is read or written. */
    private static void assertSendable(SendMessageRequest request) {
        List<FieldError> errors = new ArrayList<>();
        Recipients to = request.getRecipients();
        if (!to.getClassSectionIds().isEmpty()) {
            errors.add(new FieldError("recipients.classSectionIds", "sending to classes is not available yet",
                    "NOT_SUPPORTED"));
        }
        if (!to.getCohortIds().isEmpty()) {
            errors.add(new FieldError("recipients.cohortIds", "sending to cohorts is not available yet",
                    "NOT_SUPPORTED"));
        }
        if (Boolean.TRUE.equals(to.getIncludeGuardians())) {
            errors.add(new FieldError("recipients.includeGuardians", "sending to guardians is not available yet",
                    "NOT_SUPPORTED"));
        }
        if (Boolean.TRUE.equals(to.getStaffOnly())) {
            errors.add(new FieldError("recipients.staffOnly", "narrowing to staff is not available yet",
                    "NOT_SUPPORTED"));
        }
        if (to.getUserIds().isEmpty() && to.getAudienceGroupIds().isEmpty()) {
            errors.add(new FieldError("recipients", "must name users or audience groups", "Required"));
        }
        if (request.getScheduledAt() != null) {
            errors.add(new FieldError("scheduledAt", "sending later is not available yet; send now",
                    "NOT_SUPPORTED"));
        }
        String body = request.getBody();
        if ((request.getTemplateId() == null) == (body == null || body.isBlank())) {
            errors.add(new FieldError("body", "give either a body or a templateId", "Required"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    /** The text from the body or the template, and every recipient the caller reaches. */
    private Prepared prepare(SendMessageRequest request, String channel, DataScope scope) {
        List<FieldError> errors = new ArrayList<>();
        String subject = request.getSubject();
        String body = request.getBody();
        UUID templateId = request.getTemplateId();
        if (templateId != null) {
            MessageTemplateEntity template = templates.findById(templateId)
                    .filter(found -> scope.canRead(Optional.ofNullable(found.getInstitutionId())))
                    .filter(found -> Boolean.TRUE.equals(found.getIsActive()) && channel.equals(found.getChannel()))
                    .orElse(null);
            if (template == null || template.getBody() == null) {
                errors.add(new FieldError("templateId", "names no active template of this channel", "NOT_FOUND"));
            } else {
                Map<?, ?> values = request.getPayload() instanceof Map<?, ?> map ? map : Map.of();
                subject = TemplateText.render(template.getSubject(), values);
                body = TemplateText.render(template.getBody(), values);
            }
        }
        List<UUID> groupIds = request.getRecipients().getAudienceGroupIds().stream().distinct().toList();
        if (!groupIds.isEmpty() && groups.reachableGroups(groupIds, scope).size() != groupIds.size()) {
            errors.add(new FieldError("recipients.audienceGroupIds", "names a group that does not exist",
                    "NOT_FOUND"));
        }
        Set<UUID> users = new LinkedHashSet<>(request.getRecipients().getUserIds());
        if (!groupIds.isEmpty()) {
            users.addAll(groups.memberUsers(groupIds));
        }
        if (users.size() > MAX_RECIPIENTS) {
            errors.add(new FieldError("recipients", "may reach at most " + MAX_RECIPIENTS + " people", "Size"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        return new Prepared(subject, body, templateId, List.copyOf(users));
    }

    public OutboundMessage getOutboundMessage(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<OutboundMessage> listOutboundMessages(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, OutboundMessageRepository.SORT_FIELDS, OutboundMessageRepository.DEFAULT_ORDER,
                OutboundMessageRepository.ID_COLUMN);
        // a partitioned table: the list reads only the partitions of the range the client names
        FilterParser.requireRange(filter, OutboundMessageRepository.FILTER_FIELDS, "createTime");
        SqlFragment where = FilterParser.parse(filter, OutboundMessageRepository.FILTER_FIELDS)
                .and(OutboundMessageRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<OutboundMessageEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private OutboundMessageEntity loadVisible(DataScope scope, UUID id) {
        OutboundMessageEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(OutboundMessageEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(OutboundMessageEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getMessageKind() == null) {
            errors.add(new FieldError("messageKind", "must not be null", "NotNull"));
        }
        if (entity.getChannel() == null) {
            errors.add(new FieldError("channel", "must not be null", "NotNull"));
        }
        if (entity.getSender() == null) {
            errors.add(new FieldError("sender", "must not be null", "NotNull"));
        }
        if (entity.getRecipient() == null) {
            errors.add(new FieldError("recipient", "must not be null", "NotNull"));
        }
        if (entity.getBody() == null) {
            errors.add(new FieldError("body", "must not be null", "NotNull"));
        }
        if (entity.getTemplateId() != null && !repository.existsTemplateId(entity.getTemplateId())) {
            errors.add(new FieldError("templateId", "message template does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

}
