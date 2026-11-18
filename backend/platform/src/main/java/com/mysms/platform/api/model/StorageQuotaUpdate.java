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
 * Fields of StorageQuota that can be updated (partial update; see updateMask).
 */

public class StorageQuotaUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String quotaBytes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String usedBytes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  public StorageQuotaUpdate userId(@Nullable UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * User the quota belongs to.
   * @return userId
   */
  @Valid 
  @JsonProperty("userId")
  public @Nullable UUID getUserId() {
    return userId;
  }

  @JsonProperty("userId")
  public void setUserId(@Nullable UUID userId) {
    this.userId = userId;
  }

  public StorageQuotaUpdate institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution the quota belongs to (institution-wide pool).
   * @return institutionId
   */
  @Valid 
  @JsonProperty("institutionId")
  public @Nullable UUID getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
  }

  public StorageQuotaUpdate quotaBytes(@Nullable String quotaBytes) {
    this.quotaBytes = quotaBytes;
    return this;
  }

  /**
   * Allowed bytes.
   * @return quotaBytes
   */
  @Pattern(regexp = "^-?[0-9]+$") 
  @JsonProperty("quotaBytes")
  public @Nullable String getQuotaBytes() {
    return quotaBytes;
  }

  @JsonProperty("quotaBytes")
  public void setQuotaBytes(@Nullable String quotaBytes) {
    this.quotaBytes = quotaBytes;
  }

  public StorageQuotaUpdate usedBytes(@Nullable String usedBytes) {
    this.usedBytes = usedBytes;
    return this;
  }

  /**
   * Bytes in use (maintained by the application).
   * @return usedBytes
   */
  @Pattern(regexp = "^-?[0-9]+$") 
  @JsonProperty("usedBytes")
  public @Nullable String getUsedBytes() {
    return usedBytes;
  }

  @JsonProperty("usedBytes")
  public void setUsedBytes(@Nullable String usedBytes) {
    this.usedBytes = usedBytes;
  }

  public StorageQuotaUpdate isActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Quota is enforced.
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
    StorageQuotaUpdate storageQuotaUpdate = (StorageQuotaUpdate) o;
    return Objects.equals(this.userId, storageQuotaUpdate.userId) &&
        Objects.equals(this.institutionId, storageQuotaUpdate.institutionId) &&
        Objects.equals(this.quotaBytes, storageQuotaUpdate.quotaBytes) &&
        Objects.equals(this.usedBytes, storageQuotaUpdate.usedBytes) &&
        Objects.equals(this.isActive, storageQuotaUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, institutionId, quotaBytes, usedBytes, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StorageQuotaUpdate {\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    quotaBytes: ").append(toIndentedString(quotaBytes)).append("\n");
    sb.append("    usedBytes: ").append(toIndentedString(usedBytes)).append("\n");
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

