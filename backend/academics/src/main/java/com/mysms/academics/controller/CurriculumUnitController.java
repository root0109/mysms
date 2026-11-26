package com.mysms.academics.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.academics.business.CurriculumUnitService;
import com.mysms.academics.api.CurriculumUnitApi;
import com.mysms.academics.api.model.CurriculumUnit;
import com.mysms.academics.api.model.CurriculumUnitUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the CurriculumUnit operations; authentication and the permission check already happened in the
 * auth-client filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class CurriculumUnitController implements CurriculumUnitApi {

    private final CurriculumUnitService curriculumUnitService;

    public CurriculumUnitController(CurriculumUnitService curriculumUnitService) {
        this.curriculumUnitService = curriculumUnitService;
    }

    @Override
    public ResponseEntity<CurriculumUnit> createCurriculumUnit(CurriculumUnit curriculumUnit,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        CurriculumUnit created = curriculumUnitService.createCurriculumUnit(curriculumUnit, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/academics/curriculum-units/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteCurriculumUnit(UUID curriculumUnitId, String ifMatch) {
        curriculumUnitService.deleteCurriculumUnit(curriculumUnitId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<CurriculumUnit> getCurriculumUnit(UUID curriculumUnitId, String fields) {
        CurriculumUnit found = curriculumUnitService.getCurriculumUnit(curriculumUnitId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<CurriculumUnit>> listCurriculumUnits(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<CurriculumUnit> list =
                PagingUtil.pagingInfo(curriculumUnitService.listCurriculumUnits(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<CurriculumUnit> updateCurriculumUnit(UUID curriculumUnitId, String ifMatch,
            CurriculumUnitUpdate curriculumUnitUpdate, Boolean validateOnly) {
        CurriculumUnit updated =
                curriculumUnitService.updateCurriculumUnit(curriculumUnitId, ifMatch, curriculumUnitUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
