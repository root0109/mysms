// Owned API contract of org.
package com.mysms.platform.org.api.model;

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
 * Fields of InstitutionAttribute that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class InstitutionAttributeUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 120) String> attrKey;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> attrValue;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> attributeDefinitionId;

  public InstitutionAttributeUpdate attrKey(@Nullable Optional<String> attrKey) {
    this.attrKey = attrKey;
    return this;
  }

  /**
   * Attribute key.
   * @return attrKey
   */
  
  @JsonProperty("attrKey")
  public @Nullable Optional<String> getAttrKey() {
    return attrKey;
  }

  @JsonProperty("attrKey")
  public void setAttrKey(@Nullable Optional<String> attrKey) {
    this.attrKey = attrKey;
  }

  public InstitutionAttributeUpdate attrValue(@Nullable Optional<String> attrValue) {
    this.attrValue = attrValue;
    return this;
  }

  /**
   * Attribute value.
   * @return attrValue
   */
  
  @JsonProperty("attrValue")
  public @Nullable Optional<String> getAttrValue() {
    return attrValue;
  }

  @JsonProperty("attrValue")
  public void setAttrValue(@Nullable Optional<String> attrValue) {
    this.attrValue = attrValue;
  }

  public InstitutionAttributeUpdate attributeDefinitionId(@Nullable Optional<UUID> attributeDefinitionId) {
    this.attributeDefinitionId = attributeDefinitionId;
    return this;
  }

  /**
   * Attribute definition (NULL = free key/value).
   * @return attributeDefinitionId
   */
  
  @JsonProperty("attributeDefinitionId")
  public @Nullable Optional<UUID> getAttributeDefinitionId() {
    return attributeDefinitionId;
  }

  @JsonProperty("attributeDefinitionId")
  public void setAttributeDefinitionId(@Nullable Optional<UUID> attributeDefinitionId) {
    this.attributeDefinitionId = attributeDefinitionId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InstitutionAttributeUpdate institutionAttributeUpdate = (InstitutionAttributeUpdate) o;
    return Objects.equals(this.attrKey, institutionAttributeUpdate.attrKey) &&
        Objects.equals(this.attrValue, institutionAttributeUpdate.attrValue) &&
        Objects.equals(this.attributeDefinitionId, institutionAttributeUpdate.attributeDefinitionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(attrKey, attrValue, attributeDefinitionId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InstitutionAttributeUpdate {\n");
    sb.append("    attrKey: ").append(toIndentedString(attrKey)).append("\n");
    sb.append("    attrValue: ").append(toIndentedString(attrValue)).append("\n");
    sb.append("    attributeDefinitionId: ").append(toIndentedString(attributeDefinitionId)).append("\n");
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

