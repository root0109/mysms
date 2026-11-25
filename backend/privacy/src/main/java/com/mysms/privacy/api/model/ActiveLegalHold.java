// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.lang.Nullable;

/**
 * What one active legal hold covers, for a module that erases or sweeps a subject's data (internal): rows it covers are kept and counted as held.
 */

public class ActiveLegalHold {

  /**
   * Why the data is held.
   */
  public enum ReasonCodeEnum {
    AUDIT("audit"),
    
    LITIGATION("litigation"),
    
    REGULATOR("regulator"),
    
    SAFEGUARDING("safeguarding");

    private final String value;

    ReasonCodeEnum(String value) {
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
    public static ReasonCodeEnum fromValue(String value) {
      for (ReasonCodeEnum b : ReasonCodeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  /**
   * subject (all the subject's data), category (the category's data) or records (the named records).
   */
  public enum ScopeEnum {
    CATEGORY("category"),
    
    RECORDS("records"),
    
    SUBJECT("subject");

    private final String value;

    ScopeEnum(String value) {
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
    public static ScopeEnum fromValue(String value) {
      for (ScopeEnum b : ScopeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private ReasonCodeEnum reasonCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private ScopeEnum scope;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID subjectPersonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String categoryCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String module;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String entityType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<UUID> entityIds;

  public ActiveLegalHold() {
    super();
  }

  public ActiveLegalHold id(UUID id) {
    this.id = id;
    return this;
  }

  /**
   * The hold.
   * @return id
   */
  @NotNull @Valid 
  @JsonProperty("id")
  public UUID getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(UUID id) {
    this.id = id;
  }

  public ActiveLegalHold reasonCode(ReasonCodeEnum reasonCode) {
    this.reasonCode = reasonCode;
    return this;
  }

  /**
   * Why the data is held.
   * @return reasonCode
   */
  @NotNull 
  @JsonProperty("reasonCode")
  public ReasonCodeEnum getReasonCode() {
    return reasonCode;
  }

  @JsonProperty("reasonCode")
  public void setReasonCode(ReasonCodeEnum reasonCode) {
    this.reasonCode = reasonCode;
  }

  public ActiveLegalHold scope(ScopeEnum scope) {
    this.scope = scope;
    return this;
  }

  /**
   * subject (all the subject's data), category (the category's data) or records (the named records).
   * @return scope
   */
  @NotNull 
  @JsonProperty("scope")
  public ScopeEnum getScope() {
    return scope;
  }

  @JsonProperty("scope")
  public void setScope(ScopeEnum scope) {
    this.scope = scope;
  }

  public ActiveLegalHold subjectPersonId(@Nullable UUID subjectPersonId) {
    this.subjectPersonId = subjectPersonId;
    return this;
  }

  /**
   * The data principal held; absent = everyone's data of the category or records.
   * @return subjectPersonId
   */
  @Valid 
  @JsonProperty("subjectPersonId")
  public @Nullable UUID getSubjectPersonId() {
    return subjectPersonId;
  }

  @JsonProperty("subjectPersonId")
  public void setSubjectPersonId(@Nullable UUID subjectPersonId) {
    this.subjectPersonId = subjectPersonId;
  }

  public ActiveLegalHold categoryCode(@Nullable String categoryCode) {
    this.categoryCode = categoryCode;
    return this;
  }

  /**
   * The data category held (scope category).
   * @return categoryCode
   */
   
  @JsonProperty("categoryCode")
  public @Nullable String getCategoryCode() {
    return categoryCode;
  }

  @JsonProperty("categoryCode")
  public void setCategoryCode(@Nullable String categoryCode) {
    this.categoryCode = categoryCode;
  }

  public ActiveLegalHold module(@Nullable String module) {
    this.module = module;
    return this;
  }

  /**
   * The module holding the records (scope records).
   * @return module
   */
   
  @JsonProperty("module")
  public @Nullable String getModule() {
    return module;
  }

  @JsonProperty("module")
  public void setModule(@Nullable String module) {
    this.module = module;
  }

  public ActiveLegalHold entityType(@Nullable String entityType) {
    this.entityType = entityType;
    return this;
  }

  /**
   * The kind of record held (scope records).
   * @return entityType
   */
   
  @JsonProperty("entityType")
  public @Nullable String getEntityType() {
    return entityType;
  }

  @JsonProperty("entityType")
  public void setEntityType(@Nullable String entityType) {
    this.entityType = entityType;
  }

  public ActiveLegalHold entityIds(@Nullable List<UUID> entityIds) {
    this.entityIds = entityIds;
    return this;
  }

  public ActiveLegalHold addEntityIdsItem(UUID entityIdsItem) {
    if (this.entityIds == null) {
      this.entityIds = new ArrayList<>();
    }
    this.entityIds.add(entityIdsItem);
    return this;
  }

  /**
   * The records held (scope records).
   * @return entityIds
   */
   
  @JsonProperty("entityIds")
  public @Nullable List<UUID> getEntityIds() {
    return entityIds;
  }

  @JsonProperty("entityIds")
  public void setEntityIds(@Nullable List<UUID> entityIds) {
    this.entityIds = entityIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ActiveLegalHold other = (ActiveLegalHold) o;
    return Objects.equals(this.id, other.id) &&
        Objects.equals(this.reasonCode, other.reasonCode) &&
        Objects.equals(this.scope, other.scope) &&
        Objects.equals(this.subjectPersonId, other.subjectPersonId) &&
        Objects.equals(this.categoryCode, other.categoryCode) &&
        Objects.equals(this.module, other.module) &&
        Objects.equals(this.entityType, other.entityType) &&
        Objects.equals(this.entityIds, other.entityIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, reasonCode, scope, subjectPersonId, categoryCode, module, entityType, entityIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ActiveLegalHold {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    reasonCode: ").append(toIndentedString(reasonCode)).append("\n");
    sb.append("    scope: ").append(toIndentedString(scope)).append("\n");
    sb.append("    subjectPersonId: ").append(toIndentedString(subjectPersonId)).append("\n");
    sb.append("    categoryCode: ").append(toIndentedString(categoryCode)).append("\n");
    sb.append("    module: ").append(toIndentedString(module)).append("\n");
    sb.append("    entityType: ").append(toIndentedString(entityType)).append("\n");
    sb.append("    entityIds: ").append(toIndentedString(entityIds)).append("\n");
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
