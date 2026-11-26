// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of PlagiarismUserLink that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class PlagiarismUserLinkUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 100) String> accountRef;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 100) String> providerUserRef;

  public PlagiarismUserLinkUpdate userId(@Nullable Optional<UUID> userId) {
    this.userId = userId;
    return this;
  }

  /**
   * User.
   * @return userId
   */
  
  @JsonProperty("userId")
  public @Nullable Optional<UUID> getUserId() {
    return userId;
  }

  @JsonProperty("userId")
  public void setUserId(@Nullable Optional<UUID> userId) {
    this.userId = userId;
  }

  public PlagiarismUserLinkUpdate accountRef(@Nullable Optional<String> accountRef) {
    this.accountRef = accountRef;
    return this;
  }

  /**
   * Provider account id.
   * @return accountRef
   */
  
  @JsonProperty("accountRef")
  public @Nullable Optional<String> getAccountRef() {
    return accountRef;
  }

  @JsonProperty("accountRef")
  public void setAccountRef(@Nullable Optional<String> accountRef) {
    this.accountRef = accountRef;
  }

  public PlagiarismUserLinkUpdate providerUserRef(@Nullable Optional<String> providerUserRef) {
    this.providerUserRef = providerUserRef;
    return this;
  }

  /**
   * Provider user id.
   * @return providerUserRef
   */
  
  @JsonProperty("providerUserRef")
  public @Nullable Optional<String> getProviderUserRef() {
    return providerUserRef;
  }

  @JsonProperty("providerUserRef")
  public void setProviderUserRef(@Nullable Optional<String> providerUserRef) {
    this.providerUserRef = providerUserRef;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PlagiarismUserLinkUpdate plagiarismUserLinkUpdate = (PlagiarismUserLinkUpdate) o;
    return Objects.equals(this.userId, plagiarismUserLinkUpdate.userId) &&
        Objects.equals(this.accountRef, plagiarismUserLinkUpdate.accountRef) &&
        Objects.equals(this.providerUserRef, plagiarismUserLinkUpdate.providerUserRef);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, accountRef, providerUserRef);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PlagiarismUserLinkUpdate {\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    accountRef: ").append(toIndentedString(accountRef)).append("\n");
    sb.append("    providerUserRef: ").append(toIndentedString(providerUserRef)).append("\n");
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

