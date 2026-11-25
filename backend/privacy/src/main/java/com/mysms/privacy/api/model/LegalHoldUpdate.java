// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;

/**
 * The field of an active legal hold that may change: its review date. What it holds is fixed once placed.
 */

public class LegalHoldUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate reviewBy;

  public LegalHoldUpdate() {
    super();
  }

  public LegalHoldUpdate reviewBy(@Nullable LocalDate reviewBy) {
    this.reviewBy = reviewBy;
    return this;
  }

  /**
   * When the hold must next be reviewed: today or later.
   * @return reviewBy
   */
  @Valid 
  @JsonProperty("reviewBy")
  public @Nullable LocalDate getReviewBy() {
    return reviewBy;
  }

  @JsonProperty("reviewBy")
  public void setReviewBy(@Nullable LocalDate reviewBy) {
    this.reviewBy = reviewBy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LegalHoldUpdate other = (LegalHoldUpdate) o;
    return Objects.equals(this.reviewBy, other.reviewBy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(reviewBy);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LegalHoldUpdate {\n");
    sb.append("    reviewBy: ").append(toIndentedString(reviewBy)).append("\n");
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
