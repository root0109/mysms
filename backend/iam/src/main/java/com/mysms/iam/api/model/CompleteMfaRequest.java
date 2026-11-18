// Owned API contract of iam.
package com.mysms.iam.api.model;

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
 * CompleteMfaRequest
 */

public class CompleteMfaRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String mfaToken;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String code;

  public CompleteMfaRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CompleteMfaRequest(String mfaToken, String code) {
    this.mfaToken = mfaToken;
    this.code = code;
  }

  public CompleteMfaRequest mfaToken(String mfaToken) {
    this.mfaToken = mfaToken;
    return this;
  }

  /**
   * Token from Login (or from EnrollMfa verification).
   * @return mfaToken
   */
  @NotNull 
  @JsonProperty("mfaToken")
  public String getMfaToken() {
    return mfaToken;
  }

  @JsonProperty("mfaToken")
  public void setMfaToken(String mfaToken) {
    this.mfaToken = mfaToken;
  }

  public CompleteMfaRequest code(String code) {
    this.code = code;
    return this;
  }

  /**
   * Current TOTP/OTP or a recovery code.
   * @return code
   */
  @NotNull @Size(max = 12) 
  @JsonProperty("code")
  public String getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(String code) {
    this.code = code;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CompleteMfaRequest completeMfaRequest = (CompleteMfaRequest) o;
    return Objects.equals(this.mfaToken, completeMfaRequest.mfaToken) &&
        Objects.equals(this.code, completeMfaRequest.code);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mfaToken, code);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CompleteMfaRequest {\n");
    sb.append("    mfaToken: ").append(toIndentedString(mfaToken)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
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

