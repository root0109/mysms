// Owned API contract of sis.
package com.mysms.students.sis.api.model;

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
 * Fields of SubjectRegistrationLine that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class SubjectRegistrationLineUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> subjectId;

  /**
   * Compulsory or elective.
   */
  public enum SelectionTypeEnum {
    COMPULSORY("compulsory"),
    
    ELECTIVE("elective");

    private final String value;

    SelectionTypeEnum(String value) {
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
    public static SelectionTypeEnum fromValue(String value) {
      for (SelectionTypeEnum b : SelectionTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull SelectionTypeEnum> selectionType;

  public SubjectRegistrationLineUpdate subjectId(@Nullable Optional<UUID> subjectId) {
    this.subjectId = subjectId;
    return this;
  }

  /**
   * Subject.
   * @return subjectId
   */
  
  @JsonProperty("subjectId")
  public @Nullable Optional<UUID> getSubjectId() {
    return subjectId;
  }

  @JsonProperty("subjectId")
  public void setSubjectId(@Nullable Optional<UUID> subjectId) {
    this.subjectId = subjectId;
  }

  public SubjectRegistrationLineUpdate selectionType(@Nullable Optional<SelectionTypeEnum> selectionType) {
    this.selectionType = selectionType;
    return this;
  }

  /**
   * Compulsory or elective.
   * @return selectionType
   */
  
  @JsonProperty("selectionType")
  public @Nullable Optional<SelectionTypeEnum> getSelectionType() {
    return selectionType;
  }

  @JsonProperty("selectionType")
  public void setSelectionType(@Nullable Optional<SelectionTypeEnum> selectionType) {
    this.selectionType = selectionType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SubjectRegistrationLineUpdate subjectRegistrationLineUpdate = (SubjectRegistrationLineUpdate) o;
    return Objects.equals(this.subjectId, subjectRegistrationLineUpdate.subjectId) &&
        Objects.equals(this.selectionType, subjectRegistrationLineUpdate.selectionType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subjectId, selectionType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SubjectRegistrationLineUpdate {\n");
    sb.append("    subjectId: ").append(toIndentedString(subjectId)).append("\n");
    sb.append("    selectionType: ").append(toIndentedString(selectionType)).append("\n");
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

