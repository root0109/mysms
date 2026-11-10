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
 * Fields of Loan that can be updated (partial update; see updateMask).
 */

public class LoanUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID mediaUnitId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID mediaId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID libraryCardId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID borrowerUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate issuedOn;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate dueOn;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate returnedOn;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String fineAmount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID invoiceId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID issuedByUserId;

  public LoanUpdate mediaUnitId(@Nullable UUID mediaUnitId) {
    this.mediaUnitId = mediaUnitId;
    return this;
  }

  /**
   * Copy issued.
   * @return mediaUnitId
   */
  @Valid 
  @JsonProperty("mediaUnitId")
  public @Nullable UUID getMediaUnitId() {
    return mediaUnitId;
  }

  @JsonProperty("mediaUnitId")
  public void setMediaUnitId(@Nullable UUID mediaUnitId) {
    this.mediaUnitId = mediaUnitId;
  }

  public LoanUpdate mediaId(@Nullable UUID mediaId) {
    this.mediaId = mediaId;
    return this;
  }

  /**
   * Title (denormalised from the copy for reporting).
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

  public LoanUpdate libraryCardId(@Nullable UUID libraryCardId) {
    this.libraryCardId = libraryCardId;
    return this;
  }

  /**
   * Card used.
   * @return libraryCardId
   */
  @Valid 
  @JsonProperty("libraryCardId")
  public @Nullable UUID getLibraryCardId() {
    return libraryCardId;
  }

  @JsonProperty("libraryCardId")
  public void setLibraryCardId(@Nullable UUID libraryCardId) {
    this.libraryCardId = libraryCardId;
  }

  public LoanUpdate borrowerUserId(@Nullable UUID borrowerUserId) {
    this.borrowerUserId = borrowerUserId;
    return this;
  }

  /**
   * Borrower account.
   * @return borrowerUserId
   */
  @Valid 
  @JsonProperty("borrowerUserId")
  public @Nullable UUID getBorrowerUserId() {
    return borrowerUserId;
  }

  @JsonProperty("borrowerUserId")
  public void setBorrowerUserId(@Nullable UUID borrowerUserId) {
    this.borrowerUserId = borrowerUserId;
  }

  public LoanUpdate issuedOn(@Nullable LocalDate issuedOn) {
    this.issuedOn = issuedOn;
    return this;
  }

  /**
   * Issue date (NULL while only reserved / drafted).
   * @return issuedOn
   */
  @Valid 
  @JsonProperty("issuedOn")
  public @Nullable LocalDate getIssuedOn() {
    return issuedOn;
  }

  @JsonProperty("issuedOn")
  public void setIssuedOn(@Nullable LocalDate issuedOn) {
    this.issuedOn = issuedOn;
  }

  public LoanUpdate dueOn(@Nullable LocalDate dueOn) {
    this.dueOn = dueOn;
    return this;
  }

  /**
   * Due date (issued_on + card type loan_days when not given).
   * @return dueOn
   */
  @Valid 
  @JsonProperty("dueOn")
  public @Nullable LocalDate getDueOn() {
    return dueOn;
  }

  @JsonProperty("dueOn")
  public void setDueOn(@Nullable LocalDate dueOn) {
    this.dueOn = dueOn;
  }

  public LoanUpdate returnedOn(@Nullable LocalDate returnedOn) {
    this.returnedOn = returnedOn;
    return this;
  }

  /**
   * Actual return date.
   * @return returnedOn
   */
  @Valid 
  @JsonProperty("returnedOn")
  public @Nullable LocalDate getReturnedOn() {
    return returnedOn;
  }

  @JsonProperty("returnedOn")
  public void setReturnedOn(@Nullable LocalDate returnedOn) {
    this.returnedOn = returnedOn;
  }

  public LoanUpdate fineAmount(@Nullable String fineAmount) {
    this.fineAmount = fineAmount;
    return this;
  }

  /**
   * Fine charged.
   * @return fineAmount
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("fineAmount")
  public @Nullable String getFineAmount() {
    return fineAmount;
  }

  @JsonProperty("fineAmount")
  public void setFineAmount(@Nullable String fineAmount) {
    this.fineAmount = fineAmount;
  }

  public LoanUpdate invoiceId(@Nullable UUID invoiceId) {
    this.invoiceId = invoiceId;
    return this;
  }

  /**
   * Fine invoice.
   * @return invoiceId
   */
  @Valid 
  @JsonProperty("invoiceId")
  public @Nullable UUID getInvoiceId() {
    return invoiceId;
  }

  @JsonProperty("invoiceId")
  public void setInvoiceId(@Nullable UUID invoiceId) {
    this.invoiceId = invoiceId;
  }

  public LoanUpdate issuedByUserId(@Nullable UUID issuedByUserId) {
    this.issuedByUserId = issuedByUserId;
    return this;
  }

  /**
   * Librarian.
   * @return issuedByUserId
   */
  @Valid 
  @JsonProperty("issuedByUserId")
  public @Nullable UUID getIssuedByUserId() {
    return issuedByUserId;
  }

  @JsonProperty("issuedByUserId")
  public void setIssuedByUserId(@Nullable UUID issuedByUserId) {
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

