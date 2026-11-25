package com.mysms.org.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.paging.FieldSpec;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.paging.ValueType;
import com.mysms.org.config.OrgDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * org's global directories, read before a tenant is known (no row-level security, no personal data): the tenant codes
 * and status kept from platform's tenant events, each institution's public face, and which schools opted in to the
 * finder (kept from iam's setting events). The school finder reads them together.
 */
@Repository
public class DirectoryRepository {

    /** A school as the finder shows it, and its name as the database orders it (the keyset value). */
    public record Listing(UUID institutionId, String tenantCode, String name, UUID cityId, String nameKey) {
    }

    /** The finder's one order, by name then id: a keyset on (lower(name), institution_id). */
    public static final Map<String, FieldSpec> SORT_FIELDS = Map.of("name",
            new FieldSpec("name", "lower(d.name)", ValueType.STRING));
    public static final String DEFAULT_ORDER = "name";
    public static final String ID_COLUMN = "d.institution_id";

    /**
     * A tenant entry changes only to a newer tenant row version, so a late or repeated event never restores an older
     * code or status.
     */
    private static final String APPLY_TENANT = """
            INSERT INTO org.tenant_directory (tenant_id, tenant_code, status, source_version, synced_at)
            VALUES (:tenant, :code, :status, :version, :now)
            ON CONFLICT (tenant_id) DO UPDATE
               SET tenant_code = EXCLUDED.tenant_code, status = EXCLUDED.status,
                   source_version = EXCLUDED.source_version, synced_at = EXCLUDED.synced_at
             WHERE org.tenant_directory.source_version < EXCLUDED.source_version
            """;
    /**
     * An opt-in entry changes only to a newer setting-value version, so a late or repeated event never restores an
     * older choice.
     */
    private static final String APPLY_LISTING = """
            INSERT INTO org.finder_listing (setting_value_id, tenant_id, institution_id, is_listed, is_deleted,
                                            source_version, synced_at)
            VALUES (:id, :tenant, :institution, :listed, :deleted, :version, :now)
            ON CONFLICT (setting_value_id) DO UPDATE
               SET institution_id = EXCLUDED.institution_id, is_listed = EXCLUDED.is_listed,
                   is_deleted = EXCLUDED.is_deleted, source_version = EXCLUDED.source_version,
                   synced_at = EXCLUDED.synced_at
             WHERE org.finder_listing.source_version < EXCLUDED.source_version
               AND org.finder_listing.tenant_id = EXCLUDED.tenant_id
            """;
    /**
     * Listed schools of active tenants that opted in, whose name holds the words or whose code is them; the caller
     * appends the keyset, the order and the limit.
     */
    private static final String FIND = """
            SELECT d.institution_id, t.tenant_code, d.name, d.city_id, lower(d.name) AS name_key
              FROM org.institution_directory d
              JOIN org.tenant_directory t ON t.tenant_id = d.tenant_id AND t.status = 'active'
             WHERE d.is_listed
               AND EXISTS (SELECT 1 FROM org.finder_listing l
                            WHERE l.institution_id = d.institution_id AND l.tenant_id = d.tenant_id
                              AND l.is_listed AND NOT l.is_deleted)
               AND (lower(d.name) LIKE :pattern ESCAPE '\\' OR lower(d.code) = :code)
               AND (CAST(:countryId AS uuid) IS NULL OR d.country_id = :countryId)
            """;

    private final JdbcClient jdbc;

    public DirectoryRepository(@Qualifier(OrgDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Applies one tenant row version; whether the entry changed (false for an older or repeated version). */
    public boolean applyTenant(UUID tenant, String code, String status, int version, OffsetDateTime now) {
        return jdbc.sql(APPLY_TENANT).param("tenant", tenant).param("code", code).param("status", status)
                .param("version", version).param("now", now).update() > 0;
    }

    /** Applies one setting-value version; whether the entry changed (false for an older or repeated version). */
    public boolean applyListing(UUID settingValueId, UUID tenant, UUID institution, boolean listed, boolean deleted,
            int version, OffsetDateTime now) {
        return jdbc.sql(APPLY_LISTING).param("id", settingValueId).param("tenant", tenant)
                .param("institution", institution).param("listed", listed).param("deleted", deleted)
                .param("version", version).param("now", now).update() > 0;
    }

    /** One page of the schools found; the text is matched lower-cased, its wildcards escaped. */
    public Page<Listing> find(String text, UUID countryId, PageRequest page) {
        String lowered = text.toLowerCase(Locale.ROOT);
        String escaped = lowered.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        SqlFragment keyset = page.keysetPredicate();
        String sql = FIND + " AND (" + keyset.sql() + ") ORDER BY " + page.orderByClause() + page.limitClause();
        List<Listing> rows = jdbc.sql(sql).param("pattern", "%" + escaped + "%").param("code", lowered)
                .param("countryId", countryId).params(keyset.params()).params(page.limitParams())
                .query((rs, row) -> new Listing(rs.getObject("institution_id", UUID.class),
                        rs.getString("tenant_code"), rs.getString("name"), rs.getObject("city_id", UUID.class),
                        rs.getString("name_key")))
                .list();
        return page.toPage(rows, listing -> List.of(listing.nameKey()), Listing::institutionId);
    }
}
