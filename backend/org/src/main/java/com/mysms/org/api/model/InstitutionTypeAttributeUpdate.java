// Owned API contract of org.
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
 * Fields of InstitutionTypeAttribute that can be updated (partial update; see updateMask).
 */

public class InstitutionTypeAttributeUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID attributeDefinitionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isRequired;

  public InstitutionTypeAttributeUpdate attributeDefinitionId(@Nullable UUID attributeDefinitionId) {
    this.attributeDefinitionId = attributeDefinitionId;
    return this;
  }

  /**
   * Attribute.
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

  public InstitutionTypeAttributeUpdate isRequired(@Nullable Boolean isRequired) {
    this.isRequired = isRequired;
    return this;
  }

  /**
   * Value is mandatory.
   * @return isRequired
   */
  
  @JsonProperty("isRequired")
  public @Nullable Boolean getIsRequired() {
    return isRequired;
  }

  @JsonProperty("isRequired")
  public void setIsRequired(@Nullable Boolean isRequired) {
    this.isRequired = isRequired;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InstitutionTypeAttributeUpdate institutionTypeAttributeUpdate = (InstitutionTypeAttributeUpdate) o;
    return Objects.equals(this.attributeDefinitionId, institutionTypeAttributeUpdate.attributeDefinitionId) &&
        Objects.equals(this.isRequired, institutionTypeAttributeUpdate.isRequired);
  }

  @Override
  public int hashCode() {
    return Objects.hash(attributeDefinitionId, isRequired);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InstitutionTypeAttributeUpdate {\n");
    sb.append("    attributeDefinitionId: ").append(toIndentedString(attributeDefinitionId)).append("\n");
    sb.append("    isRequired: ").append(toIndentedString(isRequired)).append("\n");
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

