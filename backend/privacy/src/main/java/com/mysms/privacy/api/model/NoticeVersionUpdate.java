// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.lang.Nullable;

/**
 * Fields of NoticeVersion that can be updated while it is a draft (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class NoticeVersionUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> requiresReconsent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(min = 1) List<String>> purposeCodes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Valid GrievanceContact> grievanceContact;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> documentFileId;

  public NoticeVersionUpdate requiresReconsent(@Nullable Optional<Boolean> requiresReconsent) {
    this.requiresReconsent = requiresReconsent;
    return this;
  }

  /**
   * Whether consents given under earlier versions must be asked for again once this version is published.
   * @return requiresReconsent
   */
  
  @JsonProperty("requiresReconsent")
  public @Nullable Optional<Boolean> getRequiresReconsent() {
    return requiresReconsent;
  }

  @JsonProperty("requiresReconsent")
  public void setRequiresReconsent(@Nullable Optional<Boolean> requiresReconsent) {
    this.requiresReconsent = requiresReconsent;
  }

  public NoticeVersionUpdate purposeCodes(@Nullable Optional<List<String>> purposeCodes) {
    this.purposeCodes = purposeCodes;
    return this;
  }

  /**
   * Codes of the active catalogue purposes the version asks consent for.
   * @return purposeCodes
   */
  
  @JsonProperty("purposeCodes")
  public @Nullable Optional<List<String>> getPurposeCodes() {
    return purposeCodes;
  }

  @JsonProperty("purposeCodes")
  public void setPurposeCodes(@Nullable Optional<List<String>> purposeCodes) {
    this.purposeCodes = purposeCodes;
  }

  public NoticeVersionUpdate grievanceContact(@Nullable Optional<GrievanceContact> grievanceContact) {
    this.grievanceContact = grievanceContact;
    return this;
  }

  /**
   * The school's grievance officer the version names.
   * @return grievanceContact
   */
  
  @JsonProperty("grievanceContact")
  public @Nullable Optional<GrievanceContact> getGrievanceContact() {
    return grievanceContact;
  }

  @JsonProperty("grievanceContact")
  public void setGrievanceContact(@Nullable Optional<GrievanceContact> grievanceContact) {
    this.grievanceContact = grievanceContact;
  }

  public NoticeVersionUpdate documentFileId(@Nullable Optional<UUID> documentFileId) {
    this.documentFileId = documentFileId;
    return this;
  }

  /**
   * Optional signed PDF of the version (a platform file).
   * @return documentFileId
   */
  
  @JsonProperty("documentFileId")
  public @Nullable Optional<UUID> getDocumentFileId() {
    return documentFileId;
  }

  @JsonProperty("documentFileId")
  public void setDocumentFileId(@Nullable Optional<UUID> documentFileId) {
    this.documentFileId = documentFileId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NoticeVersionUpdate other = (NoticeVersionUpdate) o;
    return Objects.equals(this.requiresReconsent, other.requiresReconsent) &&
        Objects.equals(this.purposeCodes, other.purposeCodes) &&
        Objects.equals(this.grievanceContact, other.grievanceContact) &&
        Objects.equals(this.documentFileId, other.documentFileId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(requiresReconsent, purposeCodes, grievanceContact, documentFileId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NoticeVersionUpdate {\n");
    sb.append("    requiresReconsent: ").append(toIndentedString(requiresReconsent)).append("\n");
    sb.append("    purposeCodes: ").append(toIndentedString(purposeCodes)).append("\n");
    sb.append("    grievanceContact: ").append(toIndentedString(grievanceContact)).append("\n");
    sb.append("    documentFileId: ").append(toIndentedString(documentFileId)).append("\n");
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
