// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of TimetableLegend that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class TimetableLegendUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> timetableTemplateId;

  /**
   * Item kind coloured.
   */
  public enum ItemKindEnum {
    LESSON("lesson"),
    
    QUIZ("quiz"),
    
    ASSIGNMENT("assignment"),
    
    EXERCISE("exercise"),
    
    LIVE_CLASS("live_class");

    private final String value;

    ItemKindEnum(String value) {
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
    public static ItemKindEnum fromValue(String value) {
      for (ItemKindEnum b : ItemKindEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull ItemKindEnum> itemKind;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 20) String> color;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 60) String> label;

  public TimetableLegendUpdate cohortId(@Nullable Optional<UUID> cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort.
   * @return cohortId
   */
  
  @JsonProperty("cohortId")
  public @Nullable Optional<UUID> getCohortId() {
    return cohortId;
  }

  @JsonProperty("cohortId")
  public void setCohortId(@Nullable Optional<UUID> cohortId) {
    this.cohortId = cohortId;
  }

  public TimetableLegendUpdate timetableTemplateId(@Nullable Optional<UUID> timetableTemplateId) {
    this.timetableTemplateId = timetableTemplateId;
    return this;
  }

  /**
   * Template.
   * @return timetableTemplateId
   */
  
  @JsonProperty("timetableTemplateId")
  public @Nullable Optional<UUID> getTimetableTemplateId() {
    return timetableTemplateId;
  }

  @JsonProperty("timetableTemplateId")
  public void setTimetableTemplateId(@Nullable Optional<UUID> timetableTemplateId) {
    this.timetableTemplateId = timetableTemplateId;
  }

  public TimetableLegendUpdate itemKind(@Nullable Optional<ItemKindEnum> itemKind) {
    this.itemKind = itemKind;
    return this;
  }

  /**
   * Item kind coloured.
   * @return itemKind
   */
  
  @JsonProperty("itemKind")
  public @Nullable Optional<ItemKindEnum> getItemKind() {
    return itemKind;
  }

  @JsonProperty("itemKind")
  public void setItemKind(@Nullable Optional<ItemKindEnum> itemKind) {
    this.itemKind = itemKind;
  }

  public TimetableLegendUpdate color(@Nullable Optional<String> color) {
    this.color = color;
    return this;
  }

  /**
   * Colour.
   * @return color
   */
  
  @JsonProperty("color")
  public @Nullable Optional<String> getColor() {
    return color;
  }

  @JsonProperty("color")
  public void setColor(@Nullable Optional<String> color) {
    this.color = color;
  }

  public TimetableLegendUpdate label(@Nullable Optional<String> label) {
    this.label = label;
    return this;
  }

  /**
   * Legend label.
   * @return label
   */
  
  @JsonProperty("label")
  public @Nullable Optional<String> getLabel() {
    return label;
  }

  @JsonProperty("label")
  public void setLabel(@Nullable Optional<String> label) {
    this.label = label;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TimetableLegendUpdate timetableLegendUpdate = (TimetableLegendUpdate) o;
    return Objects.equals(this.cohortId, timetableLegendUpdate.cohortId) &&
        Objects.equals(this.timetableTemplateId, timetableLegendUpdate.timetableTemplateId) &&
        Objects.equals(this.itemKind, timetableLegendUpdate.itemKind) &&
        Objects.equals(this.color, timetableLegendUpdate.color) &&
        Objects.equals(this.label, timetableLegendUpdate.label);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cohortId, timetableTemplateId, itemKind, color, label);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TimetableLegendUpdate {\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    timetableTemplateId: ").append(toIndentedString(timetableTemplateId)).append("\n");
    sb.append("    itemKind: ").append(toIndentedString(itemKind)).append("\n");
    sb.append("    color: ").append(toIndentedString(color)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
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

