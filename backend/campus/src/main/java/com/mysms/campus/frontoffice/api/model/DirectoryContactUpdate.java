// Owned API contract of frontoffice.
package com.mysms.campus.frontoffice.api.model;

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
 * Fields of DirectoryContact that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class DirectoryContactUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 150) String> name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 120) String> designation;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 20) String> phone;

  public DirectoryContactUpdate institutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  
  @JsonProperty("institutionId")
  public @Nullable Optional<UUID> getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
  }

  public DirectoryContactUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Name.
   * @return name
   */
  
  @JsonProperty("name")
  public @Nullable Optional<String> getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable Optional<String> name) {
    this.name = name;
  }

  public DirectoryContactUpdate designation(@Nullable Optional<String> designation) {
    this.designation = designation;
    return this;
  }

  /**
   * Designation.
   * @return designation
   */
  
  @JsonProperty("designation")
  public @Nullable Optional<String> getDesignation() {
    return designation;
  }

  @JsonProperty("designation")
  public void setDesignation(@Nullable Optional<String> designation) {
    this.designation = designation;
  }

  public DirectoryContactUpdate phone(@Nullable Optional<String> phone) {
    this.phone = phone;
    return this;
  }

  /**
   * Phone.
   * @return phone
   */
  
  @JsonProperty("phone")
  public @Nullable Optional<String> getPhone() {
    return phone;
  }

  @JsonProperty("phone")
  public void setPhone(@Nullable Optional<String> phone) {
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

