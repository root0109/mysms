// Owned API contract of iam.
package com.mysms.identity.iam.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.identity.iam.api.model.Institution;
import com.mysms.identity.iam.api.model.Person;
import com.mysms.identity.iam.api.model.RoleGrant;
import com.mysms.identity.iam.api.model.UserAccount;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Me
 */

public class Me {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UserAccount user;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Person person;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<@Valid RoleGrant> roles = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<String> permissions = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<@Valid Institution> institutions = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<UUID> studentIds = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<UUID> staffIds = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID guardianId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable RoleGrant activeRole;

  public Me() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Me(UserAccount user) {
    this.user = user;
  }

  public Me user(UserAccount user) {
    this.user = user;
    return this;
  }

  /**
   * Get user
   * @return user
   */
  @NotNull @Valid 
  @JsonProperty("user")
  public UserAccount getUser() {
    return user;
  }

  @JsonProperty("user")
  public void setUser(UserAccount user) {
    this.user = user;
  }

  public Me person(@Nullable Person person) {
    this.person = person;
    return this;
  }

  /**
   * Get person
   * @return person
   */
  @Valid 
  @JsonProperty("person")
  public @Nullable Person getPerson() {
    return person;
  }

  @JsonProperty("person")
  public void setPerson(@Nullable Person person) {
    this.person = person;
  }

  public Me roles(List<@Valid RoleGrant> roles) {
    this.roles = roles;
    return this;
  }

  public Me addRolesItem(RoleGrant rolesItem) {
    if (this.roles == null) {
      this.roles = new ArrayList<>();
    }
    this.roles.add(rolesItem);
    return this;
  }

  /**
   * Role grants with scope.
   * @return roles
   */
  
  @JsonProperty("roles")
  public List<@Valid RoleGrant> getRoles() {
    return roles;
  }

  @JsonProperty("roles")
  public void setRoles(List<@Valid RoleGrant> roles) {
    this.roles = roles;
  }

  public Me permissions(List<String> permissions) {
    this.permissions = permissions;
    return this;
  }

  public Me addPermissionsItem(String permissionsItem) {
    if (this.permissions == null) {
      this.permissions = new ArrayList<>();
    }
    this.permissions.add(permissionsItem);
    return this;
  }

  /**
   * Effective permission codes.
   * @return permissions
   */
  
  @JsonProperty("permissions")
  public List<String> getPermissions() {
    return permissions;
  }

  @JsonProperty("permissions")
  public void setPermissions(List<String> permissions) {
    this.permissions = permissions;
  }

  public Me institutions(List<@Valid Institution> institutions) {
    this.institutions = institutions;
    return this;
  }

  public Me addInstitutionsItem(Institution institutionsItem) {
    if (this.institutions == null) {
      this.institutions = new ArrayList<>();
    }
    this.institutions.add(institutionsItem);
    return this;
  }

  /**
   * Institutions the user can act in.
   * @return institutions
   */
  
  @JsonProperty("institutions")
  public List<@Valid Institution> getInstitutions() {
    return institutions;
  }

  @JsonProperty("institutions")
  public void setInstitutions(List<@Valid Institution> institutions) {
    this.institutions = institutions;
  }

  public Me studentIds(List<UUID> studentIds) {
    this.studentIds = studentIds;
    return this;
  }

  public Me addStudentIdsItem(UUID studentIdsItem) {
    if (this.studentIds == null) {
      this.studentIds = new ArrayList<>();
    }
    this.studentIds.add(studentIdsItem);
    return this;
  }

  /**
   * Student records of this person.
   * @return studentIds
   */
  
  @JsonProperty("studentIds")
  public List<UUID> getStudentIds() {
    return studentIds;
  }

  @JsonProperty("studentIds")
  public void setStudentIds(List<UUID> studentIds) {
    this.studentIds = studentIds;
  }

  public Me staffIds(List<UUID> staffIds) {
    this.staffIds = staffIds;
    return this;
  }

  public Me addStaffIdsItem(UUID staffIdsItem) {
    if (this.staffIds == null) {
      this.staffIds = new ArrayList<>();
    }
    this.staffIds.add(staffIdsItem);
    return this;
  }

  /**
   * Staff records of this person.
   * @return staffIds
   */
  
  @JsonProperty("staffIds")
  public List<UUID> getStaffIds() {
    return staffIds;
  }

  @JsonProperty("staffIds")
  public void setStaffIds(List<UUID> staffIds) {
    this.staffIds = staffIds;
  }

  public Me guardianId(@Nullable UUID guardianId) {
    this.guardianId = guardianId;
    return this;
  }

  /**
   * Guardian record of this person.
   * @return guardianId
   */
  @Valid 
  @JsonProperty("guardianId")
  public @Nullable UUID getGuardianId() {
    return guardianId;
  }

  @JsonProperty("guardianId")
  public void setGuardianId(@Nullable UUID guardianId) {
    this.guardianId = guardianId;
  }

  public Me activeRole(@Nullable RoleGrant activeRole) {
    this.activeRole = activeRole;
    return this;
  }

  /**
   * Get activeRole
   * @return activeRole
   */
  @Valid 
  @JsonProperty("activeRole")
  public @Nullable RoleGrant getActiveRole() {
    return activeRole;
  }

  @JsonProperty("activeRole")
  public void setActiveRole(@Nullable RoleGrant activeRole) {
    this.activeRole = activeRole;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Me me = (Me) o;
    return Objects.equals(this.user, me.user) &&
        Objects.equals(this.person, me.person) &&
        Objects.equals(this.roles, me.roles) &&
        Objects.equals(this.permissions, me.permissions) &&
        Objects.equals(this.institutions, me.institutions) &&
        Objects.equals(this.studentIds, me.studentIds) &&
        Objects.equals(this.staffIds, me.staffIds) &&
        Objects.equals(this.guardianId, me.guardianId) &&
        Objects.equals(this.activeRole, me.activeRole);
  }

  @Override
  public int hashCode() {
    return Objects.hash(user, person, roles, permissions, institutions, studentIds, staffIds, guardianId, activeRole);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Me {\n");
    sb.append("    user: ").append(toIndentedString(user)).append("\n");
    sb.append("    person: ").append(toIndentedString(person)).append("\n");
    sb.append("    roles: ").append(toIndentedString(roles)).append("\n");
    sb.append("    permissions: ").append(toIndentedString(permissions)).append("\n");
    sb.append("    institutions: ").append(toIndentedString(institutions)).append("\n");
    sb.append("    studentIds: ").append(toIndentedString(studentIds)).append("\n");
    sb.append("    staffIds: ").append(toIndentedString(staffIds)).append("\n");
    sb.append("    guardianId: ").append(toIndentedString(guardianId)).append("\n");
    sb.append("    activeRole: ").append(toIndentedString(activeRole)).append("\n");
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

