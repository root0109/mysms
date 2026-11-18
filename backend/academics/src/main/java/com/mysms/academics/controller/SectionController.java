package com.mysms.academics.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.academics.business.SectionService;
import com.mysms.academics.api.SectionApi;
import com.mysms.academics.api.model.Section;
import com.mysms.academics.api.model.SectionUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Section operations; authentication and the permission check already happened in the auth-client
 * filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class SectionController implements SectionApi {

    private final SectionService sectionService;

    public SectionController(SectionService sectionService) {
        this.sectionService = sectionService;
    }

    @Override
    public ResponseEntity<Section> createSection(Section section, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Section created = sectionService.createSection(section, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/academics/sections/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteSection(UUID sectionId, String ifMatch) {
        sectionService.deleteSection(sectionId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Section> getSection(UUID sectionId, String fields) {
        Section found = sectionService.getSection(sectionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Section>> listSections(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Section> list = PagingUtil.pagingInfo(sectionService.listSections(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Section> updateSection(UUID sectionId, String ifMatch,
            SectionUpdate sectionUpdate, String updateMask, Boolean validateOnly) {
        Section updated = sectionService.updateSection(sectionId, ifMatch, sectionUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
