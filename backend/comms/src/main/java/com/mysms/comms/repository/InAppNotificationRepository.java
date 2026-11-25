package com.mysms.comms.repository;

import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain-JDBC access to comms.in_app_notification, always narrowed to one recipient: a notification is read and changed
 * only by the account it was sent to. Archived rows have left the inbox and are not listed.
 */
@Repository
public class InAppNotificationRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("readAt", "r.read_at", ValueType.TIMESTAMP),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("notificationTypeId", "r.notification_type_id", ValueType.UUID),
            field("cohortId", "r.cohort_id", ValueType.UUID),
            field("entityType", "r.entity_type", ValueType.STRING),
            field("entityId", "r.entity_id", ValueType.UUID));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP));

    /** The newest first, as an inbox reads. */
    public static final String DEFAULT_ORDER = "createTime,desc";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<InAppNotificationRow, Object>> SORT_VALUES = Map.of(
            "createTime", InAppNotificationRow::createdAt);

    private static final String COLUMNS = """
            r.id, r.recipient_user_id, r.read_at, r.created_at, r.notification_type_id, r.title, r.body, r.sms_text,
            r.action_url, r.cohort_id, r.email_status, r.sms_status, r.popup_status, r.digest_status, r.archived_at,
            r.entity_type, r.entity_id, r.updated_at
            """;
    private static final String SELECT_OWN = "SELECT " + COLUMNS + """
              FROM comms.in_app_notification r
             WHERE r.tenant_id = :tenantId
               AND r.recipient_user_id = :recipient
            """;
    private static final String FIND_OWN = SELECT_OWN + " AND r.id = :id";
    private static final String LIST_OWN = SELECT_OWN + " AND r.archived_at IS NULL";
    private static final String MARK_READ = """
            UPDATE comms.in_app_notification
               SET read_at = :now,
                   updated_at = :now
             WHERE tenant_id = :tenantId
               AND recipient_user_id = :recipient AND id = :id AND read_at IS NULL
            """;
    private static final String MARK_ALL_READ = """
            UPDATE comms.in_app_notification
               SET read_at = :now,
                   updated_at = :now
             WHERE tenant_id = :tenantId
               AND recipient_user_id = :recipient AND read_at IS NULL AND archived_at IS NULL
            RETURNING id
            """;
    private static final String ARCHIVE = """
            UPDATE comms.in_app_notification
               SET archived_at = :now,
                   updated_at = :now
             WHERE tenant_id = :tenantId
               AND recipient_user_id = :recipient AND id = ANY(:ids) AND archived_at IS NULL
            RETURNING id
            """;
    private static final String ARCHIVE_READ = """
            UPDATE comms.in_app_notification
               SET archived_at = :now,
                   updated_at = :now
             WHERE tenant_id = :tenantId
               AND recipient_user_id = :recipient AND read_at IS NOT NULL AND archived_at IS NULL
            RETURNING id
            """;

    private static final String INSERT = """
            INSERT INTO comms.in_app_notification
            (id, tenant_id, recipient_user_id, created_at, notification_type_id, title, body, entity_type, entity_id,
             updated_at)
            VALUES (:id, :tenantId, :recipient, :now, :typeId, :title, :body, :entityType, :entityId, :now)
            """;
    /** The accounts already told about a record since a moment (the partition range keeps the read narrow). */
    private static final String NOTIFIED = """
            SELECT r.recipient_user_id
              FROM comms.in_app_notification r
             WHERE r.tenant_id = :tenantId
               AND r.created_at >= :since
               AND r.entity_type = :entityType
               AND r.entity_id = :entityId
            """;
    private static final String TYPE_BY_CODE = """
            SELECT t.id
              FROM comms.notification_type t
             WHERE t.code = :code
               AND t.is_enabled
               AND t.deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public InAppNotificationRepository(@Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** The recipient's own notification, archived or not. */
    public Optional<InAppNotificationRow> findOwn(UUID recipient, UUID id) {
        return jdbc.sql(FIND_OWN).param("recipient", recipient).param("id", id)
                .query(InAppNotificationRepository::map).optional();
    }

    /** The recipient's inbox: rows not archived, filtered and paged. */
    public Page<InAppNotificationRow> listOwn(UUID recipient, PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = LIST_OWN + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<InAppNotificationRow> rows = jdbc.sql(sql).param("recipient", recipient).params(where.params())
                .params(page.limitParams()).query(InAppNotificationRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), InAppNotificationRow::id);
    }

    /** The accounts told about the record since the moment. */
    public Set<UUID> notified(String entityType, UUID entityId, OffsetDateTime since) {
        return new HashSet<>(jdbc.sql(NOTIFIED).param("entityType", entityType).param("entityId", entityId)
                .param("since", since).query(UUID.class).list());
    }

    /** Puts a new unread notification about a record into the recipient's inbox. */
    public void insert(UUID id, UUID recipient, UUID typeId, String title, String body, String entityType,
            UUID entityId, OffsetDateTime now) {
        jdbc.sql(INSERT).param("id", id).param("recipient", recipient).param("typeId", typeId).param("title", title)
                .param("body", body).param("entityType", entityType).param("entityId", entityId).param("now", now)
                .update();
    }

    /** The enabled notification type of the catalogue with this code (a global table). */
    public Optional<UUID> typeId(String code) {
        return jdbc.sql(TYPE_BY_CODE).param("code", code).query(UUID.class).optional();
    }

    /** Marks one unread notification of the recipient read; 0 when it was read already. */
    public int markRead(UUID recipient, UUID id, OffsetDateTime now) {
        return jdbc.sql(MARK_READ).param("recipient", recipient).param("id", id).param("now", now).update();
    }

    /** Marks every unread notification in the recipient's inbox read; the ids it changed. */
    public List<UUID> markAllRead(UUID recipient, OffsetDateTime now) {
        return jdbc.sql(MARK_ALL_READ).param("recipient", recipient).param("now", now).query(UUID.class).list();
    }

    /** Archives those of the ids that are the recipient's and still in the inbox; the ids it changed. */
    public List<UUID> archive(UUID recipient, List<UUID> ids, OffsetDateTime now) {
        return jdbc.sql(ARCHIVE).param("recipient", recipient).param("ids", ids.toArray(UUID[]::new))
                .param("now", now).query(UUID.class).list();
    }

    /** Archives every read notification still in the recipient's inbox; the ids it changed. */
    public List<UUID> archiveRead(UUID recipient, OffsetDateTime now) {
        return jdbc.sql(ARCHIVE_READ).param("recipient", recipient).param("now", now).query(UUID.class).list();
    }

    private static List<Object> sortValues(SortSpec sort, InAppNotificationRow e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static InAppNotificationRow map(ResultSet rs, int row) throws SQLException {
        return new InAppNotificationRow(rs.getObject("id", UUID.class), rs.getObject("recipient_user_id", UUID.class),
                rs.getObject("read_at", OffsetDateTime.class), rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("notification_type_id", UUID.class), rs.getString("title"), rs.getString("body"),
                rs.getString("sms_text"), rs.getString("action_url"), rs.getObject("cohort_id", UUID.class),
                rs.getString("email_status"), rs.getString("sms_status"), rs.getString("popup_status"),
                rs.getString("digest_status"), rs.getObject("archived_at", OffsetDateTime.class),
                rs.getString("entity_type"), rs.getObject("entity_id", UUID.class),
                rs.getObject("updated_at", OffsetDateTime.class));
    }

    private static FieldSpec field(String name, String column, ValueType type) {
        return new FieldSpec(name, column, type);
    }

    private static Map<String, FieldSpec> fields(FieldSpec... specs) {
        return Arrays.stream(specs).collect(Collectors.toUnmodifiableMap(FieldSpec::name, s -> s));
    }
}
