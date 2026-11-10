// Owned API contract of finance (JAVA-CODING-RULES §7); first materialized from api/openapi/finance.yaml on 2026-11-10.
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
 * Fields of CouponScope that can be updated (partial update; see updateMask).
 */

public class CouponScopeUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID lmsCourseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID lmsCohortId;

  public CouponScopeUpdate lmsCourseId(@Nullable UUID lmsCourseId) {
    this.lmsCourseId = lmsCourseId;
    return this;
  }

  /**
   * Course.
   * @return lmsCourseId
   */
  @Valid 
  @JsonProperty("lmsCourseId")
  public @Nullable UUID getLmsCourseId() {
    return lmsCourseId;
  }

  @JsonProperty("lmsCourseId")
  public void setLmsCourseId(@Nullable UUID lmsCourseId) {
    this.lmsCourseId = lmsCourseId;
  }

  public CouponScopeUpdate lmsCohortId(@Nullable UUID lmsCohortId) {
    this.lmsCohortId = lmsCohortId;
    return this;
  }

  /**
   * Cohort.
   * @return lmsCohortId
   */
  @Valid 
  @JsonProperty("lmsCohortId")
  public @Nullable UUID getLmsCohortId() {
    return lmsCohortId;
  }

  @JsonProperty("lmsCohortId")
  public void setLmsCohortId(@Nullable UUID lmsCohortId) {
    this.lmsCohortId = lmsCohortId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CouponScopeUpdate couponScopeUpdate = (CouponScopeUpdate) o;
    return Objects.equals(this.lmsCourseId, couponScopeUpdate.lmsCourseId) &&
        Objects.equals(this.lmsCohortId, couponScopeUpdate.lmsCohortId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lmsCourseId, lmsCohortId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CouponScopeUpdate {\n");
    sb.append("    lmsCourseId: ").append(toIndentedString(lmsCourseId)).append("\n");
    sb.append("    lmsCohortId: ").append(toIndentedString(lmsCohortId)).append("\n");
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

