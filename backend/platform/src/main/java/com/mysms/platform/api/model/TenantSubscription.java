// Owned API contract of platform.
package com.mysms.platform.api.model;

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
 * A tenant&#39;s (or one institution&#39;s) subscription to a SaaS plan.
 */

public class TenantSubscription {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer maxMembers;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String price;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID currencyId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer billingCycleMonths;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String gatewayName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String gatewayTxnRef;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String amountPaid;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String gatewayStatus;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime paidAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String payerName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID planId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID requestedByUserId;

  /**
   * Subscription state. Changed only through the workflow operations.
   */
  public enum StatusEnum {
    PENDING("pending"),
    
    ACTIVE("active"),
    
    CANCELLED("cancelled"),
    
    EXPIRED("expired");

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
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate startDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate endDate;

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

  public TenantSubscription() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TenantSubscription(UUID requestedByUserId) {
    this.requestedByUserId = requestedByUserId;
  }

  public TenantSubscription maxMembers(@Nullable Integer maxMembers) {
    this.maxMembers = maxMembers;
    return this;
  }

  /**
   * Plan limit copied at purchase (absent = unlimited).
   * @return maxMembers
   */
  
  @JsonProperty("maxMembers")
  public @Nullable Integer getMaxMembers() {
    return maxMembers;
  }

  @JsonProperty("maxMembers")
  public void setMaxMembers(@Nullable Integer maxMembers) {
    this.maxMembers = maxMembers;
  }

  public TenantSubscription price(@Nullable String price) {
    this.price = price;
    return this;
  }

  /**
   * Price paid for the cycle.
   * @return price
   */
  @Pattern(regexp = "^\\d+(\\.\\d+)?$") 
  @JsonProperty("price")
  public @Nullable String getPrice() {
    return price;
  }

  @JsonProperty("price")
  public void setPrice(@Nullable String price) {
    this.price = price;
  }

  public TenantSubscription currencyId(@Nullable UUID currencyId) {
    this.currencyId = currencyId;
    return this;
  }

  /**
   * Currency of the purchase.
   * @return currencyId
   */
  @Valid 
  @JsonProperty("currencyId")
  public @Nullable UUID getCurrencyId() {
    return currencyId;
  }

  @JsonProperty("currencyId")
  public void setCurrencyId(@Nullable UUID currencyId) {
    this.currencyId = currencyId;
  }

  public TenantSubscription billingCycleMonths(@Nullable Integer billingCycleMonths) {
    this.billingCycleMonths = billingCycleMonths;
    return this;
  }

  /**
   * Billing cycle purchased, in months.
   * @return billingCycleMonths
   */
  
  @JsonProperty("billingCycleMonths")
  public @Nullable Integer getBillingCycleMonths() {
    return billingCycleMonths;
  }

  @JsonProperty("billingCycleMonths")
  public void setBillingCycleMonths(@Nullable Integer billingCycleMonths) {
    this.billingCycleMonths = billingCycleMonths;
  }

  public TenantSubscription gatewayName(@Nullable String gatewayName) {
    this.gatewayName = gatewayName;
    return this;
  }

  /**
   * Gateway that took the subscription payment.
   * @return gatewayName
   */
  
  @JsonProperty("gatewayName")
  public @Nullable String getGatewayName() {
    return gatewayName;
  }

  @JsonProperty("gatewayName")
  public void setGatewayName(@Nullable String gatewayName) {
    this.gatewayName = gatewayName;
  }

  public TenantSubscription gatewayTxnRef(@Nullable String gatewayTxnRef) {
    this.gatewayTxnRef = gatewayTxnRef;
    return this;
  }

  /**
   * Gateway transaction reference.
   * @return gatewayTxnRef
   */
  
  @JsonProperty("gatewayTxnRef")
  public @Nullable String getGatewayTxnRef() {
    return gatewayTxnRef;
  }

