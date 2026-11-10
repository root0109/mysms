// Owned API contract of platform (JAVA-CODING-RULES §7); first materialized from api/openapi/platform.yaml on 2026-11-10.
package com.mysms.platform.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of UploadPolicy that can be updated (partial update; see updateMask).
 */

public class UploadPolicyUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID roleId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String moduleCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String operationCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String maxSizeBytes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String allowedExtensions;

  public UploadPolicyUpdate roleId(@Nullable UUID roleId) {
    this.roleId = roleId;
    return this;
  }

  /**
   * Role the limit applies to (NULL = everyone).
   * @return roleId
   */
  @Valid 
  @JsonProperty("roleId")
  public @Nullable UUID getRoleId() {
    return roleId;
  }

  @JsonProperty("roleId")
  public void setRoleId(@Nullable UUID roleId) {
    this.roleId = roleId;
  }

  public UploadPolicyUpdate moduleCode(@Nullable String moduleCode) {
    this.moduleCode = moduleCode;
    return this;
  }

  /**
   * Feature area (assignments, quiz media, profile picture...).
   * @return moduleCode
   */
  @Size(max = 60) 
  @JsonProperty("moduleCode")
  public @Nullable String getModuleCode() {
    return moduleCode;
  }

  @JsonProperty("moduleCode")
  public void setModuleCode(@Nullable String moduleCode) {
    this.moduleCode = moduleCode;
  }

  public UploadPolicyUpdate operationCode(@Nullable String operationCode) {
    this.operationCode = operationCode;
    return this;
  }

  /**
   * Specific operation within the feature (e.g. create_question_image).
   * @return operationCode
   */
  @Size(max = 60) 
  @JsonProperty("operationCode")
  public @Nullable String getOperationCode() {
    return operationCode;
  }

  @JsonProperty("operationCode")
  public void setOperationCode(@Nullable String operationCode) {
    this.operationCode = operationCode;
  }

  public UploadPolicyUpdate maxSizeBytes(@Nullable String maxSizeBytes) {
    this.maxSizeBytes = maxSizeBytes;
    return this;
  }

  /**
   * Maximum file size in bytes (NULL = platform default).
   * @return maxSizeBytes
   */
  @Pattern(regexp = "^-?[0-9]+$") 
  @JsonProperty("maxSizeBytes")
  public @Nullable String getMaxSizeBytes() {
    return maxSizeBytes;
  }

  @JsonProperty("maxSizeBytes")
  public void setMaxSizeBytes(@Nullable String maxSizeBytes) {
    this.maxSizeBytes = maxSizeBytes;
  }

  public UploadPolicyUpdate allowedExtensions(@Nullable String allowedExtensions) {
    this.allowedExtensions = allowedExtensions;
    return this;
  }

  /**
   * Comma-separated allowed extensions (NULL = platform default list).
   * @return allowedExtensions
   */
  @Size(max = 500) 
  @JsonProperty("allowedExtensions")
  public @Nullable String getAllowedExtensions() {
    return allowedExtensions;
  }

  @JsonProperty("allowedExtensions")
  public void setAllowedExtensions(@Nullable String allowedExtensions) {
    this.allowedExtensions = allowedExtensions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UploadPolicyUpdate uploadPolicyUpdate = (UploadPolicyUpdate) o;
    return Objects.equals(this.roleId, uploadPolicyUpdate.roleId) &&
        Objects.equals(this.moduleCode, uploadPolicyUpdate.moduleCode) &&
        Objects.equals(this.operationCode, uploadPolicyUpdate.operationCode) &&
        Objects.equals(this.maxSizeBytes, uploadPolicyUpdate.maxSizeBytes) &&
        Objects.equals(this.allowedExtensions, uploadPolicyUpdate.allowedExtensions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roleId, moduleCode, operationCode, maxSizeBytes, allowedExtensions);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UploadPolicyUpdate {\n");
    sb.append("    roleId: ").append(toIndentedString(roleId)).append("\n");
    sb.append("    moduleCode: ").append(toIndentedString(moduleCode)).append("\n");
    sb.append("    operationCode: ").append(toIndentedString(operationCode)).append("\n");
    sb.append("    maxSizeBytes: ").append(toIndentedString(maxSizeBytes)).append("\n");
    sb.append("    allowedExtensions: ").append(toIndentedString(allowedExtensions)).append("\n");
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

