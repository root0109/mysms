package com.mysms.lms.grpc;

import com.mysms.lms.business.CatalogService;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.lms.api.model.Settings;
import com.mysms.lms.api.model.SettingsPrimaryCountry;
import com.mysms.lms.api.model.SettingsPrimaryCountryUpdate;
import com.mysms.lms.api.model.SettingsSidebarItem;
import com.mysms.lms.api.model.SettingsSidebarItemUpdate;
import com.mysms.lms.api.model.SettingsUpdate;
import com.mysms.lms.business.SettingsService;
import com.mysms.lms.v1.CreateSettingsPrimaryCountryRequest;
import com.mysms.lms.v1.CreateSettingsPrimaryCountryResponse;
import com.mysms.lms.v1.CreateSettingsSidebarItemRequest;
import com.mysms.lms.v1.CreateSettingsSidebarItemResponse;
import com.mysms.lms.v1.DeleteSettingsPrimaryCountryRequest;
import com.mysms.lms.v1.DeleteSettingsPrimaryCountryResponse;
import com.mysms.lms.v1.DeleteSettingsSidebarItemRequest;
import com.mysms.lms.v1.DeleteSettingsSidebarItemResponse;
import com.mysms.lms.v1.GetPublicSettingsRequest;
import com.mysms.lms.v1.GetPublicSettingsResponse;
import com.mysms.lms.v1.GetSettingsPrimaryCountryRequest;
import com.mysms.lms.v1.GetSettingsPrimaryCountryResponse;
import com.mysms.lms.v1.GetSettingsRequest;
import com.mysms.lms.v1.GetSettingsResponse;
import com.mysms.lms.v1.GetSettingsSidebarItemRequest;
import com.mysms.lms.v1.GetSettingsSidebarItemResponse;
import com.mysms.lms.v1.ListSettingsPrimaryCountriesRequest;
import com.mysms.lms.v1.ListSettingsPrimaryCountriesResponse;
import com.mysms.lms.v1.ListSettingsSidebarItemsRequest;
import com.mysms.lms.v1.ListSettingsSidebarItemsResponse;
import com.mysms.lms.v1.SendContactMessageRequest;
import com.mysms.lms.v1.SendContactMessageResponse;
import com.mysms.lms.v1.SettingsServiceGrpc;
import com.mysms.lms.v1.UpdateSettingsPrimaryCountryRequest;
import com.mysms.lms.v1.UpdateSettingsPrimaryCountryResponse;
import com.mysms.lms.v1.UpdateSettingsRequest;
import com.mysms.lms.v1.UpdateSettingsResponse;
import com.mysms.lms.v1.UpdateSettingsSidebarItemRequest;
import com.mysms.lms.v1.UpdateSettingsSidebarItemResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.SettingsService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class SettingsResolver extends SettingsServiceGrpc.SettingsServiceImplBase {

    private final SettingsService service;
    private final CatalogService catalog;
    private final ProtoBridge bridge;

    public SettingsResolver(SettingsService service, CatalogService catalog, ProtoBridge bridge) {
        this.catalog = catalog;
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void getSettings(GetSettingsRequest request, StreamObserver<GetSettingsResponse> out) {
        Settings found = service.getSettings();
        ProtoBridge.send(out, bridge.reply(GetSettingsResponse.newBuilder(), "settings", found).build());
    }

    @Override
    public void updateSettings(UpdateSettingsRequest request, StreamObserver<UpdateSettingsResponse> out) {
        Settings updated = service.updateSettings(bridge.requiredIfMatch(request.getSettings().getEtag()),
                bridge.changes(request.getSettings(), request.getUpdateMask(), SettingsUpdate.class),
                bridge.updateMask(request.getUpdateMask()), false);
        ProtoBridge.send(out, bridge.reply(UpdateSettingsResponse.newBuilder(), "settings", updated).build());
    }

    @Override
    public void listSettingsSidebarItems(ListSettingsSidebarItemsRequest request,
            StreamObserver<ListSettingsSidebarItemsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<SettingsSidebarItem> rows = service.listSettingsSidebarItems(bridge.id(request.getSettingsId(), "settingsId"),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListSettingsSidebarItemsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getSettingsSidebarItem(GetSettingsSidebarItemRequest request,
            StreamObserver<GetSettingsSidebarItemResponse> out) {
        SettingsSidebarItem found = service.getSettingsSidebarItem(bridge.id(request.getSettingsId(), "settingsId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetSettingsSidebarItemResponse.newBuilder(), "settings_sidebar_item",
                found).build());
    }

    @Override
    public void createSettingsSidebarItem(CreateSettingsSidebarItemRequest request,
            StreamObserver<CreateSettingsSidebarItemResponse> out) {
        SettingsSidebarItem created = service.createSettingsSidebarItem(bridge.id(request.getSettingsId(), "settingsId"),
                bridge.body(request.getSettingsSidebarItem(), SettingsSidebarItem.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateSettingsSidebarItemResponse.newBuilder(), "settings_sidebar_item",
                created).build());
    }

    @Override
    public void updateSettingsSidebarItem(UpdateSettingsSidebarItemRequest request,
            StreamObserver<UpdateSettingsSidebarItemResponse> out) {
        SettingsSidebarItem updated = service.updateSettingsSidebarItem(bridge.id(request.getSettingsId(), "settingsId"),
                bridge.id(request.getSettingsSidebarItem().getId(), "id"),
                bridge.requiredIfMatch(request.getSettingsSidebarItem().getEtag()),
                bridge.changes(request.getSettingsSidebarItem(), request.getUpdateMask(), SettingsSidebarItemUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateSettingsSidebarItemResponse.newBuilder(), "settings_sidebar_item",
                updated).build());
    }

    @Override
    public void deleteSettingsSidebarItem(DeleteSettingsSidebarItemRequest request,
            StreamObserver<DeleteSettingsSidebarItemResponse> out) {
        service.deleteSettingsSidebarItem(bridge.id(request.getSettingsId(), "settingsId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteSettingsSidebarItemResponse.getDefaultInstance());
    }

    @Override
    public void listSettingsPrimaryCountries(ListSettingsPrimaryCountriesRequest request,
            StreamObserver<ListSettingsPrimaryCountriesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<SettingsPrimaryCountry> rows = service.listSettingsPrimaryCountrys(
                bridge.id(request.getSettingsId(), "settingsId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListSettingsPrimaryCountriesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getSettingsPrimaryCountry(GetSettingsPrimaryCountryRequest request,
            StreamObserver<GetSettingsPrimaryCountryResponse> out) {
        SettingsPrimaryCountry found = service.getSettingsPrimaryCountry(bridge.id(request.getSettingsId(), "settingsId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetSettingsPrimaryCountryResponse.newBuilder(), "settings_primary_country",
                found).build());
    }

    @Override
    public void createSettingsPrimaryCountry(CreateSettingsPrimaryCountryRequest request,
            StreamObserver<CreateSettingsPrimaryCountryResponse> out) {
        SettingsPrimaryCountry created = service.createSettingsPrimaryCountry(bridge.id(request.getSettingsId(), "settingsId"),
                bridge.body(request.getSettingsPrimaryCountry(), SettingsPrimaryCountry.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateSettingsPrimaryCountryResponse.newBuilder(), "settings_primary_country",
                created).build());
    }

    @Override
    public void updateSettingsPrimaryCountry(UpdateSettingsPrimaryCountryRequest request,
            StreamObserver<UpdateSettingsPrimaryCountryResponse> out) {
        SettingsPrimaryCountry updated = service.updateSettingsPrimaryCountry(bridge.id(request.getSettingsId(), "settingsId"),
                bridge.id(request.getSettingsPrimaryCountry().getId(), "id"),
                bridge.requiredIfMatch(request.getSettingsPrimaryCountry().getEtag()),
                bridge.changes(request.getSettingsPrimaryCountry(), request.getUpdateMask(), SettingsPrimaryCountryUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateSettingsPrimaryCountryResponse.newBuilder(), "settings_primary_country",
                updated).build());
    }

    @Override
    public void deleteSettingsPrimaryCountry(DeleteSettingsPrimaryCountryRequest request,
            StreamObserver<DeleteSettingsPrimaryCountryResponse> out) {
        service.deleteSettingsPrimaryCountry(bridge.id(request.getSettingsId(), "settingsId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteSettingsPrimaryCountryResponse.getDefaultInstance());
    }

    @Override
    public void getPublicSettings(GetPublicSettingsRequest request, StreamObserver<GetPublicSettingsResponse> out) {
        ProtoBridge.send(out, bridge.reply(GetPublicSettingsResponse.newBuilder(), "result", catalog.getPublicSettings())
                .build());
    }

    @Override
    public void sendContactMessage(SendContactMessageRequest request, StreamObserver<SendContactMessageResponse> out) {
        catalog.sendContactMessage(bridge.body(request, com.mysms.lms.api.model.SendContactMessageRequest.class,
                "request_id"));
        ProtoBridge.send(out, SendContactMessageResponse.getDefaultInstance());
    }
}
