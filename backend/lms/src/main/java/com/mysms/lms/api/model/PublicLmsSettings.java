// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.lms.api.model.SettingsSidebarItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * PublicLmsSettings
 */

public class PublicLmsSettings {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean allowGuestAccess;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean disableSignup;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean showCourses;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean showLiveClass;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String textDirection;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String contactUsEmail;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String contactUsUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String metaDescription;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String metaKeywords;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String customSignupContent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid SettingsSidebarItem> sidebarItems = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String defaultCurrencyCode;

  public PublicLmsSettings() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public PublicLmsSettings(Boolean allowGuestAccess, Boolean disableSignup, Boolean showCourses, Boolean showLiveClass, String textDirection) {
    this.allowGuestAccess = allowGuestAccess;
    this.disableSignup = disableSignup;
    this.showCourses = showCourses;
    this.showLiveClass = showLiveClass;
    this.textDirection = textDirection;
  }

  public PublicLmsSettings allowGuestAccess(Boolean allowGuestAccess) {
    this.allowGuestAccess = allowGuestAccess;
    return this;
  }

  /**
   * Guests may browse.
   * @return allowGuestAccess
   */
  @NotNull 
  @JsonProperty("allowGuestAccess")
  public Boolean getAllowGuestAccess() {
    return allowGuestAccess;
  }

  @JsonProperty("allowGuestAccess")
  public void setAllowGuestAccess(Boolean allowGuestAccess) {
    this.allowGuestAccess = allowGuestAccess;
  }

  public PublicLmsSettings disableSignup(Boolean disableSignup) {
    this.disableSignup = disableSignup;
    return this;
  }

  /**
   * Self-signup disabled.
   * @return disableSignup
   */
  @NotNull 
  @JsonProperty("disableSignup")
  public Boolean getDisableSignup() {
    return disableSignup;
  }

  @JsonProperty("disableSignup")
  public void setDisableSignup(Boolean disableSignup) {
    this.disableSignup = disableSignup;
  }

  public PublicLmsSettings showCourses(Boolean showCourses) {
    this.showCourses = showCourses;
    return this;
  }

  /**
   * Courses section.
   * @return showCourses
   */
  @NotNull 
  @JsonProperty("showCourses")
  public Boolean getShowCourses() {
    return showCourses;
  }

  @JsonProperty("showCourses")
  public void setShowCourses(Boolean showCourses) {
    this.showCourses = showCourses;
  }

  public PublicLmsSettings showLiveClass(Boolean showLiveClass) {
    this.showLiveClass = showLiveClass;
    return this;
  }

  /**
   * Live-class section.
   * @return showLiveClass
   */
  @NotNull 
  @JsonProperty("showLiveClass")
  public Boolean getShowLiveClass() {
    return showLiveClass;
  }

  @JsonProperty("showLiveClass")
  public void setShowLiveClass(Boolean showLiveClass) {
    this.showLiveClass = showLiveClass;
  }

  public PublicLmsSettings textDirection(String textDirection) {
    this.textDirection = textDirection;
    return this;
  }

  /**
   * auto / ltr / rtl.
   * @return textDirection
   */
  @NotNull 
  @JsonProperty("textDirection")
  public String getTextDirection() {
    return textDirection;
  }

  @JsonProperty("textDirection")
  public void setTextDirection(String textDirection) {
    this.textDirection = textDirection;
  }

  public PublicLmsSettings contactUsEmail(@Nullable String contactUsEmail) {
    this.contactUsEmail = contactUsEmail;
    return this;
  }

  /**
   * Contact e-mail.
   * @return contactUsEmail
   */
  
  @JsonProperty("contactUsEmail")
  public @Nullable String getContactUsEmail() {
    return contactUsEmail;
  }

  @JsonProperty("contactUsEmail")
  public void setContactUsEmail(@Nullable String contactUsEmail) {
    this.contactUsEmail = contactUsEmail;
  }

  public PublicLmsSettings contactUsUrl(@Nullable String contactUsUrl) {
    this.contactUsUrl = contactUsUrl;
    return this;
  }

  /**
   * Contact page.
   * @return contactUsUrl
   */
  
  @JsonProperty("contactUsUrl")
  public @Nullable String getContactUsUrl() {
    return contactUsUrl;
  }

  @JsonProperty("contactUsUrl")
  public void setContactUsUrl(@Nullable String contactUsUrl) {
    this.contactUsUrl = contactUsUrl;
  }

  public PublicLmsSettings metaDescription(@Nullable String metaDescription) {
    this.metaDescription = metaDescription;
    return this;
  }

  /**
   * SEO description.
   * @return metaDescription
   */
  
  @JsonProperty("metaDescription")
  public @Nullable String getMetaDescription() {
    return metaDescription;
  }

  @JsonProperty("metaDescription")
  public void setMetaDescription(@Nullable String metaDescription) {
    this.metaDescription = metaDescription;
  }

  public PublicLmsSettings metaKeywords(@Nullable String metaKeywords) {
    this.metaKeywords = metaKeywords;
    return this;
  }

