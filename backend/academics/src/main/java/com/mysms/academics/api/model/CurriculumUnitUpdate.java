// Owned API contract of academics.
package com.mysms.academics.api.model;

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
 * Fields of CurriculumUnit that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class CurriculumUnitUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> classSubjectId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> sequence;

  /**
   * Whether in use.
   */
  public enum StatusEnum {
    ACTIVE("active"),
    
    INACTIVE("inactive");

    private final String value;

    StatusEnum(String value) {
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
    public static StatusEnum fromValue(String value) {
      for (StatusEnum b : StatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull StatusEnum> status;

  public CurriculumUnitUpdate classSubjectId(@Nullable Optional<UUID> classSubjectId) {
    this.classSubjectId = classSubjectId;
    return this;
  }

  /**
   * Class subject.
   * @return classSubjectId
   */
  
  @JsonProperty("classSubjectId")
  public @Nullable Optional<UUID> getClassSubjectId() {
    return classSubjectId;
  }

  @JsonProperty("classSubjectId")
  public void setClassSubjectId(@Nullable Optional<UUID> classSubjectId) {
    this.classSubjectId = classSubjectId;
  }

  public CurriculumUnitUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Unit / chapter name.
   * @return name
   */
  
  @JsonProperty("name")
  public @Nullable Optional<String> getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable Optional<String> name) {
    this.name = name;
  }

  public CurriculumUnitUpdate sequence(@Nullable Optional<Integer> sequence) {
    this.sequence = sequence;
    return this;
  }

  /**
   * Order.
   * @return sequence
   */
  
  @JsonProperty("sequence")
  public @Nullable Optional<Integer> getSequence() {
    return sequence;
  }

  @JsonProperty("sequence")
  public void setSequence(@Nullable Optional<Integer> sequence) {
    this.sequence = sequence;
  }

  public CurriculumUnitUpdate status(@Nullable Optional<StatusEnum> status) {
    this.status = status;
    return this;
  }

  /**
   * Whether in use.
   * @return status
   */
  
  @JsonProperty("status")
  public @Nullable Optional<StatusEnum> getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable Optional<StatusEnum> status) {
    this.status = status;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CurriculumUnitUpdate curriculumUnitUpdate = (CurriculumUnitUpdate) o;
    return Objects.equals(this.classSubjectId, curriculumUnitUpdate.classSubjectId) &&
        Objects.equals(this.name, curriculumUnitUpdate.name) &&
        Objects.equals(this.sequence, curriculumUnitUpdate.sequence) &&
        Objects.equals(this.status, curriculumUnitUpdate.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(classSubjectId, name, sequence, status);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CurriculumUnitUpdate {\n");
    sb.append("    classSubjectId: ").append(toIndentedString(classSubjectId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    sequence: ").append(toIndentedString(sequence)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
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

