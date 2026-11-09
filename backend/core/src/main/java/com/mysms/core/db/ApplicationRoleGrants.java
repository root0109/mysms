package com.mysms.core.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;

/**
 * Runs after every migration, as the schema owner: the application role may use the schema and read and write its rows,
 * nothing more — it owns nothing, so it can neither alter tables nor disable or bypass row-level security
 * (database/README "Roles"). Names are validated by {@link ModuleDatabase} before they reach this SQL.
 */
final class ApplicationRoleGrants implements Callback {

    private final String schema;
    private final String role;
    private final String relayRole;

    ApplicationRoleGrants(String schema, String role) {
        this(schema, role, null);
    }

    /**
     * @param relayRole the outbox relay's role (`mysms_relay`, BYPASSRLS): it may read, mark and prune the outbox and
     *                  prune the inbox, and nothing else
     */
    ApplicationRoleGrants(String schema, String role, String relayRole) {
        this.schema = schema;
        this.role = role;
        this.relayRole = relayRole;
    }

    @Override
    public boolean supports(Event event, Context context) {
        return event == Event.AFTER_MIGRATE;
    }

    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return true;
    }

    @Override
    public void handle(Event event, Context context) {
        Connection connection = context.getConnection();
        try (Statement statement = connection.createStatement()) {
            statement.execute("GRANT USAGE ON SCHEMA " + schema + " TO " + role);
            statement.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA " + schema + " TO " + role);
            statement.execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA " + schema + " TO " + role);
            // the migration history belongs to the owner alone
            statement.execute("REVOKE ALL ON " + schema + ".flyway_schema_history FROM " + role);
            if (relayRole != null) {
                statement.execute("GRANT USAGE ON SCHEMA " + schema + " TO " + relayRole);
                // REVOKE first: a grant of an earlier release (whole-row UPDATE) never survives a migration
                statement.execute("REVOKE ALL ON " + schema + ".outbox_event, " + schema + ".inbox_event FROM "
                        + relayRole);
                statement.execute("GRANT SELECT, DELETE ON " + schema + ".outbox_event TO " + relayRole);
                statement.execute("GRANT UPDATE (published_at, publish_error) ON " + schema + ".outbox_event TO "
                        + relayRole);
                statement.execute("GRANT SELECT, DELETE ON " + schema + ".inbox_event TO " + relayRole);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("granting the application role on schema " + schema + " failed", e);
        }
    }

    @Override
    public String getCallbackName() {
        return "application-role-grants";
    }
}
