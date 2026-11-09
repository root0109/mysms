package com.mysms.core.entity;

import com.mysms.core.id.UuidV7;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * The audit set every MySMS table carries, column for column (JAVA-CODING-RULES §5): id (UUIDv7, generated here),
 * created/updated at+by, soft-delete marker and the optimistic-lock version. The lifecycle hooks are called by the
 * owning business service immediately before the write — never by a framework.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class BaseEntity {

    @EqualsAndHashCode.Include
    private UUID id;
    private OffsetDateTime createdAt;
    private UUID createdBy;
    private OffsetDateTime updatedAt;
    private UUID updatedBy;
    private OffsetDateTime deletedAt;
    private int rowVersion;

    /** New row: UUIDv7 id, both audit stamps, version 1, live. */
    public void onCreatingData(Optional<UUID> actor, Clock clock) {
        OffsetDateTime now = now(clock);
        this.id = UuidV7.next(clock);
        this.createdAt = now;
        this.createdBy = actor.orElse(null);
        this.updatedAt = now;
        this.updatedBy = actor.orElse(null);
        this.deletedAt = null;
        this.rowVersion = 1;
    }

    /** Update: stamps who and when; the repository bumps row_version under the If-Match guard. */
    public void onUpdatingData(Optional<UUID> actor, Clock clock) {
        this.updatedAt = now(clock);
        this.updatedBy = actor.orElse(null);
    }

    /** Soft delete: marks the row deleted; it disappears from every read. */
    public void onDeletingData(Optional<UUID> actor, Clock clock) {
        OffsetDateTime now = now(clock);
        this.deletedAt = now;
        this.updatedAt = now;
        this.updatedBy = actor.orElse(null);
    }

    /** PostgreSQL keeps microseconds; truncating here makes the returned entity equal what a re-read returns. */
    private static OffsetDateTime now(Clock clock) {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
