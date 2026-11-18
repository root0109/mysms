package com.mysms.careers.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.careers.business.CandidatePreferenceService;
import com.mysms.careers.api.CandidatePreferenceApi;
import com.mysms.careers.api.model.CandidatePreference;
import com.mysms.careers.api.model.CandidatePreferenceUpdate;
import com.mysms.careers.api.model.CandidatePreferredFunction;
import com.mysms.careers.api.model.CandidatePreferredFunctionUpdate;
import com.mysms.careers.api.model.CandidatePreferredIndustry;
import com.mysms.careers.api.model.CandidatePreferredIndustryUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the CandidatePreference operations; authentication and the permission check already ran in the
 * auth-client filter. Operations not served yet answer 501.
 */
@RestController
public class CandidatePreferenceController implements CandidatePreferenceApi {

    private final CandidatePreferenceService candidatePreferenceService;

    public CandidatePreferenceController(CandidatePreferenceService candidatePreferenceService) {
        this.candidatePreferenceService = candidatePreferenceService;
    }

    @Override
    public ResponseEntity<CandidatePreference> createCandidatePreference(
            CandidatePreference candidatePreference, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        CandidatePreference created = candidatePreferenceService.createCandidatePreference(candidatePreference, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/careers/candidate-preferences/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<CandidatePreferredFunction> createCandidatePreferredFunction(
            UUID candidatePreferenceId, CandidatePreferredFunction candidatePreferredFunction, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        CandidatePreferredFunction created =
                candidatePreferenceService.createCandidatePreferredFunction(candidatePreferenceId,
                        candidatePreferredFunction, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/careers/candidate-preferences/" + candidatePreferenceId + "/functions/"
                        + created.getId())).eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<CandidatePreferredIndustry> createCandidatePreferredIndustry(
            UUID candidatePreferenceId, CandidatePreferredIndustry candidatePreferredIndustry, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        CandidatePreferredIndustry created =
                candidatePreferenceService.createCandidatePreferredIndustry(candidatePreferenceId,
                        candidatePreferredIndustry, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/careers/candidate-preferences/" + candidatePreferenceId + "/industries/"
                        + created.getId())).eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteCandidatePreference(UUID candidatePreferenceId, String ifMatch) {
        candidatePreferenceService.deleteCandidatePreference(candidatePreferenceId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteCandidatePreferredFunction(UUID candidatePreferenceId,
            UUID candidatePreferredFunctionId, String ifMatch) {
        candidatePreferenceService.deleteCandidatePreferredFunction(candidatePreferenceId,
                candidatePreferredFunctionId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteCandidatePreferredIndustry(UUID candidatePreferenceId,
            UUID candidatePreferredIndustryId, String ifMatch) {
        candidatePreferenceService.deleteCandidatePreferredIndustry(candidatePreferenceId,
                candidatePreferredIndustryId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<CandidatePreference> getCandidatePreference(UUID candidatePreferenceId,
            String fields) {
        CandidatePreference found = candidatePreferenceService.getCandidatePreference(candidatePreferenceId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<CandidatePreferredFunction> getCandidatePreferredFunction(
            UUID candidatePreferenceId, UUID candidatePreferredFunctionId, String fields) {
        CandidatePreferredFunction found =
                candidatePreferenceService.getCandidatePreferredFunction(candidatePreferenceId,
                        candidatePreferredFunctionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<CandidatePreferredIndustry> getCandidatePreferredIndustry(
            UUID candidatePreferenceId, UUID candidatePreferredIndustryId, String fields) {
        CandidatePreferredIndustry found =
                candidatePreferenceService.getCandidatePreferredIndustry(candidatePreferenceId,
                        candidatePreferredIndustryId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<CandidatePreference>> listCandidatePreferences(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<CandidatePreference> list =
                PagingUtil.pagingInfo(candidatePreferenceService.listCandidatePreferences(page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<CandidatePreferredFunction>> listCandidatePreferredFunctions(
            UUID candidatePreferenceId, Integer page, Integer size, String cursor, String filter, List<String> sort,
            String fields) {
        PagingInfo<CandidatePreferredFunction> list =
                PagingUtil.pagingInfo(candidatePreferenceService.listCandidatePreferredFunctions(candidatePreferenceId,
                        page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<CandidatePreferredIndustry>> listCandidatePreferredIndustries(
            UUID candidatePreferenceId, Integer page, Integer size, String cursor, String filter, List<String> sort,
            String fields) {
        PagingInfo<CandidatePreferredIndustry> list =
                PagingUtil.pagingInfo(candidatePreferenceService.listCandidatePreferredIndustrys(candidatePreferenceId,
                        page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<CandidatePreference> updateCandidatePreference(UUID candidatePreferenceId,
            String ifMatch, CandidatePreferenceUpdate candidatePreferenceUpdate, String updateMask,
            Boolean validateOnly) {
        CandidatePreference updated =
                candidatePreferenceService.updateCandidatePreference(candidatePreferenceId, ifMatch,
                        candidatePreferenceUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<CandidatePreferredFunction> updateCandidatePreferredFunction(
            UUID candidatePreferenceId, UUID candidatePreferredFunctionId, String ifMatch,
            CandidatePreferredFunctionUpdate candidatePreferredFunctionUpdate, String updateMask,
            Boolean validateOnly) {
        CandidatePreferredFunction updated =
                candidatePreferenceService.updateCandidatePreferredFunction(candidatePreferenceId,
                        candidatePreferredFunctionId, ifMatch, candidatePreferredFunctionUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<CandidatePreferredIndustry> updateCandidatePreferredIndustry(
            UUID candidatePreferenceId, UUID candidatePreferredIndustryId, String ifMatch,
            CandidatePreferredIndustryUpdate candidatePreferredIndustryUpdate, String updateMask,
            Boolean validateOnly) {
        CandidatePreferredIndustry updated =
                candidatePreferenceService.updateCandidatePreferredIndustry(candidatePreferenceId,
                        candidatePreferredIndustryId, ifMatch, candidatePreferredIndustryUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
