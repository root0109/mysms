// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;

/**
 * The caller's active nominee (DPDP Act s.14): who may exercise the caller's rights on their death or incapacity. The name and contact are shown masked; they are never returned in clear.
 */

public class MyNominee {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID nomineePersonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String nomineeNameMasked;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String nomineeContactMasked;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime activatedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String etag;

  public MyNominee() {
    super();
  }

  public MyNominee id(UUID id) {
    this.id = id;
    return this;
  }

  /**
   * The nomination's id. Output only.
   * @return id
   */
  @NotNull @Valid 
  @JsonProperty("id")
  public UUID getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(UUID id) {
    this.id = id;
  }

  public MyNominee nomineePersonId(@Nullable UUID nomineePersonId) {
    this.nomineePersonId = nomineePersonId;
    return this;
  }

  /**
   * The nominee's person when they are known to the school and sign in.
   * @return nomineePersonId
   */
  @Valid 
  @JsonProperty("nomineePersonId")
  public @Nullable UUID getNomineePersonId() {
    return nomineePersonId;
  }

  @JsonProperty("nomineePersonId")
  public void setNomineePersonId(@Nullable UUID nomineePersonId) {
    this.nomineePersonId = nomineePersonId;
  }

  public MyNominee nomineeNameMasked(String nomineeNameMasked) {
    this.nomineeNameMasked = nomineeNameMasked;
    return this;
  }

  /**
   * The nominee's name, masked (••••, then its last four characters).
   * @return nomineeNameMasked
   */
  @NotNull 
  @JsonProperty("nomineeNameMasked")
  public String getNomineeNameMasked() {
    return nomineeNameMasked;
  }

  @JsonProperty("nomineeNameMasked")
  public void setNomineeNameMasked(String nomineeNameMasked) {
    this.nomineeNameMasked = nomineeNameMasked;
  }

  public MyNominee nomineeContactMasked(String nomineeContactMasked) {
    this.nomineeContactMasked = nomineeContactMasked;
    return this;
  }

  /**
   * The nominee's phone number or e-mail address, masked.
   * @return nomineeContactMasked
   */
  @NotNull 
  @JsonProperty("nomineeContactMasked")
  public String getNomineeContactMasked() {
    return nomineeContactMasked;
  }

  @JsonProperty("nomineeContactMasked")
  public void setNomineeContactMasked(String nomineeContactMasked) {
    this.nomineeContactMasked = nomineeContactMasked;
  }

  public MyNominee activatedAt(@Nullable OffsetDateTime activatedAt) {
    this.activatedAt = activatedAt;
    return this;
  }

  /**
   * When the nominee became able to act for the caller (their nomination request was fulfilled); absent until then.
   * @return activatedAt
   */
  @Valid 
  @JsonProperty("activatedAt")
  public @Nullable OffsetDateTime getActivatedAt() {
    return activatedAt;
  }

  @JsonProperty("activatedAt")
  public void setActivatedAt(@Nullable OffsetDateTime activatedAt) {
    this.activatedAt = activatedAt;
  }

  public MyNominee createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * When the nominee was named.
   * @return createdAt
   */
  @NotNull @Valid 
  @JsonProperty("createdAt")
  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  @JsonProperty("createdAt")
  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public MyNominee etag(String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Concurrency token; send it back to replace or revoke the nominee.
   * @return etag
   */
  @NotNull 
  @JsonProperty("etag")
  public String getEtag() {
    return etag;
  }

  @JsonProperty("etag")
  public void setEtag(String etag) {
    this.etag = etag;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MyNominee other = (MyNominee) o;
    return Objects.equals(this.id, other.id) &&
        Objects.equals(this.nomineePersonId, other.nomineePersonId) &&
        Objects.equals(this.nomineeNameMasked, other.nomineeNameMasked) &&
        Objects.equals(this.nomineeContactMasked, other.nomineeContactMasked) &&
        Objects.equals(this.activatedAt, other.activatedAt) &&
        Objects.equals(this.createdAt, other.createdAt) &&
        Objects.equals(this.etag, other.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, nomineePersonId, nomineeNameMasked, nomineeContactMasked, activatedAt, createdAt, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MyNominee {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    nomineePersonId: ").append(toIndentedString(nomineePersonId)).append("\n");
    sb.append("    nomineeNameMasked: ").append(toIndentedString(nomineeNameMasked)).append("\n");
    sb.append("    nomineeContactMasked: ").append(toIndentedString(nomineeContactMasked)).append("\n");
    sb.append("    activatedAt: ").append(toIndentedString(activatedAt)).append("\n");
    sb.append("    createdAt: ").append(toIndentedString(createdAt)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
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
