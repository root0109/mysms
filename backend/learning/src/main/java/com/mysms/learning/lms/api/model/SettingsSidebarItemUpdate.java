// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of SettingsSidebarItem that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class SettingsSidebarItemUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> sortOrder;

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
  private @Nullable Optional<@NotNull ItemTypeEnum> itemType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 60) String> itemKey;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 120) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 60) String> icon;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 255) String> webPageRoute;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 255) String> route;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 500) String> url;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isStandard;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isHidden;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> openInNewWindow;

  public SettingsSidebarItemUpdate sortOrder(@Nullable Optional<Integer> sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Position.
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

  public SettingsSidebarItemUpdate itemType(@Nullable Optional<ItemTypeEnum> itemType) {
    this.itemType = itemType;
    return this;
  }

  /**
   * Item kind.
   * @return itemType
   */
  
  @JsonProperty("itemType")
  public @Nullable Optional<ItemTypeEnum> getItemType() {
    return itemType;
  }

  @JsonProperty("itemType")
  public void setItemType(@Nullable Optional<ItemTypeEnum> itemType) {
    this.itemType = itemType;
  }

  public SettingsSidebarItemUpdate itemKey(@Nullable Optional<String> itemKey) {
    this.itemKey = itemKey;
    return this;
  }

  /**
   * Built-in item key.
   * @return itemKey
   */
  
  @JsonProperty("itemKey")
  public @Nullable Optional<String> getItemKey() {
    return itemKey;
  }

  @JsonProperty("itemKey")
  public void setItemKey(@Nullable Optional<String> itemKey) {
    this.itemKey = itemKey;
  }

  public SettingsSidebarItemUpdate title(@Nullable Optional<String> title) {
    this.title = title;
    return this;
  }

  /**
   * Label.
   * @return title
   */
  
  @JsonProperty("title")
  public @Nullable Optional<String> getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable Optional<String> title) {
    this.title = title;
  }

  public SettingsSidebarItemUpdate icon(@Nullable Optional<String> icon) {
    this.icon = icon;
    return this;
  }

  /**
   * Icon name.
   * @return icon
   */
  
  @JsonProperty("icon")
  public @Nullable Optional<String> getIcon() {
    return icon;
  }

  @JsonProperty("icon")
  public void setIcon(@Nullable Optional<String> icon) {
    this.icon = icon;
  }

  public SettingsSidebarItemUpdate webPageRoute(@Nullable Optional<String> webPageRoute) {
    this.webPageRoute = webPageRoute;
    return this;
  }

  /**
   * Website page route (item_type web_page).
   * @return webPageRoute
   */
  
  @JsonProperty("webPageRoute")
  public @Nullable Optional<String> getWebPageRoute() {
    return webPageRoute;
  }

  @JsonProperty("webPageRoute")
  public void setWebPageRoute(@Nullable Optional<String> webPageRoute) {
    this.webPageRoute = webPageRoute;
  }

  public SettingsSidebarItemUpdate route(@Nullable Optional<String> route) {
    this.route = route;
    return this;
  }

  /**
   * Internal route.
   * @return route
   */
  
  @JsonProperty("route")
  public @Nullable Optional<String> getRoute() {
    return route;
  }

  @JsonProperty("route")
  public void setRoute(@Nullable Optional<String> route) {
    this.route = route;
  }

  public SettingsSidebarItemUpdate url(@Nullable Optional<String> url) {
    this.url = url;
    return this;
  }

  /**
   * External URL.
   * @return url
   */
  
  @JsonProperty("url")
  public @Nullable Optional<String> getUrl() {
    return url;
  }

  @JsonProperty("url")
  public void setUrl(@Nullable Optional<String> url) {
    this.url = url;
  }

  public SettingsSidebarItemUpdate isStandard(@Nullable Optional<Boolean> isStandard) {
    this.isStandard = isStandard;
    return this;
  }

  /**
   * Shipped item.
   * @return isStandard
   */
  
  @JsonProperty("isStandard")
  public @Nullable Optional<Boolean> getIsStandard() {
    return isStandard;
  }

  @JsonProperty("isStandard")
  public void setIsStandard(@Nullable Optional<Boolean> isStandard) {
    this.isStandard = isStandard;
  }

  public SettingsSidebarItemUpdate isHidden(@Nullable Optional<Boolean> isHidden) {
    this.isHidden = isHidden;
    return this;
  }

  /**
   * Hidden.
   * @return isHidden
   */
  
  @JsonProperty("isHidden")
  public @Nullable Optional<Boolean> getIsHidden() {
    return isHidden;
  }

  @JsonProperty("isHidden")
  public void setIsHidden(@Nullable Optional<Boolean> isHidden) {
    this.isHidden = isHidden;
  }

  public SettingsSidebarItemUpdate openInNewWindow(@Nullable Optional<Boolean> openInNewWindow) {
    this.openInNewWindow = openInNewWindow;
    return this;
  }

  /**
   * Open in a new tab.
   * @return openInNewWindow
   */
  
  @JsonProperty("openInNewWindow")
  public @Nullable Optional<Boolean> getOpenInNewWindow() {
    return openInNewWindow;
  }

  @JsonProperty("openInNewWindow")
  public void setOpenInNewWindow(@Nullable Optional<Boolean> openInNewWindow) {
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