  /**
   * SEO keywords.
   * @return metaKeywords
   */
  
  @JsonProperty("metaKeywords")
  public @Nullable String getMetaKeywords() {
    return metaKeywords;
  }

  @JsonProperty("metaKeywords")
  public void setMetaKeywords(@Nullable String metaKeywords) {
    this.metaKeywords = metaKeywords;
  }

  public PublicLmsSettings customSignupContent(@Nullable String customSignupContent) {
    this.customSignupContent = customSignupContent;
    return this;
  }

  /**
   * Signup page HTML.
   * @return customSignupContent
   */
  
  @JsonProperty("customSignupContent")
  public @Nullable String getCustomSignupContent() {
    return customSignupContent;
  }

  @JsonProperty("customSignupContent")
  public void setCustomSignupContent(@Nullable String customSignupContent) {
    this.customSignupContent = customSignupContent;
  }

  public PublicLmsSettings sidebarItems(List<@Valid SettingsSidebarItem> sidebarItems) {
    this.sidebarItems = sidebarItems;
    return this;
  }

  public PublicLmsSettings addSidebarItemsItem(SettingsSidebarItem sidebarItemsItem) {
    if (this.sidebarItems == null) {
      this.sidebarItems = new ArrayList<>();
    }
    this.sidebarItems.add(sidebarItemsItem);
    return this;
  }

  /**
   * Navigation items.
   * @return sidebarItems
   */
  
  @JsonProperty("sidebarItems")
  public List<@Valid SettingsSidebarItem> getSidebarItems() {
    return sidebarItems;
  }

  @JsonProperty("sidebarItems")
  public void setSidebarItems(List<@Valid SettingsSidebarItem> sidebarItems) {
    this.sidebarItems = sidebarItems;
  }

  public PublicLmsSettings defaultCurrencyCode(@Nullable String defaultCurrencyCode) {
    this.defaultCurrencyCode = defaultCurrencyCode;
    return this;
  }

  /**
   * Currency.
   * @return defaultCurrencyCode
   */
  
  @JsonProperty("defaultCurrencyCode")
  public @Nullable String getDefaultCurrencyCode() {
    return defaultCurrencyCode;
  }

  @JsonProperty("defaultCurrencyCode")
  public void setDefaultCurrencyCode(@Nullable String defaultCurrencyCode) {
    this.defaultCurrencyCode = defaultCurrencyCode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PublicLmsSettings publicLmsSettings = (PublicLmsSettings) o;
    return Objects.equals(this.allowGuestAccess, publicLmsSettings.allowGuestAccess) &&
        Objects.equals(this.disableSignup, publicLmsSettings.disableSignup) &&
        Objects.equals(this.showCourses, publicLmsSettings.showCourses) &&
        Objects.equals(this.showLiveClass, publicLmsSettings.showLiveClass) &&
        Objects.equals(this.textDirection, publicLmsSettings.textDirection) &&
        Objects.equals(this.contactUsEmail, publicLmsSettings.contactUsEmail) &&
        Objects.equals(this.contactUsUrl, publicLmsSettings.contactUsUrl) &&
        Objects.equals(this.metaDescription, publicLmsSettings.metaDescription) &&
        Objects.equals(this.metaKeywords, publicLmsSettings.metaKeywords) &&
        Objects.equals(this.customSignupContent, publicLmsSettings.customSignupContent) &&
        Objects.equals(this.sidebarItems, publicLmsSettings.sidebarItems) &&
        Objects.equals(this.defaultCurrencyCode, publicLmsSettings.defaultCurrencyCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(allowGuestAccess, disableSignup, showCourses, showLiveClass, textDirection, contactUsEmail, contactUsUrl, metaDescription, metaKeywords, customSignupContent, sidebarItems, defaultCurrencyCode);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PublicLmsSettings {\n");
    sb.append("    allowGuestAccess: ").append(toIndentedString(allowGuestAccess)).append("\n");
    sb.append("    disableSignup: ").append(toIndentedString(disableSignup)).append("\n");
    sb.append("    showCourses: ").append(toIndentedString(showCourses)).append("\n");
    sb.append("    showLiveClass: ").append(toIndentedString(showLiveClass)).append("\n");
    sb.append("    textDirection: ").append(toIndentedString(textDirection)).append("\n");
    sb.append("    contactUsEmail: ").append(toIndentedString(contactUsEmail)).append("\n");
    sb.append("    contactUsUrl: ").append(toIndentedString(contactUsUrl)).append("\n");
    sb.append("    metaDescription: ").append(toIndentedString(metaDescription)).append("\n");
    sb.append("    metaKeywords: ").append(toIndentedString(metaKeywords)).append("\n");
    sb.append("    customSignupContent: ").append(toIndentedString(customSignupContent)).append("\n");
    sb.append("    sidebarItems: ").append(toIndentedString(sidebarItems)).append("\n");
    sb.append("    defaultCurrencyCode: ").append(toIndentedString(defaultCurrencyCode)).append("\n");
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

