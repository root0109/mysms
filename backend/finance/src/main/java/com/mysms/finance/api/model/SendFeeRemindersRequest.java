// Owned API contract of finance.
package com.mysms.finance.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * SendFeeRemindersRequest
 */

public class SendFeeRemindersRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<UUID> invoiceIds = new ArrayList<>();

  /**
   * Channel.
   */
  public enum ChannelEnum {
    SMS("sms"),
    
    EMAIL("email"),
    
    PUSH("push");

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

  public SendFeeRemindersRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SendFeeRemindersRequest(UUID institutionId, ChannelEnum channel) {
    this.institutionId = institutionId;
    this.channel = channel;
  }

  public SendFeeRemindersRequest institutionId(UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  @NotNull @Valid 
  @JsonProperty("institutionId")
  public UUID getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(UUID institutionId) {
    this.institutionId = institutionId;
  }

  public SendFeeRemindersRequest invoiceIds(List<UUID> invoiceIds) {
    this.invoiceIds = invoiceIds;
    return this;
  }

  public SendFeeRemindersRequest addInvoiceIdsItem(UUID invoiceIdsItem) {
    if (this.invoiceIds == null) {
      this.invoiceIds = new ArrayList<>();
    }
    this.invoiceIds.add(invoiceIdsItem);
    return this;
  }

  /**
   * Invoices (default all overdue).
   * @return invoiceIds
   */
  
  @JsonProperty("invoiceIds")
  public List<UUID> getInvoiceIds() {
    return invoiceIds;
  }

  @JsonProperty("invoiceIds")
  public void setInvoiceIds(List<UUID> invoiceIds) {
    this.invoiceIds = invoiceIds;
  }

  public SendFeeRemindersRequest channel(ChannelEnum channel) {
    this.channel = channel;
    return this;
  }

  /**
   * Channel.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SendFeeRemindersRequest sendFeeRemindersRequest = (SendFeeRemindersRequest) o;
    return Objects.equals(this.institutionId, sendFeeRemindersRequest.institutionId) &&
        Objects.equals(this.invoiceIds, sendFeeRemindersRequest.invoiceIds) &&
        Objects.equals(this.channel, sendFeeRemindersRequest.channel);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, invoiceIds, channel);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SendFeeRemindersRequest {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    invoiceIds: ").append(toIndentedString(invoiceIds)).append("\n");
    sb.append("    channel: ").append(toIndentedString(channel)).append("\n");
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

