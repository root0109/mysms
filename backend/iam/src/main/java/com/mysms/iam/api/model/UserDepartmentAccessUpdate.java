// Owned API contract of iam.
package com.mysms.iam.api.model;

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
 * Fields of UserDepartmentAccess that can be updated (partial update; see updateMask).
 */

public class UserDepartmentAccessUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID departmentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isPrimary;

  public UserDepartmentAccessUpdate departmentId(@Nullable UUID departmentId) {
    this.departmentId = departmentId;
    return this;
  }

  /**
   * Department.
   * @return departmentId
   */
  @Valid 
  @JsonProperty("departmentId")
  public @Nullable UUID getDepartmentId() {
    return departmentId;
  }

  @JsonProperty("departmentId")
  public void setDepartmentId(@Nullable UUID departmentId) {
    this.departmentId = departmentId;
  }

  public UserDepartmentAccessUpdate isPrimary(@Nullable Boolean isPrimary) {
    this.isPrimary = isPrimary;
    return this;
  }

  /**
   * Primary department of the account.
   * @return isPrimary
   */
  
  @JsonProperty("isPrimary")
  public @Nullable Boolean getIsPrimary() {
    return isPrimary;
  }

  @JsonProperty("isPrimary")
  public void setIsPrimary(@Nullable Boolean isPrimary) {
    this.isPrimary = isPrimary;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserDepartmentAccessUpdate userDepartmentAccessUpdate = (UserDepartmentAccessUpdate) o;
    return Objects.equals(this.departmentId, userDepartmentAccessUpdate.departmentId) &&
        Objects.equals(this.isPrimary, userDepartmentAccessUpdate.isPrimary);
  }

  @Override
  public int hashCode() {
    return Objects.hash(departmentId, isPrimary);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserDepartmentAccessUpdate {\n");
    sb.append("    departmentId: ").append(toIndentedString(departmentId)).append("\n");
    sb.append("    isPrimary: ").append(toIndentedString(isPrimary)).append("\n");
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

