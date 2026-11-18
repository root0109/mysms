// Owned API contract of lms.
package com.mysms.lms.api.model;

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
 * Fields of SettingsSidebarItem that can be updated (partial update; see updateMask).
 */

public class SettingsSidebarItemUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  /**
   * Item kind.
   */
  public enum ItemTypeEnum {
    BUILT_IN("built_in"),
    
    WEB_PAGE("web_page"),
    
    ROUTE("route"),
    
    EXTERNAL("external");

    private final String value;

    ItemTypeEnum(String value) {
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
    public static ItemTypeEnum fromValue(String value) {
      for (ItemTypeEnum b : ItemTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable ItemTypeEnum itemType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String itemKey;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String icon;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String webPageRoute;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String route;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String url;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isStandard;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isHidden;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean openInNewWindow;

  public SettingsSidebarItemUpdate sortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Position.
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

  public SettingsSidebarItemUpdate itemType(@Nullable ItemTypeEnum itemType) {
    this.itemType = itemType;
    return this;
  }

  /**
   * Item kind.
   * @return itemType
   */
  
  @JsonProperty("itemType")
  public @Nullable ItemTypeEnum getItemType() {
    return itemType;
  }

  @JsonProperty("itemType")
  public void setItemType(@Nullable ItemTypeEnum itemType) {
    this.itemType = itemType;
  }

  public SettingsSidebarItemUpdate itemKey(@Nullable String itemKey) {
    this.itemKey = itemKey;
    return this;
  }

  /**
   * Built-in item key.
   * @return itemKey
   */
  @Size(max = 60) 
  @JsonProperty("itemKey")
  public @Nullable String getItemKey() {
    return itemKey;
  }

  @JsonProperty("itemKey")
  public void setItemKey(@Nullable String itemKey) {
    this.itemKey = itemKey;
  }

  public SettingsSidebarItemUpdate title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * Label.
   * @return title
   */
  @Size(max = 120) 
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public SettingsSidebarItemUpdate icon(@Nullable String icon) {
    this.icon = icon;
    return this;
  }

  /**
   * Icon name.
   * @return icon
   */
  @Size(max = 60) 
  @JsonProperty("icon")
  public @Nullable String getIcon() {
    return icon;
  }

  @JsonProperty("icon")
  public void setIcon(@Nullable String icon) {
    this.icon = icon;
  }

  public SettingsSidebarItemUpdate webPageRoute(@Nullable String webPageRoute) {
    this.webPageRoute = webPageRoute;
    return this;
  }

  /**
   * Website page route (item_type web_page).
   * @return webPageRoute
   */
  @Size(max = 255) 
  @JsonProperty("webPageRoute")
  public @Nullable String getWebPageRoute() {
    return webPageRoute;
  }

  @JsonProperty("webPageRoute")
  public void setWebPageRoute(@Nullable String webPageRoute) {
    this.webPageRoute = webPageRoute;
  }

  public SettingsSidebarItemUpdate route(@Nullable String route) {
    this.route = route;
    return this;
  }

  /**
   * Internal route.
   * @return route
   */
  @Size(max = 255) 
  @JsonProperty("route")
  public @Nullable String getRoute() {
    return route;
  }

  @JsonProperty("route")
  public void setRoute(@Nullable String route) {
    this.route = route;
  }

  public SettingsSidebarItemUpdate url(@Nullable String url) {
    this.url = url;
    return this;
  }

  /**
   * External URL.
   * @return url
   */
  @Size(max = 500) 
  @JsonProperty("url")
  public @Nullable String getUrl() {
    return url;
  }

  @JsonProperty("url")
  public void setUrl(@Nullable String url) {
    this.url = url;
  }

  public SettingsSidebarItemUpdate isStandard(@Nullable Boolean isStandard) {
    this.isStandard = isStandard;
    return this;
  }

  /**
   * Shipped item.
   * @return isStandard
   */
  
  @JsonProperty("isStandard")
  public @Nullable Boolean getIsStandard() {
    return isStandard;
  }

  @JsonProperty("isStandard")
  public void setIsStandard(@Nullable Boolean isStandard) {
    this.isStandard = isStandard;
  }

  public SettingsSidebarItemUpdate isHidden(@Nullable Boolean isHidden) {
    this.isHidden = isHidden;
    return this;
  }

  /**
   * Hidden.
   * @return isHidden
   */
  
  @JsonProperty("isHidden")
  public @Nullable Boolean getIsHidden() {
    return isHidden;
  }

  @JsonProperty("isHidden")
  public void setIsHidden(@Nullable Boolean isHidden) {
    this.isHidden = isHidden;
  }

  public SettingsSidebarItemUpdate openInNewWindow(@Nullable Boolean openInNewWindow) {
    this.openInNewWindow = openInNewWindow;
    return this;
  }

  /**
   * Open in a new tab.
   * @return openInNewWindow
   */
  
  @JsonProperty("openInNewWindow")
  public @Nullable Boolean getOpenInNewWindow() {
    return openInNewWindow;
  }

  @JsonProperty("openInNewWindow")
  public void setOpenInNewWindow(@Nullable Boolean openInNewWindow) {
    this.openInNewWindow = openInNewWindow;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SettingsSidebarItemUpdate settingsSidebarItemUpdate = (SettingsSidebarItemUpdate) o;
    return Objects.equals(this.sortOrder, settingsSidebarItemUpdate.sortOrder) &&
        Objects.equals(this.itemType, settingsSidebarItemUpdate.itemType) &&
        Objects.equals(this.itemKey, settingsSidebarItemUpdate.itemKey) &&
        Objects.equals(this.title, settingsSidebarItemUpdate.title) &&
        Objects.equals(this.icon, settingsSidebarItemUpdate.icon) &&
        Objects.equals(this.webPageRoute, settingsSidebarItemUpdate.webPageRoute) &&
        Objects.equals(this.route, settingsSidebarItemUpdate.route) &&
        Objects.equals(this.url, settingsSidebarItemUpdate.url) &&
        Objects.equals(this.isStandard, settingsSidebarItemUpdate.isStandard) &&
        Objects.equals(this.isHidden, settingsSidebarItemUpdate.isHidden) &&
        Objects.equals(this.openInNewWindow, settingsSidebarItemUpdate.openInNewWindow);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sortOrder, itemType, itemKey, title, icon, webPageRoute, route, url, isStandard, isHidden, openInNewWindow);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SettingsSidebarItemUpdate {\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
    sb.append("    itemType: ").append(toIndentedString(itemType)).append("\n");
    sb.append("    itemKey: ").append(toIndentedString(itemKey)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    icon: ").append(toIndentedString(icon)).append("\n");
    sb.append("    webPageRoute: ").append(toIndentedString(webPageRoute)).append("\n");
    sb.append("    route: ").append(toIndentedString(route)).append("\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    isStandard: ").append(toIndentedString(isStandard)).append("\n");
    sb.append("    isHidden: ").append(toIndentedString(isHidden)).append("\n");
    sb.append("    openInNewWindow: ").append(toIndentedString(openInNewWindow)).append("\n");
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

