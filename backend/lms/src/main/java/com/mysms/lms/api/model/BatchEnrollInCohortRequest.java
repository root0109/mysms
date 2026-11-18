// Owned API contract of lms.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * BatchEnrollInCohortRequest
 */

public class BatchEnrollInCohortRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<UUID> userIds = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<UUID> paymentIds = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean validateOnly;

  public BatchEnrollInCohortRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BatchEnrollInCohortRequest(List<UUID> userIds) {
    this.userIds = userIds;
  }

  public BatchEnrollInCohortRequest userIds(List<UUID> userIds) {
    this.userIds = userIds;
    return this;
  }

  public BatchEnrollInCohortRequest addUserIdsItem(UUID userIdsItem) {
    if (this.userIds == null) {
      this.userIds = new ArrayList<>();
    }
    this.userIds.add(userIdsItem);
    return this;
  }

  /**
   * Students.
   * @return userIds
   */
  @JsonProperty("userIds")
  public @Nullable List<UUID> getUserIds() {
    return userIds;
  }

  @JsonProperty("userIds")
  public void setUserIds(List<UUID> userIds) {
    this.userIds = userIds;
  }

  public BatchEnrollInCohortRequest paymentIds(List<UUID> paymentIds) {
    this.paymentIds = paymentIds;
    return this;
  }

  public BatchEnrollInCohortRequest addPaymentIdsItem(UUID paymentIdsItem) {
    if (this.paymentIds == null) {
      this.paymentIds = new ArrayList<>();
    }
    this.paymentIds.add(paymentIdsItem);
    return this;
  }

  /**
   * Payments.
   * @return paymentIds
   */
  
  @JsonProperty("paymentIds")
  public List<UUID> getPaymentIds() {
    return paymentIds;
  }

  @JsonProperty("paymentIds")
  public void setPaymentIds(List<UUID> paymentIds) {
    this.paymentIds = paymentIds;
  }

  public BatchEnrollInCohortRequest validateOnly(@Nullable Boolean validateOnly) {
    this.validateOnly = validateOnly;
    return this;
  }

  /**
   * Preview only.
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
    BatchEnrollInCohortRequest batchEnrollInCohortRequest = (BatchEnrollInCohortRequest) o;
    return Objects.equals(this.userIds, batchEnrollInCohortRequest.userIds) &&
        Objects.equals(this.paymentIds, batchEnrollInCohortRequest.paymentIds) &&
        Objects.equals(this.validateOnly, batchEnrollInCohortRequest.validateOnly);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userIds, paymentIds, validateOnly);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BatchEnrollInCohortRequest {\n");
    sb.append("    userIds: ").append(toIndentedString(userIds)).append("\n");
    sb.append("    paymentIds: ").append(toIndentedString(paymentIds)).append("\n");
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

