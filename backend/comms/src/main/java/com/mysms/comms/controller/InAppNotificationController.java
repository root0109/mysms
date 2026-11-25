package com.mysms.comms.controller;

import com.mysms.comms.api.InAppNotificationApi;
import com.mysms.comms.api.model.ArchiveNotificationsRequest;
import com.mysms.comms.api.model.InAppNotification;
import com.mysms.comms.business.InAppNotificationService;
import com.mysms.core.paging.PagingInfo;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.web.ReadMask;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** REST edge of the caller's in-app inbox; authentication and the permission check already ran in the filter. */
@RestController
public class InAppNotificationController implements InAppNotificationApi {

    private final InAppNotificationService inAppNotificationService;

    public InAppNotificationController(InAppNotificationService inAppNotificationService) {
        this.inAppNotificationService = inAppNotificationService;
    }

    @Override
    public ResponseEntity<Void> archiveNotifications(ArchiveNotificationsRequest archiveNotificationsRequest,
            UUID idempotencyKey) {
        inAppNotificationService.archiveNotifications(archiveNotificationsRequest);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<InAppNotification> getInAppNotification(UUID inAppNotificationId, String fields) {
        InAppNotification found = inAppNotificationService.getInAppNotification(inAppNotificationId);
        return ResponseEntity.ok(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<InAppNotification>> listInAppNotifications(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<InAppNotification> list = PagingUtil.pagingInfo(
                inAppNotificationService.listInAppNotifications(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Void> markAllNotificationsRead(Object body, UUID idempotencyKey) {
        inAppNotificationService.markAllNotificationsRead();
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<InAppNotification> markNotificationRead(UUID inAppNotificationId, Object body,
            UUID idempotencyKey) {
        return ResponseEntity.ok(inAppNotificationService.markNotificationRead(inAppNotificationId));
    }
}
