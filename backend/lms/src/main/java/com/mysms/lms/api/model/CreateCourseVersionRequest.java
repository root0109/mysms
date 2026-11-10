// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

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
 * CreateCourseVersionRequest
 */

public class CreateCourseVersionRequest {

  /**
   * Kind of version.
   */
  public enum VersionKindEnum {
    MAJOR("major"),
    
    MINOR("minor");

    private final String value;

    VersionKindEnum(String value) {
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
    public static VersionKindEnum fromValue(String value) {
      for (VersionKindEnum b : VersionKindEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private VersionKindEnum versionKind;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String authorComments;

  public CreateCourseVersionRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateCourseVersionRequest(VersionKindEnum versionKind) {
    this.versionKind = versionKind;
  }

  public CreateCourseVersionRequest versionKind(VersionKindEnum versionKind) {
    this.versionKind = versionKind;
    return this;
  }

  /**
   * Kind of version.
   * @return versionKind
   */
  @NotNull 
  @JsonProperty("versionKind")
  public VersionKindEnum getVersionKind() {
    return versionKind;
  }

  @JsonProperty("versionKind")
  public void setVersionKind(VersionKindEnum versionKind) {
    this.versionKind = versionKind;
  }

  public CreateCourseVersionRequest authorComments(@Nullable String authorComments) {
    this.authorComments = authorComments;
    return this;
  }

  /**
   * Notes on the new version.
   * @return authorComments
   */
  
  @JsonProperty("authorComments")
  public @Nullable String getAuthorComments() {
    return authorComments;
  }

  @JsonProperty("authorComments")
  public void setAuthorComments(@Nullable String authorComments) {
    this.authorComments = authorComments;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateCourseVersionRequest createCourseVersionRequest = (CreateCourseVersionRequest) o;
    return Objects.equals(this.versionKind, createCourseVersionRequest.versionKind) &&
        Objects.equals(this.authorComments, createCourseVersionRequest.authorComments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(versionKind, authorComments);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateCourseVersionRequest {\n");
    sb.append("    versionKind: ").append(toIndentedString(versionKind)).append("\n");
    sb.append("    authorComments: ").append(toIndentedString(authorComments)).append("\n");
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

