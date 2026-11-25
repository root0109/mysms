package com.mysms.comms.business;

import com.mysms.comms.api.model.ArchiveNotificationsRequest;
import com.mysms.comms.api.model.InAppNotification;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.config.CommsEventsConfig;
import com.mysms.comms.domain.MessageTemplateEntity;
import com.mysms.comms.events.CommsEvents;
import com.mysms.comms.mapper.InAppNotificationMapper;
import com.mysms.comms.repository.InAppNotificationRepository;
import com.mysms.comms.repository.MessageTemplateRepository;
import com.mysms.comms.repository.StudentRecordRepository;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.id.UuidV7;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The caller's in-app inbox (comms.in_app_notification): reads, read state and archiving touch only the caller's own
 * notifications, whatever the caller's grants; another account's notification is answered as absent. Notifications
 * are written by comms itself, here for the greetings people asks for.
 */
@Service
public class InAppNotificationService extends AppTransactionManager {

    public static final String AGGREGATE = "comms.in_app_notification";
    /** The catalogue's notification type of a greeting (seeded by the comms baseline) and the record it is about. */
    public static final String GREETING = "people.greeting";
    static final int TITLE_LENGTH = 255;
    /** The event announcing a greeting comms could not deliver (people takes it back). */
    public static final String GREETING_REJECTED = "comms.greeting_delivery.rejected";
    static final String GREETING_DELIVERY = "comms.greeting_delivery";
    /** How far back a delivery looks for notifications of the same greeting (greetings are delivered within hours). */
    static final Duration GREETING_REDELIVERY = Duration.ofDays(7);

    private final InAppNotificationRepository repository;
    private final StudentRecordRepository students;
    private final MessageTemplateRepository templates;
    private final InAppNotificationMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;

    public InAppNotificationService(InAppNotificationRepository repository, StudentRecordRepository students,
            MessageTemplateRepository templates, InAppNotificationMapper mapper,
            @Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(CommsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(CommsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(CommsEventsConfig.AUDIT) AuditWriter audit, Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.students = students;
        this.templates = templates;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
    }

    public InAppNotification getInAppNotification(UUID id) {
        UUID caller = OwnRows.caller();
        return doInReadTransaction(() -> mapper.toDto(repository.findOwn(caller, id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id))));
    }

    /** The caller's inbox; a partitioned table, so the filter must bound createTime on both sides. */
    public Page<InAppNotification> listInAppNotifications(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, InAppNotificationRepository.SORT_FIELDS,
                InAppNotificationRepository.DEFAULT_ORDER, InAppNotificationRepository.ID_COLUMN);
        FilterParser.requireRange(filter, InAppNotificationRepository.FILTER_FIELDS, "createTime");
        SqlFragment where = FilterParser.parse(filter, InAppNotificationRepository.FILTER_FIELDS);
        UUID caller = OwnRows.caller();
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + caller, clock);
        return doInReadTransaction(() -> repository.listOwn(caller, page, where).map(mapper::toDto));
    }

    /** Marks the caller's notification read; one already read is answered as it stands. */
    public InAppNotification markNotificationRead(UUID id) {
        UUID caller = OwnRows.caller();
        return doInWriteTransaction(() -> {
            repository.findOwn(caller, id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (repository.markRead(caller, id, now()) == 1) {
                recorded(id, "mark_read", "readAt");
            }
            return mapper.toDto(repository.findOwn(caller, id)
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)));
        });
    }

    /** Marks every unread notification in the caller's inbox read. */
    public void markAllNotificationsRead() {
        UUID caller = OwnRows.caller();
        runInWriteTransaction(() -> repository.markAllRead(caller, now())
                .forEach(id -> recorded(id, "mark_read", "readAt")));
    }

    /**
     * Takes the listed notifications of the caller out of the inbox, or every read one when none is listed; ids that
     * are not the caller's change nothing.
     */
    public void archiveNotifications(ArchiveNotificationsRequest request) {
        UUID caller = OwnRows.caller();
        List<UUID> ids = request == null || request.getNotificationIds() == null ? List.of()
                : request.getNotificationIds();
        runInWriteTransaction(() -> {
            OffsetDateTime now = now();
            List<UUID> archived = ids.isEmpty() ? repository.archiveRead(caller, now)
                    : repository.archive(caller, ids, now);
            archived.forEach(id -> recorded(id, "archive", "archivedAt"));
        });
    }

    /**
     * Delivers a greeting people asked for as an in-app notification to each login account comms knows of the person:
     * the accounts of their student records (comms holds no account of a staff member or guardian yet, so their
     * greeting reaches no one). The template's subject (else its name) is the title and its body the text. Runs in the
     * event's tenant, joining the inbox transaction. Idempotent on the greeting: an account already told about it is
     * not told again. An absent or inactive template writes nothing and announces the greeting as rejected
     * ({@value #GREETING_REJECTED}), so people takes it back and it costs the sender nothing. Answers how many
     * notifications were written.
     */
    public int notifyGreeting(UUID greetingId, UUID personId, UUID templateId) {
        return doInWriteTransaction(() -> {
            List<UUID> accounts = students.accountsOf(personId);
            if (accounts.isEmpty()) {
                return 0;
            }
            Optional<MessageTemplateEntity> found = templates.findById(templateId)
                    .filter(template -> Boolean.TRUE.equals(template.getIsActive()));
            if (found.isEmpty()) {
                outbox.publish(GREETING_REJECTED, GREETING_DELIVERY, greetingId, Map.of("id", greetingId.toString(),
                        "reason", "TEMPLATE_UNAVAILABLE"));
                return 0;
            }
            MessageTemplateEntity template = found.get();
            UUID type = repository.typeId(GREETING)
                    .orElseThrow(() -> new IllegalStateException("notification type " + GREETING + " is not seeded"));
            String heading = template.getSubject() == null || template.getSubject().isBlank() ? template.getName()
                    : template.getSubject();
            String title = TemplateText.render(heading, Map.of());
            String body = TemplateText.render(template.getBody(), Map.of());
            OffsetDateTime now = now();
            Set<UUID> told = repository.notified(GREETING, greetingId, now.minus(GREETING_REDELIVERY));
            int written = 0;
            for (UUID account : accounts) {
                if (told.contains(account)) {
                    continue;
                }
                UUID id = UuidV7.next(clock);
                repository.insert(id, account, type, title.length() > TITLE_LENGTH ? title.substring(0, TITLE_LENGTH)
                        : title, body, GREETING, greetingId, now);
                audit.recordCreate(AGGREGATE, id);
                outbox.publish(AGGREGATE + CommsEvents.CREATED, AGGREGATE, id, CommsEvents.payload(id));
                written++;
            }
            return written;
        });
    }

    private void recorded(UUID id, String step, String field) {
        audit.recordTransition(AGGREGATE, id, step, List.of(field), null);
        outbox.publish(AGGREGATE + "." + step, AGGREGATE, id, CommsEvents.payload(id));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
