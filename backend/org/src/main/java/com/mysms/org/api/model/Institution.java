// Owned API contract of org.
package com.mysms.org.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * A school, college, campus or training centre operated by a tenant. Institutions can be nested (group → campus).
 */

public class Institution {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID parentInstitutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String slug;

  /**
   * Kind of institution.
   */
  public enum InstitutionTypeEnum {
    SCHOOL("school"),
    
    COLLEGE("college"),
    
    UNIVERSITY("university"),
    
    COACHING_CENTRE("coaching_centre"),
    
    TRAINING_CENTRE("training_centre");

    private final String value;

    InstitutionTypeEnum(String value) {
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
    public static InstitutionTypeEnum fromValue(String value) {
      for (InstitutionTypeEnum b : InstitutionTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable InstitutionTypeEnum institutionType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String email;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String phone;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String addressLine1;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String addressLine2;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID cityId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID stateProvinceId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID countryId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String postalCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String timezone;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID currencyId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID logoFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID signatureFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String accreditation;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String approvalAuthority;

  /**
   * Institution state.
   */
  public enum StatusEnum {
    ACTIVE("active"),
    
    INACTIVE("inactive");

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
  private @Nullable UUID institutionTypeId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID themeId;

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

  public Institution() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Institution(String code, String name) {
    this.code = code;
    this.name = name;
  }

  public Institution id(@Nullable UUID id) {
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

  public Institution parentInstitutionId(@Nullable UUID parentInstitutionId) {
    this.parentInstitutionId = parentInstitutionId;
    return this;
  }

  /**
   * Parent institution for multi-campus groups.
   * @return parentInstitutionId
   */
  @Valid 
  @JsonProperty("parentInstitutionId")
  public @Nullable UUID getParentInstitutionId() {
    return parentInstitutionId;
  }

  @JsonProperty("parentInstitutionId")
  public void setParentInstitutionId(@Nullable UUID parentInstitutionId) {
    this.parentInstitutionId = parentInstitutionId;
  }

  public Institution code(String code) {
    this.code = code;
    return this;
  }

  /**
   * Institution code, unique within the tenant.
   * @return code
   */
  @NotNull @Size(max = 30) 
  @JsonProperty("code")
  public String getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(String code) {
    this.code = code;
  }

  public Institution name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Institution name.
   * @return name
   */
  @NotNull @Size(max = 200) 
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public Institution slug(@Nullable String slug) {
    this.slug = slug;
    return this;
  }

  /**
   * URL-safe name used for public pages and sub-domains.
   * @return slug
   */
  @Size(max = 200) 
  @JsonProperty("slug")
  public @Nullable String getSlug() {
    return slug;
  }

  @JsonProperty("slug")
  public void setSlug(@Nullable String slug) {
    this.slug = slug;
  }

  public Institution institutionType(@Nullable InstitutionTypeEnum institutionType) {
    this.institutionType = institutionType;
    return this;
  }

  /**
   * Kind of institution.
   * @return institutionType
   */
  
  @JsonProperty("institutionType")
  public @Nullable InstitutionTypeEnum getInstitutionType() {
    return institutionType;
  }

  @JsonProperty("institutionType")
  public void setInstitutionType(@Nullable InstitutionTypeEnum institutionType) {
    this.institutionType = institutionType;
  }

  public Institution email(@Nullable String email) {
    this.email = email;
    return this;
  }

  /**
   * Official e-mail.
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

  public Institution phone(@Nullable String phone) {
    this.phone = phone;
    return this;
  }

  /**
   * Official phone.
   * @return phone
   */
  @Size(max = 20) 
  @JsonProperty("phone")
  public @Nullable String getPhone() {
    return phone;
  }

  @JsonProperty("phone")
  public void setPhone(@Nullable String phone) {
    this.phone = phone;
  }

  public Institution addressLine1(@Nullable String addressLine1) {
    this.addressLine1 = addressLine1;
    return this;
  }

  /**
   * Street address line 1.
   * @return addressLine1
   */
  @Size(max = 255) 
  @JsonProperty("addressLine1")
  public @Nullable String getAddressLine1() {
    return addressLine1;
  }

  @JsonProperty("addressLine1")
  public void setAddressLine1(@Nullable String addressLine1) {
    this.addressLine1 = addressLine1;
  }

  public Institution addressLine2(@Nullable String addressLine2) {
    this.addressLine2 = addressLine2;
    return this;
  }

  /**
   * Street address line 2.
   * @return addressLine2
   */
  @Size(max = 255) 
  @JsonProperty("addressLine2")
  public @Nullable String getAddressLine2() {
    return addressLine2;
  }

  @JsonProperty("addressLine2")
  public void setAddressLine2(@Nullable String addressLine2) {
    this.addressLine2 = addressLine2;
  }

  public Institution cityId(@Nullable UUID cityId) {
    this.cityId = cityId;
    return this;
  }

  /**
   * City.
   * @return cityId
   */
  @Valid 
  @JsonProperty("cityId")
  public @Nullable UUID getCityId() {
    return cityId;
  }

  @JsonProperty("cityId")
  public void setCityId(@Nullable UUID cityId) {
    this.cityId = cityId;
  }

  public Institution stateProvinceId(@Nullable UUID stateProvinceId) {
    this.stateProvinceId = stateProvinceId;
    return this;
  }

  /**
   * State / province.
   * @return stateProvinceId
   */
  @Valid 
  @JsonProperty("stateProvinceId")
  public @Nullable UUID getStateProvinceId() {
    return stateProvinceId;
  }

  @JsonProperty("stateProvinceId")
  public void setStateProvinceId(@Nullable UUID stateProvinceId) {
    this.stateProvinceId = stateProvinceId;
  }

  public Institution countryId(@Nullable UUID countryId) {
    this.countryId = countryId;
    return this;
  }

  /**
   * Country.
   * @return countryId
   */
  @Valid 
  @JsonProperty("countryId")
  public @Nullable UUID getCountryId() {
    return countryId;
  }

  @JsonProperty("countryId")
  public void setCountryId(@Nullable UUID countryId) {
    this.countryId = countryId;
  }

  public Institution postalCode(@Nullable String postalCode) {
    this.postalCode = postalCode;
    return this;
  }

  /**
   * Postal / PIN code.
   * @return postalCode
   */
  @Size(max = 12) 
  @JsonProperty("postalCode")
  public @Nullable String getPostalCode() {
    return postalCode;
  }

  @JsonProperty("postalCode")
  public void setPostalCode(@Nullable String postalCode) {
    this.postalCode = postalCode;
  }

  public Institution timezone(@Nullable String timezone) {
    this.timezone = timezone;
    return this;
  }

  /**
   * Time zone if different from the tenant.
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

  public Institution currencyId(@Nullable UUID currencyId) {
    this.currencyId = currencyId;
    return this;
  }

  /**
   * Operating currency if different from the tenant.
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

  public Institution logoFileId(@Nullable UUID logoFileId) {
    this.logoFileId = logoFileId;
    return this;
  }

  /**
   * Logo image.
   * @return logoFileId
   */
  @Valid 
  @JsonProperty("logoFileId")
  public @Nullable UUID getLogoFileId() {
    return logoFileId;
  }

  @JsonProperty("logoFileId")
  public void setLogoFileId(@Nullable UUID logoFileId) {
    this.logoFileId = logoFileId;
  }

  public Institution signatureFileId(@Nullable UUID signatureFileId) {
    this.signatureFileId = signatureFileId;
    return this;
  }

  /**
   * Authorised signatory's signature used on certificates.
   * @return signatureFileId
   */
  @Valid 
  @JsonProperty("signatureFileId")
  public @Nullable UUID getSignatureFileId() {
    return signatureFileId;
  }

  @JsonProperty("signatureFileId")
  public void setSignatureFileId(@Nullable UUID signatureFileId) {
    this.signatureFileId = signatureFileId;
  }

  public Institution accreditation(@Nullable String accreditation) {
    this.accreditation = accreditation;
    return this;
  }

  /**
   * Accreditation statement printed on certificates.
   * @return accreditation
   */
  
  @JsonProperty("accreditation")
  public @Nullable String getAccreditation() {
    return accreditation;
  }

  @JsonProperty("accreditation")
  public void setAccreditation(@Nullable String accreditation) {
    this.accreditation = accreditation;
  }

  public Institution approvalAuthority(@Nullable String approvalAuthority) {
    this.approvalAuthority = approvalAuthority;
    return this;
  }

  /**
   * Approving / affiliating authority.
   * @return approvalAuthority
   */
  
  @JsonProperty("approvalAuthority")
  public @Nullable String getApprovalAuthority() {
    return approvalAuthority;
  }

  @JsonProperty("approvalAuthority")
  public void setApprovalAuthority(@Nullable String approvalAuthority) {
    this.approvalAuthority = approvalAuthority;
  }

  public Institution status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Institution state.
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

  public Institution institutionTypeId(@Nullable UUID institutionTypeId) {
    this.institutionTypeId = institutionTypeId;
    return this;
  }

  /**
   * Configurable unit type (drives which attributes are captured).
   * @return institutionTypeId
   */
  @Valid 
  @JsonProperty("institutionTypeId")
  public @Nullable UUID getInstitutionTypeId() {
    return institutionTypeId;
  }

  @JsonProperty("institutionTypeId")
  public void setInstitutionTypeId(@Nullable UUID institutionTypeId) {
    this.institutionTypeId = institutionTypeId;
  }

  public Institution themeId(@Nullable UUID themeId) {
    this.themeId = themeId;
    return this;
  }

  /**
   * Visual theme of the institution's portal and apps.
   * @return themeId
   */
  @Valid 
  @JsonProperty("themeId")
  public @Nullable UUID getThemeId() {
    return themeId;
  }

  @JsonProperty("themeId")
  public void setThemeId(@Nullable UUID themeId) {
    this.themeId = themeId;
  }

  public Institution createTime(@Nullable OffsetDateTime createTime) {
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

  public Institution createdBy(@Nullable UUID createdBy) {
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

  public Institution updateTime(@Nullable OffsetDateTime updateTime) {
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

  public Institution updatedBy(@Nullable UUID updatedBy) {
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

  public Institution etag(@Nullable String etag) {
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
    Institution institution = (Institution) o;
    return Objects.equals(this.id, institution.id) &&
        Objects.equals(this.parentInstitutionId, institution.parentInstitutionId) &&
        Objects.equals(this.code, institution.code) &&
        Objects.equals(this.name, institution.name) &&
        Objects.equals(this.slug, institution.slug) &&
        Objects.equals(this.institutionType, institution.institutionType) &&
        Objects.equals(this.email, institution.email) &&
        Objects.equals(this.phone, institution.phone) &&
        Objects.equals(this.addressLine1, institution.addressLine1) &&
        Objects.equals(this.addressLine2, institution.addressLine2) &&
        Objects.equals(this.cityId, institution.cityId) &&
        Objects.equals(this.stateProvinceId, institution.stateProvinceId) &&
        Objects.equals(this.countryId, institution.countryId) &&
        Objects.equals(this.postalCode, institution.postalCode) &&
        Objects.equals(this.timezone, institution.timezone) &&
        Objects.equals(this.currencyId, institution.currencyId) &&
        Objects.equals(this.logoFileId, institution.logoFileId) &&
        Objects.equals(this.signatureFileId, institution.signatureFileId) &&
        Objects.equals(this.accreditation, institution.accreditation) &&
        Objects.equals(this.approvalAuthority, institution.approvalAuthority) &&
        Objects.equals(this.status, institution.status) &&
        Objects.equals(this.institutionTypeId, institution.institutionTypeId) &&
        Objects.equals(this.themeId, institution.themeId) &&
        Objects.equals(this.createTime, institution.createTime) &&
        Objects.equals(this.createdBy, institution.createdBy) &&
        Objects.equals(this.updateTime, institution.updateTime) &&
        Objects.equals(this.updatedBy, institution.updatedBy) &&
        Objects.equals(this.etag, institution.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, parentInstitutionId, code, name, slug, institutionType, email, phone, addressLine1, addressLine2, cityId, stateProvinceId, countryId, postalCode, timezone, currencyId, logoFileId, signatureFileId, accreditation, approvalAuthority, status, institutionTypeId, themeId, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Institution {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    parentInstitutionId: ").append(toIndentedString(parentInstitutionId)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    slug: ").append(toIndentedString(slug)).append("\n");
    sb.append("    institutionType: ").append(toIndentedString(institutionType)).append("\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    phone: ").append(toIndentedString(phone)).append("\n");
    sb.append("    addressLine1: ").append(toIndentedString(addressLine1)).append("\n");
    sb.append("    addressLine2: ").append(toIndentedString(addressLine2)).append("\n");
    sb.append("    cityId: ").append(toIndentedString(cityId)).append("\n");
    sb.append("    stateProvinceId: ").append(toIndentedString(stateProvinceId)).append("\n");
    sb.append("    countryId: ").append(toIndentedString(countryId)).append("\n");
    sb.append("    postalCode: ").append(toIndentedString(postalCode)).append("\n");
    sb.append("    timezone: ").append(toIndentedString(timezone)).append("\n");
    sb.append("    currencyId: ").append(toIndentedString(currencyId)).append("\n");
    sb.append("    logoFileId: ").append(toIndentedString(logoFileId)).append("\n");
    sb.append("    signatureFileId: ").append(toIndentedString(signatureFileId)).append("\n");
    sb.append("    accreditation: ").append(toIndentedString(accreditation)).append("\n");
    sb.append("    approvalAuthority: ").append(toIndentedString(approvalAuthority)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    institutionTypeId: ").append(toIndentedString(institutionTypeId)).append("\n");
    sb.append("    themeId: ").append(toIndentedString(themeId)).append("\n");
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

