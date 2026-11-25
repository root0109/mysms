// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import org.springframework.lang.Nullable;

/**
 * Body of CloseRightsRequest: the outcome of a request in progress.
 */

public class CloseRightsRequestRequest {

  /**
   * fulfilled, partially_fulfilled or declined.
   */
  public enum OutcomeCodeEnum {
    DECLINED("declined"),
    
    FULFILLED("fulfilled"),
    
    PARTIALLY_FULFILLED("partially_fulfilled");

    private final String value;

    OutcomeCodeEnum(String value) {
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
    public static OutcomeCodeEnum fromValue(String value) {
      for (OutcomeCodeEnum b : OutcomeCodeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private OutcomeCodeEnum outcomeCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String note;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String response;

  public CloseRightsRequestRequest() {
    super();
  }

  public CloseRightsRequestRequest outcomeCode(OutcomeCodeEnum outcomeCode) {
    this.outcomeCode = outcomeCode;
    return this;
  }

  /**
   * fulfilled, partially_fulfilled or declined.
   * @return outcomeCode
   */
  @NotNull 
  @JsonProperty("outcomeCode")
  public OutcomeCodeEnum getOutcomeCode() {
    return outcomeCode;
  }

  @JsonProperty("outcomeCode")
  public void setOutcomeCode(OutcomeCodeEnum outcomeCode) {
    this.outcomeCode = outcomeCode;
  }

  public CloseRightsRequestRequest note(@Nullable String note) {
    this.note = note;
    return this;
  }

  /**
   * The staff's note on the outcome; not shown to the requester.
   * @return note
   */
  @Size(max = 4000) 
  @JsonProperty("note")
  public @Nullable String getNote() {
    return note;
  }

  @JsonProperty("note")
  public void setNote(@Nullable String note) {
    this.note = note;
  }

  public CloseRightsRequestRequest etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Concurrency token from the last read (optional but recommended).
   * @return etag
   */
   
  @JsonProperty("etag")
  public @Nullable String getEtag() {
    return etag;
  }

  @JsonProperty("etag")
  public void setEtag(@Nullable String etag) {
    this.etag = etag;
  }

  public CloseRightsRequestRequest response(@Nullable String response) {
    this.response = response;
    return this;
  }

  /**
   * The school's answer the requester sees: required to close a grievance and to decline a correction.
   * @return response
   */
  @Size(max = 4000) 
  @JsonProperty("response")
  public @Nullable String getResponse() {
    return response;
  }

  @JsonProperty("response")
  public void setResponse(@Nullable String response) {
    this.response = response;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CloseRightsRequestRequest other = (CloseRightsRequestRequest) o;
    return Objects.equals(this.outcomeCode, other.outcomeCode) &&
        Objects.equals(this.note, other.note) &&
        Objects.equals(this.etag, other.etag) &&
        Objects.equals(this.response, other.response);
  }

  @Override
  public int hashCode() {
    return Objects.hash(outcomeCode, note, etag, response);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CloseRightsRequestRequest {\n");
    sb.append("    outcomeCode: ").append(toIndentedString(outcomeCode)).append("\n");
    sb.append("    note: ").append(toIndentedString(note)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
    sb.append("    response: ").append(toIndentedString(response)).append("\n");
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
