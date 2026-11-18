// Owned API contract of comms.
package com.mysms.comms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * ExportCalendarRequest
 */

public class ExportCalendarRequest {

  /**
   * External calendar.
   */
  public enum ProviderEnum {
    GOOGLE("google"),
    
    MICROSOFT("microsoft");

    private final String value;

    ProviderEnum(String value) {
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
    public static ProviderEnum fromValue(String value) {
      for (ProviderEnum b : ProviderEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private ProviderEnum provider;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime from;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime to;

  public ExportCalendarRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ExportCalendarRequest(ProviderEnum provider, OffsetDateTime from, OffsetDateTime to) {
    this.provider = provider;
    this.from = from;
    this.to = to;
  }

  public ExportCalendarRequest provider(ProviderEnum provider) {
    this.provider = provider;
    return this;
  }

  /**
   * External calendar.
   * @return provider
   */
  @NotNull 
  @JsonProperty("provider")
  public ProviderEnum getProvider() {
    return provider;
  }

  @JsonProperty("provider")
  public void setProvider(ProviderEnum provider) {
    this.provider = provider;
  }

  public ExportCalendarRequest from(OffsetDateTime from) {
    this.from = from;
    return this;
  }

  /**
   * Window start.
   * @return from
   */
  @NotNull @Valid 
  @JsonProperty("from")
  public OffsetDateTime getFrom() {
    return from;
  }

  @JsonProperty("from")
  public void setFrom(OffsetDateTime from) {
    this.from = from;
  }

  public ExportCalendarRequest to(OffsetDateTime to) {
    this.to = to;
    return this;
  }

  /**
   * Window end.
   * @return to
   */
  @NotNull @Valid 
  @JsonProperty("to")
  public OffsetDateTime getTo() {
    return to;
  }

  @JsonProperty("to")
  public void setTo(OffsetDateTime to) {
    this.to = to;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExportCalendarRequest exportCalendarRequest = (ExportCalendarRequest) o;
    return Objects.equals(this.provider, exportCalendarRequest.provider) &&
        Objects.equals(this.from, exportCalendarRequest.from) &&
        Objects.equals(this.to, exportCalendarRequest.to);
  }

  @Override
  public int hashCode() {
    return Objects.hash(provider, from, to);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExportCalendarRequest {\n");
    sb.append("    provider: ").append(toIndentedString(provider)).append("\n");
    sb.append("    from: ").append(toIndentedString(from)).append("\n");
    sb.append("    to: ").append(toIndentedString(to)).append("\n");
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

