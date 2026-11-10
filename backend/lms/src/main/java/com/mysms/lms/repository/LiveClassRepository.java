package com.mysms.lms.repository;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.domain.LiveClassEntity;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
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
 * Plain-JDBC access to lms.live_class (JAVA-CODING-RULES §6): schema-qualified constant SQL, named parameters, live
 * rows only,
 * keyset paging over an allowlisted order and the row_version guard on every update and delete. Every statement
 * filters the caller's tenant itself ({@code tenant_id = :tenantId}, bound by core's TenantJdbcClient); row-level
 * security is the backstop.
 */
@Repository
public class LiveClassRepository {

    /** Fields clients may filter on (contract names → columns). */
    public static final Map<String, FieldSpec> FILTER_FIELDS = fields(
            field("id", "r.id", ValueType.UUID),
            field("cohortId", "r.cohort_id", ValueType.UUID),
            field("title", "r.title", ValueType.STRING),
            field("description", "r.description", ValueType.STRING),
            field("classDate", "r.class_date", ValueType.DATE),
            field("durationMinutes", "r.duration_minutes", ValueType.INTEGER),
            field("timezone", "r.timezone", ValueType.STRING),
            field("hostUserId", "r.host_user_id", ValueType.UUID),
            field("conferencingProvider", "r.conferencing_provider", ValueType.STRING),
            field("conferencingAccountId", "r.conferencing_account_id", ValueType.UUID),
            field("meetingId", "r.meeting_id", ValueType.STRING),
            field("meetingUuid", "r.meeting_uuid", ValueType.STRING),
            field("joinUrl", "r.join_url", ValueType.STRING),
            field("autoRecording", "r.auto_recording", ValueType.STRING),
            field("calendarEventId", "r.calendar_event_id", ValueType.UUID),
            field("attendeeCount", "r.attendee_count", ValueType.INTEGER),
            field("inviteStatus", "r.invite_status", ValueType.STRING),
            field("inviteAttempts", "r.invite_attempts", ValueType.INTEGER),
            field("welcomeMessage", "r.welcome_message", ValueType.STRING),
            field("providerCreatedRef", "r.provider_created_ref", ValueType.STRING),
            field("recordingUrl", "r.recording_url", ValueType.STRING),
            field("inviteeCount", "r.invitee_count", ValueType.INTEGER),
            field("prepMinutes", "r.prep_minutes", ValueType.INTEGER),
            field("extensionMinutes", "r.extension_minutes", ValueType.INTEGER),
            field("showInCalendar", "r.show_in_calendar", ValueType.BOOLEAN),
            field("hostJoined", "r.host_joined", ValueType.BOOLEAN),
            field("isActive", "r.is_active", ValueType.BOOLEAN),
            field("endedAt", "r.ended_at", ValueType.TIMESTAMP),
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP));

    /** Fields clients may order by; NOT NULL expressions, so keyset paging can compare them. */
    public static final Map<String, FieldSpec> SORT_FIELDS = fields(
            field("createTime", "r.created_at", ValueType.TIMESTAMP),
            field("updateTime", "r.updated_at", ValueType.TIMESTAMP),
            field("title", "r.title", ValueType.STRING));

    public static final String DEFAULT_ORDER = "createTime";
    public static final String ID_COLUMN = "r.id";

    private static final Map<String, Function<LiveClassEntity, Object>> SORT_VALUES = Map.ofEntries(
            Map.entry("createTime", LiveClassEntity::getCreatedAt),
            Map.entry("updateTime", LiveClassEntity::getUpdatedAt),
            Map.entry("title", LiveClassEntity::getTitle));

    private static final String SELECT_LIVE = """
            SELECT r.id, r.tenant_id, r.cohort_id, r.title, r.description, r.class_date, r.start_time,
                   r.duration_minutes, r.timezone, r.host_user_id, r.conferencing_provider, r.conferencing_account_id,
                   r.meeting_id, r.meeting_uuid, r.join_url, r.meeting_password_enc, r.auto_recording,
                   r.calendar_event_id, r.attendee_count, r.invite_status, r.invite_attempts,
                   r.moderator_password_enc, r.welcome_message, r.provider_created_ref, r.recording_url,
                   r.invitee_count, r.prep_minutes, r.extension_minutes, r.show_in_calendar, r.host_joined,
                   r.is_active, r.ended_at, r.created_at, r.created_by, r.updated_at, r.updated_by, r.deleted_at,
                   r.row_version
              FROM lms.live_class r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
            """;
    private static final String FIND_BY_ID = SELECT_LIVE + " AND r.id = :id";
    private static final String INSERT = """
            INSERT INTO lms.live_class
            (id, tenant_id, cohort_id, title, description, class_date, start_time, duration_minutes, timezone,
             host_user_id, conferencing_provider, conferencing_account_id, meeting_id, meeting_uuid, join_url,
             meeting_password_enc, auto_recording, calendar_event_id, attendee_count, invite_status, invite_attempts,
             moderator_password_enc, welcome_message, provider_created_ref, recording_url, invitee_count,
             prep_minutes, extension_minutes, show_in_calendar, host_joined, is_active, ended_at, created_at,
             created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :cohortId, :title, :description, :classDate, :startTime, :durationMinutes,
                    :timezone, :hostUserId, :conferencingProvider, :conferencingAccountId, :meetingId, :meetingUuid,
                    :joinUrl, :meetingPasswordEnc, COALESCE(:autoRecording, 'none'::character varying),
                    :calendarEventId, :attendeeCount, COALESCE(:inviteStatus, 'pending'::character varying),
                    COALESCE(:inviteAttempts, 0), :moderatorPasswordEnc, :welcomeMessage, :providerCreatedRef,
                    :recordingUrl, :inviteeCount, :prepMinutes, :extensionMinutes, COALESCE(:showInCalendar, true),
                    COALESCE(:hostJoined, false), COALESCE(:isActive, true), :endedAt, :createdAt, :createdBy,
                    :updatedAt, :updatedBy, :rowVersion)
            RETURNING id, tenant_id, cohort_id, title, description, class_date, start_time, duration_minutes, timezone,
                      host_user_id, conferencing_provider, conferencing_account_id, meeting_id, meeting_uuid, join_url,
                      meeting_password_enc, auto_recording, calendar_event_id, attendee_count, invite_status,
                      invite_attempts, moderator_password_enc, welcome_message, provider_created_ref, recording_url,
                      invitee_count, prep_minutes, extension_minutes, show_in_calendar, host_joined, is_active,
                      ended_at, created_at, created_by, updated_at, updated_by, deleted_at, row_version
            """;
    private static final String SOFT_DELETE = """
            UPDATE lms.live_class
               SET deleted_at = :deletedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String UPDATE = """
            UPDATE lms.live_class
               SET cohort_id = :cohortId,
                   title = :title,
                   description = :description,
                   class_date = :classDate,
                   start_time = :startTime,
                   duration_minutes = :durationMinutes,
                   timezone = :timezone,
                   host_user_id = :hostUserId,
                   conferencing_provider = :conferencingProvider,
                   conferencing_account_id = :conferencingAccountId,
                   meeting_id = :meetingId,
                   meeting_uuid = :meetingUuid,
                   join_url = :joinUrl,
                   meeting_password_enc = :meetingPasswordEnc,
                   auto_recording = :autoRecording,
                   calendar_event_id = :calendarEventId,
                   attendee_count = :attendeeCount,
                   invite_status = :inviteStatus,
                   invite_attempts = :inviteAttempts,
                   moderator_password_enc = :moderatorPasswordEnc,
                   welcome_message = :welcomeMessage,
                   provider_created_ref = :providerCreatedRef,
                   recording_url = :recordingUrl,
                   invitee_count = :inviteeCount,
                   prep_minutes = :prepMinutes,
                   extension_minutes = :extensionMinutes,
                   show_in_calendar = :showInCalendar,
                   host_joined = :hostJoined,
                   is_active = :isActive,
                   ended_at = :endedAt,
                   updated_at = :updatedAt,
                   updated_by = :updatedBy,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String EXISTS_COHORT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.cohort WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL)
            """;
    private static final String EXISTS_CONFERENCING_ACCOUNT_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.conferencing_account WHERE tenant_id = :tenantId
                AND id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_CONTENT_ITEM_LIVE_CLASS_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.content_item WHERE tenant_id = :tenantId
                AND live_class_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_LIVE_CLASS_PARTICIPANT_LIVE_CLASS_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.live_class_participant WHERE tenant_id = :tenantId
                AND live_class_id = :id AND deleted_at IS NULL)
            """;
    private static final String REFERENCED_BY_TIMETABLE_ENTRY_LIVE_CLASS_ID = """
            SELECT EXISTS (SELECT 1 FROM lms.timetable_entry WHERE tenant_id = :tenantId
                AND live_class_id = :id AND deleted_at IS NULL)
            """;
    private static final String INSTITUTION_OF = """
            SELECT p0.institution_id FROM lms.cohort p0 WHERE p0.tenant_id = :tenantId AND p0.id = :ref
            """;
    private static final String SCOPE = """
             (:scopeAll OR r.cohort_id IN (SELECT p0.id FROM lms.cohort p0 WHERE p0.tenant_id = :tenantId
                 AND (p0.institution_id = ANY(:scopeIds) OR
             p0.institution_id IS NULL)) OR r.cohort_id IS NULL)
            """;

    private final JdbcClient jdbc;

    public LiveClassRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<LiveClassEntity> findById(UUID id) {
        return jdbc.sql(FIND_BY_ID).param("id", id).query(LiveClassRepository::map).optional();
    }

    public Page<LiveClassEntity> list(PageRequest page, SqlFragment filter) {
        SqlFragment where = filter.and(page.keysetPredicate());
        String sql = SELECT_LIVE + " AND (" + where.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<LiveClassEntity> rows = jdbc.sql(sql).params(where.params()).params(page.limitParams())
                .query(LiveClassRepository::map).list();
        return page.toPage(rows, e -> sortValues(page.sort(), e), LiveClassEntity::getId);
    }

    /** Inserts the row and returns it as stored (database defaults of read-only columns included). */
    public LiveClassEntity insert(LiveClassEntity e) {
        return jdbc.sql(INSERT)
                .param("id", e.getId())
                .param("tenantId", e.getTenantId())
                .param("cohortId", e.getCohortId())
                .param("title", e.getTitle())
                .param("description", e.getDescription())
                .param("classDate", e.getClassDate())
                .param("startTime", e.getStartTime())
                .param("durationMinutes", e.getDurationMinutes())
                .param("timezone", e.getTimezone())
                .param("hostUserId", e.getHostUserId())
                .param("conferencingProvider", e.getConferencingProvider())
                .param("conferencingAccountId", e.getConferencingAccountId())
                .param("meetingId", e.getMeetingId())
                .param("meetingUuid", e.getMeetingUuid())
                .param("joinUrl", e.getJoinUrl())
                .param("meetingPasswordEnc", e.getMeetingPasswordEnc())
                .param("autoRecording", e.getAutoRecording())
                .param("calendarEventId", e.getCalendarEventId())
                .param("attendeeCount", e.getAttendeeCount())
                .param("inviteStatus", e.getInviteStatus())
                .param("inviteAttempts", e.getInviteAttempts())
                .param("moderatorPasswordEnc", e.getModeratorPasswordEnc())
                .param("welcomeMessage", e.getWelcomeMessage())
                .param("providerCreatedRef", e.getProviderCreatedRef())
                .param("recordingUrl", e.getRecordingUrl())
                .param("inviteeCount", e.getInviteeCount())
                .param("prepMinutes", e.getPrepMinutes())
                .param("extensionMinutes", e.getExtensionMinutes())
                .param("showInCalendar", e.getShowInCalendar())
                .param("hostJoined", e.getHostJoined())
                .param("isActive", e.getIsActive())
                .param("endedAt", e.getEndedAt())
                .param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("rowVersion", e.getRowVersion())
                .query(LiveClassRepository::map).single();
    }

    /** Updates under the ETag guard; 0 rows means the row changed (or vanished) since it was read. */
    public int update(LiveClassEntity e, int expectedVersion) {
        return jdbc.sql(UPDATE)
                .param("id", e.getId())
                .param("cohortId", e.getCohortId())
                .param("title", e.getTitle())
                .param("description", e.getDescription())
                .param("classDate", e.getClassDate())
                .param("startTime", e.getStartTime())
                .param("durationMinutes", e.getDurationMinutes())
                .param("timezone", e.getTimezone())
                .param("hostUserId", e.getHostUserId())
                .param("conferencingProvider", e.getConferencingProvider())
                .param("conferencingAccountId", e.getConferencingAccountId())
                .param("meetingId", e.getMeetingId())
                .param("meetingUuid", e.getMeetingUuid())
                .param("joinUrl", e.getJoinUrl())
                .param("meetingPasswordEnc", e.getMeetingPasswordEnc())
                .param("autoRecording", e.getAutoRecording())
                .param("calendarEventId", e.getCalendarEventId())
                .param("attendeeCount", e.getAttendeeCount())
                .param("inviteStatus", e.getInviteStatus())
                .param("inviteAttempts", e.getInviteAttempts())
                .param("moderatorPasswordEnc", e.getModeratorPasswordEnc())
                .param("welcomeMessage", e.getWelcomeMessage())
                .param("providerCreatedRef", e.getProviderCreatedRef())
                .param("recordingUrl", e.getRecordingUrl())
                .param("inviteeCount", e.getInviteeCount())
                .param("prepMinutes", e.getPrepMinutes())
                .param("extensionMinutes", e.getExtensionMinutes())
                .param("showInCalendar", e.getShowInCalendar())
                .param("hostJoined", e.getHostJoined())
                .param("isActive", e.getIsActive())
                .param("endedAt", e.getEndedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    public int softDelete(LiveClassEntity e, int expectedVersion) {
        return jdbc.sql(SOFT_DELETE)
                .param("id", e.getId())
                .param("deletedAt", e.getDeletedAt())
                .param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy())
                .param("expected", expectedVersion)
                .update();
    }

    /** Whether the referenced lms.cohort row exists (live). */
    public boolean existsCohortId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_COHORT_ID).param("id", id).query(Boolean.class).single());
    }

    /** Whether the referenced lms.conferencing_account row exists (live). */
    public boolean existsConferencingAccountId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(EXISTS_CONFERENCING_ACCOUNT_ID).param("id", id).query(Boolean.class)
                .single());
    }

    /** Whether live lms.content_item rows still reference the row. */
    public boolean referencedByContentItemLiveClassId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_CONTENT_ITEM_LIVE_CLASS_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.live_class_participant rows still reference the row. */
    public boolean referencedByLiveClassParticipantLiveClassId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_LIVE_CLASS_PARTICIPANT_LIVE_CLASS_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** Whether live lms.timetable_entry rows still reference the row. */
    public boolean referencedByTimetableEntryLiveClassId(UUID id) {
        return Boolean.TRUE.equals(jdbc.sql(REFERENCED_BY_TIMETABLE_ENTRY_LIVE_CLASS_ID).param("id", id)
                .query(Boolean.class).single());
    }

    /** The institution owning the row reached through cohort_id (empty: a tenant-level row, or none). */
    public Optional<UUID> institutionOf(UUID ref) {
        if (ref == null) {
            return Optional.empty();
        }
        return jdbc.sql(INSTITUTION_OF).param("ref", ref)
                .query((rs, row) -> Optional.ofNullable(rs.getObject(1, UUID.class)))
                .optional().flatMap(found -> found);
    }

    /** Rows of the caller's institutions only (api/README "Authorization"). */
    public static SqlFragment scope(DataScope scope) {
        return new SqlFragment(SCOPE, Map.of("scopeAll", scope.all(), "scopeIds", scope.institutionArray()));
    }

    private static List<Object> sortValues(SortSpec sort, LiveClassEntity e) {
        return sort.orders().stream().map(o -> SORT_VALUES.get(o.field().name()).apply(e)).toList();
    }

    private static LiveClassEntity map(ResultSet rs, int row) throws SQLException {
        return LiveClassEntity.builder()
                .id(rs.getObject("id", UUID.class))
                .tenantId(rs.getObject("tenant_id", UUID.class))
                .cohortId(rs.getObject("cohort_id", UUID.class))
                .title(rs.getString("title"))
                .description(rs.getString("description"))
                .classDate(rs.getObject("class_date", LocalDate.class))
                .startTime(rs.getObject("start_time", LocalTime.class))
                .durationMinutes(rs.getObject("duration_minutes", Integer.class))
                .timezone(rs.getString("timezone"))
                .hostUserId(rs.getObject("host_user_id", UUID.class))
                .conferencingProvider(rs.getString("conferencing_provider"))
                .conferencingAccountId(rs.getObject("conferencing_account_id", UUID.class))
                .meetingId(rs.getString("meeting_id"))
                .meetingUuid(rs.getString("meeting_uuid"))
                .joinUrl(rs.getString("join_url"))
                .meetingPasswordEnc(rs.getString("meeting_password_enc"))
                .autoRecording(rs.getString("auto_recording"))
                .calendarEventId(rs.getObject("calendar_event_id", UUID.class))
                .attendeeCount(rs.getObject("attendee_count", Integer.class))
                .inviteStatus(rs.getString("invite_status"))
                .inviteAttempts(rs.getObject("invite_attempts", Integer.class))
                .moderatorPasswordEnc(rs.getString("moderator_password_enc"))
                .welcomeMessage(rs.getString("welcome_message"))
                .providerCreatedRef(rs.getString("provider_created_ref"))
                .recordingUrl(rs.getString("recording_url"))
                .inviteeCount(rs.getObject("invitee_count", Integer.class))
                .prepMinutes(rs.getObject("prep_minutes", Integer.class))
                .extensionMinutes(rs.getObject("extension_minutes", Integer.class))
                .showInCalendar(rs.getObject("show_in_calendar", Boolean.class))
                .hostJoined(rs.getObject("host_joined", Boolean.class))
                .isActive(rs.getObject("is_active", Boolean.class))
                .endedAt(rs.getObject("ended_at", OffsetDateTime.class))
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
