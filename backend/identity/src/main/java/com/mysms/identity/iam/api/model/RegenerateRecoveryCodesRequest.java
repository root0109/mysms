// Owned API contract of iam.
package com.mysms.identity.iam.api.model;

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
 * RegenerateRecoveryCodesRequest
 */

public class RegenerateRecoveryCodesRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String code;

  public RegenerateRecoveryCodesRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RegenerateRecoveryCodesRequest(String code) {
    this.code = code;
  }

  public RegenerateRecoveryCodesRequest code(String code) {
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
    RegenerateRecoveryCodesRequest regenerateRecoveryCodesRequest = (RegenerateRecoveryCodesRequest) o;
    return Objects.equals(this.code, regenerateRecoveryCodesRequest.code);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RegenerateRecoveryCodesRequest {\n");
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

