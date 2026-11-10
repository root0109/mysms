// Owned API contract of iam (JAVA-CODING-RULES §7); first materialized from api/openapi/iam.yaml on 2026-11-10.
package com.mysms.iam.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * LogoutRequest
 */

public class LogoutRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean allDevices;

  public LogoutRequest allDevices(@Nullable Boolean allDevices) {
    this.allDevices = allDevices;
    return this;
  }

  /**
   * Revoke every session of the user.
   * @return allDevices
   */
  
  @JsonProperty("allDevices")
  public @Nullable Boolean getAllDevices() {
    return allDevices;
  }

  @JsonProperty("allDevices")
  public void setAllDevices(@Nullable Boolean allDevices) {
    this.allDevices = allDevices;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LogoutRequest logoutRequest = (LogoutRequest) o;
    return Objects.equals(this.allDevices, logoutRequest.allDevices);
  }

  @Override
  public int hashCode() {
    return Objects.hash(allDevices);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LogoutRequest {\n");
    sb.append("    allDevices: ").append(toIndentedString(allDevices)).append("\n");
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

