package com.mysms.students.sis.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.students.sis.api.ActivityTypeApi;
import com.mysms.students.sis.api.model.ActivityType;
import com.mysms.students.sis.api.model.ActivityTypeUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.students.sis.business.ActivityTypeService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ActivityType operations; authentication and the permission check already happened in the auth-client
 * filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class ActivityTypeController implements ActivityTypeApi {

    private final ActivityTypeService activityTypeService;

    public ActivityTypeController(ActivityTypeService activityTypeService) {
        this.activityTypeService = activityTypeService;
    }

    @Override
    public ResponseEntity<ActivityType> createActivityType(ActivityType activityType, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ActivityType created = activityTypeService.createActivityType(activityType, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/sis/activity-types/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteActivityType(UUID activityTypeId, String ifMatch) {
        activityTypeService.deleteActivityType(activityTypeId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ActivityType> getActivityType(UUID activityTypeId, String fields) {
        ActivityType found = activityTypeService.getActivityType(activityTypeId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<ActivityType>> listActivityTypes(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<ActivityType> list =
                PagingUtil.pagingInfo(activityTypeService.listActivityTypes(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ActivityType> updateActivityType(UUID activityTypeId, String ifMatch,
            ActivityTypeUpdate activityTypeUpdate, Boolean validateOnly) {
        ActivityType updated =
                activityTypeService.updateActivityType(activityTypeId, ifMatch, activityTypeUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
