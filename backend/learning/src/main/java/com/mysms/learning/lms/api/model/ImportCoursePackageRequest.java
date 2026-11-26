// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import org.springframework.lang.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/**
 * ImportCoursePackageRequest
 */

public class ImportCoursePackageRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID fileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID targetChapterId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean addToRepository;

  public ImportCoursePackageRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ImportCoursePackageRequest(UUID fileId) {
    this.fileId = fileId;
  }

  public ImportCoursePackageRequest fileId(UUID fileId) {
    this.fileId = fileId;
    return this;
  }

  /**
   * The uploaded package (a platform file, .zip).
   * @return fileId
   */
  @NotNull @Valid 
  @JsonProperty("fileId")
  public UUID getFileId() {
    return fileId;
  }

  @JsonProperty("fileId")
  public void setFileId(UUID fileId) {
    this.fileId = fileId;
  }

  public ImportCoursePackageRequest targetChapterId(@Nullable UUID targetChapterId) {
    this.targetChapterId = targetChapterId;
    return this;
  }

  /**
   * Chapter of the course the imported content is placed in (default: new chapters, as in the package).
   * @return targetChapterId
   */
  @Valid 
  @JsonProperty("targetChapterId")
  public @Nullable UUID getTargetChapterId() {
    return targetChapterId;
  }

  @JsonProperty("targetChapterId")
  public void setTargetChapterId(@Nullable UUID targetChapterId) {
    this.targetChapterId = targetChapterId;
  }

  public ImportCoursePackageRequest addToRepository(@Nullable Boolean addToRepository) {
    this.addToRepository = addToRepository;
    return this;
  }

  /**
   * Also list the imported quizzes and assignments in the shared content repository.
   * @return addToRepository
   */
  
  @JsonProperty("addToRepository")
  public @Nullable Boolean getAddToRepository() {
    return addToRepository;
  }

  @JsonProperty("addToRepository")
  public void setAddToRepository(@Nullable Boolean addToRepository) {
    this.addToRepository = addToRepository;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ImportCoursePackageRequest other = (ImportCoursePackageRequest) o;
    return Objects.equals(this.fileId, other.fileId) && Objects.equals(this.targetChapterId, other.targetChapterId)
        && Objects.equals(this.addToRepository, other.addToRepository);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileId, targetChapterId, addToRepository);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ImportCoursePackageRequest {\n");
    sb.append("    fileId: ").append(toIndentedString(fileId)).append("\n");
    sb.append("    targetChapterId: ").append(toIndentedString(targetChapterId)).append("\n");
    sb.append("    addToRepository: ").append(toIndentedString(addToRepository)).append("\n");
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
