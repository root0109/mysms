// Owned API contract of library.
package com.mysms.library.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of LibraryCardType that can be updated (partial update; see updateMask).
 */

public class LibraryCardTypeUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer maxItems;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer loanDays;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String finePerDay;

  public LibraryCardTypeUpdate code(@Nullable String code) {
    this.code = code;
    return this;
  }

  /**
   * Stable key used by imports, integrations and seed scripts.
   * @return code
   */
  @Size(max = 40) 
  @JsonProperty("code")
  public @Nullable String getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable String code) {
    this.code = code;
  }

  public LibraryCardTypeUpdate sortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Display order.
   * @return sortOrder
   */
  
  @JsonProperty("sortOrder")
  public @Nullable Integer getSortOrder() {
    return sortOrder;
  }

  @JsonProperty("sortOrder")
  public void setSortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  public LibraryCardTypeUpdate isActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether offered for new picks.
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Boolean getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
  }

  public LibraryCardTypeUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Card type.
   * @return name
   */
  @Size(max = 120) 
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public LibraryCardTypeUpdate maxItems(@Nullable Integer maxItems) {
    this.maxItems = maxItems;
    return this;
  }

  /**
   * Items that can be borrowed at once.
   * @return maxItems
   */
  
  @JsonProperty("maxItems")
  public @Nullable Integer getMaxItems() {
    return maxItems;
  }

  @JsonProperty("maxItems")
  public void setMaxItems(@Nullable Integer maxItems) {
    this.maxItems = maxItems;
  }

  public LibraryCardTypeUpdate loanDays(@Nullable Integer loanDays) {
    this.loanDays = loanDays;
    return this;
  }

  /**
   * Loan period in days.
   * @return loanDays
   */
  
  @JsonProperty("loanDays")
  public @Nullable Integer getLoanDays() {
    return loanDays;
  }

  @JsonProperty("loanDays")
  public void setLoanDays(@Nullable Integer loanDays) {
    this.loanDays = loanDays;
  }

  public LibraryCardTypeUpdate finePerDay(@Nullable String finePerDay) {
    this.finePerDay = finePerDay;
    return this;
  }

  /**
   * Overdue fine per day.
   * @return finePerDay
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("finePerDay")
  public @Nullable String getFinePerDay() {
    return finePerDay;
  }

  @JsonProperty("finePerDay")
  public void setFinePerDay(@Nullable String finePerDay) {
    this.finePerDay = finePerDay;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LibraryCardTypeUpdate libraryCardTypeUpdate = (LibraryCardTypeUpdate) o;
    return Objects.equals(this.code, libraryCardTypeUpdate.code) &&
        Objects.equals(this.sortOrder, libraryCardTypeUpdate.sortOrder) &&
        Objects.equals(this.isActive, libraryCardTypeUpdate.isActive) &&
        Objects.equals(this.name, libraryCardTypeUpdate.name) &&
        Objects.equals(this.maxItems, libraryCardTypeUpdate.maxItems) &&
        Objects.equals(this.loanDays, libraryCardTypeUpdate.loanDays) &&
        Objects.equals(this.finePerDay, libraryCardTypeUpdate.finePerDay);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, sortOrder, isActive, name, maxItems, loanDays, finePerDay);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LibraryCardTypeUpdate {\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    maxItems: ").append(toIndentedString(maxItems)).append("\n");
    sb.append("    loanDays: ").append(toIndentedString(loanDays)).append("\n");
    sb.append("    finePerDay: ").append(toIndentedString(finePerDay)).append("\n");
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

