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
 * Fields of Reservation that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ReservationUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> mediaId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 40) String> reference;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> requestedByPersonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<@NotNull LocalDate> dateFrom;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<@NotNull LocalDate> dateTo;

  public ReservationUpdate mediaId(@Nullable Optional<UUID> mediaId) {
    this.mediaId = mediaId;
    return this;
  }

  /**
   * Title.
   * @return mediaId
   */
  
  @JsonProperty("mediaId")
  public @Nullable Optional<UUID> getMediaId() {
    return mediaId;
  }

  @JsonProperty("mediaId")
  public void setMediaId(@Nullable Optional<UUID> mediaId) {
    this.mediaId = mediaId;
  }

  public ReservationUpdate reference(@Nullable Optional<String> reference) {
    this.reference = reference;
    return this;
  }

  /**
   * Request reference.
   * @return reference
   */
  
  @JsonProperty("reference")
  public @Nullable Optional<String> getReference() {
    return reference;
  }

  @JsonProperty("reference")
  public void setReference(@Nullable Optional<String> reference) {
    this.reference = reference;
  }

  public ReservationUpdate requestedByPersonId(@Nullable Optional<UUID> requestedByPersonId) {
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

  public ReservationUpdate dateFrom(@Nullable Optional<LocalDate> dateFrom) {
    this.dateFrom = dateFrom;
    return this;
  }

  /**
   * Wanted from.
   * @return dateFrom
   */
  
  @JsonProperty("dateFrom")
  public @Nullable Optional<LocalDate> getDateFrom() {
    return dateFrom;
  }

  @JsonProperty("dateFrom")
  public void setDateFrom(@Nullable Optional<LocalDate> dateFrom) {
    this.dateFrom = dateFrom;
  }

  public ReservationUpdate dateTo(@Nullable Optional<LocalDate> dateTo) {
    this.dateTo = dateTo;
    return this;
  }

  /**
   * Wanted until.
   * @return dateTo
   */
  
  @JsonProperty("dateTo")
  public @Nullable Optional<LocalDate> getDateTo() {
    return dateTo;
  }

  @JsonProperty("dateTo")
  public void setDateTo(@Nullable Optional<LocalDate> dateTo) {
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
        Objects.equals(this.dateFrom, reservationUpdate.dateFrom) &&
        Objects.equals(this.dateTo, reservationUpdate.dateTo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mediaId, reference, requestedByPersonId, dateFrom, dateTo);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ReservationUpdate {\n");
    sb.append("    mediaId: ").append(toIndentedString(mediaId)).append("\n");
    sb.append("    reference: ").append(toIndentedString(reference)).append("\n");
    sb.append("    requestedByPersonId: ").append(toIndentedString(requestedByPersonId)).append("\n");
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

