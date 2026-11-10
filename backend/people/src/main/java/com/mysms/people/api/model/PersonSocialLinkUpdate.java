// Owned API contract of people (JAVA-CODING-RULES §7); first materialized from api/openapi/people.yaml on 2026-11-10.
package com.mysms.people.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of PersonSocialLink that can be updated (partial update; see updateMask).
 */

public class PersonSocialLinkUpdate {

  /**
   * Network (from the fixed network catalogue).
   */
  public enum NetworkEnum {
    FACEBOOK("facebook"),
    
    TWITTER("twitter"),
    
    LINKEDIN("linkedin"),
    
    INSTAGRAM("instagram"),
    
    YOUTUBE("youtube"),
    
    SKYPE("skype"),
    
    WHATSAPP("whatsapp"),
    
    TELEGRAM("telegram"),
    
    GITHUB("github"),
    
    WEBSITE("website"),
    
    OTHER("other");

    private final String value;

    NetworkEnum(String value) {
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
    public static NetworkEnum fromValue(String value) {
      for (NetworkEnum b : NetworkEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable NetworkEnum network;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String handle;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  public PersonSocialLinkUpdate network(@Nullable NetworkEnum network) {
    this.network = network;
    return this;
  }

  /**
   * Network (from the fixed network catalogue).
   * @return network
   */
  
  @JsonProperty("network")
  public @Nullable NetworkEnum getNetwork() {
    return network;
  }

  @JsonProperty("network")
  public void setNetwork(@Nullable NetworkEnum network) {
    this.network = network;
  }

  public PersonSocialLinkUpdate handle(@Nullable String handle) {
    this.handle = handle;
    return this;
  }

  /**
   * Handle, profile URL or number.
   * @return handle
   */
  @Size(max = 255) 
  @JsonProperty("handle")
  public @Nullable String getHandle() {
    return handle;
  }

  @JsonProperty("handle")
  public void setHandle(@Nullable String handle) {
    this.handle = handle;
  }

  public PersonSocialLinkUpdate sortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Display order.
   * @return sortOrder
   */
  
  @JsonProperty("sortOrder")
  public @Nullable Integer getSortOrder() {
    return sortOrder;
  }

  @JsonProperty("sortOrder")
  public void setSortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PersonSocialLinkUpdate personSocialLinkUpdate = (PersonSocialLinkUpdate) o;
    return Objects.equals(this.network, personSocialLinkUpdate.network) &&
        Objects.equals(this.handle, personSocialLinkUpdate.handle) &&
        Objects.equals(this.sortOrder, personSocialLinkUpdate.sortOrder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(network, handle, sortOrder);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PersonSocialLinkUpdate {\n");
    sb.append("    network: ").append(toIndentedString(network)).append("\n");
    sb.append("    handle: ").append(toIndentedString(handle)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
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

