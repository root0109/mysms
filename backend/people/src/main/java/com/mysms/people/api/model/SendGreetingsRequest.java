// Owned API contract of people (JAVA-CODING-RULES §7); first materialized from api/openapi/people.yaml on 2026-11-10.
package com.mysms.people.api.model;

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
 * SendGreetingsRequest
 */

public class SendGreetingsRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<UUID> personIds = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID templateId;

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

  public SendGreetingsRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SendGreetingsRequest(List<UUID> personIds, UUID templateId, ChannelEnum channel) {
    this.personIds = personIds;
    this.templateId = templateId;
    this.channel = channel;
  }

  public SendGreetingsRequest personIds(List<UUID> personIds) {
    this.personIds = personIds;
    return this;
  }

  public SendGreetingsRequest addPersonIdsItem(UUID personIdsItem) {
    if (this.personIds == null) {
      this.personIds = new ArrayList<>();
    }
    this.personIds.add(personIdsItem);
    return this;
  }

  /**
   * People to greet.
   * @return personIds
   */
  @NotNull 
  @JsonProperty("personIds")
  public List<UUID> getPersonIds() {
    return personIds;
  }

  @JsonProperty("personIds")
  public void setPersonIds(List<UUID> personIds) {
    this.personIds = personIds;
  }

  public SendGreetingsRequest templateId(UUID templateId) {
    this.templateId = templateId;
    return this;
  }

  /**
   * Message template.
   * @return templateId
   */
  @NotNull @Valid 
  @JsonProperty("templateId")
  public UUID getTemplateId() {
    return templateId;
  }

  @JsonProperty("templateId")
  public void setTemplateId(UUID templateId) {
    this.templateId = templateId;
  }

  public SendGreetingsRequest channel(ChannelEnum channel) {
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
    SendGreetingsRequest sendGreetingsRequest = (SendGreetingsRequest) o;
    return Objects.equals(this.personIds, sendGreetingsRequest.personIds) &&
        Objects.equals(this.templateId, sendGreetingsRequest.templateId) &&
        Objects.equals(this.channel, sendGreetingsRequest.channel);
  }

  @Override
  public int hashCode() {
    return Objects.hash(personIds, templateId, channel);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SendGreetingsRequest {\n");
    sb.append("    personIds: ").append(toIndentedString(personIds)).append("\n");
    sb.append("    templateId: ").append(toIndentedString(templateId)).append("\n");
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

