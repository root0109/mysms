// Owned API contract of platform (JAVA-CODING-RULES §7); first materialized from api/openapi/platform.yaml on 2026-11-10.
package com.mysms.platform.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of SaasPlan that can be updated (partial update; see updateMask).
 */

public class SaasPlanUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String displayName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer billingCycleMonths;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String price;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID currencyId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer maxMembers;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer maxEvents;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer maxFolders;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer maxFiles;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer maxVideos;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer maxAudios;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer maxBulletins;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer maxGroups;

  public SaasPlanUpdate code(@Nullable String code) {
    this.code = code;
    return this;
  }

  /**
   * Unique plan code.
   * @return code
   */
  @Size(max = 50) 
  @JsonProperty("code")
  public @Nullable String getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable String code) {
    this.code = code;
  }

  public SaasPlanUpdate displayName(@Nullable String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * Plan name shown to customers.
   * @return displayName
   */
  @Size(max = 120) 
  @JsonProperty("displayName")
  public @Nullable String getDisplayName() {
    return displayName;
  }

  @JsonProperty("displayName")
  public void setDisplayName(@Nullable String displayName) {
    this.displayName = displayName;
  }

  public SaasPlanUpdate billingCycleMonths(@Nullable Integer billingCycleMonths) {
    this.billingCycleMonths = billingCycleMonths;
    return this;
  }

  /**
   * Billing cycle length in months.
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

  public SaasPlanUpdate price(@Nullable String price) {
    this.price = price;
    return this;
  }

  /**
   * Price per billing cycle.
   * @return price
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("price")
  public @Nullable String getPrice() {
    return price;
  }

  @JsonProperty("price")
  public void setPrice(@Nullable String price) {
    this.price = price;
  }

  public SaasPlanUpdate currencyId(@Nullable UUID currencyId) {
    this.currencyId = currencyId;
    return this;
  }

  /**
   * Currency of the price.
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

  public SaasPlanUpdate sortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Display order.
   * @return sortOrder
   */
  
  @JsonProperty("sortOrder")
  public @Nullable Integer getSortOrder() {
    return sortOrder;
  }

  @JsonProperty("sortOrder")
  public void setSortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  public SaasPlanUpdate isActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether the plan can be purchased.
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Boolean getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
  }

  public SaasPlanUpdate maxMembers(@Nullable Integer maxMembers) {
    this.maxMembers = maxMembers;
    return this;
  }

  /**
   * Member (user) limit; NULL = unlimited.
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

  public SaasPlanUpdate maxEvents(@Nullable Integer maxEvents) {
    this.maxEvents = maxEvents;
    return this;
  }

  /**
   * Event limit.
   * @return maxEvents
   */
  
  @JsonProperty("maxEvents")
  public @Nullable Integer getMaxEvents() {
    return maxEvents;
  }

  @JsonProperty("maxEvents")
  public void setMaxEvents(@Nullable Integer maxEvents) {
    this.maxEvents = maxEvents;
  }

  public SaasPlanUpdate maxFolders(@Nullable Integer maxFolders) {
    this.maxFolders = maxFolders;
    return this;
  }

  /**
   * Folder limit.
   * @return maxFolders
   */
  
  @JsonProperty("maxFolders")
  public @Nullable Integer getMaxFolders() {
    return maxFolders;
  }

  @JsonProperty("maxFolders")
  public void setMaxFolders(@Nullable Integer maxFolders) {
    this.maxFolders = maxFolders;
  }

  public SaasPlanUpdate maxFiles(@Nullable Integer maxFiles) {
    this.maxFiles = maxFiles;
    return this;
  }

  /**
   * File limit.
   * @return maxFiles
   */
  
  @JsonProperty("maxFiles")
  public @Nullable Integer getMaxFiles() {
    return maxFiles;
  }

  @JsonProperty("maxFiles")
  public void setMaxFiles(@Nullable Integer maxFiles) {
    this.maxFiles = maxFiles;
  }

  public SaasPlanUpdate maxVideos(@Nullable Integer maxVideos) {
    this.maxVideos = maxVideos;
    return this;
  }

  /**
   * Video limit.
   * @return maxVideos
   */
  
  @JsonProperty("maxVideos")
  public @Nullable Integer getMaxVideos() {
    return maxVideos;
  }

  @JsonProperty("maxVideos")
  public void setMaxVideos(@Nullable Integer maxVideos) {
    this.maxVideos = maxVideos;
  }

  public SaasPlanUpdate maxAudios(@Nullable Integer maxAudios) {
    this.maxAudios = maxAudios;
    return this;
  }

  /**
   * Audio limit.
   * @return maxAudios
   */
  
  @JsonProperty("maxAudios")
  public @Nullable Integer getMaxAudios() {
    return maxAudios;
  }

  @JsonProperty("maxAudios")
  public void setMaxAudios(@Nullable Integer maxAudios) {
    this.maxAudios = maxAudios;
  }

  public SaasPlanUpdate maxBulletins(@Nullable Integer maxBulletins) {
    this.maxBulletins = maxBulletins;
    return this;
  }

  /**
   * Bulletin/magazine limit.
   * @return maxBulletins
   */
  
  @JsonProperty("maxBulletins")
  public @Nullable Integer getMaxBulletins() {
    return maxBulletins;
  }

  @JsonProperty("maxBulletins")
  public void setMaxBulletins(@Nullable Integer maxBulletins) {
    this.maxBulletins = maxBulletins;
  }

  public SaasPlanUpdate maxGroups(@Nullable Integer maxGroups) {
    this.maxGroups = maxGroups;
    return this;
  }

  /**
   * Group limit.
   * @return maxGroups
   */
  
  @JsonProperty("maxGroups")
  public @Nullable Integer getMaxGroups() {
    return maxGroups;
  }

  @JsonProperty("maxGroups")
  public void setMaxGroups(@Nullable Integer maxGroups) {
    this.maxGroups = maxGroups;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SaasPlanUpdate saasPlanUpdate = (SaasPlanUpdate) o;
    return Objects.equals(this.code, saasPlanUpdate.code) &&
        Objects.equals(this.displayName, saasPlanUpdate.displayName) &&
        Objects.equals(this.billingCycleMonths, saasPlanUpdate.billingCycleMonths) &&
        Objects.equals(this.price, saasPlanUpdate.price) &&
        Objects.equals(this.currencyId, saasPlanUpdate.currencyId) &&
        Objects.equals(this.sortOrder, saasPlanUpdate.sortOrder) &&
        Objects.equals(this.isActive, saasPlanUpdate.isActive) &&
        Objects.equals(this.maxMembers, saasPlanUpdate.maxMembers) &&
        Objects.equals(this.maxEvents, saasPlanUpdate.maxEvents) &&
        Objects.equals(this.maxFolders, saasPlanUpdate.maxFolders) &&
        Objects.equals(this.maxFiles, saasPlanUpdate.maxFiles) &&
        Objects.equals(this.maxVideos, saasPlanUpdate.maxVideos) &&
        Objects.equals(this.maxAudios, saasPlanUpdate.maxAudios) &&
        Objects.equals(this.maxBulletins, saasPlanUpdate.maxBulletins) &&
        Objects.equals(this.maxGroups, saasPlanUpdate.maxGroups);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, displayName, billingCycleMonths, price, currencyId, sortOrder, isActive, maxMembers, maxEvents, maxFolders, maxFiles, maxVideos, maxAudios, maxBulletins, maxGroups);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SaasPlanUpdate {\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    billingCycleMonths: ").append(toIndentedString(billingCycleMonths)).append("\n");
    sb.append("    price: ").append(toIndentedString(price)).append("\n");
    sb.append("    currencyId: ").append(toIndentedString(currencyId)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
    sb.append("    maxMembers: ").append(toIndentedString(maxMembers)).append("\n");
    sb.append("    maxEvents: ").append(toIndentedString(maxEvents)).append("\n");
    sb.append("    maxFolders: ").append(toIndentedString(maxFolders)).append("\n");
    sb.append("    maxFiles: ").append(toIndentedString(maxFiles)).append("\n");
    sb.append("    maxVideos: ").append(toIndentedString(maxVideos)).append("\n");
    sb.append("    maxAudios: ").append(toIndentedString(maxAudios)).append("\n");
    sb.append("    maxBulletins: ").append(toIndentedString(maxBulletins)).append("\n");
    sb.append("    maxGroups: ").append(toIndentedString(maxGroups)).append("\n");
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

