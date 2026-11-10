// Owned API contract of people (JAVA-CODING-RULES §7); first materialized from api/openapi/people.yaml on 2026-11-10.
package com.mysms.people.api.model;

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
 * Fields of PersonDocument that can be updated (partial update; see updateMask).
 */

public class PersonDocumentUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  /**
   * Document category.
   */
  public enum DocumentTypeEnum {
    CERTIFICATE("certificate"),
    
    ID_PROOF("id_proof"),
    
    OTHER("other");

    private final String value;

    DocumentTypeEnum(String value) {
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
    public static DocumentTypeEnum fromValue(String value) {
      for (DocumentTypeEnum b : DocumentTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable DocumentTypeEnum documentType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID fileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer documentVersion;

  /**
   * Whether current.
   */
  public enum StatusEnum {
    ACTIVE("active"),
    
    ARCHIVED("archived");

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

  public PersonDocumentUpdate institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution that holds the document.
   * @return institutionId
   */
  @Valid 
  @JsonProperty("institutionId")
  public @Nullable UUID getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
  }

  public PersonDocumentUpdate documentType(@Nullable DocumentTypeEnum documentType) {
    this.documentType = documentType;
    return this;
  }

  /**
   * Document category.
   * @return documentType
   */
  
  @JsonProperty("documentType")
  public @Nullable DocumentTypeEnum getDocumentType() {
    return documentType;
  }

  @JsonProperty("documentType")
  public void setDocumentType(@Nullable DocumentTypeEnum documentType) {
    this.documentType = documentType;
  }

  public PersonDocumentUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Document title.
   * @return name
   */
  @Size(max = 200) 
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public PersonDocumentUpdate fileId(@Nullable UUID fileId) {
    this.fileId = fileId;
    return this;
  }

  /**
   * Stored file.
   * @return fileId
   */
  @Valid 
  @JsonProperty("fileId")
  public @Nullable UUID getFileId() {
    return fileId;
  }

  @JsonProperty("fileId")
  public void setFileId(@Nullable UUID fileId) {
    this.fileId = fileId;
  }

  public PersonDocumentUpdate documentVersion(@Nullable Integer documentVersion) {
    this.documentVersion = documentVersion;
    return this;
  }

  /**
   * Version number of this document.
   * @return documentVersion
   */
  
  @JsonProperty("documentVersion")
  public @Nullable Integer getDocumentVersion() {
    return documentVersion;
  }

  @JsonProperty("documentVersion")
  public void setDocumentVersion(@Nullable Integer documentVersion) {
    this.documentVersion = documentVersion;
  }

  public PersonDocumentUpdate status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Whether current.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PersonDocumentUpdate personDocumentUpdate = (PersonDocumentUpdate) o;
    return Objects.equals(this.institutionId, personDocumentUpdate.institutionId) &&
        Objects.equals(this.documentType, personDocumentUpdate.documentType) &&
        Objects.equals(this.name, personDocumentUpdate.name) &&
        Objects.equals(this.fileId, personDocumentUpdate.fileId) &&
        Objects.equals(this.documentVersion, personDocumentUpdate.documentVersion) &&
        Objects.equals(this.status, personDocumentUpdate.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, documentType, name, fileId, documentVersion, status);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PersonDocumentUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    documentType: ").append(toIndentedString(documentType)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    fileId: ").append(toIndentedString(fileId)).append("\n");
    sb.append("    documentVersion: ").append(toIndentedString(documentVersion)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
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

