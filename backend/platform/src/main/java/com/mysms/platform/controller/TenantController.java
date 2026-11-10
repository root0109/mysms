package com.mysms.platform.controller;

import com.mysms.platform.api.model.StockImage;
import com.mysms.platform.api.model.ReportDefinition;
import com.mysms.platform.api.model.SearchHit;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.platform.api.TenantApi;
import com.mysms.platform.api.model.Branding;
import com.mysms.platform.api.model.DownloadLink;
import com.mysms.platform.api.model.Operation;
import com.mysms.platform.api.model.RunReportRequest;
import com.mysms.platform.api.model.StartImportRequest;
import com.mysms.platform.api.model.Tenant;
import com.mysms.platform.api.model.TenantUpdate;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import com.mysms.platform.business.TenantService;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Tenant operations: implements the generated contract interface, so paths, parameters and payloads
 * cannot drift from api/openapi/platform.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class TenantController implements TenantApi {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @Override
    public ResponseEntity<Operation> clearDemoData(UUID tenantId, Object body, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Tenant> createTenant(Tenant tenant, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Tenant created = tenantService.createTenant(tenant, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/platform/tenants/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteTenant(UUID tenantId, String ifMatch) {
        tenantService.deleteTenant(tenantId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Branding> getBranding(String tenantCode, UUID institutionId) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<DownloadLink> getImportTemplate(String entityType) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Tenant> getTenant(UUID tenantId, String fields) {
        Tenant found = tenantService.getTenant(tenantId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<SearchHit>> globalSearch(String query, List<String> entityTypes, Integer page,
            Integer size, String cursor) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<PagingInfo<ReportDefinition>> listReportDefinitions(String module, Integer page, Integer size,
            String cursor) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<PagingInfo<Tenant>> listTenants(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Tenant> list = PagingUtil.pagingInfo(tenantService.listTenants(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Operation> runReport(RunReportRequest runReportRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<PagingInfo<StockImage>> searchStockImages(String query, Integer page, Integer size,
            String cursor) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Operation> startImport(StartImportRequest startImportRequest,
            UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Tenant> updateTenant(UUID tenantId, String ifMatch, TenantUpdate tenantUpdate,
            String updateMask, Boolean validateOnly) {
        Tenant updated = tenantService.updateTenant(tenantId, ifMatch, tenantUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
