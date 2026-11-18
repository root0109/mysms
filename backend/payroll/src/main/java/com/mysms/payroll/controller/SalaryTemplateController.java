package com.mysms.payroll.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.payroll.api.SalaryTemplateApi;
import com.mysms.payroll.api.model.SalaryTemplate;
import com.mysms.payroll.api.model.SalaryTemplateComponent;
import com.mysms.payroll.api.model.SalaryTemplateComponentUpdate;
import com.mysms.payroll.api.model.SalaryTemplateUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.payroll.business.SalaryTemplateService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the SalaryTemplate operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class SalaryTemplateController implements SalaryTemplateApi {

    private final SalaryTemplateService salaryTemplateService;

    public SalaryTemplateController(SalaryTemplateService salaryTemplateService) {
        this.salaryTemplateService = salaryTemplateService;
    }

    @Override
    public ResponseEntity<SalaryTemplate> createSalaryTemplate(SalaryTemplate salaryTemplate,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        SalaryTemplate created = salaryTemplateService.createSalaryTemplate(salaryTemplate, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/payroll/salary-templates/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<SalaryTemplateComponent> createSalaryTemplateComponent(UUID salaryTemplateId,
            SalaryTemplateComponent salaryTemplateComponent, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        SalaryTemplateComponent created =
                salaryTemplateService.createSalaryTemplateComponent(salaryTemplateId, salaryTemplateComponent,
                        dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/payroll/salary-templates/" + salaryTemplateId + "/components/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteSalaryTemplate(UUID salaryTemplateId, String ifMatch) {
        salaryTemplateService.deleteSalaryTemplate(salaryTemplateId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteSalaryTemplateComponent(UUID salaryTemplateId,
            UUID salaryTemplateComponentId, String ifMatch) {
        salaryTemplateService.deleteSalaryTemplateComponent(salaryTemplateId, salaryTemplateComponentId,
                ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<SalaryTemplate> getSalaryTemplate(UUID salaryTemplateId, String fields) {
        SalaryTemplate found = salaryTemplateService.getSalaryTemplate(salaryTemplateId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<SalaryTemplateComponent> getSalaryTemplateComponent(UUID salaryTemplateId,
            UUID salaryTemplateComponentId, String fields) {
        SalaryTemplateComponent found =
                salaryTemplateService.getSalaryTemplateComponent(salaryTemplateId, salaryTemplateComponentId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<SalaryTemplateComponent>> listSalaryTemplateComponents(UUID salaryTemplateId,
            Integer page, Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<SalaryTemplateComponent> list =
                PagingUtil.pagingInfo(salaryTemplateService.listSalaryTemplateComponents(salaryTemplateId, page, size,
                        cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<SalaryTemplate>> listSalaryTemplates(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<SalaryTemplate> list =
                PagingUtil.pagingInfo(salaryTemplateService.listSalaryTemplates(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<SalaryTemplate> updateSalaryTemplate(UUID salaryTemplateId, String ifMatch,
            SalaryTemplateUpdate salaryTemplateUpdate, String updateMask, Boolean validateOnly) {
        SalaryTemplate updated =
                salaryTemplateService.updateSalaryTemplate(salaryTemplateId, ifMatch, salaryTemplateUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<SalaryTemplateComponent> updateSalaryTemplateComponent(UUID salaryTemplateId,
            UUID salaryTemplateComponentId, String ifMatch, SalaryTemplateComponentUpdate salaryTemplateComponentUpdate,
            String updateMask, Boolean validateOnly) {
        SalaryTemplateComponent updated =
                salaryTemplateService.updateSalaryTemplateComponent(salaryTemplateId,
                        salaryTemplateComponentId, ifMatch, salaryTemplateComponentUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