  @JsonProperty("gatewayTxnRef")
  public void setGatewayTxnRef(@Nullable String gatewayTxnRef) {
    this.gatewayTxnRef = gatewayTxnRef;
  }

  public TenantSubscription amountPaid(@Nullable String amountPaid) {
    this.amountPaid = amountPaid;
    return this;
  }

  /**
   * Amount actually captured.
   * @return amountPaid
   */
  @Pattern(regexp = "^\\d+(\\.\\d+)?$") 
  @JsonProperty("amountPaid")
  public @Nullable String getAmountPaid() {
    return amountPaid;
  }

  @JsonProperty("amountPaid")
  public void setAmountPaid(@Nullable String amountPaid) {
    this.amountPaid = amountPaid;
  }

  public TenantSubscription gatewayStatus(@Nullable String gatewayStatus) {
    this.gatewayStatus = gatewayStatus;
    return this;
  }

  /**
   * Status reported by the gateway.
   * @return gatewayStatus
   */
  
  @JsonProperty("gatewayStatus")
  public @Nullable String getGatewayStatus() {
    return gatewayStatus;
  }

  @JsonProperty("gatewayStatus")
  public void setGatewayStatus(@Nullable String gatewayStatus) {
    this.gatewayStatus = gatewayStatus;
  }

  public TenantSubscription paidAt(@Nullable OffsetDateTime paidAt) {
    this.paidAt = paidAt;
    return this;
  }

  /**
   * When the payment was captured.
   * @return paidAt
   */
  @Valid 
  @JsonProperty("paidAt")
  public @Nullable OffsetDateTime getPaidAt() {
    return paidAt;
  }

  @JsonProperty("paidAt")
  public void setPaidAt(@Nullable OffsetDateTime paidAt) {
    this.paidAt = paidAt;
  }

  public TenantSubscription payerName(@Nullable String payerName) {
    this.payerName = payerName;
    return this;
  }

  /**
   * Payer name as reported.
   * @return payerName
   */
  
  @JsonProperty("payerName")
  public @Nullable String getPayerName() {
    return payerName;
  }

  @JsonProperty("payerName")
  public void setPayerName(@Nullable String payerName) {
    this.payerName = payerName;
  }

  public TenantSubscription id(@Nullable UUID id) {
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

  public TenantSubscription institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution the subscription covers; NULL = whole tenant.
   * @return institutionId
   */
  @Valid 
  @JsonProperty("institutionId")
  public @Nullable UUID getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
  }

  public TenantSubscription planId(@Nullable UUID planId) {
    this.planId = planId;
    return this;
  }

  /**
   * Subscribed plan (NULL while a custom plan is being negotiated).
   * @return planId
   */
  @Valid 
  @JsonProperty("planId")
  public @Nullable UUID getPlanId() {
    return planId;
  }

  @JsonProperty("planId")
  public void setPlanId(@Nullable UUID planId) {
    this.planId = planId;
  }

  public TenantSubscription requestedByUserId(UUID requestedByUserId) {
    this.requestedByUserId = requestedByUserId;
    return this;
  }

  /**
   * User who requested the subscription.
   * @return requestedByUserId
   */
  @NotNull @Valid 
  @JsonProperty("requestedByUserId")
  public UUID getRequestedByUserId() {
    return requestedByUserId;
  }

  @JsonProperty("requestedByUserId")
  public void setRequestedByUserId(UUID requestedByUserId) {
    this.requestedByUserId = requestedByUserId;
  }

  public TenantSubscription status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Subscription state. Changed only through the workflow operations.
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

  public TenantSubscription startDate(@Nullable LocalDate startDate) {
    this.startDate = startDate;
    return this;
  }

  /**
   * Start of the subscription.
   * @return startDate
   */
  @Valid 
  @JsonProperty("startDate")
  public @Nullable LocalDate getStartDate() {
    return startDate;
  }

  @JsonProperty("startDate")
  public void setStartDate(@Nullable LocalDate startDate) {
    this.startDate = startDate;
  }

