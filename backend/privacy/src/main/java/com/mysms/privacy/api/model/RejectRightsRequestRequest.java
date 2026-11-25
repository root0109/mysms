// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import org.springframework.lang.Nullable;

/**
 * Body of RejectRightsRequest: why the request is rejected (the requester may appeal it as a grievance).
 */

public class RejectRightsRequestRequest {

  /**
   * identity_not_verified, not_entitled, duplicate or other.
   */
  public enum ReasonCodeEnum {
    DUPLICATE("duplicate"),
    
    IDENTITY_NOT_VERIFIED("identity_not_verified"),
    
    NOT_ENTITLED("not_entitled"),
    
    OTHER("other");

    private final String value;

    ReasonCodeEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static ReasonCodeEnum fromValue(String value) {
      for (ReasonCodeEnum b : ReasonCodeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private ReasonCodeEnum reasonCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  public RejectRightsRequestRequest() {
    super();
  }

  public RejectRightsRequestRequest reasonCode(ReasonCodeEnum reasonCode) {
    this.reasonCode = reasonCode;
    return this;
  }

  /**
   * identity_not_verified, not_entitled, duplicate or other.
   * @return reasonCode
   */
  @NotNull 
  @JsonProperty("reasonCode")
  public ReasonCodeEnum getReasonCode() {
    return reasonCode;
  }

  @JsonProperty("reasonCode")
  public void setReasonCode(ReasonCodeEnum reasonCode) {
    this.reasonCode = reasonCode;
  }

  public RejectRightsRequestRequest etag(@Nullable String etag) {
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
    RejectRightsRequestRequest other = (RejectRightsRequestRequest) o;
    return Objects.equals(this.reasonCode, other.reasonCode) &&
        Objects.equals(this.etag, other.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(reasonCode, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RejectRightsRequestRequest {\n");
    sb.append("    reasonCode: ").append(toIndentedString(reasonCode)).append("\n");
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
