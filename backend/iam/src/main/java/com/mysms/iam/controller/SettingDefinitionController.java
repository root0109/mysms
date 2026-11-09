package com.mysms.iam.controller;

import com.mysms.contracts.iam.api.SettingDefinitionApi;
import com.mysms.contracts.iam.model.SettingDefinition;
import com.mysms.contracts.iam.model.SettingDefinitionList;
import com.mysms.core.web.ReadMask;
import com.mysms.iam.business.SettingDefinitionService;
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
    public ResponseEntity<SettingDefinitionList> listSettingDefinitions(Integer pageSize, String pageToken, String filter,
                                                                        String orderBy, String fields) {
        SettingDefinitionList list = service.list(pageSize, pageToken, filter, orderBy);
        list.getItems().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<SettingDefinition> getSettingDefinition(UUID settingDefinitionId, String fields) {
        SettingDefinition found = service.get(settingDefinitionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }
}
