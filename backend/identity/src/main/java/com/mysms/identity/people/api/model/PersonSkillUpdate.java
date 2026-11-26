// Owned API contract of people.
package com.mysms.identity.people.api.model;

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
 * Fields of PersonSkill that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class PersonSkillUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> skillId;

  public PersonSkillUpdate skillId(@Nullable Optional<UUID> skillId) {
    this.skillId = skillId;
    return this;
  }

  /**
   * Skill.
   * @return skillId
   */
  
  @JsonProperty("skillId")
  public @Nullable Optional<UUID> getSkillId() {
    return skillId;
  }

  @JsonProperty("skillId")
  public void setSkillId(@Nullable Optional<UUID> skillId) {
    this.skillId = skillId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PersonSkillUpdate personSkillUpdate = (PersonSkillUpdate) o;
    return Objects.equals(this.skillId, personSkillUpdate.skillId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(skillId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PersonSkillUpdate {\n");
    sb.append("    skillId: ").append(toIndentedString(skillId)).append("\n");
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

