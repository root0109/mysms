// Owned API contract of finance (JAVA-CODING-RULES §7); first materialized from api/openapi/finance.yaml on 2026-11-10.
package com.mysms.finance.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of FeePlanInstallment that can be updated (partial update; see updateMask).
 */

public class FeePlanInstallmentUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sequence;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer dueDays;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate dueDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String sharePercent;

  public FeePlanInstallmentUpdate sequence(@Nullable Integer sequence) {
    this.sequence = sequence;
    return this;
  }

  /**
   * Installment number.
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

  public FeePlanInstallmentUpdate dueDays(@Nullable Integer dueDays) {
    this.dueDays = dueDays;
    return this;
  }

  /**
   * Days after the start date (fixed_days).
   * @return dueDays
   */
  
  @JsonProperty("dueDays")
  public @Nullable Integer getDueDays() {
    return dueDays;
  }

  @JsonProperty("dueDays")
  public void setDueDays(@Nullable Integer dueDays) {
    this.dueDays = dueDays;
  }

  public FeePlanInstallmentUpdate dueDate(@Nullable LocalDate dueDate) {
    this.dueDate = dueDate;
    return this;
  }

  /**
   * Fixed due date (fixed_date).
   * @return dueDate
   */
  @Valid 
  @JsonProperty("dueDate")
  public @Nullable LocalDate getDueDate() {
    return dueDate;
  }

  @JsonProperty("dueDate")
  public void setDueDate(@Nullable LocalDate dueDate) {
    this.dueDate = dueDate;
  }

  public FeePlanInstallmentUpdate sharePercent(@Nullable String sharePercent) {
    this.sharePercent = sharePercent;
    return this;
  }

  /**
   * Share of the total fee due in this installment.
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
    FeePlanInstallmentUpdate feePlanInstallmentUpdate = (FeePlanInstallmentUpdate) o;
    return Objects.equals(this.sequence, feePlanInstallmentUpdate.sequence) &&
        Objects.equals(this.dueDays, feePlanInstallmentUpdate.dueDays) &&
        Objects.equals(this.dueDate, feePlanInstallmentUpdate.dueDate) &&
        Objects.equals(this.sharePercent, feePlanInstallmentUpdate.sharePercent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sequence, dueDays, dueDate, sharePercent);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FeePlanInstallmentUpdate {\n");
    sb.append("    sequence: ").append(toIndentedString(sequence)).append("\n");
    sb.append("    dueDays: ").append(toIndentedString(dueDays)).append("\n");
    sb.append("    dueDate: ").append(toIndentedString(dueDate)).append("\n");
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

