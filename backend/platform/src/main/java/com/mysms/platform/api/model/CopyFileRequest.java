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
 * CopyFileRequest
 */

public class CopyFileRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String ownerEntityType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID ownerEntityId;

  public CopyFileRequest ownerEntityType(@Nullable String ownerEntityType) {
    this.ownerEntityType = ownerEntityType;
    return this;
  }

  /**
   * Owner type of the copy.
   * @return ownerEntityType
   */
  
  @JsonProperty("ownerEntityType")
  public @Nullable String getOwnerEntityType() {
    return ownerEntityType;
  }

  @JsonProperty("ownerEntityType")
  public void setOwnerEntityType(@Nullable String ownerEntityType) {
    this.ownerEntityType = ownerEntityType;
  }

  public CopyFileRequest ownerEntityId(@Nullable UUID ownerEntityId) {
    this.ownerEntityId = ownerEntityId;
    return this;
  }

  /**
   * Owner record of the copy.
   * @return ownerEntityId
   */
  @Valid 
  @JsonProperty("ownerEntityId")
  public @Nullable UUID getOwnerEntityId() {
    return ownerEntityId;
  }

  @JsonProperty("ownerEntityId")
  public void setOwnerEntityId(@Nullable UUID ownerEntityId) {
    this.ownerEntityId = ownerEntityId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CopyFileRequest copyFileRequest = (CopyFileRequest) o;
    return Objects.equals(this.ownerEntityType, copyFileRequest.ownerEntityType) &&
        Objects.equals(this.ownerEntityId, copyFileRequest.ownerEntityId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ownerEntityType, ownerEntityId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CopyFileRequest {\n");
    sb.append("    ownerEntityType: ").append(toIndentedString(ownerEntityType)).append("\n");
    sb.append("    ownerEntityId: ").append(toIndentedString(ownerEntityId)).append("\n");
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

