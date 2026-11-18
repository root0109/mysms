// Owned API contract of iam.
package com.mysms.iam.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** The schools of a tenant a guest may sign up at, by name. */
public class SignUpInstitutionList {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid SignUpInstitution> items = new ArrayList<>();

  public SignUpInstitutionList() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SignUpInstitutionList(List<@Valid SignUpInstitution> items) {
    this.items = items;
  }

  /**
   * Get items
   * @return items
   */
  @NotNull
  @JsonProperty("items")
  public List<@Valid SignUpInstitution> getItems() {
    return items;
  }

  @JsonProperty("items")
  public void setItems(List<@Valid SignUpInstitution> items) {
    this.items = items;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    return Objects.equals(this.items, ((SignUpInstitutionList) o).items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items);
  }

  @Override
  public String toString() {
    return "class SignUpInstitutionList {\n    items: " + items + "\n}";
  }
}
