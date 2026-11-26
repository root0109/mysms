package com.mysms.academics.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.academics.business.ProgramService;
import com.mysms.academics.api.ProgramApi;
import com.mysms.academics.api.model.Program;
import com.mysms.academics.api.model.ProgramUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Program operations; authentication and the permission check already happened in the auth-client
 * filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class ProgramController implements ProgramApi {

    private final ProgramService programService;

    public ProgramController(ProgramService programService) {
        this.programService = programService;
    }

    @Override
    public ResponseEntity<Program> createProgram(Program program, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Program created = programService.createProgram(program, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/academics/programs/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteProgram(UUID programId, String ifMatch) {
        programService.deleteProgram(programId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Program> getProgram(UUID programId, String fields) {
        Program found = programService.getProgram(programId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Program>> listPrograms(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Program> list = PagingUtil.pagingInfo(programService.listPrograms(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Program> updateProgram(UUID programId, String ifMatch,
            ProgramUpdate programUpdate, Boolean validateOnly) {
        Program updated = programService.updateProgram(programId, ifMatch, programUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
