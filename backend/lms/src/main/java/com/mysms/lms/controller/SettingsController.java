package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.business.SettingsService;
import com.mysms.lms.api.SettingsApi;
import com.mysms.lms.api.model.PublicLmsSettings;
import com.mysms.lms.api.model.SendContactMessageRequest;
import com.mysms.lms.api.model.Settings;
import com.mysms.lms.api.model.SettingsPrimaryCountry;
import com.mysms.lms.api.model.SettingsPrimaryCountryUpdate;
import com.mysms.lms.api.model.SettingsSidebarItem;
import com.mysms.lms.api.model.SettingsSidebarItemUpdate;
import com.mysms.lms.api.model.SettingsUpdate;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Settings operations: implements the generated contract interface, so paths, parameters and payloads
 * cannot drift from api/openapi/lms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class SettingsController implements SettingsApi {


    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Override
    public ResponseEntity<SettingsPrimaryCountry> createSettingsPrimaryCountry(UUID settingsId,
            SettingsPrimaryCountry settingsPrimaryCountry, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        SettingsPrimaryCountry created =
                settingsService.createSettingsPrimaryCountry(settingsId, settingsPrimaryCountry, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/lms/settings/" + settingsId + "/primary-countries/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<SettingsSidebarItem> createSettingsSidebarItem(UUID settingsId,
            SettingsSidebarItem settingsSidebarItem, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        SettingsSidebarItem created =
                settingsService.createSettingsSidebarItem(settingsId, settingsSidebarItem, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/lms/settings/" + settingsId + "/sidebar-items/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteSettingsPrimaryCountry(UUID settingsId,
            UUID settingsPrimaryCountryId, String ifMatch) {
        settingsService.deleteSettingsPrimaryCountry(settingsId, settingsPrimaryCountryId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteSettingsSidebarItem(UUID settingsId, UUID settingsSidebarItemId,
            String ifMatch) {
        settingsService.deleteSettingsSidebarItem(settingsId, settingsSidebarItemId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PublicLmsSettings> getPublicSettings() {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Settings> getSettings() {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<SettingsPrimaryCountry> getSettingsPrimaryCountry(UUID settingsId,
            UUID settingsPrimaryCountryId, String fields) {
        SettingsPrimaryCountry found =
                settingsService.getSettingsPrimaryCountry(settingsId, settingsPrimaryCountryId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<SettingsSidebarItem> getSettingsSidebarItem(UUID settingsId,
            UUID settingsSidebarItemId, String fields) {
        SettingsSidebarItem found =
                settingsService.getSettingsSidebarItem(settingsId, settingsSidebarItemId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<SettingsPrimaryCountry>> listSettingsPrimaryCountries(UUID settingsId,
            Integer page, Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<SettingsPrimaryCountry> list =
                PagingUtil.pagingInfo(settingsService.listSettingsPrimaryCountrys(settingsId, page, size, cursor,
                        filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<SettingsSidebarItem>> listSettingsSidebarItems(UUID settingsId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<SettingsSidebarItem> list =
                PagingUtil.pagingInfo(settingsService.listSettingsSidebarItems(settingsId, page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Void> sendContactMessage(SendContactMessageRequest sendContactMessageRequest,
            UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Settings> updateSettings(String ifMatch, SettingsUpdate settingsUpdate,
            String updateMask) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<SettingsPrimaryCountry> updateSettingsPrimaryCountry(UUID settingsId,
            UUID settingsPrimaryCountryId, String ifMatch, SettingsPrimaryCountryUpdate settingsPrimaryCountryUpdate,
            String updateMask, Boolean validateOnly) {
        SettingsPrimaryCountry updated =
                settingsService.updateSettingsPrimaryCountry(settingsId, settingsPrimaryCountryId,
                        ifMatch, settingsPrimaryCountryUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<SettingsSidebarItem> updateSettingsSidebarItem(UUID settingsId,
            UUID settingsSidebarItemId, String ifMatch, SettingsSidebarItemUpdate settingsSidebarItemUpdate,
            String updateMask, Boolean validateOnly) {
        SettingsSidebarItem updated =
                settingsService.updateSettingsSidebarItem(settingsId, settingsSidebarItemId, ifMatch,
                        settingsSidebarItemUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
