package com.mysms.comms.repository;

import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The calendar of one person, read from comms.event and comms.event_occurrence: active events of their institutions
 * and of the tenant, their own personal events and the events they are invited to. Another person's personal event
 * never shows. Class, cohort and alumni events show only to their invited attendees, since comms does not know who is
 * in which class or cohort.
 *
 * <p>Events that are materialised as occurrences show as their occurrences (a person's own copy, or the shared
 * one); the others show as themselves, as holidays when their category is holiday. Both queries read in the order
 * start, id, so a page is the first rows of the two merged.
 */
@Repository
public class MyCalendarRepository {

    /** The calendar's order: start, then id. */
    public static final Map<String, FieldSpec> SORT_FIELDS = Map.of(
            "startAt", new FieldSpec("startAt", "r.start_at", ValueType.TIMESTAMP));
    public static final String ORDER = "startAt";
    public static final String ID_COLUMN = "r.id";
    private static final String IN_ORDER = "r.start_at ASC, r.id ASC";

    private static final String EVENTS = """
            SELECT CASE WHEN r.category = 'holiday' THEN 'holiday' ELSE 'event' END AS item_type, r.id, r.title,
                   r.start_at, r.end_at, r.is_all_day, r.color, r.cohort_id, NULL::boolean AS is_cancelled,
                   NULL::boolean AS is_completed
              FROM comms.event r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL AND r.status = 'active' AND r.start_at IS NOT NULL
               AND r.start_at < :to AND COALESCE(r.end_at, r.start_at) >= :from
               AND ((r.category = 'holiday' AND :holidays) OR (r.category <> 'holiday' AND :events))
               AND (CAST(:cohortId AS uuid) IS NULL OR r.cohort_id = :cohortId)
               AND NOT EXISTS (SELECT 1 FROM comms.event_occurrence o
                                WHERE o.tenant_id = :tenantId
                                  AND o.event_id = r.id AND o.deleted_at IS NULL)
               AND (r.owner_user_id = :me
                    OR EXISTS (SELECT 1 FROM comms.event_attendee a
                                WHERE a.tenant_id = :tenantId
                                  AND a.event_id = r.id AND a.user_id = :me AND a.deleted_at IS NULL)
                    OR r.audience = 'tenant'
                    OR (r.audience = 'school'
                        AND (r.institution_id IS NULL OR :everywhere OR r.institution_id = ANY(:institutions))))
               AND
            """;
    private static final String OCCURRENCES = """
            SELECT 'occurrence' AS item_type, r.id, COALESCE(r.title, e.title) AS title, r.start_at, r.end_at,
                   e.is_all_day, COALESCE(r.color, e.color) AS color, COALESCE(r.cohort_id, e.cohort_id) AS cohort_id,
                   r.is_cancelled, r.response_status = 'completed' AS is_completed
              FROM comms.event_occurrence r
              JOIN comms.event e ON e.tenant_id = :tenantId
                   AND e.id = r.event_id AND e.deleted_at IS NULL AND e.status = 'active'
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
               AND r.start_at < :to AND COALESCE(r.end_at, r.start_at) >= :from
               AND (CAST(:cohortId AS uuid) IS NULL OR COALESCE(r.cohort_id, e.cohort_id) = :cohortId)
               AND (r.user_id = :me
                    OR (r.user_id IS NULL
                        AND (e.owner_user_id = :me
                             OR EXISTS (SELECT 1 FROM comms.event_attendee a
                                         WHERE a.tenant_id = :tenantId
                                           AND a.event_id = e.id AND a.user_id = :me AND a.deleted_at IS NULL)
                             OR e.audience = 'tenant'
                             OR (e.audience = 'school'
                                 AND (e.institution_id IS NULL OR :everywhere
                                      OR e.institution_id = ANY(:institutions))))))
               AND
            """;

    private final JdbcClient jdbc;

    public MyCalendarRepository(@Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Whose calendar, over which window, and which of its kinds. */
    public record Window(UUID me, boolean everywhere, UUID[] institutions, OffsetDateTime from, OffsetDateTime to,
            UUID cohortId, boolean events, boolean holidays, boolean occurrences) {

        public Window {
            institutions = institutions.clone();
        }

        @Override
        public UUID[] institutions() {
            return institutions.clone();
        }
    }

    /**
     * Up to {@code limit} items of the calendar after the keyset position, in order: the first rows of each kind,
     * merged.
     */
    public List<CalendarRow> read(Window window, SqlFragment after, String orderBy, int limit) {
        Map<String, Object> params = new HashMap<>(after.params());
        params.put("me", window.me());
        params.put("everywhere", window.everywhere());
        params.put("institutions", window.institutions());
        params.put("from", window.from());
        params.put("to", window.to());
        params.put("cohortId", window.cohortId());
        params.put("rows", limit);
        String tail = " (" + after.sql() + ") ORDER BY " + orderBy + " LIMIT :rows";
        List<CalendarRow> rows = new ArrayList<>();
        if (window.events() || window.holidays()) {
            params.put("events", window.events());
            params.put("holidays", window.holidays());
            rows.addAll(jdbc.sql(EVENTS + tail).params(params).query(MyCalendarRepository::map).list());
        }
        if (window.occurrences()) {
            rows.addAll(jdbc.sql(OCCURRENCES + tail).params(params).query(MyCalendarRepository::map).list());
        }
        // uuids compare as PostgreSQL orders them: byte by byte, unsigned, which is their text order
        rows.sort(Comparator.comparing(CalendarRow::startAt).thenComparing(item -> item.id().toString()));
        return rows.size() > limit ? List.copyOf(rows.subList(0, limit)) : rows;
    }

    /** The first {@code limit} items of the calendar, in order. */
    public List<CalendarRow> readAll(Window window, int limit) {
        return read(window, SqlFragment.TRUE, IN_ORDER, limit);
    }

    private static CalendarRow map(ResultSet rs, int row) throws SQLException {
        return new CalendarRow(rs.getString("item_type"), rs.getObject("id", UUID.class), rs.getString("title"),
                rs.getObject("start_at", OffsetDateTime.class), rs.getObject("end_at", OffsetDateTime.class),
                (Boolean) rs.getObject("is_all_day"), rs.getString("color"), rs.getObject("cohort_id", UUID.class),
                (Boolean) rs.getObject("is_cancelled"), (Boolean) rs.getObject("is_completed"));
    }
}
