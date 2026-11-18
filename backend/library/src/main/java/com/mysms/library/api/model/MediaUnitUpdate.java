// Owned API contract of library.
package com.mysms.library.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of MediaUnit that can be updated (partial update; see updateMask).
 */

public class MediaUnitUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String accessionNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String barcode;

  /**
   * Copy state.
   */
  public enum StatusEnum {
    AVAILABLE("available"),
    
    ISSUED("issued"),
    
    RESERVED("reserved"),
    
    LOST("lost"),
    
    DAMAGED("damaged"),
    
    WITHDRAWN("withdrawn");

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
  private @Nullable StatusEnum status;

  public MediaUnitUpdate accessionNumber(@Nullable String accessionNumber) {
    this.accessionNumber = accessionNumber;
    return this;
  }

  /**
   * Copy number.
   * @return accessionNumber
   */
  @Size(max = 40) 
  @JsonProperty("accessionNumber")
  public @Nullable String getAccessionNumber() {
    return accessionNumber;
  }

  @JsonProperty("accessionNumber")
  public void setAccessionNumber(@Nullable String accessionNumber) {
    this.accessionNumber = accessionNumber;
  }

  public MediaUnitUpdate barcode(@Nullable String barcode) {
    this.barcode = barcode;
    return this;
  }

  /**
   * Barcode.
   * @return barcode
   */
  @Size(max = 60) 
  @JsonProperty("barcode")
  public @Nullable String getBarcode() {
    return barcode;
  }

  @JsonProperty("barcode")
  public void setBarcode(@Nullable String barcode) {
    this.barcode = barcode;
  }

  public MediaUnitUpdate status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Copy state.
   * @return status
   */
  
  @JsonProperty("status")
  public @Nullable StatusEnum getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable StatusEnum status) {
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
    MediaUnitUpdate mediaUnitUpdate = (MediaUnitUpdate) o;
    return Objects.equals(this.accessionNumber, mediaUnitUpdate.accessionNumber) &&
        Objects.equals(this.barcode, mediaUnitUpdate.barcode) &&
        Objects.equals(this.status, mediaUnitUpdate.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessionNumber, barcode, status);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MediaUnitUpdate {\n");
    sb.append("    accessionNumber: ").append(toIndentedString(accessionNumber)).append("\n");
    sb.append("    barcode: ").append(toIndentedString(barcode)).append("\n");
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

