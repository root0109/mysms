// Owned API contract of platform.
package com.mysms.platform.api.model;

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
 * Fields of SalesEnquiry that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class SalesEnquiryUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 150) String> contactName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 320) String> email;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 20) String> phone;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> organisationName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 120) String> designation;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 60) String> channel;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> message;

  /**
   * Sales follow-up state.
   */
  public enum StatusEnum {
    NEW("new"),
    
    CONTACTED("contacted"),
    
    QUALIFIED("qualified"),
    
    CONVERTED("converted"),
    
    CLOSED("closed");

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

  public SalesEnquiryUpdate contactName(@Nullable Optional<String> contactName) {
    this.contactName = contactName;
    return this;
  }

  /**
   * Person making the enquiry.
   * @return contactName
   */
  
  @JsonProperty("contactName")
  public @Nullable Optional<String> getContactName() {
    return contactName;
  }

  @JsonProperty("contactName")
  public void setContactName(@Nullable Optional<String> contactName) {
    this.contactName = contactName;
  }

  public SalesEnquiryUpdate email(@Nullable Optional<String> email) {
    this.email = email;
    return this;
  }

  /**
   * Contact e-mail.
   * @return email
   */
  
  @JsonProperty("email")
  public @Nullable Optional<String> getEmail() {
    return email;
  }

  @JsonProperty("email")
  public void setEmail(@Nullable Optional<String> email) {
    this.email = email;
  }

  public SalesEnquiryUpdate phone(@Nullable Optional<String> phone) {
    this.phone = phone;
    return this;
  }

  /**
   * Contact phone.
   * @return phone
   */
  
  @JsonProperty("phone")
  public @Nullable Optional<String> getPhone() {
    return phone;
  }

  @JsonProperty("phone")
  public void setPhone(@Nullable Optional<String> phone) {
    this.phone = phone;
  }

  public SalesEnquiryUpdate organisationName(@Nullable Optional<String> organisationName) {
    this.organisationName = organisationName;
    return this;
  }

  /**
   * School / organisation name.
   * @return organisationName
   */
  
  @JsonProperty("organisationName")
  public @Nullable Optional<String> getOrganisationName() {
    return organisationName;
  }

  @JsonProperty("organisationName")
  public void setOrganisationName(@Nullable Optional<String> organisationName) {
    this.organisationName = organisationName;
  }

  public SalesEnquiryUpdate designation(@Nullable Optional<String> designation) {
    this.designation = designation;
    return this;
  }

  /**
   * Contact's role.
   * @return designation
   */
  
  @JsonProperty("designation")
  public @Nullable Optional<String> getDesignation() {
    return designation;
  }

  @JsonProperty("designation")
  public void setDesignation(@Nullable Optional<String> designation) {
    this.designation = designation;
  }

  public SalesEnquiryUpdate channel(@Nullable Optional<String> channel) {
    this.channel = channel;
    return this;
  }

  /**
   * How the enquiry arrived (web, phone, event).
   * @return channel
   */
  
  @JsonProperty("channel")
  public @Nullable Optional<String> getChannel() {
    return channel;
  }

  @JsonProperty("channel")
  public void setChannel(@Nullable Optional<String> channel) {
    this.channel = channel;
  }

  public SalesEnquiryUpdate message(@Nullable Optional<String> message) {
    this.message = message;
    return this;
  }

  /**
   * Enquiry text.
   * @return message
   */
  
  @JsonProperty("message")
  public @Nullable Optional<String> getMessage() {
    return message;
  }

  @JsonProperty("message")
  public void setMessage(@Nullable Optional<String> message) {
    this.message = message;
  }

  public SalesEnquiryUpdate status(@Nullable Optional<StatusEnum> status) {
    this.status = status;
    return this;
  }

  /**
   * Sales follow-up state.
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
    SalesEnquiryUpdate salesEnquiryUpdate = (SalesEnquiryUpdate) o;
    return Objects.equals(this.contactName, salesEnquiryUpdate.contactName) &&
        Objects.equals(this.email, salesEnquiryUpdate.email) &&
        Objects.equals(this.phone, salesEnquiryUpdate.phone) &&
        Objects.equals(this.organisationName, salesEnquiryUpdate.organisationName) &&
        Objects.equals(this.designation, salesEnquiryUpdate.designation) &&
        Objects.equals(this.channel, salesEnquiryUpdate.channel) &&
        Objects.equals(this.message, salesEnquiryUpdate.message) &&
        Objects.equals(this.status, salesEnquiryUpdate.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(contactName, email, phone, organisationName, designation, channel, message, status);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SalesEnquiryUpdate {\n");
    sb.append("    contactName: ").append(toIndentedString(contactName)).append("\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    phone: ").append(toIndentedString(phone)).append("\n");
    sb.append("    organisationName: ").append(toIndentedString(organisationName)).append("\n");
    sb.append("    designation: ").append(toIndentedString(designation)).append("\n");
    sb.append("    channel: ").append(toIndentedString(channel)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
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

