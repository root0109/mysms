// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.lang.Nullable;

/**
 * Fields of NoticeVersion that can be updated while it is a draft (partial update; see updateMask).
 */

public class NoticeVersionUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean requiresReconsent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<String> purposeCodes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable GrievanceContact grievanceContact;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID documentFileId;

  public NoticeVersionUpdate requiresReconsent(@Nullable Boolean requiresReconsent) {
    this.requiresReconsent = requiresReconsent;
    return this;
  }

  /**
   * Whether consents given under earlier versions must be asked for again once this version is published.
   * @return requiresReconsent
   */
   
  @JsonProperty("requiresReconsent")
  public @Nullable Boolean getRequiresReconsent() {
    return requiresReconsent;
  }

  @JsonProperty("requiresReconsent")
  public void setRequiresReconsent(@Nullable Boolean requiresReconsent) {
    this.requiresReconsent = requiresReconsent;
  }

  public NoticeVersionUpdate purposeCodes(@Nullable List<String> purposeCodes) {
    this.purposeCodes = purposeCodes;
    return this;
  }

  public NoticeVersionUpdate addPurposeCodesItem(String purposeCodesItem) {
    if (this.purposeCodes == null) {
      this.purposeCodes = new ArrayList<>();
    }
    this.purposeCodes.add(purposeCodesItem);
    return this;
  }

  /**
   * Codes of the active catalogue purposes the version asks consent for.
   * @return purposeCodes
   */
  @Size(min = 1) 
  @JsonProperty("purposeCodes")
  public @Nullable List<String> getPurposeCodes() {
    return purposeCodes;
  }

  @JsonProperty("purposeCodes")
  public void setPurposeCodes(@Nullable List<String> purposeCodes) {
    this.purposeCodes = purposeCodes;
  }

  public NoticeVersionUpdate grievanceContact(@Nullable GrievanceContact grievanceContact) {
    this.grievanceContact = grievanceContact;
    return this;
  }

  /**
   * The school's grievance officer the version names.
   * @return grievanceContact
   */
  @Valid 
  @JsonProperty("grievanceContact")
  public @Nullable GrievanceContact getGrievanceContact() {
    return grievanceContact;
  }

  @JsonProperty("grievanceContact")
  public void setGrievanceContact(@Nullable GrievanceContact grievanceContact) {
    this.grievanceContact = grievanceContact;
  }

  public NoticeVersionUpdate documentFileId(@Nullable UUID documentFileId) {
    this.documentFileId = documentFileId;
    return this;
  }

  /**
   * Optional signed PDF of the version (a platform file).
   * @return documentFileId
   */
  @Valid 
  @JsonProperty("documentFileId")
  public @Nullable UUID getDocumentFileId() {
    return documentFileId;
  }

  @JsonProperty("documentFileId")
  public void setDocumentFileId(@Nullable UUID documentFileId) {
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
