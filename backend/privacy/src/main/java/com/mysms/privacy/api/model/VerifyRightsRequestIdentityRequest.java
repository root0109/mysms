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
 * Body of VerifyRightsRequestIdentity: how staff established the requester's identity.
 */

public class VerifyRightsRequestIdentityRequest {

  /**
   * in_person (seen by a staff member) or document (an identity document checked).
   */
  public enum IdentityCheckEnum {
    DOCUMENT("document"),
    
    IN_PERSON("in_person");

    private final String value;

    IdentityCheckEnum(String value) {
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
    public static IdentityCheckEnum fromValue(String value) {
      for (IdentityCheckEnum b : IdentityCheckEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private IdentityCheckEnum identityCheck;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  public VerifyRightsRequestIdentityRequest() {
    super();
  }

  public VerifyRightsRequestIdentityRequest identityCheck(IdentityCheckEnum identityCheck) {
    this.identityCheck = identityCheck;
    return this;
  }

  /**
   * in_person (seen by a staff member) or document (an identity document checked).
   * @return identityCheck
   */
  @NotNull 
  @JsonProperty("identityCheck")
  public IdentityCheckEnum getIdentityCheck() {
    return identityCheck;
  }

  @JsonProperty("identityCheck")
  public void setIdentityCheck(IdentityCheckEnum identityCheck) {
    this.identityCheck = identityCheck;
  }

  public VerifyRightsRequestIdentityRequest etag(@Nullable String etag) {
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
    VerifyRightsRequestIdentityRequest other = (VerifyRightsRequestIdentityRequest) o;
    return Objects.equals(this.identityCheck, other.identityCheck) &&
        Objects.equals(this.etag, other.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(identityCheck, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class VerifyRightsRequestIdentityRequest {\n");
    sb.append("    identityCheck: ").append(toIndentedString(identityCheck)).append("\n");
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
