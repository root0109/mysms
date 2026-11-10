// Owned API contract of comms (JAVA-CODING-RULES §7); first materialized from api/openapi/comms.yaml on 2026-11-10.
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
 * Fields of FeedbackMessage that can be updated (partial update; see updateMask).
 */

public class FeedbackMessageUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID senderUserId;

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
  private @Nullable CategoryEnum category;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String message;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID attachmentFileId;

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
  private @Nullable ReviewStatusEnum reviewStatus;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean deletedBySender;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean deletedByReceiver;

  public FeedbackMessageUpdate senderUserId(@Nullable UUID senderUserId) {
    this.senderUserId = senderUserId;
    return this;
  }

  /**
   * Sender.
   * @return senderUserId
   */
  @Valid 
  @JsonProperty("senderUserId")
  public @Nullable UUID getSenderUserId() {
    return senderUserId;
  }

  @JsonProperty("senderUserId")
  public void setSenderUserId(@Nullable UUID senderUserId) {
    this.senderUserId = senderUserId;
  }

  public FeedbackMessageUpdate category(@Nullable CategoryEnum category) {
    this.category = category;
    return this;
  }

  /**
   * Category.
   * @return category
   */
  
  @JsonProperty("category")
  public @Nullable CategoryEnum getCategory() {
    return category;
  }

  @JsonProperty("category")
  public void setCategory(@Nullable CategoryEnum category) {
    this.category = category;
  }

  public FeedbackMessageUpdate message(@Nullable String message) {
    this.message = message;
    return this;
  }

  /**
   * Message.
   * @return message
   */
  
  @JsonProperty("message")
  public @Nullable String getMessage() {
    return message;
  }

  @JsonProperty("message")
  public void setMessage(@Nullable String message) {
    this.message = message;
  }

  public FeedbackMessageUpdate attachmentFileId(@Nullable UUID attachmentFileId) {
    this.attachmentFileId = attachmentFileId;
    return this;
  }

  /**
   * Attachment.
   * @return attachmentFileId
   */
  @Valid 
  @JsonProperty("attachmentFileId")
  public @Nullable UUID getAttachmentFileId() {
    return attachmentFileId;
  }

  @JsonProperty("attachmentFileId")
  public void setAttachmentFileId(@Nullable UUID attachmentFileId) {
    this.attachmentFileId = attachmentFileId;
  }

  public FeedbackMessageUpdate reviewStatus(@Nullable ReviewStatusEnum reviewStatus) {
    this.reviewStatus = reviewStatus;
    return this;
  }

  /**
   * Handling state.
   * @return reviewStatus
   */
  
  @JsonProperty("reviewStatus")
  public @Nullable ReviewStatusEnum getReviewStatus() {
    return reviewStatus;
  }

  @JsonProperty("reviewStatus")
  public void setReviewStatus(@Nullable ReviewStatusEnum reviewStatus) {
    this.reviewStatus = reviewStatus;
  }

  public FeedbackMessageUpdate deletedBySender(@Nullable Boolean deletedBySender) {
    this.deletedBySender = deletedBySender;
    return this;
  }

  /**
   * Hidden for the sender.
   * @return deletedBySender
   */
  
  @JsonProperty("deletedBySender")
  public @Nullable Boolean getDeletedBySender() {
    return deletedBySender;
  }

  @JsonProperty("deletedBySender")
  public void setDeletedBySender(@Nullable Boolean deletedBySender) {
    this.deletedBySender = deletedBySender;
  }

  public FeedbackMessageUpdate deletedByReceiver(@Nullable Boolean deletedByReceiver) {
    this.deletedByReceiver = deletedByReceiver;
    return this;
  }

  /**
   * Hidden for the receiver.
   * @return deletedByReceiver
   */
  
  @JsonProperty("deletedByReceiver")
  public @Nullable Boolean getDeletedByReceiver() {
    return deletedByReceiver;
  }

  @JsonProperty("deletedByReceiver")
  public void setDeletedByReceiver(@Nullable Boolean deletedByReceiver) {
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
    return Objects.equals(this.senderUserId, feedbackMessageUpdate.senderUserId) &&
        Objects.equals(this.category, feedbackMessageUpdate.category) &&
        Objects.equals(this.message, feedbackMessageUpdate.message) &&
        Objects.equals(this.attachmentFileId, feedbackMessageUpdate.attachmentFileId) &&
        Objects.equals(this.reviewStatus, feedbackMessageUpdate.reviewStatus) &&
        Objects.equals(this.deletedBySender, feedbackMessageUpdate.deletedBySender) &&
        Objects.equals(this.deletedByReceiver, feedbackMessageUpdate.deletedByReceiver);
  }

  @Override
  public int hashCode() {
    return Objects.hash(senderUserId, category, message, attachmentFileId, reviewStatus, deletedBySender, deletedByReceiver);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FeedbackMessageUpdate {\n");
    sb.append("    senderUserId: ").append(toIndentedString(senderUserId)).append("\n");
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

