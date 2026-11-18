package com.mysms.lms.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.lms.config.LmsDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Plain-JDBC access of the LTI workflows: a tool with its settings (plain and encrypted), a resource link, the
 * placement it is launched from, and the tool's activation.
 */
@Repository
public class LtiRepository {

    public record Tool(UUID id, String title, boolean included, boolean paid, int rowVersion) {
    }

    /** A setting's plain value, or its encrypted secret. */
    public record Setting(String value, String secretEnc) {
    }

    public record Link(UUID id, UUID resourceId, UUID toolId, String linkRef, String title, UUID institutionId) {
    }

    /** A content item placing a tool resource in a cohort. */
    public record Placement(UUID id, UUID cohortId, UUID institutionId, String title) {
    }

    private static final String TOOL = """
            SELECT id, title, is_included, is_paid, row_version FROM lms.external_tool
             WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL
            """;
    private static final String TOOL_FOR_UPDATE = TOOL + " FOR UPDATE";
    private static final String SETTINGS = """
            SELECT setting_key, setting_value, secret_value_enc FROM lms.external_tool_setting
             WHERE tenant_id = :tenantId AND external_tool_id = :toolId AND deleted_at IS NULL
            """;
    private static final String TOOL_INSTITUTIONS = """
            SELECT institution_id FROM lms.external_tool_institution
             WHERE tenant_id = :tenantId AND external_tool_id = :toolId AND deleted_at IS NULL
            """;
    private static final String LINK = """
            SELECT l.id, l.external_tool_resource_id, l.external_tool_id, l.link_ref, l.link_title, r.institution_id
              FROM lms.external_tool_link l
              JOIN lms.external_tool_resource r ON r.id = l.external_tool_resource_id AND r.tenant_id = :tenantId
                   AND r.deleted_at IS NULL
             WHERE l.tenant_id = :tenantId AND l.id = :id AND l.external_tool_resource_id = :resourceId
               AND l.deleted_at IS NULL
            """;
    private static final String PLACEMENT = """
            SELECT ci.id, COALESCE(ci.cohort_id, ch.cohort_id) AS cohort_id, co.institution_id, ci.title
              FROM lms.content_item ci
              LEFT JOIN lms.chapter ch ON ch.id = ci.chapter_id AND ch.tenant_id = :tenantId
              LEFT JOIN lms.cohort co ON co.id = COALESCE(ci.cohort_id, ch.cohort_id) AND co.tenant_id = :tenantId
             WHERE ci.tenant_id = :tenantId AND ci.id = :id AND ci.external_tool_resource_id = :resourceId
               AND ci.deleted_at IS NULL
            """;
    private static final String UPSERT_SETTING = """
            INSERT INTO lms.external_tool_setting (id, tenant_id, external_tool_id, setting_key, setting_value,
                   created_at, created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :toolId, :key, :value, :now, :actor, :now, :actor, 1)
            ON CONFLICT (external_tool_id, setting_key) WHERE deleted_at IS NULL
            DO UPDATE SET setting_value = EXCLUDED.setting_value, updated_at = EXCLUDED.updated_at,
                          updated_by = EXCLUDED.updated_by, row_version = lms.external_tool_setting.row_version + 1
            """;
    private static final String INCLUDE = """
            UPDATE lms.external_tool SET is_included = true, updated_at = :now, updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public LtiRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<Tool> tool(UUID id) {
        return jdbc.sql(TOOL).param("id", id).query((rs, n) -> new Tool(rs.getObject("id", UUID.class),
                rs.getString("title"), rs.getBoolean("is_included"), rs.getBoolean("is_paid"),
                rs.getInt("row_version"))).optional();
    }

    public Optional<Tool> lockTool(UUID id) {
        return jdbc.sql(TOOL_FOR_UPDATE).param("id", id).query((rs, n) -> new Tool(rs.getObject("id", UUID.class),
                rs.getString("title"), rs.getBoolean("is_included"), rs.getBoolean("is_paid"),
                rs.getInt("row_version"))).optional();
    }

    public Map<String, Setting> settings(UUID toolId) {
        Map<String, Setting> settings = new LinkedHashMap<>();
        jdbc.sql(SETTINGS).param("toolId", toolId).query(rs -> {
            settings.put(rs.getString("setting_key"), new Setting(rs.getString("setting_value"),
                    rs.getString("secret_value_enc")));
        });
        return settings;
    }

    public List<UUID> toolInstitutions(UUID toolId) {
        return jdbc.sql(TOOL_INSTITUTIONS).param("toolId", toolId).query(UUID.class).list();
    }

    public Optional<Link> link(UUID resourceId, UUID id) {
        return jdbc.sql(LINK).param("resourceId", resourceId).param("id", id).query((rs, n) -> new Link(
                rs.getObject("id", UUID.class), rs.getObject("external_tool_resource_id", UUID.class),
                rs.getObject("external_tool_id", UUID.class), rs.getString("link_ref"), rs.getString("link_title"),
                rs.getObject("institution_id", UUID.class))).optional();
    }

    public Optional<Placement> placement(UUID resourceId, UUID contentItemId) {
        return jdbc.sql(PLACEMENT).param("resourceId", resourceId).param("id", contentItemId).query((rs, n) ->
                new Placement(rs.getObject("id", UUID.class), rs.getObject("cohort_id", UUID.class),
                        rs.getObject("institution_id", UUID.class), rs.getString("title"))).optional();
    }

    public void putSetting(UUID id, UUID toolId, String key, String value, OffsetDateTime now, UUID actor) {
        jdbc.sql(UPSERT_SETTING).param("id", id).param("toolId", toolId).param("key", key).param("value", value)
                .param("now", now).param("actor", actor).update();
    }

    public int include(UUID id, int expected, OffsetDateTime now, UUID actor) {
        return jdbc.sql(INCLUDE).param("id", id).param("expected", expected).param("now", now).param("actor", actor)
                .update();
    }
}
