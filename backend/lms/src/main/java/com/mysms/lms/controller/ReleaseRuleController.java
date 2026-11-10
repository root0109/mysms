package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.ReleaseRuleApi;
import com.mysms.lms.api.model.CopyReleaseRuleRequest;
import com.mysms.lms.api.model.DisableReleaseRuleRequest;
import com.mysms.lms.api.model.EnableReleaseRuleRequest;
import com.mysms.lms.api.model.ReleaseRule;
import com.mysms.lms.api.model.ReleaseRuleCondition;
import com.mysms.lms.api.model.ReleaseRuleConditionUpdate;
import com.mysms.lms.api.model.ReleaseRuleList;
import com.mysms.lms.api.model.ReleaseRuleTarget;
import com.mysms.lms.api.model.ReleaseRuleTargetUpdate;
import com.mysms.lms.api.model.ReleaseRuleUpdate;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.ReleaseRuleService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ReleaseRule operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/lms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class ReleaseRuleController implements ReleaseRuleApi {

    private final ReleaseRuleService releaseRuleService;

    public ReleaseRuleController(ReleaseRuleService releaseRuleService) {
        this.releaseRuleService = releaseRuleService;
    }

    @Override
    public ResponseEntity<ReleaseRuleList> copyReleaseRule(UUID releaseRuleId,
            CopyReleaseRuleRequest copyReleaseRuleRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<ReleaseRule> createReleaseRule(ReleaseRule releaseRule, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ReleaseRule created = releaseRuleService.createReleaseRule(releaseRule, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/release-rules/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<ReleaseRuleCondition> createReleaseRuleCondition(UUID releaseRuleId,
            ReleaseRuleCondition releaseRuleCondition, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ReleaseRuleCondition created =
                releaseRuleService.createReleaseRuleCondition(releaseRuleId, releaseRuleCondition, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/lms/release-rules/" + releaseRuleId + "/release-rule-conditions/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<ReleaseRuleTarget> createReleaseRuleTarget(UUID releaseRuleId,
            ReleaseRuleTarget releaseRuleTarget, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ReleaseRuleTarget created =
                releaseRuleService.createReleaseRuleTarget(releaseRuleId, releaseRuleTarget, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/lms/release-rules/" + releaseRuleId + "/release-rule-targets/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteReleaseRule(UUID releaseRuleId, String ifMatch) {
        releaseRuleService.deleteReleaseRule(releaseRuleId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteReleaseRuleCondition(UUID releaseRuleId,
            UUID releaseRuleConditionId, String ifMatch) {
        releaseRuleService.deleteReleaseRuleCondition(releaseRuleId, releaseRuleConditionId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteReleaseRuleTarget(UUID releaseRuleId, UUID releaseRuleTargetId,
            String ifMatch) {
        releaseRuleService.deleteReleaseRuleTarget(releaseRuleId, releaseRuleTargetId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ReleaseRule> disableReleaseRule(UUID releaseRuleId,
            DisableReleaseRuleRequest disableReleaseRuleRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<ReleaseRule> enableReleaseRule(UUID releaseRuleId,
            EnableReleaseRuleRequest enableReleaseRuleRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<ReleaseRule> getReleaseRule(UUID releaseRuleId, String fields) {
        ReleaseRule found = releaseRuleService.getReleaseRule(releaseRuleId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ReleaseRuleCondition> getReleaseRuleCondition(UUID releaseRuleId,
            UUID releaseRuleConditionId, String fields) {
        ReleaseRuleCondition found =
                releaseRuleService.getReleaseRuleCondition(releaseRuleId, releaseRuleConditionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ReleaseRuleTarget> getReleaseRuleTarget(UUID releaseRuleId,
            UUID releaseRuleTargetId, String fields) {
        ReleaseRuleTarget found = releaseRuleService.getReleaseRuleTarget(releaseRuleId, releaseRuleTargetId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<ReleaseRuleCondition>> listReleaseRuleConditions(UUID releaseRuleId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ReleaseRuleCondition> list =
                PagingUtil.pagingInfo(releaseRuleService.listReleaseRuleConditions(releaseRuleId, page, size, cursor,
                        filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<ReleaseRuleTarget>> listReleaseRuleTargets(UUID releaseRuleId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ReleaseRuleTarget> list =
                PagingUtil.pagingInfo(releaseRuleService.listReleaseRuleTargets(releaseRuleId, page, size, cursor,
                        filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<ReleaseRule>> listReleaseRules(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<ReleaseRule> list =
                PagingUtil.pagingInfo(releaseRuleService.listReleaseRules(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ReleaseRule> updateReleaseRule(UUID releaseRuleId, String ifMatch,
            ReleaseRuleUpdate releaseRuleUpdate, String updateMask, Boolean validateOnly) {
        ReleaseRule updated =
                releaseRuleService.updateReleaseRule(releaseRuleId, ifMatch, releaseRuleUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ReleaseRuleCondition> updateReleaseRuleCondition(UUID releaseRuleId,
            UUID releaseRuleConditionId, String ifMatch, ReleaseRuleConditionUpdate releaseRuleConditionUpdate,
            String updateMask, Boolean validateOnly) {
        ReleaseRuleCondition updated =
                releaseRuleService.updateReleaseRuleCondition(releaseRuleId, releaseRuleConditionId, ifMatch,
                        releaseRuleConditionUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ReleaseRuleTarget> updateReleaseRuleTarget(UUID releaseRuleId,
            UUID releaseRuleTargetId, String ifMatch, ReleaseRuleTargetUpdate releaseRuleTargetUpdate,
            String updateMask, Boolean validateOnly) {
        ReleaseRuleTarget updated =
                releaseRuleService.updateReleaseRuleTarget(releaseRuleId, releaseRuleTargetId, ifMatch,
                        releaseRuleTargetUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
