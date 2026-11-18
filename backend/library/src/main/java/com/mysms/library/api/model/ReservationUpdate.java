// Owned API contract of library.
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
 * Fields of Reservation that can be updated (partial update; see updateMask).
 */

public class ReservationUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID mediaId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String reference;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID requestedByPersonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID requestedByUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate dateFrom;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate dateTo;

  public ReservationUpdate mediaId(@Nullable UUID mediaId) {
    this.mediaId = mediaId;
    return this;
  }

  /**
   * Title.
   * @return mediaId
   */
  @Valid 
  @JsonProperty("mediaId")
  public @Nullable UUID getMediaId() {
    return mediaId;
  }

  @JsonProperty("mediaId")
  public void setMediaId(@Nullable UUID mediaId) {
    this.mediaId = mediaId;
  }

  public ReservationUpdate reference(@Nullable String reference) {
    this.reference = reference;
    return this;
  }

  /**
   * Request reference.
   * @return reference
   */
  @Size(max = 40) 
  @JsonProperty("reference")
  public @Nullable String getReference() {
    return reference;
  }

  @JsonProperty("reference")
  public void setReference(@Nullable String reference) {
    this.reference = reference;
  }

  public ReservationUpdate requestedByPersonId(@Nullable UUID requestedByPersonId) {
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

  public ReservationUpdate requestedByUserId(@Nullable UUID requestedByUserId) {
    this.requestedByUserId = requestedByUserId;
    return this;
  }

  /**
   * Requester account.
   * @return requestedByUserId
   */
  @Valid 
  @JsonProperty("requestedByUserId")
  public @Nullable UUID getRequestedByUserId() {
    return requestedByUserId;
  }

  @JsonProperty("requestedByUserId")
  public void setRequestedByUserId(@Nullable UUID requestedByUserId) {
    this.requestedByUserId = requestedByUserId;
  }

  public ReservationUpdate dateFrom(@Nullable LocalDate dateFrom) {
    this.dateFrom = dateFrom;
    return this;
  }

  /**
   * Wanted from.
   * @return dateFrom
   */
  @Valid 
  @JsonProperty("dateFrom")
  public @Nullable LocalDate getDateFrom() {
    return dateFrom;
  }

  @JsonProperty("dateFrom")
  public void setDateFrom(@Nullable LocalDate dateFrom) {
    this.dateFrom = dateFrom;
  }

  public ReservationUpdate dateTo(@Nullable LocalDate dateTo) {
    this.dateTo = dateTo;
    return this;
  }

  /**
   * Wanted until.
   * @return dateTo
   */
  @Valid 
  @JsonProperty("dateTo")
  public @Nullable LocalDate getDateTo() {
    return dateTo;
  }

  @JsonProperty("dateTo")
  public void setDateTo(@Nullable LocalDate dateTo) {
    this.dateTo = dateTo;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReservationUpdate reservationUpdate = (ReservationUpdate) o;
    return Objects.equals(this.mediaId, reservationUpdate.mediaId) &&
        Objects.equals(this.reference, reservationUpdate.reference) &&
        Objects.equals(this.requestedByPersonId, reservationUpdate.requestedByPersonId) &&
        Objects.equals(this.requestedByUserId, reservationUpdate.requestedByUserId) &&
        Objects.equals(this.dateFrom, reservationUpdate.dateFrom) &&
        Objects.equals(this.dateTo, reservationUpdate.dateTo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mediaId, reference, requestedByPersonId, requestedByUserId, dateFrom, dateTo);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ReservationUpdate {\n");
    sb.append("    mediaId: ").append(toIndentedString(mediaId)).append("\n");
    sb.append("    reference: ").append(toIndentedString(reference)).append("\n");
    sb.append("    requestedByPersonId: ").append(toIndentedString(requestedByPersonId)).append("\n");
    sb.append("    requestedByUserId: ").append(toIndentedString(requestedByUserId)).append("\n");
    sb.append("    dateFrom: ").append(toIndentedString(dateFrom)).append("\n");
    sb.append("    dateTo: ").append(toIndentedString(dateTo)).append("\n");
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

