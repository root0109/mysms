package com.mysms.privacy.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.RejectedEventException;
import com.mysms.privacy.repository.GuardianLinkRepository;
import com.mysms.privacy.repository.GuardianLinkRepository.Link;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** The guardian link sync's reading of sis.student_guardian events: the link as carried, a deletion made inactive. */
class GuardianLinkSyncTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-21T10:00:00Z"), ZoneOffset.UTC);

    private final GuardianLinkRepository links = mock(GuardianLinkRepository.class);
    private final GuardianLinkSync sync = new GuardianLinkSync(links, JsonMapper.builder().build(), CLOCK);

    private static EventEnvelope event(String verb, UUID id, String data) {
        return new EventEnvelope(UUID.randomUUID(), "sis", "sis.student_guardian." + verb,
                Optional.of(UUID.randomUUID()), "sis.student_guardian", id, Instant.now(), data, Optional.empty());
    }

    private static String link(UUID id, UUID student, UUID guardian, String status, int version) {
        return "{\"id\":\"" + id + "\",\"studentPersonId\":\"" + student + "\",\"guardianPersonId\":\"" + guardian
                + "\",\"guardianUserId\":null,\"isLegalGuardian\":true,\"hasCustody\":false,\"status\":\"" + status
                + "\",\"rowVersion\":" + version + "}";
    }

    @Test
    void keepsTheLinkAsItsEventCarriesIt() {
        UUID id = UUID.randomUUID();
        UUID student = UUID.randomUUID();
        UUID guardian = UUID.randomUUID();
        when(links.apply(any(), any())).thenReturn(true);
        assertThat(sync.apply(event("created", id, link(id, student, guardian, "active", 1)))).isTrue();
        verify(links).apply(new Link(id, student, guardian, null, true, false, "active", 1),
                CLOCK.instant().atOffset(ZoneOffset.UTC));
    }

    @Test
    void aDeletedLinkIsInactive() {
        UUID id = UUID.randomUUID();
        UUID student = UUID.randomUUID();
        UUID guardian = UUID.randomUUID();
        sync.apply(event("deleted", id, link(id, student, guardian, "active", 3)));
        verify(links).apply(new Link(id, student, guardian, null, true, false, "inactive", 3),
                CLOCK.instant().atOffset(ZoneOffset.UTC));
    }

    @Test
    void anAnnouncementOfTheIdAloneChangesNothingAndAnIncompleteLinkIsRefused() {
        UUID id = UUID.randomUUID();
        assertThat(sync.apply(event("updated", id, "{\"id\":\"" + id + "\"}"))).isFalse();
        assertThatThrownBy(() -> sync.apply(event("updated", id, "{\"id\":\"" + id + "\",\"rowVersion\":2}")))
                .isInstanceOf(RejectedEventException.class);
        assertThatThrownBy(() -> sync.apply(event("updated", id,
                link(id, UUID.randomUUID(), UUID.randomUUID(), "archived", 2)))).isInstanceOf(RejectedEventException.class);
        verifyNoInteractions(links);
    }
}
