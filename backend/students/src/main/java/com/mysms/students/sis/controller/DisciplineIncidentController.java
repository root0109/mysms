package com.mysms.students.sis.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.students.sis.api.DisciplineIncidentApi;
import com.mysms.students.sis.api.model.DisciplineIncident;
import com.mysms.students.sis.api.model.DisciplineIncidentUpdate;
import com.mysms.students.sis.api.model.MarkIncidentActionTakenRequest;
import com.mysms.students.sis.api.model.MarkIncidentSeenRequest;
import com.mysms.core.web.ReadMask;
import com.mysms.students.sis.business.DisciplineIncidentService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the DisciplineIncident operations; authentication and the permission check already happened in the
 * auth-client filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class DisciplineIncidentController implements DisciplineIncidentApi {

    private final DisciplineIncidentService disciplineIncidentService;

    public DisciplineIncidentController(DisciplineIncidentService disciplineIncidentService) {
        this.disciplineIncidentService = disciplineIncidentService;
    }

    @Override
    public ResponseEntity<DisciplineIncident> createDisciplineIncident(
            DisciplineIncident disciplineIncident, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        DisciplineIncident created = disciplineIncidentService.createDisciplineIncident(disciplineIncident, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/sis/discipline-incidents/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteDisciplineIncident(UUID disciplineIncidentId, String ifMatch) {
        disciplineIncidentService.deleteDisciplineIncident(disciplineIncidentId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<DisciplineIncident> getDisciplineIncident(UUID disciplineIncidentId,
            String fields) {
        DisciplineIncident found = disciplineIncidentService.getDisciplineIncident(disciplineIncidentId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<DisciplineIncident>> listDisciplineIncidents(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<DisciplineIncident> list =
                PagingUtil.pagingInfo(disciplineIncidentService.listDisciplineIncidents(page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<DisciplineIncident> markIncidentActionTaken(UUID disciplineIncidentId,
            MarkIncidentActionTakenRequest markIncidentActionTakenRequest, UUID idempotencyKey) {
        DisciplineIncident changed = disciplineIncidentService.markIncidentActionTaken(disciplineIncidentId,
                markIncidentActionTakenRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<DisciplineIncident> markIncidentSeen(UUID disciplineIncidentId,
            MarkIncidentSeenRequest markIncidentSeenRequest, UUID idempotencyKey) {
        DisciplineIncident changed = disciplineIncidentService.markIncidentSeen(disciplineIncidentId, markIncidentSeenRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<DisciplineIncident> updateDisciplineIncident(UUID disciplineIncidentId,
            String ifMatch, DisciplineIncidentUpdate disciplineIncidentUpdate, Boolean validateOnly) {
        DisciplineIncident updated =
                disciplineIncidentService.updateDisciplineIncident(disciplineIncidentId, ifMatch,
                        disciplineIncidentUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
