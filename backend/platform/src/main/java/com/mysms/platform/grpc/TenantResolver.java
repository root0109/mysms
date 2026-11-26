package com.mysms.platform.grpc;

import com.mysms.core.error.InvalidArgumentException;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.platform.api.model.Branding;
import com.mysms.platform.api.model.DownloadLink;
import com.mysms.platform.api.model.Operation;
import com.mysms.platform.api.model.ReportDefinition;
import com.mysms.platform.api.model.SearchHit;
import com.mysms.platform.api.model.StockImage;
import com.mysms.platform.api.model.Tenant;
import com.mysms.platform.api.model.TenantUpdate;
import com.mysms.platform.business.BrandingService;
import com.mysms.platform.business.GlobalSearchService;
import com.mysms.platform.business.ImportService;
import com.mysms.platform.business.ReportService;
import com.mysms.platform.business.StockImageLookup;
import com.mysms.platform.business.TenantService;
import com.mysms.platform.v1.ClearDemoDataRequest;
import com.mysms.platform.v1.ClearDemoDataResponse;
import com.mysms.platform.v1.CreateTenantRequest;
import com.mysms.platform.v1.CreateTenantResponse;
import com.mysms.platform.v1.DeleteTenantRequest;
import com.mysms.platform.v1.DeleteTenantResponse;
import com.mysms.platform.v1.GetBrandingRequest;
import com.mysms.platform.v1.GetBrandingResponse;
import com.mysms.platform.v1.GetImportTemplateRequest;
import com.mysms.platform.v1.GetImportTemplateResponse;
import com.mysms.platform.v1.GetTenantRequest;
import com.mysms.platform.v1.GetTenantResponse;
import com.mysms.platform.v1.GlobalSearchRequest;
import com.mysms.platform.v1.GlobalSearchResponse;
import com.mysms.platform.v1.ListReportDefinitionsRequest;
import com.mysms.platform.v1.ListReportDefinitionsResponse;
import com.mysms.platform.v1.ListTenantsRequest;
import com.mysms.platform.v1.ListTenantsResponse;
import com.mysms.platform.v1.RunReportRequest;
import com.mysms.platform.v1.RunReportResponse;
import com.mysms.platform.v1.SearchStockImagesRequest;
import com.mysms.platform.v1.SearchStockImagesResponse;
import com.mysms.platform.v1.StartImportRequest;
import com.mysms.platform.v1.StartImportResponse;
import com.mysms.platform.v1.TenantServiceGrpc;
import com.mysms.platform.v1.UpdateTenantRequest;
import com.mysms.platform.v1.UpdateTenantResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of platform.TenantService: the tenants and the tenant-wide services REST serves beside them (branding,
 * demo-data clearance, global search, reports, imports, stock images), each delegated to the business service REST
 * uses.
 */
@GrpcService
public class TenantResolver extends TenantServiceGrpc.TenantServiceImplBase {

    private final TenantService tenantService;
    private final BrandingService brandingService;
    private final GlobalSearchService globalSearchService;
    private final ReportService reportService;
    private final StockImageLookup stockImageLookup;
    private final ImportService importService;
    private final ProtoBridge bridge;

    public TenantResolver(TenantService tenantService, BrandingService brandingService,
            GlobalSearchService globalSearchService, ReportService reportService, StockImageLookup stockImageLookup,
            ImportService importService, ProtoBridge bridge) {
        this.tenantService = tenantService;
        this.brandingService = brandingService;
        this.globalSearchService = globalSearchService;
        this.reportService = reportService;
        this.stockImageLookup = stockImageLookup;
        this.importService = importService;
        this.bridge = bridge;
    }

