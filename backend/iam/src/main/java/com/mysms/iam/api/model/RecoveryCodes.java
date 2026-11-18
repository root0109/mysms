// Owned API contract of iam.
package com.mysms.iam.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * RecoveryCodes
 */

public class RecoveryCodes {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<String> recoveryCodes = new ArrayList<>();

  public RecoveryCodes recoveryCodes(List<String> recoveryCodes) {
    this.recoveryCodes = recoveryCodes;
    return this;
  }

  public RecoveryCodes addRecoveryCodesItem(String recoveryCodesItem) {
    if (this.recoveryCodes == null) {
      this.recoveryCodes = new ArrayList<>();
    }
    this.recoveryCodes.add(recoveryCodesItem);
    return this;
  }

  /**
   * One-time recovery codes; shown once.
   * @return recoveryCodes
   */
  
  @JsonProperty("recoveryCodes")
  public List<String> getRecoveryCodes() {
    return recoveryCodes;
  }

  @JsonProperty("recoveryCodes")
  public void setRecoveryCodes(List<String> recoveryCodes) {
    this.recoveryCodes = recoveryCodes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RecoveryCodes recoveryCodes = (RecoveryCodes) o;
    return Objects.equals(this.recoveryCodes, recoveryCodes.recoveryCodes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(recoveryCodes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RecoveryCodes {\n");
    sb.append("    recoveryCodes: ").append(toIndentedString(recoveryCodes)).append("\n");
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

