package com.mysms.identity.people.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.identity.people.config.PeopleDatabaseConfig;
import com.mysms.identity.people.domain.GreetingEntity;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Plain-JDBC access to people.greeting: the greetings asked for, counted per sender and day. */
@Repository
public class GreetingRepository {

    private static final String INSERT = """
            INSERT INTO people.greeting (id, tenant_id, person_id, greeted_on, channel, created_at, created_by,
                                         updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :personId, :greetedOn, :channel, :createdAt, :createdBy, :updatedAt, :updatedBy,
                    :rowVersion)
            ON CONFLICT (tenant_id, person_id, greeted_on, channel) WHERE deleted_at IS NULL DO NOTHING
            """;
    private static final String SENT_BY = """
            SELECT count(*) FROM people.greeting g
             WHERE g.tenant_id = :tenantId AND g.deleted_at IS NULL AND g.created_by = :sender AND g.greeted_on = :day
            """;
    private static final String GREETED = """
            SELECT g.person_id FROM people.greeting g
             WHERE g.tenant_id = :tenantId AND g.deleted_at IS NULL AND g.greeted_on = :day AND g.channel = :channel
               AND g.person_id = ANY(:personIds)
            """;

    private static final String LIVE_PERSONS = """
            SELECT p.id FROM people.person p
             WHERE p.tenant_id = :tenantId AND p.deleted_at IS NULL AND p.id = ANY(:personIds)
            """;

    /** A greeting comms could not deliver is taken back: it no longer counts for its day or its sender. */
    private static final String WITHDRAW = """
            UPDATE people.greeting
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public GreetingRepository(@Qualifier(PeopleDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Takes the greeting back; whether it was still live. */
    public boolean withdraw(UUID id, OffsetDateTime now) {
        return jdbc.sql(WITHDRAW).param("id", id).param("now", now).update() == 1;
    }

    /** Records a greeting; false when the person was already greeted that day on that channel. */
    public boolean insert(GreetingEntity e) {
        return jdbc.sql(INSERT).param("id", e.getId()).param("tenantId", e.getTenantId())
                .param("personId", e.getPersonId()).param("greetedOn", e.getGreetedOn())
                .param("channel", e.getChannel()).param("createdAt", e.getCreatedAt())
                .param("createdBy", e.getCreatedBy()).param("updatedAt", e.getUpdatedAt())
                .param("updatedBy", e.getUpdatedBy()).param("rowVersion", e.getRowVersion()).update() == 1;
    }

    /** How many greetings the sender asked for on a day. */
    public long sentBy(UUID sender, LocalDate day) {
        return jdbc.sql(SENT_BY).param("sender", sender).param("day", day).query(Long.class).single();
    }

    /** The persons among the ids that exist and are not deleted. */
    public Set<UUID> livePersons(List<UUID> personIds) {
        return new HashSet<>(jdbc.sql(LIVE_PERSONS).param("personIds", personIds.toArray(UUID[]::new))
                .query(UUID.class).list());
    }

    /** The persons among the ids already greeted that day on that channel. */
    public Set<UUID> greeted(List<UUID> personIds, LocalDate day, String channel) {
        return new HashSet<>(jdbc.sql(GREETED).param("personIds", personIds.toArray(UUID[]::new)).param("day", day)
                .param("channel", channel).query(UUID.class).list());
    }
}
