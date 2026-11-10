package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.ExternalToolApi;
import com.mysms.lms.api.model.ActivateExternalToolRequest;
import com.mysms.lms.api.model.ExternalTool;
import com.mysms.lms.api.model.ExternalToolInstitution;
import com.mysms.lms.api.model.ExternalToolInstitutionUpdate;
import com.mysms.lms.api.model.ExternalToolOption;
import com.mysms.lms.api.model.ExternalToolOptionUpdate;
import com.mysms.lms.api.model.ExternalToolRole;
import com.mysms.lms.api.model.ExternalToolRoleUpdate;
import com.mysms.lms.api.model.ExternalToolSetting;
import com.mysms.lms.api.model.ExternalToolSettingUpdate;
import com.mysms.lms.api.model.ExternalToolUpdate;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.ExternalToolService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ExternalTool operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/lms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class ExternalToolController implements ExternalToolApi {

    private final ExternalToolService externalToolService;

    public ExternalToolController(ExternalToolService externalToolService) {
        this.externalToolService = externalToolService;
    }

    @Override
    public ResponseEntity<ExternalTool> activateExternalTool(UUID externalToolId,
            ActivateExternalToolRequest activateExternalToolRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
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
            ExternalToolUpdate externalToolUpdate, String updateMask, Boolean validateOnly) {
        ExternalTool updated =
                externalToolService.updateExternalTool(externalToolId, ifMatch, externalToolUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ExternalToolInstitution> updateExternalToolInstitution(UUID externalToolId,
            UUID externalToolInstitutionId, String ifMatch, ExternalToolInstitutionUpdate externalToolInstitutionUpdate,
            String updateMask, Boolean validateOnly) {
        ExternalToolInstitution updated =
                externalToolService.updateExternalToolInstitution(externalToolId, externalToolInstitutionId,
                        ifMatch, externalToolInstitutionUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ExternalToolOption> updateExternalToolOption(UUID externalToolId,
            UUID externalToolOptionId, String ifMatch, ExternalToolOptionUpdate externalToolOptionUpdate,
            String updateMask, Boolean validateOnly) {
        ExternalToolOption updated =
                externalToolService.updateExternalToolOption(externalToolId, externalToolOptionId, ifMatch,
                        externalToolOptionUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ExternalToolRole> updateExternalToolRole(UUID externalToolId,
            UUID externalToolRoleId, String ifMatch, ExternalToolRoleUpdate externalToolRoleUpdate, String updateMask,
            Boolean validateOnly) {
        ExternalToolRole updated =
                externalToolService.updateExternalToolRole(externalToolId, externalToolRoleId, ifMatch,
                        externalToolRoleUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ExternalToolSetting> updateExternalToolSetting(UUID externalToolId,
            UUID externalToolSettingId, String ifMatch, ExternalToolSettingUpdate externalToolSettingUpdate,
            String updateMask, Boolean validateOnly) {
        ExternalToolSetting updated =
                externalToolService.updateExternalToolSetting(externalToolId, externalToolSettingId, ifMatch,
                        externalToolSettingUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
