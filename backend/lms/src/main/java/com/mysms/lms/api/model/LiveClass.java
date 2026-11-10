// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Scheduled online class (Zoom / Google Meet).
 */

public class LiveClass {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate classDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String startTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer durationMinutes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String timezone;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID hostUserId;

  /**
   * Provider.
   */
  public enum ConferencingProviderEnum {
    ZOOM("zoom"),
    
    GOOGLE_MEET("google_meet"),
    
    BIGBLUEBUTTON("bigbluebutton"),
    
    OTHER("other");

    private final String value;

    ConferencingProviderEnum(String value) {
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
    public static ConferencingProviderEnum fromValue(String value) {
      for (ConferencingProviderEnum b : ConferencingProviderEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private ConferencingProviderEnum conferencingProvider;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID conferencingAccountId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String meetingId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String meetingUuid;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String joinUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String meetingPassword;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String meetingPasswordMasked;

  /**
   * Recording mode.
   */
  public enum AutoRecordingEnum {
    NONE("none"),
    
    LOCAL("local"),
    
    CLOUD("cloud");

    private final String value;

    AutoRecordingEnum(String value) {
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
    public static AutoRecordingEnum fromValue(String value) {
      for (AutoRecordingEnum b : AutoRecordingEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable AutoRecordingEnum autoRecording;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID calendarEventId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer attendeeCount;

  /**
   * Calendar-invite delivery state.
   */
  public enum InviteStatusEnum {
    PENDING("pending"),
    
    SENT("sent"),
    
    RETRYING("retrying"),
    
    FAILED("failed");

    private final String value;

    InviteStatusEnum(String value) {
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
    public static InviteStatusEnum fromValue(String value) {
      for (InviteStatusEnum b : InviteStatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable InviteStatusEnum inviteStatus;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer inviteAttempts;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String moderatorPassword;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String moderatorPasswordMasked;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String welcomeMessage;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String providerCreatedRef;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String recordingUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer inviteeCount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer prepMinutes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer extensionMinutes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean showInCalendar;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean hostJoined;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime endedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID createdBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime updateTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID updatedBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  public LiveClass() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public LiveClass(String title, LocalDate classDate, String startTime, Integer durationMinutes, String timezone, UUID hostUserId, ConferencingProviderEnum conferencingProvider) {
    this.title = title;
    this.classDate = classDate;
    this.startTime = startTime;
    this.durationMinutes = durationMinutes;
    this.timezone = timezone;
    this.hostUserId = hostUserId;
    this.conferencingProvider = conferencingProvider;
  }

  public LiveClass id(@Nullable UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback).
   * @return id
   */
  @Valid 
  @JsonProperty("id")
  public @Nullable UUID getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable UUID id) {
    this.id = id;
  }

  public LiveClass cohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort.
   * @return cohortId
   */
  @Valid 
  @JsonProperty("cohortId")
  public @Nullable UUID getCohortId() {
    return cohortId;
  }

  @JsonProperty("cohortId")
  public void setCohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
  }

  public LiveClass title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Title.
   * @return title
   */
  @NotNull @Size(max = 200) 
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(String title) {
    this.title = title;
  }

  public LiveClass description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Description.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public LiveClass classDate(LocalDate classDate) {
    this.classDate = classDate;
    return this;
  }

  /**
   * Date.
   * @return classDate
   */
  @NotNull @Valid 
  @JsonProperty("classDate")
  public LocalDate getClassDate() {
    return classDate;
  }

  @JsonProperty("classDate")
  public void setClassDate(LocalDate classDate) {
    this.classDate = classDate;
  }

  public LiveClass startTime(String startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * Start time.
   * @return startTime
   */
  @NotNull @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") 
  @JsonProperty("startTime")
  public String getStartTime() {
    return startTime;
  }

  @JsonProperty("startTime")
  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public LiveClass durationMinutes(Integer durationMinutes) {
    this.durationMinutes = durationMinutes;
    return this;
  }

  /**
   * Duration.
   * @return durationMinutes
   */
  @NotNull 
  @JsonProperty("durationMinutes")
  public Integer getDurationMinutes() {
    return durationMinutes;
  }

  @JsonProperty("durationMinutes")
  public void setDurationMinutes(Integer durationMinutes) {
    this.durationMinutes = durationMinutes;
  }

  public LiveClass timezone(String timezone) {
    this.timezone = timezone;
    return this;
  }

  /**
   * Time zone.
   * @return timezone
   */
  @NotNull @Size(max = 64) 
  @JsonProperty("timezone")
  public String getTimezone() {
    return timezone;
  }

  @JsonProperty("timezone")
  public void setTimezone(String timezone) {
    this.timezone = timezone;
  }

  public LiveClass hostUserId(UUID hostUserId) {
    this.hostUserId = hostUserId;
    return this;
  }

  /**
   * Host.
   * @return hostUserId
   */
  @NotNull @Valid 
  @JsonProperty("hostUserId")
  public UUID getHostUserId() {
    return hostUserId;
  }

  @JsonProperty("hostUserId")
  public void setHostUserId(UUID hostUserId) {
    this.hostUserId = hostUserId;
  }

  public LiveClass conferencingProvider(ConferencingProviderEnum conferencingProvider) {
    this.conferencingProvider = conferencingProvider;
    return this;
  }

  /**
   * Provider.
   * @return conferencingProvider
   */
  @NotNull 
  @JsonProperty("conferencingProvider")
  public ConferencingProviderEnum getConferencingProvider() {
    return conferencingProvider;
  }

  @JsonProperty("conferencingProvider")
  public void setConferencingProvider(ConferencingProviderEnum conferencingProvider) {
    this.conferencingProvider = conferencingProvider;
  }

  public LiveClass conferencingAccountId(@Nullable UUID conferencingAccountId) {
    this.conferencingAccountId = conferencingAccountId;
    return this;
  }

  /**
   * Account used.
   * @return conferencingAccountId
   */
  @Valid 
  @JsonProperty("conferencingAccountId")
  public @Nullable UUID getConferencingAccountId() {
    return conferencingAccountId;
  }

  @JsonProperty("conferencingAccountId")
  public void setConferencingAccountId(@Nullable UUID conferencingAccountId) {
    this.conferencingAccountId = conferencingAccountId;
  }

  public LiveClass meetingId(@Nullable String meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  /**
   * Provider meeting id.
   * @return meetingId
   */
  @Size(max = 120) 
  @JsonProperty("meetingId")
  public @Nullable String getMeetingId() {
    return meetingId;
  }

  @JsonProperty("meetingId")
  public void setMeetingId(@Nullable String meetingId) {
    this.meetingId = meetingId;
  }

  public LiveClass meetingUuid(@Nullable String meetingUuid) {
    this.meetingUuid = meetingUuid;
    return this;
  }

  /**
   * Provider meeting UUID.
   * @return meetingUuid
   */
  @Size(max = 120) 
  @JsonProperty("meetingUuid")
  public @Nullable String getMeetingUuid() {
    return meetingUuid;
  }

  @JsonProperty("meetingUuid")
  public void setMeetingUuid(@Nullable String meetingUuid) {
    this.meetingUuid = meetingUuid;
  }

  public LiveClass joinUrl(@Nullable String joinUrl) {
    this.joinUrl = joinUrl;
    return this;
  }

  /**
   * Participant join URL.
   * @return joinUrl
   */
  
  @JsonProperty("joinUrl")
  public @Nullable String getJoinUrl() {
    return joinUrl;
  }

  @JsonProperty("joinUrl")
  public void setJoinUrl(@Nullable String joinUrl) {
    this.joinUrl = joinUrl;
  }

  public LiveClass meetingPassword(@Nullable String meetingPassword) {
    this.meetingPassword = meetingPassword;
    return this;
  }

  /**
   * Plain value, input only: stored encrypted and never returned. Meeting password, encrypted.
   * @return meetingPassword
   */
  
  @JsonProperty("meetingPassword")
  public @Nullable String getMeetingPassword() {
    return meetingPassword;
  }

  @JsonProperty("meetingPassword")
  public void setMeetingPassword(@Nullable String meetingPassword) {
    this.meetingPassword = meetingPassword;
  }

  public LiveClass meetingPasswordMasked(@Nullable String meetingPasswordMasked) {
    this.meetingPasswordMasked = meetingPasswordMasked;
    return this;
  }

  /**
   * Masked form for display, e.g. \"••••1234\". Output only.
   * @return meetingPasswordMasked
   */
  
  @JsonProperty("meetingPasswordMasked")
  public @Nullable String getMeetingPasswordMasked() {
    return meetingPasswordMasked;
  }

  @JsonProperty("meetingPasswordMasked")
  public void setMeetingPasswordMasked(@Nullable String meetingPasswordMasked) {
    this.meetingPasswordMasked = meetingPasswordMasked;
  }

  public LiveClass autoRecording(@Nullable AutoRecordingEnum autoRecording) {
    this.autoRecording = autoRecording;
    return this;
  }

  /**
   * Recording mode.
   * @return autoRecording
   */
  
  @JsonProperty("autoRecording")
  public @Nullable AutoRecordingEnum getAutoRecording() {
    return autoRecording;
  }

  @JsonProperty("autoRecording")
  public void setAutoRecording(@Nullable AutoRecordingEnum autoRecording) {
    this.autoRecording = autoRecording;
  }

  public LiveClass calendarEventId(@Nullable UUID calendarEventId) {
    this.calendarEventId = calendarEventId;
    return this;
  }

  /**
   * Calendar event created for the class.
   * @return calendarEventId
   */
  @Valid 
  @JsonProperty("calendarEventId")
  public @Nullable UUID getCalendarEventId() {
    return calendarEventId;
  }

  @JsonProperty("calendarEventId")
  public void setCalendarEventId(@Nullable UUID calendarEventId) {
    this.calendarEventId = calendarEventId;
  }

  public LiveClass attendeeCount(@Nullable Integer attendeeCount) {
    this.attendeeCount = attendeeCount;
    return this;
  }

  /**
   * Participants who joined.
   * @return attendeeCount
   */
  
  @JsonProperty("attendeeCount")
  public @Nullable Integer getAttendeeCount() {
    return attendeeCount;
  }

  @JsonProperty("attendeeCount")
  public void setAttendeeCount(@Nullable Integer attendeeCount) {
    this.attendeeCount = attendeeCount;
  }

  public LiveClass inviteStatus(@Nullable InviteStatusEnum inviteStatus) {
    this.inviteStatus = inviteStatus;
    return this;
  }

  /**
   * Calendar-invite delivery state.
   * @return inviteStatus
   */
  
  @JsonProperty("inviteStatus")
  public @Nullable InviteStatusEnum getInviteStatus() {
    return inviteStatus;
  }

  @JsonProperty("inviteStatus")
  public void setInviteStatus(@Nullable InviteStatusEnum inviteStatus) {
    this.inviteStatus = inviteStatus;
  }

  public LiveClass inviteAttempts(@Nullable Integer inviteAttempts) {
    this.inviteAttempts = inviteAttempts;
    return this;
  }

  /**
   * Invite delivery attempts.
   * @return inviteAttempts
   */
  
  @JsonProperty("inviteAttempts")
  public @Nullable Integer getInviteAttempts() {
    return inviteAttempts;
  }

  @JsonProperty("inviteAttempts")
  public void setInviteAttempts(@Nullable Integer inviteAttempts) {
    this.inviteAttempts = inviteAttempts;
  }

  public LiveClass moderatorPassword(@Nullable String moderatorPassword) {
    this.moderatorPassword = moderatorPassword;
    return this;
  }

  /**
   * Plain value, input only: stored encrypted and never returned. Moderator password (BigBlueButton), encrypted by the application.
   * @return moderatorPassword
   */
  
  @JsonProperty("moderatorPassword")
  public @Nullable String getModeratorPassword() {
    return moderatorPassword;
  }

  @JsonProperty("moderatorPassword")
  public void setModeratorPassword(@Nullable String moderatorPassword) {
    this.moderatorPassword = moderatorPassword;
  }

  public LiveClass moderatorPasswordMasked(@Nullable String moderatorPasswordMasked) {
    this.moderatorPasswordMasked = moderatorPasswordMasked;
    return this;
  }

  /**
   * Masked form for display, e.g. \"••••1234\". Output only.
   * @return moderatorPasswordMasked
   */
  
  @JsonProperty("moderatorPasswordMasked")
  public @Nullable String getModeratorPasswordMasked() {
    return moderatorPasswordMasked;
  }

  @JsonProperty("moderatorPasswordMasked")
  public void setModeratorPasswordMasked(@Nullable String moderatorPasswordMasked) {
    this.moderatorPasswordMasked = moderatorPasswordMasked;
  }

  public LiveClass welcomeMessage(@Nullable String welcomeMessage) {
    this.welcomeMessage = welcomeMessage;
    return this;
  }

  /**
   * Welcome message shown in the room.
   * @return welcomeMessage
   */
  
  @JsonProperty("welcomeMessage")
  public @Nullable String getWelcomeMessage() {
    return welcomeMessage;
  }

  @JsonProperty("welcomeMessage")
  public void setWelcomeMessage(@Nullable String welcomeMessage) {
    this.welcomeMessage = welcomeMessage;
  }

  public LiveClass providerCreatedRef(@Nullable String providerCreatedRef) {
    this.providerCreatedRef = providerCreatedRef;
    return this;
  }

  /**
   * Provider creation timestamp / token used to re-join the same room.
   * @return providerCreatedRef
   */
  @Size(max = 50) 
  @JsonProperty("providerCreatedRef")
  public @Nullable String getProviderCreatedRef() {
    return providerCreatedRef;
  }

  @JsonProperty("providerCreatedRef")
  public void setProviderCreatedRef(@Nullable String providerCreatedRef) {
    this.providerCreatedRef = providerCreatedRef;
  }

  public LiveClass recordingUrl(@Nullable String recordingUrl) {
    this.recordingUrl = recordingUrl;
    return this;
  }

  /**
   * Recording link.
   * @return recordingUrl
   */
  @Size(max = 1000) 
  @JsonProperty("recordingUrl")
  public @Nullable String getRecordingUrl() {
    return recordingUrl;
  }

  @JsonProperty("recordingUrl")
  public void setRecordingUrl(@Nullable String recordingUrl) {
    this.recordingUrl = recordingUrl;
  }

  public LiveClass inviteeCount(@Nullable Integer inviteeCount) {
    this.inviteeCount = inviteeCount;
    return this;
  }

  /**
   * Students invited (when not the whole cohort).
   * @return inviteeCount
   */
  
  @JsonProperty("inviteeCount")
  public @Nullable Integer getInviteeCount() {
    return inviteeCount;
  }

  @JsonProperty("inviteeCount")
  public void setInviteeCount(@Nullable Integer inviteeCount) {
    this.inviteeCount = inviteeCount;
  }

  public LiveClass prepMinutes(@Nullable Integer prepMinutes) {
    this.prepMinutes = prepMinutes;
    return this;
  }

  /**
   * Minutes before start the host can enter.
   * @return prepMinutes
   */
  
  @JsonProperty("prepMinutes")
  public @Nullable Integer getPrepMinutes() {
    return prepMinutes;
  }

  @JsonProperty("prepMinutes")
  public void setPrepMinutes(@Nullable Integer prepMinutes) {
    this.prepMinutes = prepMinutes;
  }

  public LiveClass extensionMinutes(@Nullable Integer extensionMinutes) {
    this.extensionMinutes = extensionMinutes;
    return this;
  }

  /**
   * Minutes the session can be extended.
   * @return extensionMinutes
   */
  
  @JsonProperty("extensionMinutes")
  public @Nullable Integer getExtensionMinutes() {
    return extensionMinutes;
  }

  @JsonProperty("extensionMinutes")
  public void setExtensionMinutes(@Nullable Integer extensionMinutes) {
    this.extensionMinutes = extensionMinutes;
  }

  public LiveClass showInCalendar(@Nullable Boolean showInCalendar) {
    this.showInCalendar = showInCalendar;
    return this;
  }

  /**
   * Shown in the cohort calendar.
   * @return showInCalendar
   */
  
  @JsonProperty("showInCalendar")
  public @Nullable Boolean getShowInCalendar() {
    return showInCalendar;
  }

  @JsonProperty("showInCalendar")
  public void setShowInCalendar(@Nullable Boolean showInCalendar) {
    this.showInCalendar = showInCalendar;
  }

  public LiveClass hostJoined(@Nullable Boolean hostJoined) {
    this.hostJoined = hostJoined;
    return this;
  }

  /**
   * Host has joined.
   * @return hostJoined
   */
  
  @JsonProperty("hostJoined")
  public @Nullable Boolean getHostJoined() {
    return hostJoined;
  }

  @JsonProperty("hostJoined")
  public void setHostJoined(@Nullable Boolean hostJoined) {
    this.hostJoined = hostJoined;
  }

  public LiveClass isActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Session active (not cancelled).
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Boolean getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
  }

  public LiveClass endedAt(@Nullable OffsetDateTime endedAt) {
    this.endedAt = endedAt;
    return this;
  }

  /**
   * Session ended.
   * @return endedAt
   */
  @Valid 
  @JsonProperty("endedAt")
  public @Nullable OffsetDateTime getEndedAt() {
    return endedAt;
  }

  @JsonProperty("endedAt")
  public void setEndedAt(@Nullable OffsetDateTime endedAt) {
    this.endedAt = endedAt;
  }

  public LiveClass createTime(@Nullable OffsetDateTime createTime) {
    this.createTime = createTime;
    return this;
  }

  /**
   * Row creation time.
   * @return createTime
   */
  @Valid 
  @JsonProperty("createTime")
  public @Nullable OffsetDateTime getCreateTime() {
    return createTime;
  }

  @JsonProperty("createTime")
  public void setCreateTime(@Nullable OffsetDateTime createTime) {
    this.createTime = createTime;
  }

  public LiveClass createdBy(@Nullable UUID createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * User account that created the row (audit reference to iam.user_account; not FK-constrained).
   * @return createdBy
   */
  @Valid 
  @JsonProperty("createdBy")
  public @Nullable UUID getCreatedBy() {
    return createdBy;
  }

  @JsonProperty("createdBy")
  public void setCreatedBy(@Nullable UUID createdBy) {
    this.createdBy = createdBy;
  }

  public LiveClass updateTime(@Nullable OffsetDateTime updateTime) {
    this.updateTime = updateTime;
    return this;
  }

  /**
   * Last update time.
   * @return updateTime
   */
  @Valid 
  @JsonProperty("updateTime")
  public @Nullable OffsetDateTime getUpdateTime() {
    return updateTime;
  }

  @JsonProperty("updateTime")
  public void setUpdateTime(@Nullable OffsetDateTime updateTime) {
    this.updateTime = updateTime;
  }

  public LiveClass updatedBy(@Nullable UUID updatedBy) {
    this.updatedBy = updatedBy;
    return this;
  }

  /**
   * User account that last updated the row (audit reference to iam.user_account; not FK-constrained).
   * @return updatedBy
   */
  @Valid 
  @JsonProperty("updatedBy")
  public @Nullable UUID getUpdatedBy() {
    return updatedBy;
  }

  @JsonProperty("updatedBy")
  public void setUpdatedBy(@Nullable UUID updatedBy) {
    this.updatedBy = updatedBy;
  }

  public LiveClass etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Entity tag (row version); send back in If-Match / etag.
   * @return etag
   */
  
  @JsonProperty("etag")
  public @Nullable String getEtag() {
    return etag;
  }

  @JsonProperty("etag")
  public void setEtag(@Nullable String etag) {
    this.etag = etag;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LiveClass liveClass = (LiveClass) o;
    return Objects.equals(this.id, liveClass.id) &&
        Objects.equals(this.cohortId, liveClass.cohortId) &&
        Objects.equals(this.title, liveClass.title) &&
        Objects.equals(this.description, liveClass.description) &&
        Objects.equals(this.classDate, liveClass.classDate) &&
        Objects.equals(this.startTime, liveClass.startTime) &&
        Objects.equals(this.durationMinutes, liveClass.durationMinutes) &&
        Objects.equals(this.timezone, liveClass.timezone) &&
        Objects.equals(this.hostUserId, liveClass.hostUserId) &&
        Objects.equals(this.conferencingProvider, liveClass.conferencingProvider) &&
        Objects.equals(this.conferencingAccountId, liveClass.conferencingAccountId) &&
        Objects.equals(this.meetingId, liveClass.meetingId) &&
        Objects.equals(this.meetingUuid, liveClass.meetingUuid) &&
        Objects.equals(this.joinUrl, liveClass.joinUrl) &&
        Objects.equals(this.meetingPassword, liveClass.meetingPassword) &&
        Objects.equals(this.meetingPasswordMasked, liveClass.meetingPasswordMasked) &&
        Objects.equals(this.autoRecording, liveClass.autoRecording) &&
        Objects.equals(this.calendarEventId, liveClass.calendarEventId) &&
        Objects.equals(this.attendeeCount, liveClass.attendeeCount) &&
        Objects.equals(this.inviteStatus, liveClass.inviteStatus) &&
        Objects.equals(this.inviteAttempts, liveClass.inviteAttempts) &&
        Objects.equals(this.moderatorPassword, liveClass.moderatorPassword) &&
        Objects.equals(this.moderatorPasswordMasked, liveClass.moderatorPasswordMasked) &&
        Objects.equals(this.welcomeMessage, liveClass.welcomeMessage) &&
        Objects.equals(this.providerCreatedRef, liveClass.providerCreatedRef) &&
        Objects.equals(this.recordingUrl, liveClass.recordingUrl) &&
        Objects.equals(this.inviteeCount, liveClass.inviteeCount) &&
        Objects.equals(this.prepMinutes, liveClass.prepMinutes) &&
        Objects.equals(this.extensionMinutes, liveClass.extensionMinutes) &&
        Objects.equals(this.showInCalendar, liveClass.showInCalendar) &&
        Objects.equals(this.hostJoined, liveClass.hostJoined) &&
        Objects.equals(this.isActive, liveClass.isActive) &&
        Objects.equals(this.endedAt, liveClass.endedAt) &&
        Objects.equals(this.createTime, liveClass.createTime) &&
        Objects.equals(this.createdBy, liveClass.createdBy) &&
        Objects.equals(this.updateTime, liveClass.updateTime) &&
        Objects.equals(this.updatedBy, liveClass.updatedBy) &&
        Objects.equals(this.etag, liveClass.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, cohortId, title, description, classDate, startTime, durationMinutes, timezone, hostUserId, conferencingProvider, conferencingAccountId, meetingId, meetingUuid, joinUrl, meetingPassword, meetingPasswordMasked, autoRecording, calendarEventId, attendeeCount, inviteStatus, inviteAttempts, moderatorPassword, moderatorPasswordMasked, welcomeMessage, providerCreatedRef, recordingUrl, inviteeCount, prepMinutes, extensionMinutes, showInCalendar, hostJoined, isActive, endedAt, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LiveClass {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    classDate: ").append(toIndentedString(classDate)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    durationMinutes: ").append(toIndentedString(durationMinutes)).append("\n");
    sb.append("    timezone: ").append(toIndentedString(timezone)).append("\n");
    sb.append("    hostUserId: ").append(toIndentedString(hostUserId)).append("\n");
    sb.append("    conferencingProvider: ").append(toIndentedString(conferencingProvider)).append("\n");
    sb.append("    conferencingAccountId: ").append(toIndentedString(conferencingAccountId)).append("\n");
    sb.append("    meetingId: ").append(toIndentedString(meetingId)).append("\n");
    sb.append("    meetingUuid: ").append(toIndentedString(meetingUuid)).append("\n");
    sb.append("    joinUrl: ").append(toIndentedString(joinUrl)).append("\n");
    sb.append("    meetingPassword: ").append(toIndentedString(meetingPassword)).append("\n");
    sb.append("    meetingPasswordMasked: ").append(toIndentedString(meetingPasswordMasked)).append("\n");
    sb.append("    autoRecording: ").append(toIndentedString(autoRecording)).append("\n");
    sb.append("    calendarEventId: ").append(toIndentedString(calendarEventId)).append("\n");
    sb.append("    attendeeCount: ").append(toIndentedString(attendeeCount)).append("\n");
    sb.append("    inviteStatus: ").append(toIndentedString(inviteStatus)).append("\n");
    sb.append("    inviteAttempts: ").append(toIndentedString(inviteAttempts)).append("\n");
    sb.append("    moderatorPassword: ").append(toIndentedString(moderatorPassword)).append("\n");
    sb.append("    moderatorPasswordMasked: ").append(toIndentedString(moderatorPasswordMasked)).append("\n");
    sb.append("    welcomeMessage: ").append(toIndentedString(welcomeMessage)).append("\n");
    sb.append("    providerCreatedRef: ").append(toIndentedString(providerCreatedRef)).append("\n");
    sb.append("    recordingUrl: ").append(toIndentedString(recordingUrl)).append("\n");
    sb.append("    inviteeCount: ").append(toIndentedString(inviteeCount)).append("\n");
    sb.append("    prepMinutes: ").append(toIndentedString(prepMinutes)).append("\n");
    sb.append("    extensionMinutes: ").append(toIndentedString(extensionMinutes)).append("\n");
    sb.append("    showInCalendar: ").append(toIndentedString(showInCalendar)).append("\n");
    sb.append("    hostJoined: ").append(toIndentedString(hostJoined)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
    sb.append("    endedAt: ").append(toIndentedString(endedAt)).append("\n");
    sb.append("    createTime: ").append(toIndentedString(createTime)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    updateTime: ").append(toIndentedString(updateTime)).append("\n");
    sb.append("    updatedBy: ").append(toIndentedString(updatedBy)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
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

