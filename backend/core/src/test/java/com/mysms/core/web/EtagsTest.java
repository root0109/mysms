package com.mysms.core.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.PreconditionRequiredException;
import org.junit.jupiter.api.Test;

class EtagsTest {

    @Test
    void roundTripsTheRowVersion() {
        assertThat(Etags.of(7)).isEqualTo("\"7\"");
        assertThat(Etags.expectedVersion("\"7\"")).isEqualTo(7);
        assertThat(Etags.expectedVersion(" \"12\" ")).isEqualTo(12);
    }

    @Test
    void requiresAnExplicitTag() {
        assertThatThrownBy(() -> Etags.expectedVersion(null)).isInstanceOf(PreconditionRequiredException.class);
        assertThatThrownBy(() -> Etags.expectedVersion(" ")).isInstanceOf(PreconditionRequiredException.class);
        assertThatThrownBy(() -> Etags.expectedVersion("*")).isInstanceOf(PreconditionRequiredException.class);
    }

    @Test
    void bodyTagsAreOptionalAndMayBeBare() {
        assertThat(Etags.bodyVersion(null)).isEmpty();
        assertThat(Etags.bodyVersion(" ")).isEmpty();
        assertThat(Etags.bodyVersion("\"4\"").getAsInt()).isEqualTo(4);
        assertThat(Etags.bodyVersion(" 5 ").getAsInt()).isEqualTo(5);
        assertThatThrownBy(() -> Etags.bodyVersion("W/\"4\"")).isInstanceOf(PreconditionFailedException.class);
        assertThatThrownBy(() -> Etags.bodyVersion("four")).isInstanceOf(PreconditionFailedException.class);
    }

    @Test
    void weakOrMalformedTagsNeverMatch() {
        assertThatThrownBy(() -> Etags.expectedVersion("W/\"7\"")).isInstanceOf(PreconditionFailedException.class);
        assertThatThrownBy(() -> Etags.expectedVersion("7")).isInstanceOf(PreconditionFailedException.class);
        assertThatThrownBy(() -> Etags.expectedVersion("\"7\", \"8\"")).isInstanceOf(PreconditionFailedException.class);
        assertThatThrownBy(() -> Etags.expectedVersion("\"9999999999\"")).isInstanceOf(PreconditionFailedException.class);
    }
}
