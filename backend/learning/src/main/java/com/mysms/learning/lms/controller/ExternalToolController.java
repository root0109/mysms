package com.mysms.learning.lms.controller;

import com.mysms.learning.lms.business.ExternalToolWorkflowService;
import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.ExternalToolApi;
import com.mysms.learning.lms.api.model.ActivateExternalToolRequest;
import com.mysms.learning.lms.api.model.ExternalTool;
import com.mysms.learning.lms.api.model.ExternalToolInstitution;
import com.mysms.learning.lms.api.model.ExternalToolInstitutionUpdate;
import com.mysms.learning.lms.api.model.ExternalToolOption;
import com.mysms.learning.lms.api.model.ExternalToolOptionUpdate;
import com.mysms.learning.lms.api.model.ExternalToolRole;
import com.mysms.learning.lms.api.model.ExternalToolRoleUpdate;
import com.mysms.learning.lms.api.model.ExternalToolSetting;
import com.mysms.learning.lms.api.model.ExternalToolSettingUpdate;
import com.mysms.learning.lms.api.model.ExternalToolUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.learning.lms.business.ExternalToolService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ExternalTool operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class ExternalToolController implements ExternalToolApi {

    private final ExternalToolService externalToolService;
    private final ExternalToolWorkflowService externalToolWorkflowService;

    public ExternalToolController(ExternalToolService externalToolService,
            ExternalToolWorkflowService externalToolWorkflowService) {
        this.externalToolWorkflowService = externalToolWorkflowService;
        this.externalToolService = externalToolService;
    }

    @Override
    public ResponseEntity<ExternalTool> activateExternalTool(UUID externalToolId,
            ActivateExternalToolRequest activateExternalToolRequest, UUID idempotencyKey) {
        ExternalTool activated = externalToolWorkflowService.activateExternalTool(externalToolId,
                activateExternalToolRequest);
        return ResponseEntity.ok().eTag(activated.getEtag()).body(activated);
    }

    @Override
    public ResponseEntity<ExternalTool> createExternalTool(ExternalTool externalTool, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ExternalTool created = externalToolService.createExternalTool(externalTool, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/external-tools/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<ExternalToolInstitution> createExternalToolInstitution(UUID externalToolId,
            ExternalToolInstitution externalToolInstitution, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ExternalToolInstitution created =
                externalToolService.createExternalToolInstitution(externalToolId, externalToolInstitution,
                        dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/lms/external-tools/" + externalToolId + "/institutions/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<ExternalToolOption> createExternalToolOption(UUID externalToolId,
            ExternalToolOption externalToolOption, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ExternalToolOption created =
                externalToolService.createExternalToolOption(externalToolId, externalToolOption, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/lms/external-tools/" + externalToolId + "/options/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<ExternalToolRole> createExternalToolRole(UUID externalToolId,
            ExternalToolRole externalToolRole, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ExternalToolRole created =
                externalToolService.createExternalToolRole(externalToolId, externalToolRole, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/lms/external-tools/" + externalToolId + "/roles/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<ExternalToolSetting> createExternalToolSetting(UUID externalToolId,
            ExternalToolSetting externalToolSetting, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ExternalToolSetting created =
                externalToolService.createExternalToolSetting(externalToolId, externalToolSetting, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/lms/external-tools/" + externalToolId + "/settings/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteExternalTool(UUID externalToolId, String ifMatch) {
        externalToolService.deleteExternalTool(externalToolId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteExternalToolInstitution(UUID externalToolId,
            UUID externalToolInstitutionId, String ifMatch) {
        externalToolService.deleteExternalToolInstitution(externalToolId, externalToolInstitutionId,
                ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteExternalToolOption(UUID externalToolId, UUID externalToolOptionId,
            String ifMatch) {
        externalToolService.deleteExternalToolOption(externalToolId, externalToolOptionId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteExternalToolRole(UUID externalToolId, UUID externalToolRoleId,
            String ifMatch) {
        externalToolService.deleteExternalToolRole(externalToolId, externalToolRoleId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteExternalToolSetting(UUID externalToolId, UUID externalToolSettingId,
            String ifMatch) {
        externalToolService.deleteExternalToolSetting(externalToolId, externalToolSettingId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ExternalTool> getExternalTool(UUID externalToolId, String fields) {
        ExternalTool found = externalToolService.getExternalTool(externalToolId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ExternalToolInstitution> getExternalToolInstitution(UUID externalToolId,
            UUID externalToolInstitutionId, String fields) {
        ExternalToolInstitution found =
                externalToolService.getExternalToolInstitution(externalToolId, externalToolInstitutionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ExternalToolOption> getExternalToolOption(UUID externalToolId,
            UUID externalToolOptionId, String fields) {
        ExternalToolOption found =
                externalToolService.getExternalToolOption(externalToolId, externalToolOptionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ExternalToolRole> getExternalToolRole(UUID externalToolId,
            UUID externalToolRoleId, String fields) {
        ExternalToolRole found = externalToolService.getExternalToolRole(externalToolId, externalToolRoleId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ExternalToolSetting> getExternalToolSetting(UUID externalToolId,
            UUID externalToolSettingId, String fields) {
        ExternalToolSetting found =
                externalToolService.getExternalToolSetting(externalToolId, externalToolSettingId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<ExternalToolInstitution>> listExternalToolInstitutions(UUID externalToolId,
            Integer page, Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ExternalToolInstitution> list =
                PagingUtil.pagingInfo(externalToolService.listExternalToolInstitutions(externalToolId, page, size,
                        cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<ExternalToolOption>> listExternalToolOptions(UUID externalToolId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ExternalToolOption> list =
                PagingUtil.pagingInfo(externalToolService.listExternalToolOptions(externalToolId, page, size, cursor,
                        filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<ExternalToolRole>> listExternalToolRoles(UUID externalToolId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ExternalToolRole> list =
                PagingUtil.pagingInfo(externalToolService.listExternalToolRoles(externalToolId, page, size, cursor,
                        filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<ExternalToolSetting>> listExternalToolSettings(UUID externalToolId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ExternalToolSetting> list =
                PagingUtil.pagingInfo(externalToolService.listExternalToolSettings(externalToolId, page, size, cursor,
                        filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<ExternalTool>> listExternalTools(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<ExternalTool> list =
                PagingUtil.pagingInfo(externalToolService.listExternalTools(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ExternalTool> updateExternalTool(UUID externalToolId, String ifMatch,
            ExternalToolUpdate externalToolUpdate, Boolean validateOnly) {
        ExternalTool updated =
                externalToolService.updateExternalTool(externalToolId, ifMatch, externalToolUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ExternalToolInstitution> updateExternalToolInstitution(UUID externalToolId,
            UUID externalToolInstitutionId, String ifMatch, ExternalToolInstitutionUpdate externalToolInstitutionUpdate,
            Boolean validateOnly) {
        ExternalToolInstitution updated =
                externalToolService.updateExternalToolInstitution(externalToolId, externalToolInstitutionId,
                        ifMatch, externalToolInstitutionUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ExternalToolOption> updateExternalToolOption(UUID externalToolId,
            UUID externalToolOptionId, String ifMatch, ExternalToolOptionUpdate externalToolOptionUpdate,
            Boolean validateOnly) {
        ExternalToolOption updated =
                externalToolService.updateExternalToolOption(externalToolId, externalToolOptionId, ifMatch,
                        externalToolOptionUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ExternalToolRole> updateExternalToolRole(UUID externalToolId,
            UUID externalToolRoleId, String ifMatch, ExternalToolRoleUpdate externalToolRoleUpdate, Boolean validateOnly) {
        ExternalToolRole updated =
                externalToolService.updateExternalToolRole(externalToolId, externalToolRoleId, ifMatch,
                        externalToolRoleUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ExternalToolSetting> updateExternalToolSetting(UUID externalToolId,
            UUID externalToolSettingId, String ifMatch, ExternalToolSettingUpdate externalToolSettingUpdate,
            Boolean validateOnly) {
        ExternalToolSetting updated =
                externalToolService.updateExternalToolSetting(externalToolId, externalToolSettingId, ifMatch,
                        externalToolSettingUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
