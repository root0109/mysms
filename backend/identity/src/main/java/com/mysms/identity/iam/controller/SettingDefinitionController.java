package com.mysms.identity.iam.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.identity.iam.api.SettingDefinitionApi;
import com.mysms.identity.iam.api.model.SettingDefinition;
import com.mysms.core.web.ReadMask;
import com.mysms.identity.iam.business.SettingDefinitionService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** REST edge of the read-only setting catalogue (iam.ListSettingDefinitions, iam.GetSettingDefinition). */
@RestController
public class SettingDefinitionController implements SettingDefinitionApi {

    private final SettingDefinitionService service;

    public SettingDefinitionController(SettingDefinitionService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<PagingInfo<SettingDefinition>> listSettingDefinitions(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<SettingDefinition> list = PagingUtil.pagingInfo(service.list(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<SettingDefinition> getSettingDefinition(UUID settingDefinitionId, String fields) {
        SettingDefinition found = service.get(settingDefinitionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }
}
