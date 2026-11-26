// Owned API contract of platform.
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
 * Fields of SaasPlan that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class SaasPlanUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 50) String> code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 120) String> displayName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> billingCycleMonths;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> price;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> currencyId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> maxMembers;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> maxEvents;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> maxFolders;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> maxFiles;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> maxVideos;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> maxAudios;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> maxBulletins;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> maxGroups;

  public SaasPlanUpdate code(@Nullable Optional<String> code) {
    this.code = code;
    return this;
  }

  /**
   * Unique plan code.
   * @return code
   */
  
  @JsonProperty("code")
  public @Nullable Optional<String> getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable Optional<String> code) {
    this.code = code;
  }

  public SaasPlanUpdate displayName(@Nullable Optional<String> displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * Plan name shown to customers.
   * @return displayName
   */
  
  @JsonProperty("displayName")
  public @Nullable Optional<String> getDisplayName() {
    return displayName;
  }

  @JsonProperty("displayName")
  public void setDisplayName(@Nullable Optional<String> displayName) {
    this.displayName = displayName;
  }

  public SaasPlanUpdate billingCycleMonths(@Nullable Optional<Integer> billingCycleMonths) {
    this.billingCycleMonths = billingCycleMonths;
    return this;
  }

  /**
   * Billing cycle length in months.
   * @return billingCycleMonths
   */
  
  @JsonProperty("billingCycleMonths")
  public @Nullable Optional<Integer> getBillingCycleMonths() {
    return billingCycleMonths;
  }

  @JsonProperty("billingCycleMonths")
  public void setBillingCycleMonths(@Nullable Optional<Integer> billingCycleMonths) {
    this.billingCycleMonths = billingCycleMonths;
  }

  public SaasPlanUpdate price(@Nullable Optional<String> price) {
    this.price = price;
    return this;
  }

  /**
   * Price per billing cycle.
   * @return price
   */
  
  @JsonProperty("price")
  public @Nullable Optional<String> getPrice() {
    return price;
  }

  @JsonProperty("price")
  public void setPrice(@Nullable Optional<String> price) {
    this.price = price;
  }

  public SaasPlanUpdate currencyId(@Nullable Optional<UUID> currencyId) {
    this.currencyId = currencyId;
    return this;
  }

  /**
   * Currency of the price.
   * @return currencyId
   */
  
  @JsonProperty("currencyId")
  public @Nullable Optional<UUID> getCurrencyId() {
    return currencyId;
  }

  @JsonProperty("currencyId")
  public void setCurrencyId(@Nullable Optional<UUID> currencyId) {
    this.currencyId = currencyId;
  }

  public SaasPlanUpdate sortOrder(@Nullable Optional<Integer> sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Display order.
   * @return sortOrder
   */
  
  @JsonProperty("sortOrder")
  public @Nullable Optional<Integer> getSortOrder() {
    return sortOrder;
  }

  @JsonProperty("sortOrder")
  public void setSortOrder(@Nullable Optional<Integer> sortOrder) {
    this.sortOrder = sortOrder;
  }

  public SaasPlanUpdate isActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether the plan can be purchased.
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Optional<Boolean> getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
  }

  public SaasPlanUpdate maxMembers(@Nullable Optional<Integer> maxMembers) {
    this.maxMembers = maxMembers;
    return this;
  }

  /**
   * Member (user) limit; NULL = unlimited.
   * @return maxMembers
   */
  
  @JsonProperty("maxMembers")
  public @Nullable Optional<Integer> getMaxMembers() {
    return maxMembers;
  }

  @JsonProperty("maxMembers")
  public void setMaxMembers(@Nullable Optional<Integer> maxMembers) {
    this.maxMembers = maxMembers;
  }

  public SaasPlanUpdate maxEvents(@Nullable Optional<Integer> maxEvents) {
    this.maxEvents = maxEvents;
    return this;
  }

  /**
   * Event limit.
   * @return maxEvents
   */
  
  @JsonProperty("maxEvents")
  public @Nullable Optional<Integer> getMaxEvents() {
    return maxEvents;
  }

  @JsonProperty("maxEvents")
  public void setMaxEvents(@Nullable Optional<Integer> maxEvents) {
    this.maxEvents = maxEvents;
  }

  public SaasPlanUpdate maxFolders(@Nullable Optional<Integer> maxFolders) {
    this.maxFolders = maxFolders;
    return this;
  }

  /**
   * Folder limit.
   * @return maxFolders
   */
  
  @JsonProperty("maxFolders")
  public @Nullable Optional<Integer> getMaxFolders() {
    return maxFolders;
  }

  @JsonProperty("maxFolders")
  public void setMaxFolders(@Nullable Optional<Integer> maxFolders) {
    this.maxFolders = maxFolders;
  }

  public SaasPlanUpdate maxFiles(@Nullable Optional<Integer> maxFiles) {
    this.maxFiles = maxFiles;
    return this;
  }

  /**
   * File limit.
   * @return maxFiles
   */
  
  @JsonProperty("maxFiles")
  public @Nullable Optional<Integer> getMaxFiles() {
    return maxFiles;
  }

  @JsonProperty("maxFiles")
  public void setMaxFiles(@Nullable Optional<Integer> maxFiles) {
    this.maxFiles = maxFiles;
  }

  public SaasPlanUpdate maxVideos(@Nullable Optional<Integer> maxVideos) {
    this.maxVideos = maxVideos;
    return this;
  }

  /**
   * Video limit.
   * @return maxVideos
   */
  
  @JsonProperty("maxVideos")
  public @Nullable Optional<Integer> getMaxVideos() {
    return maxVideos;
  }

  @JsonProperty("maxVideos")
  public void setMaxVideos(@Nullable Optional<Integer> maxVideos) {
    this.maxVideos = maxVideos;
  }

  public SaasPlanUpdate maxAudios(@Nullable Optional<Integer> maxAudios) {
    this.maxAudios = maxAudios;
    return this;
  }

  /**
   * Audio limit.
   * @return maxAudios
   */
  
  @JsonProperty("maxAudios")
  public @Nullable Optional<Integer> getMaxAudios() {
    return maxAudios;
  }

  @JsonProperty("maxAudios")
  public void setMaxAudios(@Nullable Optional<Integer> maxAudios) {
    this.maxAudios = maxAudios;
  }

  public SaasPlanUpdate maxBulletins(@Nullable Optional<Integer> maxBulletins) {
    this.maxBulletins = maxBulletins;
    return this;
  }

  /**
   * Bulletin/magazine limit.
   * @return maxBulletins
   */
  
  @JsonProperty("maxBulletins")
  public @Nullable Optional<Integer> getMaxBulletins() {
    return maxBulletins;
  }

  @JsonProperty("maxBulletins")
  public void setMaxBulletins(@Nullable Optional<Integer> maxBulletins) {
    this.maxBulletins = maxBulletins;
  }

  public SaasPlanUpdate maxGroups(@Nullable Optional<Integer> maxGroups) {
    this.maxGroups = maxGroups;
    return this;
  }

  /**
   * Group limit.
   * @return maxGroups
   */
  
  @JsonProperty("maxGroups")
  public @Nullable Optional<Integer> getMaxGroups() {
    return maxGroups;
  }

  @JsonProperty("maxGroups")
  public void setMaxGroups(@Nullable Optional<Integer> maxGroups) {
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

