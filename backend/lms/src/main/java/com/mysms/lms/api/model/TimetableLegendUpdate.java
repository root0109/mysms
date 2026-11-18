// Owned API contract of lms.
package com.mysms.lms.api.model;

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
 * Fields of TimetableLegend that can be updated (partial update; see updateMask).
 */

public class TimetableLegendUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID timetableTemplateId;

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
  private @Nullable ItemKindEnum itemKind;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String color;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String label;

  public TimetableLegendUpdate cohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort.
   * @return cohortId
   */
  @Valid 
  @JsonProperty("cohortId")
  public @Nullable UUID getCohortId() {
    return cohortId;
  }

  @JsonProperty("cohortId")
  public void setCohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
  }

  public TimetableLegendUpdate timetableTemplateId(@Nullable UUID timetableTemplateId) {
    this.timetableTemplateId = timetableTemplateId;
    return this;
  }

  /**
   * Template.
   * @return timetableTemplateId
   */
  @Valid 
  @JsonProperty("timetableTemplateId")
  public @Nullable UUID getTimetableTemplateId() {
    return timetableTemplateId;
  }

  @JsonProperty("timetableTemplateId")
  public void setTimetableTemplateId(@Nullable UUID timetableTemplateId) {
    this.timetableTemplateId = timetableTemplateId;
  }

  public TimetableLegendUpdate itemKind(@Nullable ItemKindEnum itemKind) {
    this.itemKind = itemKind;
    return this;
  }

  /**
   * Item kind coloured.
   * @return itemKind
   */
  
  @JsonProperty("itemKind")
  public @Nullable ItemKindEnum getItemKind() {
    return itemKind;
  }

  @JsonProperty("itemKind")
  public void setItemKind(@Nullable ItemKindEnum itemKind) {
    this.itemKind = itemKind;
  }

  public TimetableLegendUpdate color(@Nullable String color) {
    this.color = color;
    return this;
  }

  /**
   * Colour.
   * @return color
   */
  @Size(max = 20) 
  @JsonProperty("color")
  public @Nullable String getColor() {
    return color;
  }

  @JsonProperty("color")
  public void setColor(@Nullable String color) {
    this.color = color;
  }

  public TimetableLegendUpdate label(@Nullable String label) {
    this.label = label;
    return this;
  }

  /**
   * Legend label.
   * @return label
   */
  @Size(max = 60) 
  @JsonProperty("label")
  public @Nullable String getLabel() {
    return label;
  }

  @JsonProperty("label")
  public void setLabel(@Nullable String label) {
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

