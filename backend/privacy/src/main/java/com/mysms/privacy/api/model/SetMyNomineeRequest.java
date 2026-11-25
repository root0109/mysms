// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import java.util.UUID;
import org.springframework.lang.Nullable;

/**
 * Body of SetMyNominee: the person the caller names as their nominee. Naming a nominee replaces the active one.
 */

public class SetMyNomineeRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String nomineeName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String nomineeContact;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID nomineePersonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  public SetMyNomineeRequest() {
    super();
  }

  public SetMyNomineeRequest nomineeName(String nomineeName) {
    this.nomineeName = nomineeName;
    return this;
  }

  /**
   * The nominee's full name. Input only: stored encrypted, never returned in clear.
   * @return nomineeName
   */
  @NotNull @NotBlank @Size(max = 200) 
  @JsonProperty("nomineeName")
  public String getNomineeName() {
    return nomineeName;
  }

  @JsonProperty("nomineeName")
  public void setNomineeName(String nomineeName) {
    this.nomineeName = nomineeName;
  }

  public SetMyNomineeRequest nomineeContact(String nomineeContact) {
    this.nomineeContact = nomineeContact;
    return this;
  }

  /**
   * The nominee's phone number or e-mail address. Input only: stored encrypted, never returned in clear.
   * @return nomineeContact
   */
  @NotNull @NotBlank @Size(max = 200) 
  @JsonProperty("nomineeContact")
  public String getNomineeContact() {
    return nomineeContact;
  }

  @JsonProperty("nomineeContact")
  public void setNomineeContact(String nomineeContact) {
    this.nomineeContact = nomineeContact;
  }

  public SetMyNomineeRequest nomineePersonId(@Nullable UUID nomineePersonId) {
    this.nomineePersonId = nomineePersonId;
    return this;
  }

  /**
   * The nominee's person, when they are known to the school and will sign in to act for the caller.
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

  public SetMyNomineeRequest etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * The active nominee's etag when one is being replaced (optional but recommended).
   * @return etag
   */
   
  @JsonProperty("etag")
  public @Nullable String getEtag() {
    return etag;
  }

  @JsonProperty("etag")
  public void setEtag(@Nullable String etag) {
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
    SetMyNomineeRequest other = (SetMyNomineeRequest) o;
    return Objects.equals(this.nomineeName, other.nomineeName) &&
        Objects.equals(this.nomineeContact, other.nomineeContact) &&
        Objects.equals(this.nomineePersonId, other.nomineePersonId) &&
        Objects.equals(this.etag, other.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nomineeName, nomineeContact, nomineePersonId, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SetMyNomineeRequest {\n");
    sb.append("    nomineeName: ").append(toIndentedString(nomineeName)).append("\n");
    sb.append("    nomineeContact: ").append(toIndentedString(nomineeContact)).append("\n");
    sb.append("    nomineePersonId: ").append(toIndentedString(nomineePersonId)).append("\n");
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
