package com.mysms.core.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PatchesTest {

    @Test
    void anAbsentFieldIsUntouchedANullOneClearedAndAValueSet() {
        List<String> name = new ArrayList<>(List.of("India"));
        List<String> code = new ArrayList<>(List.of("IN"));
        List<String> note = new ArrayList<>(List.of("kept"));
        Optional<String> cleared = Optional.empty();
        Optional<String> absent = null;
        Patches patch = new Patches();

        patch.apply("name", Optional.of("Bharat"), name::add);
        patch.apply("code", cleared, code::add);
        patch.apply("note", absent, note::add);

        assertThat(name).as("a value is set").containsExactly("India", "Bharat");
        assertThat(code).as("JSON null clears: the setter gets null").containsExactly("IN", null);
        assertThat(note).as("an absent property never reaches the setter").containsExactly("kept");
        assertThat(patch.applied()).as("the audit record names what was applied, in call order")
                .containsExactly("name", "code");
    }

    @Test
    void theValueOfAFieldIsNullWhenAbsentOrCleared() {
        Optional<String> absent = null;
        assertThat(Patches.value(Optional.of("10:30"))).isEqualTo("10:30");
        assertThat(Patches.value(Optional.<String>empty())).isNull();
        assertThat(Patches.value(absent)).isNull();
    }
}
