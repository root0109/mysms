package com.mysms.lms.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.lms.config.LmsDatabaseConfig;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Reads the tenant's lms.settings row for the learner-facing operations ({@link SettingsView}). */
@Repository
public class SettingsViewRepository {

    private static final String FIND = """
            SELECT r.id, r.allow_guest_access, r.disable_signup, r.show_courses, r.show_live_class, r.text_direction,
                   r.contact_us_email, r.contact_us_url, r.meta_description, r.meta_keywords,
                   r.custom_signup_content, r.enforce_video_completion, r.enforce_quiz_completion,
                   r.enforce_quiz_passing
              FROM lms.settings r
             WHERE r.tenant_id = :tenantId
               AND r.deleted_at IS NULL
             ORDER BY r.created_at, r.id
             LIMIT 1
            """;

    private final JdbcClient jdbc;

    public SettingsViewRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** The tenant's settings; the column defaults when it has saved none. */
    public SettingsView find() {
        Optional<SettingsView> found = jdbc.sql(FIND).query((rs, row) -> new SettingsView(
                rs.getObject("id", UUID.class), rs.getBoolean("allow_guest_access"), rs.getBoolean("disable_signup"),
                rs.getBoolean("show_courses"), rs.getBoolean("show_live_class"), rs.getString("text_direction"),
                rs.getString("contact_us_email"), rs.getString("contact_us_url"), rs.getString("meta_description"),
                rs.getString("meta_keywords"), rs.getString("custom_signup_content"),
                rs.getBoolean("enforce_video_completion"), rs.getBoolean("enforce_quiz_completion"),
                rs.getBoolean("enforce_quiz_passing"))).optional();
        return found.orElseGet(SettingsView::defaults);
    }
}
