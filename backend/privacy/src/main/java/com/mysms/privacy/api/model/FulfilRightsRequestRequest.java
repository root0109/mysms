// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import org.springframework.lang.Nullable;

/**
 * Body of StartRightsRequestFulfilment and FulfilWithdrawAllRequest: the request's etag (required).
 */

public class FulfilRightsRequestRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String etag;

  public FulfilRightsRequestRequest() {
    super();
  }

  public FulfilRightsRequestRequest etag(String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Concurrency token from the last read (required: the request fulfilled is the version the staff reviewed).
   * @return etag
   */
  @NotNull @NotBlank 
  @JsonProperty("etag")
  public String getEtag() {
    return etag;
  }

  @JsonProperty("etag")
  public void setEtag(String etag) {
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
    FulfilRightsRequestRequest other = (FulfilRightsRequestRequest) o;
    return Objects.equals(this.etag, other.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FulfilRightsRequestRequest {\n");
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
