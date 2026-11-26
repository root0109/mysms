package com.mysms.platform.controller;

import com.mysms.core.paging.PagingInfo;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.web.ReadMask;
import com.mysms.platform.api.ThemeApi;
import com.mysms.platform.api.model.Theme;
import com.mysms.platform.api.model.ThemeUpdate;
import com.mysms.platform.business.ThemeService;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Theme operations; authentication and the permission check already happened in the auth-client
 * filter.
 */
@RestController
public class ThemeController implements ThemeApi {

    private final ThemeService themeService;

    public ThemeController(ThemeService themeService) {
        this.themeService = themeService;
    }

    @Override
    public ResponseEntity<Theme> createTheme(Theme theme, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Theme created = themeService.createTheme(theme, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/platform/themes/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteTheme(UUID themeId, String ifMatch) {
        themeService.deleteTheme(themeId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Theme> getTheme(UUID themeId, String fields) {
        Theme found = themeService.getTheme(themeId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Theme>> listThemes(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Theme> list =
                PagingUtil.pagingInfo(themeService.listThemes(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Theme> updateTheme(UUID themeId, String ifMatch, ThemeUpdate themeUpdate, Boolean validateOnly) {
        Theme updated = themeService.updateTheme(themeId, ifMatch, themeUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
