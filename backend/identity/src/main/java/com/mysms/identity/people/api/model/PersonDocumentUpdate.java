// Owned API contract of people.
package com.mysms.identity.people.api.model;

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
 * Fields of PersonDocument that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class PersonDocumentUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> institutionId;

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
  private @Nullable Optional<@NotNull DocumentTypeEnum> documentType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> fileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> documentVersion;

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
  private @Nullable Optional<@NotNull StatusEnum> status;

  public PersonDocumentUpdate institutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution that holds the document.
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

  public PersonDocumentUpdate documentType(@Nullable Optional<DocumentTypeEnum> documentType) {
    this.documentType = documentType;
    return this;
  }

  /**
   * Document category.
   * @return documentType
   */
  
  @JsonProperty("documentType")
  public @Nullable Optional<DocumentTypeEnum> getDocumentType() {
    return documentType;
  }

  @JsonProperty("documentType")
  public void setDocumentType(@Nullable Optional<DocumentTypeEnum> documentType) {
    this.documentType = documentType;
  }

  public PersonDocumentUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Document title.
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

  public PersonDocumentUpdate fileId(@Nullable Optional<UUID> fileId) {
    this.fileId = fileId;
    return this;
  }

  /**
   * Stored file.
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

  public PersonDocumentUpdate documentVersion(@Nullable Optional<Integer> documentVersion) {
    this.documentVersion = documentVersion;
    return this;
  }

  /**
   * Version number of this document.
   * @return documentVersion
   */
  
  @JsonProperty("documentVersion")
  public @Nullable Optional<Integer> getDocumentVersion() {
    return documentVersion;
  }

  @JsonProperty("documentVersion")
  public void setDocumentVersion(@Nullable Optional<Integer> documentVersion) {
    this.documentVersion = documentVersion;
  }

  public PersonDocumentUpdate status(@Nullable Optional<StatusEnum> status) {
    this.status = status;
    return this;
  }

  /**
   * Whether current.
   * @return status
   */
  
  @JsonProperty("status")
  public @Nullable Optional<StatusEnum> getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable Optional<StatusEnum> status) {
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

