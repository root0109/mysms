// Owned API contract of org (JAVA-CODING-RULES §7); first materialized from api/openapi/org.yaml on 2026-11-10.
package com.mysms.org.api.model;

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
 * Fields of InstitutionAttribute that can be updated (partial update; see updateMask).
 */

public class InstitutionAttributeUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String attrKey;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String attrValue;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID attributeDefinitionId;

  public InstitutionAttributeUpdate attrKey(@Nullable String attrKey) {
    this.attrKey = attrKey;
    return this;
  }

  /**
   * Attribute key.
   * @return attrKey
   */
  @Size(max = 120) 
  @JsonProperty("attrKey")
  public @Nullable String getAttrKey() {
    return attrKey;
  }

  @JsonProperty("attrKey")
  public void setAttrKey(@Nullable String attrKey) {
    this.attrKey = attrKey;
  }

  public InstitutionAttributeUpdate attrValue(@Nullable String attrValue) {
    this.attrValue = attrValue;
    return this;
  }

  /**
   * Attribute value.
   * @return attrValue
   */
  
  @JsonProperty("attrValue")
  public @Nullable String getAttrValue() {
    return attrValue;
  }

  @JsonProperty("attrValue")
  public void setAttrValue(@Nullable String attrValue) {
    this.attrValue = attrValue;
  }

  public InstitutionAttributeUpdate attributeDefinitionId(@Nullable UUID attributeDefinitionId) {
    this.attributeDefinitionId = attributeDefinitionId;
    return this;
  }

  /**
   * Attribute definition (NULL = free key/value).
   * @return attributeDefinitionId
   */
  @Valid 
  @JsonProperty("attributeDefinitionId")
  public @Nullable UUID getAttributeDefinitionId() {
    return attributeDefinitionId;
  }

  @JsonProperty("attributeDefinitionId")
  public void setAttributeDefinitionId(@Nullable UUID attributeDefinitionId) {
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

