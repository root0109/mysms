// Owned API contract of academics.
package com.mysms.academics.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of Subject that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class SubjectUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> departmentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 256) String> code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 150) String> name;

  /**
   * Theory / practical.
   */
  public enum DeliveryTypeEnum {
    THEORY("theory"),
    
    PRACTICAL("practical"),
    
    BOTH("both"),
    
    OTHER("other");

    private final String value;

    DeliveryTypeEnum(String value) {
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
    public static DeliveryTypeEnum fromValue(String value) {
      for (DeliveryTypeEnum b : DeliveryTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull DeliveryTypeEnum> deliveryType;

  /**
   * Default category when offered.
   */
  public enum DefaultCategoryEnum {
    COMPULSORY("compulsory"),
    
    ELECTIVE("elective");

    private final String value;

    DefaultCategoryEnum(String value) {
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
    public static DefaultCategoryEnum fromValue(String value) {
      for (DefaultCategoryEnum b : DefaultCategoryEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull DefaultCategoryEnum> defaultCategory;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> gradeWeightage;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  public SubjectUpdate institutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  
  @JsonProperty("institutionId")
  public @Nullable Optional<UUID> getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
  }

  public SubjectUpdate departmentId(@Nullable Optional<UUID> departmentId) {
    this.departmentId = departmentId;
    return this;
  }

  /**
   * Owning department.
   * @return departmentId
   */
  
  @JsonProperty("departmentId")
  public @Nullable Optional<UUID> getDepartmentId() {
    return departmentId;
  }

  @JsonProperty("departmentId")
  public void setDepartmentId(@Nullable Optional<UUID> departmentId) {
    this.departmentId = departmentId;
  }

  public SubjectUpdate code(@Nullable Optional<String> code) {
    this.code = code;
    return this;
  }

  /**
   * Subject code.
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

  public SubjectUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Subject name.
   * @return name
   */
  
  @JsonProperty("name")
  public @Nullable Optional<String> getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable Optional<String> name) {
    this.name = name;
  }

  public SubjectUpdate deliveryType(@Nullable Optional<DeliveryTypeEnum> deliveryType) {
    this.deliveryType = deliveryType;
    return this;
  }

  /**
   * Theory / practical.
   * @return deliveryType
   */
  
  @JsonProperty("deliveryType")
  public @Nullable Optional<DeliveryTypeEnum> getDeliveryType() {
    return deliveryType;
  }

  @JsonProperty("deliveryType")
  public void setDeliveryType(@Nullable Optional<DeliveryTypeEnum> deliveryType) {
    this.deliveryType = deliveryType;
  }

  public SubjectUpdate defaultCategory(@Nullable Optional<DefaultCategoryEnum> defaultCategory) {
    this.defaultCategory = defaultCategory;
    return this;
  }

  /**
   * Default category when offered.
   * @return defaultCategory
   */
  
  @JsonProperty("defaultCategory")
  public @Nullable Optional<DefaultCategoryEnum> getDefaultCategory() {
    return defaultCategory;
  }

  @JsonProperty("defaultCategory")
  public void setDefaultCategory(@Nullable Optional<DefaultCategoryEnum> defaultCategory) {
    this.defaultCategory = defaultCategory;
  }

  public SubjectUpdate gradeWeightage(@Nullable Optional<String> gradeWeightage) {
    this.gradeWeightage = gradeWeightage;
    return this;
  }

  /**
   * Weight of the subject in aggregate grades.
   * @return gradeWeightage
   */
  
  @JsonProperty("gradeWeightage")
  public @Nullable Optional<String> getGradeWeightage() {
    return gradeWeightage;
  }

  @JsonProperty("gradeWeightage")
  public void setGradeWeightage(@Nullable Optional<String> gradeWeightage) {
    this.gradeWeightage = gradeWeightage;
  }

  public SubjectUpdate isActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether offered.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SubjectUpdate subjectUpdate = (SubjectUpdate) o;
    return Objects.equals(this.institutionId, subjectUpdate.institutionId) &&
        Objects.equals(this.departmentId, subjectUpdate.departmentId) &&
        Objects.equals(this.code, subjectUpdate.code) &&
        Objects.equals(this.name, subjectUpdate.name) &&
        Objects.equals(this.deliveryType, subjectUpdate.deliveryType) &&
        Objects.equals(this.defaultCategory, subjectUpdate.defaultCategory) &&
        Objects.equals(this.gradeWeightage, subjectUpdate.gradeWeightage) &&
        Objects.equals(this.isActive, subjectUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, departmentId, code, name, deliveryType, defaultCategory, gradeWeightage, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SubjectUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    departmentId: ").append(toIndentedString(departmentId)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    deliveryType: ").append(toIndentedString(deliveryType)).append("\n");
    sb.append("    defaultCategory: ").append(toIndentedString(defaultCategory)).append("\n");
    sb.append("    gradeWeightage: ").append(toIndentedString(gradeWeightage)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
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

