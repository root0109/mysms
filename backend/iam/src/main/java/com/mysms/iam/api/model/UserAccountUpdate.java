// Owned API contract of iam.
package com.mysms.iam.api.model;

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
 * Fields of UserAccount that can be updated (partial update; see updateMask).
 */

public class UserAccountUpdate {

  /**
   * Default persona hint used for routing after login. Never used for authorisation: permissions come only from roles.
   */
  public enum UserTypeEnum {
    ADMIN("admin"),
    
    TEACHER("teacher"),
    
    STUDENT("student"),
    
    GUARDIAN("guardian"),
    
    STAFF("staff"),
    
    LIBRARIAN("librarian"),
    
    ACCOUNTANT("accountant"),
    
    ALUMNI("alumni");

    private final String value;

    UserTypeEnum(String value) {
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
    public static UserTypeEnum fromValue(String value) {
      for (UserTypeEnum b : UserTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UserTypeEnum userType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID personId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String username;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String email;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String mobileNo;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID referredByUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String locale;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String timezone;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate joinedOn;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String externalRef;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String signature;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean useSignature;

  public UserAccountUpdate userType(@Nullable UserTypeEnum userType) {
    this.userType = userType;
    return this;
  }

  /**
   * Default persona hint used for routing after login. Never used for authorisation: permissions come only from roles.
   * @return userType
   */
  
  @JsonProperty("userType")
  public @Nullable UserTypeEnum getUserType() {
    return userType;
  }

  @JsonProperty("userType")
  public void setUserType(@Nullable UserTypeEnum userType) {
    this.userType = userType;
  }

  public UserAccountUpdate personId(@Nullable UUID personId) {
    this.personId = personId;
    return this;
  }

  /**
   * Person this account belongs to (NULL for service accounts).
   * @return personId
   */
  @Valid 
  @JsonProperty("personId")
  public @Nullable UUID getPersonId() {
    return personId;
  }

  @JsonProperty("personId")
  public void setPersonId(@Nullable UUID personId) {
    this.personId = personId;
  }

  public UserAccountUpdate username(@Nullable String username) {
    this.username = username;
    return this;
  }

  /**
   * Login name, unique per tenant ignoring case (optional when the user logs in by e-mail or mobile).
   * @return username
   */
  @Size(max = 150) 
  @JsonProperty("username")
  public @Nullable String getUsername() {
    return username;
  }

  @JsonProperty("username")
  public void setUsername(@Nullable String username) {
    this.username = username;
  }

  public UserAccountUpdate email(@Nullable String email) {
    this.email = email;
    return this;
  }

  /**
   * Login / notification e-mail, unique per tenant ignoring case.
   * @return email
   */
  @Size(max = 320) 
  @JsonProperty("email")
  public @Nullable String getEmail() {
    return email;
  }

  @JsonProperty("email")
  public void setEmail(@Nullable String email) {
    this.email = email;
  }

  public UserAccountUpdate mobileNo(@Nullable String mobileNo) {
    this.mobileNo = mobileNo;
    return this;
  }

  /**
   * Login / notification mobile number.
   * @return mobileNo
   */
  @Size(max = 20) 
  @JsonProperty("mobileNo")
  public @Nullable String getMobileNo() {
    return mobileNo;
  }

  @JsonProperty("mobileNo")
  public void setMobileNo(@Nullable String mobileNo) {
    this.mobileNo = mobileNo;
  }

  public UserAccountUpdate referredByUserId(@Nullable UUID referredByUserId) {
    this.referredByUserId = referredByUserId;
    return this;
  }

  /**
   * Account that created / referred this account.
   * @return referredByUserId
   */
  @Valid 
  @JsonProperty("referredByUserId")
  public @Nullable UUID getReferredByUserId() {
    return referredByUserId;
  }

  @JsonProperty("referredByUserId")
  public void setReferredByUserId(@Nullable UUID referredByUserId) {
    this.referredByUserId = referredByUserId;
  }

  public UserAccountUpdate locale(@Nullable String locale) {
    this.locale = locale;
    return this;
  }

  /**
   * Preferred UI locale.
   * @return locale
   */
  @Size(max = 10) 
  @JsonProperty("locale")
  public @Nullable String getLocale() {
    return locale;
  }

  @JsonProperty("locale")
  public void setLocale(@Nullable String locale) {
    this.locale = locale;
  }

  public UserAccountUpdate timezone(@Nullable String timezone) {
    this.timezone = timezone;
    return this;
  }

  /**
   * Preferred time zone.
   * @return timezone
   */
  @Size(max = 64) 
  @JsonProperty("timezone")
  public @Nullable String getTimezone() {
    return timezone;
  }

  @JsonProperty("timezone")
  public void setTimezone(@Nullable String timezone) {
    this.timezone = timezone;
  }

  public UserAccountUpdate joinedOn(@Nullable LocalDate joinedOn) {
    this.joinedOn = joinedOn;
    return this;
  }

  /**
   * Date the user joined the institution.
   * @return joinedOn
   */
  @Valid 
  @JsonProperty("joinedOn")
  public @Nullable LocalDate getJoinedOn() {
    return joinedOn;
  }

  @JsonProperty("joinedOn")
  public void setJoinedOn(@Nullable LocalDate joinedOn) {
    this.joinedOn = joinedOn;
  }

  public UserAccountUpdate externalRef(@Nullable String externalRef) {
    this.externalRef = externalRef;
    return this;
  }

  /**
   * External system reference (SIS / HR integration key).
   * @return externalRef
   */
  @Size(max = 100) 
  @JsonProperty("externalRef")
  public @Nullable String getExternalRef() {
    return externalRef;
  }

  @JsonProperty("externalRef")
  public void setExternalRef(@Nullable String externalRef) {
    this.externalRef = externalRef;
  }

  public UserAccountUpdate signature(@Nullable String signature) {
    this.signature = signature;
    return this;
  }

  /**
   * Signature appended to discussion posts.
   * @return signature
   */
  
  @JsonProperty("signature")
  public @Nullable String getSignature() {
    return signature;
  }

  @JsonProperty("signature")
  public void setSignature(@Nullable String signature) {
    this.signature = signature;
  }

  public UserAccountUpdate useSignature(@Nullable Boolean useSignature) {
    this.useSignature = useSignature;
    return this;
  }

  /**
   * Append the signature by default.
   * @return useSignature
   */
  
  @JsonProperty("useSignature")
  public @Nullable Boolean getUseSignature() {
    return useSignature;
  }

  @JsonProperty("useSignature")
  public void setUseSignature(@Nullable Boolean useSignature) {
    this.useSignature = useSignature;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserAccountUpdate userAccountUpdate = (UserAccountUpdate) o;
    return Objects.equals(this.userType, userAccountUpdate.userType) &&
        Objects.equals(this.personId, userAccountUpdate.personId) &&
        Objects.equals(this.username, userAccountUpdate.username) &&
        Objects.equals(this.email, userAccountUpdate.email) &&
        Objects.equals(this.mobileNo, userAccountUpdate.mobileNo) &&
        Objects.equals(this.referredByUserId, userAccountUpdate.referredByUserId) &&
        Objects.equals(this.locale, userAccountUpdate.locale) &&
        Objects.equals(this.timezone, userAccountUpdate.timezone) &&
        Objects.equals(this.joinedOn, userAccountUpdate.joinedOn) &&
        Objects.equals(this.externalRef, userAccountUpdate.externalRef) &&
        Objects.equals(this.signature, userAccountUpdate.signature) &&
        Objects.equals(this.useSignature, userAccountUpdate.useSignature);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userType, personId, username, email, mobileNo, referredByUserId, locale, timezone, joinedOn, externalRef, signature, useSignature);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserAccountUpdate {\n");
    sb.append("    userType: ").append(toIndentedString(userType)).append("\n");
    sb.append("    personId: ").append(toIndentedString(personId)).append("\n");
    sb.append("    username: ").append(toIndentedString(username)).append("\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    mobileNo: ").append(toIndentedString(mobileNo)).append("\n");
    sb.append("    referredByUserId: ").append(toIndentedString(referredByUserId)).append("\n");
    sb.append("    locale: ").append(toIndentedString(locale)).append("\n");
    sb.append("    timezone: ").append(toIndentedString(timezone)).append("\n");
    sb.append("    joinedOn: ").append(toIndentedString(joinedOn)).append("\n");
    sb.append("    externalRef: ").append(toIndentedString(externalRef)).append("\n");
    sb.append("    signature: ").append(toIndentedString(signature)).append("\n");
    sb.append("    useSignature: ").append(toIndentedString(useSignature)).append("\n");
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

