// Owned API contract of people (JAVA-CODING-RULES §7); first materialized from api/openapi/people.yaml on 2026-11-10.
package com.mysms.people.api.model;

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
 * Fields of EmergencyContact that can be updated (partial update; see updateMask).
 */

public class EmergencyContactUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID contactPersonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String contactName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String relationship;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String phone;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer priority;

  public EmergencyContactUpdate contactPersonId(@Nullable UUID contactPersonId) {
    this.contactPersonId = contactPersonId;
    return this;
  }

  /**
   * Contact, when recorded as a person.
   * @return contactPersonId
   */
  @Valid 
  @JsonProperty("contactPersonId")
  public @Nullable UUID getContactPersonId() {
    return contactPersonId;
  }

  @JsonProperty("contactPersonId")
  public void setContactPersonId(@Nullable UUID contactPersonId) {
    this.contactPersonId = contactPersonId;
  }

  public EmergencyContactUpdate contactName(@Nullable String contactName) {
    this.contactName = contactName;
    return this;
  }

  /**
   * Contact name (when not a person record).
   * @return contactName
   */
  @Size(max = 150) 
  @JsonProperty("contactName")
  public @Nullable String getContactName() {
    return contactName;
  }

  @JsonProperty("contactName")
  public void setContactName(@Nullable String contactName) {
    this.contactName = contactName;
  }

  public EmergencyContactUpdate relationship(@Nullable String relationship) {
    this.relationship = relationship;
    return this;
  }

  /**
   * Relationship to the person.
   * @return relationship
   */
  @Size(max = 60) 
  @JsonProperty("relationship")
  public @Nullable String getRelationship() {
    return relationship;
  }

  @JsonProperty("relationship")
  public void setRelationship(@Nullable String relationship) {
    this.relationship = relationship;
  }

  public EmergencyContactUpdate phone(@Nullable String phone) {
    this.phone = phone;
    return this;
  }

  /**
   * Contact number (may come from the linked person instead).
   * @return phone
   */
  @Size(max = 20) 
  @JsonProperty("phone")
  public @Nullable String getPhone() {
    return phone;
  }

  @JsonProperty("phone")
  public void setPhone(@Nullable String phone) {
    this.phone = phone;
  }

  public EmergencyContactUpdate priority(@Nullable Integer priority) {
    this.priority = priority;
    return this;
  }

  /**
   * Call order (1 = first).
   * @return priority
   */
  
  @JsonProperty("priority")
  public @Nullable Integer getPriority() {
    return priority;
  }

  @JsonProperty("priority")
  public void setPriority(@Nullable Integer priority) {
    this.priority = priority;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EmergencyContactUpdate emergencyContactUpdate = (EmergencyContactUpdate) o;
    return Objects.equals(this.contactPersonId, emergencyContactUpdate.contactPersonId) &&
        Objects.equals(this.contactName, emergencyContactUpdate.contactName) &&
        Objects.equals(this.relationship, emergencyContactUpdate.relationship) &&
        Objects.equals(this.phone, emergencyContactUpdate.phone) &&
        Objects.equals(this.priority, emergencyContactUpdate.priority);
  }

  @Override
  public int hashCode() {
    return Objects.hash(contactPersonId, contactName, relationship, phone, priority);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EmergencyContactUpdate {\n");
    sb.append("    contactPersonId: ").append(toIndentedString(contactPersonId)).append("\n");
    sb.append("    contactName: ").append(toIndentedString(contactName)).append("\n");
    sb.append("    relationship: ").append(toIndentedString(relationship)).append("\n");
    sb.append("    phone: ").append(toIndentedString(phone)).append("\n");
    sb.append("    priority: ").append(toIndentedString(priority)).append("\n");
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

