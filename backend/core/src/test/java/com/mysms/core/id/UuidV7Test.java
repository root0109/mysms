package com.mysms.core.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidV7Test {

    @Test
    void encodesVersionVariantAndTime() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-06T10:15:30.123Z"), ZoneOffset.UTC);
        UUID id = UuidV7.next(clock);
        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
        assertThat(UuidV7.timestampOf(id)).isEqualTo(clock.millis());
    }

    @Test
    void sortsByCreationTimeAndIsUnique() {
        UUID earlier = UuidV7.next(Clock.fixed(Instant.ofEpochMilli(1_000), ZoneOffset.UTC));
        UUID later = UuidV7.next(Clock.fixed(Instant.ofEpochMilli(2_000), ZoneOffset.UTC));
        assertThat(earlier.toString()).isLessThan(later.toString());
        Set<UUID> ids = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            ids.add(UuidV7.next());
        }
        assertThat(ids).hasSize(10_000);
    }

    @Test
    void rejectsOtherVersions() {
        assertThatThrownBy(() -> UuidV7.timestampOf(UUID.randomUUID())).isInstanceOf(IllegalArgumentException.class);
    }
}
