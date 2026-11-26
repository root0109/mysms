// Owned API contract of comms.
package com.mysms.comms.api.model;

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
 * Fields of FeedbackMessage that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class FeedbackMessageUpdate {

  /**
   * Category.
   */
  public enum CategoryEnum {
    APP_FEEDBACK_OR_BUG("app_feedback_or_bug"),
    
    STUDENT_PROFILE("student_profile"),
    
    COMPLAINT("complaint"),
    
    SUGGESTION("suggestion"),
    
    OTHER("other");

    private final String value;

    CategoryEnum(String value) {
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
    public static CategoryEnum fromValue(String value) {
      for (CategoryEnum b : CategoryEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull CategoryEnum> category;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> message;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> attachmentFileId;

  /**
   * Handling state.
   */
  public enum ReviewStatusEnum {
    NEW("new"),
    
    SEEN("seen"),
    
    ACTION_TAKEN("action_taken");

    private final String value;

    ReviewStatusEnum(String value) {
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
    public static ReviewStatusEnum fromValue(String value) {
      for (ReviewStatusEnum b : ReviewStatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull ReviewStatusEnum> reviewStatus;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> deletedBySender;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> deletedByReceiver;

  public FeedbackMessageUpdate category(@Nullable Optional<CategoryEnum> category) {
    this.category = category;
    return this;
  }

  /**
   * Category.
   * @return category
   */
  
  @JsonProperty("category")
  public @Nullable Optional<CategoryEnum> getCategory() {
    return category;
  }

  @JsonProperty("category")
  public void setCategory(@Nullable Optional<CategoryEnum> category) {
    this.category = category;
  }

  public FeedbackMessageUpdate message(@Nullable Optional<String> message) {
    this.message = message;
    return this;
  }

  /**
   * Message.
   * @return message
   */
  
  @JsonProperty("message")
  public @Nullable Optional<String> getMessage() {
    return message;
  }

  @JsonProperty("message")
  public void setMessage(@Nullable Optional<String> message) {
    this.message = message;
  }

  public FeedbackMessageUpdate attachmentFileId(@Nullable Optional<UUID> attachmentFileId) {
    this.attachmentFileId = attachmentFileId;
    return this;
  }

  /**
   * Attachment.
   * @return attachmentFileId
   */
  
  @JsonProperty("attachmentFileId")
  public @Nullable Optional<UUID> getAttachmentFileId() {
    return attachmentFileId;
  }

  @JsonProperty("attachmentFileId")
  public void setAttachmentFileId(@Nullable Optional<UUID> attachmentFileId) {
    this.attachmentFileId = attachmentFileId;
  }

  public FeedbackMessageUpdate reviewStatus(@Nullable Optional<ReviewStatusEnum> reviewStatus) {
    this.reviewStatus = reviewStatus;
    return this;
  }

  /**
   * Handling state.
   * @return reviewStatus
   */
  
  @JsonProperty("reviewStatus")
  public @Nullable Optional<ReviewStatusEnum> getReviewStatus() {
    return reviewStatus;
  }

  @JsonProperty("reviewStatus")
  public void setReviewStatus(@Nullable Optional<ReviewStatusEnum> reviewStatus) {
    this.reviewStatus = reviewStatus;
  }

  public FeedbackMessageUpdate deletedBySender(@Nullable Optional<Boolean> deletedBySender) {
    this.deletedBySender = deletedBySender;
    return this;
  }

  /**
   * Hidden for the sender.
   * @return deletedBySender
   */
  
  @JsonProperty("deletedBySender")
  public @Nullable Optional<Boolean> getDeletedBySender() {
    return deletedBySender;
  }

  @JsonProperty("deletedBySender")
  public void setDeletedBySender(@Nullable Optional<Boolean> deletedBySender) {
    this.deletedBySender = deletedBySender;
  }

  public FeedbackMessageUpdate deletedByReceiver(@Nullable Optional<Boolean> deletedByReceiver) {
    this.deletedByReceiver = deletedByReceiver;
    return this;
  }

  /**
   * Hidden for the receiver.
   * @return deletedByReceiver
   */
  
  @JsonProperty("deletedByReceiver")
  public @Nullable Optional<Boolean> getDeletedByReceiver() {
    return deletedByReceiver;
  }

  @JsonProperty("deletedByReceiver")
  public void setDeletedByReceiver(@Nullable Optional<Boolean> deletedByReceiver) {
    this.deletedByReceiver = deletedByReceiver;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FeedbackMessageUpdate feedbackMessageUpdate = (FeedbackMessageUpdate) o;
    return Objects.equals(this.category, feedbackMessageUpdate.category) &&
        Objects.equals(this.message, feedbackMessageUpdate.message) &&
        Objects.equals(this.attachmentFileId, feedbackMessageUpdate.attachmentFileId) &&
        Objects.equals(this.reviewStatus, feedbackMessageUpdate.reviewStatus) &&
        Objects.equals(this.deletedBySender, feedbackMessageUpdate.deletedBySender) &&
        Objects.equals(this.deletedByReceiver, feedbackMessageUpdate.deletedByReceiver);
  }

  @Override
  public int hashCode() {
    return Objects.hash(category, message, attachmentFileId, reviewStatus, deletedBySender, deletedByReceiver);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FeedbackMessageUpdate {\n");
    sb.append("    category: ").append(toIndentedString(category)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    attachmentFileId: ").append(toIndentedString(attachmentFileId)).append("\n");
    sb.append("    reviewStatus: ").append(toIndentedString(reviewStatus)).append("\n");
    sb.append("    deletedBySender: ").append(toIndentedString(deletedBySender)).append("\n");
    sb.append("    deletedByReceiver: ").append(toIndentedString(deletedByReceiver)).append("\n");
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

