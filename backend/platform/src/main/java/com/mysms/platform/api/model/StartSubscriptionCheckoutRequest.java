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
 * StartSubscriptionCheckoutRequest
 */

public class StartSubscriptionCheckoutRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID planId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  public StartSubscriptionCheckoutRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public StartSubscriptionCheckoutRequest(UUID planId) {
    this.planId = planId;
  }

  public StartSubscriptionCheckoutRequest planId(UUID planId) {
    this.planId = planId;
    return this;
  }

  /**
   * Plan.
   * @return planId
   */
  @NotNull @Valid 
  @JsonProperty("planId")
  public UUID getPlanId() {
    return planId;
  }

  @JsonProperty("planId")
  public void setPlanId(UUID planId) {
    this.planId = planId;
  }

  public StartSubscriptionCheckoutRequest institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StartSubscriptionCheckoutRequest startSubscriptionCheckoutRequest = (StartSubscriptionCheckoutRequest) o;
    return Objects.equals(this.planId, startSubscriptionCheckoutRequest.planId) &&
        Objects.equals(this.institutionId, startSubscriptionCheckoutRequest.institutionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(planId, institutionId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StartSubscriptionCheckoutRequest {\n");
    sb.append("    planId: ").append(toIndentedString(planId)).append("\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
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

