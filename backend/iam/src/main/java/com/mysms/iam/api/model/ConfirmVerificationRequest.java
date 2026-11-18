// Owned API contract of iam.
package com.mysms.iam.api.model;

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
 * ConfirmVerificationRequest
 */

public class ConfirmVerificationRequest {

  /**
   * What is verified.
   */
  public enum ChannelEnum {
    EMAIL("email"),
    
    MOBILE("mobile");

    private final String value;

    ChannelEnum(String value) {
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
    public static ChannelEnum fromValue(String value) {
      for (ChannelEnum b : ChannelEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private ChannelEnum channel;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String code;

  public ConfirmVerificationRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ConfirmVerificationRequest(ChannelEnum channel, String code) {
    this.channel = channel;
    this.code = code;
  }

  public ConfirmVerificationRequest channel(ChannelEnum channel) {
    this.channel = channel;
    return this;
  }

  /**
   * What is verified.
   * @return channel
   */
  @NotNull 
  @JsonProperty("channel")
  public ChannelEnum getChannel() {
    return channel;
  }

  @JsonProperty("channel")
  public void setChannel(ChannelEnum channel) {
    this.channel = channel;
  }

  public ConfirmVerificationRequest code(String code) {
    this.code = code;
    return this;
  }

  /**
   * Code received.
   * @return code
   */
  @NotNull 
  @JsonProperty("code")
  public String getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(String code) {
    this.code = code;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConfirmVerificationRequest confirmVerificationRequest = (ConfirmVerificationRequest) o;
    return Objects.equals(this.channel, confirmVerificationRequest.channel) &&
        Objects.equals(this.code, confirmVerificationRequest.code);
  }

  @Override
  public int hashCode() {
    return Objects.hash(channel, code);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConfirmVerificationRequest {\n");
    sb.append("    channel: ").append(toIndentedString(channel)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
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

