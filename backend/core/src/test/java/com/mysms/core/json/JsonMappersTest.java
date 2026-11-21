package com.mysms.core.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.config.CoreAutoConfiguration;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;

/** The wire format the hand-built mapper fixes, and that it is the mapper of the application. */
class JsonMappersTest {

    private final JsonMapper json = JsonMappers.create();

    /** A contract-like class: a date, an instant, a duration, a list and a primitive. */
    public record Sample(LocalDate day, OffsetDateTime at, Duration took, List<String> tags, int count) {
    }

    @Test
    void datesAndTimesAreIsoStringsAndInstantsAreHeldInUtc() {
        Sample sample = new Sample(LocalDate.of(2026, 3, 2), OffsetDateTime.of(2026, 3, 2, 8, 30, 0, 0,
                ZoneOffset.ofHoursMinutes(5, 30)), Duration.ofMinutes(45), List.of("a"), 1);
        String written = json.writeValueAsString(sample);
        assertThat(written).isEqualTo("{\"day\":\"2026-03-02\",\"at\":\"2026-03-02T08:30:00+05:30\","
                + "\"took\":\"PT45M\",\"tags\":[\"a\"],\"count\":1}");
        assertThat(json.readValue(written, Sample.class).at())
                .isEqualTo(OffsetDateTime.of(2026, 3, 2, 3, 0, 0, 0, ZoneOffset.UTC));
    }

    @Test
    void unknownPropertiesAreIgnoredButANullPrimitiveAndASingleValueForAListAreRefused() {
        assertThat(json.readValue("{\"count\":2,\"addedLater\":true}", Sample.class).count()).isEqualTo(2);
        assertThatThrownBy(() -> json.readValue("{\"count\":null}", Sample.class))
                .hasMessageContaining("count");
        // since Jackson 3.2 (databind#5734) an absent primitive takes its default; contract DTOs carry no primitives
        assertThat(json.readValue("{\"tags\":[]}", Sample.class).count()).isZero();
        assertThatThrownBy(() -> json.readValue("{\"count\":1,\"tags\":\"a\"}", Sample.class))
                .hasMessageContaining("tags");
        assertThat(json.writeValueAsString(Map.of())).isEqualTo("{}");
    }

    /** A contract-like class with an enum, a UUID and a boxed boolean. */
    public record Flags(Status status, UUID id, Boolean active) {
    }

    /** A contract enum. */
    public enum Status { ACTIVE, CLOSED }

    @Test
    void enumsAreReadByNameInAnyCaseButNeverByNumber() {
        assertThat(json.readValue("{\"status\":\"closed\"}", Flags.class).status()).isEqualTo(Status.CLOSED);
        assertThat(json.readValue("{\"status\":\"ACTIVE\"}", Flags.class).status()).isEqualTo(Status.ACTIVE);
        assertThatThrownBy(() -> json.readValue("{\"status\":1}", Flags.class)).hasMessageContaining("status");
    }

    @Test
    void uuidsAreReadOnlyInTheirCanonicalForm() {
        UUID id = UUID.fromString("0190b0a0-0000-7000-8000-00000000000a");
        assertThat(json.readValue("{\"id\":\"0190B0A0-0000-7000-8000-00000000000A\"}", Flags.class).id())
                .isEqualTo(id);
        assertThatThrownBy(() -> json.readValue("{\"id\":\"1-2-3-4-5\"}", Flags.class)).hasMessageContaining("id");
        assertThatThrownBy(() -> json.readValue("{\"id\":\"0190b0a0-0000-7000-8000-00000000000a-\"}", Flags.class))
                .hasMessageContaining("id");
        assertThatThrownBy(() -> json.readValue("{\"id\":42}", Flags.class)).hasMessageContaining("id");
        assertThatThrownBy(() -> json.readValue("{\"id\":\" 0190b0a0-0000-7000-8000-00000000000a\"}", Flags.class))
                .as("no surrounding spaces").hasMessageContaining("id");
    }

    @Test
    void booleansAreReadOnlyAsTrueOrFalse() {
        assertThat(json.readValue("{\"active\":true}", Flags.class).active()).isTrue();
        assertThat(json.readValue("{\"active\":\" False \"}", Flags.class).active()).isFalse();
        assertThat(json.readValue("{\"active\":null}", Flags.class).active()).isNull();
        assertThatThrownBy(() -> json.readValue("{\"active\":\"yes\"}", Flags.class)).hasMessageContaining("active");
        assertThatThrownBy(() -> json.readValue("{\"active\":1}", Flags.class)).hasMessageContaining("active");
    }

    @Test
    void bootBacksOffForTheMapperCoreDeclares() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .withBean(JsonMapper.class, new CoreAutoConfiguration()::jacksonJsonMapper)
                .run(context -> {
                    assertThat(context).hasSingleBean(JsonMapper.class);
                    JsonMapper used = context.getBean(JsonMapper.class);
                    assertThat(used.writeValueAsString(new Sample(LocalDate.of(2026, 1, 1), null, null, List.of(), 0)))
                            .startsWith("{\"day\":\"2026-01-01\"");
                    assertThat(used.isEnabled(tools.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES))
                            .isTrue();
                });
    }
}
