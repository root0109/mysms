// Owned API contract of platform.
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
 * Fields of UploadPolicy that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class UploadPolicyUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> roleId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 60) String> moduleCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 60) String> operationCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?[0-9]+$") String> maxSizeBytes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 500) String> allowedExtensions;

  public UploadPolicyUpdate roleId(@Nullable Optional<UUID> roleId) {
    this.roleId = roleId;
    return this;
  }

  /**
   * Role the limit applies to (NULL = everyone).
   * @return roleId
   */
  
  @JsonProperty("roleId")
  public @Nullable Optional<UUID> getRoleId() {
    return roleId;
  }

  @JsonProperty("roleId")
  public void setRoleId(@Nullable Optional<UUID> roleId) {
    this.roleId = roleId;
  }

  public UploadPolicyUpdate moduleCode(@Nullable Optional<String> moduleCode) {
    this.moduleCode = moduleCode;
    return this;
  }

  /**
   * Feature area (assignments, quiz media, profile picture...).
   * @return moduleCode
   */
  
  @JsonProperty("moduleCode")
  public @Nullable Optional<String> getModuleCode() {
    return moduleCode;
  }

  @JsonProperty("moduleCode")
  public void setModuleCode(@Nullable Optional<String> moduleCode) {
    this.moduleCode = moduleCode;
  }

  public UploadPolicyUpdate operationCode(@Nullable Optional<String> operationCode) {
    this.operationCode = operationCode;
    return this;
  }

  /**
   * Specific operation within the feature (e.g. create_question_image).
   * @return operationCode
   */
  
  @JsonProperty("operationCode")
  public @Nullable Optional<String> getOperationCode() {
    return operationCode;
  }

  @JsonProperty("operationCode")
  public void setOperationCode(@Nullable Optional<String> operationCode) {
    this.operationCode = operationCode;
  }

  public UploadPolicyUpdate maxSizeBytes(@Nullable Optional<String> maxSizeBytes) {
    this.maxSizeBytes = maxSizeBytes;
    return this;
  }

  /**
   * Maximum file size in bytes (NULL = platform default).
   * @return maxSizeBytes
   */
  
  @JsonProperty("maxSizeBytes")
  public @Nullable Optional<String> getMaxSizeBytes() {
    return maxSizeBytes;
  }

  @JsonProperty("maxSizeBytes")
  public void setMaxSizeBytes(@Nullable Optional<String> maxSizeBytes) {
    this.maxSizeBytes = maxSizeBytes;
  }

  public UploadPolicyUpdate allowedExtensions(@Nullable Optional<String> allowedExtensions) {
    this.allowedExtensions = allowedExtensions;
    return this;
  }

  /**
   * Comma-separated allowed extensions (NULL = platform default list).
   * @return allowedExtensions
   */
  
  @JsonProperty("allowedExtensions")
  public @Nullable Optional<String> getAllowedExtensions() {
    return allowedExtensions;
  }

  @JsonProperty("allowedExtensions")
  public void setAllowedExtensions(@Nullable Optional<String> allowedExtensions) {
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

