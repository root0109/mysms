package com.mysms.conformance;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.conformance.rules.ApiLayerRule;
import org.junit.jupiter.api.Test;

/** How D2 reads a PATCH body (§7.2) and a container's {@code @Valid} (§7.1). */
class ApiLayerRuleTest {

    @Test
    void everyFieldOfAPatchBodyIsAnUninitialisedOptional() {
        assertThat(ApiLayerRule.notOptionalUpdateFields("""
                public class FeeHeadUpdate {
                  private @Nullable Optional<@NotNull @Size(max = 150) String> name;
                  private Optional<String> note;
                  private @Nullable Optional<@NotNull @Size(max = 20) List<String>> categories;
                  private @Nullable String code;
                  private Optional<String> kept = Optional.empty();
                  private static final long serialVersionUID = 1L;
                  public enum StatusEnum {
                    ACTIVE("active");
                    private final String value;
                  }
                }
                """)).containsExactly("code", "kept");
    }

    @Test
    void validBelongsOnTheTypeArgument() {
        assertThat(ApiLayerRule.validOnContainer("@Valid @RequestParam(value = \"sort\") @Nullable List<String> sort"))
                .isTrue();
        assertThat(ApiLayerRule.validOnContainer("@NotNull @Valid @Size(min = 1, max = 20) \n"
                + "  @JsonProperty(\"lines\")\n  public List<@Valid Line> getLines() {")).isTrue();
        assertThat(ApiLayerRule.validOnContainer("public List<@Valid Line> getLines() {")).isFalse();
        assertThat(ApiLayerRule.validOnContainer("@Valid @RequestBody FeeHeadUpdate feeHeadUpdate,")).isFalse();
        assertThat(ApiLayerRule.validOnContainer("@Valid @RequestBody Object body,")).as("may be a container").isTrue();
        assertThat(ApiLayerRule.validOnContainer("private Optional<List<@Valid Line>> lines;")).isFalse();
    }
}
