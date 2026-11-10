// Owned API contract of sis (JAVA-CODING-RULES §7); first materialized from api/openapi/sis.yaml on 2026-11-10.
package com.mysms.sis.api.model;

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
 * IssueBonafideCertificateRequest
 */

public class IssueBonafideCertificateRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String purpose;

  public IssueBonafideCertificateRequest purpose(@Nullable String purpose) {
    this.purpose = purpose;
    return this;
  }

  /**
   * Purpose printed on the certificate.
   * @return purpose
   */
  
  @JsonProperty("purpose")
  public @Nullable String getPurpose() {
    return purpose;
  }

  @JsonProperty("purpose")
  public void setPurpose(@Nullable String purpose) {
    this.purpose = purpose;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IssueBonafideCertificateRequest issueBonafideCertificateRequest = (IssueBonafideCertificateRequest) o;
    return Objects.equals(this.purpose, issueBonafideCertificateRequest.purpose);
  }

  @Override
  public int hashCode() {
    return Objects.hash(purpose);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IssueBonafideCertificateRequest {\n");
    sb.append("    purpose: ").append(toIndentedString(purpose)).append("\n");
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

