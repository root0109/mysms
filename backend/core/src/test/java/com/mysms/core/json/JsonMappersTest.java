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
    void unknownPropertiesAreIgnoredButAMissingPrimitiveAndASingleValueForAListAreRefused() {
        assertThat(json.readValue("{\"count\":2,\"addedLater\":true}", Sample.class).count()).isEqualTo(2);
        assertThatThrownBy(() -> json.readValue("{\"tags\":[]}", Sample.class))
                .hasMessageContaining("count");
        assertThatThrownBy(() -> json.readValue("{\"count\":1,\"tags\":\"a\"}", Sample.class))
                .hasMessageContaining("tags");
        assertThat(json.writeValueAsString(Map.of())).isEqualTo("{}");
    }

    @Test
    void bootBacksOffForTheMapperCoreDeclares() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .withBean(JsonMapper.class, new CoreAutoConfiguration()::jsonMapper)
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
