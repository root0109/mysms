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
 * Fields of Loan that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class LoanUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> mediaUnitId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> mediaId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> libraryCardId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> borrowerUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> issuedOn;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> dueOn;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> returnedOn;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> fineAmount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> invoiceId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> issuedByUserId;

  public LoanUpdate mediaUnitId(@Nullable Optional<UUID> mediaUnitId) {
    this.mediaUnitId = mediaUnitId;
    return this;
  }

  /**
   * Copy issued.
   * @return mediaUnitId
   */
  
  @JsonProperty("mediaUnitId")
  public @Nullable Optional<UUID> getMediaUnitId() {
    return mediaUnitId;
  }

  @JsonProperty("mediaUnitId")
  public void setMediaUnitId(@Nullable Optional<UUID> mediaUnitId) {
    this.mediaUnitId = mediaUnitId;
  }

  public LoanUpdate mediaId(@Nullable Optional<UUID> mediaId) {
    this.mediaId = mediaId;
    return this;
  }

  /**
   * Title (denormalised from the copy for reporting).
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

  public LoanUpdate libraryCardId(@Nullable Optional<UUID> libraryCardId) {
    this.libraryCardId = libraryCardId;
    return this;
  }

  /**
   * Card used.
   * @return libraryCardId
   */
  
  @JsonProperty("libraryCardId")
  public @Nullable Optional<UUID> getLibraryCardId() {
    return libraryCardId;
  }

  @JsonProperty("libraryCardId")
  public void setLibraryCardId(@Nullable Optional<UUID> libraryCardId) {
    this.libraryCardId = libraryCardId;
  }

  public LoanUpdate borrowerUserId(@Nullable Optional<UUID> borrowerUserId) {
    this.borrowerUserId = borrowerUserId;
    return this;
  }

  /**
   * Borrower account.
   * @return borrowerUserId
   */
  
  @JsonProperty("borrowerUserId")
  public @Nullable Optional<UUID> getBorrowerUserId() {
    return borrowerUserId;
  }

  @JsonProperty("borrowerUserId")
  public void setBorrowerUserId(@Nullable Optional<UUID> borrowerUserId) {
    this.borrowerUserId = borrowerUserId;
  }

  public LoanUpdate issuedOn(@Nullable Optional<LocalDate> issuedOn) {
    this.issuedOn = issuedOn;
    return this;
  }

  /**
   * Issue date (NULL while only reserved / drafted).
   * @return issuedOn
   */
  
  @JsonProperty("issuedOn")
  public @Nullable Optional<LocalDate> getIssuedOn() {
    return issuedOn;
  }

  @JsonProperty("issuedOn")
  public void setIssuedOn(@Nullable Optional<LocalDate> issuedOn) {
    this.issuedOn = issuedOn;
  }

  public LoanUpdate dueOn(@Nullable Optional<LocalDate> dueOn) {
    this.dueOn = dueOn;
    return this;
  }

  /**
   * Due date (issued_on + card type loan_days when not given).
   * @return dueOn
   */
  
  @JsonProperty("dueOn")
  public @Nullable Optional<LocalDate> getDueOn() {
    return dueOn;
  }

  @JsonProperty("dueOn")
  public void setDueOn(@Nullable Optional<LocalDate> dueOn) {
    this.dueOn = dueOn;
  }

  public LoanUpdate returnedOn(@Nullable Optional<LocalDate> returnedOn) {
    this.returnedOn = returnedOn;
    return this;
  }

  /**
   * Actual return date.
   * @return returnedOn
   */
  
  @JsonProperty("returnedOn")
  public @Nullable Optional<LocalDate> getReturnedOn() {
    return returnedOn;
  }

  @JsonProperty("returnedOn")
  public void setReturnedOn(@Nullable Optional<LocalDate> returnedOn) {
    this.returnedOn = returnedOn;
  }

  public LoanUpdate fineAmount(@Nullable Optional<String> fineAmount) {
    this.fineAmount = fineAmount;
    return this;
  }

  /**
   * Fine charged.
   * @return fineAmount
   */
  
  @JsonProperty("fineAmount")
  public @Nullable Optional<String> getFineAmount() {
    return fineAmount;
  }

  @JsonProperty("fineAmount")
  public void setFineAmount(@Nullable Optional<String> fineAmount) {
    this.fineAmount = fineAmount;
  }

  public LoanUpdate invoiceId(@Nullable Optional<UUID> invoiceId) {
    this.invoiceId = invoiceId;
    return this;
  }

  /**
   * Fine invoice.
   * @return invoiceId
   */
  
  @JsonProperty("invoiceId")
  public @Nullable Optional<UUID> getInvoiceId() {
    return invoiceId;
  }

  @JsonProperty("invoiceId")
  public void setInvoiceId(@Nullable Optional<UUID> invoiceId) {
    this.invoiceId = invoiceId;
  }

  public LoanUpdate issuedByUserId(@Nullable Optional<UUID> issuedByUserId) {
    this.issuedByUserId = issuedByUserId;
    return this;
  }

  /**
   * Librarian.
   * @return issuedByUserId
   */
  
  @JsonProperty("issuedByUserId")
  public @Nullable Optional<UUID> getIssuedByUserId() {
    return issuedByUserId;
  }

  @JsonProperty("issuedByUserId")
  public void setIssuedByUserId(@Nullable Optional<UUID> issuedByUserId) {
    this.issuedByUserId = issuedByUserId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LoanUpdate loanUpdate = (LoanUpdate) o;
    return Objects.equals(this.mediaUnitId, loanUpdate.mediaUnitId) &&
        Objects.equals(this.mediaId, loanUpdate.mediaId) &&
        Objects.equals(this.libraryCardId, loanUpdate.libraryCardId) &&
        Objects.equals(this.borrowerUserId, loanUpdate.borrowerUserId) &&
        Objects.equals(this.issuedOn, loanUpdate.issuedOn) &&
        Objects.equals(this.dueOn, loanUpdate.dueOn) &&
        Objects.equals(this.returnedOn, loanUpdate.returnedOn) &&
        Objects.equals(this.fineAmount, loanUpdate.fineAmount) &&
        Objects.equals(this.invoiceId, loanUpdate.invoiceId) &&
        Objects.equals(this.issuedByUserId, loanUpdate.issuedByUserId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mediaUnitId, mediaId, libraryCardId, borrowerUserId, issuedOn, dueOn, returnedOn, fineAmount, invoiceId, issuedByUserId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LoanUpdate {\n");
    sb.append("    mediaUnitId: ").append(toIndentedString(mediaUnitId)).append("\n");
    sb.append("    mediaId: ").append(toIndentedString(mediaId)).append("\n");
    sb.append("    libraryCardId: ").append(toIndentedString(libraryCardId)).append("\n");
    sb.append("    borrowerUserId: ").append(toIndentedString(borrowerUserId)).append("\n");
    sb.append("    issuedOn: ").append(toIndentedString(issuedOn)).append("\n");
    sb.append("    dueOn: ").append(toIndentedString(dueOn)).append("\n");
    sb.append("    returnedOn: ").append(toIndentedString(returnedOn)).append("\n");
    sb.append("    fineAmount: ").append(toIndentedString(fineAmount)).append("\n");
    sb.append("    invoiceId: ").append(toIndentedString(invoiceId)).append("\n");
    sb.append("    issuedByUserId: ").append(toIndentedString(issuedByUserId)).append("\n");
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

