// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;

/**
 * Body of ExtendRightsRequest: a later response deadline and why.
 */

public class ExtendRightsRequestRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime dueAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String reason;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  public ExtendRightsRequestRequest() {
    super();
  }

  public ExtendRightsRequestRequest dueAt(OffsetDateTime dueAt) {
    this.dueAt = dueAt;
    return this;
  }

  /**
   * The new response deadline; later than the current one.
   * @return dueAt
   */
  @NotNull @Valid 
  @JsonProperty("dueAt")
  public OffsetDateTime getDueAt() {
    return dueAt;
  }

  @JsonProperty("dueAt")
  public void setDueAt(OffsetDateTime dueAt) {
    this.dueAt = dueAt;
  }

  public ExtendRightsRequestRequest reason(String reason) {
    this.reason = reason;
    return this;
  }

  /**
   * Why the deadline is extended; kept on the request and in the audit trail.
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

  public ExtendRightsRequestRequest etag(@Nullable String etag) {
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
    ExtendRightsRequestRequest other = (ExtendRightsRequestRequest) o;
    return Objects.equals(this.dueAt, other.dueAt) &&
        Objects.equals(this.reason, other.reason) &&
        Objects.equals(this.etag, other.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(dueAt, reason, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExtendRightsRequestRequest {\n");
    sb.append("    dueAt: ").append(toIndentedString(dueAt)).append("\n");
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
