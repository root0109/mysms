// Owned API contract of iam.
package com.mysms.identity.iam.api.model;

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
 * DisableMfaRequest
 */

public class DisableMfaRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID mfaCredentialId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String code;

  public DisableMfaRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public DisableMfaRequest(UUID mfaCredentialId, String code) {
    this.mfaCredentialId = mfaCredentialId;
    this.code = code;
  }

  public DisableMfaRequest mfaCredentialId(UUID mfaCredentialId) {
    this.mfaCredentialId = mfaCredentialId;
    return this;
  }

  /**
   * Credential to disable.
   * @return mfaCredentialId
   */
  @NotNull @Valid 
  @JsonProperty("mfaCredentialId")
  public UUID getMfaCredentialId() {
    return mfaCredentialId;
  }

  @JsonProperty("mfaCredentialId")
  public void setMfaCredentialId(UUID mfaCredentialId) {
    this.mfaCredentialId = mfaCredentialId;
  }

  public DisableMfaRequest code(String code) {
    this.code = code;
    return this;
  }

  /**
   * Current code or recovery code proving possession.
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
    DisableMfaRequest disableMfaRequest = (DisableMfaRequest) o;
    return Objects.equals(this.mfaCredentialId, disableMfaRequest.mfaCredentialId) &&
        Objects.equals(this.code, disableMfaRequest.code);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mfaCredentialId, code);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DisableMfaRequest {\n");
    sb.append("    mfaCredentialId: ").append(toIndentedString(mfaCredentialId)).append("\n");
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

