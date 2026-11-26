// Owned API contract of iam.
package com.mysms.identity.iam.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * CreateApiTokenRequest
 */

public class CreateApiTokenRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<String> abilities = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime expiresAt;

  public CreateApiTokenRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateApiTokenRequest(String name) {
    this.name = name;
  }

  public CreateApiTokenRequest name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Label.
   * @return name
   */
  @NotNull 
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public CreateApiTokenRequest abilities(List<String> abilities) {
    this.abilities = abilities;
    return this;
  }

  public CreateApiTokenRequest addAbilitiesItem(String abilitiesItem) {
    if (this.abilities == null) {
      this.abilities = new ArrayList<>();
    }
    this.abilities.add(abilitiesItem);
    return this;
  }

  /**
   * Permission codes the token is limited to (subset of the user's own).
   * @return abilities
   */
  
  @JsonProperty("abilities")
  public List<String> getAbilities() {
    return abilities;
  }

  @JsonProperty("abilities")
  public void setAbilities(List<String> abilities) {
    this.abilities = abilities;
  }

  public CreateApiTokenRequest expiresAt(@Nullable OffsetDateTime expiresAt) {
    this.expiresAt = expiresAt;
    return this;
  }

  /**
   * Expiry (default 90 days, maximum 1 year).
   * @return expiresAt
   */
  @Valid 
  @JsonProperty("expiresAt")
  public @Nullable OffsetDateTime getExpiresAt() {
    return expiresAt;
  }

  @JsonProperty("expiresAt")
  public void setExpiresAt(@Nullable OffsetDateTime expiresAt) {
    this.expiresAt = expiresAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateApiTokenRequest createApiTokenRequest = (CreateApiTokenRequest) o;
    return Objects.equals(this.name, createApiTokenRequest.name) &&
        Objects.equals(this.abilities, createApiTokenRequest.abilities) &&
        Objects.equals(this.expiresAt, createApiTokenRequest.expiresAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, abilities, expiresAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateApiTokenRequest {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    abilities: ").append(toIndentedString(abilities)).append("\n");
    sb.append("    expiresAt: ").append(toIndentedString(expiresAt)).append("\n");
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