    @Override
    public void listTenants(ListTenantsRequest request, StreamObserver<ListTenantsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Tenant> rows = tenantService.listTenants(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListTenantsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getTenant(GetTenantRequest request, StreamObserver<GetTenantResponse> out) {
        Tenant found = tenantService.getTenant(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetTenantResponse.newBuilder(), "tenant", found).build());
    }

    @Override
    public void createTenant(CreateTenantRequest request, StreamObserver<CreateTenantResponse> out) {
        Tenant created = tenantService.createTenant(
                bridge.body(request.getTenant(), Tenant.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateTenantResponse.newBuilder(), "tenant", created).build());
    }

    @Override
    public void updateTenant(UpdateTenantRequest request, StreamObserver<UpdateTenantResponse> out) {
        Tenant updated = tenantService.updateTenant(
                bridge.id(request.getTenant().getId(), "id"),
                bridge.requiredIfMatch(request.getTenant().getEtag()),
                bridge.changes(request.getTenant(), request.getUpdateMask(), TenantUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateTenantResponse.newBuilder(), "tenant", updated).build());
    }

    @Override
    public void deleteTenant(DeleteTenantRequest request, StreamObserver<DeleteTenantResponse> out) {
        tenantService.deleteTenant(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteTenantResponse.getDefaultInstance());
    }

    @Override
    public void getBranding(GetBrandingRequest request, StreamObserver<GetBrandingResponse> out) {
        Branding branding = brandingService.getBranding(required(request.getTenantCode(), "tenantCode"),
                request.hasInstitutionId() ? bridge.id(request.getInstitutionId(), "institutionId") : null);
        ProtoBridge.send(out, bridge.reply(GetBrandingResponse.newBuilder(), "result", branding).build());
    }

    @Override
    public void clearDemoData(ClearDemoDataRequest request, StreamObserver<ClearDemoDataResponse> out) {
        Operation operation = tenantService.clearDemoData(bridge.id(request.getTenantId(), "tenantId"));
        ProtoBridge.send(out, bridge.reply(ClearDemoDataResponse.newBuilder(), "result", operation).build());
    }

    @Override
    public void globalSearch(GlobalSearchRequest request, StreamObserver<GlobalSearchResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<SearchHit> hits = globalSearchService.search(required(request.getQuery(), "query"),
                request.getEntityTypesList(), page.page(), page.size());
        ProtoBridge.send(out, bridge.page(GlobalSearchResponse.newBuilder(), hits).build());
    }

    @Override
    public void listReportDefinitions(ListReportDefinitionsRequest request,
            StreamObserver<ListReportDefinitionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ReportDefinition> reports = reportService.listReportDefinitions(
                request.hasModule() ? request.getModule() : null, page.page(), page.size());
        ProtoBridge.send(out, bridge.page(ListReportDefinitionsResponse.newBuilder(), reports).build());
    }

    @Override
    public void runReport(RunReportRequest request, StreamObserver<RunReportResponse> out) {
        Operation operation = reportService.runReport(
                bridge.body(request, com.mysms.platform.api.model.RunReportRequest.class, "request_id"));
        ProtoBridge.send(out, bridge.reply(RunReportResponse.newBuilder(), "result", operation).build());
    }

    @Override
    public void getImportTemplate(GetImportTemplateRequest request, StreamObserver<GetImportTemplateResponse> out) {
        DownloadLink template = importService.getImportTemplate(required(request.getEntityType(), "entityType"));
        ProtoBridge.send(out, bridge.reply(GetImportTemplateResponse.newBuilder(), "result", template).build());
    }

    @Override
    public void startImport(StartImportRequest request, StreamObserver<StartImportResponse> out) {
        Operation operation = importService.startImport(
                bridge.body(request, com.mysms.platform.api.model.StartImportRequest.class, "request_id"));
        ProtoBridge.send(out, bridge.reply(StartImportResponse.newBuilder(), "result", operation).build());
    }

    @Override
    public void searchStockImages(SearchStockImagesRequest request, StreamObserver<SearchStockImagesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<StockImage> images = stockImageLookup.search(required(request.getQuery(), "query"), page.page(),
                page.size());
        ProtoBridge.send(out, bridge.page(SearchStockImagesResponse.newBuilder(), images).build());
    }

    /**
     * A query parameter REST requires: proto3's empty string is "not sent", which REST's parameter binding answers
     * 400 INVALID_ARGUMENT before the service runs.
     */
    private static String required(String value, String name) {
        if (value.isEmpty()) {
            throw new InvalidArgumentException("Missing parameter " + name + ".");
        }
        return value;
    }
}
