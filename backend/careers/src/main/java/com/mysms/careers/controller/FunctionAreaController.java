package com.mysms.careers.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.careers.business.FunctionAreaService;
import com.mysms.careers.api.FunctionAreaApi;
import com.mysms.careers.api.model.FunctionArea;
import com.mysms.careers.api.model.FunctionAreaUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the FunctionArea operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class FunctionAreaController implements FunctionAreaApi {

    private final FunctionAreaService functionAreaService;

    public FunctionAreaController(FunctionAreaService functionAreaService) {
        this.functionAreaService = functionAreaService;
    }

    @Override
    public ResponseEntity<FunctionArea> createFunctionArea(FunctionArea functionArea, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        FunctionArea created = functionAreaService.createFunctionArea(functionArea, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/careers/function-areas/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteFunctionArea(UUID functionAreaId, String ifMatch) {
        functionAreaService.deleteFunctionArea(functionAreaId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<FunctionArea> getFunctionArea(UUID functionAreaId, String fields) {
        FunctionArea found = functionAreaService.getFunctionArea(functionAreaId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<FunctionArea>> listFunctionAreas(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<FunctionArea> list =
                PagingUtil.pagingInfo(functionAreaService.listFunctionAreas(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<FunctionArea> updateFunctionArea(UUID functionAreaId, String ifMatch,
            FunctionAreaUpdate functionAreaUpdate, String updateMask, Boolean validateOnly) {
        FunctionArea updated =
                functionAreaService.updateFunctionArea(functionAreaId, ifMatch, functionAreaUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
