package com.mysms.identity.iam.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.identity.iam.api.SettingValueApi;
import com.mysms.identity.iam.api.model.SettingValue;
import com.mysms.identity.iam.api.model.SettingValueUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.identity.iam.business.SettingValueService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** REST edge of iam.setting_value. */
@RestController
public class SettingValueController implements SettingValueApi {

    private static final String PATH = "/v1/iam/setting-values/";

    private final SettingValueService service;

    public SettingValueController(SettingValueService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<PagingInfo<SettingValue>> listSettingValues(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<SettingValue> list = PagingUtil.pagingInfo(service.list(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<SettingValue> getSettingValue(UUID settingValueId, String fields) {
        SettingValue found = service.get(settingValueId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    /** 201 + Location + ETag; with validateOnly nothing is created and the would-be resource comes back with 200. */
    @Override
    public ResponseEntity<SettingValue> createSettingValue(SettingValue settingValue, UUID idempotencyKey,
                                                           Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        SettingValue created = service.create(settingValue, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create(PATH + created.getId())).eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<SettingValue> updateSettingValue(UUID settingValueId, String ifMatch,
                                                           SettingValueUpdate settingValueUpdate, Boolean validateOnly) {
        SettingValue updated = service.update(settingValueId, ifMatch, settingValueUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<Void> deleteSettingValue(UUID settingValueId, String ifMatch) {
        service.delete(settingValueId, ifMatch);
        return ResponseEntity.noContent().build();
    }
}
