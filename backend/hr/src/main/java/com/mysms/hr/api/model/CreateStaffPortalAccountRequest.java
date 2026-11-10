// Owned API contract of hr (JAVA-CODING-RULES §7); first materialized from api/openapi/hr.yaml on 2026-11-10.
package com.mysms.hr.api.model;

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
 * CreateStaffPortalAccountRequest
 */

public class CreateStaffPortalAccountRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean sendInvite;

  public CreateStaffPortalAccountRequest sendInvite(@Nullable Boolean sendInvite) {
    this.sendInvite = sendInvite;
    return this;
  }

  /**
   * Send invitation.
   * @return sendInvite
   */
  
  @JsonProperty("sendInvite")
  public @Nullable Boolean getSendInvite() {
    return sendInvite;
  }

  @JsonProperty("sendInvite")
  public void setSendInvite(@Nullable Boolean sendInvite) {
    this.sendInvite = sendInvite;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateStaffPortalAccountRequest createStaffPortalAccountRequest = (CreateStaffPortalAccountRequest) o;
    return Objects.equals(this.sendInvite, createStaffPortalAccountRequest.sendInvite);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sendInvite);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateStaffPortalAccountRequest {\n");
    sb.append("    sendInvite: ").append(toIndentedString(sendInvite)).append("\n");
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

