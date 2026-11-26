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
import com.mysms.core.web.ReadMask;
import com.mysms.platform.business.BrandingService;
import com.mysms.platform.business.GlobalSearchService;
import com.mysms.platform.business.ImportService;
import com.mysms.platform.business.ReportService;
import com.mysms.platform.business.StockImageLookup;
import com.mysms.platform.business.TenantService;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Tenant operations; authentication and the permission check already happened in the auth-client
 * filter; every operation is served.
 */
@RestController
public class TenantController implements TenantApi {

    private final TenantService tenantService;
    private final BrandingService brandingService;
    private final GlobalSearchService globalSearchService;
    private final ReportService reportService;
    private final StockImageLookup stockImageLookup;
    private final ImportService importService;

    public TenantController(TenantService tenantService, BrandingService brandingService,
            GlobalSearchService globalSearchService,
            ReportService reportService, StockImageLookup stockImageLookup, ImportService importService) {
        this.tenantService = tenantService;
        this.brandingService = brandingService;
        this.globalSearchService = globalSearchService;
        this.reportService = reportService;
        this.stockImageLookup = stockImageLookup;
        this.importService = importService;
    }

    @Override
    public ResponseEntity<Operation> clearDemoData(UUID tenantId, Object body, UUID idempotencyKey) {
        return ResponseEntity.accepted().body(tenantService.clearDemoData(tenantId));
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
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(brandingService.getBranding(tenantCode, institutionId));
    }

    @Override
    public ResponseEntity<DownloadLink> getImportTemplate(String entityType) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(importService.getImportTemplate(entityType));
    }

    @Override
    public ResponseEntity<Tenant> getTenant(UUID tenantId, String fields) {
        Tenant found = tenantService.getTenant(tenantId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<SearchHit>> globalSearch(String query, List<String> entityTypes, Integer page,
            Integer size, String cursor) {
        return ResponseEntity.ok(PagingUtil.pagingInfo(globalSearchService.search(query, entityTypes, page, size)));
    }

    @Override
    public ResponseEntity<PagingInfo<ReportDefinition>> listReportDefinitions(String module, Integer page, Integer size,
            String cursor) {
        return ResponseEntity.ok(PagingUtil.pagingInfo(reportService.listReportDefinitions(module, page, size)));
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
        return ResponseEntity.accepted().body(reportService.runReport(runReportRequest));
    }

    @Override
    public ResponseEntity<PagingInfo<StockImage>> searchStockImages(String query, Integer page, Integer size,
            String cursor) {
        return ResponseEntity.ok(PagingUtil.pagingInfo(stockImageLookup.search(query, page, size)));
    }

    @Override
    public ResponseEntity<Operation> startImport(StartImportRequest startImportRequest,
            UUID idempotencyKey) {
        return ResponseEntity.accepted().body(importService.startImport(startImportRequest));
    }

    @Override
    public ResponseEntity<Tenant> updateTenant(UUID tenantId, String ifMatch, TenantUpdate tenantUpdate,
            Boolean validateOnly) {
        Tenant updated = tenantService.updateTenant(tenantId, ifMatch, tenantUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
