// Owned API contract of library.
package com.mysms.campus.library.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of LibraryCard that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class LibraryCardUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 40) String> cardNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> cardTypeId;

  /**
   * Holder type.
   */
  public enum HolderTypeEnum {
    STUDENT("student"),
    
    STAFF("staff");

    private final String value;

    HolderTypeEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static HolderTypeEnum fromValue(String value) {
      for (HolderTypeEnum b : HolderTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull HolderTypeEnum> holderType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> personId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> studentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> staffId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> maxItemsOverride;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<@NotNull LocalDate> issueDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> expiryDate;

  /**
   * State.
   */
  public enum StatusEnum {
    ACTIVE("active"),
    
    INACTIVE("inactive"),
    
    EXPIRED("expired"),
    
    LOST("lost");

    private final String value;

    StatusEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static StatusEnum fromValue(String value) {
      for (StatusEnum b : StatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull StatusEnum> status;

  public LibraryCardUpdate institutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  
  @JsonProperty("institutionId")
  public @Nullable Optional<UUID> getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
  }

  public LibraryCardUpdate cardNumber(@Nullable Optional<String> cardNumber) {
    this.cardNumber = cardNumber;
    return this;
  }

  /**
   * Card number.
   * @return cardNumber
   */
  
  @JsonProperty("cardNumber")
  public @Nullable Optional<String> getCardNumber() {
    return cardNumber;
  }

  @JsonProperty("cardNumber")
  public void setCardNumber(@Nullable Optional<String> cardNumber) {
    this.cardNumber = cardNumber;
  }

  public LibraryCardUpdate cardTypeId(@Nullable Optional<UUID> cardTypeId) {
    this.cardTypeId = cardTypeId;
    return this;
  }

  /**
   * Card type.
   * @return cardTypeId
   */
  
  @JsonProperty("cardTypeId")
  public @Nullable Optional<UUID> getCardTypeId() {
    return cardTypeId;
  }

  @JsonProperty("cardTypeId")
  public void setCardTypeId(@Nullable Optional<UUID> cardTypeId) {
    this.cardTypeId = cardTypeId;
  }

  public LibraryCardUpdate holderType(@Nullable Optional<HolderTypeEnum> holderType) {
    this.holderType = holderType;
    return this;
  }

  /**
   * Holder type.
   * @return holderType
   */
  
  @JsonProperty("holderType")
  public @Nullable Optional<HolderTypeEnum> getHolderType() {
    return holderType;
  }

  @JsonProperty("holderType")
  public void setHolderType(@Nullable Optional<HolderTypeEnum> holderType) {
    this.holderType = holderType;
  }

  public LibraryCardUpdate personId(@Nullable Optional<UUID> personId) {
    this.personId = personId;
    return this;
  }

  /**
   * Card holder.
   * @return personId
   */
  
  @JsonProperty("personId")
  public @Nullable Optional<UUID> getPersonId() {
    return personId;
  }

  @JsonProperty("personId")
  public void setPersonId(@Nullable Optional<UUID> personId) {
    this.personId = personId;
  }

  public LibraryCardUpdate studentId(@Nullable Optional<UUID> studentId) {
    this.studentId = studentId;
    return this;
  }

  /**
   * Holder as student.
   * @return studentId
   */
  
  @JsonProperty("studentId")
  public @Nullable Optional<UUID> getStudentId() {
    return studentId;
  }

  @JsonProperty("studentId")
  public void setStudentId(@Nullable Optional<UUID> studentId) {
    this.studentId = studentId;
  }

  public LibraryCardUpdate staffId(@Nullable Optional<UUID> staffId) {
    this.staffId = staffId;
    return this;
  }

  /**
   * Holder as staff.
   * @return staffId
   */
  
  @JsonProperty("staffId")
  public @Nullable Optional<UUID> getStaffId() {
    return staffId;
  }

  @JsonProperty("staffId")
  public void setStaffId(@Nullable Optional<UUID> staffId) {
    this.staffId = staffId;
  }

  public LibraryCardUpdate maxItemsOverride(@Nullable Optional<Integer> maxItemsOverride) {
    this.maxItemsOverride = maxItemsOverride;
    return this;
  }

  /**
   * Item limit overriding the card type.
   * @return maxItemsOverride
   */
  
  @JsonProperty("maxItemsOverride")
  public @Nullable Optional<Integer> getMaxItemsOverride() {
    return maxItemsOverride;
  }

  @JsonProperty("maxItemsOverride")
  public void setMaxItemsOverride(@Nullable Optional<Integer> maxItemsOverride) {
    this.maxItemsOverride = maxItemsOverride;
  }

  public LibraryCardUpdate issueDate(@Nullable Optional<LocalDate> issueDate) {
    this.issueDate = issueDate;
    return this;
  }

  /**
   * Issue date.
   * @return issueDate
   */
  
  @JsonProperty("issueDate")
  public @Nullable Optional<LocalDate> getIssueDate() {
    return issueDate;
  }

  @JsonProperty("issueDate")
  public void setIssueDate(@Nullable Optional<LocalDate> issueDate) {
    this.issueDate = issueDate;
  }

  public LibraryCardUpdate expiryDate(@Nullable Optional<LocalDate> expiryDate) {
    this.expiryDate = expiryDate;
    return this;
  }

  /**
   * Expiry date.
   * @return expiryDate
   */
  
  @JsonProperty("expiryDate")
  public @Nullable Optional<LocalDate> getExpiryDate() {
    return expiryDate;
  }

  @JsonProperty("expiryDate")
  public void setExpiryDate(@Nullable Optional<LocalDate> expiryDate) {
    this.expiryDate = expiryDate;
  }

  public LibraryCardUpdate status(@Nullable Optional<StatusEnum> status) {
    this.status = status;
    return this;
  }

  /**
   * State.
   * @return status
   */
  
  @JsonProperty("status")
  public @Nullable Optional<StatusEnum> getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable Optional<StatusEnum> status) {
    this.status = status;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LibraryCardUpdate libraryCardUpdate = (LibraryCardUpdate) o;
    return Objects.equals(this.institutionId, libraryCardUpdate.institutionId) &&
        Objects.equals(this.cardNumber, libraryCardUpdate.cardNumber) &&
        Objects.equals(this.cardTypeId, libraryCardUpdate.cardTypeId) &&
        Objects.equals(this.holderType, libraryCardUpdate.holderType) &&
        Objects.equals(this.personId, libraryCardUpdate.personId) &&
        Objects.equals(this.studentId, libraryCardUpdate.studentId) &&
        Objects.equals(this.staffId, libraryCardUpdate.staffId) &&
        Objects.equals(this.maxItemsOverride, libraryCardUpdate.maxItemsOverride) &&
        Objects.equals(this.issueDate, libraryCardUpdate.issueDate) &&
        Objects.equals(this.expiryDate, libraryCardUpdate.expiryDate) &&
        Objects.equals(this.status, libraryCardUpdate.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, cardNumber, cardTypeId, holderType, personId, studentId, staffId, maxItemsOverride, issueDate, expiryDate, status);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LibraryCardUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    cardNumber: ").append(toIndentedString(cardNumber)).append("\n");
    sb.append("    cardTypeId: ").append(toIndentedString(cardTypeId)).append("\n");
    sb.append("    holderType: ").append(toIndentedString(holderType)).append("\n");
    sb.append("    personId: ").append(toIndentedString(personId)).append("\n");
    sb.append("    studentId: ").append(toIndentedString(studentId)).append("\n");
    sb.append("    staffId: ").append(toIndentedString(staffId)).append("\n");
    sb.append("    maxItemsOverride: ").append(toIndentedString(maxItemsOverride)).append("\n");
    sb.append("    issueDate: ").append(toIndentedString(issueDate)).append("\n");
    sb.append("    expiryDate: ").append(toIndentedString(expiryDate)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
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

