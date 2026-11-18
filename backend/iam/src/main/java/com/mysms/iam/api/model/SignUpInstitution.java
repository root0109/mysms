// Owned API contract of iam.
package com.mysms.iam.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import java.util.UUID;

/** A school a guest may sign up at: the public projection of an active institution. */
public class SignUpInstitution {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String name;

  public SignUpInstitution() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SignUpInstitution(UUID id, String name) {
    this.id = id;
    this.name = name;
  }

  /**
   * Institution (org.institution.id); the sign-up's institutionId.
   * @return id
   */
  @NotNull
  @JsonProperty("id")
  public UUID getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(UUID id) {
    this.id = id;
  }

  /**
   * Name.
   * @return name
   */
  @NotNull
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SignUpInstitution other = (SignUpInstitution) o;
    return Objects.equals(this.id, other.id) && Objects.equals(this.name, other.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name);
  }

  @Override
  public String toString() {
    return "class SignUpInstitution {\n    id: " + id + "\n    name: " + name + "\n}";
  }
}
