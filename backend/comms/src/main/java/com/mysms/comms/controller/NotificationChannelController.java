package com.mysms.comms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.business.NotificationChannelService;
import com.mysms.comms.api.NotificationChannelApi;
import com.mysms.comms.api.model.NotificationChannel;
import com.mysms.comms.api.model.NotificationChannelProperty;
import com.mysms.comms.api.model.NotificationChannelPropertyUpdate;
import com.mysms.comms.api.model.NotificationChannelUpdate;
import com.mysms.comms.api.model.SendTestMessageRequest;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the NotificationChannel operations; authentication and the permission check already ran in the
 * auth-client filter.
 */
@RestController
public class NotificationChannelController implements NotificationChannelApi {

    private final NotificationChannelService notificationChannelService;

    public NotificationChannelController(NotificationChannelService notificationChannelService) {
        this.notificationChannelService = notificationChannelService;
    }

    @Override
    public ResponseEntity<NotificationChannel> createNotificationChannel(
            NotificationChannel notificationChannel, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        NotificationChannel created = notificationChannelService.createNotificationChannel(notificationChannel, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/comms/notification-channels/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<NotificationChannelProperty> createNotificationChannelProperty(
            UUID notificationChannelId, NotificationChannelProperty notificationChannelProperty, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        NotificationChannelProperty created =
                notificationChannelService.createNotificationChannelProperty(notificationChannelId,
                        notificationChannelProperty, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/comms/notification-channels/" + notificationChannelId + "/properties/"
                        + created.getId())).eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteNotificationChannel(UUID notificationChannelId, String ifMatch) {
        notificationChannelService.deleteNotificationChannel(notificationChannelId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteNotificationChannelProperty(UUID notificationChannelId,
            UUID notificationChannelPropertyId, String ifMatch) {
        notificationChannelService.deleteNotificationChannelProperty(notificationChannelId,
                notificationChannelPropertyId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<NotificationChannel> getNotificationChannel(UUID notificationChannelId,
            String fields) {
        NotificationChannel found = notificationChannelService.getNotificationChannel(notificationChannelId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<NotificationChannelProperty> getNotificationChannelProperty(
            UUID notificationChannelId, UUID notificationChannelPropertyId, String fields) {
        NotificationChannelProperty found =
                notificationChannelService.getNotificationChannelProperty(notificationChannelId,
                        notificationChannelPropertyId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<NotificationChannelProperty>> listNotificationChannelProperties(
            UUID notificationChannelId, Integer page, Integer size, String cursor, String filter, List<String> sort,
            String fields) {
        PagingInfo<NotificationChannelProperty> list =
                PagingUtil.pagingInfo(notificationChannelService.listNotificationChannelPropertys(notificationChannelId,
                        page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<NotificationChannel>> listNotificationChannels(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<NotificationChannel> list =
                PagingUtil.pagingInfo(notificationChannelService.listNotificationChannels(page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Void> sendTestMessage(UUID notificationChannelId,
            SendTestMessageRequest sendTestMessageRequest, UUID idempotencyKey) {
        notificationChannelService.sendTestMessage(notificationChannelId, sendTestMessageRequest);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<NotificationChannel> updateNotificationChannel(UUID notificationChannelId,
            String ifMatch, NotificationChannelUpdate notificationChannelUpdate, Boolean validateOnly) {
        NotificationChannel updated =
                notificationChannelService.updateNotificationChannel(notificationChannelId, ifMatch,
                        notificationChannelUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<NotificationChannelProperty> updateNotificationChannelProperty(
            UUID notificationChannelId, UUID notificationChannelPropertyId, String ifMatch,
            NotificationChannelPropertyUpdate notificationChannelPropertyUpdate, Boolean validateOnly) {
        NotificationChannelProperty updated =
                notificationChannelService.updateNotificationChannelProperty(notificationChannelId,
                        notificationChannelPropertyId, ifMatch, notificationChannelPropertyUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
