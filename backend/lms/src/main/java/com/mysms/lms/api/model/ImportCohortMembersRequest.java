// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
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
 * ImportCohortMembersRequest
 */

public class ImportCohortMembersRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID fileId;

  /**
   * Rows describe students, staff or both.
   */
  public enum MemberKindEnum {
    STUDENT("student"),
    
    STAFF("staff"),
    
    ANY("any");

    private final String value;

    MemberKindEnum(String value) {
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
    public static MemberKindEnum fromValue(String value) {
      for (MemberKindEnum b : MemberKindEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private MemberKindEnum memberKind;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean validateOnly;

  public ImportCohortMembersRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ImportCohortMembersRequest(UUID fileId, MemberKindEnum memberKind) {
    this.fileId = fileId;
    this.memberKind = memberKind;
  }

  public ImportCohortMembersRequest fileId(UUID fileId) {
    this.fileId = fileId;
    return this;
  }

  /**
   * Uploaded CSV / spreadsheet.
   * @return fileId
   */
  @NotNull @Valid 
  @JsonProperty("fileId")
  public UUID getFileId() {
    return fileId;
  }

  @JsonProperty("fileId")
  public void setFileId(UUID fileId) {
    this.fileId = fileId;
  }

  public ImportCohortMembersRequest memberKind(MemberKindEnum memberKind) {
    this.memberKind = memberKind;
    return this;
  }

  /**
   * Rows describe students, staff or both.
   * @return memberKind
   */
  @NotNull 
  @JsonProperty("memberKind")
  public MemberKindEnum getMemberKind() {
    return memberKind;
  }

  @JsonProperty("memberKind")
  public void setMemberKind(MemberKindEnum memberKind) {
    this.memberKind = memberKind;
  }

  public ImportCohortMembersRequest validateOnly(@Nullable Boolean validateOnly) {
    this.validateOnly = validateOnly;
    return this;
  }

  /**
   * Check the file without importing.
   * @return validateOnly
   */
  
  @JsonProperty("validateOnly")
  public @Nullable Boolean getValidateOnly() {
    return validateOnly;
  }

  @JsonProperty("validateOnly")
  public void setValidateOnly(@Nullable Boolean validateOnly) {
    this.validateOnly = validateOnly;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ImportCohortMembersRequest importCohortMembersRequest = (ImportCohortMembersRequest) o;
    return Objects.equals(this.fileId, importCohortMembersRequest.fileId) &&
        Objects.equals(this.memberKind, importCohortMembersRequest.memberKind) &&
        Objects.equals(this.validateOnly, importCohortMembersRequest.validateOnly);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileId, memberKind, validateOnly);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ImportCohortMembersRequest {\n");
    sb.append("    fileId: ").append(toIndentedString(fileId)).append("\n");
    sb.append("    memberKind: ").append(toIndentedString(memberKind)).append("\n");
    sb.append("    validateOnly: ").append(toIndentedString(validateOnly)).append("\n");
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

