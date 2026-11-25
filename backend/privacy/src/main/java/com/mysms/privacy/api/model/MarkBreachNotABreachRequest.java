// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import org.springframework.lang.Nullable;

/**
 * Body of MarkBreachNotABreach: why triage found it was not a personal data breach.
 */

public class MarkBreachNotABreachRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String reason;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  public MarkBreachNotABreachRequest() {
    super();
  }

  public MarkBreachNotABreachRequest reason(String reason) {
    this.reason = reason;
    return this;
  }

  /**
   * Why it is not a personal data breach; kept on the breach and in the audit trail.
   * @return reason
   */
  @NotNull @NotBlank @Size(max = 1000) 
  @JsonProperty("reason")
  public String getReason() {
    return reason;
  }

  @JsonProperty("reason")
  public void setReason(String reason) {
    this.reason = reason;
  }

  public MarkBreachNotABreachRequest etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Concurrency token from the last read (optional but recommended).
   * @return etag
   */
   
  @JsonProperty("etag")
  public @Nullable String getEtag() {
    return etag;
  }

  @JsonProperty("etag")
  public void setEtag(@Nullable String etag) {
    this.etag = etag;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MarkBreachNotABreachRequest other = (MarkBreachNotABreachRequest) o;
    return Objects.equals(this.reason, other.reason) &&
        Objects.equals(this.etag, other.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(reason, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MarkBreachNotABreachRequest {\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
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
