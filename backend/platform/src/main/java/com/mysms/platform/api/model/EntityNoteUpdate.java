// Owned API contract of platform.
package com.mysms.platform.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of EntityNote that can be updated (partial update; see updateMask).
 */

public class EntityNoteUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String entityType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID entityId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String noteText;

  public EntityNoteUpdate institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution scope.
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

  public EntityNoteUpdate entityType(@Nullable String entityType) {
    this.entityType = entityType;
    return this;
  }

  /**
   * Polymorphic target table name.
   * @return entityType
   */
  @Size(max = 80) 
  @JsonProperty("entityType")
  public @Nullable String getEntityType() {
    return entityType;
  }

  @JsonProperty("entityType")
  public void setEntityType(@Nullable String entityType) {
    this.entityType = entityType;
  }

  public EntityNoteUpdate entityId(@Nullable UUID entityId) {
    this.entityId = entityId;
    return this;
  }

  /**
   * Polymorphic target record id.
   * @return entityId
   */
  @Valid 
  @JsonProperty("entityId")
  public @Nullable UUID getEntityId() {
    return entityId;
  }

  @JsonProperty("entityId")
  public void setEntityId(@Nullable UUID entityId) {
    this.entityId = entityId;
  }

  public EntityNoteUpdate noteText(@Nullable String noteText) {
    this.noteText = noteText;
    return this;
  }

  /**
   * Note content.
   * @return noteText
   */
  
  @JsonProperty("noteText")
  public @Nullable String getNoteText() {
    return noteText;
  }

  @JsonProperty("noteText")
  public void setNoteText(@Nullable String noteText) {
    this.noteText = noteText;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EntityNoteUpdate entityNoteUpdate = (EntityNoteUpdate) o;
    return Objects.equals(this.institutionId, entityNoteUpdate.institutionId) &&
        Objects.equals(this.entityType, entityNoteUpdate.entityType) &&
        Objects.equals(this.entityId, entityNoteUpdate.entityId) &&
        Objects.equals(this.noteText, entityNoteUpdate.noteText);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, entityType, entityId, noteText);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EntityNoteUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    entityType: ").append(toIndentedString(entityType)).append("\n");
    sb.append("    entityId: ").append(toIndentedString(entityId)).append("\n");
    sb.append("    noteText: ").append(toIndentedString(noteText)).append("\n");
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

