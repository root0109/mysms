// Owned API contract of library (JAVA-CODING-RULES §7); first materialized from api/openapi/library.yaml on 2026-11-10.
package com.mysms.library.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of PurchaseRequest that can be updated (partial update; see updateMask).
 */

public class PurchaseRequestUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String requestNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String authorText;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String edition;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String publisherText;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID mediaTypeId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID requestedByPersonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate requestDate;

  public PurchaseRequestUpdate requestNumber(@Nullable String requestNumber) {
    this.requestNumber = requestNumber;
    return this;
  }

  /**
   * Request number.
   * @return requestNumber
   */
  @Size(max = 40) 
  @JsonProperty("requestNumber")
  public @Nullable String getRequestNumber() {
    return requestNumber;
  }

  @JsonProperty("requestNumber")
  public void setRequestNumber(@Nullable String requestNumber) {
    this.requestNumber = requestNumber;
  }

  public PurchaseRequestUpdate title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * Requested title.
   * @return title
   */
  @Size(max = 300) 
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public PurchaseRequestUpdate authorText(@Nullable String authorText) {
    this.authorText = authorText;
    return this;
  }

  /**
   * Author(s).
   * @return authorText
   */
  @Size(max = 300) 
  @JsonProperty("authorText")
  public @Nullable String getAuthorText() {
    return authorText;
  }

  @JsonProperty("authorText")
  public void setAuthorText(@Nullable String authorText) {
    this.authorText = authorText;
  }

  public PurchaseRequestUpdate edition(@Nullable String edition) {
    this.edition = edition;
    return this;
  }

  /**
   * Edition.
   * @return edition
   */
  @Size(max = 60) 
  @JsonProperty("edition")
  public @Nullable String getEdition() {
    return edition;
  }

  @JsonProperty("edition")
  public void setEdition(@Nullable String edition) {
    this.edition = edition;
  }

  public PurchaseRequestUpdate publisherText(@Nullable String publisherText) {
    this.publisherText = publisherText;
    return this;
  }

  /**
   * Publisher.
   * @return publisherText
   */
  @Size(max = 200) 
  @JsonProperty("publisherText")
  public @Nullable String getPublisherText() {
    return publisherText;
  }

  @JsonProperty("publisherText")
  public void setPublisherText(@Nullable String publisherText) {
    this.publisherText = publisherText;
  }

  public PurchaseRequestUpdate mediaTypeId(@Nullable UUID mediaTypeId) {
    this.mediaTypeId = mediaTypeId;
    return this;
  }

  /**
   * Type.
   * @return mediaTypeId
   */
  @Valid 
  @JsonProperty("mediaTypeId")
  public @Nullable UUID getMediaTypeId() {
    return mediaTypeId;
  }

  @JsonProperty("mediaTypeId")
  public void setMediaTypeId(@Nullable UUID mediaTypeId) {
    this.mediaTypeId = mediaTypeId;
  }

  public PurchaseRequestUpdate requestedByPersonId(@Nullable UUID requestedByPersonId) {
    this.requestedByPersonId = requestedByPersonId;
    return this;
  }

  /**
   * Requester.
   * @return requestedByPersonId
   */
  @Valid 
  @JsonProperty("requestedByPersonId")
  public @Nullable UUID getRequestedByPersonId() {
    return requestedByPersonId;
  }

  @JsonProperty("requestedByPersonId")
  public void setRequestedByPersonId(@Nullable UUID requestedByPersonId) {
    this.requestedByPersonId = requestedByPersonId;
  }

  public PurchaseRequestUpdate requestDate(@Nullable LocalDate requestDate) {
    this.requestDate = requestDate;
    return this;
  }

  /**
   * Request date.
   * @return requestDate
   */
  @Valid 
  @JsonProperty("requestDate")
  public @Nullable LocalDate getRequestDate() {
    return requestDate;
  }

  @JsonProperty("requestDate")
  public void setRequestDate(@Nullable LocalDate requestDate) {
    this.requestDate = requestDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PurchaseRequestUpdate purchaseRequestUpdate = (PurchaseRequestUpdate) o;
    return Objects.equals(this.requestNumber, purchaseRequestUpdate.requestNumber) &&
        Objects.equals(this.title, purchaseRequestUpdate.title) &&
        Objects.equals(this.authorText, purchaseRequestUpdate.authorText) &&
        Objects.equals(this.edition, purchaseRequestUpdate.edition) &&
        Objects.equals(this.publisherText, purchaseRequestUpdate.publisherText) &&
        Objects.equals(this.mediaTypeId, purchaseRequestUpdate.mediaTypeId) &&
        Objects.equals(this.requestedByPersonId, purchaseRequestUpdate.requestedByPersonId) &&
        Objects.equals(this.requestDate, purchaseRequestUpdate.requestDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(requestNumber, title, authorText, edition, publisherText, mediaTypeId, requestedByPersonId, requestDate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PurchaseRequestUpdate {\n");
    sb.append("    requestNumber: ").append(toIndentedString(requestNumber)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    authorText: ").append(toIndentedString(authorText)).append("\n");
    sb.append("    edition: ").append(toIndentedString(edition)).append("\n");
    sb.append("    publisherText: ").append(toIndentedString(publisherText)).append("\n");
    sb.append("    mediaTypeId: ").append(toIndentedString(mediaTypeId)).append("\n");
    sb.append("    requestedByPersonId: ").append(toIndentedString(requestedByPersonId)).append("\n");
    sb.append("    requestDate: ").append(toIndentedString(requestDate)).append("\n");
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

