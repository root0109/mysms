package com.mysms.people.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.people.config.PeopleDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * people.greeting_template: which comms message templates a greeting may name, kept from comms's template events. A
 * row changes only to a newer source row version, so a late or repeated event changes nothing.
 */
@Repository
public class GreetingTemplateRepository {

    /** A template comms uses: its institution, none for a tenant-wide one. */
    public record Usable(Optional<UUID> institutionId) {
    }

    private static final String APPLY = """
            INSERT INTO people.greeting_template (tenant_id, template_id, institution_id, is_active, is_deleted,
                                                  source_version, synced_at)
            VALUES (:tenantId, :templateId, :institutionId, :active, :deleted, :version, :now)
            ON CONFLICT (tenant_id, template_id) DO UPDATE
               SET institution_id = EXCLUDED.institution_id, is_active = EXCLUDED.is_active,
                   is_deleted = EXCLUDED.is_deleted, source_version = EXCLUDED.source_version,
                   synced_at = EXCLUDED.synced_at
             WHERE people.greeting_template.source_version < EXCLUDED.source_version
            """;
    private static final String USABLE = """
            SELECT t.institution_id
              FROM people.greeting_template t
             WHERE t.tenant_id = :tenantId
               AND t.template_id = :templateId
               AND t.is_active
               AND NOT t.is_deleted
            """;

    private final JdbcClient jdbc;

    public GreetingTemplateRepository(@Qualifier(PeopleDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Applies one template event; whether the stored entry changed (false for an older or repeated one). */
    public boolean apply(UUID templateId, UUID institutionId, boolean active, boolean deleted, int version,
            OffsetDateTime now) {
        return jdbc.sql(APPLY).param("templateId", templateId).param("institutionId", institutionId)
                .param("active", active).param("deleted", deleted).param("version", version).param("now", now)
                .update() == 1;
    }

    /** The template, while comms uses it. */
    public Optional<Usable> usable(UUID templateId) {
        return jdbc.sql(USABLE).param("templateId", templateId)
                .query((rs, row) -> new Usable(Optional.ofNullable(rs.getObject(1, UUID.class)))).optional();
    }
}