  public TenantSubscription endDate(@Nullable LocalDate endDate) {
    this.endDate = endDate;
    return this;
  }

  /**
   * End of the current paid period.
   * @return endDate
   */
  @Valid 
  @JsonProperty("endDate")
  public @Nullable LocalDate getEndDate() {
    return endDate;
  }

  @JsonProperty("endDate")
  public void setEndDate(@Nullable LocalDate endDate) {
    this.endDate = endDate;
  }

  public TenantSubscription createTime(@Nullable OffsetDateTime createTime) {
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

  public TenantSubscription createdBy(@Nullable UUID createdBy) {
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

  public TenantSubscription updateTime(@Nullable OffsetDateTime updateTime) {
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

  public TenantSubscription updatedBy(@Nullable UUID updatedBy) {
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

  public TenantSubscription etag(@Nullable String etag) {
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
    TenantSubscription tenantSubscription = (TenantSubscription) o;
    return Objects.equals(this.maxMembers, tenantSubscription.maxMembers) &&
        Objects.equals(this.price, tenantSubscription.price) &&
        Objects.equals(this.currencyId, tenantSubscription.currencyId) &&
        Objects.equals(this.billingCycleMonths, tenantSubscription.billingCycleMonths) &&
        Objects.equals(this.gatewayName, tenantSubscription.gatewayName) &&
        Objects.equals(this.gatewayTxnRef, tenantSubscription.gatewayTxnRef) &&
        Objects.equals(this.amountPaid, tenantSubscription.amountPaid) &&
        Objects.equals(this.gatewayStatus, tenantSubscription.gatewayStatus) &&
        Objects.equals(this.paidAt, tenantSubscription.paidAt) &&
        Objects.equals(this.payerName, tenantSubscription.payerName) &&
        Objects.equals(this.id, tenantSubscription.id) &&
        Objects.equals(this.institutionId, tenantSubscription.institutionId) &&
        Objects.equals(this.planId, tenantSubscription.planId) &&
        Objects.equals(this.requestedByUserId, tenantSubscription.requestedByUserId) &&
        Objects.equals(this.status, tenantSubscription.status) &&
        Objects.equals(this.startDate, tenantSubscription.startDate) &&
        Objects.equals(this.endDate, tenantSubscription.endDate) &&
        Objects.equals(this.createTime, tenantSubscription.createTime) &&
        Objects.equals(this.createdBy, tenantSubscription.createdBy) &&
        Objects.equals(this.updateTime, tenantSubscription.updateTime) &&
        Objects.equals(this.updatedBy, tenantSubscription.updatedBy) &&
        Objects.equals(this.etag, tenantSubscription.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(maxMembers, price, currencyId, billingCycleMonths, gatewayName, gatewayTxnRef, amountPaid, gatewayStatus, paidAt, payerName, id, institutionId, planId, requestedByUserId, status, startDate, endDate, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TenantSubscription {\n");
    sb.append("    maxMembers: ").append(toIndentedString(maxMembers)).append("\n");
    sb.append("    price: ").append(toIndentedString(price)).append("\n");
    sb.append("    currencyId: ").append(toIndentedString(currencyId)).append("\n");
    sb.append("    billingCycleMonths: ").append(toIndentedString(billingCycleMonths)).append("\n");
    sb.append("    gatewayName: ").append(toIndentedString(gatewayName)).append("\n");
    sb.append("    gatewayTxnRef: ").append(toIndentedString(gatewayTxnRef)).append("\n");
    sb.append("    amountPaid: ").append(toIndentedString(amountPaid)).append("\n");
    sb.append("    gatewayStatus: ").append(toIndentedString(gatewayStatus)).append("\n");
    sb.append("    paidAt: ").append(toIndentedString(paidAt)).append("\n");
    sb.append("    payerName: ").append(toIndentedString(payerName)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    planId: ").append(toIndentedString(planId)).append("\n");
    sb.append("    requestedByUserId: ").append(toIndentedString(requestedByUserId)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
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

