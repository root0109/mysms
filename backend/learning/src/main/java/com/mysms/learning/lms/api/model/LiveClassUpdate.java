// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of LiveClass that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class LiveClassUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<@NotNull LocalDate> classDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") String> startTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> durationMinutes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 64) String> timezone;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> hostUserId;

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
  private @Nullable Optional<@NotNull ConferencingProviderEnum> conferencingProvider;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> conferencingAccountId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 120) String> meetingId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 120) String> meetingUuid;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> joinUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> meetingPassword;

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
  private @Nullable Optional<@NotNull AutoRecordingEnum> autoRecording;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> calendarEventId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> attendeeCount;

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
  private @Nullable Optional<@NotNull InviteStatusEnum> inviteStatus;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> inviteAttempts;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> moderatorPassword;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> welcomeMessage;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 50) String> providerCreatedRef;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 1000) String> recordingUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> inviteeCount;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> prepMinutes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> extensionMinutes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> showInCalendar;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> hostJoined;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> endedAt;

  public LiveClassUpdate cohortId(@Nullable Optional<UUID> cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort.
   * @return cohortId
   */
  
  @JsonProperty("cohortId")
  public @Nullable Optional<UUID> getCohortId() {
    return cohortId;
  }

  @JsonProperty("cohortId")
  public void setCohortId(@Nullable Optional<UUID> cohortId) {
    this.cohortId = cohortId;
  }

  public LiveClassUpdate title(@Nullable Optional<String> title) {
    this.title = title;
    return this;
  }

  /**
   * Title.
   * @return title
   */
  
  @JsonProperty("title")
  public @Nullable Optional<String> getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable Optional<String> title) {
    this.title = title;
  }

  public LiveClassUpdate description(@Nullable Optional<String> description) {
    this.description = description;
    return this;
  }

  /**
   * Description.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable Optional<String> getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable Optional<String> description) {
    this.description = description;
  }

  public LiveClassUpdate classDate(@Nullable Optional<LocalDate> classDate) {
    this.classDate = classDate;
    return this;
  }

  /**
   * Date.
   * @return classDate
   */
  
  @JsonProperty("classDate")
  public @Nullable Optional<LocalDate> getClassDate() {
    return classDate;
  }

  @JsonProperty("classDate")
  public void setClassDate(@Nullable Optional<LocalDate> classDate) {
    this.classDate = classDate;
  }

  public LiveClassUpdate startTime(@Nullable Optional<String> startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * Start time.
   * @return startTime
   */
  
  @JsonProperty("startTime")
  public @Nullable Optional<String> getStartTime() {
    return startTime;
  }

  @JsonProperty("startTime")
  public void setStartTime(@Nullable Optional<String> startTime) {
    this.startTime = startTime;
  }

  public LiveClassUpdate durationMinutes(@Nullable Optional<Integer> durationMinutes) {
    this.durationMinutes = durationMinutes;
    return this;
  }

  /**
   * Duration.
   * @return durationMinutes
   */
  
  @JsonProperty("durationMinutes")
  public @Nullable Optional<Integer> getDurationMinutes() {
    return durationMinutes;
  }

  @JsonProperty("durationMinutes")
  public void setDurationMinutes(@Nullable Optional<Integer> durationMinutes) {
    this.durationMinutes = durationMinutes;
  }

  public LiveClassUpdate timezone(@Nullable Optional<String> timezone) {
    this.timezone = timezone;
    return this;
  }

  /**
   * Time zone.
   * @return timezone
   */
  
  @JsonProperty("timezone")
  public @Nullable Optional<String> getTimezone() {
    return timezone;
  }

  @JsonProperty("timezone")
  public void setTimezone(@Nullable Optional<String> timezone) {
    this.timezone = timezone;
  }

  public LiveClassUpdate hostUserId(@Nullable Optional<UUID> hostUserId) {
    this.hostUserId = hostUserId;
    return this;
  }

  /**
   * Host.
   * @return hostUserId
   */
  
  @JsonProperty("hostUserId")
  public @Nullable Optional<UUID> getHostUserId() {
    return hostUserId;
  }

  @JsonProperty("hostUserId")
  public void setHostUserId(@Nullable Optional<UUID> hostUserId) {
    this.hostUserId = hostUserId;
  }

  public LiveClassUpdate conferencingProvider(@Nullable Optional<ConferencingProviderEnum> conferencingProvider) {
    this.conferencingProvider = conferencingProvider;
    return this;
  }

  /**
   * Provider.
   * @return conferencingProvider
   */
  
  @JsonProperty("conferencingProvider")
  public @Nullable Optional<ConferencingProviderEnum> getConferencingProvider() {
    return conferencingProvider;
  }

  @JsonProperty("conferencingProvider")
  public void setConferencingProvider(@Nullable Optional<ConferencingProviderEnum> conferencingProvider) {
    this.conferencingProvider = conferencingProvider;
  }

  public LiveClassUpdate conferencingAccountId(@Nullable Optional<UUID> conferencingAccountId) {
    this.conferencingAccountId = conferencingAccountId;
    return this;
  }

  /**
   * Account used.
   * @return conferencingAccountId
   */
  
  @JsonProperty("conferencingAccountId")
  public @Nullable Optional<UUID> getConferencingAccountId() {
    return conferencingAccountId;
  }

  @JsonProperty("conferencingAccountId")
  public void setConferencingAccountId(@Nullable Optional<UUID> conferencingAccountId) {
    this.conferencingAccountId = conferencingAccountId;
  }

  public LiveClassUpdate meetingId(@Nullable Optional<String> meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  /**
   * Provider meeting id.
   * @return meetingId
   */
  
  @JsonProperty("meetingId")
  public @Nullable Optional<String> getMeetingId() {
    return meetingId;
  }

  @JsonProperty("meetingId")
  public void setMeetingId(@Nullable Optional<String> meetingId) {
    this.meetingId = meetingId;
  }

  public LiveClassUpdate meetingUuid(@Nullable Optional<String> meetingUuid) {
    this.meetingUuid = meetingUuid;
    return this;
  }

  /**
   * Provider meeting UUID.
   * @return meetingUuid
   */
  
  @JsonProperty("meetingUuid")
  public @Nullable Optional<String> getMeetingUuid() {
    return meetingUuid;
  }

  @JsonProperty("meetingUuid")
  public void setMeetingUuid(@Nullable Optional<String> meetingUuid) {
    this.meetingUuid = meetingUuid;
  }

  public LiveClassUpdate joinUrl(@Nullable Optional<String> joinUrl) {
    this.joinUrl = joinUrl;
    return this;
  }

  /**
   * Participant join URL.
   * @return joinUrl
   */
  
  @JsonProperty("joinUrl")
  public @Nullable Optional<String> getJoinUrl() {
    return joinUrl;
  }

  @JsonProperty("joinUrl")
  public void setJoinUrl(@Nullable Optional<String> joinUrl) {
    this.joinUrl = joinUrl;
  }

  public LiveClassUpdate meetingPassword(@Nullable Optional<String> meetingPassword) {
    this.meetingPassword = meetingPassword;
    return this;
  }

  /**
   * Plain value, input only: stored encrypted and never returned. Meeting password, encrypted.
   * @return meetingPassword
   */
  
  @JsonProperty("meetingPassword")
  public @Nullable Optional<String> getMeetingPassword() {
    return meetingPassword;
  }

  @JsonProperty("meetingPassword")
  public void setMeetingPassword(@Nullable Optional<String> meetingPassword) {
    this.meetingPassword = meetingPassword;
  }

  public LiveClassUpdate autoRecording(@Nullable Optional<AutoRecordingEnum> autoRecording) {
    this.autoRecording = autoRecording;
    return this;
  }

  /**
   * Recording mode.
   * @return autoRecording
   */
  
  @JsonProperty("autoRecording")
  public @Nullable Optional<AutoRecordingEnum> getAutoRecording() {
    return autoRecording;
  }

  @JsonProperty("autoRecording")
  public void setAutoRecording(@Nullable Optional<AutoRecordingEnum> autoRecording) {
    this.autoRecording = autoRecording;
  }

  public LiveClassUpdate calendarEventId(@Nullable Optional<UUID> calendarEventId) {
    this.calendarEventId = calendarEventId;
    return this;
  }

  /**
   * Calendar event created for the class.
   * @return calendarEventId
   */
  
  @JsonProperty("calendarEventId")
  public @Nullable Optional<UUID> getCalendarEventId() {
    return calendarEventId;
  }

  @JsonProperty("calendarEventId")
  public void setCalendarEventId(@Nullable Optional<UUID> calendarEventId) {
    this.calendarEventId = calendarEventId;
  }

  public LiveClassUpdate attendeeCount(@Nullable Optional<Integer> attendeeCount) {
    this.attendeeCount = attendeeCount;
    return this;
  }

  /**
   * Participants who joined.
   * @return attendeeCount
   */
  
  @JsonProperty("attendeeCount")
  public @Nullable Optional<Integer> getAttendeeCount() {
    return attendeeCount;
  }

  @JsonProperty("attendeeCount")
  public void setAttendeeCount(@Nullable Optional<Integer> attendeeCount) {
    this.attendeeCount = attendeeCount;
  }

  public LiveClassUpdate inviteStatus(@Nullable Optional<InviteStatusEnum> inviteStatus) {
    this.inviteStatus = inviteStatus;
    return this;
  }

  /**
   * Calendar-invite delivery state.
   * @return inviteStatus
   */
  
  @JsonProperty("inviteStatus")
  public @Nullable Optional<InviteStatusEnum> getInviteStatus() {
    return inviteStatus;
  }

  @JsonProperty("inviteStatus")
  public void setInviteStatus(@Nullable Optional<InviteStatusEnum> inviteStatus) {
    this.inviteStatus = inviteStatus;
  }

  public LiveClassUpdate inviteAttempts(@Nullable Optional<Integer> inviteAttempts) {
    this.inviteAttempts = inviteAttempts;
    return this;
  }

  /**
   * Invite delivery attempts.
   * @return inviteAttempts
   */
  
  @JsonProperty("inviteAttempts")
  public @Nullable Optional<Integer> getInviteAttempts() {
    return inviteAttempts;
  }

  @JsonProperty("inviteAttempts")
  public void setInviteAttempts(@Nullable Optional<Integer> inviteAttempts) {
    this.inviteAttempts = inviteAttempts;
  }

  public LiveClassUpdate moderatorPassword(@Nullable Optional<String> moderatorPassword) {
    this.moderatorPassword = moderatorPassword;
    return this;
  }

  /**
   * Plain value, input only: stored encrypted and never returned. Moderator password (BigBlueButton), encrypted by the application.
   * @return moderatorPassword
   */
  
  @JsonProperty("moderatorPassword")
  public @Nullable Optional<String> getModeratorPassword() {
    return moderatorPassword;
  }

  @JsonProperty("moderatorPassword")
  public void setModeratorPassword(@Nullable Optional<String> moderatorPassword) {
    this.moderatorPassword = moderatorPassword;
  }

  public LiveClassUpdate welcomeMessage(@Nullable Optional<String> welcomeMessage) {
    this.welcomeMessage = welcomeMessage;
    return this;
  }

  /**
   * Welcome message shown in the room.
   * @return welcomeMessage
   */
  
  @JsonProperty("welcomeMessage")
  public @Nullable Optional<String> getWelcomeMessage() {
    return welcomeMessage;
  }

  @JsonProperty("welcomeMessage")
  public void setWelcomeMessage(@Nullable Optional<String> welcomeMessage) {
    this.welcomeMessage = welcomeMessage;
  }

  public LiveClassUpdate providerCreatedRef(@Nullable Optional<String> providerCreatedRef) {
    this.providerCreatedRef = providerCreatedRef;
    return this;
  }

  /**
   * Provider creation timestamp / token used to re-join the same room.
   * @return providerCreatedRef
   */
  
  @JsonProperty("providerCreatedRef")
  public @Nullable Optional<String> getProviderCreatedRef() {
    return providerCreatedRef;
  }

  @JsonProperty("providerCreatedRef")
  public void setProviderCreatedRef(@Nullable Optional<String> providerCreatedRef) {
    this.providerCreatedRef = providerCreatedRef;
  }

  public LiveClassUpdate recordingUrl(@Nullable Optional<String> recordingUrl) {
    this.recordingUrl = recordingUrl;
    return this;
  }

  /**
   * Recording link.
   * @return recordingUrl
   */
  
  @JsonProperty("recordingUrl")
  public @Nullable Optional<String> getRecordingUrl() {
    return recordingUrl;
  }

  @JsonProperty("recordingUrl")
  public void setRecordingUrl(@Nullable Optional<String> recordingUrl) {
    this.recordingUrl = recordingUrl;
  }

  public LiveClassUpdate inviteeCount(@Nullable Optional<Integer> inviteeCount) {
    this.inviteeCount = inviteeCount;
    return this;
  }

  /**
   * Students invited (when not the whole cohort).
   * @return inviteeCount
   */
  
  @JsonProperty("inviteeCount")
  public @Nullable Optional<Integer> getInviteeCount() {
    return inviteeCount;
  }

  @JsonProperty("inviteeCount")
  public void setInviteeCount(@Nullable Optional<Integer> inviteeCount) {
    this.inviteeCount = inviteeCount;
  }

  public LiveClassUpdate prepMinutes(@Nullable Optional<Integer> prepMinutes) {
    this.prepMinutes = prepMinutes;
    return this;
  }

  /**
   * Minutes before start the host can enter.
   * @return prepMinutes
   */
  
  @JsonProperty("prepMinutes")
  public @Nullable Optional<Integer> getPrepMinutes() {
    return prepMinutes;
  }

  @JsonProperty("prepMinutes")
  public void setPrepMinutes(@Nullable Optional<Integer> prepMinutes) {
    this.prepMinutes = prepMinutes;
  }

  public LiveClassUpdate extensionMinutes(@Nullable Optional<Integer> extensionMinutes) {
    this.extensionMinutes = extensionMinutes;
    return this;
  }

  /**
   * Minutes the session can be extended.
   * @return extensionMinutes
   */
  
  @JsonProperty("extensionMinutes")
  public @Nullable Optional<Integer> getExtensionMinutes() {
    return extensionMinutes;
  }

  @JsonProperty("extensionMinutes")
  public void setExtensionMinutes(@Nullable Optional<Integer> extensionMinutes) {
    this.extensionMinutes = extensionMinutes;
  }

  public LiveClassUpdate showInCalendar(@Nullable Optional<Boolean> showInCalendar) {
    this.showInCalendar = showInCalendar;
    return this;
  }

  /**
   * Shown in the cohort calendar.
   * @return showInCalendar
   */
  
  @JsonProperty("showInCalendar")
  public @Nullable Optional<Boolean> getShowInCalendar() {
    return showInCalendar;
  }

  @JsonProperty("showInCalendar")
  public void setShowInCalendar(@Nullable Optional<Boolean> showInCalendar) {
    this.showInCalendar = showInCalendar;
  }

  public LiveClassUpdate hostJoined(@Nullable Optional<Boolean> hostJoined) {
    this.hostJoined = hostJoined;
    return this;
  }

  /**
   * Host has joined.
   * @return hostJoined
   */
  
  @JsonProperty("hostJoined")
  public @Nullable Optional<Boolean> getHostJoined() {
    return hostJoined;
  }

  @JsonProperty("hostJoined")
  public void setHostJoined(@Nullable Optional<Boolean> hostJoined) {
    this.hostJoined = hostJoined;
  }

  public LiveClassUpdate isActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Session active (not cancelled).
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Optional<Boolean> getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
  }

  public LiveClassUpdate endedAt(@Nullable Optional<OffsetDateTime> endedAt) {
    this.endedAt = endedAt;
    return this;
  }

  /**
   * Session ended.
   * @return endedAt
   */
  
  @JsonProperty("endedAt")
  public @Nullable Optional<OffsetDateTime> getEndedAt() {
    return endedAt;
  }

  @JsonProperty("endedAt")
  public void setEndedAt(@Nullable Optional<OffsetDateTime> endedAt) {
    this.endedAt = endedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LiveClassUpdate liveClassUpdate = (LiveClassUpdate) o;
    return Objects.equals(this.cohortId, liveClassUpdate.cohortId) &&
        Objects.equals(this.title, liveClassUpdate.title) &&
        Objects.equals(this.description, liveClassUpdate.description) &&
        Objects.equals(this.classDate, liveClassUpdate.classDate) &&
        Objects.equals(this.startTime, liveClassUpdate.startTime) &&
        Objects.equals(this.durationMinutes, liveClassUpdate.durationMinutes) &&
        Objects.equals(this.timezone, liveClassUpdate.timezone) &&
        Objects.equals(this.hostUserId, liveClassUpdate.hostUserId) &&
        Objects.equals(this.conferencingProvider, liveClassUpdate.conferencingProvider) &&
        Objects.equals(this.conferencingAccountId, liveClassUpdate.conferencingAccountId) &&
        Objects.equals(this.meetingId, liveClassUpdate.meetingId) &&
        Objects.equals(this.meetingUuid, liveClassUpdate.meetingUuid) &&
        Objects.equals(this.joinUrl, liveClassUpdate.joinUrl) &&
        Objects.equals(this.meetingPassword, liveClassUpdate.meetingPassword) &&
        Objects.equals(this.autoRecording, liveClassUpdate.autoRecording) &&
        Objects.equals(this.calendarEventId, liveClassUpdate.calendarEventId) &&
        Objects.equals(this.attendeeCount, liveClassUpdate.attendeeCount) &&
        Objects.equals(this.inviteStatus, liveClassUpdate.inviteStatus) &&
        Objects.equals(this.inviteAttempts, liveClassUpdate.inviteAttempts) &&
        Objects.equals(this.moderatorPassword, liveClassUpdate.moderatorPassword) &&
        Objects.equals(this.welcomeMessage, liveClassUpdate.welcomeMessage) &&
        Objects.equals(this.providerCreatedRef, liveClassUpdate.providerCreatedRef) &&
        Objects.equals(this.recordingUrl, liveClassUpdate.recordingUrl) &&
        Objects.equals(this.inviteeCount, liveClassUpdate.inviteeCount) &&
        Objects.equals(this.prepMinutes, liveClassUpdate.prepMinutes) &&
        Objects.equals(this.extensionMinutes, liveClassUpdate.extensionMinutes) &&
        Objects.equals(this.showInCalendar, liveClassUpdate.showInCalendar) &&
        Objects.equals(this.hostJoined, liveClassUpdate.hostJoined) &&
        Objects.equals(this.isActive, liveClassUpdate.isActive) &&
        Objects.equals(this.endedAt, liveClassUpdate.endedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cohortId, title, description, classDate, startTime, durationMinutes, timezone, hostUserId, conferencingProvider, conferencingAccountId, meetingId, meetingUuid, joinUrl, meetingPassword, autoRecording, calendarEventId, attendeeCount, inviteStatus, inviteAttempts, moderatorPassword, welcomeMessage, providerCreatedRef, recordingUrl, inviteeCount, prepMinutes, extensionMinutes, showInCalendar, hostJoined, isActive, endedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LiveClassUpdate {\n");
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
    sb.append("    autoRecording: ").append(toIndentedString(autoRecording)).append("\n");
    sb.append("    calendarEventId: ").append(toIndentedString(calendarEventId)).append("\n");
    sb.append("    attendeeCount: ").append(toIndentedString(attendeeCount)).append("\n");
    sb.append("    inviteStatus: ").append(toIndentedString(inviteStatus)).append("\n");
    sb.append("    inviteAttempts: ").append(toIndentedString(inviteAttempts)).append("\n");
    sb.append("    moderatorPassword: ").append(toIndentedString(moderatorPassword)).append("\n");
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

