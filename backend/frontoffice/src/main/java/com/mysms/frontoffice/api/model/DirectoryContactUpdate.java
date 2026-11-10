// Owned API contract of frontoffice (JAVA-CODING-RULES §7); first materialized from api/openapi/frontoffice.yaml on 2026-11-10.
package com.mysms.frontoffice.api.model;

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
 * Fields of DirectoryContact that can be updated (partial update; see updateMask).
 */

public class DirectoryContactUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String designation;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String phone;

  public DirectoryContactUpdate institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  @Valid 
  @JsonProperty("institutionId")
  public @Nullable UUID getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
  }

  public DirectoryContactUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Name.
   * @return name
   */
  @Size(max = 150) 
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public DirectoryContactUpdate designation(@Nullable String designation) {
    this.designation = designation;
    return this;
  }

  /**
   * Designation.
   * @return designation
   */
  @Size(max = 120) 
  @JsonProperty("designation")
  public @Nullable String getDesignation() {
    return designation;
  }

  @JsonProperty("designation")
  public void setDesignation(@Nullable String designation) {
    this.designation = designation;
  }

  public DirectoryContactUpdate phone(@Nullable String phone) {
    this.phone = phone;
    return this;
  }

  /**
   * Phone.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DirectoryContactUpdate directoryContactUpdate = (DirectoryContactUpdate) o;
    return Objects.equals(this.institutionId, directoryContactUpdate.institutionId) &&
        Objects.equals(this.name, directoryContactUpdate.name) &&
        Objects.equals(this.designation, directoryContactUpdate.designation) &&
        Objects.equals(this.phone, directoryContactUpdate.phone);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, name, designation, phone);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DirectoryContactUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    designation: ").append(toIndentedString(designation)).append("\n");
    sb.append("    phone: ").append(toIndentedString(phone)).append("\n");
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

