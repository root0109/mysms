package com.mysms.academics.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.academics.business.ProgramLevelService;
import com.mysms.academics.api.ProgramLevelApi;
import com.mysms.academics.api.model.ProgramLevel;
import com.mysms.academics.api.model.ProgramLevelUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ProgramLevel operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/academics.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class ProgramLevelController implements ProgramLevelApi {

    private final ProgramLevelService programLevelService;

    public ProgramLevelController(ProgramLevelService programLevelService) {
        this.programLevelService = programLevelService;
    }

    @Override
    public ResponseEntity<ProgramLevel> createProgramLevel(ProgramLevel programLevel, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ProgramLevel created = programLevelService.createProgramLevel(programLevel, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/academics/program-levels/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteProgramLevel(UUID programLevelId, String ifMatch) {
        programLevelService.deleteProgramLevel(programLevelId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ProgramLevel> getProgramLevel(UUID programLevelId, String fields) {
        ProgramLevel found = programLevelService.getProgramLevel(programLevelId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<ProgramLevel>> listProgramLevels(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<ProgramLevel> list =
                PagingUtil.pagingInfo(programLevelService.listProgramLevels(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ProgramLevel> updateProgramLevel(UUID programLevelId, String ifMatch,
            ProgramLevelUpdate programLevelUpdate, String updateMask, Boolean validateOnly) {
        ProgramLevel updated =
                programLevelService.updateProgramLevel(programLevelId, ifMatch, programLevelUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
