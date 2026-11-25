// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.lang.Nullable;

/**
 * The fields of a breach that may change while it is open; the times and the clock are fixed once reported.
 */

public class BreachUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<String> categories;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<String> modules;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer estimatedPrincipals;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String likelyConsequences;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String mitigation;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID ownerUserId;

  public BreachUpdate() {
    super();
  }

  public BreachUpdate description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * What happened, in ids and counts only, never a name or contact.
   * @return description
   */
  @Size(min = 1, max = 4000) 
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public BreachUpdate categories(@Nullable List<String> categories) {
    this.categories = categories;
    return this;
  }

  public BreachUpdate addCategoriesItem(String categoriesItem) {
    if (this.categories == null) {
      this.categories = new ArrayList<>();
    }
    this.categories.add(categoriesItem);
    return this;
  }

  /**
   * Codes of the data categories affected.
   * @return categories
   */
  @Size(max = 20) 
  @JsonProperty("categories")
  public @Nullable List<String> getCategories() {
    return categories;
  }

  @JsonProperty("categories")
  public void setCategories(@Nullable List<String> categories) {
    this.categories = categories;
  }

  public BreachUpdate modules(@Nullable List<String> modules) {
    this.modules = modules;
    return this;
  }

  public BreachUpdate addModulesItem(String modulesItem) {
    if (this.modules == null) {
      this.modules = new ArrayList<>();
    }
    this.modules.add(modulesItem);
    return this;
  }

  /**
   * The MySMS modules whose data is affected.
   * @return modules
   */
  @Size(max = 30) 
  @JsonProperty("modules")
  public @Nullable List<String> getModules() {
    return modules;
  }

  @JsonProperty("modules")
  public void setModules(@Nullable List<String> modules) {
    this.modules = modules;
  }

  public BreachUpdate estimatedPrincipals(@Nullable Integer estimatedPrincipals) {
    this.estimatedPrincipals = estimatedPrincipals;
    return this;
  }

  /**
   * How many data principals are estimated to be affected.
   * @return estimatedPrincipals
   */
  @Min(value = 0) 
  @JsonProperty("estimatedPrincipals")
  public @Nullable Integer getEstimatedPrincipals() {
    return estimatedPrincipals;
  }

  @JsonProperty("estimatedPrincipals")
  public void setEstimatedPrincipals(@Nullable Integer estimatedPrincipals) {
    this.estimatedPrincipals = estimatedPrincipals;
  }

  public BreachUpdate likelyConsequences(@Nullable String likelyConsequences) {
    this.likelyConsequences = likelyConsequences;
    return this;
  }

  /**
   * The likely consequences for the principals, in ids and counts only.
   * @return likelyConsequences
   */
  @Size(min = 1, max = 4000) 
  @JsonProperty("likelyConsequences")
  public @Nullable String getLikelyConsequences() {
    return likelyConsequences;
  }

  @JsonProperty("likelyConsequences")
  public void setLikelyConsequences(@Nullable String likelyConsequences) {
    this.likelyConsequences = likelyConsequences;
  }

  public BreachUpdate mitigation(@Nullable String mitigation) {
    this.mitigation = mitigation;
    return this;
  }

  /**
   * What was done or is planned to contain the breach, in ids and counts only.
   * @return mitigation
   */
  @Size(min = 1, max = 4000) 
  @JsonProperty("mitigation")
  public @Nullable String getMitigation() {
    return mitigation;
  }

  @JsonProperty("mitigation")
  public void setMitigation(@Nullable String mitigation) {
    this.mitigation = mitigation;
  }

  public BreachUpdate ownerUserId(@Nullable UUID ownerUserId) {
    this.ownerUserId = ownerUserId;
    return this;
  }

  /**
   * The staff member handling the breach.
   * @return ownerUserId
   */
  @Valid 
  @JsonProperty("ownerUserId")
  public @Nullable UUID getOwnerUserId() {
    return ownerUserId;
  }

  @JsonProperty("ownerUserId")
  public void setOwnerUserId(@Nullable UUID ownerUserId) {
    this.ownerUserId = ownerUserId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BreachUpdate other = (BreachUpdate) o;
    return Objects.equals(this.description, other.description) &&
        Objects.equals(this.categories, other.categories) &&
        Objects.equals(this.modules, other.modules) &&
        Objects.equals(this.estimatedPrincipals, other.estimatedPrincipals) &&
        Objects.equals(this.likelyConsequences, other.likelyConsequences) &&
        Objects.equals(this.mitigation, other.mitigation) &&
        Objects.equals(this.ownerUserId, other.ownerUserId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(description, categories, modules, estimatedPrincipals, likelyConsequences, mitigation, ownerUserId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BreachUpdate {\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    categories: ").append(toIndentedString(categories)).append("\n");
    sb.append("    modules: ").append(toIndentedString(modules)).append("\n");
    sb.append("    estimatedPrincipals: ").append(toIndentedString(estimatedPrincipals)).append("\n");
    sb.append("    likelyConsequences: ").append(toIndentedString(likelyConsequences)).append("\n");
    sb.append("    mitigation: ").append(toIndentedString(mitigation)).append("\n");
    sb.append("    ownerUserId: ").append(toIndentedString(ownerUserId)).append("\n");
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
