// Owned API contract of academics (JAVA-CODING-RULES §7); first materialized from api/openapi/academics.yaml on 2026-11-10.
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
 * Fields of ClassSubject that can be updated (partial update; see updateMask).
 */

public class ClassSubjectUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID subjectId;

  /**
   * How the subject is offered.
   */
  public enum OfferingTypeEnum {
    CORE("core"),
    
    ELECTIVE("elective"),
    
    EXAM_ONLY("exam_only");

    private final String value;

    OfferingTypeEnum(String value) {
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
    public static OfferingTypeEnum fromValue(String value) {
      for (OfferingTypeEnum b : OfferingTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable OfferingTypeEnum offeringType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  public ClassSubjectUpdate subjectId(@Nullable UUID subjectId) {
    this.subjectId = subjectId;
    return this;
  }

  /**
   * Subject.
   * @return subjectId
   */
  @Valid 
  @JsonProperty("subjectId")
  public @Nullable UUID getSubjectId() {
    return subjectId;
  }

  @JsonProperty("subjectId")
  public void setSubjectId(@Nullable UUID subjectId) {
    this.subjectId = subjectId;
  }

  public ClassSubjectUpdate offeringType(@Nullable OfferingTypeEnum offeringType) {
    this.offeringType = offeringType;
    return this;
  }

  /**
   * How the subject is offered.
   * @return offeringType
   */
  
  @JsonProperty("offeringType")
  public @Nullable OfferingTypeEnum getOfferingType() {
    return offeringType;
  }

  @JsonProperty("offeringType")
  public void setOfferingType(@Nullable OfferingTypeEnum offeringType) {
    this.offeringType = offeringType;
  }

  public ClassSubjectUpdate isActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether active.
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Boolean getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClassSubjectUpdate classSubjectUpdate = (ClassSubjectUpdate) o;
    return Objects.equals(this.subjectId, classSubjectUpdate.subjectId) &&
        Objects.equals(this.offeringType, classSubjectUpdate.offeringType) &&
        Objects.equals(this.isActive, classSubjectUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subjectId, offeringType, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClassSubjectUpdate {\n");
    sb.append("    subjectId: ").append(toIndentedString(subjectId)).append("\n");
    sb.append("    offeringType: ").append(toIndentedString(offeringType)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
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

