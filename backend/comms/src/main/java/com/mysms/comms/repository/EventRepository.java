package com.mysms.comms.repository;
import com.mysms.authclient.DataScope;
import com.mysms.comms.config.CommsDatabaseConfig;
import com.mysms.comms.domain.EventEntity;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
/**
 * Plain-JDBC access to comms.event: live rows only, keyset paging, and the row_version guard on every update and
 * delete. Every statement filters the caller's tenant itself; row-level security is the backstop.
 */
@Repository
public class EventRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("institutionId", "r.institution_id", ValueType.UUID),
            field("academicYearId", "r.academic_year_id", ValueType.UUID),
            field("audience", "r.audience", ValueType.STRING),
            field("classSectionId", "r.class_section_id", ValueType.UUID),
            field("audienceLabel", "r.audience_label", ValueType.STRING),
            field("title", "r.title", ValueType.STRING),
            field("description", "r.description", ValueType.STRING),
            field("category", "r.category", ValueType.STRING),
            field("location", "r.location", ValueType.STRING),
            field("organisedBy", "r.organised_by", ValueType.STRING),
            field("imageFileId", "r.image_file_id", ValueType.UUID),
            field("color", "r.color", ValueType.STRING),
            field("startAt", "r.start_at", ValueType.TIMESTAMP),
            field("endAt", "r.end_at", ValueType.TIMESTAMP),
            field("isAllDay", "r.is_all_day", ValueType.BOOLEAN),
            field("repeatCount", "r.repeat_count", ValueType.INTEGER),
            field("repeatInterval", "r.repeat_interval", ValueType.INTEGER),
            field("repeatUnit", "r.repeat_unit", ValueType.STRING),
            field("url", "r.url", ValueType.STRING),
            field("status", "r.status", ValueType.STRING),
            field("cohortId", "r.cohort_id", ValueType.UUID),
            field("ownerUserId", "r.owner_user_id", ValueType.UUID),
            field("recurrenceRule", "r.recurrence_rule", ValueType.STRING),
            field("forAllMembers", "r.for_all_members", ValueType.BOOLEAN),
            field("staffCanEdit", "r.staff_can_edit", ValueType.BOOLEAN),
            field("importance", "r.importance", ValueType.STRING),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("title", "r.title", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<EventEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", EventEntity::getCreatedAt),
            Map.entry("updateTime", EventEntity::getUpdatedAt),
            Map.entry("title", EventEntity::getTitle));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.institution_id, r.academic_year_id, r.audience, r.class_section_id,
                   r.audience_label, r.title, r.description, r.category, r.location, r.organised_by, r.image_file_id,
                   r.color, r.start_at, r.end_at, r.is_all_day, r.repeat_count, r.repeat_interval, r.repeat_unit,
                   r.url, r.status, r.cohort_id, r.owner_user_id, r.recurrence_rule, r.for_all_members,
                   r.staff_can_edit, r.importance, r.created_at, r.created_by, r.updated_at, r.updated_by,
                   r.deleted_at, r.row_version
              FROM comms.event r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO comms.event
            (id, tenant_id, institution_id, academic_year_id, audience, class_section_id, audience_label, title,
             description, category, location, organised_by, image_file_id, color, start_at, end_at, is_all_day,
             repeat_count, repeat_interval, repeat_unit, url, cohort_id, owner_user_id, recurrence_rule,
             for_all_members, staff_can_edit, importance, created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :academicYearId,
                    COALESCE(:audience, 'school'::character varying), :classSectionId, :audienceLabel, :title,
                    :description, COALESCE(:category, 'other'::character varying), :location, :organisedBy,
                    :imageFileId, :color, :startAt, :endAt, COALESCE(:isAllDay, true), COALESCE(:repeatCount, 0),
                    COALESCE(:repeatInterval, 0), :repeatUnit, :url, :cohortId, :ownerUserId, :recurrenceRule,
                    COALESCE(:forAllMembers, true), COALESCE(:staffCanEdit, false), :importance, :createdAt,
                    :createdBy, :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, institution_id, academic_year_id, audience, class_section_id, audience_label,
                      title, description, category, location, organised_by, image_file_id, color, start_at, end_at,
                      is_all_day, repeat_count, repeat_interval, repeat_unit, url, status, cohort_id, owner_user_id,
                      recurrence_rule, for_all_members, staff_can_edit, importance, created_at, created_by, updated_at,
                      updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE comms.event
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE comms.event
               SET institution_id = :institutionId,
                   academic_year_id = :academicYearId,
                   audience = :audience,
                   class_section_id = :classSectionId,
                   audience_label = :audienceLabel,
                   title = :title,
                   description = :description,
                   category = :category,
                   location = :location,
                   organised_by = :organisedBy,
                   image_file_id = :imageFileId,
                   color = :color,
                   start_at = :startAt,
                   end_at = :endAt,
                   is_all_day = :isAllDay,
                   repeat_count = :repeatCount,
                   repeat_interval = :repeatInterval,
                   repeat_unit = :repeatUnit,
                   url = :url,
                   cohort_id = :cohortId,
                   owner_user_id = :ownerUserId,
                   recurrence_rule = :recurrenceRule,
                   for_all_members = :forAllMembers,
                   staff_can_edit = :staffCanEdit,
                   importance = :importance,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String REFERENCED_BY_EVENT_ATTENDEE_EVENT_ID = """
            SELECT EXISTS (SELECT 1 FROM comms.event_attendee WHERE tenant_id = :tenantId
                AND event_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_EVENT_GALLERY_ITEM_EVENT_ID = """
            SELECT EXISTS (SELECT 1 FROM comms.event_gallery_item WHERE tenant_id = :tenantId
                AND event_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_EVENT_INSTITUTION_EVENT_ID = """
            SELECT EXISTS (SELECT 1 FROM comms.event_institution WHERE tenant_id = :tenantId
                AND event_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_EVENT_OCCURRENCE_EVENT_ID = """
            SELECT EXISTS (SELECT 1 FROM comms.event_occurrence WHERE tenant_id = :tenantId
                AND event_id = :id AND deleted_at IS NULL)
            """;

    /** comms.ActivateEvent: allowed while status is inactive. */
    private static final String TRANSITION_ACTIVATE_EVENT = """
            UPDATE comms.event
               SET status = 'active',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    /** comms.DeactivateEvent: allowed while status is active. */
    private static final String TRANSITION_DEACTIVATE_EVENT = """
            UPDATE comms.event
               SET status = 'inactive',
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.institution_id = ANY(:scopeIds) OR r.institution_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public EventRepository(@Qualifier(CommsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<EventEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(EventRepository::map).optional();
    }

    public Page<EventEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<EventEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(EventRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), EventEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public EventEntity insert(EventEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("institutionId", e.getInstitutionId())
                .param("academicYearId", e.getAcademicYearId())
                .param("audience", e.getAudience())
                .param("classSectionId", e.getClassSectionId())
                .param("audienceLabel", e.getAudienceLabel())
                .param("title", e.getTitle())
                .param("description", e.getDescription())
                .param("category", e.getCategory())
                .param("location", e.getLocation())
                .param("organisedBy", e.getOrganisedBy())
                .param("imageFileId", e.getImageFileId())
                .param("color", e.getColor())
                .param("startAt", e.getStartAt())
                .param("endAt", e.getEndAt())
                .param("isAllDay", e.getIsAllDay())
                .param("repeatCount", e.getRepeatCount())
                .param("repeatInterval", e.getRepeatInterval())
                .param("repeatUnit", e.getRepeatUnit())
                .param("url", e.getUrl())
                .param("cohortId", e.getCohortId())
                .param("ownerUserId", e.getOwnerUserId())
                .param("recurrenceRule", e.getRecurrenceRule())
                .param("forAllMembers", e.getForAllMembers())
                .param("staffCanEdit", e.getStaffCanEdit())
                .param("importance", e.getImportance())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(EventRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(EventEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("institutionId", e.getInstitutionId())
                .param("academicYearId", e.getAcademicYearId())
                .param("audience", e.getAudience())
                .param("classSectionId", e.getClassSectionId())
                .param("audienceLabel", e.getAudienceLabel())
                .param("title", e.getTitle())
                .param("description", e.getDescription())
                .param("category", e.getCategory())
                .param("location", e.getLocation())
                .param("organisedBy", e.getOrganisedBy())
                .param("imageFileId", e.getImageFileId())
                .param("color", e.getColor())
                .param("startAt", e.getStartAt())
                .param("endAt", e.getEndAt())
                .param("isAllDay", e.getIsAllDay())
                .param("repeatCount", e.getRepeatCount())
                .param("repeatInterval", e.getRepeatInterval())
                .param("repeatUnit", e.getRepeatUnit())
                .param("url", e.getUrl())
                .param("cohortId", e.getCohortId())
                .param("ownerUserId", e.getOwnerUserId())
                .param("recurrenceRule", e.getRecurrenceRule())
                .param("forAllMembers", e.getForAllMembers())
                .param("staffCanEdit", e.getStaffCanEdit())
                .param("importance", e.getImportance())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(EventEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether live comms.event_attendee rows still reference the row. */
    public boolean referencedByEventAttendeeEventId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_EVENT_ATTENDEE_EVENT_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live comms.event_gallery_item rows still reference the row. */
    public boolean referencedByEventGalleryItemEventId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_EVENT_GALLERY_ITEM_EVENT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live comms.event_institution rows still reference the row. */
    public boolean referencedByEventInstitutionEventId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_EVENT_INSTITUTION_EVENT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live comms.event_occurrence rows still reference the row. */
    public boolean referencedByEventOccurrenceEventId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_EVENT_OCCURRENCE_EVENT_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** comms.ActivateEvent; 0 rows means the row changed (or vanished) since it was read. */
    public int activateEvent(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_ACTIVATE_EVENT)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** comms.DeactivateEvent; 0 rows means the row changed (or vanished) since it was read. */
    public int deactivateEvent(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(TRANSITION_DEACTIVATE_EVENT)
                .param("id", id)
                .param("expected", expected)
                .param("now", now)
                .param("actor", actor).update();
    }

    /** Rows of the caller's institutions only. */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, EventEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static EventEntity map(ResultSet rs, int row) throws SQLException {
        return EventEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .institutionId(rs.getObject("institution_id", UUID.class))
                .academicYearId(rs.getObject("academic_year_id", UUID.class))
                .audience(rs.getString("audience"))
                .classSectionId(rs.getObject("class_section_id", UUID.class))
                .audienceLabel(rs.getString("audience_label"))
                .title(rs.getString("title"))
                .description(rs.getString("description"))
                .category(rs.getString("category"))
                .location(rs.getString("location"))
                .organisedBy(rs.getString("organised_by"))
                .imageFileId(rs.getObject("image_file_id", UUID.class))
                .color(rs.getString("color"))
                .startAt(rs.getObject("start_at", OffsetDateTime.class))
                .endAt(rs.getObject("end_at", OffsetDateTime.class))
                .isAllDay(rs.getObject("is_all_day", Boolean.class))
                .repeatCount(rs.getObject("repeat_count", Integer.class))
                .repeatInterval(rs.getObject("repeat_interval", Integer.class))
                .repeatUnit(rs.getString("repeat_unit"))
                .url(rs.getString("url"))
                .status(rs.getString("status"))
                .cohortId(rs.getObject("cohort_id", UUID.class))
                .ownerUserId(rs.getObject("owner_user_id", UUID.class))
                .recurrenceRule(rs.getString("recurrence_rule"))
                .forAllMembers(rs.getObject("for_all_members", Boolean.class))
                .staffCanEdit(rs.getObject("staff_can_edit", Boolean.class))
                .importance(rs.getString("importance"))
                .createdAt(rs.getObject("created_at", OffsetDateTime.class))
                .createdBy(rs.getObject("created_by", UUID.class))
                .updatedAt(rs.getObject("updated_at", OffsetDateTime.class))
                .updatedBy(rs.getObject("updated_by", UUID.class))
                .deletedAt(rs.getObject("deleted_at", OffsetDateTime.class))
                .rowVersion(rs.getInt("row_version"))
                .build();
    }

    private static FieldSpec field(String name, String column, ValueType type) {
        return new FieldSpec(name, column, type);
    }

    private static Map<String, FieldSpec> fields(FieldSpec... specs) {
        return Arrays.stream(specs).collect(Collectors.toUnmodifiableMap(FieldSpec::name, s -> s));
    }
}
