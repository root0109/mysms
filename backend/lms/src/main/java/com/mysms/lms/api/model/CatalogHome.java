// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

import com.mysms.core.paging.NavigationInfo;
import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.lms.api.model.CatalogBanner;
import com.mysms.lms.api.model.Category;
import com.mysms.lms.api.model.Cohort;
import com.mysms.lms.api.model.LmsCourse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * CatalogHome
 */

public class CatalogHome {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid CatalogBanner> featured = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid CatalogBanner> showcase = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid LmsCourse> courses = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid Category> categories = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid Cohort> openCohorts = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable NavigationInfo paging;

  public CatalogHome featured(List<@Valid CatalogBanner> featured) {
    this.featured = featured;
    return this;
  }

  public CatalogHome addFeaturedItem(CatalogBanner featuredItem) {
    if (this.featured == null) {
      this.featured = new ArrayList<>();
    }
    this.featured.add(featuredItem);
    return this;
  }

  /**
   * Featured banners.
   * @return featured
   */
  
  @JsonProperty("featured")
  public List<@Valid CatalogBanner> getFeatured() {
    return featured;
  }

  @JsonProperty("featured")
  public void setFeatured(List<@Valid CatalogBanner> featured) {
    this.featured = featured;
  }

  public CatalogHome showcase(List<@Valid CatalogBanner> showcase) {
    this.showcase = showcase;
    return this;
  }

  public CatalogHome addShowcaseItem(CatalogBanner showcaseItem) {
    if (this.showcase == null) {
      this.showcase = new ArrayList<>();
    }
    this.showcase.add(showcaseItem);
    return this;
  }

  /**
   * Showcase banners.
   * @return showcase
   */
  
  @JsonProperty("showcase")
  public List<@Valid CatalogBanner> getShowcase() {
    return showcase;
  }

  @JsonProperty("showcase")
  public void setShowcase(List<@Valid CatalogBanner> showcase) {
    this.showcase = showcase;
  }

  public CatalogHome courses(List<@Valid LmsCourse> courses) {
    this.courses = courses;
    return this;
  }

  public CatalogHome addCoursesItem(LmsCourse coursesItem) {
    if (this.courses == null) {
      this.courses = new ArrayList<>();
    }
    this.courses.add(coursesItem);
    return this;
  }

  /**
   * Published catalogue courses (first page).
   * @return courses
   */
  
  @JsonProperty("courses")
  public List<@Valid LmsCourse> getCourses() {
    return courses;
  }

  @JsonProperty("courses")
  public void setCourses(List<@Valid LmsCourse> courses) {
    this.courses = courses;
  }

  public CatalogHome categories(List<@Valid Category> categories) {
    this.categories = categories;
    return this;
  }

  public CatalogHome addCategoriesItem(Category categoriesItem) {
    if (this.categories == null) {
      this.categories = new ArrayList<>();
    }
    this.categories.add(categoriesItem);
    return this;
  }

  /**
   * Categories.
   * @return categories
   */
  
  @JsonProperty("categories")
  public List<@Valid Category> getCategories() {
    return categories;
  }

  @JsonProperty("categories")
  public void setCategories(List<@Valid Category> categories) {
    this.categories = categories;
  }

  public CatalogHome openCohorts(List<@Valid Cohort> openCohorts) {
    this.openCohorts = openCohorts;
    return this;
  }

  public CatalogHome addOpenCohortsItem(Cohort openCohortsItem) {
    if (this.openCohorts == null) {
      this.openCohorts = new ArrayList<>();
    }
    this.openCohorts.add(openCohortsItem);
    return this;
  }

  /**
   * Cohorts open for enrolment.
   * @return openCohorts
   */
  
  @JsonProperty("openCohorts")
  public List<@Valid Cohort> getOpenCohorts() {
    return openCohorts;
  }

  @JsonProperty("openCohorts")
  public void setOpenCohorts(List<@Valid Cohort> openCohorts) {
    this.openCohorts = openCohorts;
  }

  public CatalogHome paging(@Nullable NavigationInfo paging) {
    this.paging = paging;
    return this;
  }

  /**
   * Navigation over the pages of courses; next is absent on the last page.
   * @return paging
   */
  
  @JsonProperty("paging")
  public @Nullable NavigationInfo getPaging() {
    return paging;
  }

  @JsonProperty("paging")
  public void setPaging(@Nullable NavigationInfo paging) {
    this.paging = paging;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CatalogHome catalogHome = (CatalogHome) o;
    return Objects.equals(this.featured, catalogHome.featured) &&
        Objects.equals(this.showcase, catalogHome.showcase) &&
        Objects.equals(this.courses, catalogHome.courses) &&
        Objects.equals(this.categories, catalogHome.categories) &&
        Objects.equals(this.openCohorts, catalogHome.openCohorts) &&
        Objects.equals(this.paging, catalogHome.paging);
  }

  @Override
  public int hashCode() {
    return Objects.hash(featured, showcase, courses, categories, openCohorts, paging);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CatalogHome {\n");
    sb.append("    featured: ").append(toIndentedString(featured)).append("\n");
    sb.append("    showcase: ").append(toIndentedString(showcase)).append("\n");
    sb.append("    courses: ").append(toIndentedString(courses)).append("\n");
    sb.append("    categories: ").append(toIndentedString(categories)).append("\n");
    sb.append("    openCohorts: ").append(toIndentedString(openCohorts)).append("\n");
    sb.append("    paging: ").append(toIndentedString(paging)).append("\n");
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

