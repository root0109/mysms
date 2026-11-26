// Owned API contract of iam.
package com.mysms.identity.iam.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * A login account. Holds credentials and verification state only; personal data lives in people.person.
 */

public class UserAccount {

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
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID personId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String username;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String email;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String mobileNo;

  /**
   * Account state. Changed only through the workflow operations.
   */
  public enum StatusEnum {
    ACTIVE("active"),
    
    INACTIVE("inactive"),
    
    LOCKED("locked"),
    
    EXITED("exited");

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

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime emailVerifiedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime mobileVerifiedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean mustResetPassword;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID referredByUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime termsAcceptedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String locale;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String timezone;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastLoginAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime accountExpiresAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer failedLoginCount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID lastLoginRoleId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime registeredAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate joinedOn;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String externalRef;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String signature;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean useSignature;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID createdBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime updateTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID updatedBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  public UserAccount() {
    super();
  }

  public UserAccount userType(@Nullable UserTypeEnum userType) {
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

  public UserAccount id(@Nullable UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback).
   * @return id
   */
  @Valid 
  @JsonProperty("id")
  public @Nullable UUID getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable UUID id) {
    this.id = id;
  }

  public UserAccount personId(@Nullable UUID personId) {
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

  public UserAccount username(@Nullable String username) {
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

  public UserAccount email(@Nullable String email) {
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

  public UserAccount mobileNo(@Nullable String mobileNo) {
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

  public UserAccount status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Account state. Changed only through the workflow operations.
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

  public UserAccount emailVerifiedAt(@Nullable OffsetDateTime emailVerifiedAt) {
    this.emailVerifiedAt = emailVerifiedAt;
    return this;
  }

  /**
   * When the e-mail was verified.
   * @return emailVerifiedAt
   */
  @Valid 
  @JsonProperty("emailVerifiedAt")
  public @Nullable OffsetDateTime getEmailVerifiedAt() {
    return emailVerifiedAt;
  }

  @JsonProperty("emailVerifiedAt")
  public void setEmailVerifiedAt(@Nullable OffsetDateTime emailVerifiedAt) {
    this.emailVerifiedAt = emailVerifiedAt;
  }

  public UserAccount mobileVerifiedAt(@Nullable OffsetDateTime mobileVerifiedAt) {
    this.mobileVerifiedAt = mobileVerifiedAt;
    return this;
  }

  /**
   * When the mobile number was verified.
   * @return mobileVerifiedAt
   */
  @Valid 
  @JsonProperty("mobileVerifiedAt")
  public @Nullable OffsetDateTime getMobileVerifiedAt() {
    return mobileVerifiedAt;
  }

  @JsonProperty("mobileVerifiedAt")
  public void setMobileVerifiedAt(@Nullable OffsetDateTime mobileVerifiedAt) {
    this.mobileVerifiedAt = mobileVerifiedAt;
  }

  public UserAccount mustResetPassword(@Nullable Boolean mustResetPassword) {
    this.mustResetPassword = mustResetPassword;
    return this;
  }

  /**
   * Force a password change at next login.
   * @return mustResetPassword
   */
  
  @JsonProperty("mustResetPassword")
  public @Nullable Boolean getMustResetPassword() {
    return mustResetPassword;
  }

  @JsonProperty("mustResetPassword")
  public void setMustResetPassword(@Nullable Boolean mustResetPassword) {
    this.mustResetPassword = mustResetPassword;
  }

  public UserAccount referredByUserId(@Nullable UUID referredByUserId) {
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

  public UserAccount termsAcceptedAt(@Nullable OffsetDateTime termsAcceptedAt) {
    this.termsAcceptedAt = termsAcceptedAt;
    return this;
  }

  /**
   * When the user accepted the terms of use.
   * @return termsAcceptedAt
   */
  @Valid 
  @JsonProperty("termsAcceptedAt")
  public @Nullable OffsetDateTime getTermsAcceptedAt() {
    return termsAcceptedAt;
  }

  @JsonProperty("termsAcceptedAt")
  public void setTermsAcceptedAt(@Nullable OffsetDateTime termsAcceptedAt) {
    this.termsAcceptedAt = termsAcceptedAt;
  }

  public UserAccount locale(@Nullable String locale) {
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

  public UserAccount timezone(@Nullable String timezone) {
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

  public UserAccount lastLoginAt(@Nullable OffsetDateTime lastLoginAt) {
    this.lastLoginAt = lastLoginAt;
    return this;
  }

  /**
   * Last successful login.
   * @return lastLoginAt
   */
  @Valid 
  @JsonProperty("lastLoginAt")
  public @Nullable OffsetDateTime getLastLoginAt() {
    return lastLoginAt;
  }

  @JsonProperty("lastLoginAt")
  public void setLastLoginAt(@Nullable OffsetDateTime lastLoginAt) {
    this.lastLoginAt = lastLoginAt;
  }

  public UserAccount accountExpiresAt(@Nullable OffsetDateTime accountExpiresAt) {
    this.accountExpiresAt = accountExpiresAt;
    return this;
  }

  /**
   * Account stops working after this time (fixed-term students, guests).
   * @return accountExpiresAt
   */
  @Valid 
  @JsonProperty("accountExpiresAt")
  public @Nullable OffsetDateTime getAccountExpiresAt() {
    return accountExpiresAt;
  }

  @JsonProperty("accountExpiresAt")
  public void setAccountExpiresAt(@Nullable OffsetDateTime accountExpiresAt) {
    this.accountExpiresAt = accountExpiresAt;
  }

  public UserAccount failedLoginCount(@Nullable Integer failedLoginCount) {
    this.failedLoginCount = failedLoginCount;
    return this;
  }

  /**
   * Consecutive failed sign-ins (lock-out after the configured limit).
   * @return failedLoginCount
   */
  
  @JsonProperty("failedLoginCount")
  public @Nullable Integer getFailedLoginCount() {
    return failedLoginCount;
  }

  @JsonProperty("failedLoginCount")
  public void setFailedLoginCount(@Nullable Integer failedLoginCount) {
    this.failedLoginCount = failedLoginCount;
  }

  public UserAccount lastLoginRoleId(@Nullable UUID lastLoginRoleId) {
    this.lastLoginRoleId = lastLoginRoleId;
    return this;
  }

  /**
   * Role the user last worked in (restored at next sign-in).
   * @return lastLoginRoleId
   */
  @Valid 
  @JsonProperty("lastLoginRoleId")
  public @Nullable UUID getLastLoginRoleId() {
    return lastLoginRoleId;
  }

  @JsonProperty("lastLoginRoleId")
  public void setLastLoginRoleId(@Nullable UUID lastLoginRoleId) {
    this.lastLoginRoleId = lastLoginRoleId;
  }

  public UserAccount registeredAt(@Nullable OffsetDateTime registeredAt) {
    this.registeredAt = registeredAt;
    return this;
  }

  /**
   * Self-registration time.
   * @return registeredAt
   */
  @Valid 
  @JsonProperty("registeredAt")
  public @Nullable OffsetDateTime getRegisteredAt() {
    return registeredAt;
  }

  @JsonProperty("registeredAt")
  public void setRegisteredAt(@Nullable OffsetDateTime registeredAt) {
    this.registeredAt = registeredAt;
  }

  public UserAccount joinedOn(@Nullable LocalDate joinedOn) {
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

  public UserAccount externalRef(@Nullable String externalRef) {
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

  public UserAccount signature(@Nullable String signature) {
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

  public UserAccount useSignature(@Nullable Boolean useSignature) {
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

  public UserAccount createTime(@Nullable OffsetDateTime createTime) {
    this.createTime = createTime;
    return this;
  }

  /**
   * Row creation time.
   * @return createTime
   */
  @Valid 
  @JsonProperty("createTime")
  public @Nullable OffsetDateTime getCreateTime() {
    return createTime;
  }

  @JsonProperty("createTime")
  public void setCreateTime(@Nullable OffsetDateTime createTime) {
    this.createTime = createTime;
  }

  public UserAccount createdBy(@Nullable UUID createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * User account that created the row (audit reference to iam.user_account; not FK-constrained).
   * @return createdBy
   */
  @Valid 
  @JsonProperty("createdBy")
  public @Nullable UUID getCreatedBy() {
    return createdBy;
  }

  @JsonProperty("createdBy")
  public void setCreatedBy(@Nullable UUID createdBy) {
    this.createdBy = createdBy;
  }

  public UserAccount updateTime(@Nullable OffsetDateTime updateTime) {
    this.updateTime = updateTime;
    return this;
  }

  /**
   * Last update time.
   * @return updateTime
   */
  @Valid 
  @JsonProperty("updateTime")
  public @Nullable OffsetDateTime getUpdateTime() {
    return updateTime;
  }

  @JsonProperty("updateTime")
  public void setUpdateTime(@Nullable OffsetDateTime updateTime) {
    this.updateTime = updateTime;
  }

  public UserAccount updatedBy(@Nullable UUID updatedBy) {
    this.updatedBy = updatedBy;
    return this;
  }

  /**
   * User account that last updated the row (audit reference to iam.user_account; not FK-constrained).
   * @return updatedBy
   */
  @Valid 
  @JsonProperty("updatedBy")
  public @Nullable UUID getUpdatedBy() {
    return updatedBy;
  }

  @JsonProperty("updatedBy")
  public void setUpdatedBy(@Nullable UUID updatedBy) {
    this.updatedBy = updatedBy;
  }

  public UserAccount etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Entity tag (row version); send back in If-Match / etag.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserAccount userAccount = (UserAccount) o;
    return Objects.equals(this.userType, userAccount.userType) &&
        Objects.equals(this.id, userAccount.id) &&
        Objects.equals(this.personId, userAccount.personId) &&
        Objects.equals(this.username, userAccount.username) &&
        Objects.equals(this.email, userAccount.email) &&
        Objects.equals(this.mobileNo, userAccount.mobileNo) &&
        Objects.equals(this.status, userAccount.status) &&
        Objects.equals(this.emailVerifiedAt, userAccount.emailVerifiedAt) &&
        Objects.equals(this.mobileVerifiedAt, userAccount.mobileVerifiedAt) &&
        Objects.equals(this.mustResetPassword, userAccount.mustResetPassword) &&
        Objects.equals(this.referredByUserId, userAccount.referredByUserId) &&
        Objects.equals(this.termsAcceptedAt, userAccount.termsAcceptedAt) &&
        Objects.equals(this.locale, userAccount.locale) &&
        Objects.equals(this.timezone, userAccount.timezone) &&
        Objects.equals(this.lastLoginAt, userAccount.lastLoginAt) &&
        Objects.equals(this.accountExpiresAt, userAccount.accountExpiresAt) &&
        Objects.equals(this.failedLoginCount, userAccount.failedLoginCount) &&
        Objects.equals(this.lastLoginRoleId, userAccount.lastLoginRoleId) &&
        Objects.equals(this.registeredAt, userAccount.registeredAt) &&
        Objects.equals(this.joinedOn, userAccount.joinedOn) &&
        Objects.equals(this.externalRef, userAccount.externalRef) &&
        Objects.equals(this.signature, userAccount.signature) &&
        Objects.equals(this.useSignature, userAccount.useSignature) &&
        Objects.equals(this.createTime, userAccount.createTime) &&
        Objects.equals(this.createdBy, userAccount.createdBy) &&
        Objects.equals(this.updateTime, userAccount.updateTime) &&
        Objects.equals(this.updatedBy, userAccount.updatedBy) &&
        Objects.equals(this.etag, userAccount.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userType, id, personId, username, email, mobileNo, status, emailVerifiedAt, mobileVerifiedAt, mustResetPassword, referredByUserId, termsAcceptedAt, locale, timezone, lastLoginAt, accountExpiresAt, failedLoginCount, lastLoginRoleId, registeredAt, joinedOn, externalRef, signature, useSignature, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserAccount {\n");
    sb.append("    userType: ").append(toIndentedString(userType)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    personId: ").append(toIndentedString(personId)).append("\n");
    sb.append("    username: ").append(toIndentedString(username)).append("\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    mobileNo: ").append(toIndentedString(mobileNo)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    emailVerifiedAt: ").append(toIndentedString(emailVerifiedAt)).append("\n");
    sb.append("    mobileVerifiedAt: ").append(toIndentedString(mobileVerifiedAt)).append("\n");
    sb.append("    mustResetPassword: ").append(toIndentedString(mustResetPassword)).append("\n");
    sb.append("    referredByUserId: ").append(toIndentedString(referredByUserId)).append("\n");
    sb.append("    termsAcceptedAt: ").append(toIndentedString(termsAcceptedAt)).append("\n");
    sb.append("    locale: ").append(toIndentedString(locale)).append("\n");
    sb.append("    timezone: ").append(toIndentedString(timezone)).append("\n");
    sb.append("    lastLoginAt: ").append(toIndentedString(lastLoginAt)).append("\n");
    sb.append("    accountExpiresAt: ").append(toIndentedString(accountExpiresAt)).append("\n");
    sb.append("    failedLoginCount: ").append(toIndentedString(failedLoginCount)).append("\n");
    sb.append("    lastLoginRoleId: ").append(toIndentedString(lastLoginRoleId)).append("\n");
    sb.append("    registeredAt: ").append(toIndentedString(registeredAt)).append("\n");
    sb.append("    joinedOn: ").append(toIndentedString(joinedOn)).append("\n");
    sb.append("    externalRef: ").append(toIndentedString(externalRef)).append("\n");
    sb.append("    signature: ").append(toIndentedString(signature)).append("\n");
    sb.append("    useSignature: ").append(toIndentedString(useSignature)).append("\n");
    sb.append("    createTime: ").append(toIndentedString(createTime)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    updateTime: ").append(toIndentedString(updateTime)).append("\n");
    sb.append("    updatedBy: ").append(toIndentedString(updatedBy)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
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

