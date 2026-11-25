package com.mysms.comms.business;

import java.util.regex.Pattern;
import java.time.temporal.ChronoUnit;
import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import com.mysms.core.error.ApiException;
import com.mysms.core.context.CallContextHolder;
import com.mysms.comms.api.model.ComposeMailRequest;
import com.mysms.authclient.DataScope;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.config.CommsEventsConfig;
import com.mysms.comms.domain.MailboxMessageEntity;
import com.mysms.comms.events.CommsEvents;
import com.mysms.comms.mapper.MailboxMessageMapper;
import com.mysms.comms.repository.MailboxMessageRepository;
import com.mysms.comms.repository.MemberDirectory;
import com.mysms.comms.api.model.MailboxMessage;
import com.mysms.core.context.Actor;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.RateLimitedException;
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
import com.mysms.core.web.Etags;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of comms.mailbox_message: audit and domain event commit in the same transaction as every change.
 * A mailbox is its owner's alone: every read and change reaches only the caller's own messages, whatever the
 * caller's grants, and only in the caller's institutions.
 */
@Service
public class MailboxMessageService extends AppTransactionManager {

    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    public static final String AGGREGATE = MailboxMessageEntity.TABLE;
    static final String PERMISSION_LIST = "comms.mailbox_message.read";
    static final String PERMISSION_GET = "comms.mailbox_message.read";
    static final String PERMISSION_DELETE = "comms.mailbox_message.delete";
    static final String PERMISSION_COMPOSE_MAIL = "comms.mailbox_message.compose_mail";
    static final String PERMISSION_MARK_MAIL_READ = "comms.mailbox_message.mark_mail_read";
    /** The most recipients (accounts and addresses together) one mail goes to. */
    static final int MAX_RECIPIENTS = 200;
    /** The most addresses outside the school one mail goes to: the school's mail server is no bulk relay. */
    static final int MAX_EXTERNAL_PER_MAIL = 10;
    /** The most mails with outside addresses one sender sends a day (UTC); bulk mail is SendMessage's. */
    static final int MAX_EXTERNAL_MAILS_PER_DAY = 50;


    private final MailboxMessageRepository repository;
    private final MailboxMessageMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final DeliveryChannels channels;
    private final MemberDirectory members;

    public MailboxMessageService(MailboxMessageRepository repository, MailboxMessageMapper mapper,
            @Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(CommsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(CommsEventsConfig.OUTBOX) OutboxWriter outbox,
                    @Qualifier(CommsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock, DeliveryChannels channels, MemberDirectory members) {

        super(database, transactions);
        this.channels = channels;
        this.members = members;
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public MailboxMessage getMailboxMessage(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<MailboxMessage> listMailboxMessages(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, MailboxMessageRepository.SORT_FIELDS, MailboxMessageRepository.DEFAULT_ORDER,
                MailboxMessageRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, MailboxMessageRepository.FILTER_FIELDS)
                .and(MailboxMessageRepository.scope(DataScope.current(PERMISSION_LIST)))
                .and(MailboxMessageRepository.ownedBy(OwnRows.caller()));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + OwnRows.caller(), clock);
        return doInReadTransaction(() -> {
            Page<MailboxMessageEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteMailboxMessage(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            MailboxMessageEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + CommsEvents.DELETED, AGGREGATE, id, CommsEvents.payload(id));
        });
    }

    /**
     * comms.ComposeMail: the caller's mail lands as a copy in each recipient account's inbox and as one copy in the
     * caller's sent folder, in the institution the caller acts in. Recipient accounts must be the school's (its
     * students, staff and students' guardians), as must the student the mail is about (422). External addresses — at
     * most {@value #MAX_EXTERNAL_PER_MAIL} a mail and {@value #MAX_EXTERNAL_MAILS_PER_DAY} such mails a sender a day
     * (422, 429) — are sent by e-mail once the copies are committed, and the sent copy records how that went (503
     * before anything is written when e-mail is not configured). Attachments are not offered by this release (422).
     */
    public MailboxMessage composeMail(ComposeMailRequest request) {
        validator.validate(request);
        List<UUID> recipients = request.getToUserIds() == null ? List.of()
                : request.getToUserIds().stream().distinct().toList();
        List<String> external = request.getToAddresses() == null ? List.of()
                : request.getToAddresses().stream().map(String::trim).filter(address -> !address.isEmpty()).distinct()
                .toList();
        assertComposable(request, recipients, external);
        DeliveryChannel email = external.isEmpty() ? null : channels.require(EmailChannel.NAME);
        UUID institution = ActingInstitution.require(PERMISSION_COMPOSE_MAIL);
        UUID caller = OwnRows.caller();
        UUID tenant = CallContextHolder.require().tenantId().orElseThrow();
        String to = String.join(", ", recipients.stream().map(UUID::toString).toList())
                + (recipients.isEmpty() || external.isEmpty() ? "" : ", ") + String.join(", ", external);
        MailboxMessageEntity sent = doInWriteTransaction(() -> {
            assertReachable(request, recipients, institution);
            if (!external.isEmpty() && repository.externalSentSince(caller, now().truncatedTo(ChronoUnit.DAYS))
                    >= MAX_EXTERNAL_MAILS_PER_DAY) {
                throw new RateLimitedException("At most " + MAX_EXTERNAL_MAILS_PER_DAY + " mails to outside addresses "
                        + "a day; try again tomorrow.");
            }
            for (UUID recipient : recipients) {
                stored(copy(tenant, institution, recipient, "inbox", to, request).build());
            }
            return stored(copy(tenant, institution, caller, "sent", to, request)
                    .deliveryStatus(external.isEmpty() ? null : "queued").build());
        });
        if (email != null) {
            boolean delivered = true;
            for (String address : external) {
                try {
                    email.deliver(address, request.getSubject(), request.getBody());
                } catch (ApiException e) {
                    delivered = false;
                }
            }
            String status = delivered ? "delivered" : "failed";
            runInWriteTransaction(() -> {
                if (repository.recordDelivery(sent.getId(), status, now(), Actor.current().orElse(null)) == 1) {
                    audit.recordTransition(AGGREGATE, sent.getId(), status, List.of("deliveryStatus"), null);
                }
            });
        }
        return doInReadTransaction(() -> mapper.toDto(repository.findById(sent.getId())
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, sent.getId()))));
    }

