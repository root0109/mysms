package com.mysms.platform.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.platform.api.SystemSettingApi;
import com.mysms.platform.api.model.SystemSetting;
import com.mysms.platform.api.model.SystemSettingUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.platform.business.SystemSettingService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the SystemSetting operations; authentication and the permission check already happened in the
 * auth-client filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class SystemSettingController implements SystemSettingApi {

    private final SystemSettingService systemSettingService;

    public SystemSettingController(SystemSettingService systemSettingService) {
        this.systemSettingService = systemSettingService;
    }

    @Override
    public ResponseEntity<SystemSetting> createSystemSetting(SystemSetting systemSetting,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        SystemSetting created = systemSettingService.createSystemSetting(systemSetting, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/platform/system-settings/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteSystemSetting(UUID systemSettingId, String ifMatch) {
        systemSettingService.deleteSystemSetting(systemSettingId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<SystemSetting> getSystemSetting(UUID systemSettingId, String fields) {
        SystemSetting found = systemSettingService.getSystemSetting(systemSettingId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<SystemSetting>> listSystemSettings(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<SystemSetting> list =
                PagingUtil.pagingInfo(systemSettingService.listSystemSettings(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<SystemSetting> updateSystemSetting(UUID systemSettingId, String ifMatch,
            SystemSettingUpdate systemSettingUpdate, Boolean validateOnly) {
        SystemSetting updated =
                systemSettingService.updateSystemSetting(systemSettingId, ifMatch, systemSettingUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
