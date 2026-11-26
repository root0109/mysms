// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of ReleaseRuleCondition that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ReleaseRuleConditionUpdate {

  /**
   * Condition the release engine evaluates.
   */
  public enum ConditionTypeEnum {
    COMPLETED("completed"),
    
    NOT_COMPLETED("not_completed"),
    
    SUBMITTED("submitted"),
    
    NOT_SUBMITTED("not_submitted"),
    
    SCORE_AT_LEAST("score_at_least"),
    
    SCORE_BELOW("score_below"),
    
    VIEWED("viewed"),
    
    DATE_AFTER("date_after"),
    
    DATE_BEFORE("date_before"),
    
    ATTEMPTS_AT_LEAST("attempts_at_least");

    private @Nullable final String value;

    ConditionTypeEnum(String value) {
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
    public static ConditionTypeEnum fromValue(String value) {
      for (ConditionTypeEnum b : ConditionTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull ConditionTypeEnum> conditionType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 45) String> operator;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 45) String> value;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 45) String> value2;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> sortOrder;

  public ReleaseRuleConditionUpdate conditionType(@Nullable Optional<ConditionTypeEnum> conditionType) {
    this.conditionType = conditionType;
    return this;
  }

  /**
   * Condition the release engine evaluates.
   * @return conditionType
   */
  
  @JsonProperty("conditionType")
  public @Nullable Optional<ConditionTypeEnum> getConditionType() {
    return conditionType;
  }

  @JsonProperty("conditionType")
  public void setConditionType(@Nullable Optional<ConditionTypeEnum> conditionType) {
    this.conditionType = conditionType;
  }

  public ReleaseRuleConditionUpdate operator(@Nullable Optional<String> operator) {
    this.operator = operator;
    return this;
  }

  /**
   * Comparison operator.
   * @return operator
   */
  
  @JsonProperty("operator")
  public @Nullable Optional<String> getOperator() {
    return operator;
  }

  @JsonProperty("operator")
  public void setOperator(@Nullable Optional<String> operator) {
    this.operator = operator;
  }

  public ReleaseRuleConditionUpdate value(@Nullable Optional<String> value) {
    this.value = value;
    return this;
  }

  /**
   * Value.
   * @return value
   */
  
  @JsonProperty("value")
  public @Nullable Optional<String> getValue() {
    return value;
  }

  @JsonProperty("value")
  public void setValue(@Nullable Optional<String> value) {
    this.value = value;
  }

  public ReleaseRuleConditionUpdate value2(@Nullable Optional<String> value2) {
    this.value2 = value2;
    return this;
  }

  /**
   * Second value (ranges).
   * @return value2
   */
  
  @JsonProperty("value2")
  public @Nullable Optional<String> getValue2() {
    return value2;
  }

  @JsonProperty("value2")
  public void setValue2(@Nullable Optional<String> value2) {
    this.value2 = value2;
  }

  public ReleaseRuleConditionUpdate sortOrder(@Nullable Optional<Integer> sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Order.
   * @return sortOrder
   */
  
  @JsonProperty("sortOrder")
  public @Nullable Optional<Integer> getSortOrder() {
    return sortOrder;
  }

  @JsonProperty("sortOrder")
  public void setSortOrder(@Nullable Optional<Integer> sortOrder) {
    this.sortOrder = sortOrder;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReleaseRuleConditionUpdate releaseRuleConditionUpdate = (ReleaseRuleConditionUpdate) o;
    return Objects.equals(this.conditionType, releaseRuleConditionUpdate.conditionType) &&
        Objects.equals(this.operator, releaseRuleConditionUpdate.operator) &&
        Objects.equals(this.value, releaseRuleConditionUpdate.value) &&
        Objects.equals(this.value2, releaseRuleConditionUpdate.value2) &&
        Objects.equals(this.sortOrder, releaseRuleConditionUpdate.sortOrder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(conditionType, operator, value, value2, sortOrder);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ReleaseRuleConditionUpdate {\n");
    sb.append("    conditionType: ").append(toIndentedString(conditionType)).append("\n");
    sb.append("    operator: ").append(toIndentedString(operator)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
    sb.append("    value2: ").append(toIndentedString(value2)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
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

