// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import org.springframework.lang.Nullable;

/**
 * One field a correction request asks to change, with the value asked for.
 */

public class RightsRequestCorrectionField {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String field;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String requestedValue;

  public RightsRequestCorrectionField() {
    super();
  }

  public RightsRequestCorrectionField field(String field) {
    this.field = field;
    return this;
  }

  /**
   * The field as the requester names it (for example "date of birth" or "address").
   * @return field
   */
  @NotNull @Size(min = 1, max = 60) 
  @JsonProperty("field")
  public String getField() {
    return field;
  }

  @JsonProperty("field")
  public void setField(String field) {
    this.field = field;
  }

  public RightsRequestCorrectionField requestedValue(String requestedValue) {
    this.requestedValue = requestedValue;
    return this;
  }

  /**
   * The value the requester asks for.
   * @return requestedValue
   */
  @NotNull @Size(max = 1000) 
  @JsonProperty("requestedValue")
  public String getRequestedValue() {
    return requestedValue;
  }

  @JsonProperty("requestedValue")
  public void setRequestedValue(String requestedValue) {
    this.requestedValue = requestedValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RightsRequestCorrectionField other = (RightsRequestCorrectionField) o;
    return Objects.equals(this.field, other.field) &&
        Objects.equals(this.requestedValue, other.requestedValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(field, requestedValue);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RightsRequestCorrectionField {\n");
    sb.append("    field: ").append(toIndentedString(field)).append("\n");
    sb.append("    requestedValue: ").append(toIndentedString(requestedValue)).append("\n");
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
