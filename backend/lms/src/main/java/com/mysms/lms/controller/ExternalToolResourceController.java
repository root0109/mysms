package com.mysms.lms.controller;

import com.mysms.lms.business.ExternalToolWorkflowService;
import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.ExternalToolResourceApi;
import com.mysms.lms.api.model.ExternalToolLink;
import com.mysms.lms.api.model.ExternalToolLinkUpdate;
import com.mysms.lms.api.model.ExternalToolResource;
import com.mysms.lms.api.model.ExternalToolResourceUpdate;
import com.mysms.lms.api.model.LaunchExternalToolRequest;
import com.mysms.lms.api.model.LtiLaunch;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.ExternalToolResourceService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ExternalToolResource operations; authentication and the permission check already ran in the
 * auth-client filter. Operations not served yet answer 501.
 */
@RestController
public class ExternalToolResourceController implements ExternalToolResourceApi {

    private final ExternalToolResourceService externalToolResourceService;
    private final ExternalToolWorkflowService externalToolWorkflowService;

    public ExternalToolResourceController(ExternalToolResourceService externalToolResourceService,
            ExternalToolWorkflowService externalToolWorkflowService) {
        this.externalToolWorkflowService = externalToolWorkflowService;
        this.externalToolResourceService = externalToolResourceService;
    }

    @Override
    public ResponseEntity<ExternalToolLink> createExternalToolLink(UUID externalToolResourceId,
            ExternalToolLink externalToolLink, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ExternalToolLink created =
                externalToolResourceService.createExternalToolLink(externalToolResourceId, externalToolLink, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/lms/external-tool-resources/" + externalToolResourceId + "/links/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<ExternalToolResource> createExternalToolResource(
            ExternalToolResource externalToolResource, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ExternalToolResource created =
                externalToolResourceService.createExternalToolResource(externalToolResource, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/external-tool-resources/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteExternalToolLink(UUID externalToolResourceId,
            UUID externalToolLinkId, String ifMatch) {
        externalToolResourceService.deleteExternalToolLink(externalToolResourceId, externalToolLinkId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteExternalToolResource(UUID externalToolResourceId, String ifMatch) {
        externalToolResourceService.deleteExternalToolResource(externalToolResourceId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ExternalToolLink> getExternalToolLink(UUID externalToolResourceId,
            UUID externalToolLinkId, String fields) {
        ExternalToolLink found =
                externalToolResourceService.getExternalToolLink(externalToolResourceId, externalToolLinkId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ExternalToolResource> getExternalToolResource(UUID externalToolResourceId,
            String fields) {
        ExternalToolResource found = externalToolResourceService.getExternalToolResource(externalToolResourceId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<LtiLaunch> launchExternalTool(UUID externalToolResourceId,
            UUID externalToolLinkId, LaunchExternalToolRequest launchExternalToolRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(externalToolWorkflowService.launchExternalTool(externalToolResourceId,
                externalToolLinkId, launchExternalToolRequest));
    }

    @Override
    public ResponseEntity<PagingInfo<ExternalToolLink>> listExternalToolLinks(UUID externalToolResourceId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ExternalToolLink> list =
                PagingUtil.pagingInfo(externalToolResourceService.listExternalToolLinks(externalToolResourceId, page,
                        size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<ExternalToolResource>> listExternalToolResources(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ExternalToolResource> list =
                PagingUtil.pagingInfo(externalToolResourceService.listExternalToolResources(page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Void> receiveToolOutcome(String authorization, byte[] body) {
        externalToolWorkflowService.receiveToolOutcome(authorization, body);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ExternalToolLink> updateExternalToolLink(UUID externalToolResourceId,
            UUID externalToolLinkId, String ifMatch, ExternalToolLinkUpdate externalToolLinkUpdate, String updateMask,
            Boolean validateOnly) {
        ExternalToolLink updated =
                externalToolResourceService.updateExternalToolLink(externalToolResourceId, externalToolLinkId, ifMatch,
                        externalToolLinkUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ExternalToolResource> updateExternalToolResource(UUID externalToolResourceId,
            String ifMatch, ExternalToolResourceUpdate externalToolResourceUpdate, String updateMask,
            Boolean validateOnly) {
        ExternalToolResource updated =
                externalToolResourceService.updateExternalToolResource(externalToolResourceId, ifMatch,
                        externalToolResourceUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
