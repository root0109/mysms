package com.mysms.comms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.business.NotificationTypeService;
import com.mysms.comms.api.NotificationTypeApi;
import com.mysms.comms.api.model.NotificationCohortSetting;
import com.mysms.comms.api.model.NotificationCohortSettingUpdate;
import com.mysms.comms.api.model.NotificationRule;
import com.mysms.comms.api.model.NotificationRuleUpdate;
import com.mysms.comms.api.model.NotificationType;
import com.mysms.comms.api.model.NotificationTypeProperty;
import com.mysms.comms.api.model.NotificationTypePropertyUpdate;
import com.mysms.comms.api.model.NotificationTypeUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the NotificationType operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class NotificationTypeController implements NotificationTypeApi {

    private final NotificationTypeService notificationTypeService;

    public NotificationTypeController(NotificationTypeService notificationTypeService) {
        this.notificationTypeService = notificationTypeService;
    }

    @Override
    public ResponseEntity<NotificationCohortSetting> createNotificationCohortSetting(
            UUID notificationTypeId, NotificationCohortSetting notificationCohortSetting, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        NotificationCohortSetting created =
                notificationTypeService.createNotificationCohortSetting(notificationTypeId,
                        notificationCohortSetting, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/comms/notification-types/" + notificationTypeId + "/cohort-settings/"
                        + created.getId())).eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<NotificationRule> createNotificationRule(UUID notificationTypeId,
            NotificationRule notificationRule, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        NotificationRule created =
                notificationTypeService.createNotificationRule(notificationTypeId, notificationRule, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/comms/notification-types/" + notificationTypeId + "/rules/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<NotificationType> createNotificationType(NotificationType notificationType,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        NotificationType created = notificationTypeService.createNotificationType(notificationType, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/comms/notification-types/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<NotificationTypeProperty> createNotificationTypeProperty(
            UUID notificationTypeId, NotificationTypeProperty notificationTypeProperty, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        NotificationTypeProperty created =
                notificationTypeService.createNotificationTypeProperty(notificationTypeId,
                        notificationTypeProperty, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/comms/notification-types/" + notificationTypeId + "/properties/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteNotificationCohortSetting(UUID notificationTypeId,
            UUID notificationCohortSettingId, String ifMatch) {
        notificationTypeService.deleteNotificationCohortSetting(notificationTypeId,
                notificationCohortSettingId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteNotificationRule(UUID notificationTypeId, UUID notificationRuleId,
            String ifMatch) {
        notificationTypeService.deleteNotificationRule(notificationTypeId, notificationRuleId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteNotificationType(UUID notificationTypeId, String ifMatch) {
        notificationTypeService.deleteNotificationType(notificationTypeId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteNotificationTypeProperty(UUID notificationTypeId,
            UUID notificationTypePropertyId, String ifMatch) {
        notificationTypeService.deleteNotificationTypeProperty(notificationTypeId, notificationTypePropertyId,
                ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<NotificationCohortSetting> getNotificationCohortSetting(UUID notificationTypeId,
            UUID notificationCohortSettingId, String fields) {
        NotificationCohortSetting found =
                notificationTypeService.getNotificationCohortSetting(notificationTypeId,
                        notificationCohortSettingId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<NotificationRule> getNotificationRule(UUID notificationTypeId,
            UUID notificationRuleId, String fields) {
        NotificationRule found = notificationTypeService.getNotificationRule(notificationTypeId, notificationRuleId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<NotificationType> getNotificationType(UUID notificationTypeId, String fields) {
        NotificationType found = notificationTypeService.getNotificationType(notificationTypeId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<NotificationTypeProperty> getNotificationTypeProperty(UUID notificationTypeId,
            UUID notificationTypePropertyId, String fields) {
        NotificationTypeProperty found =
                notificationTypeService.getNotificationTypeProperty(notificationTypeId,
                        notificationTypePropertyId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<NotificationCohortSetting>> listNotificationCohortSettings(UUID notificationTypeId,
            Integer page, Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<NotificationCohortSetting> list =
                PagingUtil.pagingInfo(notificationTypeService.listNotificationCohortSettings(notificationTypeId, page,
                        size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<NotificationRule>> listNotificationRules(UUID notificationTypeId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<NotificationRule> list =
                PagingUtil.pagingInfo(notificationTypeService.listNotificationRules(notificationTypeId, page, size,
                        cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<NotificationTypeProperty>> listNotificationTypeProperties(UUID notificationTypeId,
            Integer page, Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<NotificationTypeProperty> list =
                PagingUtil.pagingInfo(notificationTypeService.listNotificationTypePropertys(notificationTypeId, page,
                        size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<NotificationType>> listNotificationTypes(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<NotificationType> list =
                PagingUtil.pagingInfo(notificationTypeService.listNotificationTypes(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<NotificationCohortSetting> updateNotificationCohortSetting(
            UUID notificationTypeId, UUID notificationCohortSettingId, String ifMatch,
            NotificationCohortSettingUpdate notificationCohortSettingUpdate, String updateMask, Boolean validateOnly) {
        NotificationCohortSetting updated =
                notificationTypeService.updateNotificationCohortSetting(notificationTypeId,
                        notificationCohortSettingId, ifMatch, notificationCohortSettingUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<NotificationRule> updateNotificationRule(UUID notificationTypeId,
            UUID notificationRuleId, String ifMatch, NotificationRuleUpdate notificationRuleUpdate, String updateMask,
            Boolean validateOnly) {
        NotificationRule updated =
                notificationTypeService.updateNotificationRule(notificationTypeId, notificationRuleId, ifMatch,
                        notificationRuleUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<NotificationType> updateNotificationType(UUID notificationTypeId,
            String ifMatch, NotificationTypeUpdate notificationTypeUpdate, String updateMask, Boolean validateOnly) {
        NotificationType updated =
                notificationTypeService.updateNotificationType(notificationTypeId, ifMatch, notificationTypeUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<NotificationTypeProperty> updateNotificationTypeProperty(
            UUID notificationTypeId, UUID notificationTypePropertyId, String ifMatch,
            NotificationTypePropertyUpdate notificationTypePropertyUpdate, String updateMask, Boolean validateOnly) {
        NotificationTypeProperty updated =
                notificationTypeService.updateNotificationTypeProperty(notificationTypeId,
                        notificationTypePropertyId, ifMatch, notificationTypePropertyUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
