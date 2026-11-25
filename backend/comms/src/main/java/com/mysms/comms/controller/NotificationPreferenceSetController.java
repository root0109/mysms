package com.mysms.comms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.business.NotificationPreferenceSetService;
import com.mysms.comms.api.NotificationPreferenceSetApi;
import com.mysms.comms.api.model.NotificationPreference;
import com.mysms.comms.api.model.NotificationPreferenceSet;
import com.mysms.comms.api.model.NotificationPreferenceSetUpdate;
import com.mysms.comms.api.model.NotificationPreferenceUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the NotificationPreferenceSet operations; authentication and the permission check already ran in the
 * auth-client filter.
 */
@RestController
public class NotificationPreferenceSetController implements NotificationPreferenceSetApi {

    private final NotificationPreferenceSetService notificationPreferenceSetService;

    public NotificationPreferenceSetController(NotificationPreferenceSetService notificationPreferenceSetService) {
        this.notificationPreferenceSetService = notificationPreferenceSetService;
    }

    @Override
    public ResponseEntity<NotificationPreference> createNotificationPreference(
            UUID notificationPreferenceSetId, NotificationPreference notificationPreference, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        NotificationPreference created =
                notificationPreferenceSetService.createNotificationPreference(notificationPreferenceSetId,
                        notificationPreference, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/comms/notification-preference-sets/" + notificationPreferenceSetId + "/preferences/"
                        + created.getId())).eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<NotificationPreferenceSet> createNotificationPreferenceSet(
            NotificationPreferenceSet notificationPreferenceSet, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        NotificationPreferenceSet created =
                notificationPreferenceSetService.createNotificationPreferenceSet(notificationPreferenceSet, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/comms/notification-preference-sets/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteNotificationPreference(UUID notificationPreferenceSetId,
            UUID notificationPreferenceId, String ifMatch) {
        notificationPreferenceSetService.deleteNotificationPreference(notificationPreferenceSetId,
                notificationPreferenceId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteNotificationPreferenceSet(UUID notificationPreferenceSetId,
            String ifMatch) {
        notificationPreferenceSetService.deleteNotificationPreferenceSet(notificationPreferenceSetId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<NotificationPreference> getNotificationPreference(
            UUID notificationPreferenceSetId, UUID notificationPreferenceId, String fields) {
        NotificationPreference found =
                notificationPreferenceSetService.getNotificationPreference(notificationPreferenceSetId,
                        notificationPreferenceId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<NotificationPreferenceSet> getNotificationPreferenceSet(
            UUID notificationPreferenceSetId, String fields) {
        NotificationPreferenceSet found =
                notificationPreferenceSetService.getNotificationPreferenceSet(notificationPreferenceSetId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<NotificationPreferenceSet>> listNotificationPreferenceSets(Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<NotificationPreferenceSet> list =
                PagingUtil.pagingInfo(notificationPreferenceSetService.listNotificationPreferenceSets(page, size,
                        cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<NotificationPreference>> listNotificationPreferences(
            UUID notificationPreferenceSetId, Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<NotificationPreference> list =
                PagingUtil.pagingInfo(notificationPreferenceSetService.listNotificationPreferences(
                        notificationPreferenceSetId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<NotificationPreference> updateNotificationPreference(
            UUID notificationPreferenceSetId, UUID notificationPreferenceId, String ifMatch,
            NotificationPreferenceUpdate notificationPreferenceUpdate, String updateMask, Boolean validateOnly) {
        NotificationPreference updated =
                notificationPreferenceSetService.updateNotificationPreference(notificationPreferenceSetId,
                        notificationPreferenceId, ifMatch, notificationPreferenceUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<NotificationPreferenceSet> updateNotificationPreferenceSet(
            UUID notificationPreferenceSetId, String ifMatch,
            NotificationPreferenceSetUpdate notificationPreferenceSetUpdate, String updateMask, Boolean validateOnly) {
        NotificationPreferenceSet updated =
                notificationPreferenceSetService.updateNotificationPreferenceSet(notificationPreferenceSetId, ifMatch,
                        notificationPreferenceSetUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
