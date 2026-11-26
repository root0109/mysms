// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of Ebook that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class EbookUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 255) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> description;

  /**
   * Source document kind.
   */
  public enum EbookTypeEnum {
    DOCUMENT("document"),
    
    PRESENTATION("presentation"),
    
    PDF("pdf"),
    
    EPUB("epub"),
    
    OTHER("other");

    private final String value;

    EbookTypeEnum(String value) {
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
    public static EbookTypeEnum fromValue(String value) {
      for (EbookTypeEnum b : EbookTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull EbookTypeEnum> ebookType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> fileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> sourceFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 100) String> conversionJobRef;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> showRelatedMaterials;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> relatedMaterialSeed;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> categoryId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isInRepository;

  public EbookUpdate institutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Owning institution.
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

  public EbookUpdate title(@Nullable Optional<String> title) {
    this.title = title;
    return this;
  }

  /**
   * Title.
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

  public EbookUpdate description(@Nullable Optional<String> description) {
    this.description = description;
    return this;
  }

  /**
   * Description.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable Optional<String> getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable Optional<String> description) {
    this.description = description;
  }

  public EbookUpdate ebookType(@Nullable Optional<EbookTypeEnum> ebookType) {
    this.ebookType = ebookType;
    return this;
  }

  /**
   * Source document kind.
   * @return ebookType
   */
  
  @JsonProperty("ebookType")
  public @Nullable Optional<EbookTypeEnum> getEbookType() {
    return ebookType;
  }

  @JsonProperty("ebookType")
  public void setEbookType(@Nullable Optional<EbookTypeEnum> ebookType) {
    this.ebookType = ebookType;
  }

  public EbookUpdate fileId(@Nullable Optional<UUID> fileId) {
    this.fileId = fileId;
    return this;
  }

  /**
   * Converted e-book file.
   * @return fileId
   */
  
  @JsonProperty("fileId")
  public @Nullable Optional<UUID> getFileId() {
    return fileId;
  }

  @JsonProperty("fileId")
  public void setFileId(@Nullable Optional<UUID> fileId) {
    this.fileId = fileId;
  }

  public EbookUpdate sourceFileId(@Nullable Optional<UUID> sourceFileId) {
    this.sourceFileId = sourceFileId;
    return this;
  }

  /**
   * Uploaded source document (conversion state is tracked on the file).
   * @return sourceFileId
   */
  
  @JsonProperty("sourceFileId")
  public @Nullable Optional<UUID> getSourceFileId() {
    return sourceFileId;
  }

  @JsonProperty("sourceFileId")
  public void setSourceFileId(@Nullable Optional<UUID> sourceFileId) {
    this.sourceFileId = sourceFileId;
  }

  public EbookUpdate conversionJobRef(@Nullable Optional<String> conversionJobRef) {
    this.conversionJobRef = conversionJobRef;
    return this;
  }

  /**
   * External conversion-service job reference.
   * @return conversionJobRef
   */
  
  @JsonProperty("conversionJobRef")
  public @Nullable Optional<String> getConversionJobRef() {
    return conversionJobRef;
  }

  @JsonProperty("conversionJobRef")
  public void setConversionJobRef(@Nullable Optional<String> conversionJobRef) {
    this.conversionJobRef = conversionJobRef;
  }

  public EbookUpdate showRelatedMaterials(@Nullable Optional<Boolean> showRelatedMaterials) {
    this.showRelatedMaterials = showRelatedMaterials;
    return this;
  }

  /**
   * Show recommended related materials.
   * @return showRelatedMaterials
   */
  
  @JsonProperty("showRelatedMaterials")
  public @Nullable Optional<Boolean> getShowRelatedMaterials() {
    return showRelatedMaterials;
  }

  @JsonProperty("showRelatedMaterials")
  public void setShowRelatedMaterials(@Nullable Optional<Boolean> showRelatedMaterials) {
    this.showRelatedMaterials = showRelatedMaterials;
  }

  public EbookUpdate relatedMaterialSeed(@Nullable Optional<String> relatedMaterialSeed) {
    this.relatedMaterialSeed = relatedMaterialSeed;
    return this;
  }

  /**
   * Initial ordered list of related materials before personalisation.
   * @return relatedMaterialSeed
   */
  
  @JsonProperty("relatedMaterialSeed")
  public @Nullable Optional<String> getRelatedMaterialSeed() {
    return relatedMaterialSeed;
  }

  @JsonProperty("relatedMaterialSeed")
  public void setRelatedMaterialSeed(@Nullable Optional<String> relatedMaterialSeed) {
    this.relatedMaterialSeed = relatedMaterialSeed;
  }

  public EbookUpdate categoryId(@Nullable Optional<UUID> categoryId) {
    this.categoryId = categoryId;
    return this;
  }

  /**
   * Category.
   * @return categoryId
   */
  
  @JsonProperty("categoryId")
  public @Nullable Optional<UUID> getCategoryId() {
    return categoryId;
  }

  @JsonProperty("categoryId")
  public void setCategoryId(@Nullable Optional<UUID> categoryId) {
    this.categoryId = categoryId;
  }

  public EbookUpdate isInRepository(@Nullable Optional<Boolean> isInRepository) {
    this.isInRepository = isInRepository;
    return this;
  }

  /**
   * Listed in the shared content repository.
   * @return isInRepository
   */
  
  @JsonProperty("isInRepository")
  public @Nullable Optional<Boolean> getIsInRepository() {
    return isInRepository;
  }

  @JsonProperty("isInRepository")
  public void setIsInRepository(@Nullable Optional<Boolean> isInRepository) {
    this.isInRepository = isInRepository;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EbookUpdate ebookUpdate = (EbookUpdate) o;
    return Objects.equals(this.institutionId, ebookUpdate.institutionId) &&
        Objects.equals(this.title, ebookUpdate.title) &&
        Objects.equals(this.description, ebookUpdate.description) &&
        Objects.equals(this.ebookType, ebookUpdate.ebookType) &&
        Objects.equals(this.fileId, ebookUpdate.fileId) &&
        Objects.equals(this.sourceFileId, ebookUpdate.sourceFileId) &&
        Objects.equals(this.conversionJobRef, ebookUpdate.conversionJobRef) &&
        Objects.equals(this.showRelatedMaterials, ebookUpdate.showRelatedMaterials) &&
        Objects.equals(this.relatedMaterialSeed, ebookUpdate.relatedMaterialSeed) &&
        Objects.equals(this.categoryId, ebookUpdate.categoryId) &&
        Objects.equals(this.isInRepository, ebookUpdate.isInRepository);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, title, description, ebookType, fileId, sourceFileId, conversionJobRef, showRelatedMaterials, relatedMaterialSeed, categoryId, isInRepository);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EbookUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    ebookType: ").append(toIndentedString(ebookType)).append("\n");
    sb.append("    fileId: ").append(toIndentedString(fileId)).append("\n");
    sb.append("    sourceFileId: ").append(toIndentedString(sourceFileId)).append("\n");
    sb.append("    conversionJobRef: ").append(toIndentedString(conversionJobRef)).append("\n");
    sb.append("    showRelatedMaterials: ").append(toIndentedString(showRelatedMaterials)).append("\n");
    sb.append("    relatedMaterialSeed: ").append(toIndentedString(relatedMaterialSeed)).append("\n");
    sb.append("    categoryId: ").append(toIndentedString(categoryId)).append("\n");
    sb.append("    isInRepository: ").append(toIndentedString(isInRepository)).append("\n");
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

