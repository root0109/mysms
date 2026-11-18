// Owned API contract of finance.
package com.mysms.finance.api.model;

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
 * Fields of FeePlanInstallmentComponent that can be updated (partial update; see updateMask).
 */

public class FeePlanInstallmentComponentUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID installmentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID feeHeadId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sequence;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String sharePercent;

  public FeePlanInstallmentComponentUpdate installmentId(@Nullable UUID installmentId) {
    this.installmentId = installmentId;
    return this;
  }

  /**
   * Installment.
   * @return installmentId
   */
  @Valid 
  @JsonProperty("installmentId")
  public @Nullable UUID getInstallmentId() {
    return installmentId;
  }

  @JsonProperty("installmentId")
  public void setInstallmentId(@Nullable UUID installmentId) {
    this.installmentId = installmentId;
  }

  public FeePlanInstallmentComponentUpdate feeHeadId(@Nullable UUID feeHeadId) {
    this.feeHeadId = feeHeadId;
    return this;
  }

  /**
   * Fee head.
   * @return feeHeadId
   */
  @Valid 
  @JsonProperty("feeHeadId")
  public @Nullable UUID getFeeHeadId() {
    return feeHeadId;
  }

  @JsonProperty("feeHeadId")
  public void setFeeHeadId(@Nullable UUID feeHeadId) {
    this.feeHeadId = feeHeadId;
  }

  public FeePlanInstallmentComponentUpdate sequence(@Nullable Integer sequence) {
    this.sequence = sequence;
    return this;
  }

  /**
   * Order.
   * @return sequence
   */
  
  @JsonProperty("sequence")
  public @Nullable Integer getSequence() {
    return sequence;
  }

  @JsonProperty("sequence")
  public void setSequence(@Nullable Integer sequence) {
    this.sequence = sequence;
  }

  public FeePlanInstallmentComponentUpdate sharePercent(@Nullable String sharePercent) {
    this.sharePercent = sharePercent;
    return this;
  }

  /**
   * Share of the installment amount.
   * @return sharePercent
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("sharePercent")
  public @Nullable String getSharePercent() {
    return sharePercent;
  }

  @JsonProperty("sharePercent")
  public void setSharePercent(@Nullable String sharePercent) {
    this.sharePercent = sharePercent;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FeePlanInstallmentComponentUpdate feePlanInstallmentComponentUpdate = (FeePlanInstallmentComponentUpdate) o;
    return Objects.equals(this.installmentId, feePlanInstallmentComponentUpdate.installmentId) &&
        Objects.equals(this.feeHeadId, feePlanInstallmentComponentUpdate.feeHeadId) &&
        Objects.equals(this.sequence, feePlanInstallmentComponentUpdate.sequence) &&
        Objects.equals(this.sharePercent, feePlanInstallmentComponentUpdate.sharePercent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(installmentId, feeHeadId, sequence, sharePercent);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FeePlanInstallmentComponentUpdate {\n");
    sb.append("    installmentId: ").append(toIndentedString(installmentId)).append("\n");
    sb.append("    feeHeadId: ").append(toIndentedString(feeHeadId)).append("\n");
    sb.append("    sequence: ").append(toIndentedString(sequence)).append("\n");
    sb.append("    sharePercent: ").append(toIndentedString(sharePercent)).append("\n");
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

