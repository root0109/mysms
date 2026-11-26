// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * BatchEnrollPreview
 */

public class BatchEnrollPreview {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean alreadyEnrolled;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean paymentRequired;

  public BatchEnrollPreview() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BatchEnrollPreview(UUID userId, Boolean alreadyEnrolled, Boolean paymentRequired) {
    this.userId = userId;
    this.alreadyEnrolled = alreadyEnrolled;
    this.paymentRequired = paymentRequired;
  }

  public BatchEnrollPreview userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * User.
   * @return userId
   */
  @NotNull @Valid 
  @JsonProperty("userId")
  public UUID getUserId() {
    return userId;
  }

  @JsonProperty("userId")
  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public BatchEnrollPreview alreadyEnrolled(Boolean alreadyEnrolled) {
    this.alreadyEnrolled = alreadyEnrolled;
    return this;
  }

  /**
   * Already enrolled.
   * @return alreadyEnrolled
   */
  @NotNull 
  @JsonProperty("alreadyEnrolled")
  public Boolean getAlreadyEnrolled() {
    return alreadyEnrolled;
  }

  @JsonProperty("alreadyEnrolled")
  public void setAlreadyEnrolled(Boolean alreadyEnrolled) {
    this.alreadyEnrolled = alreadyEnrolled;
  }

  public BatchEnrollPreview paymentRequired(Boolean paymentRequired) {
    this.paymentRequired = paymentRequired;
    return this;
  }

  /**
   * Payment needed.
   * @return paymentRequired
   */
  @NotNull 
  @JsonProperty("paymentRequired")
  public Boolean getPaymentRequired() {
    return paymentRequired;
  }

  @JsonProperty("paymentRequired")
  public void setPaymentRequired(Boolean paymentRequired) {
    this.paymentRequired = paymentRequired;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BatchEnrollPreview batchEnrollPreview = (BatchEnrollPreview) o;
    return Objects.equals(this.userId, batchEnrollPreview.userId) &&
        Objects.equals(this.alreadyEnrolled, batchEnrollPreview.alreadyEnrolled) &&
        Objects.equals(this.paymentRequired, batchEnrollPreview.paymentRequired);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, alreadyEnrolled, paymentRequired);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BatchEnrollPreview {\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    alreadyEnrolled: ").append(toIndentedString(alreadyEnrolled)).append("\n");
    sb.append("    paymentRequired: ").append(toIndentedString(paymentRequired)).append("\n");
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

