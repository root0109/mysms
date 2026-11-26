// Owned API contract of library.
package com.mysms.campus.library.api.model;

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
 * Fields of PurchaseRequest that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class PurchaseRequestUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 40) String> requestNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 300) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 300) String> authorText;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 60) String> edition;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 200) String> publisherText;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> mediaTypeId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> requestedByPersonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> requestDate;

  public PurchaseRequestUpdate requestNumber(@Nullable Optional<String> requestNumber) {
    this.requestNumber = requestNumber;
    return this;
  }

  /**
   * Request number.
   * @return requestNumber
   */
  
  @JsonProperty("requestNumber")
  public @Nullable Optional<String> getRequestNumber() {
    return requestNumber;
  }

  @JsonProperty("requestNumber")
  public void setRequestNumber(@Nullable Optional<String> requestNumber) {
    this.requestNumber = requestNumber;
  }

  public PurchaseRequestUpdate title(@Nullable Optional<String> title) {
    this.title = title;
    return this;
  }

  /**
   * Requested title.
   * @return title
   */
  
  @JsonProperty("title")
  public @Nullable Optional<String> getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable Optional<String> title) {
    this.title = title;
  }

  public PurchaseRequestUpdate authorText(@Nullable Optional<String> authorText) {
    this.authorText = authorText;
    return this;
  }

  /**
   * Author(s).
   * @return authorText
   */
  
  @JsonProperty("authorText")
  public @Nullable Optional<String> getAuthorText() {
    return authorText;
  }

  @JsonProperty("authorText")
  public void setAuthorText(@Nullable Optional<String> authorText) {
    this.authorText = authorText;
  }

  public PurchaseRequestUpdate edition(@Nullable Optional<String> edition) {
    this.edition = edition;
    return this;
  }

  /**
   * Edition.
   * @return edition
   */
  
  @JsonProperty("edition")
  public @Nullable Optional<String> getEdition() {
    return edition;
  }

  @JsonProperty("edition")
  public void setEdition(@Nullable Optional<String> edition) {
    this.edition = edition;
  }

  public PurchaseRequestUpdate publisherText(@Nullable Optional<String> publisherText) {
    this.publisherText = publisherText;
    return this;
  }

  /**
   * Publisher.
   * @return publisherText
   */
  
  @JsonProperty("publisherText")
  public @Nullable Optional<String> getPublisherText() {
    return publisherText;
  }

  @JsonProperty("publisherText")
  public void setPublisherText(@Nullable Optional<String> publisherText) {
    this.publisherText = publisherText;
  }

  public PurchaseRequestUpdate mediaTypeId(@Nullable Optional<UUID> mediaTypeId) {
    this.mediaTypeId = mediaTypeId;
    return this;
  }

  /**
   * Type.
   * @return mediaTypeId
   */
  
  @JsonProperty("mediaTypeId")
  public @Nullable Optional<UUID> getMediaTypeId() {
    return mediaTypeId;
  }

  @JsonProperty("mediaTypeId")
  public void setMediaTypeId(@Nullable Optional<UUID> mediaTypeId) {
    this.mediaTypeId = mediaTypeId;
  }

  public PurchaseRequestUpdate requestedByPersonId(@Nullable Optional<UUID> requestedByPersonId) {
    this.requestedByPersonId = requestedByPersonId;
    return this;
  }

  /**
   * Requester.
   * @return requestedByPersonId
   */
  
  @JsonProperty("requestedByPersonId")
  public @Nullable Optional<UUID> getRequestedByPersonId() {
    return requestedByPersonId;
  }

  @JsonProperty("requestedByPersonId")
  public void setRequestedByPersonId(@Nullable Optional<UUID> requestedByPersonId) {
    this.requestedByPersonId = requestedByPersonId;
  }

  public PurchaseRequestUpdate requestDate(@Nullable Optional<LocalDate> requestDate) {
    this.requestDate = requestDate;
    return this;
  }

  /**
   * Request date.
   * @return requestDate
   */
  
  @JsonProperty("requestDate")
  public @Nullable Optional<LocalDate> getRequestDate() {
    return requestDate;
  }

  @JsonProperty("requestDate")
  public void setRequestDate(@Nullable Optional<LocalDate> requestDate) {
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