    /** comms.MarkMailRead: the caller marks a message of their own mailbox read; one already read stays as it is. */
    public MailboxMessage markMailRead(UUID id) {
        DataScope scope = DataScope.current(PERMISSION_MARK_MAIL_READ);
        return doInWriteTransaction(() -> {
            MailboxMessageEntity entity = loadVisible(scope, id);
            if (entity.getReadAt() == null) {
                if (repository.markRead(id, entity.getRowVersion(), now(), Actor.current().orElse(null)) == 0) {
                    throw new PreconditionFailedException("The message changed since it was read.");
                }
                audit.recordTransition(AGGREGATE, id, "mark_read", List.of("readAt"), null);
                outbox.publish(AGGREGATE + ".mark_read", AGGREGATE, id, CommsEvents.payload(id));
            }
            return mapper.toDto(repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    private static void assertComposable(ComposeMailRequest request, List<UUID> recipients, List<String> external) {
        List<FieldError> errors = new ArrayList<>();
        if (recipients.isEmpty() && external.isEmpty()) {
            errors.add(new FieldError("toUserIds", "name at least one recipient", "Required"));
        }
        if (recipients.size() + external.size() > MAX_RECIPIENTS) {
            errors.add(new FieldError("toUserIds", "may reach at most " + MAX_RECIPIENTS + " recipients", "Size"));
        }
        if (external.size() > MAX_EXTERNAL_PER_MAIL) {
            errors.add(new FieldError("toAddresses", "may reach at most " + MAX_EXTERNAL_PER_MAIL
                    + " outside addresses", "Size"));
        }
        if (external.stream().anyMatch(address -> !EMAIL.matcher(address).matches())) {
            errors.add(new FieldError("toAddresses", "must be e-mail addresses", "Email"));
        }
        if (request.getAttachmentFileIds() != null && !request.getAttachmentFileIds().isEmpty()) {
            errors.add(new FieldError("attachmentFileIds", "attachments are not available yet", "NOT_SUPPORTED"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    /** Every recipient account and the student the mail is about must be the school's (422 naming each that is not). */
    private void assertReachable(ComposeMailRequest request, List<UUID> recipients, UUID institution) {
        List<FieldError> errors = new ArrayList<>();
        Set<UUID> known = members.accountsIn(recipients, institution);
        List<UUID> asked = request.getToUserIds() == null ? List.of() : request.getToUserIds();
        for (int i = 0; i < asked.size(); i++) {
            if (!known.contains(asked.get(i))) {
                errors.add(new FieldError("toUserIds[" + i + "]", "is not an account of the school", "NOT_FOUND"));
            }
        }
        if (request.getAboutStudentId() != null && !members.studentIn(request.getAboutStudentId(), institution)) {
            errors.add(new FieldError("aboutStudentId", "is not a student of the school", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private MailboxMessageEntity.MailboxMessageEntityBuilder<?, ?> copy(UUID tenant, UUID institution, UUID owner,
            String folder, String to, ComposeMailRequest request) {
        return MailboxMessageEntity.builder().tenantId(tenant).institutionId(institution).ownerUserId(owner)
                .aboutStudentId(request.getAboutStudentId()).folder(folder).toAddresses(to)
                .subject(request.getSubject()).body(request.getBody());
    }

    private MailboxMessageEntity stored(MailboxMessageEntity entity) {
        entity.onCreatingData(Actor.current(), clock);
        MailboxMessageEntity stored = repository.insert(entity);
        audit.recordCreate(AGGREGATE, stored.getId());
        outbox.publish(AGGREGATE + CommsEvents.CREATED, AGGREGATE, stored.getId(), CommsEvents.payload(stored.getId()));
        return stored;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    /**
     * The caller's own message: mail is owner-only, so a message of another mailbox — or outside the caller's
     * institutions — is 404 whatever the caller's grants.
     */
    private MailboxMessageEntity loadVisible(DataScope scope, UUID id) {
        MailboxMessageEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        OwnRows.require(entity.getOwnerUserId(), AGGREGATE, id);
        return entity;
    }

    /** The institution that owns the row. */
    private Optional<UUID> institutionOf(MailboxMessageEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(MailboxMessageEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getInstitutionId() == null) {
            errors.add(new FieldError("institutionId", "must not be null", "NotNull"));
        }
        if (entity.getFolder() == null) {
            errors.add(new FieldError("folder", "must not be null", "NotNull"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(MailboxMessageEntity entity) {

    }
}
