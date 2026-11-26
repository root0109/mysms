// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.util.Objects;
import java.util.Optional;
import org.springframework.lang.Nullable;

/**
 * The tenant's own setting of a retention policy: its period (never below the legal floor) and its action.
 */

public class RetentionPolicyUpdate {

  /**
   * What happens to data past the period.
   */
  public enum ActionEnum {
    ANONYMISE("anonymise"),
    
    ARCHIVE("archive"),
    
    DELETE("delete");

    private final String value;

    ActionEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static ActionEnum fromValue(String value) {
      for (ActionEnum b : ActionEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Min(value = 1) Integer> periodDays;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull ActionEnum> action;

  public RetentionPolicyUpdate() {
    super();
  }

  public RetentionPolicyUpdate periodDays(@Nullable Optional<Integer> periodDays) {
    this.periodDays = periodDays;
    return this;
  }

  /**
   * How many days the data is kept after the trigger; at least the policy's minimumPeriodDays.
   * @return periodDays
   */
  
  @JsonProperty("periodDays")
  public @Nullable Optional<Integer> getPeriodDays() {
    return periodDays;
  }

  @JsonProperty("periodDays")
  public void setPeriodDays(@Nullable Optional<Integer> periodDays) {
    this.periodDays = periodDays;
  }

  public RetentionPolicyUpdate action(@Nullable Optional<ActionEnum> action) {
    this.action = action;
    return this;
  }

  /**
   * What happens to data past the period.
   * @return action
   */
  
  @JsonProperty("action")
  public @Nullable Optional<ActionEnum> getAction() {
    return action;
  }

  @JsonProperty("action")
  public void setAction(@Nullable Optional<ActionEnum> action) {
    this.action = action;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RetentionPolicyUpdate other = (RetentionPolicyUpdate) o;
    return Objects.equals(this.periodDays, other.periodDays) &&
        Objects.equals(this.action, other.action);
  }

  @Override
  public int hashCode() {
    return Objects.hash(periodDays, action);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RetentionPolicyUpdate {\n");
    sb.append("    periodDays: ").append(toIndentedString(periodDays)).append("\n");
    sb.append("    action: ").append(toIndentedString(action)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    return o == null ? "null" : o.toString().replace("\n", "\n    ");
  }
}
